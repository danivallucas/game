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
        //Log.e("game", "**** run ****");
        for (int i = 0; i < main.game.playerList.size(); i++) {
            Player player = main.game.playerList.get(i);
            long now = System.currentTimeMillis();
            if (!player.status.equals("moving")) continue;
            Log.e("game", "moving...");
            RouteLeg leg = player.legList.get(0);
            if (now > leg.endTime) {
                player.status = "in";
                Point lastPoint = leg.pointList.get(leg.pointList.size()-1);
                player.setLocation(lastPoint.lat, lastPoint.lng);
                continue;
            }
            long legStart = leg.endTime - leg.totalDuration; // em que miliseg
            long pos = (now < legStart) ? 0 : now - legStart;
            int j = 0;
            Point point1 = leg.pointList.get(j);
            long sum = point1.duration;
            while ( (pos > sum) && (j < leg.pointList.size()-1) ) {
                point1 = leg.pointList.get(++j);
                sum += point1.duration;
            }
            if (pos > sum) {
                player.lat = point1.lat;
                player.lng = point1.lng;
            } else {
                Point point2 = leg.pointList.get(++j);
                double percent = 1 - ( (sum - pos) / (double)point1.duration );
                player.lat = point1.lat + (point2.lat - point1.lat) * percent;
                player.lng = point1.lng + (point2.lng - point1.lng) * percent;
            }
            player.drawLegList(j);
        }
        //main.handler.postDelayed(this, 80);
        main.handler.postDelayed(this, 300);
    }
}
