package com.mobiquity.mydropbox.event;

import com.dropbox.core.v2.DbxFiles;

public class OnUploadSuccessfulEvent {

    private DbxFiles.FileMetadata metadata;

    public OnUploadSuccessfulEvent(DbxFiles.FileMetadata metadata) {
        this.metadata = metadata;
    }

    public DbxFiles.FileMetadata getMetadata() {
        return metadata;
    }
}
