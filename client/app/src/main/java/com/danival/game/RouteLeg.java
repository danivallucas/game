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
    public List<Point> pointList;
    public long totalDuration;
    public long endTime;
    public Polyline startPolyline;
    public Polyline middlePolyline;
    public Polyline endPolyline;
    public boolean drawingFirstTime;

    public RouteLeg() {
        pointList = new ArrayList<Point>();
        totalDuration = 0;
        endTime = 0;
        drawingFirstTime = true;
    }

    public void addPoint(Point p) {
        pointList.add(p);
    }

    public void clear() {
        if (startPolyline != null) startPolyline.remove();
        if (middlePolyline != null) middlePolyline.remove();
        if (endPolyline != null) endPolyline.remove();
    }

    // desenha essa leg completa (do 1o ao último ponto), pois o player não está nela
    public void draw(MainActivity context) {
        if (!drawingFirstTime) return; // só desenha essa leg uma vez após ser criada
        LatLng latLng = new LatLng(pointList.get(0).lat, pointList.get(0).lng);
        draw(context, latLng, 1);
    }

    // context: MainActivity
    // latLng: posição atual do player (iniciar a leg daqui)
    // pointIndex: primeiro ponto da leg a ser desenhado
    // Obs.: primeira linha vai do latLng ao pointIndex... o player está nessa leg
    public void draw(MainActivity context, LatLng latLng, int pointIndex) {
        clear();
        // draw
        List<PatternItem> pattern = Arrays.<PatternItem>asList(new Dot(), new Gap(15));
        LatLng latLng1, latLng2; // primeira linha
        List<LatLng> middlePath = new ArrayList<LatLng>(); // linhas do meio
        LatLng latLng3, latLng4; // última linha

        if (pointIndex < pointList.size()-1) {
            if (pointIndex == 1) {
                latLng1 = latLng;
                latLng2 = new LatLng(pointList.get(1).lat, pointList.get(1).lng);
                startPolyline = context.mMap.addPolyline(new PolylineOptions().width(30).color(0xCC3B7AC9).jointType(JointType.ROUND).pattern(pattern).add(latLng1, latLng2));
            } else {
                middlePath.add(latLng);
            }
            for (int i = pointIndex; i < pointList.size()-1; i++)
                middlePath.add(new LatLng(pointList.get(i).lat, pointList.get(i).lng));
            middlePolyline = context.mMap.addPolyline(new PolylineOptions().width(30).color(0xCC3B7AC9).jointType(JointType.ROUND).addAll(middlePath));
            latLng3 = new LatLng(pointList.get(pointList.size()-2).lat, pointList.get(pointList.size()-2).lng);
        } else {
            latLng3 = latLng;
        }
        latLng4 = new LatLng(pointList.get(pointList.size()-1).lat, pointList.get(pointList.size()-1).lng);
        endPolyline = context.mMap.addPolyline(new PolylineOptions().width(30).color(0xCC3B7AC9).jointType(JointType.ROUND).pattern(pattern).startCap(new CustomCap(BitmapDescriptorFactory.fromResource(R.drawable.point),64)).add(latLng4, latLng3)); // ao contrário (efeito na linha pontilhada no DIRECT)
        drawingFirstTime =false;
    }

}
