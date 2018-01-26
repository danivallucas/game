package com.danival.game;

import android.util.Log;

import com.google.android.gms.maps.model.LatLng;

public class Animator implements Runnable {

    protected MainActivity main;

    public Animator(MainActivity context) {
        main = context;
    }

    @Override
    public void run() {
        for (int i = 0; i < main.game.playerList.size(); i++) {
            Player player = main.game.playerList.get(i);
            player.drawMoving();
        }
        main.handler.postDelayed(this, 300);
    }
}
