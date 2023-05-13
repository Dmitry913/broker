package org.wasend.broker.zookeeper;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.x.async.AsyncCuratorFramework;
import org.apache.curator.x.async.modeled.JacksonModelSerializer;
import org.apache.curator.x.async.modeled.ModelSpec;
import org.apache.curator.x.async.modeled.ModeledFramework;
import org.apache.curator.x.async.modeled.ZNode;
import org.apache.curator.x.async.modeled.ZPath;
import org.apache.curator.x.async.modeled.details.ZNodeImpl;
import org.apache.curator.x.async.modeled.versioned.Versioned;
import org.apache.curator.x.async.modeled.versioned.VersionedModeledFramework;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.data.Stat;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.ApplicationEventMulticaster;
import org.springframework.stereotype.Component;
import org.wasend.broker.dao.entity.MetaInfoZK;
import org.wasend.broker.dao.entity.NodeInfo;
import org.wasend.broker.eventObjects.Children;
import org.wasend.broker.eventObjects.MetaInfo;

import javax.xml.soap.Node;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
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
    private final CuratorFramework syncCuratorFramework;
    // todo можно определять корневую директорию самому на этапе создания объекта данного класса
    private final String rootDirectory = "/root";
    private final String directoryInstanceName;
    private final ApplicationEventPublisher applicationEventPublisher;
    private final ApplicationEventMulticaster multicaster;
    private final String prefixEphemeralNode;
    private final ObjectMapper objectMapper;

    @Autowired
    public CuratorZooKeeperImpl(AsyncCuratorFramework asyncCuratorFramework,
                                ApplicationEventPublisher applicationEventPublisher,
                                CuratorFramework syncCuratorFramework,
                                ApplicationEventMulticaster multicaster,
                                @Value("${prefix.ephemeral.node}") String prefixEphemeralNode,
                                @Value("${external.node.host}") String externalHost,
                                @Value("${external.node.port}") String externalPort) {
        this.asyncCuratorFramework = asyncCuratorFramework;
        this.applicationEventPublisher = applicationEventPublisher;
        this.syncCuratorFramework = syncCuratorFramework;
        this.objectMapper = new ObjectMapper();
        this.prefixEphemeralNode = prefixEphemeralNode;
        this.multicaster = multicaster;
        // todo создать ноду в директории cluster, как только приложение запустилось (nodeId данного узла взять из zooKeeper.current.nodeId)
        directoryInstanceName = createEphemeralNode(externalHost, externalPort);
        log.info("EphemeralNode success created with nodeId - " + directoryInstanceName);
        createChildrenWatcher();
        createRootDirectoryWatcher();
    }


    @Override
    public MetaInfo getRootInfo() throws Exception {
        try {
            // Todo возможно синхронизация не нужна (в целом это затратная операция)
            syncCuratorFramework.sync().forPath(rootDirectory);
            log.info("Start getting rooInfo from ZooKeeper");
            ZNode<MetaInfoZK> zNode = getData(rootDirectory, MetaInfoZK.class);
            log.info("Success reading rootInfo(version={}):{}", zNode.stat().getVersion(), zNode.model().toString());
            return new MetaInfo(this, zNode.stat().getVersion(), zNode.model());
        } catch (Exception e) {
            log.error("Can't read info from zooKeeper by path: " + rootDirectory);
            throw new Exception(e);
        }
    }

    @Override
    public NodeInfo getNodeInfoByDirectory(String directory) {
        String correctDirectory = rootDirectory + "/" + directory;
        try {
            return getData(correctDirectory, NodeInfo.class).model();
        } catch (Exception e) {
            log.error("Can't read info from zooKeeper by path <{}>", correctDirectory, e);
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

    @Override
    public List<String> getAllEphemeralNodeDirectory() {
        return getChildrenDirectory(rootDirectory);
    }

    private List<String> getChildrenDirectory(String directory) {
        try {
            return syncCuratorFramework
                    .getChildren()
                    .forPath(directory);
        } catch (Exception e) {
            log.error("Failed getting child-nodes for directory<{}>", directory, e);
            throw new RuntimeException(e);
        }
    }

    // создаёт путь с такими данными или обновляет существующие
    private <T> boolean setData(String path, T info, int version) {
//        ModelSpec<T> spec = ModelSpec.builder(
//                        ZPath.parseWithIds(path),
//                        JacksonModelSerializer.build((Class<T>) info.getClass()))
//                .build();
//        VersionedModeledFramework<T> versionedModeledClient = ModeledFramework.wrap(asyncCuratorFramework, spec).versioned();
        log.info("Set data to " + path);
        try {
            syncCuratorFramework.setData().withVersion(version)
                    .forPath(path,
                            objectMapper.writerFor(info.getClass()).writeValueAsBytes(info));
            log.info("Succes set data to " + path);
            return true;
        } catch (Exception e) {
            log.error("Failed while set data to {}", path, e);
            return false;
        }
//        // todo тут нужно как-то обработать ModelStage.exceptionally
//        try {
//            versionedModeledClient.set(Versioned.from(info, version)).toCompletableFuture().get();
//            log.info("Succes set data to " + path);
//            return true;
//        } catch (Exception e) {
//            log.error("Failed while set data to {}", path, e);
//            return false;
//        }
    }

    private <T> ZNode<T> getData(String path, Class<T> tClass) throws Exception {
        Stat stat = new Stat();
        byte[] readBytes = syncCuratorFramework.getData().storingStatIn(stat).forPath(path);
        return new ZNodeImpl<>(ZPath.parse(path), stat, objectMapper.readValue(readBytes, tClass));
//        ModelSpec<T> spec = ModelSpec.builder(
//                        ZPath.parseWithIds(path),
//                        JacksonModelSerializer.build(tClass))
//                .build();
//        ModeledFramework<T> modeledClient = ModeledFramework.wrap(asyncCuratorFramework, spec);
//        return modeledClient.readAsZNode().toCompletableFuture().get();
    }

    private void createRootDirectoryWatcher() {
        asyncCuratorFramework.watched()
                .getData()
                .forPath(rootDirectory)
                .event()
                .thenAccept(watchedEvent -> {
                    if (watchedEvent.getType().equals(NodeDataChanged)) {
                        log.info("Received event: rootDirectoryUpdated");
                        try {
                            multicaster.multicastEvent(getRootInfo());
//                            applicationEventPublisher.publishEvent(getRootInfo());
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
                            log.info("Ephemeral children changes");
                            applicationEventPublisher.publishEvent(new Children(getChildrenDirectory(rootDirectory)));
//                            applicationEventPublisher.publishEvent(syncCuratorFramework
//                                    .getChildren()
//                                    .forPath(rootDirectory)
//                                    .stream();
                        } catch (Exception e) {
                            log.error("Failed getting instances changing", e);
                        }
                    }
                    log.info("here createChildrenWatcher");
                    createChildrenWatcher();
                });
    }

    private String createEphemeralNode(String externalHost, String externalPort) {
        try {
            String nodeId = prefixEphemeralNode + UUID.randomUUID();
            NodeInfo myInfo = NodeInfo.builder()
                    .host(externalHost)
                    .port(Integer.parseInt(externalPort))
                    .nodeId(nodeId)
                    .build();
            byte[] nodeInfoInBytes = objectMapper.writerFor(NodeInfo.class).writeValueAsBytes(myInfo);
            String directory = syncCuratorFramework
                    .create()
                    .withMode(CreateMode.EPHEMERAL)
                    .forPath(rootDirectory + "/" + nodeId, nodeInfoInBytes)
                    .split("/")[2];
            log.info("EphemeralNode<{}> success created", nodeId);
            return directory;
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
        // todo тут нужно как-то обработать ModelStage.exceptionally
//        modeledClient.withPath().set(nodeConfigInfo);
    }
}
