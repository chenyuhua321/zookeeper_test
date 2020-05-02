package com.lagou.client;

import com.lagou.service.UserService;
import org.I0Itec.zkclient.IZkChildListener;
import org.I0Itec.zkclient.ZkClient;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class ClientBootStrap {

    public static final String providerName = "UserService#sayHello#";

    private static Map<String, RpcConsumer> rpcConsumerMap = new ConcurrentHashMap<>();

    private static List<String> serverList = new CopyOnWriteArrayList<String>();

    public static void main(String[] args) throws InterruptedException {
        ZkClient zkClient = new ZkClient("49.233.130.54:2181");
        String path = "/rpc-server";
        serverList = zkClient.getChildren(path);
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
            try{
                for (String children : serverList) {
                    String childrenPath = path + "/" + children;
                    String o = zkClient.readData(childrenPath);
                    String[] s = o.split(":");
                    RpcConsumer rpcConsumer;
                    if (rpcConsumerMap.containsKey(o)) {
                        rpcConsumer = rpcConsumerMap.get(o);
                    } else {
                        rpcConsumer = new RpcConsumer(s[0], Integer.valueOf(s[1]));
                        rpcConsumerMap.put(o, rpcConsumer);
                    }
                    doRpc(rpcConsumer);
                }
            }catch (Exception e){
                System.out.println("-------------");
            }

        }

    }

    public static void doRpc(RpcConsumer rpcConsumer) {
        UserService proxy = (UserService) rpcConsumer.createProxy(UserService.class, providerName);

        System.out.println(proxy.sayHello("are you ok?"));
    }

    private void watchNode(final ZkClient zk,String path) {
            List<String> nodeList = zk.subscribeChildChanges(path, new IZkChildListener() {
                @Override
                public void handleChildChange(String s, List<String> list) throws Exception {

                }
            });

    }

}
