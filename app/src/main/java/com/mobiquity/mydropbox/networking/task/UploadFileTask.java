package com.mobiquity.mydropbox.networking.task;

import android.content.Context;
import android.net.Uri;
import android.os.AsyncTask;
import android.text.TextUtils;

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
    private Exception exception;

    public UploadFileTask(Context context, DbxFiles filesClient) {
        this.context = context;
        files = filesClient;
        bus = DropboxApp.getBus();
    }

    @Override
    protected DbxFiles.FileMetadata doInBackground(String... params) {
        String localUri = params[0];
        String city = params[1];
        File localFile = UriHelpers.getFileForUri(context, Uri.parse(localUri));

        if (localFile != null) {
            String remoteFileName = localFile.getName();
            String fileName = remoteFileName;
            if (!TextUtils.isEmpty(city)) {
                fileName = city + "_" + remoteFileName;
            }
            try {
                InputStream inputStream = new FileInputStream(localFile);
                try {
                    files.uploadBuilder("/" + fileName)
                            .autorename(true)
                            .mode(DbxFiles.WriteMode.overwrite)
                            .run(inputStream);
                } finally {
                    inputStream.close();
                }
            } catch (DbxException | IOException e) {
                e.printStackTrace();
                this.exception = e;
            }
        }

        return null;
    }


    @Override
    protected void onPostExecute(DbxFiles.FileMetadata result) {
        super.onPostExecute(result);
        if (exception != null) {
            bus.post(new OnUploadFailedEvent());
        } else {
            bus.post(new OnUploadSuccessfulEvent());
        }
    }
}