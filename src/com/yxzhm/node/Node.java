package com.yxzhm.node;

import com.yxzhm.util.Config;
import com.yxzhm.util.ExceptionHandler;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.util.Arrays;

/**
 * Created by bkmr38 on 8/17/2016.
 */
public class Node {

    private NodeID id;
    private byte[] ip;
    private byte[] port;

    public Node(String ip, int port, String nodeId) {

        String[] ss = ip.split("\\.");
        this.ip = new byte[] {
                (byte) Integer.parseInt(ss[0]),
                (byte) Integer.parseInt(ss[1]),
                (byte) Integer.parseInt(ss[2]),
                (byte) Integer.parseInt(ss[3])
        };

        ByteBuffer bb = ByteBuffer.allocate(4).putInt(port);
        bb.flip();
        this.port = Arrays.copyOfRange(bb.array(), 2, 4);

        try {
            id = new NodeID(nodeId.getBytes(Config.getDefaultEnCodeType()));
        } catch (UnsupportedEncodingException e) {
            new ExceptionHandler(e).handle();
        }
    }

    public Node(byte[] bs) {
        if(bs.length != 26) {
            throw new RuntimeException();
        }
        id = new NodeID(Arrays.copyOfRange(bs, 0, 20));
        ip = Arrays.copyOfRange(bs, 20, 24);
        port = Arrays.copyOfRange(bs, 24, 26);
    }

    public NodeID getId() {
        return id;
    }

    public byte[] getIp() {
        return ip;
    }

    public byte[] getPort() {

        return port;
    }

    public boolean equals(Object obj) {
        if(obj instanceof Node) {
            Node n = (Node) obj;
            return id.getValue().equals(n.getId().getValue());
        }
        return false;
    }
}
