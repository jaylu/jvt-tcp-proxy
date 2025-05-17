package tech.jaylu;

public class Config {

    private final int fromPort;
    private final int toPort;

    public Config(int fromPort, int toPort) {
        this.fromPort = fromPort;
        this.toPort = toPort;
    }

    public int getFromPort() {
        return fromPort;
    }

    public int getToPort() {
        return toPort;
    }

    @Override
    public String toString() {
        return "Config{" +
                "fromPort=" + fromPort +
                ", toPort=" + toPort +
                '}';
    }
}
