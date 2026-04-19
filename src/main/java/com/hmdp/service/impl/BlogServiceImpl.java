package com.hmdp.service.impl;

import cn.hutool.core.util.BooleanUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hmdp.dto.Result;
import com.hmdp.dto.UserDTO;
import com.hmdp.entity.Blog;
import com.hmdp.entity.User;
import com.hmdp.mapper.BlogMapper;
import com.hmdp.service.IBlogService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.service.IUserService;
import com.hmdp.utils.SystemConstants;
import com.hmdp.utils.UserHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

import static com.hmdp.utils.RedisConstants.BLOG_LIKED_KEY;

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
        Boolean isMember = stringRedisTemplate.opsForSet().isMember(key, userId.toString());
        // 点赞过设为 true 未点赞过设置为 false
        blog.setIsLike(BooleanUtil.isTrue(isMember));
    }

    @Override
    public void likeBlog(Long id) {
        // 实现点赞逻辑
        // 1. 修改点赞功能，利用 Redis 的 set 集合判断是否点赞过，未点赞过则点赞数+1，将用户放到set集合中 ，已点赞过则点赞数 -1，将用户从set集合中取出
        Long userId = UserHolder.getUser().getId();

        String key = BLOG_LIKED_KEY + id;
        Boolean liked = stringRedisTemplate.opsForSet().isMember(key, userId.toString());

        // 2. 如果未点赞过，则点赞数+1，将用户放到set集合中
        if (!Boolean.TRUE.equals(liked)) {
            // 更改数据库
            boolean success = update().setSql("liked = liked + 1").eq("id", id).update();
            // 更改 Redis
            if (success) {
                stringRedisTemplate.opsForSet().add(key, userId.toString());
            }
        } else {
            // 3. 如果点赞过，则点赞数 -1，将用户从set集合中取出
            boolean success = update().setSql("liked = liked - 1").eq("id", id).update();
            if (success) {
                stringRedisTemplate.opsForSet().remove(key, userId.toString());
            }
        }
    }

    private void queryBlogUser(Blog blog) {
        Long userId = blog.getUserId();
        User user = userService.getById(userId);
        blog.setName(user.getNickName());
        blog.setIcon(user.getIcon());
    }
}
