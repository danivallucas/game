package com.danival.game;

import 	android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class FCMBroadCastReceiver extends BroadcastReceiver {

    private MainActivity main;

    public FCMBroadCastReceiver(MainActivity context) {
        main = context;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        main.onFCMTokenRefresh(intent.getStringExtra("FCM_TOKEN"));
    }

}
