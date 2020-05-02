package com.lagou.register;

import java.util.List;

/**
 * @author Chenyuhua
 * @date 2020/5/2 11:38
 */
public interface RpcRegistryHandler {

    /**
     * 服务注册
     * @param service
     * @param ip
     * @param port
     * @return
     */
    boolean registry(String service,String ip,String port);

    /**
     * 服务发现
     * @param Service
     * @return
     */
    List<String> discovery(String Service);


}
