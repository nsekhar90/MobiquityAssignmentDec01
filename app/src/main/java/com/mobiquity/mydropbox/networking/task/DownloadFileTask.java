package com.mobiquity.mydropbox.networking.task;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;
import android.util.Log;

import com.dropbox.core.DbxException;
import com.dropbox.core.v2.DbxFiles;
import com.mobiquity.mydropbox.DropboxApp;
import com.mobiquity.mydropbox.event.OnDownloadFileFailedEvent;
import com.mobiquity.mydropbox.event.OnDownloadFileSuccessEvent;
import com.squareup.otto.Bus;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

public class DownloadFileTask extends AsyncTask<DbxFiles.FileMetadata, Void, File> {

    private final Context context;
    private final DbxFiles dbxFiles;
    private Bus bus;

    public DownloadFileTask(Context context, DbxFiles filesClient) {
        this.context = context;
        dbxFiles = filesClient;
        bus = DropboxApp.getBus();
    }

    @Override
    protected File doInBackground(DbxFiles.FileMetadata... params) {
        DbxFiles.FileMetadata metadata = params[0];
        try {
            File path = Environment.getExternalStoragePublicDirectory(
                    Environment.DIRECTORY_DOWNLOADS);
            File file = new File(path, metadata.name);

            // Make sure the Downloads directory exists.
            path.mkdirs();

            // Download the file.
            OutputStream outputStream = new FileOutputStream(file);
            try {
                dbxFiles.downloadBuilder(metadata.pathLower).
                        rev(metadata.rev).run(outputStream);
            } finally {
                outputStream.close();
            }

            // Tell android about the file
            Intent intent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
            intent.setData(Uri.fromFile(file));
            context.sendBroadcast(intent);

            return file;
        } catch (DbxException | IOException e) {
           Log.e("DownloadFileTask", e.getMessage());
        }

        return null;
    }

    @Override
    protected void onPostExecute(File result) {
        super.onPostExecute(result);
        if (result == null) {
            bus.post(new OnDownloadFileFailedEvent());
        } else {
            bus.post(new OnDownloadFileSuccessEvent(result));
        }
    }

}
