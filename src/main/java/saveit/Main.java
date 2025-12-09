package saveit;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class Main extends Application {

    private static Stage primaryStage;

    @Override
    public void start(Stage stage) throws Exception {
        primaryStage = stage;
        changeScene("/FXML/login.fxml", "SaveIT - Login");
        primaryStage.show();
    }

    public static void changeScene(String fxmlPath, String title) {
        try {
            FXMLLoader loader = new FXMLLoader(Main.class.getResource(fxmlPath));
            Scene scene = new Scene(loader.load());
            primaryStage.setTitle(title);
            primaryStage.setScene(scene);
            primaryStage.setMaximized(true);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        launch();
    }
}
