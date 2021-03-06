package com.miaoshaproject.service;

import com.miaoshaproject.error.BusinessException;
import com.miaoshaproject.service.model.UserModel;

/**
 * @author yzze
 * @create 2021-06-13 16:23
 */
public interface UserService {
    //通过用户id对象的方法;
    UserModel getUserById(Integer id);

    void register(UserModel userModel) throws BusinessException;

    /**
     * @param telphone   用户注册手机
     * @param encrptPassword    用户加密后的密码
     * @return
     * @throws BusinessException
     */
    UserModel validateLoing(String telphone,String encrptPassword) throws BusinessException;

    // 通过缓存获取用户对象
    UserModel getUserByIdInCache(Integer id);
}
