package com.lagou.runner;

import com.lagou.config.RpcConfig;
import com.lagou.service.UserServiceImpl;
import org.I0Itec.zkclient.ZkClient;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/**
 * @author Chenyuhua
 * @date 2020/4/21 23:49
 */
@Component
public class ServerRunner implements CommandLineRunner {

    @Resource
    private RpcConfig rpcConfig;

    @Override
    public void run(String... args) throws Exception {
        UserServiceImpl.startServer(rpcConfig.getHost(), rpcConfig.getPort());
        System.out.println("rpc server start");
        ZkClient zkClient = new ZkClient("49.233.130.54:2181");
        if (!zkClient.exists("/rpc-server")) {
            zkClient.createPersistent("/rpc-server");
        }
        zkClient.createEphemeral("/rpc-server/8999", rpcConfig.getHost() + ":" + rpcConfig.getPort());
        System.out.println("create sserver 2 success");
    }
}
