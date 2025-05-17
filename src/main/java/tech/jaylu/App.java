package tech.jaylu;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

public class App {

    private static final Logger log = LoggerFactory.getLogger(App.class);
    private final Config config;
    private final AtomicReference<ServerSocket> serverSocketRef = new AtomicReference<>();
    private boolean isStop = false;

    public App(Config config) {
        this.config = config;
    }

    public void start() throws IOException {
        log.info("tcp proxy server starting with config: {}", this.config);
        ServerSocket serverSocket = new ServerSocket(config.getFromPort());
        serverSocketRef.set(serverSocket);
        Thread.startVirtualThread(() -> startServer(serverSocket));
        log.info("tcp proxy server started at {}.", serverSocket.getLocalSocketAddress().toString());
    }

    private void startServer(ServerSocket serverSocket) {
        while (!isStop) {
            try {
                final Socket client = serverSocket.accept();
                Thread.startVirtualThread(() -> handleClientSocket(client));
            } catch (IOException e) {
                // TODO: maybe need to stop the server have issue on accepting new connection ?
                log.info("failed to accept new connection: {}", e.getMessage());
            }

        }
    }

    private void handleClientSocket(Socket client) {
        try (Socket target = new Socket("localhost", config.getToPort())) {
            CountDownLatch latch = new CountDownLatch(2);
            Thread.startVirtualThread(() -> {
                pipe(client, target, "Client -> Target");
                latch.countDown();
            });
            Thread.startVirtualThread(() -> {
                pipe(target, client, "Target -> Client");
                latch.countDown();
            });
            latch.await();
        } catch (Exception e) {
            log.info("Error connecting to target socket: {}", e.getMessage());
        } finally {
            safeClose(client);
        }
    }

    private void pipe(Socket from, Socket to, String label) {
        byte[] buffer = new byte[8192];
        try  {
            InputStream input = from.getInputStream();
            OutputStream output = to.getOutputStream();
            int readCount;
            while ((readCount = input.read(buffer)) != -1) {
                output.write(buffer, 0, readCount);
                output.flush();
            }
        } catch (Exception e) {
            log.info(label + "  failed to pipe data stream", e);
        }
    }

    public void stop() {
        log.info("tcp proxy server stopping.");

        isStop = true;
        safeClose(serverSocketRef.get());
        log.info("server socket closed.");
        log.info("tcp proxy server stopped.");
    }


    private static <T extends Closeable> T safeClose(T closeable) {
        if (closeable == null) return null;
        try {
            closeable.close();
        } catch (IOException e) {
            log.info("failed to close {}", e.getMessage());
        }
        return closeable;
    }
}
