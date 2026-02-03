package com.example.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.dto.Result;
import com.example.entity.UserInfo;
import com.example.mapper.UserInfoMapper;
import com.example.service.IUserInfoService;
import org.springframework.stereotype.Service;

@Service
public class UserInfoServiceImpl extends ServiceImpl<UserInfoMapper, UserInfo> implements IUserInfoService {
    @Override
    public UserInfo getByUserId(Long userId) {
        return null;
    }

    @Override
    public Result<Void> updateUserLevel(Long userId, Integer newLevel) {
        return null;
    }
}
