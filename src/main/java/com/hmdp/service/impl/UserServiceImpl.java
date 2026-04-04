package com.hmdp.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.dto.LoginFormDTO;
import com.hmdp.dto.Result;
import com.hmdp.dto.UserDTO;
import com.hmdp.entity.User;
import com.hmdp.mapper.UserMapper;
import com.hmdp.service.IUserService;
import com.hmdp.utils.RedisConstants;
import com.hmdp.utils.RegexUtils;
import com.hmdp.utils.SystemConstants;
import com.hmdp.utils.UserHolder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.sql.Wrapper;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * <p>
 * 服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Slf4j
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements IUserService {

    @Autowired
    private UserMapper userMapper;
    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    /**
     *
     * 发送验证码
     *
     * @param phone
     * @param session
     */
    @Override
    public Result sendCode(String phone, HttpSession session) {
        // 1. 校验手机号(可在前端校验)
        // 正则表达式校验
        boolean phoneInvalid = RegexUtils.isPhoneInvalid(phone);
        if (phoneInvalid) {
            return Result.fail("请输入正确的手机号!");
        }
        // 2. 生成验证码
        String code = RandomUtil.randomNumbers(6);
        // 使用手机号做key，将code存到其中，有效期五分钟
        stringRedisTemplate.opsForValue().set(RedisConstants.LOGIN_CODE_KEY + phone, code, RedisConstants.LOGIN_CODE_TTL, TimeUnit.MINUTES);
        // 4. 发给客户端
        log.info("发送验证码成功，验证码{}", code);
        return Result.ok(code);
    }

    /**
     *
     * 用户登录
     *
     * @param loginForm
     * @param session
     */
    @Override
    public Result login(LoginFormDTO loginForm, HttpSession session) {
        // 0. 校验手机号(可在前端校验)
        // 正则表达式校验
        boolean phoneInvalid = RegexUtils.isPhoneInvalid(loginForm.getPhone());
        if (phoneInvalid) {
            return Result.fail("请输入正确的手机号!");
        }
        // 1. 校验验证码
        String code = stringRedisTemplate.opsForValue().get(RedisConstants.LOGIN_CODE_KEY + loginForm.getPhone());
        if (code == null || !code.equals(loginForm.getCode())) {
            return Result.fail("验证码错误！");
        }
        // 2. 根据手机号查询用户
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<User>().eq(User::getPhone, loginForm.getPhone());
        User user = getOne(wrapper);
        if (user == null) {
            // 2.1 用户不存在，则注册新用户
            user = new User();
            user.setPhone(loginForm.getPhone());
            // 生成随机用户名
            user.setNickName(SystemConstants.USER_NICK_NAME_PREFIX + RandomUtil.randomString(10));
            // 插入和更新自动填充时间已由@FieldFill实现
            save(user);
        }
        // 2.2 用户存在放行

        // 如果

        // 生成token，存到redis
        String token = UUID.randomUUID().toString();
        UserDTO userDTO = BeanUtil.copyProperties(user, UserDTO.class);
        Map<String, Object> userMap = BeanUtil.beanToMap(userDTO, new HashMap<>(),
                CopyOptions.create()
                        .setIgnoreNullValue(true) // 忽略空值，防止 Redis 存入无意义的 null
                        .setFieldValueEditor((fieldName, fieldValue) -> {
                            // 核心步骤：如果值不为空，全部转为 String
                            return fieldValue != null ? fieldValue.toString() : null;
                        })
        );
        stringRedisTemplate.opsForHash().putAll(RedisConstants.LOGIN_USER_KEY + token, userMap);

        // 设置token有效期
        stringRedisTemplate.expire(RedisConstants.LOGIN_USER_KEY + token, RedisConstants.LOGIN_USER_TTL, TimeUnit.MINUTES);
        return Result.ok(token);
    }

    /**
     *
     * 登出
     */
    @Override
    public void logout(HttpServletRequest request) {
        // 1. 查询token
        String token = request.getHeader("authorization");
        if (StrUtil.isNotBlank(token)) {
            String key = RedisConstants.LOGIN_USER_KEY + token;
            // 2. 维护Redis
            stringRedisTemplate.delete(key);
        }
        UserHolder.removeUser();
    }
}
