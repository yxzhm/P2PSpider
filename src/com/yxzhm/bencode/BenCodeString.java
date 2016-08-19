package com.yxzhm.bencode;

import com.demo.bencode.BencodeType;
import com.yxzhm.util.Config;
import com.yxzhm.util.ExceptionHandler;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;

/**
 * Created by bkmr38 on 8/17/2016.
 */
public class BenCodeString implements BencodeType {

    private String content="";

    public BenCodeString(String content) {
        this.content = content;
    }

    public BenCodeString(byte[] bs) {
        try {
            this.content = new String(bs, Config.getDefaultEnCodeType());
        } catch (UnsupportedEncodingException e) {
            new ExceptionHandler(e).handle();
        }
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
            baos.write(String.valueOf(getLength()).getBytes(Config.getDefaultEnCodeType()));
            baos.write((byte)':');
            baos.write(getData());
        } catch (IOException e) {
        }
        return baos.toByteArray();
    }

    @Override
    public int getLength() {
        return content.length();
    }

    @Override
    public int getTotalLength() {
        return getLength() + String.valueOf(getLength()).length() + 1;
    }
}
