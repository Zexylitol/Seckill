package com.miaoshaproject.controller;

import com.google.common.util.concurrent.RateLimiter;
import com.miaoshaproject.error.BusinessException;
import com.miaoshaproject.error.EmBusinessError;
import com.miaoshaproject.mq.MqProducer;
import com.miaoshaproject.response.CommonReturnType;
import com.miaoshaproject.service.ItemService;
import com.miaoshaproject.service.OrderService;
import com.miaoshaproject.service.PromoService;
import com.miaoshaproject.service.model.UserModel;
import com.miaoshaproject.util.CodeUtil;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import javax.annotation.PostConstruct;
import javax.imageio.ImageIO;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.awt.image.RenderedImage;
import java.io.IOException;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.*;

/**
 * @author yzze
 * @create 2021-06-14 17:29
 */
@Controller(value = "order")
@RequestMapping(value = "/order")
@CrossOrigin(allowCredentials = "true",allowedHeaders = "*")   //处理跨域请求
public class OrderController extends BaseController{
    @Autowired
    private OrderService orderService;

    @Autowired
    private HttpServletRequest httpServletRequest;

    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private MqProducer mqProducer;

    @Autowired
    private ItemService itemService;

    @Autowired
    private PromoService promoService;

    private ExecutorService executorService;

    private RateLimiter orderCreateRateLimiter;

    @PostConstruct
    public void init() {
        executorService = Executors.newFixedThreadPool(20);

        // TPS:3500/sec
        orderCreateRateLimiter = RateLimiter.create(3500);
    }

    /**
     * 生成验证码
     */
    @RequestMapping(value = "/generateverifycode",method ={RequestMethod.POST, RequestMethod.GET})
    @ResponseBody
    public void generateVerifyCode(HttpServletResponse response) throws BusinessException, IOException {
        String token = httpServletRequest.getParameterMap().get("token")[0];
        if (StringUtils.isEmpty(token)) {
            throw new BusinessException(EmBusinessError.USER_NOT_LOGIN,"用户未登录, 不能生成验证码");
        }
        //获取用户登录信息
        UserModel userModel = (UserModel) redisTemplate.opsForValue().get(token);
        if (Objects.isNull(userModel)) {
            throw new BusinessException(EmBusinessError.USER_NOT_LOGIN,"用户未登录, 不能生成验证码");
        }
        Map<String, Object> map = CodeUtil.generateCodeAndPic();
        ImageIO.write((RenderedImage)map.get("codePic"), "jpeg", response.getOutputStream());
        redisTemplate.opsForValue().set("verify_code_" + userModel.getId(), map.get("code"));
        redisTemplate.expire("verify_code_" + userModel.getId(), 10, TimeUnit.MINUTES);
    }

    /**
     * 生成秒杀令牌
     * @param itemId
     * @param promoId
     * @return
     * @throws BusinessException
     */
    @RequestMapping(value = "/generatetoken",method ={RequestMethod.POST},consumes = {CONTEND_TYPE_FROMED})
    @ResponseBody
    public CommonReturnType generateToken(@RequestParam(value = "itemId") Integer itemId,
                                        @RequestParam(value = "promoId") Integer promoId,
                                          @RequestParam(value = "verifyCode") String verifyCode)
            throws BusinessException {
        String token = httpServletRequest.getParameterMap().get("token")[0];
        if (StringUtils.isEmpty(token)) {
            throw new BusinessException(EmBusinessError.USER_NOT_LOGIN,"用户未登录,不能下单");
        }

        //获取用户登录信息
        UserModel userModel = (UserModel) redisTemplate.opsForValue().get(token);
        if (Objects.isNull(userModel)) {
            throw new BusinessException(EmBusinessError.USER_NOT_LOGIN,"用户未登录,不能下单");
        }

        // 校验验证码
        String inRedisVerifyCode = (String) redisTemplate.opsForValue().get("verify_code_" + userModel.getId());
        if (!StringUtils.equalsIgnoreCase(inRedisVerifyCode, verifyCode)) {
            throw new BusinessException(EmBusinessError.PARAMETER_VALIDATION_ERROR, "请求非法");
        }

        // 获取秒杀访问令牌
        String secondKillToken = promoService.generateSecondKillToken(promoId, itemId, userModel.getId());

        if (StringUtils.isEmpty(secondKillToken)) {
            throw new BusinessException(EmBusinessError.PARAMETER_VALIDATION_ERROR, "生成令牌失败");
        }

        return CommonReturnType.create(secondKillToken);
    }

    /**
     * 封装下单请求
     *
     * @param itemId
     * @param amount
     * @param promoId
     * @param promoToken
     * @return
     * @throws BusinessException
     */
    @RequestMapping(value = "/createorder",method ={RequestMethod.POST},consumes = {CONTEND_TYPE_FROMED})
    @ResponseBody
    public CommonReturnType createOrder(@RequestParam(value = "itemId") Integer itemId,
                                        @RequestParam(value = "amount") Integer amount,
                                        @RequestParam(value = "promoId",required = false) Integer promoId,  //required = false如果不传promoId则为平时的价格
                                        @RequestParam(value = "promoToken", required = false) String promoToken)
            throws BusinessException {

        if (!orderCreateRateLimiter.tryAcquire()) {
            throw new BusinessException(EmBusinessError.RATE_LIMIT);
        }
//        Boolean isLogin = (Boolean) httpServletRequest.getSession().getAttribute("IS_LOGIN");
//        if(Objects.isNull(isLogin) || !isLogin.booleanValue()){
//            throw new BusinessException(EmBusinessError.USER_NOT_LOGIN,"用户未登录,不能下单");
//        }
        String token = httpServletRequest.getParameterMap().get("token")[0];
        if (StringUtils.isEmpty(token)) {
            throw new BusinessException(EmBusinessError.USER_NOT_LOGIN,"用户未登录,不能下单");
        }


        //获取用户登录信息
//        UserModel userModel = (UserModel) httpServletRequest.getSession().getAttribute("LOGIN_USER");
        UserModel userModel = (UserModel) redisTemplate.opsForValue().get(token);
        if (Objects.isNull(userModel)) {
            throw new BusinessException(EmBusinessError.USER_NOT_LOGIN,"用户未登录,不能下单");
        }

        // 校验秒杀令牌
        if (Objects.nonNull(promoId)) {
            String inRedisSecKillToken = (String) redisTemplate.opsForValue().get("promo_token_" + promoId + "_userId_" + userModel.getId() + "_itemId_" + itemId);
            if (!StringUtils.equals(promoToken, inRedisSecKillToken)) {
                throw new BusinessException(EmBusinessError.PARAMETER_VALIDATION_ERROR, "秒杀令牌校验失败");
            }
        }

        // 同步调用线程池的submit方法
        // 拥塞窗口为20的等待队列，用来队列化泄洪
        Future<Object> future = executorService.submit(new Callable<Object>() {
            @Override
            public Object call() throws Exception {
                // 加入库存流水init状态
                String stockLogId = itemService.initStockLog(itemId, amount);

                // 再去完成对应的下单事务型消息机制
                //OrderModel orderModel = orderService.createOrder(userModel.getId(), itemId, promoId, amount);
                // 开启异步发送事务型消息
                if (!mqProducer.transactionAsyncReduceStock(userModel.getId(), itemId, promoId, amount, stockLogId)) {
                    throw new BusinessException(EmBusinessError.UNKNOWN_ERROR, "下单失败");
                }
                return null;
            }
        });

        try {
            future.get();
        } catch (InterruptedException | ExecutionException e) {
            throw new BusinessException(EmBusinessError.UNKNOWN_ERROR);
        }

        return CommonReturnType.create(null);
    }
}
