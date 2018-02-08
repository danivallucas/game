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
import com.google.android.gms.maps.model.GroundOverlay;
import com.google.android.gms.maps.model.GroundOverlayOptions;
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
    public GroundOverlay label;

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

    public void drawEnergy() {
        LatLng latLng = new LatLng(lat, lng);
        if (energyUI != null)
            energyUI.remove();
        energyUI = main.mMap.addCircle(new CircleOptions()
                .center(latLng)
                .radius(energy) // In meters
                .fillColor(0x11000000)
                .strokeColor(0x77000000)
                .strokeWidth(4));
    }

    public void drawLabel() {
        LatLng latLng = new LatLng(lat, lng);
        if (label != null)
            label.remove();
        Bitmap bmpLabel = Bitmap.createBitmap(160, 160, Bitmap.Config.ARGB_8888);
        Canvas canvasLabel = new Canvas(bmpLabel);
        Paint color = new Paint();
        color.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        color.setTextSize(30);
        color.setTextAlign(Paint.Align.CENTER);
        color.setColor(0x99000000);
        color.setShadowLayer(0.5f, 1.0f, 1.0f, Color.WHITE);
        if (playerId >= 0) {// default é -1
            Player player = main.game.getPlayer(playerId);
            canvasLabel.drawText("$" + main.format.format(Math.ceil(points)), 80, 100, color);
            canvasLabel.drawText(player.name, 80, 140, color);
        } else {
            canvasLabel.drawText("$" + main.format.format(Math.ceil(points)), 80, 120, color);
        }
        label = main.mMap.addGroundOverlay(new GroundOverlayOptions()
                .image(BitmapDescriptorFactory.fromBitmap(bmpLabel))
                .position(latLng, (float)energy*2, (float)energy*2));
    }


    public void drawOnMap() {
        drawEnergy();
        drawLabel();
        LatLng latLng = new LatLng(lat, lng);

        Bitmap.Config conf = Bitmap.Config.ARGB_8888;
        Bitmap bmp = Bitmap.createBitmap(160, 200, conf);
        Canvas canvas = new Canvas(bmp);

        Paint color = new Paint();
        color.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        color.setTextSize(40);
        color.setTextAlign(Paint.Align.CENTER);
        color.setColor(0xFF000000);
        color.setShadowLayer(2.0f, 2.0f, 2.0f, Color.WHITE);

        //canvas1.drawBitmap(BitmapFactory.decodeResource(main.getResources(), R.drawable.marker), 0,60, color);
        String emojiIcon = (type.equals("city")) ? "flag000" : String.format("flag%03d", id+1);
        int resID = main.getResources().getIdentifier(emojiIcon , "drawable", main.getPackageName());
        canvas.drawBitmap(BitmapFactory.decodeResource(main.getResources(), resID), 0,40, color);

        float anchorX = type.equals("capital") ? 0.5F : 0.1F;
        float anchorY = type.equals("capital") ? 0.59F : 0.98F;
        marker = main.mMap.addMarker(new MarkerOptions()
                .position(latLng)
                .anchor(anchorX, anchorY)
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
