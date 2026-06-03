package com.acadex.fx;

import com.acadex.fx.controller.AppController;
import com.acadex.fx.view.MainView;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class AcadexFxApplication extends Application {
    private AppController controller;

    @Override
    public void start(Stage stage) {
        controller = new AppController();
        MainView mainView = new MainView(controller);
        Scene scene = new Scene(mainView.root(), 1280, 760);
        scene.getStylesheets().add(getClass().getResource("/fx/styles.css").toExternalForm());
        stage.setTitle("Acadex PDF Library");
        stage.setMinWidth(1120);
        stage.setMinHeight(680);
        stage.setScene(scene);
        stage.show();
    }

    @Override
    public void stop() {
        if (controller != null) {
            controller.shutdown();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
