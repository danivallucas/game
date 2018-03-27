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
    public LatLng position;
    public int energy;
    public int wall;
    public int playerId;
    public double points;
    public Circle energyUI;
    public Marker marker;
    public GroundOverlay label;

    public Flag(MainActivity context, int _id, String _type, LatLng _position, int _energy, int _wall, int _playerId, double _points) {
        main = context;
        id = _id;
        type = _type;
        position = _position;
        energy = _energy;
        wall = _wall;
        playerId = _playerId;
        points = _points;
    }

    public void drawEnergy() {
        if (energyUI != null)
            energyUI.remove();
        energyUI = main.mMap.addCircle(new CircleOptions()
                .center(position)
                .radius(energy) // In meters
                .fillColor(0x11000000)
                .strokeColor(0x77000000)
                .strokeWidth(1*main.metrics.density));
    }

    public void drawLabel() {
        if (label != null)
            label.remove();
        int w = Math.round(40*main.metrics.density);
        int h = Math.round(40*main.metrics.density);
        Bitmap bmpLabel = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        Canvas canvasLabel = new Canvas(bmpLabel);
        Paint color = new Paint();
        color.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        color.setTextSize(8*main.metrics.density);
        color.setTextAlign(Paint.Align.CENTER);
        color.setColor(0x99000000);
        color.setShadowLayer(0.5f, 1.0f, 1.0f, Color.WHITE);
        if (playerId >= 0) {// default é -1
            Player player = main.game.getPlayer(playerId);
            canvasLabel.drawText(player.name, 20*main.metrics.density, 35*main.metrics.density, color);
        }
        label = main.mMap.addGroundOverlay(new GroundOverlayOptions()
                .image(BitmapDescriptorFactory.fromBitmap(bmpLabel))
                .position(position, (float)energy*2, (float)energy*2));
    }


    public void drawOnMap() {
        drawEnergy();
        drawLabel();

        int w = Math.round(50*main.metrics.density); //40
        int h = Math.round(74*main.metrics.density); //50
        Bitmap.Config conf = Bitmap.Config.ARGB_8888;
        Bitmap bmp = Bitmap.createBitmap(w, h, conf);
        Canvas canvas = new Canvas(bmp);

        Paint stroke = new Paint();
        stroke.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        stroke.setTextSize(10*main.metrics.density);
        stroke.setTextAlign(Paint.Align.CENTER);
        stroke.setColor(Color.WHITE);
        stroke.setStyle(Paint.Style.STROKE);
        stroke.setStrokeWidth(3*main.metrics.density);

        Paint color = new Paint();
        color.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        color.setTextSize(10*main.metrics.density);
        color.setTextAlign(Paint.Align.CENTER);
        color.setColor(0xFF000000);
        //color.setShadowLayer(2.0f, 2.0f, 2.0f, Color.WHITE);

        //canvas1.drawBitmap(BitmapFactory.decodeResource(main.getResources(), R.drawable.marker), 0,60, color);
        //String emojiIcon = (type.equals("city")) ? "flag000" : String.format("flag%03d", id+1);
        String emojiIcon = "flag_test";
        int resID = main.getResources().getIdentifier(emojiIcon , "drawable", main.getPackageName());
        canvas.drawBitmap(BitmapFactory.decodeResource(main.getResources(), resID), 0,14*main.metrics.density, color);
        canvas.drawBitmap(BitmapFactory.decodeResource(main.getResources(), R.drawable.flag_edge), 0,14*main.metrics.density, color);
        canvas.drawText("" + main.format.format(Math.ceil(points)), 50.0f/2*main.metrics.density, 12*main.metrics.density, stroke);
        canvas.drawText("" + main.format.format(Math.ceil(points)), 50.0f/2*main.metrics.density, 12*main.metrics.density, color);

        float anchorX = type.equals("capital") ? 0.5F : 0.1F;
        float anchorY = type.equals("capital") ? 1F : 0.98F;
        marker = main.mMap.addMarker(new MarkerOptions()
                .position(position)
                .anchor(anchorX, anchorY)
                .alpha(0.85f)
                .icon(BitmapDescriptorFactory.fromBitmap(bmp)));
        marker.setTag("Flag:"+id);
        marker.setVisible(main.isMarkerVisible(type, points));
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
        if (label != null) {
            label.remove();
            label = null;
        }
    }

    public void onFlagCaptured(int _playerId) {
        playerId = _playerId;
        clear();
        drawOnMap();
    }

    public void onFlagReleased() {
        playerId = -1;
        clear();
        drawOnMap();
    }
}