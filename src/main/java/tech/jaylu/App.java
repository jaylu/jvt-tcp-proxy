package tech.jaylu;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

public class App {

    private static final Logger log = LoggerFactory.getLogger(App.class);
    private final Config config;
    private final AtomicReference<ServerSocketChannel> serverSocketRef = new AtomicReference<>();
    private boolean isStop = false;

    public App(Config config) {
        this.config = config;
    }

    public void start() throws IOException {
        log.info("tcp proxy server starting with config: {}", this.config);

        ServerSocketChannel serverSocketChannel = ServerSocketChannel.open();
        serverSocketChannel.bind(new InetSocketAddress(config.getFromPort()));

        serverSocketRef.set(serverSocketChannel);
        Thread.startVirtualThread(() -> startServer(serverSocketChannel));
        log.info("tcp proxy server started at {}.", serverSocketChannel.socket().getLocalSocketAddress().toString());
    }

    private void startServer(ServerSocketChannel serverSocketChannel) {
        while (!isStop) {
            try {
                final SocketChannel clientChannel = serverSocketChannel.accept();
                Thread.startVirtualThread(() -> handleClientSocket(clientChannel));
            } catch (IOException e) {
                // TODO: maybe need to stop the server have issue on accepting new connection ?
                log.info("failed to accept new connection: {}", e.getMessage());
            }

        }
    }

    private void handleClientSocket(SocketChannel clientChannel) {
        InetSocketAddress targetAddress = new InetSocketAddress("localhost", config.getToPort());
        try (SocketChannel targetChannel = SocketChannel.open(targetAddress)) {
            CountDownLatch latch = new CountDownLatch(2);
            Thread.startVirtualThread(() -> {
                pipe(clientChannel, targetChannel, "Client -> Target");
                latch.countDown();
            });
            Thread.startVirtualThread(() -> {
                pipe(targetChannel, clientChannel, "Target -> Client");
                latch.countDown();
            });
            latch.await();
        } catch (Exception e) {
            log.info("Error connecting to target socket: {}", e.getMessage());
        } finally {
            safeClose(clientChannel);
        }
    }

    private void pipe(SocketChannel fromChannel, SocketChannel toChannel, String label) {
        try {
            ByteBuffer buffer = ByteBuffer.allocateDirect(8192);
            while (fromChannel.read(buffer) != -1) {
                buffer.flip();
                toChannel.write(buffer);
                buffer.clear();
            }
        } catch (Exception e) {
            log.info(label + " failed to pipe data stream", e);
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
