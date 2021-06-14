package com.miaoshaproject.service;

import com.miaoshaproject.service.model.PromoModel;

/**
 * @author yzze
 * @create 2021-06-14 19:04
 */
public interface PromoService {
    //根据itemId获取即将进行或者正在进行的秒杀活动
    PromoModel getPromoByItemId(Integer itemId);
}
