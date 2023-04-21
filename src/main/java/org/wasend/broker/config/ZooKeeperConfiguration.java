package org.wasend.broker.config;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.RetryNTimes;
import org.apache.curator.x.async.AsyncCuratorFramework;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ZooKeeperConfiguration {

    @Value("${zookeeper.retry.count}")
    private int countRetry;
    @Value("${zookeeper.retry.ms.between.retries}")
    private int msBetweenRetries;
    @Value("${zookeeper.host.address}")
    private String host;
    @Value("${zookeeper.port.address}")
    private int port;

    @Bean
    public AsyncCuratorFramework curatorFramework() {
        CuratorFramework curatorFramework = CuratorFrameworkFactory
                .newClient(
                        String.format("%s:%d", host, port),
                        new RetryNTimes(countRetry, msBetweenRetries)
                );
        curatorFramework.start();
        return AsyncCuratorFramework.wrap(curatorFramework);
    }
}
