package com.baudi;

import java.nio.file.Path;

@FunctionalInterface
public interface DownloadConfirmedAction {
    void handle(DownloadInfo downloadInfo, Path filePath);
}
