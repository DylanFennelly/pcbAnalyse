package pcb.pcb;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.Objects;

public class PCBAnalyseApp extends Application {
    @Override
    public void start(Stage stage) throws IOException {
        Parent p = FXMLLoader.load(Objects.requireNonNull(PCBAnalyseApp.class.getResource("start-view.fxml")));
        Scene scene = new Scene(p);
        stage.setTitle("PCB Analyse");
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch();
    }
}