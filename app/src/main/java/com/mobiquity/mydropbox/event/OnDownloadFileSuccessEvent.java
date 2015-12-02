package com.mobiquity.mydropbox.event;

import com.mobiquity.mydropbox.networking.task.model.ReturnTypeForDownloadTask;

public class OnDownloadFileSuccessEvent {

    private final ReturnTypeForDownloadTask downloadedFileContainer;

    public OnDownloadFileSuccessEvent(ReturnTypeForDownloadTask result) {
        downloadedFileContainer = result;
    }

    public ReturnTypeForDownloadTask getFileContainer() {
        return downloadedFileContainer;
    }
}
