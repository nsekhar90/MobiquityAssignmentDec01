package com.mobiquity.mydropbox.networking.task;

import android.os.AsyncTask;
import android.util.Log;

import com.dropbox.core.DbxException;
import com.dropbox.core.v2.DbxFiles;
import com.mobiquity.mydropbox.DropboxApp;
import com.mobiquity.mydropbox.event.OnImageFilesLoadedEvent;
import com.mobiquity.mydropbox.event.OnImageFilesLoadFailedEvent;
import com.squareup.otto.Bus;

/**
 * Async task to list items in a folder
 */
public class ListItemsInFolderTask extends AsyncTask<String, Void, DbxFiles.ListFolderResult> {

    private final DbxFiles dbxFiles;
    private final Bus bus;

    public ListItemsInFolderTask(DbxFiles filesClient) {
        dbxFiles = filesClient;
        bus = DropboxApp.getBus();
    }

    @Override
    protected DbxFiles.ListFolderResult doInBackground(String... params) {
        try {
            return dbxFiles.listFolder(params[0]);
        } catch (DbxException e) {
            Log.e("ListItemsInFolderTask", e.getMessage());
        }

        return null;
    }

    @Override
    protected void onPostExecute(DbxFiles.ListFolderResult result) {
        super.onPostExecute(result);
        if (result == null) {
            bus.post(new OnImageFilesLoadFailedEvent());
        } else {
            bus.post(new OnImageFilesLoadedEvent(result));
        }
    }

}
