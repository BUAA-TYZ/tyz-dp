package com.example.service.impl;

import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.dto.Result;
import com.example.entity.SeckillVoucher;
import com.example.entity.VoucherOrder;
import com.example.impl.SnowflakeIdGenerator;
import com.example.mapper.VoucherOrderMapper;
import com.example.service.ISeckillVoucherService;
import com.example.service.IVoucherOrderService;
import com.example.utils.UserHolder;
import jakarta.annotation.Resource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;

public class VoucherOrderServiceImpl extends ServiceImpl<VoucherOrderMapper, VoucherOrder> implements IVoucherOrderService {

    @Resource
    private SnowflakeIdGenerator snowflakeIdGenerator;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private ISeckillVoucherService seckillVoucherService;

    private static final DefaultRedisScript<Long> SECKILL_SCRIPT;

    static {
        SECKILL_SCRIPT = new DefaultRedisScript<>();
        SECKILL_SCRIPT.setLocation(new ClassPathResource("seckill.lua"));
        SECKILL_SCRIPT.setResultType(Long.class);
    }

    @Override
    public Result<Long> seckillVoucher(Long voucherId) {
        Long userId = UserHolder.getUser().getId();

        // 1. 执行 lua 脚本, 判断库存/一人一单
        Long res = stringRedisTemplate.execute(SECKILL_SCRIPT, Collections.emptyList(),
                voucherId, userId);
        int r = res.intValue();
        if (r == 1){
            return Result.fail("库存不足");
        } else if (r == 2) {
            return Result.fail("一人一单");
        }

        // 2. 生成订单 雪花ID
        VoucherOrder seckillVoucherOrder = new VoucherOrder();
        Long orderId = snowflakeIdGenerator.nextId();
        seckillVoucherOrder.setId(orderId);
        seckillVoucherOrder.setUserId(userId);
        seckillVoucherOrder.setVoucherId(voucherId);

        // 3. 订单写入 Rocketmq

        return Result.ok(orderId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result getResult(Long voucherId) {
        // 1. 一人一单兜底检验
        Long userId = UserHolder.getUser().getId();
        Long cnt = query().eq("user_id", userId)
                .eq("voucher_id", voucherId)
                .count();
        if (cnt > 0) {
            return Result.fail("一人一单");
        }

        // 2. 扣减库存
        boolean isSuccess = seckillVoucherService.update(
                new UpdateWrapper<SeckillVoucher>()
                        .set("voucher_id", voucherId)
                        .gt("stock", 0)
                        .setSql("stock = stock - 1"));
        if (!isSuccess) {
            return Result.fail("库存不足");
        }
        VoucherOrder voucherOrder = new VoucherOrder();
        voucherOrder.setVoucherId(voucherId);
        voucherOrder.setUserId(userId);
        long orderId = snowflakeIdGenerator.nextId();
        voucherOrder.setId(orderId);
        save(voucherOrder);
        return Result.ok(orderId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createVoucherOrder(VoucherOrder voucherOrder) {

    }
}
