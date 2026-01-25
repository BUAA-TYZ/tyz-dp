package com.example.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.dto.Result;
import com.example.entity.Voucher;

import java.util.List;

public interface IVoucherService extends IService<Voucher> {

        Result<List<Voucher>> queryVoucherOfShop(Long shopId);

        void addSeckillVoucher(Voucher voucher);
}
