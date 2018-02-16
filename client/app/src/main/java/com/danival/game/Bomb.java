package com.danival.game;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;

import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

public class Bomb {
    protected MainActivity main;
    public int id;
    public int type;
    public LatLng position;
    public int energy;
    public Circle energyUI;
    public Marker marker;

    public Bomb(MainActivity context, int _id, int _type, LatLng _position, int _energy) {
        main = context;
        id = _id;
        type = _type;
        position = _position;
        energy = _energy;
    }

    public void drawOnMap() {
        energyUI = main.mMap.addCircle(new CircleOptions()
                .center(position)
                .radius(energy) // In meters
                .fillColor(0x33880000)
                .strokeColor(0xAA880000)
                .strokeWidth(1*main.metrics.density));

        int w = Math.round(40*main.metrics.density);
        int h = Math.round(50*main.metrics.density);
        Bitmap.Config conf = Bitmap.Config.ARGB_8888;
        Bitmap bmp = Bitmap.createBitmap(w, h, conf);
        Canvas canvas1 = new Canvas(bmp);

        Paint color = new Paint();
        color.setColor(0xFF008800);

        String emojiIcon = String.format("bomb%02d", type+1);
        int resID = main.getResources().getIdentifier(emojiIcon , "drawable", main.getPackageName());
        canvas1.drawBitmap(BitmapFactory.decodeResource(main.getResources(), resID), 0,10*main.metrics.density, color);
        //canvas1.drawText("("+energy+")", 80, 40, color);
        marker = main.mMap.addMarker(new MarkerOptions()
                .position(position)
                .icon(BitmapDescriptorFactory.fromBitmap(bmp)));
        marker.setTag("Bomb:"+id);
        marker.setVisible(main.isMarkerVisible("bomb", energy));
    }

    public void clear() {
        if (marker != null) {
            marker.remove();
            marker = null;
        }
        if (energyUI != null) {
            energyUI.remove();
            energyUI = null;
        }
    }

}