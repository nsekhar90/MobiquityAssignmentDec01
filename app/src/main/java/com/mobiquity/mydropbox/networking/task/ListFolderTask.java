package com.mobiquity.mydropbox.networking.task;

import android.os.AsyncTask;

import com.dropbox.core.DbxException;
import com.dropbox.core.v2.DbxFiles;
import com.mobiquity.mydropbox.DropboxApp;
import com.mobiquity.mydropbox.event.OnDataLoadedEvent;
import com.mobiquity.mydropbox.event.OnDataLoadFailedEvent;
import com.squareup.otto.Bus;

/**
 * Async task to list items in a folder
 */
public class ListFolderTask extends AsyncTask<String, Void, DbxFiles.ListFolderResult> {

    private final DbxFiles dbxFiles;
    private Bus bus;

    public ListFolderTask(DbxFiles filesClient) {
        dbxFiles = filesClient;
        bus = DropboxApp.getBus();
    }

    @Override
    protected DbxFiles.ListFolderResult doInBackground(String... params) {
        try {
            return dbxFiles.listFolder(params[0]);
        } catch (DbxException e) {
            bus.post(new OnDataLoadFailedEvent());
        }

        return null;
    }

    @Override
    protected void onPostExecute(DbxFiles.ListFolderResult result) {
        super.onPostExecute(result);
        bus.post(new OnDataLoadedEvent(result));
    }

}
