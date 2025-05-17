package tech.jaylu;

import io.muserver.MuServer;
import io.muserver.MuServerBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class RunLocal {

    private static final Logger log = LoggerFactory.getLogger(RunLocal.class);

    public static void main(String[] args) throws IOException {

        Config config = new Config(8080, 8081);
        App app = new App(config);
        app.start();

        startLocalServer(8081);

        Runtime.getRuntime().addShutdownHook(new Thread(app::stop));
    }

    private static void startLocalServer(int port) {
        MuServer server = MuServerBuilder.muServer()
                .withHttpPort(port)
                .addHandler((request, response) -> {
                    response.status(200);
                    response.write("hello world!");
                    return true;
                })
                .start();

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            server.stop();
            log.info("mock server stopped.");
        }));
        log.info("mock server started at {}", server.uri());
    }
}
