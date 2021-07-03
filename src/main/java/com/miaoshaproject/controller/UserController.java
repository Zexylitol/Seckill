package com.miaoshaproject.controller;

import com.miaoshaproject.controller.viewobject.UserVO;
import com.miaoshaproject.error.BusinessException;
import com.miaoshaproject.error.EmBusinessError;
import com.miaoshaproject.response.CommonReturnType;
import com.miaoshaproject.service.UserService;
import com.miaoshaproject.service.model.UserModel;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import sun.misc.BASE64Encoder;

import javax.servlet.http.HttpServletRequest;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * @author yzze
 * @create 2021-06-13 16:19
 */
@Controller(value = "user")
@RequestMapping(value = "/user")
@CrossOrigin(allowCredentials = "true",allowedHeaders = "*")   //处理跨域请求
                                                               // DEFAULT_ALLOWED_HEADERS: 允许跨域传输所有的header参数，将用于使用token放入header域做session共享的跨域请求
                                                               // DEFAULT_ALLOW_CREDENTIALS=true: 需配合前端设置 xhrFields 授信后使得跨域session共享
public class UserController extends BaseController {

    @Autowired
    private UserService userService;

    @Autowired
    private HttpServletRequest httpServletRequest;//此处注入的是通过spring的bean包装过的

    @Autowired
    private RedisTemplate redisTemplate;

    @RequestMapping(value = "/get")
    @ResponseBody
    public CommonReturnType getUser(@RequestParam(name = "id") Integer id) throws BusinessException {
        //调用service服务获取id对应的用户信息并返回给前端
        UserModel userModel=userService.getUserById(id);

        //若获取的用户不存在
        if(Objects.isNull(userModel)){

            throw new BusinessException(EmBusinessError.USER_NOT_EXIST);
        }

        //将核心领域模型用户对象转化为可供使用的viewobject
        UserVO userVO=convertFromModel(userModel);
        //返回通用对象
        return CommonReturnType.create(userVO);
    }
    private UserVO convertFromModel(UserModel userModel){
        if(userModel==null){
            return null;
        }
        UserVO userVO=new UserVO();
        BeanUtils.copyProperties(userModel,userVO);
        return userVO;
    }

    /**
     * 用户获取opt短信接口
     *
     * @param telphone
     * @return
     */
    @RequestMapping(value = "/getotp",method ={RequestMethod.POST},consumes = {CONTEND_TYPE_FROMED})
    @ResponseBody
    public CommonReturnType getOtp(@RequestParam(name = "telphone")String telphone){
        //1. 需要按照一定规则生成OTP验证码
        Random random=new Random();
        int randomInt=random.nextInt(899999);//此处随机数范围为[0,899999]
        randomInt+=100000;//此时随机数范围为[100000，999999];
        String otpCode=String.valueOf(randomInt);

        //2. 将otp验证码同对应用户的手机号关联,企业使用分布式的采用redis,此处使用HttpSession的方式进行手机号与OTPCode进行绑定
        httpServletRequest.getSession().setAttribute(telphone,otpCode);

        //3. 将otp验证码通过短信通道发送给用户，先省略直接打印到控制台中
        System.out.println("telphone="+ telphone + "&otpCode="+ otpCode);//在企业级开发中此种方式绝对不行，不能将用户信息暴露

        return CommonReturnType.create(null);
    }

    /**
     * 用户注册接口
     *
     * @param telphone
     * @param otpCode
     * @param name
     * @param gender
     * @param age
     * @param password
     * @return
     * @throws BusinessException
     * @throws UnsupportedEncodingException
     * @throws NoSuchAlgorithmException
     */
    @RequestMapping(value = "/register",method ={RequestMethod.POST},consumes = {CONTEND_TYPE_FROMED})
    @ResponseBody
    public CommonReturnType register(@RequestParam(name = "telphone")String telphone,
                                     @RequestParam(name = "otpCode")String otpCode,
                                     @RequestParam(name = "name")String name,
                                     @RequestParam(name = "gender")Byte gender,
                                     @RequestParam(name = "age")Integer age,
                                     @RequestParam(name = "password")String password) throws BusinessException, UnsupportedEncodingException, NoSuchAlgorithmException {

        //1. 验证手机号和对应的otpCode相符合
        String inSessionOtpCode = (String) this.httpServletRequest.getSession().getAttribute(telphone);
        if(!com.alibaba.druid.util.StringUtils.equals(otpCode,inSessionOtpCode)){  //此处使用类库中的equals的方法是因为该类库中的方法已经做了判空处理
            throw new BusinessException(EmBusinessError.PARAMETER_VALIDATION_ERROR,"短信验证码不符合");
        }
        //2. 用户的注册流程
        UserModel userModel=new UserModel();

        userModel.setName(name);
        userModel.setGender(gender);
        userModel.setAge(age);
        userModel.setTelphone(telphone);
        userModel.setRegisterMode("byphone");
        userModel.setEncrptPassword(this.enCodeByMd5(password));

        userService.register(userModel);
        return CommonReturnType.create(null);
    }
    //对密码进行加密
    public String enCodeByMd5(String str) throws NoSuchAlgorithmException, UnsupportedEncodingException {
        //确定一个计算方法
        MessageDigest md5=MessageDigest.getInstance("MD5");
        BASE64Encoder base64Encoder=new BASE64Encoder();
        //加密字符串
        String pwd= base64Encoder.encode(md5.digest(str.getBytes("UTF-8")));
        return pwd;
    }

    /**
     * 用户登录接口
     *
     * @param telphone
     * @param password
     * @return
     * @throws BusinessException
     * @throws UnsupportedEncodingException
     * @throws NoSuchAlgorithmException
     */
    @RequestMapping(value = "/login",method ={RequestMethod.POST},consumes = {CONTEND_TYPE_FROMED})
    @ResponseBody
    public CommonReturnType login(@RequestParam(name = "telphone")String telphone,
                                  @RequestParam(name = "password")String password) throws BusinessException, UnsupportedEncodingException, NoSuchAlgorithmException {

        //入参校验
        if(StringUtils.isEmpty(telphone)||StringUtils.isEmpty(password)){
            throw new BusinessException(EmBusinessError.PARAMETER_VALIDATION_ERROR);
        }

        //用户登录服务，用来校验用户登录是否合法
        UserModel userModel=userService.validateLoing(telphone,enCodeByMd5(password));

        //将登录的凭证加入到用户的登录成功的session内
        // 修改：若用户登录验证成功后将对应的登录信息和登录凭证一起存入redis中
        // 生成登录凭证token，UUID
        String uuidToken = UUID.randomUUID().toString();
        uuidToken = uuidToken.replace("-", "");
        // 建立token和用户登录态之间的联系
        redisTemplate.opsForValue().set(uuidToken, userModel);
        redisTemplate.expire(uuidToken, 1, TimeUnit.HOURS);
        //        this.httpServletRequest.getSession().setAttribute("IS_LOGIN",true);
//        this.httpServletRequest.getSession().setAttribute("LOGIN_USER",userModel);
        // 下发token
        return CommonReturnType.create(uuidToken);

    }
}
