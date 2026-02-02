package com.example.service.impl;

import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.dto.Result;
import com.example.entity.Follow;
import com.example.impl.SnowflakeIdGenerator;
import com.example.mapper.FollowMapper;
import com.example.service.IFollowService;
import com.example.utils.RedisConstants;
import com.example.utils.UserHolder;
import jakarta.annotation.Resource;
import org.springframework.data.redis.core.StringRedisTemplate;

public class FollerServiceImpl extends ServiceImpl<FollowMapper, Follow> implements IFollowService {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private SnowflakeIdGenerator snowflakeIdGenerator;

    @Override
    public Result follow(Long followUserId, Boolean isFollow) {
        Long userId = UserHolder.getUser().getId();
        String key = RedisConstants.FOLLOWER_KEY + userId;
        if (isFollow) {
            Follow follow = new Follow();
            follow.setId(snowflakeIdGenerator.nextId());
            follow.setUserId(userId);
            follow.setFollowUserId(followUserId);
            boolean isSuccess = save(follow);
            if (isSuccess) {
                stringRedisTemplate.opsForSet().add(key, followUserId.toString());
            }
        } else {
            remove(new LambdaQueryChainWrapper<Follow>()
                    .eq(Follow::getUserId, userId))
                    .eq(Follow::getFollowUserId, followUserId));
            stringRedisTemplate.opsForSet().
        }
        return null;
    }

    @Override
    public Result<Boolean> isFollow(Long followUserId) {
        Long userId = UserHolder.getUser().getId();
        return lambdaQuery().eq(Follow::getUserId, userId)
                .eq(Follow::getFollowUserId, followUserId).count() > 0 ?
                Result.ok(true) : Result.ok(false);
    }

    @Override
    public Result followCommons(Long id) {
        return null;
    }
}
