package zookeeper;

import lombok.extern.slf4j.Slf4j;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooKeeper;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.CountDownLatch;


@Slf4j
public class ZKConnection {
    private ZooKeeper zooKeeper;
    private CountDownLatch connectionLatch = new CountDownLatch(1);
    private final String zKHost;
    private final int zKPort;

    public ZKConnection() {
        Map<String,String> env = System.getenv();
        zKHost = env.get("ZK_HOST");
        zKPort = Integer.parseInt(env.get("ZK_PORT"));
    }

    public ZooKeeper connect() throws IOException, InterruptedException {
        // Singleton
        if (zooKeeper == null) {
            log.info(String.format("Connect to %s:%d\n", zKHost, zKPort));
            zooKeeper = new ZooKeeper(String.format("%s:%d", zKHost, zKPort), 2000, watchedEvent -> {
                if (watchedEvent.getState() == Watcher.Event.KeeperState.SyncConnected) {
                    log.info("Success connected");
                    connectionLatch.countDown();
                }
            });
        }
        connectionLatch.await();
        return zooKeeper;
    }

    public void close() throws InterruptedException {
        zooKeeper.close();
        log.info("Connection close");
    }
}
