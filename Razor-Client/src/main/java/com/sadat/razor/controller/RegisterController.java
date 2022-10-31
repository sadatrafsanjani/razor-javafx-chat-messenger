package com.sadat.razor.controller;

import com.sadat.razor.client.Client;
import com.sadat.razor.client.Connection;
import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;

public class RegisterController implements Initializable {

    @FXML
    private TextField usernameField;
    @FXML
    private PasswordField passwordField;
    @FXML
    private Label errorLabel;
    private String username, password;

    private Client client;
    private Stage loadingStage;

    @Override
    public void initialize(URL url, ResourceBundle rb) {

    }

    private void updateSuccessView() {

        Platform.runLater(new Runnable() {
            @Override
            public void run() {

                loadingStage.hide();
                Stage currentStage = (Stage) errorLabel.getScene().getWindow();
                currentStage.hide();
                openSuccessWindow("Registration Successful!");
                client.closeConnection();
            }
        });
    }
    
    private void updateFailedView() {

        Platform.runLater(new Runnable() {
            @Override
            public void run() {

                usernameField.setText("");
                passwordField.setText("");
                errorLabel.setText("Username in Use Already!");
                loadingStage.hide();
            }
        });
    }
    
    private void openSuccessWindow(String message) {

        try {

            FXMLLoader loader = new FXMLLoader((getClass().getResource("/fxml/Success.fxml")));
            SuccessController controller = new SuccessController();
            loader.setController(controller);
            controller.setMessage(message);
            Parent root = loader.load();
            final Stage stage = new Stage();
            Scene scene = new Scene(root);
            stage.initModality(Modality.WINDOW_MODAL);
            stage.initStyle(StageStyle.UNDECORATED);
            Stage masterStage = (Stage) errorLabel.getScene().getWindow();
            stage.initOwner(masterStage);
            stage.setScene(scene);
            stage.show();

            PauseTransition delay = new PauseTransition(Duration.seconds(1));
            delay.setOnFinished(new EventHandler<ActionEvent>() {
                @Override
                public void handle(ActionEvent event) {
                    stage.close();
                }
            });
            delay.play();

        } catch (IOException ex) {
            System.out.println(ex.getMessage());
        }

    }

    private void grabData() {

        username = usernameField.getText().trim();
        password = passwordField.getText().trim();
    }

    private boolean register() {

        grabData();
        client = Connection.getInstance();

        if (client.connectToServer()) {

            return client.register(username, password);
        } else {
            client.closeConnection();
        }

        return false;
    }

    private void checkCredential() {

        showLodingScreen();

        Runnable runnable = new Runnable() {
            @Override
            public void run() {

                if (register()) {
                    updateSuccessView();
                } else {
                    updateFailedView();
                }
            }
        };

        Thread thread = new Thread(runnable);
        thread.setDaemon(true);
        thread.start();

    }

    @FXML
    public void handleRegisterAction(ActionEvent event) {

        checkCredential();

    }

    private void showLodingScreen() {

        try {
            Parent root = FXMLLoader.load(getClass().getResource("/fxml/Loading.fxml"));
            Scene scene = new Scene(root);
            loadingStage = new Stage();
            loadingStage.initStyle(StageStyle.UNDECORATED);
            loadingStage.setScene(scene);
            loadingStage.show();
        } catch (IOException ex) {
            System.out.println(ex.getMessage());
        }
    }

}
