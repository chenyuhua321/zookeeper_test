package com.lagou.client;

import com.lagou.service.UserService;
import org.I0Itec.zkclient.IZkChildListener;
import org.I0Itec.zkclient.ZkClient;
import org.apache.commons.collections4.CollectionUtils;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ClientBootStrap {

    public static final String providerName = "UserService#sayHello#";

    private static Map<String, RpcConsumer> rpcConsumerMap = new ConcurrentHashMap<>();

    private static ZkClient zkClient = new ZkClient("49.233.130.54:2181");

    private static ExecutorService executor = Executors
            .newFixedThreadPool(Runtime.getRuntime().availableProcessors());

    private static List<String> serverList = new CopyOnWriteArrayList<String>();

    private static Map<String, Long> callTimeMap = new ConcurrentHashMap<>();

    public static void main(String[] args) throws InterruptedException {
        String path = "/rpc-server";
        serverList = zkClient.getChildren(path);
        executor.execute(() -> doCallTime());
        zkClient.subscribeChildChanges(path, new IZkChildListener() {
            @Override
            public void handleChildChange(String parentPath, List<String>
                    currentChilds) throws Exception {
                System.out.println(parentPath + " 's child changed,currentChilds:" + currentChilds);
                serverList.clear();
                serverList.addAll(currentChilds);
                Set<String> keys = rpcConsumerMap.keySet();
                Iterator it = keys.iterator();
                while (it.hasNext()) {
                    String next = (String) it.next();
                    if (!serverList.contains(next)) {
                        rpcConsumerMap.remove(next);
                    }
                }
            }
        });
        while (true) {
            Thread.sleep(2000);
            try {
                //打乱list顺序，获得最小time
                List<String> timeChilrens = zkClient.getChildren("/time");
                String server = "";
                String childrenPath = "";
                if (CollectionUtils.isNotEmpty(timeChilrens)) {
                    Collections.shuffle(timeChilrens);
                    String minPath = timeChilrens.get(0);
                    Long minTime = Long.MAX_VALUE;
                    for (String timeChilren : timeChilrens) {
                        Long o = zkClient.readData("/time/"+timeChilren);
                        if (o != 0L && o < minTime) {
                            minTime = o;
                            minPath = timeChilren;
                        }
                    }
                    System.out.println("最小时间服务器:"+minPath+"最小时间:"+minTime);
                    childrenPath = minPath.replace("/time", "");
                    server = zkClient.readData("/rpc-server/"+childrenPath);
                } else {
                    childrenPath = serverList.get(0);
                    server = zkClient.readData("/rpc-server/"+childrenPath);
                }
                String[] serverPort = server.split(":");
                RpcConsumer rpcConsumer;
                if (rpcConsumerMap.containsKey(server)) {
                    rpcConsumer = rpcConsumerMap.get(server);
                } else {
                    rpcConsumer = new RpcConsumer(serverPort[0], Integer.valueOf(serverPort[1]));
                    rpcConsumerMap.put(server, rpcConsumer);
                }
                doRpc(childrenPath, rpcConsumer);
            } catch (Exception e) {
                System.out.println("-------------"+e.getMessage());
            }

        }

    }

    public static void doRpc(String childrenPath, RpcConsumer rpcConsumer) {
        UserService proxy = (UserService) rpcConsumer.createProxy(UserService.class, providerName);
        long beginTime = System.currentTimeMillis();
        System.out.println(proxy.sayHello("are you ok?"));
        long userTime = System.currentTimeMillis() - beginTime;
        callTimeMap.put("/time/" + childrenPath, userTime);
    }

    private static void doCallTime() {
        while (true) {
            try {
                Thread.sleep(5000);
                if (!zkClient.exists("/time")) {
                    zkClient.createPersistent("/time");
                }
                callTimeMap.entrySet().stream().forEach(stringLongEntry -> {
                    if (zkClient.exists( stringLongEntry.getKey())) {
                        zkClient.writeData( stringLongEntry.getKey(), stringLongEntry.getValue());
                    } else {
                        zkClient.createEphemeral(stringLongEntry.getKey(), stringLongEntry.getValue());
                    }
                    callTimeMap.put(stringLongEntry.getKey(), 0L);
                });
                System.out.println("更新连接时间成功");
            } catch (InterruptedException e) {
                System.out.println("InterruptedException" + e);
                e.printStackTrace();
            }
        }
    }

}
