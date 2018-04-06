package com.danival.game;

import android.app.Activity;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
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
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.crashlytics.android.Crashlytics;
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

    public int START_ENERGY;
    public int WAIT_AFTER_LEG;
    public int WAIT_AFTER_BOMB;
    public int BOMB_DEFAULT_ENERGY;
    public int BOMB_MAX_DIST;
    public int BOMB_UNIT_COST;
    public int TURBO_MAX_DIST;
    public int SPAWN_AREA;
    public App app;
    public Socket mSocket;
    protected GoogleMap mMap;
    private FusedLocationProviderClient mFusedLocationClient;
    public String FCMToken = "";
    private ColorMatrix colorMatrix;
    private ColorMatrixColorFilter colorFilter;
    public DecimalFormat format;
    private SharedPreferences sharedPref; // arq configurações
    protected Metrics metrics;
    private static final int REQUEST_LOGIN = 3024;
    protected int mPlayerId;
    private String mPlayerName;
    private int mEmoji;
    private String mPlayerToken;
    private LatLng mPosition;
    private Circle spawnLimitUI;
    private boolean isConnected = false;
    private boolean isLoggedIn = false;
    public int uiState = 0; // 0=conectando, 1=spawing, 2=main, 3=Go, 4=turbo, 5=Bomb, 6=Moving
    private float previousZoomLevel = -1.0f;
    protected TextView ranking;
    protected TextView msg;
    protected TextView alert;
    protected Game game;
    public Handler animHandler; // Animator (movimenta os markers). Iniciado depois do Login com sucesso.
    private Handler zoomHandler; // Verifica a visibilidade ao mudar o zoom
    private List<LatLng> routePositions;
    private Polyline routePolyline;
    private ImageView splash_bg;
    private ImageView splash_icon;
    private Button btnConnect;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.e("game", "onCreate");
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
        //mSocket = app.getSocket();
        initMap();
        splash_bg = (ImageView) this.findViewById(R.id.splash_bg);
        splash_icon = (ImageView) this.findViewById(R.id.splash_icon);
        splash_icon.setX(metrics.w/2 - 96*metrics.density/2);
        splash_icon.setY(metrics.h/2 - 96*metrics.density/2);
        ranking = (TextView) this.findViewById(R.id.ranking);
        msg = (TextView)findViewById(R.id.msg);
        alert = (TextView)findViewById(R.id.alert);
        alert.setY(metrics.h/2 + 96*metrics.density/2);
        btnConnect = (Button)findViewById(R.id.btnConnect);
        btnConnect.setY(metrics.h/2 + 96*metrics.density/2);
        routePositions = new ArrayList<LatLng>();
        // FCM
        //Crashlytics.log("Testando Firebase Crashlytics...");
        FCMBroadCastReceiver fcmBroadCastReceiver = new FCMBroadCastReceiver(this);
        LocalBroadcastManager.getInstance(this).registerReceiver(fcmBroadCastReceiver, new IntentFilter("com.danival.game.FCM_BROADCAST"));
        FCMToken = sharedPref.getString("FCMToken", "");
    }

    private void registerSocketEvents() {
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
        mSocket.on("onFlagReleased", onFlagReleased);
        mSocket.on("onStop", onStop);
        mSocket.on("onPlayerOut", onPlayerOut);
        mSocket.on("onReadyToPlay", onReadyToPlay);
    }

    private void unregisterSocketEvents() {
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
        mSocket.off("onFlagReleased", onFlagReleased);
        mSocket.off("onStop", onStop);
        mSocket.off("onPlayerOut", onPlayerOut);
        mSocket.off("onReadyToPlay", onReadyToPlay);
    }

        @Override
    public void onDestroy() {
        Log.e("game", "onDestroy");
        super.onDestroy();
    }

    @Override
    public void onRestart() {
        Log.e("game", "onRestart");
        super.onRestart();
        if (!isConnected) {
            enterGame();
        }
    }

    @Override
    public void onResume() {
        Log.e("game", "onResume");
        super.onResume();
        app.setTag(0);
    }

    @Override
    public void onStop() {
        Log.e("game", "onStop");
        super.onStop();
        if ( isConnected && (app.getTag() == 0) ) {
            mSocket.disconnect();
            game.clear();
        }
    }

    private void startSignIn() {
        Log.e("game", "startSignIn");
        app.setTag(1);
        mPlayerName = "";
        Intent intent = new Intent(this, LoginActivity.class);
        startActivityForResult(intent, REQUEST_LOGIN);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.e("game", "onActivityResult - requestCode: " + requestCode);
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
        Log.e("game", "afterConnect");
        mPlayerId = sharedPref.getInt("id", 0);
        mPlayerToken = sharedPref.getString("token", "");
        if (mPlayerToken.equals("")) {
            startSignIn();
        }
        else {
            mSocket.emit("login", mPlayerId, mPlayerToken);
        }
    }

    public void tryConnectAgain(int mili) {
        Log.e("game", "tryConnectAgain");
        new CountDownTimer(mili, 1000) {
            public void onTick(long millisUntilFinished) {
                alert.setText("Erro: Nova tentativa em " + millisUntilFinished / 1000 + "s...");
            }
            public void onFinish() {
                alert.setText("Conectando...");
                enterGame();
            }
        }.start();
    }

    private void enterGame() {
        Log.e("game", "enterGame");
        NotificationManager nMgr = (NotificationManager) this.getSystemService(Context.NOTIFICATION_SERVICE);
        nMgr.cancelAll();
        splash_bg.setVisibility(View.VISIBLE);
        splash_icon.setVisibility(View.VISIBLE);
        alert.setText("Conectando...");
        alert.setVisibility(View.VISIBLE);
        btnConnect.setVisibility(View.GONE);
        ranking.setVisibility(View.GONE);
        msg.setVisibility(View.GONE);
        app.initSocket();
        mSocket = app.getSocket();
        registerSocketEvents();
        changeUIState(0);
        mSocket.connect();
    }

    private void exitGame() {
        Log.e("game", "exitGame");
        unregisterSocketEvents();
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putInt("id", mPlayerId);
        editor.putString("token", mPlayerToken);
        editor.commit();
        changeUIState(0);
        if (isLoggedIn) {
            game.clear();
        }
        isLoggedIn = false;
        // Se desconectou por problemas na rede e não por perda de foco do app, mostra o botão de conectar
        // Se está saindo do app (perda de foco) esse botão não será usado e, na volta, ele estará invisível
        alert.setVisibility(View.GONE);
        btnConnect.setVisibility(View.VISIBLE);
    }

    private Emitter.Listener onConnect = new Emitter.Listener() {
        @Override
        public void call(Object... args) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Log.e("game", "onConnect");
                    if (isConnected) return;
                    isConnected = true;
                    //alert.setVisibility(View.GONE);
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
                    Log.e("game", "onDisconnect");
                    if (!isConnected) return;
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
                    Log.e("game", "onConnectError");
                    alert.setText("Erro: Nova tentativa em 3s...");
                    tryConnectAgain(3000);
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
                                JSONObject conf = data.getJSONObject("conf");
                                START_ENERGY = conf.getInt("START_ENERGY");
                                WAIT_AFTER_LEG = conf.getInt("WAIT_AFTER_LEG");
                                WAIT_AFTER_BOMB = conf.getInt("WAIT_AFTER_BOMB");
                                BOMB_DEFAULT_ENERGY = conf.getInt("BOMB_DEFAULT_ENERGY");
                                BOMB_MAX_DIST = conf.getInt("BOMB_MAX_DIST");
                                BOMB_UNIT_COST =  conf.getInt("BOMB_UNIT_COST");
                                TURBO_MAX_DIST = conf.getInt("TURBO_MAX_DIST");
                                SPAWN_AREA = conf.getInt("SPAWN_AREA");
                                if (!FCMToken.equals(""))
                                    mSocket.emit("refreshtoken", FCMToken);
                                isLoggedIn = true;
                            } else {
                                mPlayerId = 0;
                                mPlayerToken = "";
                                startSignIn();
                            }
                        } else {
                            if (!isLoggedIn) return;
                            Player player = game.getPlayer(id);
                            if (player == null) return;
                            player.onLine = true;
                            if (!player.status.equals("out"))
                                player.refreshIcon();
                        }
                    } catch (JSONException e) { Log.e("game", Log.getStackTraceString(e)); }
                }
            });
        }
    };

    private Emitter.Listener onReadyToPlay = new Emitter.Listener() {
        @Override
        public void call(final Object... args) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    try {
                        JSONObject data = (JSONObject) args[0];
                        Player player = game.getPlayer(mPlayerId);
                        if (player == null) return;
                        if (player.status.equals("out")) {
                            goSpawnState();
                        } else {
                            setBtnGoDelayed(data.getInt("timeToNewRoute"));
                            setBtnBombDelayed(data.getInt("timeToNewBomb"));
                            if (player.status.equals("in"))
                                changeUIState(2);
                            if (player.status.equals("moving"))
                                changeUIState(6);
                        }
                        // inicia as animações de movimento
                        if (animHandler == null) {
                            animHandler = new Handler();
                            animHandler.postDelayed(new Animator(MainActivity.this), 80);
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
                        if (!isLoggedIn) return;
                        Log.e("game", "onNewFlag.....");
                        JSONArray list = (JSONArray) args[0];
                        for (int i = 0; i < list.length(); i++) {
                            JSONObject data = list.getJSONObject(i);
                            //Log.e("game", "id: " + data.getString("id") + " type: " + data.getString("type") + " points: " + data.getString("points") + " ");
                            Flag flag = game.newFlag(data.getInt("id"), data.getString("type"), new LatLng(data.getDouble("lat"), data.getDouble("lng")), data.getInt("energy"), data.getInt("wall"), data.getInt("playerId"), data.getDouble("points"));
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
                        if (!isLoggedIn) return;
                        JSONArray list = (JSONArray) args[0];
                        for (int i = 0; i < list.length(); i++) {
                            JSONObject data = list.getJSONObject(i);
                            Player player = game.newPlayer(data.getInt("id"), data.getString("name"), data.getInt("emoji"), data.getBoolean("onLine"), data.getString("status"), new LatLng(data.getDouble("lat"), data.getDouble("lng")), data.getInt("energy"), data.getDouble("flagPoints"), data.getInt("energyToRestore"));
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
                        if (!isLoggedIn) return;
                        Player player = game.getPlayer(data.getInt("id"));
                        if (player == null) return;
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
                        if (!isLoggedIn) return;
                        JSONObject data = (JSONObject) args[0];
                        int id = data.getInt("id");
                        Player player = game.getPlayer(id);
                        if (player == null) return;
                        player.status = data.getString("status");
                        player.energy = data.getInt("energy");
                        player.energyToRestore = data.getInt("energyToRestore");
                        player.setPosition(new LatLng(data.getDouble("lat"), data.getDouble("lng")));
                        player.drawOnMap((id == mPlayerId));
                        if (id == mPlayerId) {
                            changeUIState(2);
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
                        if (!isLoggedIn) return;
                        JSONArray list = (JSONArray) args[0];
                        for (int i = 0; i < list.length(); i++) {
                            JSONObject data = list.getJSONObject(i);
                            Food food = game.newFood(data.getInt("id"), data.getInt("type"), new LatLng(data.getDouble("lat"), data.getDouble("lng")), data.getInt("energy"));
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
                        if (!isLoggedIn) return;
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
                        if (!isLoggedIn) return;
                        JSONArray list = (JSONArray) args[0];
                        for (int i = 0; i < list.length(); i++) {
                            JSONObject data = list.getJSONObject(i);
                            Bomb bomb = game.newBomb(data.getInt("id"), data.getInt("type"), data.getInt("player"), new LatLng(data.getDouble("lat"), data.getDouble("lng")), data.getInt("energy"));
                            if (data.getInt("player") == mPlayerId) {
                                bomb.animate();
                            } else {
                                bomb.drawOnMap();
                            }
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
                        if (!isLoggedIn) return;
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
                        if (!isLoggedIn) return;
                        JSONArray list = (JSONArray) args[0];
                        for (int i = 0; i < list.length(); i++) {
                            JSONObject data = list.getJSONObject(i);
                            EnergyBall energyBall = game.newEnergyBall(data.getInt("id"), data.getInt("type"), new LatLng(data.getDouble("lat"), data.getDouble("lng")), data.getInt("energy"));
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
                        if (!isLoggedIn) return;
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
                        if (!isLoggedIn) return;
                        JSONObject data = (JSONObject) args[0];
                        int id = data.getInt("id");
                        JSONArray legList = data.getJSONArray("legList");
                        Player player = game.getPlayer(id);
                        if (player == null) return;
/*
                        player.energy = data.getInt("energy");
                        player.energyToRestore = data.getInt("energyToRestore");
                        player.drawEnergy();
*/
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
                        if (!isLoggedIn) return;
                        JSONObject data = (JSONObject) args[0];
                        int id = data.getInt("id");
                        Player player = game.getPlayer(id);
                        if (player == null) return;
                        if ( (player.id == mPlayerId) && (player.legList.size() == 1) ) {
                            // se é a última leg, sai do uiState=6 (moving)
                            setBtnGoDelayed(data.getInt("timeToNewRoute"));
                            changeUIState(2);
                            //setBtnTurboDelayed(data.getInt("timeToNewRoute"));
                        }
                        player.onLegFinished(data.getString("status"), new LatLng(data.getDouble("lat"), data.getDouble("lng")));
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
                        if (!isLoggedIn) return;
                        JSONObject data = (JSONObject) args[0];
                        int id = data.getInt("id");
                        Log.e("game", "id: " + id + " energy: " + data.getInt("energy") + " energyToRestore: " + data.getInt("energyToRestore"));
                        Player player = game.getPlayer(id);
                        if (player == null) return;
                        player.onEnergyChange(data.getInt("energy"), data.getInt("energyToRestore"));
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
                        if (!isLoggedIn) return;
                        JSONObject data = (JSONObject) args[0];
                        // Player que capturou a flag
                        int playerId = data.getInt("playerId");
                        Player player = game.getPlayer(playerId);
                        if (player == null) return;
                        player.onFlagPointsChange(data.getDouble("flagPoints"));
                        // Player que perdeu a flag
                        int oldPlayerId = data.getInt("oldPlayerId");
                        if (oldPlayerId != -1) {
                            Player oldPlayer = game.getPlayer(oldPlayerId);
                            if (oldPlayer != null)
                                oldPlayer.onFlagPointsChange(data.getDouble("oldPlayerFlagPoints"));
                        }
                        // Redesenha a flag
                        int flagId = data.getInt("flagId");
                        Flag flag = game.getFlag(flagId);
                        if (flag == null) return;
                        flag.onFlagCaptured(playerId);
                    } catch (JSONException e) { Log.e("game", Log.getStackTraceString(e)); }
                }
            });
        }
    };

    private Emitter.Listener onFlagReleased = new Emitter.Listener() {
        @Override
        public void call(final Object... args) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    try {
                        if (!isLoggedIn) return;
                        JSONObject data = (JSONObject) args[0];
                        // Redesenha a flag
                        int flagId = data.getInt("flagId");
                        Flag flag = game.getFlag(flagId);
                        if (flag == null) return;
                        flag.onFlagReleased();
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
                        if (!isLoggedIn) return;
                        JSONObject data = (JSONObject) args[0];
                        int id = data.getInt("id");
                        //Toast.makeText(getApplicationContext(), "onStop: " + id, Toast.LENGTH_LONG).show();
                        Player player = game.getPlayer(id);
                        if (player == null) return;
                        player.stop(new LatLng(data.getDouble("lat"), data.getDouble("lng")));
                        if (id == mPlayerId)
                            setBtnGoDelayed(data.getInt("timeToNewRoute"));

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
                        if (!isLoggedIn) return;
                        JSONObject data = (JSONObject) args[0];
                        int id = data.getInt("id");
                        Player player = game.getPlayer(id);
                        if (player == null) return;
                        player.status = data.getString("status");
                        player.removeFromMap();
                        if (id == mPlayerId)
                            goSpawnState();

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

    private void addRoutePosition(LatLng latLng) {
        if (uiState == 4) { // Turbo
            if (routePositions.size() == 2) { // se já tem 2 pontos não pode adicionar mais um
                Toast.makeText(getApplicationContext(), "Escolha apenas um destino.", Toast.LENGTH_LONG).show();
                return;
            }
            Player player = game.getPlayer(mPlayerId);
            if (player == null) return;
            int maxDist = player.energy * TURBO_MAX_DIST;
            double dist = SphericalUtil.computeDistanceBetween(player.position, latLng);
            if ( dist > maxDist ) {
                Toast.makeText(getApplicationContext(), "Erro: Distância maior que a permitida.", Toast.LENGTH_LONG).show();
                return;
            }
        }
        if (routePositions.size() > 20) {
            Toast.makeText(getApplicationContext(), "Escolha no máximo 20 destinos.", Toast.LENGTH_LONG).show();
            return;
        }
        routePositions.add(latLng);
        if (routePositions.size() < 2) return; // só traça a polylinha do 2o ponto em diante
        if (routePolyline != null)
            routePolyline.remove();
        int lineW = Math.round(7*metrics.density);
        int pointW = Math.round(16*metrics.density);
        routePolyline = mMap.addPolyline(
                new PolylineOptions()
                        .width(lineW)
                        .color(0xCC3B7AC9)
                        .jointType(JointType.ROUND)
                        .endCap(new CustomCap(BitmapDescriptorFactory.fromResource(R.drawable.point),pointW))
                        .addAll(routePositions)
        );
    }

    private String encodeRoute() {
        JSONArray legArray = new JSONArray();
        try {
            for (int i = 0; i < routePositions.size()-1; i++) {
                JSONObject start = new JSONObject();
                start.put("lat", routePositions.get(i).latitude);
                start.put("lng", routePositions.get(i).longitude);
                JSONObject end = new JSONObject();
                end.put("lat", routePositions.get(i+1).latitude);
                end.put("lng", routePositions.get(i+1).longitude);
                JSONObject legObject = new JSONObject();
                legObject.put("start", start);
                legObject.put("end", end);
                legArray.put(legObject);
            }

        } catch (Exception e) {
            Log.e("game", Log.getStackTraceString(e));
        }
        return legArray.toString();
    }

    private void goSpawnState() {
        CameraPosition cameraPosition = new CameraPosition.Builder()
                .target(mPosition)
                .zoom(13)
                .tilt(0)
                .build();
        mMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
        if (spawnLimitUI != null)
            spawnLimitUI.remove();
        spawnLimitUI = mMap.addCircle(new CircleOptions()
                .center(mPosition)
                .radius(SPAWN_AREA) // In meters
                .fillColor(0x11000000)
                .strokeColor(0xFF333333)
                .strokeWidth(1*metrics.density));
        changeUIState(1);
    }

    public void onBtnGoClick(View v) {
        if (!isLoggedIn) return;
        Player player = game.getPlayer(mPlayerId);
        if (player == null) return;
        player.showOriginMarker();
        changeUIState(3);
        addRoutePosition(player.position);
    }

    public void onBtnTurboClick(View v) {
        if (!isLoggedIn) return;
        Player player = game.getPlayer(mPlayerId);
        if (player == null) return;
        player.showOriginMarker();
        player.drawTurboLimit();
        changeUIState(4);
        addRoutePosition(player.position);
    }

    public void onBtnBombClick(View v) {
        if (!isLoggedIn) return;
        Player player = game.getPlayer(mPlayerId);
        if (player == null) return;
        if ( (player.energy - BOMB_UNIT_COST) < START_ENERGY ) {
            Toast.makeText(getApplicationContext(), "Energia insuficiente", Toast.LENGTH_LONG).show();
            return;
        }
        player.drawBombLimit();
        player.showOriginMarker();
        changeUIState(5);
    }

    public void onBtnOkClick(View v) {
        if (!isLoggedIn) return;
        switch (uiState) {
            case 3: // go
                finishGo(v);
                break;
            case 4: // turbo
                finishTurbo();
                break;
        }
    }

    public void onBtnCancelClick(View v) {
        if (!isLoggedIn) return;
        switch (uiState) {
            case 3: // go
                cancelGoAndTurbo(v);
                break;
            case 4: // turbo
                cancelGoAndTurbo(v);
                break;
            case 5: // bomb
                cancelBomb(v);
                break;
        }
    }

    public void onBtnStopClick(View v) {
        if (!isLoggedIn) return;
        Player player = game.getPlayer(mPlayerId);
        if (player == null) return;
        player.stop(player.position);
        mSocket.emit("stop", player.position.latitude, player.position.longitude);
        changeUIState(2);
    }

    public void onBtnConnectClick(View v) {
        btnConnect.setVisibility(View.GONE);
        enterGame();
    }

    public void focus(LatLng latLng, String type, int id) {
        double START_ZOOM;
        double zoom = 15; // ruas
        if (type.equals("Player")) {
            Player player = game.getPlayer(id);
            if (player == null) return;
            START_ZOOM = 17;
            zoom = START_ZOOM - Math.log((float)player.energy/START_ENERGY)/Math.log(2);
        }
        if (type.equals("Food")) {
            zoom = 17;
        }
        if (type.equals("Bomb")) {
            zoom = 17;
        }
        if (type.equals("Flag")) {
            zoom = 12;
        }
        CameraPosition cameraPosition = new CameraPosition.Builder()
                .target(latLng)
                .zoom((float)zoom)
                .tilt(0)
                .build();
        mMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
    }

    public void finishSpawn(LatLng latLng) {
        double dist = SphericalUtil.computeDistanceBetween(mPosition, latLng);
        if ( dist > SPAWN_AREA ) {
            Toast.makeText(getApplicationContext(), "Erro: Distância maior que a permitida.", Toast.LENGTH_LONG).show();
            return;
        }
        mSocket.emit("spawn", latLng.latitude, latLng.longitude);
        if (spawnLimitUI != null) {
            spawnLimitUI.remove();
            spawnLimitUI = null;
        }
        changeUIState(2);
    }

    public void finishGo(View v) {
        if (routePositions.size() < 2) {
            Toast.makeText(getApplicationContext(), "Escolha pelo menos um local.", Toast.LENGTH_LONG).show();
            return;
        }
        Player player = game.getPlayer(mPlayerId);
        if (player == null) return;
        player.refreshIcon();
        mSocket.emit("move", "normal", encodeRoute());
        if (routePolyline != null)
            routePolyline.remove();
        routePositions.clear();
        changeUIState(6);
    }

    public void finishTurbo() {
        if (routePositions.size() < 2) {
            Toast.makeText(getApplicationContext(), "Escolha um local.", Toast.LENGTH_LONG).show();
            return;
        }
        Player player = game.getPlayer(mPlayerId);
        if (player == null) return;
        player.refreshIcon();
        player.clearTurboLimit();
        mSocket.emit("move", "turbo", encodeRoute());
        if (routePolyline != null)
            routePolyline.remove();
        routePositions.clear();
        changeUIState(6);
    }

    public void finishBomb(LatLng latLng) {
        Player player = game.getPlayer(mPlayerId);
        if (player == null) return;
        int maxDist = player.energy * BOMB_MAX_DIST;
        double dist = SphericalUtil.computeDistanceBetween(player.position, latLng);
        if ( dist > maxDist ) {
            Toast.makeText(getApplicationContext(), "Erro: Distância maior que a permitida.", Toast.LENGTH_LONG).show();
            return;
        }
        mSocket.emit("throwBomb", latLng.latitude, latLng.longitude);
        player.clearBombLimit();
        player.refreshIcon();
        setBtnBombDelayed(WAIT_AFTER_BOMB);
        changeUIState(2);
    }

    public void cancelGoAndTurbo(View v) {
        Player player = game.getPlayer(mPlayerId);
        if (player == null) return;
        player.clearTurboLimit();
        player.refreshIcon();
        routePositions.clear();
        if (routePolyline != null)
            routePolyline.remove();
        changeUIState(2);
    }

    public void cancelBomb(View v) {
        Player player = game.getPlayer(mPlayerId);
        if (player == null) return;
        player.refreshIcon();
        player.clearBombLimit();
        changeUIState(2);
    }

    public void setButtons() {
        if (!isLoggedIn) return;
        Player player = game.getPlayer(mPlayerId);
        if (player == null) return;

        Button btnGo = (Button) findViewById(R.id.btnGo);
        Button btnTurbo = (Button) findViewById(R.id.btnTurbo);
        Button btnBomb = (Button) findViewById(R.id.btnBomb);
        Button btnOk = (Button) findViewById(R.id.btnOk);
        Button btnCancel = (Button) findViewById(R.id.btnCancel);
        Button btnStop = (Button) findViewById(R.id.btnStop);
        ProgressBar progressGo = (ProgressBar) findViewById(R.id.progressGo);
        ProgressBar progressBomb = (ProgressBar) findViewById(R.id.progressBomb);

        btnGo.setVisibility(uiState == 2 ? View.VISIBLE : View.GONE);
        progressGo.setVisibility( (btnGo.getVisibility() == View.VISIBLE) && !btnGo.isClickable() ? View.VISIBLE : View.GONE);
        btnTurbo.setVisibility( (uiState == 2) && btnGo.isClickable() && ( player.energy >= START_ENERGY * 2 ) ? View.VISIBLE : View.GONE);
        btnBomb.setVisibility( (uiState == 2) && ( (player.energy - BOMB_UNIT_COST) >= START_ENERGY ) ? View.VISIBLE : View.GONE);
        progressBomb.setVisibility( (btnBomb.getVisibility() == View.VISIBLE) && !btnBomb.isClickable() ? View.VISIBLE : View.GONE);
        btnOk.setVisibility( (uiState == 3) || (uiState == 4) ? View.VISIBLE : View.GONE);
        btnCancel.setVisibility( (uiState == 3) || (uiState == 4) || (uiState == 5) ? View.VISIBLE : View.GONE);
        btnStop.setVisibility(uiState == 6 ? View.VISIBLE : View.GONE);
    }

    public void changeUIState(int state) {
        if (!isLoggedIn) return;
        uiState = state;
        msg.setVisibility(View.GONE);
        alert.setVisibility(View.GONE);
        ranking.setVisibility(View.GONE);
        splash_bg.setVisibility(View.GONE);
        splash_icon.setVisibility(View.GONE);

        switch (state) {
            case 0: // connect
                splash_bg.setVisibility(View.VISIBLE);
                splash_icon.setVisibility(View.VISIBLE);
                alert.setText("Conectando...");
                alert.setVisibility(View.VISIBLE);
                break;
            case 1: // spawn
                msg.setText("Clique no mapa para iniciar.");
                msg.setVisibility(View.VISIBLE);
/*
                if (!ranking.getText().equals(""))
                    ranking.setVisibility(View.VISIBLE);
*/
                break;
            case 2: // main
                if (!ranking.getText().equals(""))
                    ranking.setVisibility(View.VISIBLE);
                break;
            case 3: // go
                msg.setText("Monte uma rota.");
                msg.setVisibility(View.VISIBLE);
                break;
            case 4: // turbo
                msg.setText("Clique dentro do limite.");
                msg.setVisibility(View.VISIBLE);
                break;
            case 5: // bomb
                msg.setText("Clique dentro do limite.");
                msg.setVisibility(View.VISIBLE);
                break;
            case 6: // moving
                if (!ranking.getText().equals(""))
                    ranking.setVisibility(View.VISIBLE);
                break;
        }
        if (state >= 2)
            setButtons();
    }

    public void setBtnGoDelayed(final int mili) {
        final Button btnGo = (Button) findViewById(R.id.btnGo);
        btnGo.setBackground(getResources().getDrawable(R.drawable.btn_go_disabled));
        btnGo.setClickable(false);
        final ProgressBar progressGo = (ProgressBar) findViewById(R.id.progressGo);
        setButtons();
        new CountDownTimer(mili, 80) {
            public void onTick(long millisUntilFinished) {
                progressGo.setProgress(Math.round((1 - millisUntilFinished / (float)mili) * 100));
            }
            public void onFinish() {
                btnGo.setBackground(getResources().getDrawable(R.drawable.btn_go));
                btnGo.setClickable(true);
                setButtons();
            }
        }.start();
    }

/*
    public void setBtnTurboDelayed(final int mili) {
        final Button btnTurbo = (Button) findViewById(R.id.btnTurbo);
        btnTurbo.setBackground(getResources().getDrawable(R.drawable.btn_turbo_disabled));
        btnTurbo.setClickable(false);
        final ProgressBar progressTurbo = (ProgressBar) findViewById(R.id.progressTurbo);
        progressTurbo.setVisibility(View.VISIBLE);
        new CountDownTimer(mili, 80) {
            public void onTick(long millisUntilFinished) {
                progressTurbo.setProgress(Math.round((1 - millisUntilFinished / (float)mili) * 100));
            }
            public void onFinish() {
                btnTurbo.setBackground(getResources().getDrawable(R.drawable.btn_turbo));
                btnTurbo.setClickable(true);
                progressTurbo.setVisibility(View.GONE);
            }
        }.start();
    }
*/

    public void setBtnBombDelayed(final int mili) {
        final Button btnBomb = (Button) findViewById(R.id.btnBomb);
        btnBomb.setBackground(getResources().getDrawable(R.drawable.btn_bomb_disabled));
        btnBomb.setClickable(false);
        final ProgressBar progressBomb = (ProgressBar) findViewById(R.id.progressBomb);
        setButtons();
        new CountDownTimer(mili, 80) {
            public void onTick(long millisUntilFinished) {
                progressBomb.setProgress(Math.round((1 - millisUntilFinished / (float)mili) * 100));
            }
            public void onFinish() {
                btnBomb.setBackground(getResources().getDrawable(R.drawable.btn_bomb));
                btnBomb.setClickable(true);
                setButtons();
            }
        }.start();
    }

    private void initMap() {
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(MainActivity.this);
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        Log.e("game", "onMapReady");
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
                            Log.e("game", "mFusedLocationClient.onSuccess");
                            mPosition = new LatLng(location.getLatitude(), location.getLongitude());
                            enterGame();
                        }
                    }
                });
    }

    @Override
    public void onMapClick(LatLng latLng) {
        Log.e("game", "onMapClick");
        switch (uiState) {
            case 1:
                finishSpawn(latLng);
                break;
            case 3:
                addRoutePosition(latLng);
                break;
            case 4:
                addRoutePosition(latLng);
                break;
            case 5:
                finishBomb(latLng);
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
                if (player == null) return true;
                latLng = player.position;
                break;
            case "Food":
                Food food = game.getFood(id);
                if (food == null) return true;
                latLng = food.position;
                break;
            case "Bomb":
                Bomb bomb = game.getBomb(id);
                if (bomb == null) return true;
                latLng = bomb.position;
                break;
            case "Flag":
                Flag flag = game.getFlag(id);
                if (flag == null) return true;
                latLng = flag.position;
                break;
        }
        if (uiState == 1)
            finishSpawn(latLng);
        if (uiState == 2)
            focus(latLng, type, id);
        if (uiState == 3)
            addRoutePosition(latLng);
        if (uiState == 4)
            addRoutePosition(latLng);
        if (uiState == 5)
            finishBomb(latLng);
        return true; // Event consumed! This avoid default behavior (info window, etc)
    }

    @Override
    public void onCameraMove() {
        // Reage ao onCameraMove apenas após ter parado de mexer no zoom
        Runnable onZoomChangeDelayed = new Runnable() {
            public void run() {
                checkPlayerListVisibility();
                for (Flag flag: game.flagList) {
                    if (flag.marker == null) continue;
                    flag.marker.setVisible(isMarkerVisible(flag.type, flag.points));
                }
                for (Food food: game.foodList) {
                    if (food.marker == null) continue;
                    food.marker.setVisible(isMarkerVisible("food", food.energy));
                }

                for (Bomb bomb: game.bombList){
                    if (bomb.marker == null) continue;
                    bomb.marker.setVisible(isMarkerVisible("bomb", bomb.energy));
                }
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
    }

    public void checkPlayerListVisibility() {
        int count = 0;
        for (int i = 0; i < game.playerList.size(); i++) {
            Player player = game.playerList.get(i);
            if (player == null) continue;
            if (    (player.marker == null) ||
                    (player.status.equals("out")) ||
                    (player.id == mPlayerId)    )
                continue;
            if (++count <= 3) {
                if (player.marker != null)
                    player.marker.setVisible(true); // os 3 primeiros sempres estarão visíveis (podium!!!)
                continue;
            }
            if (player.marker != null)
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

    public void onFCMTokenRefresh(String token) {
        Log.e("game", "onFCMTokenRefresh");
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putString("FCMToken", token);
        editor.commit();
        if (isLoggedIn) {
            mSocket.emit("refreshtoken", token);
        } else {
            FCMToken = token;
        }
    }

}