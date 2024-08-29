package com.hmdp.utils.transaction;

import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * @program: hm-dianping
 * @author: Qiaolezi
 * @create: 2024-08-29 10:57
 * @description:
 **/
public class TransactionUtil {
	public static void doAfterTransact(Runnable runnable) {
		//判断上下文有没有事务激活，如果有，就把DoTransactionCompletion注册进去
		TransactionSynchronizationManager.
				registerSynchronization(new DoTransactionCompletion(runnable));
	}
}
