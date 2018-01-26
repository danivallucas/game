package com.danival.game;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolygonOptions;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;


public class Player {
    protected MainActivity main;
    public int id;
    public String name;
    public int emoji;
    public boolean onLine;
    public String status;
    public double lat;
    public double lng;
    public long energy;
    public List<RouteLeg> legList;
    public Circle energyUI;
    public Marker marker;

    public Player(MainActivity context, int _id, String _name, int _emoji, boolean _onLine, String _status, double _lat, double _lng, long _energy) {
        main = context;
        id = _id;
        name = _name;
        emoji = _emoji;
        onLine = _onLine;
        status = _status;
        lat = _lat;
        lng = _lng;
        energy = _energy;
        legList = new ArrayList<RouteLeg>();
    }
    public void setLocation(double _lat, double _lng) {
        lat = _lat;
        lng = _lng;
        LatLng latLng = new LatLng(lat, lng);
        if (marker != null) {
            marker.setPosition(latLng);
            drawEnergy();
        }
    }

    public void drawOnMap(boolean moveCamera) {
        drawEnergy();
        LatLng latLng = new LatLng(lat, lng);
        Bitmap.Config conf = Bitmap.Config.ARGB_8888;
        Bitmap bmp = Bitmap.createBitmap(300, 340, conf);
        Canvas canvas1 = new Canvas(bmp);

        Paint color = new Paint();
        color.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        color.setTextSize(50);
        color.setTextAlign(Paint.Align.CENTER);
        color.setColor(Color.BLACK);
        color.setShadowLayer(2.0f, 2.0f, 2.0f, Color.WHITE);

        String emojiIcon = String.format("emoji%03d", emoji+1);
        int resID = main.getResources().getIdentifier(emojiIcon , "drawable", main.getPackageName());
        canvas1.drawBitmap(BitmapFactory.decodeResource(main.getResources(), R.drawable.marker), 0,60, color);
        canvas1.drawBitmap(BitmapFactory.decodeResource(main.getResources(), resID), 70,80, color);
        canvas1.drawText(name, 150, 40, color);
        marker = main.mMap.addMarker(new MarkerOptions()
                .position(latLng)
                .icon(BitmapDescriptorFactory.fromBitmap(bmp))
                .alpha(0.7f));
        marker.setTag("Player:"+id);
        // Specifies the anchor to be at a particular point in the marker image.
        //.anchor(0.5f, 1));
        if (moveCamera) {
            CameraPosition cameraPosition = new CameraPosition.Builder()
                    .target(latLng)
                    .zoom(18)
                    .tilt(60)
                    .build();
            main.mMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));

        }
    }

    public void drawEnergy() {
        LatLng latLng = new LatLng(lat, lng);
        if (energyUI != null)
            energyUI.remove();
        energyUI = main.mMap.addCircle(new CircleOptions()
                .center(latLng)
                .radius(energy) // In meters
                .fillColor(0x330000FF)
                .strokeColor(0xAA0000FF)
                .strokeWidth(0));

    }

    public void onMove(JSONArray jsonLegList) {
        for (int i = 0; i < legList.size(); i++)
            legList.get(i).clear();
        legList.clear();
        try {
            // monta a legList
            Log.e("game", "onMove");
            for (int i = 0; i < jsonLegList.length(); i++) {
                JSONObject jsonLeg = jsonLegList.getJSONObject(i);
                RouteLeg leg = new RouteLeg();
                long endTime = jsonLeg.getLong("endTime");
                long now = jsonLeg.getLong("now");
                leg.endTime = endTime + (System.currentTimeMillis() - now); // ajusta os tempos de acordo com o relógio do client
                leg.totalDuration = jsonLeg.getLong("totalDuration");
                JSONArray jsonPointList = jsonLeg.getJSONArray("pointList");
                for (int j = 0; j < jsonPointList.length(); j++) {
                    JSONObject jsonPoint = jsonPointList.getJSONObject(j);
                    Point point = new Point(jsonPoint.getDouble("lat"), jsonPoint.getDouble("lng"), jsonPoint.getLong("duration"));
                    leg.addPoint(point);
                }
                legList.add(leg);
            }
        } catch (JSONException e) { Log.e("game", Log.getStackTraceString(e)); }
        status = "moving";
        drawMoving(); // para calcular a posição de acordo com o tempo de movimento decorrido
        if (marker == null) // se logou e este player já estava em movimento, ainda não está desenhado no mapa
            drawOnMap(true);
        // o Animator continuará movendo este player no mapa
    }

    public void drawLegList(int pointIndex) {
        setLocation(lat, lng);
        legList.get(0).draw(main, new LatLng(lat, lng), pointIndex);
        if (id != main.mPlayerId) return; // só desenha as demais legs no player que as criou
        for (int i = 1; i < legList.size(); i++) {
            legList.get(i).draw(main);
        }
    }

    public void onLegFinished(String _status, double lat, double lng) {
        status = _status;
        setLocation(lat, lng);
        legList.get(0).clear();
    }

    public void onGrow(long _energy) {
        energy = _energy;
        drawEnergy();
    }

    public void stop(double _lat, double _lng) {
        status = "in";
        lat = _lat;
        lng = _lng;
        for (int i = 0; i < legList.size(); i++)
            legList.get(i).clear();
        legList.clear();
    }

    public void drawMoving() {
        if (!status.equals("moving")) return;
        long now = System.currentTimeMillis();
        RouteLeg leg = legList.get(0);
        if (now > leg.endTime) {
            status = "in";
            Point lastPoint = leg.pointList.get(leg.pointList.size()-1);
            setLocation(lastPoint.lat, lastPoint.lng);
            return;
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
            lat = point1.lat;
            lng = point1.lng;
        } else {
            Point point2 = leg.pointList.get(++j);
            double percent = 1 - ( (sum - pos) / (double)point1.duration );
            lat = point1.lat + (point2.lat - point1.lat) * percent;
            lng = point1.lng + (point2.lng - point1.lng) * percent;
        }
        drawLegList(j);
    }



/*
    public void drawArea(double lat1, double lng1, double lat2, double lng2) {
        main.mMap.addPolygon(new PolygonOptions()
                .add(new LatLng(lat1, lng1), new LatLng(lat1, lng2), new LatLng(lat2, lng2), new LatLng(lat2, lng1))
                .strokeColor(Color.RED)
                .fillColor(Color.BLUE));
    }
*/


}
