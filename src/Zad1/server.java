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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class server {
    private static AtomicInteger clientIdCounter = new AtomicInteger(1);
    private static Map<Integer, Client> clientMap = new HashMap<>();


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
                            Client client = new Client(clientId, socketChannel);
                            clientMap.put(clientId, client);
                            socketChannel.register(selector, SelectionKey.OP_READ, client); // Dodajemy obiekt Client jako załącznik
                            System.out.println("Klient połączony, ID klienta: " + clientId);
                        }
                    } else if (key.isReadable()) {
                        // Odczytujemy dane
                        SocketChannel socketChannel = (SocketChannel) key.channel();
                        Client client = (Client) key.attachment(); // Pobieramy obiekt Client jako załącznik
                        int clientId = client.getId(); // Pobieramy identyfikator klienta

                        ByteBuffer buffer = ByteBuffer.allocate(256);
                        int bytesRead = socketChannel.read(buffer);

                        if (bytesRead == -1) {
                            socketChannel.close();
                        } else {
                            buffer.flip();
                            String receivedMessage = new String(buffer.array()).trim();
                            System.out.println("Odebrane wiadomości od klienta " + clientId + ": " + receivedMessage);
                            Client clientid = clientMap.get(clientId);

                            String regexPattern = "\\{\"(.*?)\": \"(.*?)\"\\}";
                            Pattern pattern = Pattern.compile(regexPattern);
                            Matcher matcher = pattern.matcher(receivedMessage);

                            if (matcher.find()) {
                                String action = matcher.group(1);
                                String content = matcher.group(2);

                                switch(action) {
                                    case "subscribe":
                                        System.out.println("Subskrybujemy temat");
                                        clientid.subscribeTopic(content);
                                        break;
                                    case "unsubscribe":
                                        System.out.println("Usuwamy temat");
                                        clientid.unsubscribeTopic(content);
                                        break;
                                    default:
                                        System.out.println("Nie ma takiego kejsa");
                                }

                            }else{
                                System.out.println("Błąd dopasowania");
                            }


                            System.out.println(clientid.getSubscribedTopics());

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
    private static void sendMessageToClient(int clientId, String message) {
        Client client = clientMap.get(clientId);
        if (client != null) {
            SocketChannel clientSocketChannel = client.getSocketChannel();

            if (clientSocketChannel.isConnected()) {
                try {
                    ByteBuffer buffer = ByteBuffer.wrap(message.getBytes());
                    clientSocketChannel.write(buffer);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else {
                System.out.println("Klient o ID " + clientId + " nie jest połączony.");
            }
        } else {
            System.out.println("Klient o ID " + clientId + " nie istnieje.");
        }
    }
}
