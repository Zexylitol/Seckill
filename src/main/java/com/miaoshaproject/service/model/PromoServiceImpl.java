package com.miaoshaproject.service.model;

import com.miaoshaproject.dao.PromoDOMapper;
import com.miaoshaproject.dataobject.PromoDO;
import com.miaoshaproject.service.PromoService;
import org.joda.time.DateTime;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * @author yzze
 * @create 2021-06-14 19:05
 */
@Service
public class PromoServiceImpl implements PromoService {

    @Autowired
    private PromoDOMapper promoDOMapper;

    @Override
    public PromoModel getPromoByItemId(Integer itemId) {
        //获取对应秒杀商品的活动信息
        PromoDO promoDO = promoDOMapper.selectByItemId(itemId);

        // dataobject->model
        PromoModel promoModel = convertFromDateObject(promoDO);
        if(Objects.isNull(promoModel)){
            return null;
        }

        //判断当前时间是否有秒杀活动即将开始或者正在进行
        DateTime now = new DateTime();
        if(promoModel.getStartTime().isAfterNow()) {
            promoModel.setStatus(1);
        } else if(promoModel.getEndTime().isBeforeNow()) {
            promoModel.setStatus(3);
        } else {
            promoModel.setStatus(2);
        }
        return promoModel;
    }

    private PromoModel convertFromDateObject(PromoDO promoDO){
        if(Objects.isNull(promoDO)){
            return null;
        }
        PromoModel promoModel=new PromoModel();
        BeanUtils.copyProperties(promoDO,promoModel);

        promoModel.setPromoItemPrice(new BigDecimal(promoDO.getPromoItemPrice()));
        promoModel.setStartTime(new DateTime(promoDO.getStartDate()));
        promoModel.setEndTime(new DateTime(promoDO.getEndDate()));

        return promoModel;
    }
}
