package com.demo.bucket;

import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.util.*;

/**
 * Created by bkmr38 on 8/16/2016.
 */
public class BucketList {

    private final List<Bucket> buckets = new ArrayList<Bucket>();
    private final Node currentNode;

    public BucketList(byte[] info) {
        currentNode = new Node(info);
        try {
            Bucket b = new Bucket(new BigInteger("0"), new BigInteger(Bucket.MAX_VALUE.getBytes("iso-8859-1")));
            b.addNode(currentNode);
            buckets.add(b);
            Bucket nb;
            while(null != (nb = b.split())) {
                buckets.add(nb);
            }
            Collections.sort(buckets);
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

    public synchronized List<Node> getNearestNodes(NodeId id) {
        int index = 0;
        for(int i = 0; i < buckets.size(); i++) {
            if(buckets.get(i).contain(id.getValue())) {
                index = i;
            }
        }

        List<Node> nodes = new ArrayList<Node>(buckets.get(index).getNodes());
        List<Node> leftNodes = new ArrayList<Node>();
        List<Node> rightNodes = new ArrayList<Node>();
        for(int i = index - 1; i >= 0; i--) {
            leftNodes.addAll(buckets.get(i).getNodes());
            if(leftNodes.size() >= 8) {
                break;
            }
        }
        for(int i = index + 1; i < buckets.size(); i++) {
            rightNodes.addAll(buckets.get(i).getNodes());
            if(rightNodes.size() >= 8) {
                break;
            }
        }

        nodes.addAll(leftNodes);
        nodes.addAll(rightNodes);

        Set<BigInteger> distances = new TreeSet<BigInteger>();
        Map<BigInteger, List<Node>> distance2Node = new HashMap<BigInteger, List<Node>>();
        for(Node n : nodes) {
            BigInteger distance = n.getId().getValue().subtract(id.getValue()).abs();
            distances.add(distance);
            List<Node> value = distance2Node.get(distance);
            if(null == value) {
                value = new ArrayList<Node>();
                distance2Node.put(distance, value);
            }
            value.add(n);
        }
        List<Node> result = new ArrayList<Node>();
        for(BigInteger distance : distances) {
            result.addAll(distance2Node.get(distance));
            if(result.size() >= 8) {
                break;
            }
        }

        return result.size() > 8 ? result.subList(0, 8) : result;
    }

    public synchronized boolean addNode(Node node) {
        for(Bucket bucket : buckets) {
            if(bucket.addNode(node)) {
                return true;
            }
        }

        return false;
    }

    public Node getCurrentNode() {
        return currentNode;
    }

    public List<Bucket> getBuckets() {
        return buckets;
    }
}
