package com.example.schedly.model;

import android.app.Application;
import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;

import androidx.annotation.NonNull;

import java.util.Locale;

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

//    @NonNull
//    public static Resources getLocalizedResources(Locale locale) {
//        Context _context = instance.getApplicationContext();
//        Configuration _config = _context.getResources().getConfiguration();
//        _config = new Configuration(_config);
//        _config.setLocale(locale);
//        Context _localizedContext = _context.createConfigurationContext(_config);
//        return _localizedContext.getResources();
//    }
}

