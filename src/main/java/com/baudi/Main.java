package com.baudi;// Main.java

import com.baudi.DownloadInfo;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;

public class Main extends Application {

    @Override
    public void start(Stage primaryStage) {
        BorderPane root = new BorderPane();

        ObservableList<DownloadInfo> downloads = FXCollections.observableArrayList();

        ListView<DownloadInfo> downloadList = new ListView<>(downloads);
        downloadList.setCellFactory(lv -> new ListCell<DownloadInfo>() {
            private ProgressBar progressBar = new ProgressBar(0);
            private Label fileNameLabel = new Label();
            private Label speedLabel = new Label();
            private Label timeLabel = new Label();
            private HBox hBox = new HBox(5); // Use spacing of 5 for padding between elements

            { // Instance initializer block for setting up the HBox
                hBox.getChildren().addAll(fileNameLabel, progressBar, speedLabel, timeLabel);
                hBox.setSpacing(10); // Set spacing between items in HBox if needed
            }

            @Override
            protected void updateItem(DownloadInfo item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    fileNameLabel.textProperty().bind(item.fileNameProperty());
                    progressBar.progressProperty().bind(item.progressProperty());
                    speedLabel.textProperty().bind(item.downloadSpeedProperty());
                    timeLabel.textProperty().bind(item.estimatedTimeRemainingProperty());
                    setGraphic(hBox);
                }
            }
        });

        root.setCenter(downloadList);

        TextField urlField = new TextField();
        urlField.setPromptText("Ingrese la URL del archivo a descargar");

        Button addButton = new Button("Agregar Descarga");
        addButton.setOnAction(event -> {
            String url = urlField.getText();
            if (!url.isEmpty()) {
                // Pasa un Consumer vacío en la creación porque la actualización de UI se manejará después
                DownloadInfo downloadInfo = new DownloadInfo(url, info -> {});
                downloads.add(downloadInfo); // Añadir al observable list para mostrar en la UI

                // Define la acción de confirmación que iniciará la descarga
                DownloadConfirmedAction confirmedAction = (infoInner, path) -> {
                    // Llama al método estático de DownloadDetailsWindow para iniciar la descarga
                    DownloadDetailsWindow.startDownload(infoInner, path, downloads);
                };

                // Muestra la ventana de detalles de la descarga sin pasar ProgressBar ni Label porque se actualizarán en la ListView
                DownloadDetailsWindow.show(downloadInfo, primaryStage, confirmedAction);
                urlField.clear();
            }
        });

        HBox inputArea = new HBox(10, urlField, addButton);
        root.setTop(inputArea);

        Scene scene = new Scene(root, 600, 400);
        primaryStage.setTitle("Gestor de Descargas");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
