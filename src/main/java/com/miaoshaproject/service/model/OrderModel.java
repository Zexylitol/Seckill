package com.miaoshaproject.service.model;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 用户下单的交易模型
 *
 * @author yzze
 * @create 2021-06-14 15:56
 */
@Data
public class OrderModel {
    /**
     * 订单id
     */
    private String id;

    /**
     * 购买的用户id
     */
    private Integer userId;

    /**
     * 购买的商品id
     */
    private Integer itemId;

    /**
     * 若非空，则表示是以秒杀方式下单
     */
    private Integer promoId;

    /**
     * 商品价格;若promoId非空，则是秒杀商品价格
     */
    private BigDecimal itemPrice;

    /**
     * 购买数量
     */
    private Integer amount;

    /**
     * 订单价格;若promoId非空，则是秒杀商品价格
     */
    private BigDecimal orderPrice;
}
