package com.miaoshaproject.controller.viewobject;

import lombok.Data;

/**
 * @author yzze
 * @create 2021-06-13 16:45
 */
@Data
public class UserVO {
    private Integer id;            //用户id
    private String name;           //用户名
    private Byte gender;           //性别
    private Integer age;            //年龄
    private String telphone;       //手机号
}
