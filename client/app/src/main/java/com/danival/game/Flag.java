package com.danival.game;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.util.Log;

import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

public class Flag {
    protected MainActivity main;
    public int id;
    public String type;
    public double lat;
    public double lng;
    public int energy;
    public int wall;
    public int playerId;
    public double points;
    public Circle energyUI;
    public Marker marker;

    public Flag(MainActivity context, int _id, String _type, double _lat, double _lng, int _energy, int _wall, int _playerId, double _points) {
        main = context;
        id = _id;
        type = _type;
        lat = _lat;
        lng = _lng;
        energy = _energy;
        wall = _wall;
        playerId = _playerId;
        points = _points;
    }

    public void drawOnMap() {
        LatLng latLng = new LatLng(lat, lng);
        energyUI = main.mMap.addCircle(new CircleOptions()
                .center(latLng)
                .radius(energy) // In meters
                .fillColor(0x11000000)
                .strokeColor(0x77000000)
                .strokeWidth(4));

        Bitmap.Config conf = Bitmap.Config.ARGB_8888;
        Bitmap bmp = Bitmap.createBitmap(160, 200, conf);
        Canvas canvas1 = new Canvas(bmp);

        Paint color = new Paint();
        color.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        color.setTextSize(40);
        color.setTextAlign(Paint.Align.CENTER);
        color.setColor(0xFF000000);
        color.setShadowLayer(2.0f, 2.0f, 2.0f, Color.WHITE);

        //canvas1.drawBitmap(BitmapFactory.decodeResource(main.getResources(), R.drawable.marker), 0,60, color);
        String emojiIcon = (type.equals("city")) ? "flag000" : String.format("flag%03d", (id+1)%5+1);
        int resID = main.getResources().getIdentifier(emojiIcon , "drawable", main.getPackageName());
        canvas1.drawBitmap(BitmapFactory.decodeResource(main.getResources(), resID), 0,40, color);
        canvas1.drawText((int)Math.ceil(points) + " pts", 80, 40, color);
/*
        if (playerId >= 0)  {
            Player player = main.game.getPlayer(playerId);
            canvas1.drawText("[" + player.name + "]", 80, 40, color);
        }
*/

        marker = main.mMap.addMarker(new MarkerOptions()
                .position(latLng)
                .icon(BitmapDescriptorFactory.fromBitmap(bmp)));
        marker.setTag("Flag:"+id);
    }

    public void clear() {
        if (marker != null) {
            marker.remove();
            marker = null;
            energyUI.remove();
            energyUI = null;
        }
    }

    public void onFlagCaptured(int _playerId) {
        playerId = _playerId;
        clear();
        drawOnMap();
    }
}
