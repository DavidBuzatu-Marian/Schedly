package com.davidbuzatu.schedly.model;

import android.app.Application;
import android.content.Context;

public class ContextForStrings extends Application {

    protected static ContextForStrings instance;

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
    }

    public static Context getContext() {
        return instance.getApplicationContext();
    }

}

