package org.wasend.broker;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.wasend.broker.service.interfaces.MessageSender;
import org.wasend.broker.zookeeper.CuratorZooKeeperImpl;

@SpringBootApplication
public class Application {

    public static void main(String[] args) throws InterruptedException {
        // todo создать ноду в директории cluster, как только приложение запустилось
        // todo навешать вотчер на директорию cluster, чтобы отслеживать падение других брокеров
        ApplicationContext context = SpringApplication.run(Application.class, args);
        // запускаем отправку сообщений
        MessageSender sender = (MessageSender) context.getBean("messageSenderImpl");
        sender.startSending();
        ((CuratorZooKeeperImpl) context.getBean("curatorZooKeeperImpl")).doTest();
    }
}
