package com.yxzhm.bencode;

import com.demo.bencode.BencodeType;
import com.yxzhm.util.ExceptionHandler;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;

/**
 * Created by bkmr38 on 8/17/2016.
 */
public class BenCodeList extends ArrayList<BencodeType> implements BencodeType {

    private final int PostfixLen=2;

    @Override
    public byte[] getData() {

        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        try {
            for(BencodeType element : this) {
                baos.write(element.getTotalData());
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
            baos.write((byte)'l');
            baos.write(getData());
            baos.write((byte)'e');
        } catch (IOException e) {
            new ExceptionHandler(e).handle();
        }
        return baos.toByteArray();
    }

    @Override
    public int getLength() {

        int length = 0;
        for(BencodeType element : this) {
            length += element.getTotalLength();
        }
        return length;
    }

    @Override
    public int getTotalLength() {
        
        return getLength() + PostfixLen;
    }
}
