package Zad1;

import java.nio.channels.SocketChannel;
import java.util.HashSet;
import java.util.Set;

public class Client {
    private int id;
    private SocketChannel socketChannel;
    private Set<String> subscribedTopics;

    public Client(int id, SocketChannel socketChannel) {
        this.id = id;
        this.socketChannel = socketChannel;
        this.subscribedTopics = new HashSet<>();
    }

    public int getId() {
        return id;
    }

    public SocketChannel getSocketChannel() {
        return socketChannel;
    }

    public Set<String> getSubscribedTopics() {
        return subscribedTopics;
    }

    public void subscribeTopic(String topic) {
        subscribedTopics.add(topic);
    }

    public void unsubscribeTopic(String topic) {
        subscribedTopics.remove(topic);
    }

    public boolean isSubscribedToTopic(String topic) {
        return subscribedTopics.contains(topic);
    }
}
