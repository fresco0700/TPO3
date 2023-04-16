package Zad1;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class server {
    private static AtomicInteger clientIdCounter = new AtomicInteger(1);
    private static Map<Integer, Client> clientMap = new HashMap<>();
    private static volatile boolean isRunning = true;
    private static Set<String> Topics = new HashSet<>();
    public static void main(String[] args) {
        try {
            // tworzenie selektora
            Selector selector = Selector.open();

            // kanal do oblslugi zapytan
            ServerSocketChannel clientServerSocketChannel = ServerSocketChannel.open();
            clientServerSocketChannel.bind(new InetSocketAddress("localhost", 8080));
            clientServerSocketChannel.configureBlocking(false);
            clientServerSocketChannel.register(selector, SelectionKey.OP_ACCEPT);

            while (isRunning) {
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


                            int clientId = clientIdCounter.getAndIncrement();
                            // tworzymy obiekt klienta
                            Client client = new Client(clientId, socketChannel);
                            // umieszczamy w hashmapie obiekt clienta z kluczem clientid
                            clientMap.put(clientId, client);
                            // dodajemy obiekt klienta jako zalacznik
                            socketChannel.register(selector, SelectionKey.OP_READ, client);
                            System.out.println("Klient połączony,  ID klienta : " + clientId);

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
                                System.out.println(action+" : "+content);
                                switch (action) {
                                    // Akcje dla klienta
                                    case "subscribe" -> {
                                        System.out.println("INFO Klient o ID "+client.getId()+" zasubskrybował temat" + content);
                                        clientConnected.subscribeTopic(content);
                                    }
                                    case "unsubscribe" -> {
                                        System.out.println("INFO Klient o ID "+client.getId()+" odsubskrybował temat" + content);
                                        clientConnected.unsubscribeTopic(content);
                                    }
                                    case "EXIT" ->{
                                        System.out.println("INFO Zamykam polaczenie z klientem" + client.getId());
                                        socketChannel.close();
                                        clientMap.remove(clientId);
                                    }
                                    // Akcje dla admina
                                    case "addTopic" ->{
                                        System.out.println("Dodajemy temat");
                                        Topics.add(content);
                                        System.out.println(content);
                                        publicBroadcast("Dodano nowy temat: "+content);
                                    }
                                    case "deleteTopic" ->{
                                        System.out.println("Usuwamy temat");
                                        Topics.remove(content);
                                        publicBroadcast("Usunieto temat : "+content);
                                    }
                                    case "Download" ->{
                                        System.out.println("Pobranie istniejacych tematow");
                                        String allTopics = String.join(",",Topics);
                                        if (allTopics.isEmpty()){
                                            sendAnnouncement("Brak tematów",socketChannel);
                                        }
                                        else {
                                            sendAnnouncement(allTopics,socketChannel);
                                        }

                                    }
                                    case "sendToSubscribers" ->{
                                        System.out.println("Wysylanie tresci do zainteresowanych");
                                        String[] parts = content.split("###");
                                        if (parts.length >= 2) {
                                            String title = parts[0];
                                            String text = parts[1];
                                            sendTopicSubscribers(title,text);

                                            sendAnnouncement("Wyslano tresc do zainteresowanych", socketChannel);
                                        }
                                        else {sendAnnouncement("Twój komunikat nie został wczytany, spróbuj ponowanie...",socketChannel);}
                                    }
                                    default -> System.out.println("Taka akcja nie jest dostępna");
                                }

                            } else {
                                System.out.println("Błąd dopasowania");
                            }

                                System.out.println("Klient o ID " + clientConnected.getId()
                                        + " jest zainteresowany tym tamatami : "
                                        + clientConnected.getSubscribedTopics());
                            System.out.println("Tematy w systemie: " + Topics);
                        }
                    }
                    keyIterator.remove();
                }
            }
        } catch (IOException e) {e.printStackTrace();}
    }
    private static void sendTopicSubscribers(String topic, String message) {
        // czytanie wartosci z hashmapy w celu rozeslania tematu otrzymanego przez admina do zainteresowanch
        for (Client client : clientMap.values()) {

            if (client.isSubscribedToTopic(topic)) {

                SocketChannel socketChannel = client.getSocketChannel();
                if (socketChannel.isConnected()) {

                    try {
                        String newsletter = "{\"newsletter\": \"" + message +"\"}";
                        ByteBuffer buffer = ByteBuffer.wrap(newsletter.getBytes());
                        socketChannel.write(buffer);
                        System.out.println("Wysłano wiadomość do klienta o ID " + client.getId() + ": " + message);
                    }catch (IOException e) {e.printStackTrace();}

                }else {
                    System.out.println("Wystapil blad z polaczeniem z "+ client.getId());
                }
            }
        }
    }
    private static void publicBroadcast(String message){
            for (Client client : clientMap.values()){

                SocketChannel socketChannel = client.getSocketChannel();
                if (socketChannel.isConnected()){
                    try {
                        String newsletter = "{\"newsletter\": \"" + message +"\"}";
                        ByteBuffer buffer = ByteBuffer.wrap(newsletter.getBytes());

                        socketChannel.write(buffer);
                        System.out.println("Wyslano powiadomienie do " + client.getId());
                    }catch (IOException e){ e.printStackTrace();}
                }else {
                    System.out.println("Klient o ID " +client.getId()+" nie jest połączony");
                }
            }
    }
    private static void sendAnnouncement(String content,SocketChannel socketChannel) throws IOException {
        ByteBuffer responseBuffer = ByteBuffer.wrap(content.getBytes());
        socketChannel.write(responseBuffer);

    }

}
