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

public class EnergyBall {
    protected MainActivity main;
    public int id;
    public int type;
    public double lat;
    public double lng;
    public int energy;
    public Circle energyUI;

    public EnergyBall(MainActivity context, int _id, int _type, double _lat, double _lng, int _energy) {
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
                .fillColor(0x330000FF)
                .strokeColor(0xAA0000FF)
                .strokeWidth(4));
    }

    public void clear() {
        if (energyUI != null) {
            energyUI.remove();
            energyUI = null;
        }
    }


}
