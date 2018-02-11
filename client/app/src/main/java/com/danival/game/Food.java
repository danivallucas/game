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
    public GroundOverlay label;

    public Food(MainActivity context, int _id, int _type, double _lat, double _lng, int _energy) {
        main = context;
        id = _id;
        type = _type;
        lat = _lat;
        lng = _lng;
        energy = _energy;
    }

    public void drawEnergy() {
        LatLng latLng = new LatLng(lat, lng);
        if (energyUI != null)
            energyUI.remove();
        energyUI = main.mMap.addCircle(new CircleOptions()
                .center(latLng)
                .radius(energy) // In meters
                .fillColor(0x33FF0000)
                .strokeColor(0xAAFF0000)
                .strokeWidth(3*main.metrics.density));
    }

    public void drawLabel() {
        LatLng latLng = new LatLng(lat, lng);
        if (label != null)
            label.remove();
        Bitmap bmpLabel = Bitmap.createBitmap(160, 160, Bitmap.Config.ARGB_8888);
        Canvas canvasLabel = new Canvas(bmpLabel);
        Paint color = new Paint();
        color.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        color.setTextSize(10*main.metrics.density);
        color.setTextAlign(Paint.Align.CENTER);
        color.setColor(0x77550000);
        canvasLabel.drawText("+" + energy, 80, 120, color);
        label = main.mMap.addGroundOverlay(new GroundOverlayOptions()
                .image(BitmapDescriptorFactory.fromBitmap(bmpLabel))
                .position(latLng, (float)energy*2, (float)energy*2));
    }

    public void drawOnMap() {
        drawEnergy();
        drawLabel();
        LatLng latLng = new LatLng(lat, lng);

        Bitmap bmp = Bitmap.createBitmap(160, 160, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bmp);
        String emojiIcon = String.format("food%02d", type+1);
        int resID = main.getResources().getIdentifier(emojiIcon , "drawable", main.getPackageName());
        canvas.drawBitmap(BitmapFactory.decodeResource(main.getResources(), resID), 0,0, new Paint());
        marker = main.mMap.addMarker(new MarkerOptions()
                .position(latLng)
                .anchor(0.5F, 1F)
                .icon(BitmapDescriptorFactory.fromBitmap(bmp)));
        marker.setTag("Food:"+id);
        marker.setVisible(main.isMarkerVisible("food", energy));
    }

    public void clear() {
        if (marker != null) {
            marker.remove();
            marker = null;
            energyUI.remove();
            energyUI = null;
            label.remove();
            label = null;
        }
    }


}
