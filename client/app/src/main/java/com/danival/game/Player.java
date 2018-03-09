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
import com.google.android.gms.maps.model.Dash;
import com.google.android.gms.maps.model.Dot;
import com.google.android.gms.maps.model.Gap;
import com.google.android.gms.maps.model.GroundOverlay;
import com.google.android.gms.maps.model.GroundOverlayOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PatternItem;
import com.google.android.gms.maps.model.PolygonOptions;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


public class Player {
    protected MainActivity main;
    public int id;
    public String name;
    public int emoji;
    public boolean onLine;
    public String status;
    public LatLng position;
    public int energy;
    public int energyToRestore;
    public double flagPoints;
    public List<RouteLeg> legList;
    public Circle energyUI;
    public Circle energyToRestoreUI;
    public Circle bombLimitUI;
    public Circle turboLimitUI;
    public Marker marker;
    //public GroundOverlay label;
    private ColorMatrix colorMatrix;
    private ColorMatrixColorFilter colorFilter;

    public Player(MainActivity context, int _id, String _name, int _emoji, boolean _onLine, String _status, LatLng _position, int _energy, double _flagPoints, int _energyToRestore) {
        main = context;
        id = _id;
        name = _name;
        emoji = _emoji;
        onLine = _onLine;
        status = _status;
        position = _position;
        energy = _energy;
        energyToRestore = _energyToRestore;
        flagPoints = _flagPoints;
        legList = new ArrayList<RouteLeg>();

        colorMatrix = new ColorMatrix();
        colorMatrix.setSaturation(0);
        colorFilter = new ColorMatrixColorFilter(colorMatrix);
    }

    public void setPosition(LatLng _position) {
        position = _position;
        if (marker != null) {
            marker.setPosition(position);
            drawEnergy();
            //drawLabel();
        }
    }

    public Bitmap getIconBmp() {
        int w = Math.round(50*main.metrics.density);
        int h = Math.round(81*main.metrics.density);
        Bitmap bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
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

        String emojiIcon = String.format("emoji%03d", emoji+1);
        //String emojiIcon = String.format("emoji%03d", Math.max(id*5,1)); // testes!!!
        int resID = main.getResources().getIdentifier(emojiIcon , "drawable", main.getPackageName());
        canvas.drawBitmap(BitmapFactory.decodeResource(main.getResources(), R.drawable.marker), 0,15*main.metrics.density, color);
        canvas.drawBitmap(BitmapFactory.decodeResource(main.getResources(), resID), 5*main.metrics.density,19*main.metrics.density, color);
        //canvas1.drawText(name, 150, 40, color);
        canvas.drawText(name, 25*main.metrics.density, 12*main.metrics.density, stroke); // testes!!!
        canvas.drawText(name, 25*main.metrics.density, 12*main.metrics.density, color); // testes!!!
        return bmp;
    }

    public void drawOnMap(boolean moveCamera) {
        drawEnergy();
        //drawLabel();
        marker = main.mMap.addMarker(new MarkerOptions()
                .position(position)
                .icon(BitmapDescriptorFactory.fromBitmap(getIconBmp()))
                .alpha(1f));
        marker.setTag("Player:"+id);

        //float zoom = Math.max(1f, 19-(((float)energy - 10)/(5000-10)*(19-11))); // energia 10 = zoom 19, energia 5000 = zoom 11
        //Log.e("game", "zoom: " + zoom);
        main.game.drawRanking();
        if (moveCamera) {
            double START_ZOOM = 17;
            double zoom = START_ZOOM - Math.log((float)energy/main.START_ENERGY)/Math.log(2);
            CameraPosition cameraPosition = new CameraPosition.Builder()
                    .target(position)
                    .zoom((float)zoom)
                    .tilt(0)
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
        if (energyToRestoreUI != null) {
            energyToRestoreUI.remove();
            energyToRestoreUI = null;
        }
        if (bombLimitUI != null) {
            bombLimitUI.remove();
            bombLimitUI = null;
        }
        if (turboLimitUI != null) {
            turboLimitUI.remove();
            turboLimitUI = null;
        }
/*
        if (label != null) {
            label.remove();
            label = null;
        }
*/
        clearLegs();
        main.game.drawRanking();
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
        if (energyToRestoreUI != null)
            energyToRestoreUI.remove();
        if (energyToRestore == 0) return;
        List<PatternItem> pattern = Arrays.<PatternItem>asList(new Dash(8*main.metrics.density), new Gap(4*main.metrics.density));
        energyToRestoreUI = main.mMap.addCircle(new CircleOptions()
                .center(position)
                .radius(energy + energyToRestore) // In meters
                .strokePattern(pattern)
                .strokeColor(0xAA0000FF)
                .strokeWidth(1*main.metrics.density));
    }

/*
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
*/

    public void drawBombLimit() {
        if (bombLimitUI != null)
            bombLimitUI.remove();
        bombLimitUI = main.mMap.addCircle(new CircleOptions()
                .center(position)
                .radius(energy * main.BOMB_MAX_DIST) // In meters
                .strokeColor(0xFFAAAAAA)
                .strokeWidth(1*main.metrics.density));
    }

    public void clearBombLimit() {
        if (bombLimitUI != null) {
            bombLimitUI.remove();
            bombLimitUI = null;
        }
    }

    public void drawTurboLimit() {
        if (turboLimitUI != null)
            turboLimitUI.remove();
        turboLimitUI = main.mMap.addCircle(new CircleOptions()
                .center(position)
                .radius(energy * main.TURBO_MAX_DIST) // In meters
                .strokeColor(0xFFAAAAAA)
                .strokeWidth(1*main.metrics.density));
    }

    public void clearTurboLimit() {
        if (turboLimitUI != null) {
            turboLimitUI.remove();
            turboLimitUI = null;
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
                RouteLeg leg = new RouteLeg(main);
                long endTime = jsonLeg.getLong("endTime");
                long now = jsonLeg.getLong("now");
                leg.endTime = endTime + (System.currentTimeMillis() - now); // ajusta os tempos de acordo com o relógio do client
                leg.duration = jsonLeg.getLong("duration");
                JSONObject jsonStart = jsonLeg.getJSONObject("start");
                leg.start = new LatLng(jsonStart.getDouble("lat"), jsonStart.getDouble("lng"));
                JSONObject jsonEnd = jsonLeg.getJSONObject("end");
                leg.end = new LatLng(jsonEnd.getDouble("lat"), jsonEnd.getDouble("lng"));
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

    public void drawLegList() {
        setPosition(position);
        if (id != main.mPlayerId) return; // só desenha as legs no player que as criou
        legList.get(0).draw(position);
        for (int i = 1; i < legList.size(); i++) {
            legList.get(i).draw();
        }
    }

    public void onLegFinished(String _status, LatLng _position) {
        status = _status;
        setPosition(_position);
        if (legList.size() > 0)
            legList.get(0).clear();
    }

    public void onEnergyChange(int _energy, int _energyToRestore) {
        energy = _energy;
        energyToRestore = _energyToRestore;
        drawEnergy();
        //drawLabel();
        main.game.drawRanking();
    }

    public void onFlagPointsChange(double _flagPoints) {
        flagPoints = _flagPoints;
        //drawLabel();
        main.game.drawRanking();
    }

    public void clearLegs() {
        for (int i = 0; i < legList.size(); i++)
            legList.get(i).clear();
        legList.clear();
    }

    public void stop(LatLng _position) {
        status = "in";
        position = _position;
        clearLegs();
    }

    public void drawMoving() {
        if (!status.equals("moving")) return;
        if (legList.size() == 0) return; // Qdo loga, recebe os players e o Animator tenta já desenhar os que estão "moving". Só passar daqui se esse player já recebeu suas legs.
        RouteLeg leg = legList.get(0);
        long now = System.currentTimeMillis();
        if (now > leg.endTime) {
            status = "in";
            setPosition(leg.end);
            return;
        }
        double lat, lng;
        long startTime = leg.endTime - leg.duration; // em que miliseg
        long deltaTime = (now < startTime) ? 0 : now - startTime;
        double percent = deltaTime / (double)leg.duration;
        lat = leg.start.latitude + (leg.end.latitude - leg.start.latitude) * percent;
        double deltaLng = Math.abs(leg.end.longitude - leg.start.longitude);
        if (deltaLng < 180) {
            lng = leg.start.longitude + (leg.end.longitude - leg.start.longitude) * percent;
        } else {
            double newDeltaLngPercent = (360 - deltaLng) * percent;
            if (leg.end.longitude > leg.start.longitude) {
                lng = leg.start.longitude - newDeltaLngPercent;
                if (lng < -180)
                    lng = 360 + lng;
            } else {
                lng = leg.start.longitude + newDeltaLngPercent;
                if (lng > 180)
                    lng = lng - 360;
            }
        }
        position = new LatLng(lat, lng);
        drawLegList();
    }

    public void refreshIcon() {
        if (marker == null) return;
        marker.setIcon(BitmapDescriptorFactory.fromBitmap(getIconBmp()));
        marker.setAnchor(0.5F, 1F);
        drawEnergy();
    }

    public void showOriginMarker() {
        marker.setIcon(BitmapDescriptorFactory.fromResource(R.drawable.origin));
        marker.setAnchor(0.5F, 0.5F);
        if (energyUI != null) {
            energyUI.remove();
            energyUI = null;
        }
        if (energyToRestoreUI != null) {
            energyToRestoreUI.remove();
            energyToRestoreUI = null;
        }
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