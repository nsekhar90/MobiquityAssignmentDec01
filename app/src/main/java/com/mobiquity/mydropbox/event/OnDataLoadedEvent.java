package com.mobiquity.mydropbox.event;

import com.dropbox.core.v2.DbxFiles;

public class OnDataLoadedEvent {

    private DbxFiles.ListFolderResult result;

    public OnDataLoadedEvent(DbxFiles.ListFolderResult result) {
        this.result = result;
    }

    public DbxFiles.ListFolderResult getResult() {
        return result;
    }
}
