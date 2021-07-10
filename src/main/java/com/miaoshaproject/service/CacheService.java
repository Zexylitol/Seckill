package com.miaoshaproject.service;

/**
 * 封装本地缓存操作类
 *
 * @author yzze
 * @create 2021-07-10 12:11
 */
public interface CacheService {
    // 存
    void setCommonCache(String key, Object value);

    // 取
    Object getFromCommonCache(String key);
}
