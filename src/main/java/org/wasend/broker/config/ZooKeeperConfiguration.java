package org.wasend.broker.config;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.RetryNTimes;
import org.apache.curator.x.async.AsyncCuratorFramework;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
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
    // как только SESSION_TIMEOUT произойдёт, все ефемерные ZNode будут удалены
    public CuratorFramework syncCuratorFramework() {
        CuratorFramework curatorFramework = CuratorFrameworkFactory
                .newClient(
                        String.format("%s:%d", host, port),
                        new RetryNTimes(countRetry, msBetweenRetries)
                );
        curatorFramework.start();
        return curatorFramework;
    }

    @Bean
    @Autowired
    public AsyncCuratorFramework asyncCuratorFramework(@Qualifier("syncCuratorFramework") CuratorFramework curatorFramework) {
        return AsyncCuratorFramework.wrap(curatorFramework);
    }
}
