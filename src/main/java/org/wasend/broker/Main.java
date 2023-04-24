package org.wasend.broker;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.wasend.broker.zookeeper.CuratorZooKeeper;

@SpringBootApplication
public class Main {

    public static void main(String[] args) throws InterruptedException {
        // todo создать ноду в директории cluster, как только приложение запустилось
        // todo навешать вотчер на директорию cluster, чтобы отслеживать падение других брокеров
        ApplicationContext context = SpringApplication.run(Main.class, args);
        ((CuratorZooKeeper) context.getBean("curatorZooKeeper")).doTest();
    }
}
