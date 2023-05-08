package org.wasend.broker.zookeeper;

import lombok.extern.slf4j.Slf4j;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.x.async.AsyncCuratorFramework;
import org.apache.curator.x.async.modeled.JacksonModelSerializer;
import org.apache.curator.x.async.modeled.ModelSpec;
import org.apache.curator.x.async.modeled.ModeledFramework;
import org.apache.curator.x.async.modeled.ZNode;
import org.apache.curator.x.async.modeled.ZPath;
import org.apache.curator.x.async.modeled.versioned.Versioned;
import org.apache.curator.x.async.modeled.versioned.VersionedModeledFramework;
import org.apache.zookeeper.CreateMode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.wasend.broker.dao.entity.MetaInfoZK;
import org.wasend.broker.dao.entity.NodeInfo;
import org.wasend.broker.eventObjects.MetaInfo;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import static org.apache.zookeeper.Watcher.Event.EventType.NodeChildrenChanged;
import static org.apache.zookeeper.Watcher.Event.EventType.NodeDataChanged;

@Component
@Slf4j
// todo нужно обязательно вызывать метод sync, там где нужна согласованность данных,
//  иначе мы можем получить разные данные на двух клиентах, если они подсоединены к разным серверам

// todo можно сохранить session_id и password в локальное хранилище (в файлик сервера) и подключится к той же самой сессии после перезапуска приклада
//  - нужно просто скормить параметры session_id и password при подключении к системе
public class CuratorZooKeeperImpl implements CuratorZooKeeper {

    private static final String TEST_PATH = "/test3";
    // todo а нужен ли мне вообще асинхронный клиент, если я завязался на асинхрон на уровне вызова методов данного класса?
    private final AsyncCuratorFramework asyncCuratorFramework;
    private final CuratorFramework curatorFramework;
    // todo можно определять корневую директорию самому на этапе создания объекта данного класса
    private final String rootDirectory = "/root";
    private final String directoryInstanceName;
    private final ApplicationEventPublisher applicationEventPublisher;
    @Value("prefix.ephemeral.node")
    private String prefixEphemeralNode;

    @Autowired
    public CuratorZooKeeperImpl(AsyncCuratorFramework asyncCuratorFramework,
                                ApplicationEventPublisher applicationEventPublisher,
                                CuratorFramework curatorFramework) {
        this.asyncCuratorFramework = asyncCuratorFramework;
        this.applicationEventPublisher = applicationEventPublisher;
        this.curatorFramework = curatorFramework;
        // todo создать ноду в директории cluster, как только приложение запустилось (nodeId данного узла взять из zooKeeper.current.nodeId)
        directoryInstanceName = createEphemeralNode();
        createRootDirectoryWatcher();
        createChildrenWatcher();
    }


    @Override
    public MetaInfo getRootInfo() throws Exception {
        try {
            asyncCuratorFramework.sync().forPath(rootDirectory).toCompletableFuture().get();
            ZNode<MetaInfoZK> zNode = getData(rootDirectory, MetaInfoZK.class);
            return new MetaInfo(zNode.stat().getVersion(), zNode.model());
        } catch (Exception e) {
            log.error("Can't read info from zooKeeper by path: " + rootDirectory);
            throw new Exception(e);
        }
    }

    @Override
    public NodeInfo getNodeInfoByDirectory(String directory) {
        try {
            String correctDirectory = rootDirectory + "/" + directory;
            return getData(correctDirectory, NodeInfo.class).model();
        } catch (Exception e) {
            log.error("Can't read info from zooKeeper by path: " + directory);
            throw new RuntimeException(e);
        }
    }

    @Override
    public boolean updateMetaInfo(MetaInfoZK metaInfo, int version) {
        return setData(rootDirectory, metaInfo, version);
    }

    @Override
    public String instanceNodeId() {
        return directoryInstanceName;
    }

//    @Override
//    public boolean movePartitionToCurrentInstanceInTransaction(Set<String> partitionsId, int version) {
//        // todo нужно в транзакции прочитать ещё раз информацию из Map<partitionId, Directory>, возможно уже кто-то взял на себя эту партицию, и если никто не взял, то взять на себя (добавить значение в Map<partitionId, Directory>) и отпустить транзакцию, иначе ничего не делать, так как информацию обновится через вотчер
//        setData(rootDirectory, );
//        try {
//            CuratorOp checkOperation = curatorFramework.transactionOp().check().withVersion(version).forPath(rootDirectory);
//            CuratorOp updateOperation = curatorFramework.transactionOp().setData().withVersion(version)
//            curatorFramework.inTransaction().check().forPath(rootDirectory).
//            curatorFramework.transactionOp().check().forPath(rootDirectory).get().
//        }
//    }

    // создаёт путь с такими данными или обновляет существующие
    private <T> boolean setData(String path, T info, int version) {
        ModelSpec<T> spec = ModelSpec.builder(
                        ZPath.parseWithIds(path),
                        JacksonModelSerializer.build((Class<T>) info.getClass()))
                .build();
        VersionedModeledFramework<T> versionedModeledClient = ModeledFramework.wrap(asyncCuratorFramework, spec).versioned();
        log.info("Set data to " + path);
        // todo тут нужно как-то обработать ModelStage.exceptionally
        try {
            versionedModeledClient.set(Versioned.from(info, version)).toCompletableFuture().get();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private <T> ZNode<T> getData(String path, Class<T> tClass) throws ExecutionException, InterruptedException {
        ModelSpec<T> spec = ModelSpec.builder(
                        ZPath.parseWithIds(path),
                        JacksonModelSerializer.build(tClass))
                .build();
        ModeledFramework<T> modeledClient = ModeledFramework.wrap(asyncCuratorFramework, spec);
        return modeledClient.readAsZNode().toCompletableFuture().get();
    }

    private void createRootDirectoryWatcher() {
        asyncCuratorFramework.watched()
                .getData()
                .forPath(rootDirectory)
                .event()
                .thenAccept(watchedEvent -> {
                    if (watchedEvent.getType().equals(NodeDataChanged)) {
                        try {
                            applicationEventPublisher.publishEvent(getRootInfo());
                        } catch (Exception e) {
                            log.error("Failed reading updated information");
                        }
                        createRootDirectoryWatcher();
                    }
                });
    }

    private void createChildrenWatcher() {
        asyncCuratorFramework.watched()
                .getChildren()
                .forPath(rootDirectory)
                .event()
                .thenAccept(watchedEvent -> {
                    if (watchedEvent.getType().equals(NodeChildrenChanged)) {
                        try {
                            applicationEventPublisher.publishEvent(asyncCuratorFramework
                                    .getChildren()
                                    .forPath(rootDirectory)
                                    .toCompletableFuture()
                                    .get()
                                    .stream()
                                    // оставляем только относительную директорию
                                    .map(absolutelyDirectory -> absolutelyDirectory.split("/")[1])
                                    .collect(Collectors.toList()));
                        } catch (Exception e) {
                            log.error("Failed getting instances changing", e);
                        }
                    }
                    createChildrenWatcher();
                });
    }

    private String createEphemeralNode() {
        try {
            return asyncCuratorFramework.create().withMode(CreateMode.EPHEMERAL_SEQUENTIAL).forPath(rootDirectory + "/" + prefixEphemeralNode).toCompletableFuture().get();
        } catch (Exception e) {
            log.error("Failed creating ephemeral node for this instance");
            throw new RuntimeException(e);
        }
    }

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
        asyncCuratorFramework.delete()
                .forPath(TEST_PATH)
                .thenRun(() -> System.out.println("\n\nSuccess deleted"));
        asyncCuratorFramework.create()
                .forPath(TEST_PATH, "hello".getBytes(StandardCharsets.UTF_8))
                .thenAccept(System.out::println);
        // генерим фасад, который добавит вотчер всем билдерам созданным далее
        // доступ к AsyncStage (это обёртка над результатом большинства операций) можно будет получить из WatchedEvent, который возвращается
        asyncCuratorFramework.watched()
                // создаёт GetBuilder, который возвращает данные
                .getData()
                // данные вернутся из объекта TEST_PATH
                .forPath(TEST_PATH)
                // возвращает completion_stage, который будет использован, когда watcher тригернётся
                .event()
                .thenAccept(watchedEvent -> {
                            if (watchedEvent.getType() == NodeDataChanged) {
                                asyncCuratorFramework.getData()
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
        asyncCuratorFramework.getData()
                .forPath(TEST_PATH)
                .thenAccept(data -> {
                    System.out.println("\n\n\nhere\n\n\n");
                    System.out.println(new String(data));
                });
        asyncCuratorFramework.setData()
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
        VersionedModeledFramework<NodeConfigInfo> modeledClient = ModeledFramework.wrap(asyncCuratorFramework, spec).versioned();
        modeledClient.set(Versioned.from(nodeConfigInfo, 1));
        log.info("Set data to " + path);
        // todo тут нужно как-то обработать ModelStage.exceptionally
//        modeledClient.withPath().set(nodeConfigInfo);
    }
}
