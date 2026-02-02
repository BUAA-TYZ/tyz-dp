package com.example.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.dto.Result;
import com.example.entity.VoucherOrder;

public interface IVoucherOrderService extends IService<VoucherOrder> {

    /**
     * 秒杀优惠券
     *
     * @param voucherId 券id
     * @return {@link Result}
     */
    Result<Long> seckillVoucher(Long voucherId);

    /**
     * 得到结果
     *
     * @param voucherId 券id
     * @return {@link Result}
     */
    Result getResult(Long voucherId);

    /**
     * 创建优惠券订单
     *
     * @param voucherOrder 券订单
     */
    void createVoucherOrder(VoucherOrder voucherOrder);
}