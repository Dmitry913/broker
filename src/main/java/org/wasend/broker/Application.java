package org.wasend.broker;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.wasend.broker.service.interfaces.MessageSender;

@Slf4j
@SpringBootApplication
public class Application {

    public static void main(String[] args) {
        ApplicationContext context = SpringApplication.run(Application.class, args);
        // запускаем отправку сообщений
        MessageSender sender = (MessageSender) context.getBean("messageSenderImpl");
        sender.startSending();
    }
}

