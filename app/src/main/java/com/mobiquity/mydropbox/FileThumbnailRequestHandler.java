package com.mobiquity.mydropbox;

import android.net.Uri;
import android.provider.MediaStore;

import com.dropbox.core.DbxDownloader;
import com.dropbox.core.DbxException;
import com.dropbox.core.v2.DbxFiles;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.Request;
import com.squareup.picasso.RequestHandler;

import java.io.IOException;

public class FileThumbnailRequestHandler extends RequestHandler {

    private static final String SCHEME =  "dropbox";
    private static final String HOST = "dropbox";
    private final DbxFiles mFilesClient;

    public FileThumbnailRequestHandler(DbxFiles filesClient) {
        mFilesClient = filesClient;
    }

    /**
     * Builds a {@link Uri} for a Dropbox file thumbnail suitable for handling by this handler
     */
    public static Uri buildPicassoUri(DbxFiles.FileMetadata file) {
        return new Uri.Builder()
                .scheme(SCHEME)
                .authority(HOST)
                .path(file.pathLower).build();
    }

    @Override
    public boolean canHandleRequest(Request data) {
        return SCHEME.equals(data.uri.getScheme()) && HOST.equals(data.uri.getHost());
    }

    @Override
    public Result load(Request request, int networkPolicy) throws IOException {

        try {
            DbxDownloader<DbxFiles.FileMetadata> downloader =
                    mFilesClient.getThumbnailBuilder(request.uri.getPath())
                            .format(DbxFiles.ThumbnailFormat.jpeg)
                            .size(DbxFiles.ThumbnailSize.w64h64)
                            .start();

            return new Result(downloader.body, Picasso.LoadedFrom.NETWORK);
        } catch (DbxFiles.GetThumbnailException e) {
            throw new IOException(e);
        } catch (DbxException e) {
            throw new IOException(e);
        }
    }
}
