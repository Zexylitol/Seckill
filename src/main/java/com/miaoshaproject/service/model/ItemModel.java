package com.miaoshaproject.service.model;

import lombok.Data;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.math.BigDecimal;

/**
 * @author yzze
 * @create 2021-06-14 10:30
 */
@Data
public class ItemModel implements Serializable {
    private Integer id;            //商品主键id;

    @NotBlank(message = "商品名称不能为空")
    private String title;          //商品名称;

    @NotNull(message = "商品价格不能为空")
    @Min(value = 0,message = "商品价格必须大于0")
    private BigDecimal price;      //商品价格;

    @NotNull(message = "库存不能不填")
    private Integer stock;         //商品库存;

    @NotBlank(message = "商品描述信息不能为空")
    private String description;    //商品描述;

    private Integer sales;         //商品销量;

    @NotBlank(message = "商品图片信息不能为空")
    private String imgUrl;         //商品描述图片的url;

    /**
     * 使用聚合模型
     * 如果promoModel不为空，则表示其拥有还未结束的秒杀活动
     */
    private PromoModel promoModel;
}
