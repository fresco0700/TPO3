package Zad1;

import java.io.IOException;
import java.net.InetSocketAddress;
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
            // tworzenie selektora
            Selector selector = Selector.open();

            // kanal dla klienta
            ServerSocketChannel clientServerSocketChannel = ServerSocketChannel.open();
            clientServerSocketChannel.bind(new InetSocketAddress("localhost", 8080));
            clientServerSocketChannel.configureBlocking(false);
            clientServerSocketChannel.register(selector, SelectionKey.OP_ACCEPT);

            // kanal dla admina
            ServerSocketChannel adminServerSocketChannel = ServerSocketChannel.open();
            adminServerSocketChannel.bind(new InetSocketAddress("localhost", 8081));
            adminServerSocketChannel.configureBlocking(false);
            adminServerSocketChannel.register(selector, SelectionKey.OP_ACCEPT);

            while (true) {
                selector.select();

                Set<SelectionKey> selectedKeys = selector.selectedKeys();
                Iterator<SelectionKey> keyIterator = selectedKeys.iterator();

                while (keyIterator.hasNext()) {
                    SelectionKey key = keyIterator.next();

                    if (key.isAcceptable()) {
                        // akceptacja nowego polaczenia
                        ServerSocketChannel serverChannel = (ServerSocketChannel) key.channel();
                        SocketChannel socketChannel = serverChannel.accept();
                        socketChannel.configureBlocking(false);
                        socketChannel.register(selector, SelectionKey.OP_READ);

                        if (serverChannel == adminServerSocketChannel) {
                            System.out.println("Administrator połączony");

                        } else {
                            int clientId = clientIdCounter.getAndIncrement();
                            // tworzymy obiekt klienta
                            Client client = new Client(clientId, socketChannel);
                            // umieszczamy w hashmapie obiekt clienta z kluczem clientid
                            clientMap.put(clientId, client);
                            // dodajemy obiekt klienta jako zalacznik
                            socketChannel.register(selector, SelectionKey.OP_READ, client);
                            System.out.println("Klient połączony,  ID klienta : " + clientId);
                        }
                    } else if (key.isReadable()) {
                        // Kod nizej sluzy do czytania wiadomosci


                        SocketChannel socketChannel = (SocketChannel) key.channel();
                        // pobieramy obiekt clienta jako zalacznik aby poźniej móc się do niego odnieść
                        Client client = (Client) key.attachment();
                        // pobieramy identyfikator klienta aby pozniej odroznic od kogo byla wiadomosc
                        int clientId = client.getId();

                        ByteBuffer buffer = ByteBuffer.allocate(256);
                        int bytesRead = socketChannel.read(buffer);

                        if (bytesRead == -1) {
                            socketChannel.close();
                        } else {
                            buffer.flip();
                            String receivedMessage = new String(buffer.array()).trim();
                            System.out.println("odebrana wiadomosc od klienta o ID " + clientId + ": " + receivedMessage);
                            Client clientConnected = clientMap.get(clientId);

                            String regexPattern = "\\{\"(.*?)\": \"(.*?)\"\\}";
                            Pattern pattern = Pattern.compile(regexPattern);
                            Matcher matcher = pattern.matcher(receivedMessage);

                            if (matcher.find()) {
                                String action = matcher.group(1);
                                String content = matcher.group(2);

                                switch (action) {
                                    case "subscribe" -> {
                                        System.out.println("Subskrybujemy temat");
                                        clientConnected.subscribeTopic(content);
                                    }
                                    case "unsubscribe" -> {
                                        System.out.println("Usuwamy temat");
                                        clientConnected.unsubscribeTopic(content);
                                    }
                                    default -> System.out.println("Taka akcja nie jest dostępna");
                                }

                            }else{
                                System.out.println("Błąd dopasowania");
                            }


                            System.out.println("Klient o ID " + clientConnected.getId()
                                    + " jest zainteresowany tym tamatami : "
                                    + clientConnected.getSubscribedTopics());

                            // Odpowiedź na wiadomość
                            String response = "Operacja wykonana poprawnie";

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
    private static void sendToTopicSubscribers(String topic, String message) {
        // czytanie wartosci z hashmapy w celu rozeslania tematu otrzymanego przez admina do zainteresowanch
        for (Client client : clientMap.values()) {

            if (client.isSubscribedToTopic(topic)) {

                SocketChannel clientSocketChannel = client.getSocketChannel();

                if (clientSocketChannel.isConnected()) {

                    try {
                        String newsletter = "{\"newsletter\": \"" + message +"\"}";
                        ByteBuffer buffer = ByteBuffer.wrap(newsletter.getBytes());
                        clientSocketChannel.write(buffer);
                        System.out.println("Wysłano wiadomość do klienta o ID " + client.getId() + ": " + message);
                    }catch (IOException e) {e.printStackTrace();}

                }else {
                    System.out.println("Wystapil blad z polaczeniem z "+ client.getId());
                }
            }
        }
    }
}
