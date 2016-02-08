package ven.wxbot;

import android.app.Application;

/**
 * Created by Han on 16/02/06.
 */

public class A extends Application {

    private static final String TAG = "微信红孩儿";

    public static A context;

    public void onCreate() {
        context = this;
        super.onCreate();
    }

    public synchronized static A getInstance() {
        return context;
    }


}
