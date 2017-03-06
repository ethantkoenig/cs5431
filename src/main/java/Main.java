import java.util.Random;

public class Main {

    private static final Random RANDOM = new Random();

    public static void main(String[] args) throws Exception {
        // Just junk example of testing
        Miner miner = new Miner(4446);
        miner.startMiner();
    }
}
