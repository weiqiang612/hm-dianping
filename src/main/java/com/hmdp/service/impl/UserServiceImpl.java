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
import org.springframework.data.redis.connection.BitFieldSubCommands;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.sql.Wrapper;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
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

    @Override
    public Result sign() {
        // 1. 获取当前用户
        UserDTO dto = UserHolder.getUser();
        // 2. 判断用户是否存在
        if (dto == null) {
            return Result.fail("请先登录！");
        }
        // 3. 存在进行签到
        // 键 ： sign:用户id:年月
        String key = RedisConstants.USER_SIGN_KEY + dto.getId() + ":" + LocalDateTime.now().format(SystemConstants.DATE_FORMATTER_YYYYMM);
        Boolean success = stringRedisTemplate.opsForValue().setBit(key, LocalDateTime.now().getDayOfMonth() - 1, true);
        // 4. 返回结果
        if (Boolean.FALSE.equals(success)) {
            return Result.fail("签到失败！");
        }
        return Result.ok();
    }

    /**
     *
     * 签到统计功能
     *
     * @return {@link Result }
     */
    @Override
    public Result signCount() {
        // 1. 获取当前用户
        UserDTO dto = UserHolder.getUser();
        // 2. 判断用户是否登录
        if (dto == null) {
            return Result.fail("请先登录！");
        }
        // 3. 获取该用户的签到数据
        String key = RedisConstants.USER_SIGN_KEY + dto.getId() + ":" + LocalDateTime.now().format(SystemConstants.DATE_FORMATTER_YYYYMM);
        // 3.1 获取本月截止到今天的所有签到记录
        // 这里返回的是一个十进制数字，表示当前月的签到情况
        List<Long> bitField = stringRedisTemplate.opsForValue().bitField(
                key,
                // 从0开始，获取今天是本月的第几天，就获取多少位签到记录
                BitFieldSubCommands.create()
                        .get(BitFieldSubCommands.BitFieldType.unsigned(LocalDateTime.now().getDayOfMonth())).valueAt(0)
        );
        // 4. 对数据进行处理得到本月的连续签到天数
        // 获取所有连续签到天数，比较得到最大连续签到天数
        if (bitField == null || bitField.isEmpty()) {
            return Result.ok(0);
        }
        Long signSituation = bitField.get(0);
        if (signSituation == null ||  signSituation == 0) {
            return Result.ok(0);
        }
        // 从今天的位置开始向前统计，直到遇到第一次未签到为止，为一个连续签到天数
        int nowCount = 0;
        // 如果为0，说明未签到，结束
        // 如果为1，说明已签到，连续签到天数加1，继续统计下一位
        while ((signSituation & 1) != 0) {
            // 4.1 将该数字与1进行与运算，得到最后一位的签到情况
            // 4.2 判断是否为0
            nowCount++;
            // 4.3 对该数字进行右移操作，继续统计下一位
            signSituation >>>= 1;
        }
        return Result.ok(nowCount);
    }

}
