package com.hmdp.utils;

import cn.hutool.core.util.StrUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.CollectionType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * @author weiqiang
 * @version 1.0
 * @Date 2026/4/6 11:18
 */

@Component
@Slf4j
public class JsonUtils {

    private static ObjectMapper mapper;

    // 关键：利用 Spring 注入已配置好的 ObjectMapper
    @Autowired
    public void setMapper(ObjectMapper objectMapper) {
        JsonUtils.mapper = objectMapper;
    }

    /**
     * 对象转 JSON 字符串
     */
    public static String toJsonStr(Object obj) {
        if (obj == null) return null;
        try {
            return mapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            log.error("Jackson 序列化异常", e);
            throw new RuntimeException(e);
        }
    }

    /**
     * JSON 字符串转对象
     */
    public static <T> T toBean(String json, Class<T> clazz) {
        if (StrUtil.isBlank(json)) return null;
        try {
            return mapper.readValue(json, clazz);
        } catch (JsonProcessingException e) {
            log.error("Jackson 反序列化异常", e);
            throw new RuntimeException(e);
        }
    }

    /**
     * 万能转换：处理复杂的泛型或 Object 转具体的 Bean
     */
    public static <T> T convert(Object obj, Class<T> clazz) {
        return mapper.convertValue(obj, clazz);
    }

    /**
     * JSON 字符串转 List 集合
     */
    public static <T> List<T> toList(String json, Class<T> clazz) {
        if (StrUtil.isBlank(json)) return null;
        try {
            // 利用 JavaType 告知 Jackson 这是一个 List<T> 结构
            CollectionType listType = mapper.getTypeFactory()
                    .constructCollectionType(List.class, clazz);
            return mapper.readValue(json, listType);
        } catch (JsonProcessingException e) {
            log.error("Jackson 反序列化 List 异常", e);
            throw new RuntimeException(e);
        }
    }
}