package rarsreborn;

public class Main {
    public static void main(String[] args) {
        Thread interfaceThread = new Thread(new RarsApplication());
        interfaceThread.start();
    }
}
