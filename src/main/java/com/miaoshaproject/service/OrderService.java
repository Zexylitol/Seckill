package com.miaoshaproject.service;

import com.miaoshaproject.error.BusinessException;
import com.miaoshaproject.service.model.OrderModel;

/**
 * @author yzze
 * @create 2021-06-14 16:16
 */
public interface OrderService {
    //1. 通过前端url上传过来的秒杀活动商品id,然后下单接口内校验对应id是否属于对应商品且活动已经开始！
    //2. 直接在下单接口内判断商品是否存在秒杀活动，若存在进行中则以秒杀价格下单
    //第一种优势最大！，采用第一种
    OrderModel createOrder(Integer userId, Integer itemId, Integer promoId, Integer amount) throws BusinessException;

}
