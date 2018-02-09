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
import com.google.android.gms.maps.model.GroundOverlay;
import com.google.android.gms.maps.model.GroundOverlayOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
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
    public int energy;
    public double flagPoints;
    public List<RouteLeg> legList;
    public Circle energyUI;
    public Circle bombLimitUI;
    public Circle directLimitUI;
    public Marker marker;
    public GroundOverlay label;
    private ColorMatrix colorMatrix;
    private ColorMatrixColorFilter colorFilter;

    public Player(MainActivity context, int _id, String _name, int _emoji, boolean _onLine, String _status, double _lat, double _lng, int _energy, double _flagPoints) {
        main = context;
        id = _id;
        name = _name;
        emoji = _emoji;
        onLine = _onLine;
        status = _status;
        lat = _lat;
        lng = _lng;
        energy = _energy;
        flagPoints = _flagPoints;
        legList = new ArrayList<RouteLeg>();

        colorMatrix = new ColorMatrix();
        colorMatrix.setSaturation(0);
        colorFilter = new ColorMatrixColorFilter(colorMatrix);
    }
    public void setLocation(double _lat, double _lng) {
        lat = _lat;
        lng = _lng;
        LatLng latLng = new LatLng(lat, lng);
        if (marker != null) {
            marker.setPosition(latLng);
            drawEnergy();
            drawLabel();
        }
    }


    public Bitmap getIconBmp() {
        Bitmap bmp = Bitmap.createBitmap(300, 340, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bmp);

        Paint stroke = new Paint();
        stroke.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        stroke.setTextSize(12*main.metrics.density);
        stroke.setTextAlign(Paint.Align.CENTER);
        stroke.setColor(Color.WHITE);
        stroke.setStyle(Paint.Style.STROKE);
        stroke.setStrokeWidth(3*main.metrics.density);

        Paint color = new Paint();
        color.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        color.setTextSize(12*main.metrics.density);
        color.setTextAlign(Paint.Align.CENTER);
        color.setColor(Color.BLACK);

        if (!onLine)
            color.setColorFilter(colorFilter);

        //String emojiIcon = String.format("emoji%03d", emoji+1);
        String emojiIcon = String.format("emoji%03d", Math.max(id*5,1)); // testes!!!
        int resID = main.getResources().getIdentifier(emojiIcon , "drawable", main.getPackageName());
        canvas.drawBitmap(BitmapFactory.decodeResource(main.getResources(), R.drawable.marker), 0,60, color);
        canvas.drawBitmap(BitmapFactory.decodeResource(main.getResources(), resID), 70,80, color);
        //canvas1.drawText(name, 150, 40, color);
        canvas.drawText("Player " + id, 150, 40, stroke); // testes!!!
        canvas.drawText("Player " + id, 150, 40, color); // testes!!!
        return bmp;
    }

    public void drawOnMap(boolean moveCamera) {
        drawEnergy();
        drawLabel();
        LatLng latLng = new LatLng(lat, lng);
        marker = main.mMap.addMarker(new MarkerOptions()
                .position(latLng)
                .icon(BitmapDescriptorFactory.fromBitmap(getIconBmp()))
                .alpha(0.7f));
        marker.setTag("Player:"+id);

        //float zoom = Math.max(1f, 19-(((float)energy - 10)/(5000-10)*(19-11))); // energia 10 = zoom 19, energia 5000 = zoom 11
        //Log.e("game", "zoom: " + zoom);
        main.game.drawRanking();
        if (moveCamera) {
            double START_ZOOM = 19;
            double zoom = START_ZOOM - Math.log((float)energy/main.START_ENERGY)/Math.log(2);
            CameraPosition cameraPosition = new CameraPosition.Builder()
                    .target(latLng)
                    .zoom((float)zoom)
                    .tilt(60)
                    .build();
            main.mMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
        }
    }

    public void removeFromMap() {
        if (marker != null) {
            marker.remove();
            marker = null;
        }
        if (energyUI != null) {
            energyUI.remove();
            energyUI = null;
        }
        if (bombLimitUI != null) {
            bombLimitUI.remove();
            bombLimitUI = null;
        }
        if (directLimitUI != null) {
            directLimitUI.remove();
            directLimitUI = null;
        }
        if (label != null) {
            label.remove();
            label = null;
        }
        clearLegs();
        main.game.drawRanking();
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
        color.setTextSize(9*main.metrics.density);
        color.setTextAlign(Paint.Align.CENTER);
        color.setColor(0x55000055);
        color.setShadowLayer(0.5f, 1.0f, 1.0f, Color.WHITE);
        if (flagPoints > 0) {
            canvasLabel.drawText("+" + main.format.format(energy), 80, 100, color);
            canvasLabel.drawText("$" + main.format.format(Math.ceil(flagPoints)), 80, 140, color);
        } else {
            canvasLabel.drawText("+" + main.format.format(energy), 80, 120, color);
        }
        label = main.mMap.addGroundOverlay(new GroundOverlayOptions()
                .image(BitmapDescriptorFactory.fromBitmap(bmpLabel))
                .position(latLng, (float)energy*2, (float)energy*2));
    }

    public void drawBombLimit() {
        LatLng latLng = new LatLng(lat, lng);
        if (bombLimitUI != null)
            bombLimitUI.remove();
        bombLimitUI = main.mMap.addCircle(new CircleOptions()
                .center(latLng)
                .radius((energy - main.BOMB_UNIT_COST - main.START_ENERGY) * main.BOMB_MAX_DIST) // In meters
                .strokeColor(0xFFAAAAAA)
                .strokeWidth(1*main.metrics.density));
    }

    public void clearBombLimit() {
        if (bombLimitUI != null) {
            bombLimitUI.remove();
            bombLimitUI = null;
        }
    }

    public void drawDirectLimit(LatLng latLng, int legCount, int totalRouteDistance) {
        if (directLimitUI != null)
            directLimitUI.remove();
        Log.e("game", "drawDirectLimit");
        long energyToGo = energy - (legCount * main.DIRECT_UNIT_COST) - main.START_ENERGY;
        long maxDist = energyToGo * main.DIRECT_MAX_DIST;
        maxDist -= totalRouteDistance;
        Log.e("game", "drawDirectLimit - maxDist: " + maxDist);
        directLimitUI = main.mMap.addCircle(new CircleOptions()
                .center(latLng)
                .radius(maxDist) // In meters
                .strokeColor(0xFFAAAAAA)
                .strokeWidth(1*main.metrics.density));
    }

    public void clearDirectLimit() {
        if (directLimitUI != null) {
            directLimitUI.remove();
            directLimitUI = null;
        }
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
        main.game.drawRanking();
        if (marker == null) // no login, se este player já estava em movimento, ainda não está desenhado no mapa (até esse ponto não tem como saber a posição em que se encontra)
            drawOnMap(id == main.mPlayerId);
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
        if (legList.size() > 0)
            legList.get(0).clear();
    }

    public void onEnergyChange(int _energy) {
        energy = _energy;
        drawEnergy();
        drawLabel();
        main.game.drawRanking();
    }

    public void onFlagPointsChange(double _flagPoints) {
        flagPoints = _flagPoints;
        drawLabel();
        main.game.drawRanking();
    }

    public void clearLegs() {
        for (int i = 0; i < legList.size(); i++)
            legList.get(i).clear();
        legList.clear();
    }


    public void stop(double _lat, double _lng) {
        status = "in";
        lat = _lat;
        lng = _lng;
        clearLegs();
    }

    public void drawMoving() {
        if (!status.equals("moving")) return;
        if (legList.size() == 0) return; // Qdo loga, recebe os players e o Animator tenta já desenhar os que estão "moving". Só passar daqui se esse player já recebeu suas legs.
        RouteLeg leg = legList.get(0);
        long now = System.currentTimeMillis();
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
            double deltaLng = Math.abs(point2.lng - point1.lng);
            if (deltaLng < 180) {
                lng = point1.lng + (point2.lng - point1.lng) * percent;
            } else {
                double newDeltaLngPercent = (360 - deltaLng) * percent;
                if (point2.lng > point1.lng) {
                    lng = point1.lng - newDeltaLngPercent;
                    if (lng < -180)
                        lng = 360 + lng;
                } else {
                    lng = point1.lng + newDeltaLngPercent;
                    if (lng > 180)
                        lng = lng - 360;
                }
            }
        }
        drawLegList(j);
    }

    public void refreshIcon() {
        if (marker == null) return;
        marker.setIcon(BitmapDescriptorFactory.fromBitmap(getIconBmp()));
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
