package com.baudi;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.function.Consumer;
import javafx.application.Platform;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class DownloadInfo {
    private String url;
    private StringProperty fileName = new SimpleStringProperty();
    private DoubleProperty progress = new SimpleDoubleProperty(0);
    private StringProperty downloadSpeed = new SimpleStringProperty("0 MB/s");
    private StringProperty estimatedTimeRemaining = new SimpleStringProperty("Calculando...");
    private StringProperty statusMessage = new SimpleStringProperty("Iniciando...");
    private long size; // El tamaño del archivo en bytes

    public DownloadInfo(String url, Consumer<DownloadInfo> onInfoFetched) {
        this.url = url;
        fetchFileInfo(onInfoFetched);
    }

    // Método para iniciar la obtención de la información del archivo
    void fetchFileInfo(Consumer<DownloadInfo> onInfoFetched) {
        new Thread(() -> {
            try {
                URL downloadUrl = new URL(url);
                HttpURLConnection connection = (HttpURLConnection) downloadUrl.openConnection();
                connection.setRequestMethod("HEAD");

                String contentDisposition = connection.getHeaderField("Content-Disposition");
                if (contentDisposition != null && contentDisposition.contains("filename=")) {
                    this.fileName.set(contentDisposition.split("filename=")[1].replaceAll("\"", ""));
                } else {
                    String[] urlParts = url.split("/");
                    this.fileName.set(urlParts[urlParts.length - 1]);
                }

                this.size = connection.getContentLengthLong();

                connection.disconnect();

                Platform.runLater(() -> onInfoFetched.accept(this));
            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() -> statusMessage.set("Error"));
            }
        }).start();
    }

    // Getters y setters para las propiedades

    public StringProperty fileNameProperty() {
        return fileName;
    }

    public final String getFileName() {
        return fileName.get();
    }

    public final void setFileName(String fileName) {
        this.fileName.set(fileName);
    }

    public DoubleProperty progressProperty() {
        return progress;
    }

    public final double getProgress() {
        return progress.get();
    }

    public final void setProgress(double progress) {
        this.progress.set(progress);
    }

    public StringProperty downloadSpeedProperty() {
        return downloadSpeed;
    }

    public final String getDownloadSpeed() {
        return downloadSpeed.get();
    }

    public final void setDownloadSpeed(String downloadSpeed) {
        this.downloadSpeed.set(downloadSpeed);
    }

    public StringProperty estimatedTimeRemainingProperty() {
        return estimatedTimeRemaining;
    }

    public final String getEstimatedTimeRemaining() {
        return estimatedTimeRemaining.get();
    }

    public final void setEstimatedTimeRemaining(String estimatedTimeRemaining) {
        this.estimatedTimeRemaining.set(estimatedTimeRemaining);
    }

    public StringProperty statusMessageProperty() {
        return statusMessage;
    }

    public final String getStatusMessage() {
        return statusMessage.get();
    }

    public final void setStatusMessage(String statusMessage) {
        this.statusMessage.set(statusMessage);
    }

    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }

    public String getUrl() {
        return url;
    }

    @Override
    public String toString() {
        // Muestra el nombre del archivo o "Desconocido" si el nombre no está disponible
        return getFileName() != null && !getFileName().isEmpty() ? getFileName() : "Desconocido";
    }
}
