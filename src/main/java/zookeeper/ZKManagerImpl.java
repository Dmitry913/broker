package zookeeper;

import lombok.extern.slf4j.Slf4j;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.ZooDefs;
import org.apache.zookeeper.ZooKeeper;

import java.nio.charset.StandardCharsets;

@Slf4j
public class ZKManagerImpl implements ZKManager {
    private ZooKeeper zooKeeper;
    private ZKConnection zkConnection;

    public ZKManagerImpl() {
        zkConnection = new ZKConnection();
        try {
            zooKeeper = zkConnection.connect();
        } catch (Exception e) {
            log.error("Some error while connected", e);
        }
    }

    @Override
    public void create(String path, byte[] data) throws InterruptedException, KeeperException {
        zooKeeper.create(path,
                data,
                // ACL - уровень доступа к объекту (кто может получать доступ к нему)
                // в данном случае доступ открыт всем
                ZooDefs.Ids.OPEN_ACL_UNSAFE,
                CreateMode.PERSISTENT);
    }

    @Override
    public String getZNodeData(String path, boolean watchFlag) throws InterruptedException, KeeperException {
        return new String(zooKeeper.getData(path, false, null), StandardCharsets.UTF_8);
    }

    @Override
    public void update(String path, byte[] data) throws InterruptedException, KeeperException {
        zooKeeper.setData(path, data, zooKeeper.exists(path, true).getVersion());
    }

    @Override
    public void delete(String path) throws InterruptedException, KeeperException {
        zooKeeper.delete(path, zooKeeper.exists(path, false).getVersion());
    }
}
