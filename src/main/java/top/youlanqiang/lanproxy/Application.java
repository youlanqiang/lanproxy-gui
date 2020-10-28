package top.youlanqiang.lanproxy;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class Application extends javafx.application.Application {


    @Override
    public void start(Stage primaryStage) throws Exception{
        Parent root = FXMLLoader.load(getClass().getResource("sample.fxml"));
        primaryStage.setTitle("波塞冬内网穿透工具");
        primaryStage.setScene(new Scene(root, 400, 400));

        primaryStage.show();
    }

}
