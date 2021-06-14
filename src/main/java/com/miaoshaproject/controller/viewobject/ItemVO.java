package com.miaoshaproject.controller.viewobject;

import lombok.Data;

import java.math.BigDecimal;

/**
 * @author yzze
 * @create 2021-06-14 11:56
 */
@Data
public class ItemVO {
    /**
     * 商品主键id
     */
    private Integer id;

    /**
     * 商品名称
     */
    private String title;

    /**
     * 商品价格
     */
    private BigDecimal price;

    /**
     * 商品库存
     */
    private Integer stock;

    /**
     * 商品描述
     */
    private String description;

    /**
     * 商品销量
     */
    private Integer sales;

    /**
     * 商品描述图片的url
     */
    private String imgUrl;

    /**
     * 记录商品是否在秒杀活动中,以及对应的状态：0 没有，2 待开始，3 进行中
     */
    private Integer promoStatus;

    /**
     * 秒杀活动商品价格
     */
    private BigDecimal promoPrice;

    /**
     * 秒杀活动id
     */
    private Integer promoId;

    /**
     * 秒杀活动开始时间
     */
    private String startTime;
}
