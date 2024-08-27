package com.hmdp.entity;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * @program: hm-dianping
 * @author: Qiaolezi
 * @create: 2024-08-27 10:35
 * @description:
 **/
@Data
public class RedisData {
	private LocalDateTime expireTime;
	private Object data;
}
