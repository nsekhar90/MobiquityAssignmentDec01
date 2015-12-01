package com.mobiquity.mydropbox;


import com.dropbox.core.DbxHost;
import com.dropbox.core.DbxRequestConfig;
import com.dropbox.core.http.OkHttpRequestor;
import com.dropbox.core.v2.DbxClientV2;
import com.dropbox.core.v2.DbxFiles;
import com.dropbox.core.v2.DbxSharing;

import java.util.Locale;

public class DropboxClient {

    private static DbxClientV2 sDbxClient;

    public static void init(String accessToken) {
        if (sDbxClient == null) {
            String userLocale = Locale.getDefault().toString();
            DbxRequestConfig requestConfig = new DbxRequestConfig(
                    "dropboxapp",
                    userLocale,
                    OkHttpRequestor.Instance);

            sDbxClient = new DbxClientV2(requestConfig, accessToken, DbxHost.Default);
        }
    }

    public static DbxFiles files() {
        if (sDbxClient != null)
            return sDbxClient.files;
        else
            return null;
    }

    public static DbxSharing sharing() {
        if (sDbxClient != null)
            return sDbxClient.sharing;
        else
            return null;
    }
}
