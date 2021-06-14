package com.miaoshaproject.service.model;

import lombok.Data;
import org.joda.time.DateTime;

import java.math.BigDecimal;

/**
 * @author yzze
 * @create 2021-06-14 18:44
 */
@Data
public class PromoModel {
    /**
     * 主键id
     */
    private Integer id;

    /**
     * 秒杀活动状态:1,未开始 2,进行中 3,已结束
     */
    private Integer status;
    private String promoName;           //秒杀活动名称;
    private DateTime startTime;         //秒杀开始时间;
    private DateTime endTime;           //秒杀结束时间

    /**
     * 秒杀活动适用商品
     */
    private Integer itemId;
    private BigDecimal promoItemPrice;  //秒杀活动商品价格;
}
