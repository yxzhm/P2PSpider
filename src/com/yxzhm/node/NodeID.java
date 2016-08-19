package com.yxzhm.node;


import com.yxzhm.bencode.BenCodeString;

import java.math.BigInteger;

/**
 * Created by bkmr38 on 8/17/2016.
 */
public class NodeID {

    private final BigInteger value;

    public NodeID(byte[] bs) {
        if(bs.length != 20) {
            throw new RuntimeException();
        }

        byte[] dest = new byte[bs.length + 1];
        dest[0] = 0;
        System.arraycopy(bs, 0, dest, 1, bs.length);
        value = new BigInteger(dest);
    }

    public BigInteger getValue() {
        return value;
    }
}
