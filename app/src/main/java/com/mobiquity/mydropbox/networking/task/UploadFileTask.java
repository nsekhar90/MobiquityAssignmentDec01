package com.mobiquity.mydropbox.networking.task;

import android.content.Context;
import android.net.Uri;
import android.os.AsyncTask;

import com.dropbox.core.DbxException;
import com.dropbox.core.v2.DbxFiles;
import com.mobiquity.mydropbox.DropboxApp;
import com.mobiquity.mydropbox.UriHelpers;
import com.mobiquity.mydropbox.event.OnUploadFailedEvent;
import com.mobiquity.mydropbox.event.OnUploadSuccessfulEvent;
import com.squareup.otto.Bus;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

public class UploadFileTask extends AsyncTask<String, Void, DbxFiles.FileMetadata> {

    private final Context context;
    private final DbxFiles files;
    private Bus bus;

    public UploadFileTask(Context context, DbxFiles filesClient) {
        this.context = context;
        files = filesClient;
        bus = DropboxApp.getBus();
    }

    @Override
    protected DbxFiles.FileMetadata doInBackground(String... params) {
        String localUri = params[0];
        File localFile = UriHelpers.getFileForUri(context, Uri.parse(localUri));

        if (localFile != null) {
            String remoteFolderPath = params[1];
            String remoteFileName = localFile.getName();

            try {
                InputStream inputStream = new FileInputStream(localFile);
                try {
                    files.uploadBuilder("/Sekhar.png")
                            .autorename(true)
                            .mode(DbxFiles.WriteMode.overwrite)
                            .run(inputStream);
                } finally {
                    inputStream.close();
                }
            } catch (DbxException | IOException e) {
                e.printStackTrace();
                bus.post(new OnUploadFailedEvent(e));
            }
        }

        return null;
    }


    @Override
    protected void onPostExecute(DbxFiles.FileMetadata result) {
        super.onPostExecute(result);
        if (result == null) {
            bus.post(new OnUploadFailedEvent(null));
        } else {
            bus.post(new OnUploadSuccessfulEvent(result));
        }
    }
}