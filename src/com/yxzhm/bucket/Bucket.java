package com.yxzhm.bucket;

import com.yxzhm.node.Node;

import java.math.BigInteger;
import java.util.HashSet;
import java.util.Set;

/**
 * Created by bkmr38 on 8/17/2016.
 */
public class Bucket implements Comparable<Bucket> {

    private final int BucketMaxCount=8;
    private final String BucketMaxCountStr="8";

    private BigInteger start;
    private BigInteger end;
    private Set<Node> nodes = new HashSet<Node>(8);

    public static final String MAX_VALUE = ((char) 0) + "ÿÿÿÿÿÿÿÿÿÿÿÿÿÿÿÿÿÿÿÿ";

    public Bucket(BigInteger start, BigInteger end) {
        this.start = start;
        this.end = end;
    }

    public boolean contain(BigInteger bi) {
        return start.compareTo(bi) <= 0 && end.compareTo(bi) >= 0;
    }

    public boolean addNode(Node node) {
        return nodes.size() < BucketMaxCount && contain(node.getId().getValue()) ? nodes.add(node) : false;
    }

    public Bucket split() {

        BigInteger len = end.subtract(start).add(new BigInteger("1"));
        if(len.compareTo(new BigInteger(BucketMaxCountStr)) == 0) {
            return null;
        }
        BigInteger newStart = start.add(len.divide(new BigInteger("2")));
        BigInteger newEnd = newStart.subtract(new BigInteger("1"));

        Bucket b1 = new Bucket(start, newEnd);
        Bucket b2 = new Bucket(newStart, end);

        Node currentNode = (Node) nodes.toArray()[0];
        if(b1.contain(currentNode.getId().getValue())) {
            end = newEnd;
            return b2;
        }

        start = newStart;
        return b1;
    }

    public Set<Node> getNodes() {
        return nodes;
    }

    public int compareTo(Bucket o) {
        return start.compareTo(o.start);
    }
}
