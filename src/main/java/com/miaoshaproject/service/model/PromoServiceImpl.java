package com.miaoshaproject.service.model;

import com.miaoshaproject.dao.PromoDOMapper;
import com.miaoshaproject.dataobject.PromoDO;
import com.miaoshaproject.error.BusinessException;
import com.miaoshaproject.error.EmBusinessError;
import com.miaoshaproject.service.ItemService;
import com.miaoshaproject.service.PromoService;
import com.miaoshaproject.service.UserService;
import org.joda.time.DateTime;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * @author yzze
 * @create 2021-06-14 19:05
 */
@Service
public class PromoServiceImpl implements PromoService {

    @Autowired
    private PromoDOMapper promoDOMapper;

    @Autowired
    private ItemService itemService;

    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private UserService userService;

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

    @Override
    public void publishPromo(Integer promoId) {
        PromoDO promoDO = promoDOMapper.selectByPrimaryKey(promoId);
        if (Objects.isNull(promoDO) || promoDO.getItemId() == 0) {
            return ;
        }
        ItemModel itemModel = itemService.getItemById(promoDO.getItemId());

        // 将库存同步到redis内
        redisTemplate.opsForValue().set("promo_item_stock_" + itemModel.getId(), itemModel.getStock());

        // 将大闸的限制数字设到redis内
        redisTemplate.opsForValue().set("promo_door_count_" + promoId, itemModel.getStock() * 5);
    }

    @Override
    public String generateSecondKillToken(Integer promoId, Integer itemId, Integer userId) {

        // 判断库存是否已售罄
        Boolean hasKey = redisTemplate.hasKey("promo_item_stock_invalid_" + itemId);
        if (hasKey) {
            // 返回
            return null;
        }

        PromoDO promoDO = promoDOMapper.selectByPrimaryKey(promoId);
        PromoModel promoModel = convertFromDateObject(promoDO);
        if (Objects.isNull(promoModel)) {
            return null;
        }
        // 判断当前秒杀活动状态
        if (promoModel.getStartTime().isAfterNow()) {
            promoModel.setStatus(1);
        } else if (promoModel.getEndTime().isBeforeNow()) {
            promoModel.setStatus(3);
        } else {
            promoModel.setStatus(2);
        }
        // 判断秒杀活动是否正在进行
        if (promoModel.getStatus() != 2) {
            return null;
        }

        // 商品信息验证
        ItemModel itemModel = itemService.getItemByIdInCache(itemId);
        if(Objects.isNull(itemModel)) {
            return null;
        }
        // 用户信息验证
        //UserModel userModel=userService.getUserById(userId);
        UserModel userModel = userService.getUserByIdInCache(userId);
        if(Objects.isNull(userModel)) {
            return null;
        }
        // 获取秒杀大闸的count数量
        Long increment = redisTemplate.opsForValue().increment("promo_door_count_" + promoId, -1);
        if (increment < 0) {
            return null;
        }

        // 生成秒杀令牌并存入redis
        String token = UUID.randomUUID().toString().replace("-", "");

        // 一个用户在同一时间对一个秒杀活动中的一个商品有对应令牌的生成权限
        redisTemplate.opsForValue().set("promo_token_" + promoId + "_userId_" + userId + "_itemId_" + itemId, token);
        redisTemplate.expire("promo_token_" + promoId, 5, TimeUnit.MINUTES);

        return token;
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
