package org.wasend.broker.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.ApplicationEventMulticaster;
import org.springframework.context.event.SimpleApplicationEventMulticaster;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
@EnableAsync
@Slf4j
public class ApplicationConfiguration {

    // todo хорошо ли иметь один экземпляр вебклиента??
    @Bean
    public WebClient webClient() {
        log.info("Create bean webClient");
        return WebClient.builder()
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    @Bean(name = "applicationEventMulticaster")
    public ApplicationEventMulticaster simpleApplicationEventMulticaster() {
        SimpleApplicationEventMulticaster applicationEventMulticaster = new SimpleApplicationEventMulticaster();
        applicationEventMulticaster.setTaskExecutor(new SimpleAsyncTaskExecutor());
        return applicationEventMulticaster;
    }
}
