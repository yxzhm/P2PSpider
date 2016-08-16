package com.demo.bencode;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Map;
import java.util.TreeMap;

/**
 * Created by bkmr38 on 8/16/2016.
 */
public class BencodeMap extends TreeMap<BencodeString, BencodeType> implements BencodeType{
    public static BencodeMap getMap(String source, int index) {
        char c = source.charAt(index++);
        if(c == 'd') {
            BencodeMap result = new BencodeMap();
            BencodeString key = null;
            for(;;) {
                BencodeType element;
                if(null != (element = BencodeString.getString(source, index)) ||
                        null != (element = BencodeInteger.getInt(source, index)) ||
                        null != (element = BencodeList.getList(source, index)) ||
                        null != (element = BencodeMap.getMap(source, index))) {
                    if(null != key) {
                        result.put(key, element);
                        key = null;
                    } else {
                        key = (BencodeString) element;
                    }
                    index += element.getTotalLength();
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
        for(Map.Entry<BencodeString, BencodeType> entry : entrySet()) {
            length += entry.getKey().getTotalLength();
            length += entry.getValue().getTotalLength();
        }
        return length;
    }

    public int getTotalLength() {
        return getLength() + 2;
    }

    public byte[] getData() {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        try {
            for(Map.Entry<BencodeString, BencodeType> entry : entrySet()) {
                baos.write(entry.getKey().getTotalData());
                baos.write(entry.getValue().getTotalData());
            }
        } catch (Exception e) {
        }
        return baos.toByteArray();
    }

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

    public String toString() {
        StringBuilder sb = new StringBuilder();
        for(Map.Entry<BencodeString, BencodeType> entry : entrySet()) {
            sb.append(", ");
            sb.append(entry.getKey().toString());
            sb.append(" : ");
            sb.append(entry.getValue().toString());
        }
        return sb.length() > 0 ? "{" + sb.toString().substring(2) + "}" : "{}";
    }
}
