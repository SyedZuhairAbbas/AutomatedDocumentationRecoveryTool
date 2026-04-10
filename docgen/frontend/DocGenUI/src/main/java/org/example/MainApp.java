package org.example;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import java.io.File;

public class MainApp extends Application {

    private Process backendProcess;

    @Override
    public void start(Stage stage) throws Exception {
        startBackend();

        FXMLLoader loader = new FXMLLoader(getClass().getResource("/org/example/main.fxml"));
        Scene scene = new Scene(loader.load(), 900, 650);
        stage.setTitle("DocGen — Automated Documentation Recovery Tool");
        stage.setScene(scene);
        stage.show();

        stage.setOnCloseRequest(e -> stopBackend());
    }

    private void startBackend() {
        try {
            // looks for main.exe in the same folder as the jar
            String backendPath = new File("main.exe").getAbsolutePath();
            ProcessBuilder pb = new ProcessBuilder(backendPath);
            pb.redirectErrorStream(true);
            backendProcess = pb.start();
            // give the server 2 seconds to start up
            Thread.sleep(2000);
        } catch (Exception e) {
            System.out.println("Backend start failed: " + e.getMessage());
        }
    }

    private void stopBackend() {
        if (backendProcess != null && backendProcess.isAlive()) {
            backendProcess.destroy();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}