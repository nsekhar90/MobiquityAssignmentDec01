package com.mobiquity.mydropbox;

import android.app.Application;

import com.squareup.otto.Bus;
import com.squareup.otto.ThreadEnforcer;


public class DropboxApp extends Application {

    public static final  String DROPBOX_APP_KEY = "fzmyv43ureiky2i";
    private static Bus bus;

    @Override
    public void onCreate() {
        super.onCreate();
        bus = new Bus(ThreadEnforcer.MAIN);
    }

    public static Bus getBus() {
        return bus;
    }
}
