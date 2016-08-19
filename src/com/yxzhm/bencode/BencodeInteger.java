package com.yxzhm.bencode;

import com.yxzhm.util.Config;
import com.yxzhm.util.ExceptionHandler;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;

/**
 * Created by bkmr38 on 8/17/2016.
 */
public class BenCodeInteger implements IBenCode {

    private String content;
    private final int PostfixLen=2;

    public BenCodeInteger(String content) {
        this.content = content;
    }

    @Override
    public byte[] getData() {
        try {
            return content.getBytes(Config.getDefaultEnCodeType());
        } catch (UnsupportedEncodingException e) {
            new ExceptionHandler(e).handle();
        }

        return new byte[0];
    }

    @Override
    public byte[] getTotalData() {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            baos.write((byte)'i');
            baos.write(getData());
            baos.write((byte)'e');
        } catch (IOException e) {
            new ExceptionHandler(e).handle();
        }
        return baos.toByteArray();
    }

    @Override
    public int getLength() {
        return content.length();
    }

    @Override
    public int getTotalLength() {
        return getLength()+PostfixLen;
    }


}
