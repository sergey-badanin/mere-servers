package io.bsy.servers;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.concurrent.Executors;
import java.util.logging.Logger;

/*
 * TODO: Make connections to deregister from selector 
 */
public class Echo {

    private final static Logger logger = Logger.getLogger(Echo.class.getName());

    public static void main(String[] args) throws IOException {

        try (var serverSocketChannel = ServerSocketChannel.open();
                var selector = Selector.open()) {
            serverSocketChannel.bind(new InetSocketAddress(8091));

            logger.info("Starting server");
            Executors.newSingleThreadExecutor().submit(() -> Echo.process(selector));
            // new Timer(true).scheduleAtFixedRate(new TimerTask() {
            // public void run() {
            // logger.info("Selector has keys:" + selector.keys().size());
            // }
            // }, 5000, 5000);

            for (;;) {
                logger.info("Waiting for connection");
                var channel = serverSocketChannel.accept();
                logger.info("New connection accepted");

                channel.configureBlocking(false);
                var key = channel.register(selector, SelectionKey.OP_READ | SelectionKey.OP_WRITE);
                var buf = ByteBuffer.allocate(1024);
                var bufState = new BufferState(buf, false);
                key.attach(bufState);

                selector.wakeup();
            }
        } catch (Exception e) {
            logger.warning(String.format("Exception: %s", e));
        }
    }

    public static void process(Selector selector) {
        for (;;) {
            try {
                // logger.info("Waiting for selector");
                selector.select();
                var keys = selector.selectedKeys();

                for (var k : keys) {
                    var bs = (BufferState) k.attachment();
                    var bf = bs.buffer();
                    var sc = (SocketChannel) k.channel();

                    if (k.isReadable() && !bs.hasData()) {
                        var readyBytes = sc.read(bf);
                        if (readyBytes > 0) {
                            bs.hasData(true);
                            bf.flip();
                        }
                    }
                    if (k.isWritable() && bs.hasData()) {
                        var readyBytes = bf.limit();
                        var writtenBytes = sc.write(bf);
                        if (writtenBytes == readyBytes) {
                            bs.hasData(false);
                            bf.clear();
                        }
                    }
                }
            } catch (IOException e) {
                logger.warning(e.getMessage());
                e.printStackTrace();
            }
        }
    }

    static class BufferState {

        private ByteBuffer buffer;
        private boolean hasData;

        BufferState(ByteBuffer buffer, boolean hasData) {
            this.buffer = buffer;
            this.hasData = hasData;
        }

        public ByteBuffer buffer() {
            return buffer;
        }

        public void buffer(ByteBuffer buffer) {
            this.buffer = buffer;
        }

        public boolean hasData() {
            return hasData;
        }

        public void hasData(boolean hasData) {
            this.hasData = hasData;
        }
    }
}
