package com.zookeeper.task3.runner;

import com.mchange.v2.c3p0.ComboPooledDataSource;
import org.I0Itec.zkclient.IZkChildListener;
import org.I0Itec.zkclient.IZkDataListener;
import org.I0Itec.zkclient.ZkClient;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.beans.PropertyVetoException;
import java.sql.Connection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author Chenyuhua
 * @date 2020/5/3 4:13
 */
@Component
public class DataSourceRunner implements CommandLineRunner {

    private static Map<String, String> configMap = new ConcurrentHashMap<>();

    public static ThreadLocal<ComboPooledDataSource> threadLocal = new ThreadLocal();

    @Override
    public void run(String... args) throws Exception {
        ZkClient zkClient = new ZkClient("49.233.130.54:2181");
        zkClient.setZkSerializer(new MySerializer());
        List<String> childrens = zkClient.getChildren("/config");
        for (String children : childrens) {
            String value = zkClient.readData("/config/" + children);
            configMap.put(children, value);
            zkClient.subscribeDataChanges("/config/" + children, new IZkDataListener() {
                @Override
                public void handleDataChange(String path, Object data) throws
                        Exception {
                    System.out.println(path+"该节点内容被更新，更新后的内容"+data);
                    String key = path.replace("/config/", "");
                    configMap.put(key, (String)data);
                    if (threadLocal.get() != null) {
                        threadLocal.get().close();
                    }
                    initDataSource();
                    System.out.println("更新连接池" + key + data);
                }
                @Override
                public void handleDataDeleted(String s) throws Exception {
                    System.out.println(s+" 该节点被删除");
                }
            });
        }
        initDataSource();


        Connection connection = threadLocal.get().getConnection();
        System.out.println("connection:" + connection);
    }

    private void initDataSource() throws PropertyVetoException {
        ComboPooledDataSource comboPooledDataSource = new ComboPooledDataSource();
        comboPooledDataSource.setDriverClass(configMap.get("driverclass"));
        comboPooledDataSource.setJdbcUrl(configMap.get("jdbcUrl"));
        comboPooledDataSource.setUser(configMap.get("user"));
        comboPooledDataSource.setPassword(configMap.get("password"));
        threadLocal.set(comboPooledDataSource);
    }


}
