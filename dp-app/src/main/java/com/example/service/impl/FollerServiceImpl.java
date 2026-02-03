package com.example.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.dto.Result;
import com.example.dto.UserDTO;
import com.example.entity.Follow;
import com.example.impl.SnowflakeIdGenerator;
import com.example.mapper.FollowMapper;
import com.example.service.IFollowService;
import com.example.service.IUserService;
import com.example.utils.RedisConstants;
import com.example.utils.UserHolder;
import jakarta.annotation.Resource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Set;

@Service
public class FollerServiceImpl extends ServiceImpl<FollowMapper, Follow> implements IFollowService {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private SnowflakeIdGenerator snowflakeIdGenerator;

    @Resource
    private IUserService userService;

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
            boolean isSuccess = remove(new QueryWrapper<Follow>()
                    .eq("user_id", userId).eq("follow_user_id", followUserId));
            if (isSuccess) {
                // 把关注用户的id从Redis集合中移除
                stringRedisTemplate.opsForSet().remove(key, followUserId.toString());
            }
        }
        return Result.ok();
    }

    @Override
    public Result<Boolean> isFollow(Long followUserId) {
        Long userId = UserHolder.getUser().getId();
        return lambdaQuery().eq(Follow::getUserId, userId)
                .eq(Follow::getFollowUserId, followUserId).count() > 0 ?
                Result.ok(true) : Result.ok(false);
    }

    @Override
    public Result<List<UserDTO>> followCommons(Long id) {
        Long userId = UserHolder.getUser().getId();
        String myKey = RedisConstants.FOLLOWER_KEY + userId;
        String otherKey = RedisConstants.FOLLOWER_KEY + id;
        // 1. Set 求关注交集
        Set<String> intersect = stringRedisTemplate.opsForSet().intersect(myKey, otherKey);
        if (intersect == null || intersect.isEmpty()) {
            return Result.ok(Collections.emptyList());
        }
        List<Long> commonIds = intersect.stream().map(Long::valueOf).toList();
        // 2. 根据 id 查询关注者信息
        List<UserDTO> users = userService.listByIds(commonIds).stream().map(user -> {
            UserDTO userDTO = new UserDTO();
            userDTO.setId(user.getId());
            userDTO.setIcon(user.getIcon());
            userDTO.setNickName(user.getNickName());
            return userDTO;
        }).toList();
        return Result.ok(users);
    }
}
