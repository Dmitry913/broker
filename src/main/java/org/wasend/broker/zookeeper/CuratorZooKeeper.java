package org.wasend.broker.zookeeper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.curator.x.async.AsyncCuratorFramework;
import org.apache.curator.x.async.modeled.JacksonModelSerializer;
import org.apache.curator.x.async.modeled.ModelSpec;
import org.apache.curator.x.async.modeled.ModeledFramework;
import org.apache.curator.x.async.modeled.ZPath;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

import static org.apache.zookeeper.Watcher.Event.EventType.NodeDataChanged;

@RequiredArgsConstructor
@Component
@Slf4j
public class CuratorZooKeeper {

    private static final String TEST_PATH = "/test3";
    private final AsyncCuratorFramework curatorFramework;

    public void doTest() throws InterruptedException {
        // defaultValue: CreateMode.PERSISTENT и ZooDefs.Ids.OPEN_ACL_UNSAFE
        curatorFramework.delete()
                .forPath(TEST_PATH)
                .thenRun(() -> System.out.println("\n\nSuccess deleted"));
        curatorFramework.create()
                .forPath(TEST_PATH, "hello".getBytes(StandardCharsets.UTF_8))
                .thenAccept(System.out::println);
        // генерим фасад, который добавит вотчер всем билдерам созданным далее
        // доступ к AsyncStage (это обёртка над результатом большинства операций) можно будет получить из WatchedEvent, который возвращается
        curatorFramework.watched()
                // создаёт GetBuilder, который возвращает данные
                .getData()
                // данные вернутся из объекта TEST_PATH
                .forPath(TEST_PATH)
                // возвращает completion_stage, который будет использован, когда watcher тригернётся
                .event()
                .thenAccept(watchedEvent -> {
                            if (watchedEvent.getType() == NodeDataChanged) {
                                curatorFramework.getData()
                                        .forPath(watchedEvent.getPath())
                                        .thenAccept(
                                                data -> System.out.println(
                                                        "Data on path " + watchedEvent.getPath() + " was change to: " + new String(data)
                                                )
                                        );
                            }
                        }
                );
        setData(NodeConfigInfo.builder().host("host").port(33).build(), TEST_PATH);
        curatorFramework.getData()
                .forPath(TEST_PATH)
                .thenAccept(data -> {
                    System.out.println("\n\n\nhere\n\n\n");
                    System.out.println(new String(data));
                });
        curatorFramework.setData()
                .forPath(TEST_PATH, "java".getBytes(StandardCharsets.UTF_8));
        Thread.sleep(11000);
    }


    // создаёт путь с такими данными или обновляет существующие
    private void setData(NodeConfigInfo nodeConfigInfo, String path) {
        ModelSpec<NodeConfigInfo> spec = ModelSpec.builder(
                ZPath.parseWithIds(path),
                JacksonModelSerializer.build(NodeConfigInfo.class))
                .build();
        ModeledFramework<NodeConfigInfo> modeledClient = ModeledFramework.wrap(curatorFramework, spec);
        log.info("Set data to " + path);
        // todo тут нужно как-то обработать ModelStage.exceptionally
        modeledClient.update(nodeConfigInfo);
    }
}
