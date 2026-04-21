package com.hmdp.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.hmdp.dto.Result;
import com.hmdp.dto.UserDTO;
import com.hmdp.entity.Follow;
import com.hmdp.entity.User;
import com.hmdp.mapper.FollowMapper;
import com.hmdp.service.IFollowService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.service.IUserService;
import com.hmdp.utils.UserHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * <p>
 * 服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
public class FollowServiceImpl extends ServiceImpl<FollowMapper, Follow> implements IFollowService {

    @Autowired
    private IUserService userService;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    /**
     *
     * 关注博主
     *
     * @param followUserId
     * @param isFollow
     * @return {@link Result }
     */
    @Override
    public Result follow(Long followUserId, Boolean isFollow) {
        // 1. 博主不存在，返回错误信息
        User blogger = userService.getById(followUserId);
        if (blogger == null) {
            return Result.fail("博主不存在");
        }

        UserDTO dto = UserHolder.getUser();
        // 当前用户未登录
        if (dto == null) {
            return Result.fail("请先登录");
        }
        Long userId = dto.getId();

        String key = "follow:" + userId;
        // 2. 关注操作
        if (isFollow) {
            Follow follow = new Follow();
            follow.setUserId(userId);
            follow.setFollowUserId(followUserId);
            boolean save = save(follow);
            if (save) {
                // 3. 将关注用户的ID保存到 Redis 的 Set 中，SADD key value
                stringRedisTemplate.opsForSet().add(key, followUserId.toString());
            }

        } else {
            // 取关操作
            boolean removed = remove(new QueryWrapper<Follow>()
                    .eq("user_id", userId)
                    .eq("follow_user_id", followUserId));
            if (removed) {
                stringRedisTemplate.opsForSet().remove(key, followUserId.toString());
            }

        }
        return Result.ok();
    }

    /**
     *
     * 判断当前用户是否关注了博主
     *
     * @param followUserId
     * @return {@link Result }
     */
    @Override
    public Result orNot(Long followUserId) {
        UserDTO dto = UserHolder.getUser();
        // 当前用户未登录
        if (dto == null) {
            return Result.fail("请先登录");
        }
        Long userId = dto.getId();
        Integer count = query().eq("user_id", userId)
                .eq("follow_user_id", followUserId).count();
        return Result.ok(count > 0);
    }

    @Override
    public Result followCommons(Long id) {
        // 1. 获取当前登录用户ID
        UserDTO dto = UserHolder.getUser();
        if (dto == null) {
            return Result.fail("请先登录");
        }
        Long userId = dto.getId();

        // 2. 使用 Set 求博主和当前登录用户关注的共同用户
        String bloggerKey = "follow:" + id;
        String userKey = "follow:" + userId;
        Set<String> intersect = stringRedisTemplate.opsForSet().intersect(bloggerKey, userKey);
        if (intersect == null || intersect.isEmpty()) {
            return Result.ok(Collections.emptyList());
        }
        List<Long> idList = intersect.stream().map(Long::valueOf).collect(Collectors.toList());
        List<UserDTO> dtoList = userService.listByIds(idList)
                .stream()
                .map(user -> BeanUtil.copyProperties(user, UserDTO.class))
                .collect(Collectors.toList());
        return Result.ok(dtoList);
    }
}
