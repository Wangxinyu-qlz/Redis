package com.hmdp.utils;

/**
 * @program: hm-dianping
 * @author: Qiaolezi
 * @create: 2024-08-28 23:02
 * @description:
 **/
public interface ILock {

	/**
	 * 尝试获取锁
	 * @param timeoutSec 锁持有的超时时间，过期自动释放
	 * @return
	 */
	boolean tryLock(long timeoutSec);

	/**
	 * 释放锁
	 */
	void unlock();
}
