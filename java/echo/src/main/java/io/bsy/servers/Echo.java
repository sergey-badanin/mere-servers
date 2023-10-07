package io.bsy.servers;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.util.logging.Logger;

/*
 * https://jenkov.com/tutorials/java-nio/selectors.html
 * https://docs.oracle.com/javase/8/docs/api/java/nio/package-summary.html
 * 
 */

public class Echo {

    private final static Logger logger = Logger.getLogger(Echo.class.getName());

    public static void main(String[] args) throws IOException {

        try (var serverSocketChannel = ServerSocketChannel.open();
                var selector = Selector.open()) {
            serverSocketChannel.bind(new InetSocketAddress(8091));

            for (;;) {
                var channel = serverSocketChannel.accept();

                var buf = ByteBuffer.allocate(1024);
                channel.configureBlocking(false);
                var key = channel.register(selector, SelectionKey.OP_READ | SelectionKey.OP_WRITE);
                key.attach(buf);

                selector.select();
                var keys = selector.selectedKeys();

                for (var k : keys) {
                    if (k.isWritable()) {
                        var bf = (ByteBuffer)k.attachment();
                        bf.hasRemaining();
                    }
                    
                }

            }
        } catch (Exception e) {
            logger.warning(String.format("Exception: %s", e));
        }
    }
}
