package com.example.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.dto.Result;
import com.example.dto.ScrollResult;
import com.example.dto.UserDTO;
import com.example.entity.Blog;
import com.example.entity.Follow;
import com.example.entity.User;
import com.example.impl.SnowflakeIdGenerator;
import com.example.mapper.BlogMapper;
import com.example.service.IBlogService;
import com.example.service.IFollowService;
import com.example.service.IUserService;
import com.example.utils.RedisConstants;
import com.example.utils.SystemConstants;
import com.example.utils.UserHolder;
import jakarta.annotation.Resource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

public class BlogServiceImpl extends ServiceImpl<BlogMapper, Blog> implements IBlogService {

    @Resource
    private IUserService userService;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private IFollowService followService;

    @Resource
    private SnowflakeIdGenerator snowflakeIdGenerator;

    @Override
    public Result<List<Blog>> queryHotBlog(Integer current) {
        Page<Blog> pages = query()
                .orderByDesc("liked")
                .page(new Page<>(current, SystemConstants.MAX_PAGE_SIZE));
        List<Blog> records = pages.getRecords();
        records.forEach(blog -> {
            // 填充用户
            queryBlogUser(blog);
            // 是否点赞
            isBlogLiked(blog);
        });
        return Result.ok(records);
    }

    @Override
    public Result<Blog> queryBlogById(Long id) {
        Blog blog = getById(id);
        if (blog == null) {
            return Result.fail("博客不存在");
        }
        queryBlogUser(blog);
        isBlogLiked(blog);
        return Result.ok(blog);
    }

    @Override
    public Result likeBlog(Long id) {
        // 1. 获取用户, 判断是否点赞
        Long userId = UserHolder.getUser().getId();
        String key = RedisConstants.BLOG_LIKED_KEY + id;
        Double score = stringRedisTemplate.opsForZSet().score(key, userId.toString());
        // 2. 已点赞则取消, 反之则点赞
        if (score != null) {
            boolean isSuccess = update().eq("id", id).setSql("liked = liked - 1").update();
            if (isSuccess) {
                stringRedisTemplate.opsForZSet().remove(key, userId.toString());
            }
        } else {
            boolean isSuccess = update().eq("id", id).setSql("liked = liked + 1").update();
            if (isSuccess) {
                stringRedisTemplate.opsForZSet().add(key, userId.toString(), System.currentTimeMillis());
            }
        }
        return Result.ok();
    }

    @Override
    public Result<List<UserDTO>> queryBlogLikes(Long id) {
        String key = RedisConstants.BLOG_LIKED_KEY + id;
        Set<String> top5 = stringRedisTemplate.opsForZSet().range(key, 0, 6);
        if (top5 == null || top5.isEmpty()) {
            return Result.ok(Collections.emptyList());
        }
        List<Long> userIds = top5.stream().map(Long::valueOf).toList();
        String join = StrUtil.join(",", userIds);
        List<UserDTO> userDTOs = userService.query()
                .in("id", userIds)
                .last("ORDER BY FIELD(id," + join + ")")
                .list()
                .stream()
                .map(user ->
                    BeanUtil.copyProperties(user, UserDTO.class)
                ).toList();
        return Result.ok(userDTOs);
    }

    @Override
    public Result saveBlog(Blog blog) {
        Long userId = UserHolder.getUser().getId();
        blog.setId(snowflakeIdGenerator.nextId());
        blog.setUserId(userId);
        boolean isSuccess = save(blog);
        if (!isSuccess) {
            return Result.fail("新增笔记失败");
        }
        followService.lambdaQuery()
                .eq(Follow::getFollowUserId, userId)
                .list()
                .forEach(follow -> {
                    // 推送笔记id给粉丝
                    String key = RedisConstants.FEED_KEY + follow.getUserId();
                    stringRedisTemplate.opsForZSet().add(key, blog.getId().toString(), System.currentTimeMillis());
                });
        return Result.ok(blog.getId());
    }

    @Override
    public Result<ScrollResult> queryBlogOfFollow(Long max, Integer offset) {
        Long userId = UserHolder.getUser().getId();
        String key = RedisConstants.FEED_KEY + userId;
        Set<ZSetOperations.TypedTuple<String>> typedTuples =
                stringRedisTemplate.opsForZSet().reverseRangeByScoreWithScores(key, 0, max, offset, 2);
        if (typedTuples == null || typedTuples.isEmpty()) {
            return Result.ok();
        }
        ArrayList<Long> ids = new ArrayList<>(typedTuples.size());
        long minTime = 0;
        int os = 1;
        for (ZSetOperations.TypedTuple<String> tuple : typedTuples) {
            ids.add(Long.valueOf(tuple.getValue()));
            long time = tuple.getScore().longValue();
            if (time == minTime) {
                os++;
            } else {
                minTime = time;
                os = 1;
            }
        }
        String idStr = StrUtil.join(",", ids);
        List<Blog> blogs = query().in("id", ids).last("ORDER BY FIELD(id," + idStr + ")").list();
        for (Blog blog : blogs) {
            queryBlogUser(blog);
            isBlogLiked(blog);
        }
        ScrollResult res = new ScrollResult();
        res.setList(blogs);
        res.setOffset(os);
        res.setMinTime(minTime);
        return Result.ok(res);
    }

    /**
     * @description: 填充 blog user 信息
     */
    private void queryBlogUser(Blog blog) {
        Long userId = blog.getUserId();
        User user = userService.getById(userId);
        blog.setName(user.getNickName());
        blog.setIcon(user.getIcon());
    }

    /**
     * @description: 填充 blog 点赞 信息
     */
    private void isBlogLiked(Blog blog) {
        UserDTO userDtO = UserHolder.getUser();
        if (userDtO == null) {
            return;
        }
        Long userId = userDtO.getId();
        // 判断是否点赞
        String key = RedisConstants.BLOG_LIKED_KEY + blog.getId();
        Double score = stringRedisTemplate.opsForZSet().score(key, userId.toString());
        blog.setIsLike(score!=null);
    }
}
