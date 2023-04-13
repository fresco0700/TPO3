package Zad1;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

public class server {
    private static AtomicInteger clientIdCounter = new AtomicInteger(1);
    private static Map<Integer, SocketChannel> clientMap = new HashMap<>();

    public static void main(String[] args) {
        try {
            // Utwórz selektor
            Selector selector = Selector.open();

            // Utwórz serwerowy kanał socketów dla klientów
            ServerSocketChannel clientServerSocketChannel = ServerSocketChannel.open();
            clientServerSocketChannel.bind(new InetSocketAddress("localhost", 8080));
            clientServerSocketChannel.configureBlocking(false);
            clientServerSocketChannel.register(selector, SelectionKey.OP_ACCEPT);

            // Utwórz serwerowy kanał socketów dla administratora
            ServerSocketChannel adminServerSocketChannel = ServerSocketChannel.open();
            adminServerSocketChannel.bind(new InetSocketAddress("localhost", 8081));
            adminServerSocketChannel.configureBlocking(false);
            adminServerSocketChannel.register(selector, SelectionKey.OP_ACCEPT);

            // Główna pętla serwera
            while (true) {
                // Blokowanie, aż będzie co najmniej jeden kanał gotowy
                selector.select();

                Set<SelectionKey> selectedKeys = selector.selectedKeys();
                Iterator<SelectionKey> keyIterator = selectedKeys.iterator();

                while (keyIterator.hasNext()) {
                    SelectionKey key = keyIterator.next();

                    if (key.isAcceptable()) {
                        // Akceptujemy nowe połączenie
                        ServerSocketChannel serverChannel = (ServerSocketChannel) key.channel();
                        SocketChannel socketChannel = serverChannel.accept();
                        socketChannel.configureBlocking(false);
                        socketChannel.register(selector, SelectionKey.OP_READ);

                        // Sprawdź, czy to połączenie administratora
                        if (serverChannel == adminServerSocketChannel) {
                            System.out.println("Administrator połączony");
                        } else {
                            int clientId = clientIdCounter.getAndIncrement();
                            clientMap.put(clientId, socketChannel);
                            System.out.println("Klient połączony, ID klienta: " + clientId);

                        }
                    } else if (key.isReadable()) {
                        // Odczytujemy dane
                        SocketChannel socketChannel = (SocketChannel) key.channel();
                        ByteBuffer buffer = ByteBuffer.allocate(256);
                        int bytesRead = socketChannel.read(buffer);

                        if (bytesRead == -1) {
                            socketChannel.close();
                        } else {
                            buffer.flip();
                            String receivedMessage = new String(buffer.array()).trim();
                            System.out.println("Odebrane wiadomości: " + receivedMessage);

                            // Odpowiedź na wiadomość
                            String response = "Serwer otrzymał: " + receivedMessage;
                            ByteBuffer responseBuffer = ByteBuffer.wrap(response.getBytes());
                            socketChannel.write(responseBuffer);
                        }
                    }

                    keyIterator.remove();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }


    }
    private static void sendMsgToClient(int id,String message){
        SocketChannel socketChannel = clientMap.get(id);
        try {
            ByteBuffer buffer = ByteBuffer.wrap(message.getBytes());
            socketChannel.write(buffer);
        }catch (IOException e){e.printStackTrace();}
    }
}
