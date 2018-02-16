package com.danival.game;

import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.CustomCap;
import com.google.android.gms.maps.model.Dot;
import com.google.android.gms.maps.model.Gap;
import com.google.android.gms.maps.model.JointType;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.PatternItem;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class RouteLeg {
    protected MainActivity main;
    public LatLng start;
    public LatLng end;
    public long duration;
    public long endTime;
    public Polyline polyline;
    public boolean drawingFirstTime;

    public RouteLeg(MainActivity context) {
        main = context;
        duration = 0;
        endTime = 0;
        drawingFirstTime = true;
    }

    public void clear() {
        if (polyline != null) polyline.remove();
    }

    // desenha essa leg completa, pois o player não está nela
    public void draw() {
        if (!drawingFirstTime) return; // só desenha essa leg uma vez após ser criada
        draw(start);
    }

    // from: posição atual do player (iniciar a leg daqui)
    // Obs.: linha vai do from ao end... o player está nessa leg
    public void draw(LatLng from) {
        clear();
        List<PatternItem> pattern = Arrays.<PatternItem>asList(new Dot(), new Gap(4*main.metrics.density));
        int lineW = Math.round(7*main.metrics.density);
        int pointW = Math.round(16*main.metrics.density);
        polyline = main.mMap.addPolyline(new PolylineOptions()
                .width(lineW)
                .color(0xCC3B7AC9)
                .jointType(JointType.ROUND)
                .pattern(pattern)
                .startCap(new CustomCap(BitmapDescriptorFactory.fromResource(R.drawable.point),pointW))
                .add(end, from)); // ao contrário (efeito de "ir comendo" a linha pontilhada)
        drawingFirstTime =false;
    }

}