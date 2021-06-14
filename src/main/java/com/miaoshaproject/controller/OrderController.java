package com.miaoshaproject.controller;

import com.miaoshaproject.error.BusinessException;
import com.miaoshaproject.error.EmBusinessError;
import com.miaoshaproject.response.CommonReturnType;
import com.miaoshaproject.service.OrderService;
import com.miaoshaproject.service.model.OrderModel;
import com.miaoshaproject.service.model.UserModel;
import org.springframework.beans.factory.annotation.Autowired;
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
        Boolean isLogin = (Boolean) httpServletRequest.getSession().getAttribute("IS_LOGIN");
        if(Objects.isNull(isLogin) || !isLogin.booleanValue()){
            throw new BusinessException(EmBusinessError.USER_NOT_LOGIN,"用户未登录,不能下单");
        }

        //获取用户登录信息
        UserModel userModel = (UserModel) httpServletRequest.getSession().getAttribute("LOGIN_USER");
        OrderModel orderModel = orderService.createOrder(userModel.getId(), itemId, promoId, amount);

        return CommonReturnType.create(null);
    }
}
