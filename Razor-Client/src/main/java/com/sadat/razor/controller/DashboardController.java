package com.sadat.razor.controller;

import com.sadat.razor.client.Client;
import com.sadat.razor.client.Connection;
import com.sadat.razor.interfaces.MessageListener;
import com.sadat.razor.interfaces.ShakeListener;
import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.ListView;
import javafx.scene.image.Image;
import javafx.scene.input.MouseEvent;
import javafx.stage.Stage;
import com.sadat.razor.interfaces.StatusListener;
import static com.sadat.razor.interfaces.StatusListener.userSet;
import com.sadat.razor.interfaces.UserListener;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import javafx.animation.FadeTransition;
import javafx.animation.KeyFrame;
import javafx.animation.PauseTransition;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.text.TextAlignment;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.StageStyle;
import javafx.util.Duration;

public class DashboardController implements Initializable, StatusListener {

    @FXML
    private Label profileLabel;
    @FXML
    private TabPane tabPane;
    @FXML
    private TextField messageField;
    @FXML
    private Button buzzButton, callButton, sendButton, picButton, fileButton;
    private int x = 0, y = 0;

    @FXML
    private ListView<String> userListView;
    private final ObservableList<String> observableList = FXCollections.observableArrayList();

    private Client client;
    private String username;

    private FileChooser fileChooser;
    private File selectedFile;
    private byte[] bFile;

    @Override
    public void initialize(URL url, ResourceBundle rb) {

        client = Connection.getInstance();

        observableList.addAll(userSet);
        userListView.setItems(observableList);
        profileLabel.setText(username);

        bindButtons();
        initiateChat();
        updateUserList();
        propagateMessage();
        handleInput();
        receiveShake();
    }

    private void pictureChoosing() {

        fileChooser = new FileChooser();
        selectedFile = fileChooser.showOpenDialog(null);

        fileChooser.setTitle("Choose a Picture");
        fileChooser.setInitialDirectory(
                new File(System.getProperty("user.home"))
        );
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("JPG", "*.jpg")
        );
    }

    @FXML
    public void handlePictureBrowse(ActionEvent event) {

        pictureChoosing();

        if (selectedFile != null) {
            bFile = new byte[(int) selectedFile.length()];
            try {
                InputStream fis = new FileInputStream(selectedFile);
                fis.read(bFile);
            } catch (FileNotFoundException ex) {
                System.out.println(ex.getMessage());
            } catch (IOException ex) {
                System.out.println(ex.getMessage());
            }
        }
    }

    private void bindButtons() {

        buzzButton
                .disableProperty()
                .bind(Bindings.isEmpty(tabPane.getTabs()));

        callButton
                .disableProperty()
                .bind(Bindings.isEmpty(tabPane.getTabs()));

        picButton
                .disableProperty()
                .bind(Bindings.isEmpty(tabPane.getTabs()));

        fileButton
                .disableProperty()
                .bind(Bindings.isEmpty(tabPane.getTabs()));

        sendButton
                .disableProperty()
                .bind(Bindings.isEmpty(tabPane.getTabs()));

        messageField
                .disableProperty()
                .bind(Bindings.isEmpty(tabPane.getTabs()));
    }

    private void updateUserList() {

        client.addUser(new UserListener() {
            @Override
            public void online(final String user) {

                Platform.runLater(new Runnable() {
                    @Override
                    public void run() {

                        if (!observableList.contains(user)) {

                            observableList.add(user);
                        }
                    }
                });
            }

            @Override
            public void offline(final String user) {

                Platform.runLater(new Runnable() {
                    @Override
                    public void run() {

                        if (observableList.contains(user)) {

                            observableList.remove(user);
                        }
                    }
                });
            }
        });
    }

    public void setUser(String username) {

        this.username = username;
    }

    private void initiateChat() {

        userListView.setOnMouseClicked(new EventHandler<MouseEvent>() {

            @Override
            public void handle(MouseEvent event) {

                if (event.getClickCount() == 2) {

                    String chatter = userListView.getSelectionModel().getSelectedItem();

                    if (chatter != null) {

                        VBox vbox = new VBox();
                        vbox.setMinWidth(400);
                        vbox.setPrefWidth(400);
                        vbox.setId(chatter);
                        vbox.getStyleClass().add("chatbox-vbox");

                        ScrollPane scrollPane = new ScrollPane();
                        scrollPane.setPrefWidth(400);
                        scrollPane.setPrefHeight(400);
                        scrollPane.setFitToWidth(true);
                        scrollPane.setFitToHeight(true);
                        scrollPane.setContent(vbox);
                        scrollPane.getStyleClass().add("scrollpane");
                        scrollPane.vvalueProperty().bind(vbox.heightProperty());

                        Tab tab = new Tab();
                        tab.setId(chatter);
                        tab.setContent(scrollPane);
                        tab.setText(chatter);
                        tabPane.getTabs().add(tab);
                    }
                }
            }

        });
    }

    private void receiveShake() {

        client.addShake(new ShakeListener() {
            @Override
            public void shakeUser(String from) {

                if (!tabPane.getSelectionModel().isEmpty()) {

                    shakeStage();
                }
            }
        });
    }
    
    private boolean checkTabPane(String sender){
        
        int size = tabPane.getTabs().size();
        
        if(size < 1){
            return true;
        }
        else{
            
            for(Tab tab : tabPane.getTabs()){
                
                if(sender.equalsIgnoreCase(tab.getId())){
                
                    return false;
                }
            }
            
            return true;
        }
   
    }

    private void propagateMessage() {

        client.addMessage(new MessageListener() {
            @Override
            public void conveyMessage(final String from, final String message) {

                Platform.runLater(new Runnable() {
                    @Override
                    public void run() {

                        if (checkTabPane(from)) {

                            VBox vbox = new VBox();
                            vbox.setMinWidth(400);
                            vbox.setPrefWidth(400);
                            vbox.setId(from);
                            vbox.getStyleClass().add("chatbox-vbox");

                            ScrollPane scrollPane = new ScrollPane();
                            scrollPane.setPrefWidth(400);
                            scrollPane.setPrefHeight(400);
                            scrollPane.setFitToWidth(true);
                            scrollPane.setFitToHeight(true);
                            scrollPane.setContent(vbox);
                            scrollPane.getStyleClass().add("scrollpane");
                            scrollPane.vvalueProperty().bind(vbox.heightProperty());

                            Tab tab = new Tab();
                            tab.setId(from);
                            tab.setContent(scrollPane);
                            tab.setText(from);
                            tabPane.getTabs().add(tab);
                        }
                        
                        String to = tabPane.getSelectionModel().getSelectedItem().getId();

                        if (to != null) {

                            if (to.equalsIgnoreCase(from)) {

                                String time = getTimestamp();
                                String fullMessage = time + from + ": " + message;
                                addFromReceiver(fullMessage);
                                playMessageArrivalSound();
                            }
                        }

                    }
                });
            }
        });
    }

    @FXML
    public void handleShakeAction(final ActionEvent event) {

        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                Platform.runLater(new Runnable() {
                    @Override
                    public void run() {
                        String to = tabPane.getSelectionModel().getSelectedItem().getId();

                        if (to != null) {
                            if (userSet.contains(to)) {

                                sendShakeToServer(to);

                            } else {

                                openErrorWindow("User Offline!");
                            }
                        } else {
                            openErrorWindow("Conversation is over!");
                        }

                    }
                });
            }
        };

        Thread thread = new Thread(runnable);
        thread.setDaemon(true);
        thread.start();
    }

    private void sendShakeToServer(final String to) {

        Task task = new Task() {

            @Override
            protected Object call() {

                client.sendShakeToUser(to);

                return null;
            }
        };

        Thread thread = new Thread(task);
        thread.setDaemon(true);
        thread.start();
    }

    @FXML
    public void handleLogoutAction(final ActionEvent event) {

        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                if (client.logout()) {
                    Platform.runLater(new Runnable() {
                        @Override
                        public void run() {
                            openLoginWindow(event);
                        }
                    });
                }
            }
        };

        Thread thread = new Thread(runnable);
        thread.setDaemon(true);
        thread.start();
    }

    private void openLoginWindow(ActionEvent event) {
        try {
            ((Node) (event.getSource())).getScene().getWindow().hide();
            Parent root = FXMLLoader.load(getClass().getResource("/fxml/Login.fxml"));
            Scene scene = new Scene(root);
            Stage stage = new Stage();
            stage.setTitle("Login");
            stage.setScene(scene);
            stage.getIcons().add(new Image("/image/logo.png"));
            stage.show();
        } catch (IOException ex) {
            System.out.println(ex.getMessage());
        }
    }

    private void addFromSender(String message) {

        String tabName = tabPane.getSelectionModel().getSelectedItem().getId();

        if (tabName != null) {

            for (Tab tab : tabPane.getTabs()) {

                if (tabName.equalsIgnoreCase(tab.getId())) {

                    ScrollPane scrollPane = (ScrollPane) tab.getContent();
                    VBox vBox = (VBox) scrollPane.getContent();

                    HBox hBox = new HBox();
                    hBox.setAlignment(Pos.CENTER_LEFT);
                    hBox.setPadding(new Insets(10));
                    Label label = new Label(message);
                    label.setWrapText(true);
                    label.setTextAlignment(TextAlignment.JUSTIFY);
                    label.getStyleClass().add("label-sender");
                    hBox.getChildren().add(label);
                    vBox.getChildren().add(hBox);

                    FadeTransition ft = new FadeTransition(Duration.millis(500), hBox);
                    ft.setFromValue(0.0);
                    ft.setToValue(1.0);
                    ft.play();

                    resetField();
                    break;
                }
            }
        }
    }

    private void addFromReceiver(String message) {

        String tabName = tabPane.getSelectionModel().getSelectedItem().getId();

        if (tabName != null) {

            List<Tab> tabs = tabPane.getTabs();

            for (Tab tab : tabs) {

                if (tabName.equalsIgnoreCase(tab.getId())) {

                    ScrollPane scrollPane = (ScrollPane) tab.getContent();
                    VBox vBox = (VBox) scrollPane.getContent();

                    HBox hBox = new HBox();
                    hBox.setAlignment(Pos.CENTER_RIGHT);
                    hBox.setPadding(new Insets(10));
                    Label label = new Label(message);
                    label.setWrapText(true);
                    label.setTextAlignment(TextAlignment.JUSTIFY);
                    label.getStyleClass().add("label-receiver");
                    hBox.getChildren().add(label);
                    vBox.getChildren().add(hBox);

                    FadeTransition ft = new FadeTransition(Duration.millis(500), hBox);
                    ft.setFromValue(0.0);
                    ft.setToValue(1.0);
                    ft.play();

                    resetField();
                    break;
                }

            }
        }
    }

    private void resetField() {

        messageField.setText("");
    }

    private void handleInput() {

        messageField.setOnKeyPressed(new EventHandler<KeyEvent>() {
            @Override
            public void handle(KeyEvent event) {
                if (event.getCode().equals(KeyCode.ENTER)) {
                    sendMessage(tabPane.getSelectionModel().getSelectedItem().getId());
                }
            }
        });
    }

    @FXML
    public void handleMessageAction(ActionEvent event) {

        sendMessage(tabPane.getSelectionModel().getSelectedItem().getId());
    }

    private void sendMessage(String to) {

        String message = messageField.getText().trim();
        messageField.setText("");

        if ((message != null) && (message.length() > 0)) {

            if (userSet.contains(to)) {

                sendMessageToServer(to, message);
                addFromSender(getTimestamp() + username + ": " + message);
                playMessageDepartureSound();
            } else {

                openErrorWindow("User Offline!");
            }
        } else {
            openErrorWindow("Empty Input!");
        }
    }

    private void sendMessageToServer(final String to, final String message) {

        Task task = new Task() {

            @Override
            protected Object call() {

                client.sendMessageToUser(to, message);

                return null;
            }
        };

        Thread thread = new Thread(task);
        thread.setDaemon(true);
        thread.start();
    }

    private String getTimestamp() {

        return "[" + new SimpleDateFormat("HH:mm:ss").format(Calendar.getInstance().getTime()) + "] ";
    }

    private void openErrorWindow(String message) {

        try {

            FXMLLoader loader = new FXMLLoader((getClass().getResource("/fxml/Error.fxml")));
            ErrorController controller = new ErrorController();
            loader.setController(controller);
            controller.setMessage(message);
            Parent root = loader.load();
            final Stage stage = new Stage();
            Scene scene = new Scene(root);
            stage.initModality(Modality.WINDOW_MODAL);
            stage.initStyle(StageStyle.UNDECORATED);
            Stage masterStage = (Stage) tabPane.getScene().getWindow();
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

    private void playMessageArrivalSound() {

        Task task = new Task() {
            @Override
            protected Object call() {

                try {

                    String path = "src/main/resources/audio/arrival.mp3";
                    Media media = new Media(new File(path).toURI().toURL().toString());
                    MediaPlayer mediaPlayer = new MediaPlayer(media);
                    mediaPlayer.setAutoPlay(true);
                } catch (MalformedURLException ex) {
                    System.out.println(ex.getMessage());
                }

                return null;
            }
        };

        Thread thread = new Thread(task);
        thread.setDaemon(true);
        thread.start();

    }

    private void playMessageDepartureSound() {

        Runnable runnable = new Runnable() {
            @Override
            public void run() {

                try {

                    String path = "src/main/resources/audio/departure.mp3";
                    Media media = new Media(new File(path).toURI().toURL().toString());
                    MediaPlayer mediaPlayer = new MediaPlayer(media);
                    mediaPlayer.setAutoPlay(true);
                } catch (MalformedURLException ex) {
                    System.out.println(ex.getMessage());
                }
            }
        };

        Thread thread = new Thread(runnable);
        thread.setDaemon(true);
        thread.start();
    }

    private void shakeStage() {

        Timeline timelineX = new Timeline(new KeyFrame(Duration.seconds(0.01), new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent t) {
                Stage s = (Stage) tabPane.getScene().getWindow();
                if (x == 0) {
                    s.setX(s.getX() + 20);
                    x = 1;
                } else {
                    s.setX(s.getX() - 20);
                    x = 0;
                }
            }
        }));

        timelineX.setCycleCount(20);
        timelineX.setAutoReverse(false);
        timelineX.play();

        Timeline timelineY = new Timeline(new KeyFrame(Duration.seconds(0.01), new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent t) {
                Stage s = (Stage) tabPane.getScene().getWindow();
                if (y == 0) {

                    s.setY(s.getY() + 20);
                    y = 1;
                } else {
                    s.setY(s.getY() - 20);
                    y = 0;
                }
            }
        }));

        timelineY.setCycleCount(20);
        timelineY.setAutoReverse(false);
        timelineY.play();
    }
}
