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
import com.google.android.gms.maps.model.GroundOverlay;
import com.google.android.gms.maps.model.GroundOverlayOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

public class EnergyBall {
    protected MainActivity main;
    public int id;
    public int type;
    public LatLng position;
    public int energy;
    public Circle energyUI;
    //public GroundOverlay label;

    public EnergyBall(MainActivity context, int _id, int _type, LatLng _position, int _energy) {
        main = context;
        id = _id;
        type = _type;
        position = _position;
        energy = _energy;
    }

    public void drawEnergy() {
        if (energyUI != null)
            energyUI.remove();
        energyUI = main.mMap.addCircle(new CircleOptions()
                .center(position)
                .radius(energy) // In meters
                .fillColor(0x330000FF)
                .strokeColor(0xAA0000FF)
                .strokeWidth(1*main.metrics.density));
    }

/*
    public void drawLabel() {
        LatLng latLng = new LatLng(lat, lng);
        if (label != null)
            label.remove();
        int w = Math.round(40*main.metrics.density);
        int h = Math.round(40*main.metrics.density);
        Bitmap bmpLabel = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        Canvas canvasLabel = new Canvas(bmpLabel);
        Paint color = new Paint();
        color.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        color.setTextSize(10*main.metrics.density);
        color.setTextAlign(Paint.Align.CENTER);
        color.setColor(0x77000055);
        color.setShadowLayer(0.5f, 1.0f, 1.0f, Color.WHITE);
        canvasLabel.drawText("+" + energy, 20*main.metrics.density, 30*main.metrics.density, color);
        label = main.mMap.addGroundOverlay(new GroundOverlayOptions()
                .image(BitmapDescriptorFactory.fromBitmap(bmpLabel))
                .position(latLng, (float)energy*2, (float)energy*2));
    }
*/

    public void drawOnMap() {
        drawEnergy();
        //drawLabel();
    }

    public void clear() {
        if (energyUI != null) {
            energyUI.remove();
            energyUI = null;
        }
/*
        if (label != null) {
            label.remove();
            label = null;
        }
*/
    }


}