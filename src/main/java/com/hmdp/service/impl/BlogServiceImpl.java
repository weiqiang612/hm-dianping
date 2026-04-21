package com.hmdp.service.impl;

import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hmdp.dto.Result;
import com.hmdp.dto.ScrollResult;
import com.hmdp.dto.UserDTO;
import com.hmdp.entity.Blog;
import com.hmdp.entity.User;
import com.hmdp.mapper.BlogMapper;
import com.hmdp.service.IBlogService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.service.IFollowService;
import com.hmdp.service.IUserService;
import com.hmdp.utils.SystemConstants;
import com.hmdp.utils.UserHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;

import javax.swing.*;
import java.util.*;
import java.util.stream.Collectors;

import static com.hmdp.utils.RedisConstants.BLOG_LIKED_KEY;
import static com.hmdp.utils.RedisConstants.FEED_KEY;

/**
 * <p>
 * 服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
public class BlogServiceImpl extends ServiceImpl<BlogMapper, Blog> implements IBlogService {

    @Autowired
    private IUserService userService;

    @Autowired
    private BlogMapper blogMapper;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    private IFollowService followService;

    @Override
    public Result queryHotBlog(Integer current) {

        // 根据用户查询
        Page<Blog> page = query()
                .orderByDesc("liked")
                .page(new Page<>(current, SystemConstants.MAX_PAGE_SIZE));
        // 获取当前页数据
        List<Blog> records = page.getRecords();
        records.forEach(blog -> {
            // 查询用户
            this.queryBlogUser(blog);
            // 判断当前登录用户是否点赞过，赋值给 isLike 字段
            this.isBlogLiked(blog);
        });

        return Result.ok(records);
    }

    @Override
    public Result getBlogById(Integer id) {
        // 1. 修改根据 id 查询 Blog 的业务，查询 Blog 时顺便查询出 Blog 对应的用户信息
        Blog blog = blogMapper.selectById(id);
        if (blog == null) {
            return Result.fail("博客不存在！");
        }
        queryBlogUser(blog);
        isBlogLiked(blog);
        return Result.ok(blog);
    }

    /**
     *
     * 判断博客是否被当前用户点赞过
     *
     * @param blog
     * @return {@link Boolean }
     */
    private void isBlogLiked(Blog blog) {
        UserDTO user = UserHolder.getUser();
        if (user == null) {
            // 用户未登录，无需查询是否点赞
            return;
        }
        Long userId = user.getId();
        String key = BLOG_LIKED_KEY + blog.getId();
        Boolean isMember = stringRedisTemplate.opsForZSet().score(key, userId.toString()) != null;
        // 点赞过设为 true 未点赞过设置为 false
        blog.setIsLike(BooleanUtil.isTrue(isMember));
    }

    @Override
    public void likeBlog(Long id) {
        // 实现点赞逻辑
        // 1. 修改点赞功能，利用 Redis 的 set 集合判断是否点赞过，未点赞过则点赞数+1，将用户放到set集合中 ，已点赞过则点赞数 -1，将用户从set集合中取出
        Long userId = UserHolder.getUser().getId();

        String key = BLOG_LIKED_KEY + id;
        boolean liked = stringRedisTemplate.opsForZSet().score(key, userId.toString()) != null;

        // 2. 如果未点赞过，则点赞数+1，将用户放到set集合中
        if (!liked) {
            // 更改数据库
            boolean success = update().setSql("liked = liked + 1").eq("id", id).update();
            // 更改 Redis
            if (success) {
                stringRedisTemplate.opsForZSet().add(key, userId.toString(), System.currentTimeMillis());
            }
        } else {
            // 3. 如果点赞过，则点赞数 -1，将用户从set集合中取出
            boolean success = update().setSql("liked = liked - 1").eq("id", id).update();
            if (success) {
                stringRedisTemplate.opsForZSet().remove(key, userId.toString());
            }
        }
    }

    /**
     *
     * 查看博客点赞排名
     *
     * @param id
     * @return {@link Result }
     */
    @Override
    public Result queryBlogLikesTop5(Long id) {
        String key = BLOG_LIKED_KEY + id;
        // 1. 查询 Redis 中 zset 集合，查询点赞数前5的用户 id
        // Spring Data Redis 默认返回的是一个 LinkedHashSet,保证了插入顺序
        Set<String> top5 = stringRedisTemplate.opsForZSet().range(key, 0, 4);
        if (top5 == null || top5.isEmpty()) {
            return Result.ok(Collections.EMPTY_LIST);
        }
        // 2. 根据用户 id 查询用户信息
        List<Long> ids = top5.stream().map(Long::valueOf).collect(Collectors.toList());
        List<UserDTO> dtoList = userService.query().in("id", ids).
                last("ORDER BY FIELD(id," + StrUtil.join(",", ids) + ")")
                .list().stream()
                .map(user -> {
                    UserDTO userDTO = new UserDTO();
                    userDTO.setId(user.getId());
                    userDTO.setIcon(user.getIcon());
                    userDTO.setNickName(user.getNickName());
                    return userDTO;
                }).collect(Collectors.toList());
        return Result.ok(dtoList);
    }

    @Override
    public Result saveBlog(Blog blog) {
        // 1. 获取登录用户
        UserDTO user = UserHolder.getUser();
        Long userId = user.getId();
        blog.setUserId(userId);
        // 2. 保存探店博文
        boolean saved = save(blog);
        if (!saved) {
            return Result.fail("保存博客失败！");
        }
        // 3. 推送到粉丝的收件箱
        // 3.1 查询有哪些粉丝
        followService.query().eq("follow_user_id", userId).list().forEach(follow -> {
            // 3.2 推送blogId到粉丝收件箱
            String followerKey = FEED_KEY + follow.getUserId();
            stringRedisTemplate.opsForZSet().add(followerKey, blog.getId().toString(), System.currentTimeMillis());
        });
        // 返回ID
        return Result.ok(blog.getId());
    }

    @Override
    public Result queryBlogOfFollow(Long max, Integer offset) {
        // 1. 获取当前用户
        UserDTO dto = UserHolder.getUser();
        if (dto == null) {
            return Result.fail("请先登录！");
        }
        Long userId = dto.getId();
        // 2. 查询收件箱
        String key = FEED_KEY + userId;
        // 3. ZREVRANGEBYSCORE key max 0 WITHSCORES LIMIT offset 3
        Set<ZSetOperations.TypedTuple<String>> typedTuples = stringRedisTemplate.opsForZSet().reverseRangeByScoreWithScores(key, 0, max, offset, 2);
        if (typedTuples == null || typedTuples.isEmpty()) {
            return Result.ok();
        }
        // 4. 解析数据 blogId 时间戳
        List<Long> blogs = new ArrayList<>(typedTuples.size());
        long minTime = 0L;
        int offsetIndex = 1;
        for (ZSetOperations.TypedTuple<String> typedTuple : typedTuples) {
            blogs.add(Long.valueOf(typedTuple.getValue()));
            // 时间戳
            long time = typedTuple.getScore().longValue();
            if (minTime == time) {
                // 如果和最小值时间戳重复，偏移量 + 1
                offsetIndex++;
            } else {
                // 不一样说明当前时间戳不是最小值，偏移量重新数
                minTime = time;
                offsetIndex = 1;
            }
        }
        if (minTime == max){
            offsetIndex += offset;
        }
        // 5. 根据获得的 blogId 查询博客
        String idStr = StrUtil.join(",", blogs);
        List<Blog> blogList = query().in("id", blogs).last("ORDER BY FIELD(id," + idStr + ")").list();

        blogList.forEach(blog -> {
            // 5.1 查询博客用户信息
            queryBlogUser(blog);
            // 5.2 查询博客是否被点赞
            isBlogLiked(blog);
        });

        // 6. 封装结果返回
        ScrollResult scrollResult = new ScrollResult();
        scrollResult.setOffset(offsetIndex);
        scrollResult.setList(blogList);
        scrollResult.setMinTime(minTime);
        return Result.ok(scrollResult);
    }

    private void queryBlogUser(Blog blog) {
        Long userId = blog.getUserId();
        User user = userService.getById(userId);
        blog.setName(user.getNickName());
        blog.setIcon(user.getIcon());
    }
}
