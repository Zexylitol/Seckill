package com.miaoshaproject.service.impl;

import com.miaoshaproject.dao.UserDOMapper;
import com.miaoshaproject.dao.UserPasswordDOMapper;
import com.miaoshaproject.dataobject.UserDO;
import com.miaoshaproject.dataobject.UserPasswordDO;
import com.miaoshaproject.error.BusinessException;
import com.miaoshaproject.error.EmBusinessError;
import com.miaoshaproject.service.UserService;
import com.miaoshaproject.service.model.UserModel;
import com.miaoshaproject.validator.ValidationResult;
import com.miaoshaproject.validator.ValidatorImpl;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * @author yzze
 * @create 2021-06-13 16:23
 */
@Service
public class UserServiceImpl implements UserService {

    //注入UserDOMapper
    @Autowired
    private UserDOMapper userDOMapper;

    @Autowired
    private UserPasswordDOMapper userPasswordDOMapper;

    @Autowired
    private ValidatorImpl validator;

    @Autowired
    private RedisTemplate redisTemplate;

    @Override
    public UserModel getUserById(Integer id) {
        //返回值为void的原因:在service层不能直接把对应数据的信息返回透传给service的服务,因此需建立一个model!!!
        //调用userDo对象获取对应的用户dataobject
        UserDO userDO =userDOMapper.selectByPrimaryKey(id);
        if(Objects.isNull(userDO)){
            return null;
        }
        //通过用户id获取对应用户的加密密码信息
        UserPasswordDO userPasswordDO=userPasswordDOMapper.selectByUserId(userDO.getId());
        return convertFromdataObject(userDO,userPasswordDO);
    }

    /**
     * 注册
     *
     * @param userModel
     * @throws BusinessException
     */
    @Override
    @Transactional
    public void register(UserModel userModel) throws BusinessException {
        if (userModel==null){
            throw new BusinessException(EmBusinessError.PARAMETER_VALIDATION_ERROR);
        }

        ValidationResult result =validator.validate(userModel);
        if (result.isHasErrors()) {
            throw new BusinessException(EmBusinessError.PARAMETER_VALIDATION_ERROR, result.getErrMsg());
        }

        //实现UserModel->dataobject方法
        UserDO userDO=convertFromModel(userModel);
        try{
            userDOMapper.insertSelective(userDO);//使用此方法而非insert的原因为此方法加入了非空的判断，较为合理详细可见对应的mapper映射文件
        }catch (DuplicateKeyException ex){
            throw new BusinessException(EmBusinessError.PARAMETER_VALIDATION_ERROR,"手机号已注册！");
        }


        UserDO userDO1 = userDOMapper.selectByTelphone(userDO.getTelphone());
        userModel.setId(userDO1.getId());
        UserPasswordDO userPasswordDO=convertPasswordFromModel(userModel);

        userPasswordDOMapper.insertSelective(userPasswordDO);
        return;
    }

    /**
     * 登录
     *
     * @param telphone   用户注册手机
     * @param encrptPassword    用户加密后的密码
     * @return
     * @throws BusinessException
     */
    @Override
    public UserModel validateLoing(String telphone, String encrptPassword) throws BusinessException {
        //通过用户手机号获取用户信息
        UserDO userDO=userDOMapper.selectByTelphone(telphone);
        if (userDO==null){
            throw new BusinessException(EmBusinessError.USER_LOGIN_FAIL);
        }
        UserPasswordDO userPasswordDO=userPasswordDOMapper.selectByUserId(userDO.getId());
        UserModel userModel=convertFromdataObject(userDO,userPasswordDO);

        //比对用户信息内加密密码是否和加密进来的密码相匹配
        if(!StringUtils.equals(encrptPassword, userModel.getEncrptPassword())){
            throw new BusinessException(EmBusinessError.USER_LOGIN_FAIL);
        }
        return userModel;
    }

    @Override
    public UserModel getUserByIdInCache(Integer id) {
        UserModel userModel = (UserModel) redisTemplate.opsForValue().get("user_validate_" + id);
        if (Objects.isNull(userModel)) {
            userModel = this.getUserById(id);
            redisTemplate.opsForValue().set("user_validate_" + id, userModel);
            redisTemplate.expire("user_validate_" + id, 10, TimeUnit.MINUTES);
        }
        return userModel;
    }

    private UserDO convertFromModel(UserModel userModel) {
        if (Objects.isNull(userModel)) {
            return null;
        }
        UserDO userDO = new UserDO();
        BeanUtils.copyProperties(userModel, userDO);
        return userDO;
    }

    private UserPasswordDO convertPasswordFromModel(UserModel userModel){
        if(Objects.isNull(userModel)){
            return null;
        }
        UserPasswordDO userPasswordDO=new UserPasswordDO();
        userPasswordDO.setEncrptPassword(userModel.getEncrptPassword());
        userPasswordDO.setUserId(userModel.getId());//将user_password表中的user_id与user_info表中相应的主键id绑定一起！
        return userPasswordDO;
    }

    private UserModel convertFromdataObject(UserDO userDO, UserPasswordDO userPasswordDO){
        if(Objects.isNull(userDO)){
            return null;
        }
        UserModel userModel=new UserModel();
        BeanUtils.copyProperties(userDO,userModel);

        if(!Objects.isNull(userPasswordDO)){
            userModel.setEncrptPassword(userPasswordDO.getEncrptPassword());
        }

        return userModel;
    }
}
