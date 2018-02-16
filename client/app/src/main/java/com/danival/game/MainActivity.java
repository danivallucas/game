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

    public int START_ENERGY;
    public int WAIT_AFTER_LEG;
    public int WAIT_AFTER_BOMB;
    public int BOMB_MAX_DIST;
    public int BOMB_UNIT_COST;
    public int TURBO_MAX_DIST;
    public int SPAWN_AREA;
    public App app;
    public Socket mSocket;
    protected GoogleMap mMap;
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
    private LatLng mPosition;
    private Circle spawnLimitUI;
    private boolean isConnected = false;
    private boolean isLoggedIn = false;
    private int uiState = 0; // 0=conectando, 1=spawing, 2=main, 3=Go, 4=turbo, 5=Bomb, 6=Moving
    private float previousZoomLevel = -1.0f;
    protected TextView ranking;
    protected TextView msg;
    protected TextView alert;
    protected Game game;
    public Handler animHandler; // Animator (movimenta os markers). Iniciado depois do Login com sucesso.
    private Handler zoomHandler; // Verifica a visibilidade ao mudar o zoom
    private List<LatLng> routePositions;
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
        routePositions = new ArrayList<LatLng>();
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

    public void tryConnectAgain(int mili) {
        new CountDownTimer(mili, 0) {
            public void onTick(long millisUntilFinished) {}
            public void onFinish() {
                enterGame();
            }
        }.start();
    }

    private void enterGame() {
        changeUIState(0);
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
        changeUIState(0);
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
                                WAIT_AFTER_LEG = conf.getInt("WAIT_AFTER_LEG");
                                WAIT_AFTER_BOMB = conf.getInt("WAIT_AFTER_BOMB");
                                BOMB_MAX_DIST = conf.getInt("BOMB_MAX_DIST");
                                BOMB_UNIT_COST =  conf.getInt("BOMB_UNIT_COST");
                                TURBO_MAX_DIST = conf.getInt("TURBO_MAX_DIST");
                                SPAWN_AREA = conf.getInt("SPAWN_AREA");
                                isLoggedIn = true;
                                if (playerStatus.equals("out")) {
                                    goSpawnState();
                                } else {
                                    setBtnGoDelayed(data.getInt("timeToNewRoute"));
                                    setBtnTurboDelayed(data.getInt("timeToNewRoute"));
                                    setBtnBombDelayed(data.getInt("timeToNewBomb"));
                                    if (playerStatus.equals("in"))
                                        changeUIState(2);
                                    if (playerStatus.equals("moving"))
                                        changeUIState(6);
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
                            if (!isLoggedIn) return;
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
                        if (!isLoggedIn) return;
                        Log.e("game", "onNewFlag.....");
                        JSONArray list = (JSONArray) args[0];
                        for (int i = 0; i < list.length(); i++) {
                            JSONObject data = list.getJSONObject(i);
                            Log.e("game", "id: " + data.getString("id") + " type: " + data.getString("type") + " points: " + data.getString("points") + " ");
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
                        Log.e("game", "onNewBomb");
                        JSONArray list = (JSONArray) args[0];
                        for (int i = 0; i < list.length(); i++) {
                            JSONObject data = list.getJSONObject(i);
                            Bomb bomb = game.newBomb(data.getInt("id"), data.getInt("type"), new LatLng(data.getDouble("lat"), data.getDouble("lng")), data.getInt("energy"));
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
                        player.energy = data.getInt("energy");
                        player.energyToRestore = data.getInt("energyToRestore");
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
                        if (!isLoggedIn) return;
                        JSONObject data = (JSONObject) args[0];
                        int id = data.getInt("id");
                        Player player = game.getPlayer(id);
                        if ( (player.id == mPlayerId) && (player.legList.size() == 1) ) {
                            // se é a última leg, sai do uiState=6 (moving)
                            setBtnGoDelayed(data.getInt("timeToNewRoute"));
                            setBtnTurboDelayed(data.getInt("timeToNewRoute"));
                            changeUIState(2);
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
                        Player player = game.getPlayer(id);
                        player.onEnergyChange(data.getInt("energy"), data.getInt("energyToRestore"));
                        if (id == mPlayerId) {
                            if (uiState == 2)
                                changeUIState(2); // atualiza visibilidade dos botões
                            if ( (uiState == 3) || (uiState == 4) || (uiState == 5) ) {
                                Toast.makeText(getApplicationContext(), "Você foi atingido!", Toast.LENGTH_LONG).show();
                                onBtnCancelClick(null); // cancela a operação que estiver fazendo e vai para a tela inicial (uiState=2)
                            }
                        }
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
                        if (!isLoggedIn) return;
                        JSONObject data = (JSONObject) args[0];
                        int id = data.getInt("id");
                        //Toast.makeText(getApplicationContext(), "onStop: " + id, Toast.LENGTH_LONG).show();
                        Player player = game.getPlayer(id);
                        player.stop(new LatLng(data.getDouble("lat"), data.getDouble("lng")));
                        if (id == mPlayerId) {
                            int wait = data.getInt("timeToNewRoute");
                            setBtnGoDelayed(wait);
                            setBtnTurboDelayed(wait);
                        }

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
            int maxDist = player.energy * TURBO_MAX_DIST;
            double dist = SphericalUtil.computeDistanceBetween(player.position, latLng);
            if ( dist > maxDist ) {
                Toast.makeText(getApplicationContext(), "Erro: Distância maior que a permitida.", Toast.LENGTH_LONG).show();
                return;
            }
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
        player.showOriginMarker();
        changeUIState(3);
        addRoutePosition(player.position);
    }

    public void onBtnTurboClick(View v) {
        if (!isLoggedIn) return;
        Player player = game.getPlayer(mPlayerId);
        player.showOriginMarker();
        player.drawTurboLimit();
        changeUIState(4);
        addRoutePosition(player.position);
    }

    public void onBtnBombClick(View v) {
        if (!isLoggedIn) return;
        Player player = game.getPlayer(mPlayerId);
        if ( (player.energy - BOMB_UNIT_COST) < START_ENERGY ) {
            Toast.makeText(getApplicationContext(), "Energia insuficiente", Toast.LENGTH_LONG).show();
            return;
        }
        player.drawBombLimit();
        player.showOriginMarker();
        changeUIState(5);
    }

    public void onBtnOkClick(View v) {
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
        Player player = game.getPlayer(mPlayerId);
        player.stop(player.position);
        mSocket.emit("stop", player.position.latitude, player.position.longitude);
        changeUIState(2);
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
        if (routePositions.size() > 10) {
            Toast.makeText(getApplicationContext(), "Erro: Muitos trechos.", Toast.LENGTH_LONG).show();
            return;
        }
        mSocket.emit("move", "normal", encodeRoute());
        Player player = game.getPlayer(mPlayerId);
        player.refreshIcon();
        if (routePolyline != null)
            routePolyline.remove();
        routePositions.clear();
        //setBtnGoDelayed(WAIT_AFTER_LEG);
        //setBtnTurboDelayed(WAIT_AFTER_LEG);
        changeUIState(6);
    }

    public void finishTurbo() {
        if (routePositions.size() < 2) {
            Toast.makeText(getApplicationContext(), "Escolha um local.", Toast.LENGTH_LONG).show();
            return;
        }
        mSocket.emit("move", "turbo", encodeRoute());
        Player player = game.getPlayer(mPlayerId);
        player.refreshIcon();
        player.clearTurboLimit();
        if (routePolyline != null)
            routePolyline.remove();
        routePositions.clear();
        changeUIState(6);
    }

    public void finishBomb(LatLng latLng) {
        Player player = game.getPlayer(mPlayerId);
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
        player.clearTurboLimit();
        player.refreshIcon();
        routePositions.clear();
        if (routePolyline != null)
            routePolyline.remove();
        changeUIState(2);
    }

    public void cancelBomb(View v) {
        Player player = game.getPlayer(mPlayerId);
        player.refreshIcon();
        player.clearBombLimit();
        changeUIState(2);
    }

    public void changeUIState(int state) {
        uiState = state;
        msg.setVisibility(View.GONE);
        alert.setVisibility(View.GONE);
        ranking.setVisibility(View.GONE);
        findViewById(R.id.btnBomb).setVisibility(View.GONE);
        findViewById(R.id.btnTurbo).setVisibility(View.GONE);
        findViewById(R.id.btnGo).setVisibility(View.GONE);
        findViewById(R.id.btnOk).setVisibility(View.GONE);
        findViewById(R.id.btnCancel).setVisibility(View.GONE);
        findViewById(R.id.btnStop).setVisibility(View.GONE);

        switch (state) {
            case 0: // connect
                alert.setText("Conectando...");
                alert.setVisibility(View.VISIBLE);
                break;
            case 1: // spawn
                msg.setText("Clique no mapa para iniciar.");
                msg.setVisibility(View.VISIBLE);
                ranking.setVisibility(View.VISIBLE);
                break;
            case 2: // main
                Player player = game.getPlayer(mPlayerId);
                if ( (player.energy - BOMB_UNIT_COST) >= START_ENERGY )
                    findViewById(R.id.btnBomb).setVisibility(View.VISIBLE);
                if ( player.energy >= START_ENERGY * 2 )
                    findViewById(R.id.btnTurbo).setVisibility(View.VISIBLE);
                findViewById(R.id.btnGo).setVisibility(View.VISIBLE);
                ranking.setVisibility(View.VISIBLE);
                break;
            case 3: // go
                msg.setText("Monte uma rota.");
                msg.setVisibility(View.VISIBLE);
                findViewById(R.id.btnOk).setVisibility(View.VISIBLE);
                findViewById(R.id.btnCancel).setVisibility(View.VISIBLE);
                break;
            case 4: // turbo
                msg.setText("Clique dentro do limite.");
                msg.setVisibility(View.VISIBLE);
                findViewById(R.id.btnOk).setVisibility(View.VISIBLE);
                findViewById(R.id.btnCancel).setVisibility(View.VISIBLE);
                break;
            case 5: // bomb
                msg.setText("Clique dentro do limite.");
                msg.setVisibility(View.VISIBLE);
                findViewById(R.id.btnCancel).setVisibility(View.VISIBLE);
                break;
            case 6: // moving
                ranking.setVisibility(View.VISIBLE);
                findViewById(R.id.btnStop).setVisibility(View.VISIBLE);
                break;
        }

    }

    public void setBtnGoDelayed(int mili) {
        final Button btnGo = (Button) findViewById(R.id.btnGo);
        btnGo.setAlpha(0.5f);
        btnGo.setClickable(false);
        new CountDownTimer(mili, 1000) {
            public void onTick(long millisUntilFinished) {
                btnGo.setText(millisUntilFinished / 1000 + "s");
            }
            public void onFinish() {
                btnGo.setText("Go");
                btnGo.setAlpha(1.0f);
                btnGo.setClickable(true);
            }
        }.start();
    }

    public void setBtnTurboDelayed(int mili) {
        final Button btnTurbo = (Button) findViewById(R.id.btnTurbo);
        btnTurbo.setAlpha(0.5f);
        btnTurbo.setClickable(false);
        new CountDownTimer(mili, 1000) {
            public void onTick(long millisUntilFinished) {
                btnTurbo.setText(millisUntilFinished / 1000 + "s");
            }
            public void onFinish() {
                btnTurbo.setText("Turbo");
                btnTurbo.setAlpha(1.0f);
                btnTurbo.setClickable(true);
            }
        }.start();
    }

    public void setBtnBombDelayed(int mili) {
        final Button btnBomb = (Button) findViewById(R.id.btnBomb);
        btnBomb.setAlpha(0.5f);
        btnBomb.setClickable(false);
        new CountDownTimer(mili, 1000) {
            public void onTick(long millisUntilFinished) {
                btnBomb.setText(millisUntilFinished / 1000 + "s");
            }
            public void onFinish() {
                btnBomb.setText("Bomb");
                btnBomb.setAlpha(1f);
                btnBomb.setClickable(true);
            }
        }.start();
    }

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
                latLng = player.position;
                break;
            case "Food":
                Food food = game.getFood(id);
                latLng = food.position;
                break;
            case "Bomb":
                Bomb bomb = game.getBomb(id);
                latLng = bomb.position;
                break;
            case "Flag":
                Flag flag = game.getFlag(id);
                latLng = flag.position;
                break;
        }
        if (uiState == 1)
            finishSpawn(latLng);
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
    }

    public void checkPlayerListVisibility() {
        int count = 0;
        for (int i = 0; i < game.playerList.size(); i++) {
            Player player = game.playerList.get(i);
            if (    (player.marker == null) ||
                    (player.status.equals("out")) ||
                    (player.id == mPlayerId)    )
                continue;
            if (++count <= 3) {
                player.marker.setVisible(true); // os 3 primeiros sempres estarão visíveis (podium!!!)
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

}