package com.example.creditpromotion.util;

import org.springframework.stereotype.Component;

/**
 * 雪花算法ID生成器
 */
@Component
public class SnowflakeIdGenerator {
    
    // 开始时间戳 (2020-01-01)
    private final long startTimestamp = 1577808000000L;
    
    // 机器ID所占位数
    private final long workerIdBits = 5L;
    
    // 数据中心ID所占位数
    private final long datacenterIdBits = 5L;
    
    // 支持的最大机器ID
    private final long maxWorkerId = ~(-1L << workerIdBits);
    
    // 支持的最大数据中心ID
    private final long maxDatacenterId = ~(-1L << datacenterIdBits);
    
    // 序列号所占位数
    private final long sequenceBits = 12L;
    
    // 机器ID左移位数
    private final long workerIdShift = sequenceBits;
    
    // 数据中心ID左移位数
    private final long datacenterIdShift = sequenceBits + workerIdBits;
    
    // 时间戳左移位数
    private final long timestampLeftShift = sequenceBits + workerIdBits + datacenterIdBits;
    
    // 序列号掩码
    private final long sequenceMask = ~(-1L << sequenceBits);
    
    // 工作机器ID
    private long workerId;
    
    // 数据中心ID
    private long datacenterId;
    
    // 序列号
    private long sequence = 0L;
    
    // 上次生成ID的时间戳
    private long lastTimestamp = -1L;
    
    public SnowflakeIdGenerator() {
        this(1, 1); // 默认值
    }
    
    public SnowflakeIdGenerator(long workerId, long datacenterId) {
        if (workerId > maxWorkerId || workerId < 0) {
            throw new IllegalArgumentException("Worker ID can't be greater than " + maxWorkerId + " or less than 0");
        }
        if (datacenterId > maxDatacenterId || datacenterId < 0) {
            throw new IllegalArgumentException("Datacenter ID can't be greater than " + maxDatacenterId + " or less than 0");
        }
        this.workerId = workerId;
        this.datacenterId = datacenterId;
    }
    
    /**
     * 获取下一个ID
     */
    public synchronized long nextId() {
        long timestamp = getCurrentTimestamp();
        
        // 如果当前时间小于上一次ID生成的时间戳，说明系统时钟回退过，抛出异常
        if (timestamp < lastTimestamp) {
            throw new RuntimeException("Clock moved backwards. Refusing to generate id for " + (lastTimestamp - timestamp) + " milliseconds");
        }
        
        // 如果是同一时间生成的，则进行毫秒内序列
        if (lastTimestamp == timestamp) {
            sequence = (sequence + 1) & sequenceMask;
            // 毫秒内序列溢出
            if (sequence == 0) {
                // 阻塞到下一个毫秒，获得新的时间戳
                timestamp = waitNextMillis(lastTimestamp);
            }
        } 
        // 时间戳改变，毫秒内序列重置
        else {
            sequence = 0L;
        }
        
        // 上次生成ID的时间戳
        lastTimestamp = timestamp;
        
        // 移位并通过或运算拼到一起组成64位的ID
        return ((timestamp - startTimestamp) << timestampLeftShift)
                | (datacenterId << datacenterIdShift)
                | (workerId << workerIdShift)
                | sequence;
    }
    
    /**
     * 阻塞到下一个毫秒，直到获得新的时间戳
     */
    private long waitNextMillis(long lastTimestamp) {
        long timestamp = getCurrentTimestamp();
        while (timestamp <= lastTimestamp) {
            timestamp = getCurrentTimestamp();
        }
        return timestamp;
    }
    
    /**
     * 获取当前时间戳
     */
    private long getCurrentTimestamp() {
        return System.currentTimeMillis();
    }
}