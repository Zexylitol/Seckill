package com.miaoshaproject.controller;

import com.miaoshaproject.controller.viewobject.ItemVO;
import com.miaoshaproject.error.BusinessException;
import com.miaoshaproject.response.CommonReturnType;
import com.miaoshaproject.service.ItemService;
import com.miaoshaproject.service.model.ItemModel;
import org.joda.time.format.DateTimeFormat;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * @author yzze
 * @create 2021-06-14 11:53
 */
@Controller(value = "item")
@RequestMapping(value = "/item")
@CrossOrigin(allowCredentials = "true",allowedHeaders = "*")   //处理跨域请求
public class ItemController extends BaseController {
    @Autowired
    private ItemService itemService;

    @Autowired
    private RedisTemplate redisTemplate;

    /**
     * 创建商品的Controller
     *
     * @param title
     * @param description
     * @param price
     * @param stock
     * @param imgUrl
     * @return
     * @throws BusinessException
     */
    @RequestMapping(value = "/create",method ={RequestMethod.POST},consumes = {CONTEND_TYPE_FROMED})
    @ResponseBody
    public CommonReturnType createItem(@RequestParam(name = "title")String title,
                                       @RequestParam(name = "description")String description,
                                       @RequestParam(name = "price") BigDecimal price,
                                       @RequestParam(name = "stock")Integer stock,
                                       @RequestParam(name = "imgUrl")String imgUrl) throws BusinessException {

        // 封装service请求来创建商品
        ItemModel itemModel = new ItemModel();
        itemModel.setTitle(title);
        itemModel.setDescription(description);
        itemModel.setPrice(price);
        itemModel.setStock(stock);
        itemModel.setImgUrl(imgUrl);

        ItemModel itemModel1ForReturn = itemService.createItem(itemModel);

        ItemVO itemVO = convertVOFromModel(itemModel1ForReturn);//返回给前端

        return CommonReturnType.create(itemVO);
    }

    /**
     * 商品详情浏览
     *
     * @param id
     * @return
     */
    @RequestMapping(value = "/get",method ={RequestMethod.GET})
    @ResponseBody
    public CommonReturnType getItem(@RequestParam(name = "id")Integer id){

        // 根据商品的id到redis内获取
        ItemModel itemModel = (ItemModel) redisTemplate.opsForValue().get("item_" + id);
        // 若redis内不存在对应的itemModel，则访问下游 service
        if (Objects.isNull(itemModel)) {
             itemModel = itemService.getItemById(id);
             // 设置 itemModel 到 redis 内
            redisTemplate.opsForValue().set("item_" + id, itemModel);
            // 失效时间
            redisTemplate.expire("item_" + id, 10, TimeUnit.MINUTES);
        }

        ItemVO itemVO = convertVOFromModel(itemModel);
        return CommonReturnType.create(itemVO);
    }

    private ItemVO convertVOFromModel(ItemModel itemModel) {
        if(Objects.isNull(itemModel)) {
            return null;
        }
        ItemVO itemVO = new ItemVO();
        BeanUtils.copyProperties(itemModel,itemVO);
        if(!Objects.isNull(itemModel.getPromoModel())) {
            //有正在进行或者即将进行的秒杀活动
            itemVO.setPromoStatus(itemModel.getPromoModel().getStatus());
            itemVO.setPromoId(itemModel.getPromoModel().getId());
            itemVO.setStartTime(itemModel.getPromoModel().getStartTime().toString(DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss")));
            itemVO.setPromoPrice(itemModel.getPromoModel().getPromoItemPrice());
        } else {
            itemVO.setPromoStatus(0);
        }
        return itemVO;
    }


    /**
     * 商品列表页面浏览
     *
     * @return
     */
    @RequestMapping(value = "/list",method ={RequestMethod.GET})
    @ResponseBody
    public CommonReturnType listItem(){
        List<ItemModel> itemModelList=itemService.listItem();

        //使用stream将list内的itemModle转发为itemVO
        List<ItemVO> itemVOList=itemModelList.stream().map(itemModel -> {
            ItemVO itemVO=convertVOFromModel(itemModel);
            return itemVO;
        }).collect(Collectors.toList());
        return CommonReturnType.create(itemVOList);
    }
}
