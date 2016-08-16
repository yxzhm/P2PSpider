package com.demo;

import com.demo.bencode.BencodeMap;

/**
 * Created by bkmr38 on 8/16/2016.
 */
public abstract class Callbacker {
    private int transactionId;

    public abstract void execute(BencodeMap response);
    public void destory() {}

    public final void setTransactionId(int transactionId) {
        this.transactionId = transactionId;
    }

    public final int getTransactionId() {
        return transactionId;
    }
}
