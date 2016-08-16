package com.demo.bencode;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;

/**
 * Created by bkmr38 on 8/16/2016.
 */
public class BencodeInteger implements BencodeType{
    private final String content;

    public BencodeInteger(String content) {
        this.content = content;
    }

    public static BencodeInteger getInt(String source, int index) {
        char c = source.charAt(index);
        if(c == 'i') {
            source = source.substring(index + 1);
            return new BencodeInteger(source.substring(0, source.indexOf("e")));
        }
        return null;
    }

    public int getLength() {
        return content.length();
    }

    public int getTotalLength() {
        return getLength() + 2;
    }

    public byte[] getData() {
        try {
            return content.getBytes("iso-8859-1");
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

    public byte[] getTotalData() {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            baos.write((byte)'i');
            baos.write(getData());
            baos.write((byte)'e');
        } catch (IOException e) {
        }
        return baos.toByteArray();
    }

    public String toString() {
        return content;
    }
}
