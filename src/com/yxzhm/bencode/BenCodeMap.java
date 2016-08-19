package com.yxzhm.bencode;

import com.demo.bencode.BencodeString;
import com.demo.bencode.BencodeType;
import com.yxzhm.util.ExceptionHandler;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Map;
import java.util.TreeMap;

/**
 * Created by bkmr38 on 8/17/2016.
 */
public class BenCodeMap extends TreeMap<BencodeString, BencodeType> implements BencodeType{

    private final int PostfixLen=2;

    @Override
    public byte[] getData() {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        try {
            for(Map.Entry<BencodeString, BencodeType> entry : entrySet()) {
                baos.write(entry.getKey().getTotalData());
                baos.write(entry.getValue().getTotalData());
            }
        } catch (Exception e) {
            new ExceptionHandler(e).handle();
        }

        return baos.toByteArray();
    }

    @Override
    public byte[] getTotalData() {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            baos.write((byte)'d');
            baos.write(getData());
            baos.write((byte)'e');
        } catch (IOException e) {
        }
        return baos.toByteArray();
    }

    @Override
    public int getLength() {
        int length = 0;
        for(Map.Entry<BencodeString, BencodeType> entry : entrySet()) {
            length += entry.getKey().getTotalLength();
            length += entry.getValue().getTotalLength();
        }
        return length;
    }

    @Override
    public int getTotalLength() {
        return getLength() + PostfixLen;
    }

}
