package com.mobiquity.mydropbox;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;

import com.dropbox.core.android.AuthActivity;

/**
 * Helper class for integrating with {@link com.dropbox.core.android.AuthActivity}
 */
public class Auth {

    public static void startOAuth2Authentication(Context context, String appKey) {

        if (!com.dropbox.core.android.AuthActivity.checkAppBeforeAuth(context, appKey, true /*alertUser*/)) {
            return;
        }

        String apiType = "1";
        String webHost = "www.dropbox.com";
        Intent intent =  com.dropbox.core.android.AuthActivity.makeIntent(context, appKey, webHost, apiType);
        if (!(context instanceof Activity)) {
            // If starting the intent outside of an Activity, must include
            // this. See startActivity(). Otherwise, we prefer to stay in
            // the same task.
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        }
        context.startActivity(intent);
    }

    public static String getOAuth2Token() {
        Intent data = com.dropbox.core.android.AuthActivity.result;

        if (data == null) {
            return null;
        }

        String token = data.getStringExtra(com.dropbox.core.android.AuthActivity.EXTRA_ACCESS_TOKEN);
        String secret = data.getStringExtra(com.dropbox.core.android.AuthActivity.EXTRA_ACCESS_SECRET);
        String uid = data.getStringExtra(AuthActivity.EXTRA_UID);

        if (token != null && !token.equals("") &&
                secret != null && !secret.equals("") &&
                uid != null && !uid.equals("")) {
            return secret;
        }

        return null;
    }

}
