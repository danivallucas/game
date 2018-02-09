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
import android.os.CountDownTimer;
import android.os.Handler;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
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
import com.google.android.gms.maps.model.LatLngBounds;
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

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import io.socket.client.Socket;
import io.socket.emitter.Emitter;

public class MainActivity extends FragmentActivity implements OnMapReadyCallback, GoogleMap.OnMapClickListener, GoogleMap.OnCameraMoveListener, GoogleMap.OnMarkerClickListener {

    public App app;
    protected GoogleMap mMap;
    private LatLng mLatLng;
    private FusedLocationProviderClient mFusedLocationClient;
    private ColorMatrix colorMatrix;
    private ColorMatrixColorFilter colorFilter;
    public DecimalFormat format;
    private SharedPreferences sharedPref; // arq configurações
    protected Metrics metrics;
    private static final int REQUEST_LOGIN = 0;
    protected int mPlayerId;
    private String mPlayerName;
    private int mEmoji;
    private String mPlayerToken;
    private Circle spawnLimitUI;
    public int DIRECT_UNIT_COST;
    public int START_ENERGY;
    public int WAIT_AFTER_LEG;
    public int WAIT_AFTER_BOMB;
    public int BOMB_MAX_DIST;
    public int BOMB_UNIT_COST;
    public int DRIVING_MAX_DIST;
    public int DIRECT_MAX_DIST;
    public int SPAWN_AREA;
    public Socket mSocket;
    private boolean isConnected = false;
    private boolean isLoggedIn = false;
    private String userState = ""; // spawing, buildingRoute, throwingBomb
    private float previousZoomLevel = -1.0f;
    protected TextView ranking;
    protected TextView msg;
    protected TextView alert;
    protected Game game;
    public Handler animHandler; // Animator (movimenta os markers). Iniciado depois do Login com sucesso.
    private Handler zoomHandler; // Verifica a visibilidade ao mudar o zoom
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
        DecimalFormatSymbols symbols = new DecimalFormatSymbols();
        symbols.setGroupingSeparator(' ');
        format = new DecimalFormat("#,###", symbols);
        sharedPref = getPreferences(Context.MODE_PRIVATE);
        mPlayerName = "";
        game = new Game(this);
        metrics = new Metrics(this);
        setUpSocket();
        initMap();
        ranking = (TextView) this.findViewById(R.id.ranking);
        msg = (TextView)findViewById(R.id.msg);
        alert = (TextView)findViewById(R.id.alert);
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
        mSocket.on("onNewBomb", onNewBomb);
        mSocket.on("onRemoveBomb", onRemoveBomb);
        mSocket.on("onNewEnergyBall", onNewEnergyBall);
        mSocket.on("onRemoveEnergyBall", onRemoveEnergyBall);
        mSocket.on("onLogin", onLogin);
        mSocket.on("onLogout", onLogout);
        mSocket.on("onSpawn", onSpawn);
        mSocket.on("onMove", onMove);
        mSocket.on("onLegFinished", onLegFinished);
        mSocket.on("onEnergyChange", onEnergyChange);
        mSocket.on("onFlagCaptured", onFlagCaptured);
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
        mSocket.off("onNewBomb", onNewBomb);
        mSocket.off("onRemoveBomb", onRemoveBomb);
        mSocket.off("onNewEnergyBall", onNewEnergyBall);
        mSocket.off("onRemoveEnergyBall", onRemoveEnergyBall);
        mSocket.off("onLogin", onLogin);
        mSocket.off("onLogout", onLogout);
        mSocket.off("onSpawn", onSpawn);
        mSocket.off("onMove", onMove);
        mSocket.off("onLegFinished", onLegFinished);
        mSocket.off("onEnergyChange", onEnergyChange);
        mSocket.off("onFlagCaptured", onFlagCaptured);
        mSocket.off("onStop", onStop);
        mSocket.off("onPlayerOut", onPlayerOut);
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

    private void enterGame() {
        mSocket.connect();
    }

    private void exitGame() {
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putInt("id", mPlayerId);
        editor.putString("token", mPlayerToken);
        editor.commit();
        if (isLoggedIn) {
            game.clear();
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
                    alert.setVisibility(View.GONE);
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
                    alert.setText("Conectando...");
                    alert.setVisibility(View.VISIBLE);
                    tryConnectAgain(1000);
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
                                String playerStatus = data.getString("playerStatus");
                                JSONObject conf = data.getJSONObject("conf");
                                START_ENERGY = conf.getInt("START_ENERGY");
                                DIRECT_UNIT_COST = conf.getInt("DIRECT_UNIT_COST");
                                WAIT_AFTER_LEG = conf.getInt("WAIT_AFTER_LEG");
                                WAIT_AFTER_BOMB = conf.getInt("WAIT_AFTER_BOMB");
                                BOMB_MAX_DIST = conf.getInt("BOMB_MAX_DIST");
                                BOMB_UNIT_COST =  conf.getInt("BOMB_UNIT_COST");
                                DRIVING_MAX_DIST = conf.getInt("DRIVING_MAX_DIST");
                                DIRECT_MAX_DIST = conf.getInt("DIRECT_MAX_DIST");
                                SPAWN_AREA = conf.getInt("SPAWN_AREA");
                                isLoggedIn = true;
                                if (playerStatus.equals("in")) {
                                    findViewById(R.id.btnStopPlayer).setVisibility(View.GONE);
                                    showBtnThrowBombDelayed(data.getInt("timeToNewBomb"));
                                    showBtnStartRouteDelayed(data.getInt("timeToNewRoute"));
                                }
                                if (playerStatus.equals("out")) {
                                    findViewById(R.id.btnStartRoute).setVisibility(View.GONE);
                                    findViewById(R.id.btnStopPlayer).setVisibility(View.GONE);
                                    findViewById(R.id.btnThrowBomb).setVisibility(View.GONE);
                                    goSpawnState();
                                }
                                // inicia as animações de movimento
                                if (animHandler == null) {
                                    animHandler = new Handler();
                                    animHandler.postDelayed(new Animator(MainActivity.this), 80);
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
                            Log.e("game", "id: " + data.getString("id") + " type: " + data.getString("type") + " points: " + data.getString("points") + " ");
                            Flag flag = game.newFlag(data.getInt("id"), data.getString("type"), data.getDouble("lat"), data.getDouble("lng"), data.getInt("energy"), data.getInt("wall"), data.getInt("playerId"), data.getDouble("points"));
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
                            Player player = game.newPlayer(data.getInt("id"), data.getString("name"), data.getInt("emoji"), data.getBoolean("onLine"), data.getString("status"), data.getDouble("lat"), data.getDouble("lng"), data.getInt("energy"), data.getDouble("flagPoints"));
                            // draw on map
                            if (player.status.equals("in"))
                                player.drawOnMap(player.id == mPlayerId);
                        }
                        checkPlayerListVisibility();
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
                        player.energy = data.getInt("energy");
                        player.setLocation(data.getDouble("lat"), data.getDouble("lng"));
                        player.drawOnMap((id == mPlayerId));
                        //player.drawArea(data.getDouble("lat1"), data.getDouble("lng1"), data.getDouble("lat2"), data.getDouble("lng2"));
                        if (id == mPlayerId) {
                            userState = "";
                            msg.setVisibility(View.GONE);
                            ranking.setVisibility(View.VISIBLE);
                            findViewById(R.id.btnStartRoute).setVisibility(View.VISIBLE);
                            findViewById(R.id.btnThrowBomb).setVisibility(View.VISIBLE);
                        } else {
                            checkPlayerListVisibility();
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
                            Food food = game.newFood(data.getInt("id"), data.getInt("type"), data.getDouble("lat"), data.getDouble("lng"), data.getInt("energy"));
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
                        game.removeFood(data.getInt("id"));
                    } catch (JSONException e) { Log.e("game", Log.getStackTraceString(e)); }
                }
            });
        }
    };

    private Emitter.Listener onNewBomb = new Emitter.Listener() {
        @Override
        public void call(final Object... args) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    try {
                        Log.e("game", "onNewBomb");
                        JSONArray list = (JSONArray) args[0];
                        for (int i = 0; i < list.length(); i++) {
                            JSONObject data = list.getJSONObject(i);
                            Bomb bomb = game.newBomb(data.getInt("id"), data.getInt("type"), data.getDouble("lat"), data.getDouble("lng"), data.getInt("energy"));
                            bomb.drawOnMap();
                        }
                    } catch (JSONException e) { Log.e("game", Log.getStackTraceString(e)); }
                }
            });
        }
    };

    private Emitter.Listener onRemoveBomb = new Emitter.Listener() {
        @Override
        public void call(final Object... args) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    try {
                        JSONObject data = (JSONObject) args[0];
                        game.removeBomb(data.getInt("id"));
                    } catch (JSONException e) { Log.e("game", Log.getStackTraceString(e)); }
                }
            });
        }
    };

    private Emitter.Listener onNewEnergyBall = new Emitter.Listener() {
        @Override
        public void call(final Object... args) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    try {
                        JSONArray list = (JSONArray) args[0];
                        for (int i = 0; i < list.length(); i++) {
                            JSONObject data = list.getJSONObject(i);
                            EnergyBall energyBall = game.newEnergyBall(data.getInt("id"), data.getInt("type"), data.getDouble("lat"), data.getDouble("lng"), data.getInt("energy"));
                            energyBall.drawOnMap();
                        }
                    } catch (JSONException e) { Log.e("game", Log.getStackTraceString(e)); }
                }
            });
        }
    };

    private Emitter.Listener onRemoveEnergyBall = new Emitter.Listener() {
        @Override
        public void call(final Object... args) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    try {
                        JSONObject data = (JSONObject) args[0];
                        game.removeEnergyBall(data.getInt("id"));
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
                            //findViewById(R.id.btnStartRoute).setVisibility(View.VISIBLE);
                            showBtnStartRouteDelayed(data.getInt("timeToNewRoute"));
                            findViewById(R.id.btnThrowBomb).setVisibility(View.VISIBLE);
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
                        player.onEnergyChange(data.getInt("energy"));
                    } catch (JSONException e) { Log.e("game", Log.getStackTraceString(e)); }
                }
            });
        }
    };

    private Emitter.Listener onFlagCaptured = new Emitter.Listener() {
        @Override
        public void call(final Object... args) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    try {
                        JSONObject data = (JSONObject) args[0];
                        int playerId = data.getInt("playerId");
                        Player player = game.getPlayer(playerId);
                        player.onFlagPointsChange(data.getDouble("flagPoints"));
                        int flagId = data.getInt("flagId");
                        Flag flag = game.getFlag(flagId);
                        flag.onFlagCaptured(playerId);
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

    public void drawSpawnLimit() {
        if (spawnLimitUI != null)
            spawnLimitUI.remove();
        spawnLimitUI = mMap.addCircle(new CircleOptions()
                .center(mLatLng)
                .radius(SPAWN_AREA) // In meters
                .fillColor(0x11000000)
                .strokeColor(0xFF333333)
                .strokeWidth(1*metrics.density));
    }

    public void clearSpawnLimit() {
        if (spawnLimitUI != null) {
            spawnLimitUI.remove();
            spawnLimitUI = null;
        }
    }
    private void goSpawnState() {
        findViewById(R.id.btnStartRoute).setVisibility(View.GONE);
        findViewById(R.id.btnThrowBomb).setVisibility(View.GONE);
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(mLatLng, 10f));
        CameraPosition cameraPosition = new CameraPosition.Builder()
                .target(mLatLng)
                .zoom(13)
                .tilt(60)
                .build();
        mMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));

        drawSpawnLimit();
        userState = "spawning";
        msg.setText("Clique na área indicada para iniciar.");
        msg.setVisibility(View.VISIBLE);
        ranking.setVisibility(View.GONE);
    }

    public void finishSpawn(LatLng latLng) {
        double dist = SphericalUtil.computeDistanceBetween(new LatLng(mLatLng.latitude, mLatLng.longitude), latLng);
        if ( dist > SPAWN_AREA ) {
            Toast.makeText(getApplicationContext(), "Erro: Distância maior que a permitida.", Toast.LENGTH_LONG).show();
            return;
        }
        mSocket.emit("spawn", latLng.latitude, latLng.longitude);
        clearSpawnLimit();
        msg.setVisibility(View.GONE);
        ranking.setVisibility(View.VISIBLE);
        userState = "";
    }

    public void checkPlayerListVisibility() {
        int count = 0;
        for (int i = 0; i < game.playerList.size(); i++) {
            Player player = game.playerList.get(i);
            if ( (player.marker == null) ||
                 player.status.equals("out") )
                continue;
            if ( (++count <= 3) || (player.id == mPlayerId) ){
                player.marker.setVisible(true); // este player e os 3 primeiros sempres estarão visíveis (podium!!!)
                continue;
            }
            player.marker.setVisible(isMarkerVisible("player", player.energy));
        }
    }

    public boolean checkVisibility(int zoomMin, int zoomMax, int valueMin, int valueMax, double value) {
        float zoom =  mMap.getCameraPosition().zoom;
        long min = (zoom >= zoomMax) ? valueMin : valueMax - Math.round( (zoom-zoomMin)/(zoomMax-zoomMin) * (valueMax-valueMin) );
        return value >= min;
    }

    public boolean isMarkerVisible(String type, double value) {
        if (type.equals("capital")) {
            return checkVisibility(1, 4, 4600, 115000, value);
        }
        if (type.equals("city")) {
            return checkVisibility(1, 8, 100, 9000, value);
        }
        // Player, Food, Bomb
        return checkVisibility(4, 13, 10, 5000, value);
    }

    @Override
    public void onCameraMove() {
        // Reage ao onCameraMove apenas após ter parado de mexer no zoom
        Runnable onZoomChangeDelayed = new Runnable() {
            public void run() {
                checkPlayerListVisibility();
                for (Flag flag: game.flagList)
                    flag.marker.setVisible(isMarkerVisible(flag.type, flag.points));
                for (Food food: game.foodList)
                    food.marker.setVisible(isMarkerVisible("food", food.energy));
                for (Bomb bomb: game.bombList)
                    bomb.marker.setVisible(isMarkerVisible("bomb", bomb.energy));
            }
        };

        float zoom =  mMap.getCameraPosition().zoom;
        if (zoom == previousZoomLevel) return;
        previousZoomLevel = zoom;
        if (zoomHandler != null) {
            zoomHandler.removeCallbacksAndMessages(null);
        }
        zoomHandler = new Handler();
        zoomHandler.postDelayed(onZoomChangeDelayed, 100);


/*
        // tenta setar a stroke conforme o zoom, mas dá negativo se o mapa está rotacionado...
        Projection projection = mMap.getProjection();
        for (Player player : game.playerList) {
            if (player.energyUI == null) continue;
            LatLng newLatLng = SphericalUtil.computeOffset(player.energyUI.getCenter(), 1200, 270);
            double latDif = player.energyUI.getCenter().latitude - newLatLng.latitude;
            double lngDif = player.energyUI.getCenter().longitude - newLatLng.longitude;
            //status.setText("lat: " + latDif + " lng: " + lngDif);
            Point p1 = projection.toScreenLocation(player.energyUI.getCenter());
            Point p2 = projection.toScreenLocation(newLatLng);
            player.energyUI.setStrokeWidth(p1.y - p2.y);
            }
*/

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
        Player player = game.getPlayer(mPlayerId);
        player.marker.setVisible(false);
        int totalRouteDistance = getTotalRouteDistance();
        int energyToGo = player.energy - (routeLocations.size() * DIRECT_UNIT_COST) - START_ENERGY;
        int maxDist = energyToGo * DIRECT_MAX_DIST;
        Log.e("game", "e: " + player.energy + " tRD: " + totalRouteDistance + " u: " + (routeLocations.size() * DIRECT_UNIT_COST) + " eTG: " + energyToGo + " mD: " + maxDist);
        maxDist -= totalRouteDistance;
        Log.e("game", "mD: " + maxDist);
        if ( maxDist <= 0 ) {
            player.clearDirectLimit();
            findViewById(R.id.btnDirect).setVisibility(View.GONE);
        } else {
            player.drawDirectLimit(latLng, routeLocations.size(), totalRouteDistance);
            findViewById(R.id.btnDirect).setVisibility(View.VISIBLE);
        }
        if (routeLocations.size() < 2) return;
        if (routePolyline != null)
            routePolyline.remove();
        routePolyline = mMap.addPolyline(
                new PolylineOptions()
                        .width(8*metrics.density)
                        .color(0xCC3B7AC9)
                        .jointType(JointType.ROUND)
                        .endCap(new CustomCap(BitmapDescriptorFactory.fromResource(R.drawable.arrow),8*metrics.density))
                        .addAll(routeLocations)
        );
    }

    @Override
    public void onMapClick(LatLng latLng) {
        Log.e("game", "onMapClick");
        switch (userState) {
            case "spawning":
                finishSpawn(latLng);
                break;
            case "buildingRoute":
                addRouteLocation(latLng);
                break;
            case "throwingBomb":
                finishThrowBomb(latLng);
                break;
        }
    }

    @Override
    public boolean onMarkerClick(final Marker marker) {
        Log.e("game", "onMarkerClick");
        String tag = (String) marker.getTag();
        String[] list = tag.split(":");
        String type = list[0];
        int id = Integer.parseInt(list[1]);
        LatLng latLng = null;
        switch (type) {
            case "Player":
                Player player = game.getPlayer(id);
                latLng = new LatLng(player.lat, player.lng);
                break;
            case "Food":
                Food food = game.getFood(id);
                latLng = new LatLng(food.lat, food.lng);
                break;
            case "Bomb":
                Bomb bomb = game.getBomb(id);
                latLng = new LatLng(bomb.lat, bomb.lng);
                break;
            case "Flag":
                Flag flag = game.getFlag(id);
                latLng = new LatLng(flag.lat, flag.lng);
                break;
/*
            case "EnergyBall":
                EnergyBall energyBall = game.getEnergyBall(id);
                latLng = new LatLng(energyBall.lat, energyBall.lng);
                break;
*/
        }
        if (userState.equals("buildingRoute"))
            addRouteLocation(latLng);
        if (userState.equals("throwingBomb"))
            finishThrowBomb(latLng);
        if (userState.equals("spawning"))
            finishSpawn(latLng);
        return true; // Event consumed! This avoid default behavior (info window, etc)
    }


    public void startRoute(View v) {
        if (!isLoggedIn) return;
        findViewById(R.id.btnStartRoute).setVisibility(View.GONE);
        findViewById(R.id.btnThrowBomb).setVisibility(View.GONE);
        findViewById(R.id.btnDirect).setVisibility(View.VISIBLE);
        findViewById(R.id.btnNormal).setVisibility(View.VISIBLE);
        findViewById(R.id.btnCancel).setVisibility(View.VISIBLE);
        Player player = game.getPlayer(mPlayerId);
        addRouteLocation(new LatLng(player.lat, player.lng));
        msg.setText("Monte uma rota.");
        msg.setVisibility(View.VISIBLE);
        ranking.setVisibility(View.GONE);
        userState = "buildingRoute";
    }

    public void cancelRoute(View v) {
        if (v != null) { // acionado pelo botão
            Player player = game.getPlayer(mPlayerId);
            player.marker.setVisible(true);
            player.clearDirectLimit();
        }
        Button btnDirect = (Button) findViewById(R.id.btnDirect);
        btnDirect.setAlpha(1.0f);
        btnDirect.setClickable(true);
        btnDirect.setVisibility(View.GONE);

        findViewById(R.id.btnNormal).setVisibility(View.GONE);
        findViewById(R.id.btnCancel).setVisibility(View.GONE);
        findViewById(R.id.btnStartRoute).setVisibility(View.VISIBLE);
        findViewById(R.id.btnThrowBomb).setVisibility(View.VISIBLE);
        routeLocations.clear();
        if (routePolyline != null)
            routePolyline.remove();
        msg.setVisibility(View.GONE);
        ranking.setVisibility(View.VISIBLE);
        userState = "";
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

    public int getTotalRouteDistance() {
        int totalDistance = 0;
        for (int i = 0; i < routeLocations.size()-1; i++) {
            LatLng latLngFrom = new LatLng(routeLocations.get(i).latitude, routeLocations.get(i).longitude);
            LatLng latLngTo = new LatLng(routeLocations.get(i+1).latitude, routeLocations.get(i+1).longitude);
            totalDistance += SphericalUtil.computeDistanceBetween(latLngFrom, latLngTo);
        }
        return totalDistance;
    }

    public void finishRoute(String type) {
        if (!userState.equals("buildingRoute")) return;
        if (routeLocations.size() < 2) return;
        userState = "";
        msg.setText("Calculando rota...");
        Player player = game.getPlayer(mPlayerId);

        switch (type) {
            case "direct":
                int totalRouteDistance = getTotalRouteDistance();
                int energyToGo = player.energy - ((routeLocations.size()-1) * DIRECT_UNIT_COST) - START_ENERGY;
                int maxDist = energyToGo * DIRECT_MAX_DIST;
                Log.e("game", "e: " + player.energy + " tRD: " + totalRouteDistance + " u: " + (routeLocations.size() * DIRECT_UNIT_COST) + " eTG: " + energyToGo + " mD: " + maxDist);
                if ( totalRouteDistance <= maxDist ) {
                    mSocket.emit("move", type, encodeRoute(null), routeLocations.size()-1, totalRouteDistance);
                    break;
                } else {
                    Toast.makeText(getApplicationContext(), "Erro: Energia insuficiente para rota direta.", Toast.LENGTH_LONG).show();
                    userState = "buildingRoute";
                    return;
                }
            case "normal":
                try {
                    if (routeLocations.size() > 10) {
                        Toast.makeText(getApplicationContext(), "Erro: Muitos pontos de parada.", Toast.LENGTH_LONG).show();
                        userState = "buildingRoute";
                        return;
                    }
                    if (getTotalRouteDistance() > DRIVING_MAX_DIST) {
                        Toast.makeText(getApplicationContext(), "Erro: Rota normal maior que " + (DRIVING_MAX_DIST/1000) + " km.", Toast.LENGTH_LONG).show();
                        userState = "buildingRoute";
                        return;
                    }

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
                        userState = "buildingRoute";
                        return;
                    }

                    //long dist = result.routes[0].legs[0].distance.inMeters;
                    //List<LatLng> decodedPath = PolyUtil.decode(result.routes[0].overviewPolyline.getEncodedPath());
                } catch (Exception e) { Log.e("game", Log.getStackTraceString(e)); }
                break;
        }

        player.marker.setVisible(true);
        player.clearDirectLimit();
        if (routePolyline != null)
            routePolyline.remove();
        routeLocations.clear();
        Button btnDirect = (Button) findViewById(R.id.btnDirect);
        btnDirect.setAlpha(1.0f);
        btnDirect.setClickable(true);
        btnDirect.setVisibility(View.GONE);
        findViewById(R.id.btnNormal).setVisibility(View.GONE);
        findViewById(R.id.btnCancel).setVisibility(View.GONE);
        findViewById(R.id.btnStopPlayer).setVisibility(View.VISIBLE);
        msg.setVisibility(View.GONE);
        ranking.setVisibility(View.VISIBLE);
    }

    public void startThrowBomb(View v) {
        if (!isLoggedIn) return;
        Player player = game.getPlayer(mPlayerId);
        if ( (player.energy - BOMB_UNIT_COST) < START_ENERGY ) {
            Toast.makeText(getApplicationContext(), "Energia insuficiente", Toast.LENGTH_LONG).show();
            return;
        }
        player.drawBombLimit();
        player.marker.setVisible(false);
        findViewById(R.id.btnStartRoute).setVisibility(View.GONE);
        findViewById(R.id.btnThrowBomb).setVisibility(View.GONE);
        findViewById(R.id.btnThrowBombCancel).setVisibility(View.VISIBLE);
        msg.setText("Clique dentro do limite.");
        msg.setVisibility(View.VISIBLE);
        ranking.setVisibility(View.GONE);
        userState = "throwingBomb";
    }

    public void cancelThrowBomb(View v) {
        Player player = game.getPlayer(mPlayerId);
        player.clearBombLimit();
        findViewById(R.id.btnThrowBombCancel).setVisibility(View.GONE);
        findViewById(R.id.btnStartRoute).setVisibility(View.VISIBLE);
        findViewById(R.id.btnThrowBomb).setVisibility(View.VISIBLE);
        msg.setVisibility(View.GONE);
        ranking.setVisibility(View.VISIBLE);
        userState = "";
    }

    public void finishThrowBomb(LatLng latLng) {
        Player player = game.getPlayer(mPlayerId);
        int maxDist = (player.energy - BOMB_UNIT_COST - START_ENERGY) * BOMB_MAX_DIST;
        double dist = SphericalUtil.computeDistanceBetween(new LatLng(player.lat, player.lng), latLng);
        if ( dist <= maxDist ) {
            mSocket.emit("throwBomb", latLng.latitude, latLng.longitude);
        } else {
            Toast.makeText(getApplicationContext(), "Erro: Distância maior que a permitida.", Toast.LENGTH_LONG).show();
        }
        player.clearBombLimit();
        player.marker.setVisible(true);
        msg.setVisibility(View.GONE);
        findViewById(R.id.btnThrowBombCancel).setVisibility(View.GONE);
        findViewById(R.id.btnStartRoute).setVisibility(View.VISIBLE);
        findViewById(R.id.btnThrowBomb).setVisibility(View.VISIBLE);
        msg.setVisibility(View.GONE);
        ranking.setVisibility(View.VISIBLE);
        userState = "";
    }

    public void stopPlayer(View v) {
        Player player = game.getPlayer(mPlayerId);
        findViewById(R.id.btnStopPlayer).setVisibility(View.GONE);
        //findViewById(R.id.btnStartRoute).setVisibility(View.VISIBLE);
        showBtnStartRouteDelayed(WAIT_AFTER_LEG);
        findViewById(R.id.btnThrowBomb).setVisibility(View.VISIBLE);
        player.stop(player.lat, player.lng);
        mSocket.emit("stop", player.lat, player.lng);
        userState = "";
    }

    public void showBtnStartRouteDelayed(int mili) {
        final Button btnStartRoute = (Button) findViewById(R.id.btnStartRoute);
        btnStartRoute.setAlpha(0.5f);
        btnStartRoute.setClickable(false);
        btnStartRoute.setVisibility(View.VISIBLE);
        new CountDownTimer(mili, 1000) {
            public void onTick(long millisUntilFinished) {
                btnStartRoute.setText(millisUntilFinished / 1000 + "s");
            }
            public void onFinish() {
                btnStartRoute.setText("+");
                btnStartRoute.setAlpha(1.0f);
                btnStartRoute.setClickable(true);
            }
        }.start();
    }

    public void showBtnThrowBombDelayed(int mili) {
        final Button btnThrowBomb = (Button) findViewById(R.id.btnThrowBomb);
        btnThrowBomb.setAlpha(0.5f);
        btnThrowBomb.setClickable(false);
        btnThrowBomb.setVisibility(View.VISIBLE);
        new CountDownTimer(mili, 1000) {
            public void onTick(long millisUntilFinished) {
                btnThrowBomb.setText(millisUntilFinished / 1000 + "s");
            }
            public void onFinish() {
                btnThrowBomb.setText("Bomb");
                btnThrowBomb.setAlpha(1f);
                btnThrowBomb.setClickable(true);
            }
        }.start();
    }

    public void tryConnectAgain(int mili) {
        new CountDownTimer(mili, 0) {
            public void onTick(long millisUntilFinished) {}
            public void onFinish() {
                enterGame();
            }
        }.start();
    }

}