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

// Baixar emojis de: https://emojipedia.org/
public class Food {
    protected MainActivity main;
    public int id;
    public int type;
    public double lat;
    public double lng;
    public int energy;
    public Circle energyUI;
    public Marker marker;

    public Food(MainActivity context, int _id, int _type, double _lat, double _lng, int _energy) {
        main = context;
        id = _id;
        type = _type;
        lat = _lat;
        lng = _lng;
        energy = _energy;
    }

    public void drawOnMap() {
        LatLng latLng = new LatLng(lat, lng);
        energyUI = main.mMap.addCircle(new CircleOptions()
                .center(latLng)
                .radius(energy) // In meters
                .fillColor(0x33008800)
                .strokeColor(0xAA008800)
                .strokeWidth(4));

        Bitmap.Config conf = Bitmap.Config.ARGB_8888;
        Bitmap bmp = Bitmap.createBitmap(160, 200, conf);
        Canvas canvas1 = new Canvas(bmp);

        Paint color = new Paint();
        color.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        color.setTextSize(50);
        color.setTextAlign(Paint.Align.CENTER);
        color.setColor(0xFF008800);
        color.setShadowLayer(2.0f, 2.0f, 2.0f, Color.WHITE);

        //canvas1.drawBitmap(BitmapFactory.decodeResource(main.getResources(), R.drawable.marker), 0,60, color);
        String emojiIcon = String.format("food%02d", type+1);
        int resID = main.getResources().getIdentifier(emojiIcon , "drawable", main.getPackageName());
        canvas1.drawBitmap(BitmapFactory.decodeResource(main.getResources(), resID), 0,40, color);
        canvas1.drawText("+"+energy, 80, 40, color);
        marker = main.mMap.addMarker(new MarkerOptions()
                .position(latLng)
                .icon(BitmapDescriptorFactory.fromBitmap(bmp)));
        marker.setTag("Food:"+id);
    }

    public void clear() {
        if (marker != null) {
            marker.remove();
            marker = null;
            energyUI.remove();
            energyUI = null;
        }
    }


}
