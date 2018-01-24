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

public class Food {
    protected MainActivity main;
    public int id;
    public double lat;
    public double lng;
    public long energy;
    public Circle energyUI;
    public Marker marker;

    public Food(MainActivity context, int _id, double _lat, double _lng, long _energy) {
        main = context;
        id = _id;
        lat = _lat;
        lng = _lng;
        energy = _energy;
    }

    public void drawOnMap() {
        LatLng latLng = new LatLng(lat, lng);
        energyUI = main.mMap.addCircle(new CircleOptions()
                .center(latLng)
                .radius(energy) // In meters
                .fillColor(0x330000FF)
                .strokeColor(0xAA0000FF)
                .strokeWidth(0));

        Bitmap.Config conf = Bitmap.Config.ARGB_8888;
        Bitmap bmp = Bitmap.createBitmap(160, 200, conf);
        Canvas canvas1 = new Canvas(bmp);

        Paint color = new Paint();
        color.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        color.setTextSize(50);
        color.setTextAlign(Paint.Align.CENTER);
        color.setColor(Color.BLACK);
        color.setShadowLayer(2.0f, 2.0f, 2.0f, Color.WHITE);

        //canvas1.drawBitmap(BitmapFactory.decodeResource(main.getResources(), R.drawable.marker), 0,60, color);
        canvas1.drawBitmap(BitmapFactory.decodeResource(main.getResources(), R.drawable.apple), 0,40, color);
        canvas1.drawText(""+id, 80, 40, color);
        marker = main.mMap.addMarker(new MarkerOptions()
                .position(latLng)
                .icon(BitmapDescriptorFactory.fromBitmap(bmp)));
        marker.setTag("Food:"+id);
    }

    public void clear() {
        if (energyUI != null)
            energyUI.remove();
        if (marker != null)
            marker.remove();
    }


}
