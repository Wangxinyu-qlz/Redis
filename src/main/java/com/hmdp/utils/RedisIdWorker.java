package com.hmdp.utils;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

/**
 * @program: hm-dianping
 * @author: Qiaolezi
 * @create: 2024-08-27 11:24
 * @description: 基于redis的全局 id 生成器
 * ID的组成部分：
 * 符号位：1bit，永远为0
 *      如果timestamp的最大值在32位以内，即小于2^32，那么左移32位后，生成的ID的最高位将始终为0。
 *      2^32秒约等于136年（2^32 / (60 * 60 * 24 * 365) ≈ 136），
 *      所以只要你的BEGIN_TIMESTAMP是在可控范围内（例如当前时间戳或稍早的时间点），
 *      你可以确保在至少136年内，生成的ID的最高位为0。
 * 时间戳：31bit，以秒为单位，可以使用69年
 * 序列号：32bit，秒内的计数器，支持每秒产生2^32个不同ID
 **/
@Component
public class RedisIdWorker {
	//开始时间戳
	private static final long BEGIN_TIMESTAMP = 1724758394L;
	//序列号的位数
	private static final int COUNT_BITS = 32;

	private StringRedisTemplate stringRedisTemplate;

	public RedisIdWorker(StringRedisTemplate stringRedisTemplate) {
		this.stringRedisTemplate = stringRedisTemplate;
	}

	//根据给定的前缀生成id
	public long nextId(String keyPrefix) {
		//生成时间戳
		LocalDateTime now = LocalDateTime.now();
		long nowEpochSecond = now.toEpochSecond(ZoneOffset.UTC);
		long timestamp = nowEpochSecond - BEGIN_TIMESTAMP;

		//生成序列号
		//获取当前日期，精确到天
		//如果不设置天，会将所有value存储在一个key下，会生成大key
		//并且，设计的序列号只有32位，数据量大有可能溢出
		String date = now.format(DateTimeFormatter.ofPattern("yyyy:MM:dd"));
		Long increment = stringRedisTemplate.opsForValue().increment("icr:" + keyPrefix + ":" + date);

		//拼接并返回
		//时间戳原本是64位
		// 左移32位（高位溢出，低位补零），最右边32位全部是0
		// 或运算：有1是1，所以低32位的值全是原来的序列号
		return timestamp << COUNT_BITS | increment;
	}

	public static void main(String[] args) {
		//1724758394
		System.out.println(LocalDateTime.now().toEpochSecond(ZoneOffset.UTC));
	}
}
