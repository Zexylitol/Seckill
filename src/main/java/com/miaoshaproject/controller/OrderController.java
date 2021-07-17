package com.miaoshaproject.controller;

import com.miaoshaproject.error.BusinessException;
import com.miaoshaproject.error.EmBusinessError;
import com.miaoshaproject.mq.MqProducer;
import com.miaoshaproject.response.CommonReturnType;
import com.miaoshaproject.service.ItemService;
import com.miaoshaproject.service.OrderService;
import com.miaoshaproject.service.model.OrderModel;
import com.miaoshaproject.service.model.UserModel;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.Objects;

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

    /**
     * 封装下单请求
     *
     * @param itemId
     * @param amount
     * @param promoId
     * @return
     * @throws BusinessException
     */
    @RequestMapping(value = "/createorder",method ={RequestMethod.POST},consumes = {CONTEND_TYPE_FROMED})
    @ResponseBody
    public CommonReturnType createOrder(@RequestParam(value = "itemId") Integer itemId,
                                        @RequestParam(value = "amount") Integer amount,
                                        @RequestParam(value = "promoId",required = false) Integer promoId)  //required = false如果不传promoId则为平时的价格
            throws BusinessException {
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

        // 判断库存是否已售罄
        Boolean hasKey = redisTemplate.hasKey("promo_item_stock_invalid_" + itemId);
        if (hasKey) {
            // 返回下单失败
            throw new BusinessException(EmBusinessError.STOCK_NOT_ENOUGH);
        }

        // 加入库存流水init状态
        String stockLogId = itemService.initStockLog(itemId, amount);

        // 再去完成对应的下单事务型消息机制
        //OrderModel orderModel = orderService.createOrder(userModel.getId(), itemId, promoId, amount);
        // 开启异步发送事务型消息
        if (!mqProducer.transactionAsyncReduceStock(userModel.getId(), itemId, promoId, amount, stockLogId)) {
            throw new BusinessException(EmBusinessError.UNKNOWN_ERROR, "下单失败");
        }
        return CommonReturnType.create(null);
    }
}
