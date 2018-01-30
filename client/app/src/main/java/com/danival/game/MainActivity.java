package com.danival.game;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Typeface;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.Projection;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.CustomCap;
import com.google.android.gms.maps.model.Dash;
import com.google.android.gms.maps.model.Dot;
import com.google.android.gms.maps.model.Gap;
import com.google.android.gms.maps.model.JointType;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MapStyleOptions;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PatternItem;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.maps.DirectionsApi;
import com.google.maps.GeoApiContext;
import com.google.maps.android.PolyUtil;
import com.google.maps.android.SphericalUtil;
import com.google.maps.model.DirectionsLeg;
import com.google.maps.model.DirectionsResult;
import com.google.maps.model.DirectionsRoute;
import com.google.maps.model.DirectionsStep;
import com.google.maps.model.GeocodedWaypointStatus;
import com.google.maps.model.TravelMode;

import org.joda.time.DateTime;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import io.socket.client.Socket;
import io.socket.emitter.Emitter;

public class MainActivity extends FragmentActivity implements OnMapReadyCallback, GoogleMap.OnMapClickListener, GoogleMap.OnCameraMoveListener, GoogleMap.OnMarkerClickListener {

    private App app;
    protected GoogleMap mMap;
    private LatLng mLatLng;
    private int usr = 1;
    private FusedLocationProviderClient mFusedLocationClient;
    private ColorMatrix colorMatrix;
    private ColorMatrixColorFilter colorFilter;
    private SharedPreferences sharedPref; // arq configurações
    protected Metrics metrics;
    private static final int REQUEST_LOGIN = 0;
    protected int mPlayerId;
    private String mPlayerName;
    private int mEmoji;
    private String mPlayerToken;
    private int ENERGY_PER_DIRECT_LEG;
    private int START_ENERGY;
    public Socket mSocket;
    private boolean isConnected = false;
    private boolean isLoggedIn = false;
    private boolean isSpawning = false;
    private boolean isBuildingRoute = false;
    protected TextView status;
    protected Game game;
    public Handler handler; // Animator (movimenta os markers). Iniciado depois do Login com sucesso.
    private List<LatLng> routeLocations;
    private Polyline routePolyline;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        app = (App) this.getApplication();

        colorMatrix = new ColorMatrix();
        colorMatrix.setSaturation(0);
        colorFilter = new ColorMatrixColorFilter(colorMatrix);
        sharedPref = getPreferences(Context.MODE_PRIVATE);
        mPlayerName = "";
        setUpGame();
        setUpSocket();
        initMap();
        // Status bar
        status = (TextView) this.findViewById(R.id.status);
        status.setX(10);
        status.setY(100);
        //status.setText("[Status]");
        routeLocations = new ArrayList<LatLng>();
    }

    private void setUpSocket() {
        mSocket = app.getSocket();
        mSocket.on(Socket.EVENT_CONNECT,onConnect);
        mSocket.on(Socket.EVENT_DISCONNECT,onDisconnect);
        mSocket.on(Socket.EVENT_CONNECT_ERROR, onConnectError);
        mSocket.on(Socket.EVENT_CONNECT_TIMEOUT, onConnectError);
        mSocket.on("onTest", onTest);
        mSocket.on("onCreatePlayerResult", onCreatePlayerResult);
        mSocket.on("onNewFlag", onNewFlag);
        mSocket.on("onNewPlayer", onNewPlayer);
        mSocket.on("onNewFood", onNewFood);
        mSocket.on("onRemoveFood", onRemoveFood);
        mSocket.on("onLogin", onLogin);
        mSocket.on("onLogout", onLogout);
        mSocket.on("onSpawn", onSpawn);
        mSocket.on("onMove", onMove);
        mSocket.on("onLegFinished", onLegFinished);
        mSocket.on("onEnergyChange", onEnergyChange);
        mSocket.on("onFlagPointsChange", onFlagPointsChange);
        mSocket.on("onStop", onStop);
        mSocket.on("onPlayerOut", onPlayerOut);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        //mSocket.disconnect();
        mSocket.off(Socket.EVENT_CONNECT,onConnect);
        mSocket.off(Socket.EVENT_DISCONNECT,onDisconnect);
        mSocket.off(Socket.EVENT_CONNECT_ERROR, onConnectError);
        mSocket.off(Socket.EVENT_CONNECT_TIMEOUT, onConnectError);
        mSocket.off("onTest", onTest);
        mSocket.off("onCreatePlayerResult", onCreatePlayerResult);
        mSocket.off("onNewFlag", onNewFlag);
        mSocket.off("onNewPlayer", onNewPlayer);
        mSocket.off("onNewFood", onNewFood);
        mSocket.off("onRemoveFood", onRemoveFood);
        mSocket.off("onLogin", onLogin);
        mSocket.off("onLogout", onLogout);
        mSocket.off("onSpawn", onSpawn);
        mSocket.off("onMove", onMove);
        mSocket.off("onLegFinished", onLegFinished);
        mSocket.off("onEnergyChange", onEnergyChange);
        mSocket.off("onFlagPointsChange", onFlagPointsChange);
        mSocket.off("onStop", onStop);
        mSocket.off("onPlayerOut", onPlayerOut);
    }

    private void setUpGame() {
        game = new Game(this);
        metrics = new Metrics(this);
    }

    @Override
    public void onRestart() {
        super.onRestart();
        if (!isConnected) {
            enterGame();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        app.setTag(0);
    }

    @Override
    public void onStop() {
        super.onStop();
        if ( isConnected && (app.getTag() == 0) ) {
            mSocket.disconnect();
            game.clear();
        }
    }

    private void startSignIn() {
        mPlayerName = "";
        Intent intent = new Intent(this, LoginActivity.class);
        startActivityForResult(intent, REQUEST_LOGIN);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (Activity.RESULT_OK != resultCode) {
            this.finish();
            return;
        }

        try {
            mPlayerName = data.getStringExtra("mPlayerName");
            mEmoji = data.getIntExtra("mEmoji", 0);
            mSocket.emit("createPlayer", mPlayerName, mEmoji);
            //Toast.makeText(getApplicationContext(), "escolhido: " + mEmoji, Toast.LENGTH_LONG).show();
        } catch(NumberFormatException nfe) {}

    }

    private void afterConnect() {
        mPlayerId = sharedPref.getInt("id", 0);
        mPlayerToken = sharedPref.getString("token", "");
        if (mPlayerToken.equals("")) {
            startSignIn();
        }
        else {
            mSocket.emit("login", mPlayerId, mPlayerToken);
        }
    }

    private void exitGame() {
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putInt("id", mPlayerId);
        editor.putString("token", mPlayerToken);
        editor.commit();
        if (isLoggedIn) {
            mMap.clear();
        }
        isLoggedIn = false;
        cancelRoute(null);
    }

    private Emitter.Listener onConnect = new Emitter.Listener() {
        @Override
        public void call(Object... args) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    isConnected = true;
                    afterConnect();
                }
            });
        }
    };

    private Emitter.Listener onDisconnect = new Emitter.Listener() {
        @Override
        public void call(Object... args) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    isConnected = false;
                    exitGame();
                }
            });
        }
    };

    private Emitter.Listener onConnectError = new Emitter.Listener() {
        @Override
        public void call(Object... args) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(getApplicationContext(), R.string.error_connect, Toast.LENGTH_LONG).show();
                }
            });
        }
    };

    private Emitter.Listener onCreatePlayerResult = new Emitter.Listener() {
        @Override
        public void call(final Object... args) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    try {
                        JSONObject data = (JSONObject) args[0];
                        mPlayerId = data.getInt("id");
                        mPlayerToken = data.getString("token");
                        mSocket.emit("login", mPlayerId, mPlayerToken);
                    } catch (JSONException e) { Log.e("game", Log.getStackTraceString(e)); }
                }
            });
        }
    };

    private Emitter.Listener onLogin = new Emitter.Listener() {
        @Override
        public void call(final Object... args) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    try {
                        JSONObject data = (JSONObject) args[0];
                        int id = data.getInt("id");
                        int resultCode = data.getInt("resultCode");
                        if (id == mPlayerId) {
                            if (resultCode == 0) {
                                START_ENERGY = data.getInt("START_ENERGY");
                                ENERGY_PER_DIRECT_LEG = data.getInt("ENERGY_PER_DIRECT_LEG");
                                isLoggedIn = true;
                                // inicia as animações de movimento
                                if (handler == null) {
                                    handler = new Handler();
                                    handler.postDelayed(new Animator(MainActivity.this), 80);
                                }
                            } else {
                                mPlayerId = 0;
                                mPlayerToken = "";
                                startSignIn();
                            }
                        } else {
                            Player player = game.getPlayer(id);
                            player.onLine = true;
                            if (player.status.equals("in"))
                                player.refreshIcon();
                        }
                    } catch (JSONException e) { Log.e("game", Log.getStackTraceString(e)); }
                }
            });
        }
    };

    private Emitter.Listener onNewFlag = new Emitter.Listener() {
        @Override
        public void call(final Object... args) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    try {
                        Log.e("game", "onNewFlag.....");
                        JSONArray list = (JSONArray) args[0];
                        for (int i = 0; i < list.length(); i++) {
                            JSONObject data = list.getJSONObject(i);
                            Flag flag = game.newFlag(data.getInt("id"), data.getString("city"), data.getString("country"), data.getLong("population"), data.getDouble("lat"), data.getDouble("lng"), data.getLong("wall"), data.getInt("playerId"));
                            flag.drawOnMap();
                        }
                    } catch (JSONException e) { Log.e("game", Log.getStackTraceString(e)); }
                }
            });
        }
    };

    private Emitter.Listener onNewPlayer = new Emitter.Listener() {
        @Override
        public void call(final Object... args) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    try {
                        JSONArray list = (JSONArray) args[0];
                        for (int i = 0; i < list.length(); i++) {
                            JSONObject data = list.getJSONObject(i);
                            Player player = game.newPlayer(data.getInt("id"), data.getString("name"), data.getInt("emoji"), data.getBoolean("onLine"), data.getString("status"), data.getDouble("lat"), data.getDouble("lng"), data.getLong("energy"));
                            // draw on map
                            if (player.status.equals("in"))
                                player.drawOnMap(player.id == mPlayerId);
                            // buttons
                            if (player.id == mPlayerId) {
                                if (player.status.equals("in")) {
                                    findViewById(R.id.btnStopPlayer).setVisibility(View.GONE);
                                    findViewById(R.id.btnStartRoute).setVisibility(View.VISIBLE);
                                }
                                if (player.status.equals("out")) {
                                    goSpawnState();
                                }
                            }
                        }
                    } catch (JSONException e) { Log.e("game", Log.getStackTraceString(e)); }
                }
            });
        }
    };

    private Emitter.Listener onLogout = new Emitter.Listener() {
        @Override
        public void call(final Object... args) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    JSONObject data = (JSONObject) args[0];
                    try {
                        Player player = game.getPlayer(data.getInt("id"));
                        player.onLine = false;
                        if (!player.status.equals("out"))
                            player.refreshIcon();
                    } catch (JSONException e) { Log.e("game", Log.getStackTraceString(e)); }
                }
            });
        }
    };
    private Emitter.Listener onSpawn = new Emitter.Listener() {
        @Override
        public void call(final Object... args) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    try {
                        JSONObject data = (JSONObject) args[0];
                        int id = data.getInt("id");
                        Player player = game.getPlayer(id);
                        player.status = data.getString("status");
                        player.energy = data.getLong("energy");
                        player.setLocation(data.getDouble("lat"), data.getDouble("lng"));
                        player.drawOnMap((id == mPlayerId));
                        //player.drawArea(data.getDouble("lat1"), data.getDouble("lng1"), data.getDouble("lat2"), data.getDouble("lng2"));
                        if (id == mPlayerId) {
                            isSpawning = false;
                            findViewById(R.id.btnStartRoute).setVisibility(View.VISIBLE);
                        }

                    } catch (JSONException e) { Log.e("game", Log.getStackTraceString(e)); }
                }
            });
        }
    };

    private Emitter.Listener onNewFood = new Emitter.Listener() {
        @Override
        public void call(final Object... args) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    try {
                        JSONArray list = (JSONArray) args[0];
                        for (int i = 0; i < list.length(); i++) {
                            JSONObject data = list.getJSONObject(i);
                            Food food = game.newFood(data.getInt("id"), data.getInt("type"), data.getDouble("lat"), data.getDouble("lng"), data.getLong("energy"));
                            food.drawOnMap();
                        }
                    } catch (JSONException e) { Log.e("game", Log.getStackTraceString(e)); }
                }
            });
        }
    };

    private Emitter.Listener onRemoveFood = new Emitter.Listener() {
        @Override
        public void call(final Object... args) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    try {
                        JSONObject data = (JSONObject) args[0];
                        //Toast.makeText(getApplicationContext(), "food: " + data.getInt("id"), Toast.LENGTH_LONG).show();
                        game.removeFood(data.getInt("id"));

                    } catch (JSONException e) { Log.e("game", Log.getStackTraceString(e)); }
                }
            });
        }
    };

    private Emitter.Listener onMove = new Emitter.Listener() {
        @Override
        public void call(final Object... args) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    try {
                        JSONObject data = (JSONObject) args[0];
                        int id = data.getInt("id");
                        JSONArray legList = data.getJSONArray("legList");
                        Player player = game.getPlayer(id);
                        player.energy = data.getInt("energy");
                        player.drawEnergy();
                        //player.setStatus(data.getString("status"));
                        player.onMove(legList);
                    } catch (JSONException e) { Log.e("game", Log.getStackTraceString(e)); }
                }
            });
        }
    };

    private Emitter.Listener onLegFinished = new Emitter.Listener() {
        @Override
        public void call(final Object... args) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    try {
                        JSONObject data = (JSONObject) args[0];
                        int id = data.getInt("id");
                        Player player = game.getPlayer(id);
                        if ( (player.id == mPlayerId) && (player.legList.size() == 1) ) {
                            // se é a última leg, esconde o STOP
                            findViewById(R.id.btnStopPlayer).setVisibility(View.GONE);
                            findViewById(R.id.btnStartRoute).setVisibility(View.VISIBLE);
                        }
                        player.onLegFinished(data.getString("status"), data.getDouble("lat"), data.getDouble("lng"));
                    } catch (JSONException e) { Log.e("game", Log.getStackTraceString(e)); }
                }
            });
        }
    };

    private Emitter.Listener onEnergyChange = new Emitter.Listener() {
        @Override
        public void call(final Object... args) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    try {
                        JSONObject data = (JSONObject) args[0];
                        int id = data.getInt("id");
                        Player player = game.getPlayer(id);
                        player.onEnergyChange(data.getLong("energy"));
                    } catch (JSONException e) { Log.e("game", Log.getStackTraceString(e)); }
                }
            });
        }
    };

    private Emitter.Listener onFlagPointsChange = new Emitter.Listener() {
        @Override
        public void call(final Object... args) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    try {
                        JSONObject data = (JSONObject) args[0];
                        int id = data.getInt("id");
                        Player player = game.getPlayer(id);
                        player.onFlagPointsChange(data.getLong("flagPoints"));
                    } catch (JSONException e) { Log.e("game", Log.getStackTraceString(e)); }
                }
            });
        }
    };

    private Emitter.Listener onStop = new Emitter.Listener() {
        @Override
        public void call(final Object... args) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    try {
                        JSONObject data = (JSONObject) args[0];
                        int id = data.getInt("id");
                        Toast.makeText(getApplicationContext(), "onStop: " + id, Toast.LENGTH_LONG).show();
                        Player player = game.getPlayer(id);
                        player.stop(data.getDouble("lat"), data.getDouble("lng"));
                    } catch (JSONException e) { Log.e("game", Log.getStackTraceString(e)); }
                }
            });
        }
    };

    private Emitter.Listener onPlayerOut = new Emitter.Listener() {
        @Override
        public void call(final Object... args) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    try {
                        JSONObject data = (JSONObject) args[0];
                        int id = data.getInt("id");
                        Player player = game.getPlayer(id);
                        player.status = data.getString("status");
                        player.removeFromMap();
                        if (id == mPlayerId) {
                            cancelRoute(null);
                            goSpawnState();
                        }

                    } catch (JSONException e) { Log.e("game", Log.getStackTraceString(e)); }
                }
            });
        }
    };

        private Emitter.Listener onTest = new Emitter.Listener() {
        @Override
        public void call(final Object... args) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    try {
                        JSONArray list = (JSONArray) args[0];
                        String s = "";
                        for (int i = 0; i < list.length(); i++) {
                            if (list.get(i) == null) continue;
                            JSONObject data = list.getJSONObject(i);
                            s += " id: " + data.getInt("id");
                        }
                        Toast.makeText(getApplicationContext(), "ids: " + s, Toast.LENGTH_LONG).show();
                    } catch (JSONException e) { Log.e("game", Log.getStackTraceString(e)); }
                }
            });
        }
    };

    private void initMap() {
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(MainActivity.this);
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mMap.setOnMapClickListener(this);
        mMap.setOnCameraMoveListener(this);
        mMap.setOnMarkerClickListener(this);
        mMap.setMapStyle(
                MapStyleOptions.loadRawResourceStyle(
                        this, R.raw.style_json));

        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        mFusedLocationClient.getLastLocation()
                .addOnSuccessListener(this, new OnSuccessListener<Location>() {
                    @Override
                    public void onSuccess(Location location) {
                        if (location != null) {
                            mLatLng = new LatLng(location.getLatitude(), location.getLongitude());
                            enterGame();
                        }
                    }
                });

    }

    private void enterGame() {
        mSocket.connect();
    }

    private void goSpawnState() {
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(mLatLng, 10f));
        CameraPosition cameraPosition = new CameraPosition.Builder()
                .target(mLatLng)
                .zoom(15)
                .tilt(60)
                .build();
        mMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
        isSpawning = true;
    }


    @Override
    public void onCameraMove() {
        // tenta setar a stroke conforme o zoom, mas dá negativo se o mapa está rotacionado...
        Projection projection = mMap.getProjection();
        for (Player player : game.playerList) {
            if (player.energyUI == null) continue;
            LatLng newLatLng = SphericalUtil.computeOffset(player.energyUI.getCenter(), 1200, 270);
            double latDif = player.energyUI.getCenter().latitude - newLatLng.latitude;
            double lngDif = player.energyUI.getCenter().longitude - newLatLng.longitude;
            //status.setText("lat: " + latDif + " lng: " + lngDif);
/*
            Point p1 = projection.toScreenLocation(player.energyUI.getCenter());
            Point p2 = projection.toScreenLocation(newLatLng);
            player.energyUI.setStrokeWidth(p1.y - p2.y);
*/
        }
    }

    private GeoApiContext getGeoContext() {
        GeoApiContext geoApiContext = new GeoApiContext();
        return geoApiContext.setQueryRateLimit(50)
                .setApiKey("AIzaSyDF8VQrY6wyQaF3d9uAzkn4fZZYWqs44_M")
                .setConnectTimeout(10, TimeUnit.SECONDS)
                .setReadTimeout(10, TimeUnit.SECONDS)
                .setWriteTimeout(10, TimeUnit.SECONDS);
    }

    private String encodeRoute(DirectionsResult result) {
        JSONArray legArray = new JSONArray();
        try {
            for (int i = 0; i < routeLocations.size()-1; i++) {
                JSONArray pointArray = new JSONArray();
                JSONObject pointObject = new JSONObject();
                pointObject.put("lat", routeLocations.get(i).latitude);
                pointObject.put("lng", routeLocations.get(i).longitude);
                pointArray.put(pointObject);

                if (result != null) {
                    DirectionsLeg leg = result.routes[0].legs[i];
                    for (int j = 0; j < leg.steps.length; j++) {
                        DirectionsStep step = leg.steps[j];
                        List<com.google.maps.model.LatLng> polyline = step.polyline.decodePath();
                        for (int k = 0; k < polyline.size(); k++) {
                            pointObject = new JSONObject();
                            pointObject.put("lat", polyline.get(k).lat);
                            pointObject.put("lng", polyline.get(k).lng);
                            pointArray.put(pointObject);
                        }
                    }
                }

                pointObject = new JSONObject();
                pointObject.put("lat", routeLocations.get(i+1).latitude);
                pointObject.put("lng", routeLocations.get(i+1).longitude);
                pointArray.put(pointObject);

                JSONObject legObject = new JSONObject();
                legObject.put("pointList", pointArray);
                legArray.put(legObject);
            }

        } catch (Exception e) {
            Log.e("game", Log.getStackTraceString(e));
        }
        return legArray.toString();
    }

    private void addRouteLocation(LatLng latLng) {
        routeLocations.add(latLng);
        if (routeLocations.size() < 2) return;
        if (routePolyline != null)
            routePolyline.remove();
        routePolyline = mMap.addPolyline(
                new PolylineOptions()
                        .width(30)
                        .color(0xCC3B7AC9)
                        .jointType(JointType.ROUND)
                        .endCap(new CustomCap(BitmapDescriptorFactory.fromResource(R.drawable.arrow),32))
                        .addAll(routeLocations)
        );
    }

    @Override
    public void onMapClick(LatLng latLng) {
        Log.e("game", "onMapClick");
        if (isSpawning) {
            mSocket.emit("spawn", latLng.latitude, latLng.longitude);
            return;
        }
        if (isBuildingRoute) {
            addRouteLocation(latLng);
            return;
        }
    }

    @Override
    public boolean onMarkerClick(final Marker marker) {
        Log.e("game", "onMarkerClick");
        String tag = (String) marker.getTag();
        String[] list = tag.split(":");
        String type = list[0];
        int id = Integer.parseInt(list[1]);
        switch (type) {
            case "Player":
                break;
            case "Food":
                if (isBuildingRoute) {
                    Food food = game.getFood(id);
                    addRouteLocation(new LatLng(food.lat, food.lng));
                }
                break;
        }
        return true; // Event consumed! This avoid default behavior (info window, etc)
    }


    public void startRoute(View v) {
        if (!isLoggedIn) return;
        findViewById(R.id.btnStartRoute).setVisibility(View.GONE);
        findViewById(R.id.btnDirect).setVisibility(View.VISIBLE);
        findViewById(R.id.btnNormal).setVisibility(View.VISIBLE);
        findViewById(R.id.btnCancel).setVisibility(View.VISIBLE);
        Player player = game.getPlayer(mPlayerId);
        routeLocations.add(new LatLng(player.lat, player.lng));
        isBuildingRoute = true;
    }

    public void cancelRoute(View v) {
        findViewById(R.id.btnDirect).setVisibility(View.GONE);
        findViewById(R.id.btnNormal).setVisibility(View.GONE);
        findViewById(R.id.btnCancel).setVisibility(View.GONE);
        findViewById(R.id.btnStartRoute).setVisibility(View.VISIBLE);
        routeLocations.clear();
        if (routePolyline != null)
            routePolyline.remove();
        isBuildingRoute = false;
    }

    public void finishDirectRoute(View v) {
        finishRoute("direct");
    }

    public void finishNormalRoute(View v) {
        finishRoute("normal");
    }

    public boolean checkResultStatusOk(DirectionsResult result) {
        for (int i = 0; i < result.geocodedWaypoints.length; i++) {
            if (result.geocodedWaypoints[i].geocoderStatus != GeocodedWaypointStatus.OK)
                return false;
        }
        return true;
    }

    public void finishRoute(String type) {
        if (!isBuildingRoute) return;
        if (routeLocations.size() < 2) return;

        switch (type) {
            case "direct":
                Player player = game.getPlayer(mPlayerId);
                int energyCost = (routeLocations.size()-1) * ENERGY_PER_DIRECT_LEG;
                if (player.energy - energyCost >= START_ENERGY) {
                    mSocket.emit("move", type, encodeRoute(null));
                } else {
                    Toast.makeText(getApplicationContext(), "Energia insuficiente para DIRECT", Toast.LENGTH_LONG).show();
                    return;
                }
                break;
            case "normal":
                try {
                    List<String> wayPointList = new ArrayList<String>();
                    for (int i = 1; i < routeLocations.size()-1; i++)
                        wayPointList.add(routeLocations.get(i).latitude + "," + routeLocations.get(i).longitude);
                    DirectionsResult result = DirectionsApi.newRequest(getGeoContext())
                            .mode(TravelMode.DRIVING)
                            .origin(new com.google.maps.model.LatLng(routeLocations.get(0).latitude, routeLocations.get(0).longitude))
                            .destination(new com.google.maps.model.LatLng(routeLocations.get(routeLocations.size()-1).latitude, routeLocations.get(routeLocations.size()-1).longitude))
                            //.waypoints("-18.945003995554103,-48.2798021659255|-18.93697,-48.28301")
                            .waypoints(android.text.TextUtils.join("|", wayPointList))
                            .await();
                    if (checkResultStatusOk(result)) {
                        mSocket.emit("move", type, encodeRoute(result));
                    } else {
                        Toast.makeText(getApplicationContext(), "Erro calculando a rota", Toast.LENGTH_LONG).show();
                        return;
                    }

                    //long dist = result.routes[0].legs[0].distance.inMeters;
                    //List<LatLng> decodedPath = PolyUtil.decode(result.routes[0].overviewPolyline.getEncodedPath());
                } catch (Exception e) { Log.e("game", Log.getStackTraceString(e)); }
                break;
        }

        findViewById(R.id.btnDirect).setVisibility(View.GONE);
        findViewById(R.id.btnNormal).setVisibility(View.GONE);
        findViewById(R.id.btnCancel).setVisibility(View.GONE);
        findViewById(R.id.btnStopPlayer).setVisibility(View.VISIBLE);
        if (routePolyline != null)
            routePolyline.remove();
        routeLocations.clear();
        isBuildingRoute = false;
    }

    public void stopPlayer(View v) {
        Player player = game.getPlayer(mPlayerId);
        findViewById(R.id.btnStopPlayer).setVisibility(View.GONE);
        findViewById(R.id.btnStartRoute).setVisibility(View.VISIBLE);
        player.stop(player.lat, player.lng);
        mSocket.emit("stop", player.lat, player.lng);
    }

}