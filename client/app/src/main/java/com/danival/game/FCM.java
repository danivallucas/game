package com.danival.game;

import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.FirebaseInstanceIdService;

import io.socket.client.Socket;

public class FCM extends FirebaseInstanceIdService {

    @Override
    public void onTokenRefresh() {
        // Get updated InstanceID token.
        String refreshedToken = FirebaseInstanceId.getInstance().getToken();
        Log.e("game", "Refreshed token: " + refreshedToken);
        Intent localIntent = new Intent("com.danival.game.FCM_BROADCAST").putExtra("FCM_TOKEN", refreshedToken);
        LocalBroadcastManager.getInstance(this).sendBroadcast(localIntent);
    }
}


