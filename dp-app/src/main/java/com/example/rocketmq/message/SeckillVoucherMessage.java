package com.example.rocketmq.message;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SeckillVoucherMessage {

    private Long userId;

    private Long voucherId;

    private Long orderId;

    private Long traceId;

    private Integer beforeQty;

    private Integer changeQty;

    private Integer afterQty;

    private Boolean autoIssue;
}