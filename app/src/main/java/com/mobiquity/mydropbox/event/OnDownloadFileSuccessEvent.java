package com.mobiquity.mydropbox.event;

import java.io.File;

public class OnDownloadFileSuccessEvent {

    private File file;

    public OnDownloadFileSuccessEvent(File result) {
        this.file = result;
    }

    public File getFile() {
        return file;
    }
}
