package com.example.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.dto.Result;
import com.example.entity.UserInfo;
import com.example.mapper.UserInfoMapper;
import com.example.service.IUserInfoService;
import com.example.utils.RedisConstants;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class UserInfoServiceImpl extends ServiceImpl<UserInfoMapper, UserInfo> implements IUserInfoService {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public UserInfo getByUserId(Long userId) {
        // 1. 缓存 UserInfo
        String key = RedisConstants.USER_INFO_KEY + userId;
        String userInfoJson = stringRedisTemplate.opsForValue().get(key);
        UserInfo userInfo;
        if (StrUtil.isNotBlank(userInfoJson)) {
            userInfo = JSONUtil.toBean(userInfoJson, UserInfo.class);
            if (userInfo == null) {
                log.warn("缓存的用户信息反序列化失败，userId={}", userId);
            }
            return userInfo;
        }

        // 2. 数据库查询 UserInfo
        userInfo = lambdaQuery().eq(UserInfo::getUserId, userId).one();
        if (userInfo == null) {
            log.warn("用户信息不存在，userId={}", userId);
            return null;
        }
        // 3. 将 UserInfo 写入缓存
        stringRedisTemplate.opsForValue().set(key, JSONUtil.toJsonStr(userInfo));
        return userInfo;
    }

    @Override
    public Result<Void> updateUserLevel(Long userId, Integer newLevel) {
        return null;
    }
}
