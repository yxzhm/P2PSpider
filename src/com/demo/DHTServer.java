package com.demo;

import com.demo.bencode.BencodeMap;
import com.demo.bencode.BencodeString;
import com.demo.bucket.BucketList;
import com.demo.bucket.Node;
import com.demo.bucket.NodeId;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Created by bkmr38 on 8/16/2016.
 */
public class DHTServer {

    private final Selector selector;
    private final ExecutorService udpReader = Executors.newSingleThreadExecutor();
    private final ExecutorService udpWriter = Executors.newSingleThreadExecutor();
    private final ExecutorService dataProcessor = Executors.newSingleThreadExecutor();
    private final BlockingQueue<Object[]> responseDataQueue = new LinkedBlockingQueue<Object[]>();
    private final BlockingQueue<Object[]> requestDataQueue = new LinkedBlockingQueue<Object[]>();
    private final Lock lock = new ReentrantLock();
    private final ByteBuffer buffer = ByteBuffer.allocateDirect(10240);

    private static final ExecutorService WORKER = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
    private static final ExecutorService NODE_FINDER = Executors.newSingleThreadExecutor();
    private static final CopyOnWriteArrayList<Object[]> NODES = new CopyOnWriteArrayList<Object[]>();
    public static final String[][] ROOT_NODES = {
            {"router.utorrent.com", "6881", null},
            {"dht.transmissionbt.com", "6881", null},
            {"router.bittorrent.com", "6881", null}
    };


    public DHTServer(List<byte[]> ips, List<Integer> ports, List<String> ids) {
        try {
            selector = Selector.open();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        for (int i = 0; i < ips.size(); i++) {
            bind(ips.get(i), ports.get(i), ids.get(i));
        }

        udpReader.execute(new Runnable() {
            public void run() {
                for (; ; ) {
                    try {
                        if (selector.select() == 0) {
                            continue;
                        }

                        Iterator<SelectionKey> it = selector.selectedKeys().iterator();
                        while (it.hasNext()) {
                            final SelectionKey key = it.next();
                            final DatagramChannel dc = (DatagramChannel) key.channel();

                            if (key.isReadable()) {
                                SocketAddress address = dc.receive(buffer);
                                buffer.flip();
                                byte[] result = new byte[buffer.limit()];
                                buffer.get(result);
                                buffer.clear();

                                if (result.length > 0) {
                                    responseDataQueue.put(new Object[]{key, result, address});
                                }
                            }

                            it.remove();
                        }
                    } catch (Exception e) {
                    }
                }
            }
        });

        udpWriter.execute(new Runnable() {
            public void run() {
                for (; ; ) {
                    try {
                        Object[] info = requestDataQueue.take();
                        ByteBuffer data = (ByteBuffer) info[0];
                        SocketAddress target = (SocketAddress) info[1];
                        final DatagramChannel dc = (DatagramChannel) ((SelectionKey) info[2]).channel();

                        while (data.hasRemaining()) {
                            dc.send(data, target);
                        }
                    } catch (Exception e) {
                    }
                }
            }
        });

        dataProcessor.execute(new Runnable() {
            public void run() {
                for (; ; ) {
                    SelectionKey key = null;
                    byte[] data = null;
                    InetSocketAddress address = null;
                    try {
                        Object[] element = responseDataQueue.take();
                        key = (SelectionKey) element[0];
                        data = (byte[]) element[1];
                        address = (InetSocketAddress) element[2];
                        BucketList bucketList = (BucketList) ((Object[]) key.attachment())[2];

                        final BencodeMap response = BencodeMap.getMap(new String(data, "iso-8859-1"), 0);

                        String y = new String(response.get(new BencodeString("y")).getData(), "iso-8859-1");
                        if (y.equals("r")) {
                            final Callbacker cb = getCallbacker(key, ByteBuffer.wrap(response.get(new BencodeString("t")).getData()).getChar() - 0);
                            if (null != cb) {
                                WORKER.execute(new Runnable() {
                                    public void run() {
                                        cb.execute(response);
                                    }
                                });
                            }
                        } else if (y.equals("q")) {
                            String id = new String(((BencodeMap) (response.get(new BencodeString("a")))).get(new BencodeString("id")).getData(), "iso-8859-1");
                            String t = new String(response.get(new BencodeString("t")).getData(), "iso-8859-1");
                            bucketList.addNode(new Node(address.getAddress().getHostAddress(), address.getPort(), id));

                            String q = new String(response.get(new BencodeString("q")).getData(), "iso-8859-1");
                            if (q.equals("ping")) {
                                responsePing(key, address, t);
                            } else if (q.equals("find_node")) {
                                String target = new String(((BencodeMap) (response.get(new BencodeString("a")))).get(new BencodeString("target")).getData(), "iso-8859-1");

                                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                                for (Node node : bucketList.getNearestNodes(new NodeId(target.getBytes("iso-8859-1")))) {
                                    baos.write(node.getId().getData());
                                    baos.write(node.getIp());
                                    baos.write(node.getPort());
                                }
                                responseFindNode(key, address, t, new String(baos.toByteArray(), "iso-8859-1"));
                            } else if (q.equals("get_peers")) {
                                String infoHash = new String(((BencodeMap) (response.get(new BencodeString("a")))).get(new BencodeString("info_hash")).getData(), "iso-8859-1");
                                println(infoHash);

                                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                                for (Node node : bucketList.getNearestNodes(new NodeId(infoHash.getBytes("iso-8859-1")))) {
                                    baos.write(node.getId().getData());
                                    baos.write(node.getIp());
                                    baos.write(node.getPort());
                                }
                                responseGetPeers(key, address, t, new String(baos.toByteArray(), "iso-8859-1"), "dg");
                            } else if (q.equals("announce_peer")) {
                                String infoHash = new String(((BencodeMap) (response.get(new BencodeString("a")))).get(new BencodeString("info_hash")).getData(), "iso-8859-1");
                                println(infoHash);

                                responseAnnouncePeer(key, address, t);
                            }
                        }

                    } catch (Exception e) {
                    }
                }
            }

            private void println(String infoHash) {
                StringBuilder msg = new StringBuilder(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss SSS").format(new Date())).append(" magnet:?xt=urn:btih:");
                for (char c : infoHash.toCharArray()) {
                    String hs = Integer.toHexString(c);
                    if (hs.length() == 1) {
                        msg.append(0);
                    }
                    msg.append(hs);
                }
                System.out.println(msg);
            }
        });

        NODE_FINDER.execute(new Runnable() {

            private static final int TPS = 10;
            private static final int MAX_STACK = 10000;
            private static final int MAX_OLD_NODE = 10000;

            @SuppressWarnings("unchecked")
            public void run() {
                for (; ; ) {
                    for (Object[] objs : NODES) {
                        final Stack<String[]> stack = (Stack<String[]>) objs[0];
                        final Set<String> oldNode = (Set<String>) objs[1];
                        final SelectionKey key = (SelectionKey) objs[2];
                        final BucketList bucketList = (BucketList) objs[3];
                        final String id = (String) objs[4];

                        String[] temp = null;
                        boolean isOver = false;
                        synchronized (stack) {
                            if (stack.size() > 0) {
                                if (stack.size() > MAX_STACK) {
                                    for (int i = 0; i < MAX_STACK; i++) {
                                        stack.remove(i);
                                    }
                                }
                                temp = stack.pop();
                            } else {
                                isOver = true;
                            }
                        }

                        if (isOver) {
                            NODES.remove(objs);
                            continue;
                        }

                        final String[] address = temp;
                        final InetSocketAddress target = new InetSocketAddress(address[0], Integer.parseInt(address[1]));

                        synchronized (oldNode) {
                            if (oldNode.contains(address[0] + ":" + address[1])) {
                                continue;
                            } else {
                                if (oldNode.size() > MAX_OLD_NODE) {
                                    for (int i = 0; i < MAX_OLD_NODE / 2; i++) {
                                        oldNode.remove(i);
                                    }
                                }
                                oldNode.add(address[0] + ":" + address[1]);
                            }
                        }

                        ping(key, target, new Callbacker() {
                            public void execute(BencodeMap response) {
                                String ip = address[0];
                                String port = address[1];
                                String nodeId = address[2];

                                if (null != nodeId) {
                                    bucketList.addNode(new Node(ip, Integer.parseInt(port), nodeId));
                                }

                                findNode(key, target, id, new Callbacker() {
                                    public void execute(BencodeMap response) {
                                        try {
                                            byte[] nodes = ((BencodeMap) (response.get(new BencodeString("r")))).get(new BencodeString("nodes")).getData();

                                            for (int i = 0; i < nodes.length; i += 26) {
                                                String nodeId = new String(Arrays.copyOfRange(nodes, i, i + 20), "iso-8859-1");
                                                byte[] ipPort = Arrays.copyOfRange(nodes, i + 20, i + 26);
                                                String ip = (ipPort[0] & 0xFF) + "." + (ipPort[1] & 0xFF) + "." + (ipPort[2] & 0xFF) + "." + (ipPort[3] & 0xFF);
                                                int port = ByteBuffer.wrap(new byte[]{0, 0, ipPort[4], ipPort[5]}).getInt();

                                                boolean isContain = true;
                                                synchronized (stack) {
                                                    synchronized (oldNode) {
                                                        if (!oldNode.contains(ip + ":" + port)) {
                                                            isContain = false;
                                                        }
                                                    }

                                                    if (!isContain) {
                                                        stack.push(new String[]{ip, String.valueOf(port), nodeId});
                                                    }
                                                }
                                            }
                                        } catch (Exception e) {
                                        }
                                    }
                                });
                            }
                        });

                        try {
                            Thread.sleep(1000 / TPS);
                        } catch (InterruptedException e) {
                        }
                    }
                }
            }
        });
    }

    private SelectionKey bind(byte[] ip, int port, String id) {
        SelectionKey key = null;
        try {
            List<Integer> transactionIds = new ArrayList<Integer>(Character.MAX_VALUE + 1);
            for (int i = 0; i <= Character.MAX_VALUE; i++) {
                transactionIds.add(i);
            }

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            baos.write(id.getBytes("iso-8859-1"));
            baos.write(ip);
            ByteBuffer bb = ByteBuffer.allocate(4).putInt(port);
            bb.flip();
            baos.write(Arrays.copyOfRange(bb.array(), 2, 4));
            BucketList bucketList = new BucketList(baos.toByteArray());

            DatagramChannel channel = DatagramChannel.open();
            channel.configureBlocking(false);
            channel.socket().bind(new InetSocketAddress(port));
            key = channel.register(selector, SelectionKey.OP_READ);
            key.attach(
                    new Object[]{
                            transactionIds,
                            new ArrayList<Callbacker>(Character.MAX_VALUE + 1),
                            bucketList,
                            id
                    }
            );

            Stack<String[]> stack = new Stack<String[]>();
            for (String[] address : ROOT_NODES) {
                stack.push(address);
            }
            NODES.add(new Object[]{stack, new HashSet<String>(), key, bucketList, id});
        } catch (Exception e) {
        }

        return key;
    }

    @SuppressWarnings("unchecked")
    public int addTransaction(SelectionKey key, Callbacker cb) {
        lock.lock();
        try {
            Object[] attachment = (Object[]) key.attachment();
            List<Integer> transactionIds = (List<Integer>) attachment[0];
            List<Callbacker> callbackers = (List<Callbacker>) attachment[1];

            if (transactionIds.isEmpty()) {
                removeTransaction(key);
            }
            cb.setTransactionId(transactionIds.remove(0));
            callbackers.add(cb);
        } finally {
            lock.unlock();
        }
        return cb.getTransactionId();
    }

    @SuppressWarnings("unchecked")
    public Callbacker getCallbacker(SelectionKey key, int transactionId) {
        Callbacker result = null;
        lock.lock();
        try {
            Object[] attachment = (Object[]) key.attachment();
            List<Integer> transactionIds = (List<Integer>) attachment[0];
            List<Callbacker> callbackers = (List<Callbacker>) attachment[1];

            for (Callbacker cb : callbackers) {
                if (cb.getTransactionId() == transactionId) {
                    result = cb;
                    break;
                }
            }
            if (null != result) {
                callbackers.remove(result);
                transactionIds.add(result.getTransactionId());
            }
        } finally {
            lock.unlock();
        }

        return result;
    }

    @SuppressWarnings("unchecked")
    public void removeTransaction(SelectionKey key) {
        lock.lock();
        try {
            Object[] attachment = (Object[]) key.attachment();
            List<Integer> transactionIds = (List<Integer>) attachment[0];
            List<Callbacker> callbackers = (List<Callbacker>) attachment[1];

            final Callbacker cb = callbackers.remove(0);
            transactionIds.add(cb.getTransactionId());
            WORKER.execute(new Runnable() {
                public void run() {
                    cb.destory();
                }
            });
        } finally {
            lock.unlock();
        }
    }

    public void ping(SelectionKey key, InetSocketAddress target, Callbacker cb) {
        try {
            ByteBuffer bb = ByteBuffer.allocate(2).putShort((short) addTransaction(key, cb));
            bb.flip();

            BencodeMap request = new BencodeMap();
            request.put(new BencodeString("t"), new BencodeString(new String(bb.array(), "iso-8859-1")));
            request.put(new BencodeString("y"), new BencodeString("q"));
            request.put(new BencodeString("q"), new BencodeString("ping"));

            BencodeMap params = new BencodeMap();
            params.put(new BencodeString("id"), new BencodeString((String) ((Object[]) key.attachment())[3]));
            request.put(new BencodeString("a"), params);

            requestDataQueue.put(new Object[]{ByteBuffer.wrap(request.getTotalData()), target, key});
        } catch (Exception e) {
        }
    }

    public void findNode(SelectionKey key, InetSocketAddress target, String targetId, Callbacker cb) {
        try {
            ByteBuffer bb = ByteBuffer.allocate(2).putShort((short) addTransaction(key, cb));
            bb.flip();

            BencodeMap request = new BencodeMap();
            request.put(new BencodeString("t"), new BencodeString(new String(bb.array(), "iso-8859-1")));
            request.put(new BencodeString("y"), new BencodeString("q"));
            request.put(new BencodeString("q"), new BencodeString("find_node"));

            BencodeMap params = new BencodeMap();
            params.put(new BencodeString("id"), new BencodeString((String) ((Object[]) key.attachment())[3]));
            params.put(new BencodeString("target"), new BencodeString(targetId));
            request.put(new BencodeString("a"), params);

            requestDataQueue.put(new Object[]{ByteBuffer.wrap(request.getTotalData()), target, key});
        } catch (Exception e) {
        }
    }

    public void responsePing(SelectionKey key, InetSocketAddress target, String t) {
        try {
            BencodeMap response = new BencodeMap();
            response.put(new BencodeString("t"), new BencodeString(t));
            response.put(new BencodeString("y"), new BencodeString("r"));

            BencodeMap params = new BencodeMap();
            params.put(new BencodeString("id"), new BencodeString((String) ((Object[]) key.attachment())[3]));
            response.put(new BencodeString("r"), params);

            requestDataQueue.put(new Object[]{ByteBuffer.wrap(response.getTotalData()), target, key});
        } catch (Exception e) {
        }
    }

    public void responseFindNode(SelectionKey key, InetSocketAddress target, String t, String nodes) {
        try {
            BencodeMap response = new BencodeMap();
            response.put(new BencodeString("t"), new BencodeString(t));
            response.put(new BencodeString("y"), new BencodeString("r"));

            BencodeMap params = new BencodeMap();
            params.put(new BencodeString("id"), new BencodeString((String) ((Object[]) key.attachment())[3]));
            params.put(new BencodeString("nodes"), new BencodeString(nodes));
            response.put(new BencodeString("r"), params);

            requestDataQueue.put(new Object[]{ByteBuffer.wrap(response.getTotalData()), target, key});
        } catch (Exception e) {
        }
    }

    public void responseGetPeers(SelectionKey key, InetSocketAddress target, String t, String nodes, String token) {
        try {
            BencodeMap response = new BencodeMap();
            response.put(new BencodeString("t"), new BencodeString(t));
            response.put(new BencodeString("y"), new BencodeString("r"));

            BencodeMap params = new BencodeMap();
            params.put(new BencodeString("id"), new BencodeString((String) ((Object[]) key.attachment())[3]));
            params.put(new BencodeString("token"), new BencodeString(token));
            params.put(new BencodeString("nodes"), new BencodeString(nodes));
            response.put(new BencodeString("r"), params);

            requestDataQueue.put(new Object[]{ByteBuffer.wrap(response.getTotalData()), target, key});
        } catch (Exception e) {
        }
    }

    public void responseAnnouncePeer(SelectionKey key, InetSocketAddress target, String t) {
        try {
            BencodeMap response = new BencodeMap();
            response.put(new BencodeString("t"), new BencodeString(t));
            response.put(new BencodeString("y"), new BencodeString("r"));

            BencodeMap params = new BencodeMap();
            params.put(new BencodeString("id"), new BencodeString((String) ((Object[]) key.attachment())[3]));
            response.put(new BencodeString("r"), params);

            requestDataQueue.put(new Object[]{ByteBuffer.wrap(response.getTotalData()), target, key});
        } catch (Exception e) {
        }
    }
}
