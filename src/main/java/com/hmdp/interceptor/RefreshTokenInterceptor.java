package com.hmdp.interceptor;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.hmdp.dto.UserDTO;
import com.hmdp.utils.RedisConstants;
import com.hmdp.utils.UserHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 *
 * 用于刷新token的拦截器
 * @author weiqiang
 * @date 2026/04/03
 */

@Component
public class RefreshTokenInterceptor implements HandlerInterceptor {

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 1. 请求头中获取token
        String token = request.getHeader("authorization");
        if (StrUtil.isBlank(token)) {
            // 可能是游客，放行，此时线程空间为空，下一个拦截器可以拦截
            return true;
        }
        // 2. 从redis中查询相关信息
        String key = RedisConstants.LOGIN_USER_KEY + token;
        Map<Object, Object> entries = stringRedisTemplate.opsForHash().entries(key);
        // 3. 查看用户是否存在
        if (entries.isEmpty()) {
            // token可能过期了，放行，此时线程空间为空，下一个拦截器可以拦截
            return true;
        }
        UserDTO dto = BeanUtil.mapToBean(entries, UserDTO.class, true, null);
        // 4. 保存到线程空间
        UserHolder.saveUser(dto);
        // 5. 续期
        stringRedisTemplate.expire(key,RedisConstants.LOGIN_USER_TTL, TimeUnit.MINUTES);
        // 6. 放行
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        // 请求处理完之后将线程空间清除，防止内存泄漏
        UserHolder.removeUser();
    }

}
