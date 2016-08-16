package com.demo.bencode;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;

/**
 * Created by bkmr38 on 8/16/2016.
 */
public class BencodeList extends ArrayList<BencodeType> implements BencodeType{

    public static BencodeList getList(String source, int index) {
        char c = source.charAt(index++);
        if(c == 'l') {
            BencodeList result = new BencodeList();
            for(;;) {
                int temp = index;
                index += result.addElement(BencodeString.getString(source, index));
                index += result.addElement(BencodeInteger.getInt(source, index));
                index += result.addElement(BencodeList.getList(source, index));
                index += result.addElement(BencodeMap.getMap(source, index));
                if(index != temp) {
                    continue;
                }
                if(source.charAt(index) == 'e') {
                    break;
                } else {
                    throw new RuntimeException();
                }
            }
            return result;
        }
        return null;
    }

    public int getLength() {
        int length = 0;
        for(BencodeType element : this) {
            length += element.getTotalLength();
        }
        return length;
    }

    public int getTotalLength() {
        return getLength() + 2;
    }

    public byte[] getData() {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        try {
            for(BencodeType element : this) {
                baos.write(element.getTotalData());
            }
        } catch (Exception e) {
        }
        return baos.toByteArray();
    }

    public byte[] getTotalData() {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            baos.write((byte)'l');
            baos.write(getData());
            baos.write((byte)'e');
        } catch (IOException e) {
        }
        return baos.toByteArray();
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        for(BencodeType element : this) {
            sb.append(", ");
            sb.append(element.toString());
        }
        return sb.length() > 0 ? "[" + sb.toString().substring(2) + "]" : "[]";
    }

    private int addElement(BencodeType element) {
        if(null != element) {
            add(element);
            return element.getTotalLength();
        }
        return 0;
    }
}
