package com.demo.bencode;

/**
 * Created by bkmr38 on 8/16/2016.
 */
public interface BencodeType {
    int getLength();
    int getTotalLength();
    byte[] getData();
    byte[] getTotalData();
}
