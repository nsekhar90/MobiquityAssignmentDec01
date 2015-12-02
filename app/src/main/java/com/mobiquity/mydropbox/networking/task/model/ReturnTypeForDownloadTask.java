package com.mobiquity.mydropbox.networking.task.model;

import java.io.File;

public class ReturnTypeForDownloadTask {

    private File file;
    private boolean share;

    public File getFile() {
        return file;
    }

    public boolean isShare() {
        return share;
    }

    public void setShare(boolean share) {
        this.share = share;
    }

    public void setFile(File file) {
        this.file = file;
    }
}