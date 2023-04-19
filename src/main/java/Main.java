import org.apache.zookeeper.KeeperException;
import zookeeper.ZKManager;
import zookeeper.ZKManagerImpl;

import javax.net.SocketFactory;
import java.io.IOException;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

public class Main {
    public static void main(String[] args) throws IOException, InterruptedException, KeeperException {
        ZKManager manager = new ZKManagerImpl();
//        manager.create("/test", "Hello Zookeeper".getBytes(StandardCharsets.UTF_8));
//        manager.create("/test/one", "I am here".getBytes(StandardCharsets.UTF_8));
        System.out.println(manager.getZNodeData("/MyFirstZNode", false));
        manager.update("/test/one", "UpdateData".getBytes(StandardCharsets.UTF_8));
        System.out.println(manager.getZNodeData("/test/one", false));
//        SocketFactory socketFactory = SocketFactory.getDefault();
//        Socket socket = socketFactory.createSocket("0.0.0.0", 55003);
//        socket.getOutputStream().write("Helo".getBytes(StandardCharsets.UTF_8));
    }
}
