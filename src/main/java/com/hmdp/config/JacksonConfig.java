package com.hmdp.config;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * @author weiqiang
 * @version 1.0
 * @Date 2026/4/6 11:13
 */


@Configuration
public class JacksonConfig {
    @Bean
    @Primary // 设置为首选 Bean，方便其他组件自动注入
    public ObjectMapper objectMapper() {
        ObjectMapper om = new ObjectMapper();

        // 1. 核心：日期时间处理模块
        JavaTimeModule javaTimeModule = new JavaTimeModule();
        // 设置序列化格式（Java -> JSON）
        javaTimeModule.addSerializer(LocalDateTime.class,
                new LocalDateTimeSerializer(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        // 设置反序列化格式（JSON -> Java）
        javaTimeModule.addDeserializer(LocalDateTime.class,
                new LocalDateTimeDeserializer(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));

        // 注册模块到字典中
        om.registerModule(javaTimeModule);

        // 2. 增强通用性与健壮性
        // 【反序列化】时：如果 JSON 里多出了字段，而 Java 类里没有，不要报错（防止接口升级导致旧代码崩溃）
        om.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        // 【序列化】时：如果对象属性全是 null，不要报错（允许空对象的传输）
        om.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);

        // 3. 字段命名策略（可选）：比如把 Java 的驼峰命名转为 JSON 的下划线命名
        // om.setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);

        return om;
    }
}