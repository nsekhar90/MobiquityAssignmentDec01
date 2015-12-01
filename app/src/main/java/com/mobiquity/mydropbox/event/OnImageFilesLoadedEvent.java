package com.mobiquity.mydropbox.event;

import com.dropbox.core.v2.DbxFiles;

public class OnImageFilesLoadedEvent {

    private DbxFiles.ListFolderResult result;

    public OnImageFilesLoadedEvent(DbxFiles.ListFolderResult result) {
        this.result = result;
    }

    public DbxFiles.ListFolderResult getResult() {
        return result;
    }
}
