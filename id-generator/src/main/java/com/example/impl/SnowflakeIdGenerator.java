package com.example.impl;

import com.example.api.IdGenerator;
import com.example.config.SnowflakeProperties;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

public final class SnowflakeIdGenerator implements IdGenerator {

    // ===== bit allocation =====
    private static final long WORKER_ID_BITS = 5L;       // 0~31
    private static final long DATACENTER_ID_BITS = 5L;   // 0~31
    private static final long SEQUENCE_BITS = 12L;       // 0~4095

    private static final long MAX_WORKER_ID = ~(-1L << WORKER_ID_BITS);           // 31
    private static final long MAX_DATACENTER_ID = ~(-1L << DATACENTER_ID_BITS);   // 31
    private static final long SEQUENCE_MASK = ~(-1L << SEQUENCE_BITS);            // 4095

    private static final long WORKER_ID_SHIFT = SEQUENCE_BITS;                              // 12
    private static final long DATACENTER_ID_SHIFT = SEQUENCE_BITS + WORKER_ID_BITS;         // 17
    private static final long TIMESTAMP_SHIFT = SEQUENCE_BITS + WORKER_ID_BITS + DATACENTER_ID_BITS; // 22

    // ===== config =====
    private final long workerId;
    private final long datacenterId;
    private final long epochMillis;

    // ===== state =====
    private long sequence = 0L;
    private long lastTimestamp = -1L;

    /**
     * @param datacenterId 0..31
     * @param workerId 0..31
     * @param epochMillis 自定义纪元毫秒
     */
    public SnowflakeIdGenerator(long datacenterId, long workerId, long epochMillis) {
        if (workerId < 0 || workerId > MAX_WORKER_ID) {
            throw new IllegalArgumentException("workerId out of range [0,31]: " + workerId);
        }
        if (datacenterId < 0 || datacenterId > MAX_DATACENTER_ID) {
            throw new IllegalArgumentException("datacenterId out of range [0,31]: " + datacenterId);
        }
        this.workerId = workerId;
        this.datacenterId = datacenterId;
        this.epochMillis = epochMillis;
    }

    /**
     * 线程安全生成 ID（synchronized）。
     * 时钟回拨策略：
     * - 回拨 <= 5ms：等待追平
     * - 回拨 > 5ms：直接抛异常（避免重复）
     */
    @Override
    public synchronized long nextId() {
        long ts = System.currentTimeMillis();

        if (ts < lastTimestamp) {
            long offset = lastTimestamp - ts;
            if (offset <= 5) {
                ts = waitUntil(lastTimestamp);
            } else {
                throw new IllegalStateException("Clock moved backwards by " + offset + "ms");
            }
        }

        if (ts == lastTimestamp) {
            sequence = (sequence + 1) & SEQUENCE_MASK;
            // 当前毫秒序列号用完
            if (sequence == 0) {
                ts = waitNextMillis(ts);
            }
        } else {
            sequence = 0L;
        }

        lastTimestamp = ts;

        long timePart = (ts - epochMillis) << TIMESTAMP_SHIFT;
        long dcPart = datacenterId << DATACENTER_ID_SHIFT;
        long workerPart = workerId << WORKER_ID_SHIFT;
        return timePart | dcPart | workerPart | sequence;
    }

    private long waitNextMillis(long lastTs) {
        long ts = System.currentTimeMillis();
        while (ts <= lastTs) {
            ts = System.currentTimeMillis();
        }
        return ts;
    }

    private long waitUntil(long targetTs) {
        long ts = System.currentTimeMillis();
        while (ts < targetTs) {
            ts = System.currentTimeMillis();
        }
        return ts;
    }
}