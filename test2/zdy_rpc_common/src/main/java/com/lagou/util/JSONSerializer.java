package com.lagou.util;

import com.alibaba.fastjson.JSON;

/**
 * @author Chenyuhua
 * @date 2020/4/21 22:45
 */
public class JSONSerializer implements Serializer {


    public byte[] serialize(Object object) {
        return JSON.toJSONBytes(object);
    }


    public <T> T deserialize(Class<T> clazz, byte[] bytes) {
        return JSON.parseObject(bytes, clazz);
    }

}