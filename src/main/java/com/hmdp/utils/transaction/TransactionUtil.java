package com.hmdp.utils.transaction;

import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * @program: hm-dianping
 * @author: Qiaolezi
 * @create: 2024-08-29 10:57
 * @description:
 **/
public class TransactionUtil {
    public static void doAfterTransact(Runnable onSuccess, Runnable onFailure) {
        // 判断上下文有没有事务激活，如果有，就注册 DoTransactionCompletion
        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            TransactionSynchronizationManager.registerSynchronization(
                    new DoTransactionCompletion(onSuccess, onFailure)
            );
        } else {
            // 如果没有事务活动，可以直接执行操作或抛出异常
            if (onFailure != null) {
                onFailure.run();
            }
        }
    }
}
