package com.hmdp.utils.transaction;

import org.springframework.transaction.support.TransactionSynchronization;

/**
 * @program: hm-dianping
 * @author: Qiaolezi
 * @create: 2024-08-29 10:55
 * @description:
 **/
public class DoTransactionCompletion implements TransactionSynchronization {
	private Runnable runnable;

	public DoTransactionCompletion(Runnable runnable) {
		this.runnable = runnable;
	}

	@Override
	public void afterCompletion(int status) {
		//只有事务提交成功的时候，才做处理
		if(status == TransactionSynchronization.STATUS_COMMITTED) {
			this.runnable.run();
		}
	}
}
