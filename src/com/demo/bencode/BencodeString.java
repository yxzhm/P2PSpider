package com.demo.bencode;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;

/**
 * Created by bkmr38 on 8/16/2016.
 */
public class BencodeString implements BencodeType, Comparable<BencodeString> {
    private final String content;

    public BencodeString(String content) {
        this.content = content;
    }

    public BencodeString(byte[] bs) {
        try {
            this.content = new String(bs, "iso-8859-1");
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

    public static BencodeString getString(String source, int index) {
        char c = source.charAt(index);
        if(c >= '1' && c <= '9') {
            source = source.substring(index);
            String lengthStr = source.split(":")[0];
            int length = Integer.parseInt(lengthStr);
            return new BencodeString(source.substring(lengthStr.length() + 1, lengthStr.length() + 1 + length));

        }
        return null;
    }

    public int getLength() {
        return content.length();
    }

    public int getTotalLength() {
        return getLength() + String.valueOf(getLength()).length() + 1;
    }

    public byte[] getData() {
        return getData("iso-8859-1");
    }

    public byte[] getData(String charsetName) {
        try {
            return content.getBytes(charsetName);
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

    public byte[] getTotalData() {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            baos.write(String.valueOf(getLength()).getBytes("iso-8859-1"));
            baos.write((byte)':');
            baos.write(getData());
        } catch (IOException e) {
        }
        return baos.toByteArray();
    }

    public String toString() {
        return "\"" + content + "\"";
    }

    public int compareTo(BencodeString o) {
        return this.content.compareTo(o.content);
    }
}
