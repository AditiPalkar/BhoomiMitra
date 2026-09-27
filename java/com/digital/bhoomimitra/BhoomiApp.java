package com.digital.bhoomimitra;

import android.app.Application;
import android.content.Context;

public class BhoomiApp extends Application {
    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(LocaleHelper.onAttach(base));
    }
}