package com.hmdp.utils;

/**
 * @author weiqiang
 * @version 1.0
 * @Date 2026/4/8 20:35
 */


public interface ILock {

    /**
     *
     * 尝试获取锁
     * @param timeoutSec
     * @return boolean
     */
    boolean tryLock(long timeoutSec);

    /**
     * 释放锁
     */
    void unlock();

}
