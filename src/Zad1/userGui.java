package Zad1;

import javafx.application.Application;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.shape.Line;
import javafx.scene.text.Text;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;


public class userGui extends Application {

    SocketChannel socketChannel;

    @Override
    public void start(Stage stage) throws Exception {

        GridPane gridPane = new GridPane();
        gridPane.setMinHeight(400);
        gridPane.setMaxWidth(500);
        gridPane.setMinWidth(400);
        gridPane.setPadding(new Insets(10,10,10,10));
        gridPane.setAlignment(Pos.CENTER);
        gridPane.setVgap(15);
        gridPane.setHgap(15);
        Text title = new Text("Panel użytkownika");
        title.setStyle("-fx-font: 24 arial;");
        Text subscribeText = new Text("Zapisz się");
        TextField subscribeEntry = new TextField();
        Button subscribeButton = new Button("Wykonaj");
        Text unsubscribeText = new Text("Wypisz się");
        TextField unsubscribeEntry = new TextField();
        Button unsubscribeButton = new Button("Wykonaj");
        Line line = new Line(200,0,0,0);
        line.setStrokeWidth(2);
        line.setStyle("-fx-stroke: grey;");
        Line line1 = new Line(200,0,0,0);
        line1.setStrokeWidth(2);
        line1.setStyle("-fx-stroke: grey;");
        Text info = new Text("Newsletter");
        info.setStyle("-fx-font: 18 arial;");
        Text scrollPaneText = new Text();
        ScrollPane scrollPane = new ScrollPane();
        scrollPane.setContent(scrollPaneText);
        scrollPane.setMinWidth(200);
        scrollPane.setMaxWidth(200);
        scrollPane.setMinHeight(150);


        gridPane.add(title,1,0,2,1);

        gridPane.add(line,1,1,2,1);

        gridPane.add(subscribeText,0,2);
        gridPane.add(subscribeEntry,1,2);
        gridPane.add(subscribeButton,2,2);

        gridPane.add(unsubscribeText,0,3);
        gridPane.add(unsubscribeEntry,1,3);
        gridPane.add(unsubscribeButton,2,3);

        gridPane.add(line1,1,4,2,1);

        gridPane.add(info,1,5);
        gridPane.add(scrollPane,1,6);

        setButtonAction(subscribeButton,()->{
            subscribeTopic(subscribeEntry.getText());
            subscribeEntry.clear();
        });
        setButtonAction(unsubscribeButton,()->{
            unsubscribeTopic(unsubscribeEntry.getText());
            unsubscribeEntry.clear();
        });


        Scene scene = new Scene(gridPane);
        stage.setTitle("Panel Użytkownika");
        stage.setScene(scene);
        stage.show();


        try{
            // Laczenie z serwerem przy pomocy kanałów
            socketChannel = SocketChannel.open();
            socketChannel.connect(new InetSocketAddress("localhost",8080));

        }catch (IOException e){
            e.printStackTrace();
        }
    }

    private void setButtonAction(Button button, Runnable action) {
        button.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent actionEvent) {
                action.run();
            }
        });
    }

    private void subscribeTopic(String topicToSubscribe){
        String request = "{\"subscribe\": \"" + topicToSubscribe +"\"}";
        sendMessage(request);
    }

    private void unsubscribeTopic(String topicToUnSubscribe){
        String request = "{\"unsubscribe\": \"" + topicToUnSubscribe +"\"}";
        sendMessage(request);
    }
    private void sendMessage(String message){
        try{
            ByteBuffer reqByteBuffer = ByteBuffer.wrap(message.getBytes());
            socketChannel.write(reqByteBuffer);
            reqByteBuffer.clear();

            ByteBuffer resByteBuffer = ByteBuffer.allocate(2048);
            int bytesread = socketChannel.read(resByteBuffer);

            if (bytesread > 0){
                resByteBuffer.flip();
                String response = new String(resByteBuffer.array()).trim();
                System.out.println(response);
            }

        }catch (IOException e){
            e.printStackTrace();
        }


    }

    @Override
    public void stop(){

        try {
            socketChannel.close();
        }catch (IOException e) {e.printStackTrace();}
    }

}
