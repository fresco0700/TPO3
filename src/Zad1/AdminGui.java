package Zad1;

import javafx.application.Application;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import javafx.scene.shape.Line;



public class AdminGui extends Application{


    @Override
    public void start(Stage stage) throws Exception {

        Text title = new Text("Panel administracyjny");
        title.setStyle("-fx-font: 24 arial;");
        GridPane gridPane = new GridPane();
        gridPane.setMinHeight(600);
        gridPane.setMinWidth(500);
        gridPane.setPadding(new Insets(10,10,10,10));

        gridPane.setAlignment(Pos.CENTER);

        Text LabelTopicField = new Text("Nazwa Tematu");
        TextField EntryTopicField = new TextField();
        Text LabelMsgField = new Text("Wpisz wiadomość poniżej");
        TextArea EntryMsgField = new TextArea();

        Line line = new Line(300,0,0,0);
        line.setStrokeWidth(2);
        line.setStyle("-fx-stroke: grey;");
        Line line2 = new Line(300,0,0,0);
        line2.setStrokeWidth(2);
        line2.setStyle("-fx-stroke: grey;");
        Line line3 = new Line(300,0,0,0);
        line3.setStrokeWidth(2);
        line3.setStyle("-fx-stroke: grey;");
        Line line4 = new Line(300,0,0,0);
        line4.setStrokeWidth(2);
        line4.setStyle("-fx-stroke: grey;");

        EntryMsgField.setWrapText(true);
        EntryMsgField.setMaxWidth(250);
        EntryMsgField.setMaxHeight(100);


        gridPane.setVgap(15);
        gridPane.setHgap(15);

        Button SendTopicButton = new Button("Wyślij wiadomość");

        Text LabelEraseField = new Text("Usuń temat");
        Text LabelCreateField = new Text("Dodaj temat");
        TextField EntryEraseField = new TextField();
        TextField EntryCreateField = new TextField();

        Button DownloadTopicButton = new Button("Pobierz aktualne tematy");
        Button SendCreateButton = new Button("Wykonaj");
        Button SendEraseButton = new Button("Wykonaj");

        Text TopicField = new Text();
        ScrollPane TopicsField = new ScrollPane();


        TopicsField.setContent(TopicField);
        TopicsField.setMinWidth(300);
        TopicsField.setMaxWidth(300);


        // column,row
        gridPane.add(title,0,0,3,1);

        gridPane.add(line2,0,1,2,1);

        gridPane.add(LabelTopicField,0,2);
        gridPane.add(EntryTopicField,1,2);

        gridPane.add(LabelMsgField,0,3,2,1);
        gridPane.add(EntryMsgField,0,4,2,1);

        gridPane.add(SendTopicButton,0,5);

        gridPane.add(line,0,6,2,1);

        gridPane.add(DownloadTopicButton,0,7);

        gridPane.add(TopicsField,0,8,2,1);
        gridPane.add(line4,0,9,2,1);
        gridPane.add(LabelCreateField,0,10);
        gridPane.add(EntryCreateField,1,10);
        gridPane.add(SendCreateButton,1,11,3,1);

        gridPane.add(line3,0,12,2,1);

        gridPane.add(LabelEraseField,0,13);
        gridPane.add(EntryEraseField,1,13);
        gridPane.add(SendEraseButton,1,14,3,1);


        setButtonAction(SendTopicButton,() ->{
            sendTopic(EntryTopicField.getText(),EntryMsgField.getText());
        });
        setButtonAction(DownloadTopicButton,() ->{
            TopicField.setText(downloadTopics());
        });
        setButtonAction(SendCreateButton, () -> {
            topicEraseCreate("Temat",false);
        });
        setButtonAction(SendEraseButton,() ->{
            topicEraseCreate("Temat",true);
        });


        Scene scene = new Scene(gridPane);

        stage.setScene(scene);
        stage.setTitle("Panel Administracyjny");
        stage.show();


    }
    private void setButtonAction(Button button, Runnable action) {
        button.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent actionEvent) {
                action.run();
            }
        });
    }

    private void sendTopic(String Topic, String content){
        //wysylanie wiadomosci do serwera
        System.out.println("Temat: "+Topic +" Treść: "+content);
    }

    private String downloadTopics(){
    //Pobieranie tematów z serwera
        return "Tematy z serwera";
    }

    private void topicEraseCreate(String topic,boolean erase){
        if (erase){
            System.out.println("usuwanie");
        }
        else {
            System.out.println("Dodawanie");
        }
    }
}