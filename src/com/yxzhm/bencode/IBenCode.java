package com.yxzhm.bencode;

/**
 * Created by bkmr38 on 8/17/2016.
 */
public interface IBenCode {
    byte[] getData();
    byte[] getTotalData();
    int getLength();
    int getTotalLength();
}
