package com.example.bo.ocrbaidu;

import android.app.Application;
import android.util.Log;

public class MyApplication extends Application {
    private static final String TAG = "OCR";
    private static MyApplication app;

    public static MyApplication getInstance() {
        return app;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "MyApplication onCreate");
        app = this;
    }
}
