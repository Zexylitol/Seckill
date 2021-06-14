package com.miaoshaproject.error;

/**
 * @author yzze
 * @create 2021-06-13 17:01
 */
public interface CommonError {
    public int getErrCode();
    public String getErrMsg();
    public CommonError setErrMsg(String errMsg);
}
