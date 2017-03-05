import java.net.InetSocketAddress;
import java.util.ArrayList;

public class Main {
    public static void main(String[] args) throws Exception {
        Miner miner = new Miner(4444);
        miner.startMiner();

        Miner miner2 = new Miner(4445);
        ArrayList<InetSocketAddress> hosts = new ArrayList<>();
        hosts.add(new InetSocketAddress("localhost", 4444));
        miner2.connectAll(hosts);

    }
}
