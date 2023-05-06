package org.wasend.broker.zookeeper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.curator.x.async.AsyncCuratorFramework;
import org.apache.curator.x.async.modeled.JacksonModelSerializer;
import org.apache.curator.x.async.modeled.ModelSpec;
import org.apache.curator.x.async.modeled.ModeledFramework;
import org.apache.curator.x.async.modeled.ZPath;
import org.springframework.stereotype.Component;
import org.wasend.broker.dao.entity.MetaInfoZK;
import org.wasend.broker.dao.entity.NodeInfo;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.concurrent.ExecutionException;

import static org.apache.zookeeper.Watcher.Event.EventType.NodeDataChanged;

@RequiredArgsConstructor
@Component
@Slf4j
// todo нужно обязательно вызывать метод sync, там где нужна согласованность данных,
//  иначе мы можем получить разные данные на двух клиентах, если они подсоединены к разным серверам

// todo можно сохранить session_id и password в локальное хранилище (в файлик сервера) и подключится к той же самой сессии после перезапуска приклада
//  - нужно просто скормить параметры session_id и password при подключении к системе
public class CuratorZooKeeperImpl implements CuratorZooKeeper {

    private static final String TEST_PATH = "/test3";
    // todo а нужен ли мне вообще асинхронный клиент, если я завязался на асинхрон на уровне вызова методов данного класса?
    private final AsyncCuratorFramework curatorFramework;
    // todo можно определять корневую директорию самому на этапе создания объекта данного класса
    private final String rootDirectory = "/root";


    @Override
    public MetaInfoZK getRootInfo() throws Exception {
        try {
            curatorFramework.sync().forPath(rootDirectory).toCompletableFuture().get();
            return getData(rootDirectory, MetaInfoZK.class);
        } catch (Exception e) {
            log.error("Can't read info from zooKeeper by path: " + rootDirectory);
            throw new Exception(e);
        }
    }

    @Override
    public NodeInfo getNodeInfoByDirectory(String directory) {
        try {
            String correctDirectory = rootDirectory + "/" + directory;
            return getData(correctDirectory, NodeInfo.class);
        } catch (Exception e) {
            log.error("Can't read info from zooKeeper by path: " + directory);
            throw new RuntimeException(e);
        }
    }

    @Override
    public void updateMetaInfo(MetaInfoZK metaInfo) {
        setData(rootDirectory, metaInfo);
    }

    // создаёт путь с такими данными или обновляет существующие
    private <T> void setData(String path, T info) {
        ModelSpec<T> spec = ModelSpec.builder(
                        ZPath.parseWithIds(path),
                        JacksonModelSerializer.build((Class<T>) info.getClass()))
                .build();
        ModeledFramework<T> modeledClient = ModeledFramework.wrap(curatorFramework, spec);
        log.info("Set data to " + path);
        // todo тут нужно как-то обработать ModelStage.exceptionally
        modeledClient.set(info);
    }

    private <T> T getData(String path, Class<T> tClass) throws ExecutionException, InterruptedException {
        ModelSpec<T> spec = ModelSpec.builder(
                        ZPath.parseWithIds(path),
                        JacksonModelSerializer.build(tClass))
                .build();
        ModeledFramework<T> modeledClient = ModeledFramework.wrap(curatorFramework, spec);
        return modeledClient.read().toCompletableFuture().get();
    }





    // todo навесить вотчеры на корневую директорию для отслеживания изменения данных и для отслеживания дочерних директория (ноды)

    /**
     * Существует 2 потока:
     *  1) для IO операций, переподключения к серверу и сердцебиения
     *  2) для коллбеков и event событий
     *  Причём второй тип потоков может выполнять только один коллбек за раз - то есть, он блокируется на время выполнения callback
     */

    /**
     * 1) Тригер для вотчера будет отправлен единожды - то есть, мы читаем данные: потом навешиваем вотчер, и при измененеии данных, данный вотчер сработает.
     * Затем нам нужно будет навешать вотчер вновь, иначе тригера не произойдёт
     * 2) Мы можем пропустить некоторые события, которые будут между получением события и установкой нового вотчера (МОЖЕТ БЫТЬ МОЖНО КАК-ТО ЧЕРЕЗ ВЕРСИИ ПОРЕШАТЬ?)
     * 3) если упадёт сервер зукипер, то я получу session-event, и больше не буду получать события до тех пор, пока сервер не поднимется вновь.
     */
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
        setData(NodeConfigInfo.builder().host("host2").port(44).build(), TEST_PATH);
        setData(NodeConfigInfo.builder().host("host45").port(12).nodeIdOfStoredReplicas(Collections.singletonList("dsds")).build(), TEST_PATH);
        setData(NodeConfigInfo.builder().host("32").port(5).nodeIdOfStoredReplicas(Collections.singletonList("a23d")).build(), TEST_PATH);
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
        modeledClient.set(nodeConfigInfo);
    }
}
