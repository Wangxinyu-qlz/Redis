package com.hmdp.utils.transaction;

import org.springframework.transaction.support.TransactionSynchronization;

/**
 * @program: hm-dianping
 * @author: Qiaolezi
 * @create: 2024-08-29 10:55
 * @description:
 **/
public class DoTransactionCompletion implements TransactionSynchronization {
	private Runnable onSuccess;
	private Runnable onFailure;

	public DoTransactionCompletion(Runnable onSuccess, Runnable onFailure) {
		this.onSuccess = onSuccess;
		this.onFailure = onFailure;
	}

	@Override
	public void afterCompletion(int status) {
		if (status == TransactionSynchronization.STATUS_COMMITTED) {
			if (onSuccess != null) {
				onSuccess.run();
			}
		} else if (status == TransactionSynchronization.STATUS_ROLLED_BACK) {
			if (onFailure != null) {
				onFailure.run();
			}
		}
	}
}
