package com.baudi;

import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.Path;
import java.time.Duration;
import java.util.function.Consumer;

public class DownloadDetailsWindow {

    public static void show(DownloadInfo downloadInfo, Stage parentStage, DownloadConfirmedAction action) {
        Stage stage = new Stage();
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.initOwner(parentStage);

        // Controles para mostrar la información del archivo
        TextField fileNameField = new TextField();
        Label fileSizeLabel = new Label();

        // Set up a Consumer to update the UI with the file information when it's fetched
        Consumer<DownloadInfo> updateUI = info -> {
            Platform.runLater(() -> {
                fileNameField.setText(info.getFileName());
                fileSizeLabel.setText("Tamaño: " + formatSize(info.getSize()));
            });
        };

        // Llama a fetchFileInfo con el Consumer que actualiza la UI
        downloadInfo.fetchFileInfo(updateUI);

        // Botón para elegir dónde guardar el archivo
        Button chooseDirButton = new Button("Elegir carpeta de destino");
        TextField dirField = new TextField();
        chooseDirButton.setOnAction(e -> {
            DirectoryChooser directoryChooser = new DirectoryChooser();
            File selectedDirectory = directoryChooser.showDialog(stage);
            if (selectedDirectory != null) {
                dirField.setText(selectedDirectory.getAbsolutePath());
            }
        });

        // Botón de confirmación para iniciar la descarga
        Button confirmButton = new Button("Iniciar Descarga");
        confirmButton.setOnAction(e -> {
            // Configura la información adicional y llama a la acción confirmada
            downloadInfo.setFileName(fileNameField.getText());
            Path downloadDir = Path.of(dirField.getText());
            Path filePath = downloadDir.resolve(downloadInfo.getFileName()); // Crea la ruta completa del archivo

            action.handle(downloadInfo, filePath); // Pasa la ruta completa a handle
            stage.close();
        });

        VBox layout = new VBox(10, fileNameField, fileSizeLabel, chooseDirButton, dirField, confirmButton);
        Scene scene = new Scene(layout, 300, 200);
        stage.setScene(scene);
        stage.showAndWait();
    }

    public static void startDownload(DownloadInfo downloadInfo, Path filePath, ObservableList<DownloadInfo> downloads) {
        new Thread(() -> {
            try {
                URL website = new URL(downloadInfo.getUrl());
                URLConnection connection = website.openConnection();
                long fileSize = connection.getContentLengthLong();
                ReadableByteChannel rbc = Channels.newChannel(connection.getInputStream());
                FileOutputStream fos = new FileOutputStream(filePath.toFile());
                FileChannel fileChannel = fos.getChannel();

                ByteBuffer buffer = ByteBuffer.allocateDirect(1024);
                long bytesDownloadedSinceLastCheck = 0;
                long totalBytesDownloaded = 0;
                long startTime = System.currentTimeMillis();
                long previousTime = startTime;

                while (true) {
                    int bytesRead = rbc.read(buffer);
                    if (bytesRead == -1) {
                        break;
                    }
                    buffer.flip();
                    totalBytesDownloaded += fileChannel.write(buffer);
                    buffer.clear();

                    long currentTime = System.currentTimeMillis();
                    long timeTaken = currentTime - previousTime;
                    if (timeTaken >= 1000) {
                        double speed = bytesDownloadedSinceLastCheck / (timeTaken / 1000.0); // Velocidad en bytes por segundo
                        double speedInMb = speed / (1024 * 1024); // Convertir a MB/s
                        long timeRemaining = (long) ((fileSize - totalBytesDownloaded) / speed); // Tiempo restante en segundos

                        String speedText = String.format("%.2f MB/s", speedInMb);
                        String timeRemainingText = formatDuration(timeRemaining);

                        Platform.runLater(() -> {
                            downloadInfo.setDownloadSpeed(speedText);
                            downloadInfo.setEstimatedTimeRemaining(timeRemainingText);
                        });

                        previousTime = currentTime;
                        bytesDownloadedSinceLastCheck = 0;
                    }
                    bytesDownloadedSinceLastCheck += bytesRead;
                    final double progress = (double) totalBytesDownloaded / fileSize;

                    Platform.runLater(() -> {
                        downloadInfo.setProgress(progress);
                    });
                }

                fileChannel.close();
                fos.close();
                rbc.close();

                Platform.runLater(() -> {
                    downloadInfo.setStatusMessage("Completado");
                    downloadInfo.setDownloadSpeed("0 MB/s");
                    downloadInfo.setEstimatedTimeRemaining("Descarga completa");
                });

            } catch (IOException e) {
                e.printStackTrace();
                Platform.runLater(() -> {
                    downloadInfo.setStatusMessage("Error");
                });
            }
        }).start();
    }

    private static String formatDuration(long seconds) {
        Duration duration = Duration.ofSeconds(seconds);
        long hours = duration.toHours();
        int minutes = duration.toMinutesPart();
        int secs = duration.toSecondsPart();
        if (hours > 0) {
            return String.format("%dh %02dm %02ds", hours, minutes, secs);
        } else if (minutes > 0) {
            return String.format("%dm %02ds", minutes, secs);
        } else {
            return String.format("%ds", secs);
        }
    }



    // Método adicional para formatear el tamaño del archivo
    private static String formatSize(long size) {
        if (size < 1024) return size + " bytes";
        int z = (63 - Long.numberOfLeadingZeros(size)) / 10;
        return String.format("%.1f %sB", (double)size / (1L << (z*10)), " KMGTPE".charAt(z));
    }
}
