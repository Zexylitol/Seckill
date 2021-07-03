package com.miaoshaproject.service.model;

import lombok.Data;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.io.Serializable;

/**
 * @author yzze
 * @create 2021-06-13 16:29
 */
@Data
public class UserModel implements Serializable {
    private Integer id;            //用户id
    @NotBlank(message = "用户名不能为空")
    private String name;           //用户名

    @NotNull(message = "性别不能不填")
    private Byte gender;           //性别

    @NotNull(message = "年龄不能不填")
    @Min(value = 0,message = "年龄必须大于0岁")
    @Max(value = 150,message = "年龄不能大于150岁")
    private Integer age;            //年龄

    @NotBlank(message = "手机号不能为空")
    private String telphone;       //手机号

    private String registerMode;   //注册方式
    private String thirdPartyId;   //第三方Id

    @NotBlank(message = "密码不能为空")
    private String encrptPassword; //密文密码
}
