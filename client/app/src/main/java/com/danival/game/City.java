package com.danival.game;

import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;

public class City {
  protected MainActivity main;
  public int id;
  public LatLng location;
  public long population;
  public Marker marker;

  public City(MainActivity context, int _id, LatLng _location, long _population) {
    main = context;
    id = _id;
    location = _location;
    population = _population;
  }

}
