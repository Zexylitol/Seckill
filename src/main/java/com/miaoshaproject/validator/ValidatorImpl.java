package com.miaoshaproject.validator;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Component;

import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;
import java.util.Set;

/**
 * @author yzze
 * @create 2021-06-13 23:25
 */
@Component   //定义为一个spring  bean;,方便扫描
public class ValidatorImpl implements InitializingBean {

    private Validator validator;

    //实现校验方法并返回校验结果
    public ValidationResult validate(Object bean){
        ValidationResult result=new ValidationResult();
        Set<ConstraintViolation<Object>> constraintViolationSet= validator.validate(bean);
        if (constraintViolationSet.size()>0){
            //有错误
            result.setHasErrors(true);
            constraintViolationSet.forEach(constraintViolation->{
                String errMsg=constraintViolation.getMessage();
                String propertyName=constraintViolation.getPropertyPath().toString();
                result.getErrorMsgMap().put(propertyName,errMsg);
            });
        }
        return result;

    }

    @Override
    public void afterPropertiesSet() throws Exception {
        //将hibernate的validator通过工厂的初始化方式使其实例化
        this.validator= Validation.buildDefaultValidatorFactory().getValidator();
    }
}
