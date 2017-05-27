package com.danival.game;

import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewTreeObserver;
import android.view.animation.LinearInterpolator;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import io.socket.client.Socket;
import io.socket.emitter.Emitter;

public class MainActivity extends Activity {

    private ColorMatrix colorMatrix;
    private ColorMatrixColorFilter colorFilter;
    private SharedPreferences sharedPref; // arq configurações
    protected Metrics metrics;

    private static final int REQUEST_LOGIN = 0;
    protected int mPlayerId;
    private String mPlayerName;
    private int mEmoji;
    private String mPlayerToken;
    public int mEnergy;
    private Socket mSocket;
    private boolean isConnected = false;
    protected boolean canMove = true;
    private boolean isLoggedIn = false;
    protected TextView status;
    protected RelativeLayout score;
    protected HorizontalScrollView hsv;
    protected RelativeLayout grid;
    protected RadioGroup rgPagging;
    protected RadioButton[] rbPages;
    protected LinearLayout controlBar;
    protected GameMap gameMap;
    protected int COLUMNS_COUNT = 0;
    protected int PAGES_COUNT;
    public ProgressBar moveClock;
    public ObjectAnimator moveClockAnim;
    public ProgressBar attackClock;
    public ObjectAnimator attackClockAnim;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        colorMatrix = new ColorMatrix();
        colorMatrix.setSaturation(0);
        colorFilter = new ColorMatrixColorFilter(colorMatrix);
        sharedPref = getPreferences(Context.MODE_PRIVATE);
        mPlayerName = "";
        setUpSocket();
        moveClock = (ProgressBar) findViewById(R.id.moveClock);
        moveClock.setProgressDrawable(getResources().getDrawable(R.drawable.move_clock));
        moveClock.setProgress(5000);
        //Toast.makeText(getApplicationContext(), "isRunning: " + moveClock.getProgress(), Toast.LENGTH_LONG).show();
        attackClock = (ProgressBar) findViewById(R.id.attackClock);
        attackClock.setProgressDrawable(getResources().getDrawable(R.drawable.attack_clock));
/*
        try {
            mPlayerId = 3;
            gameMap.newBase(0, 1, 1, 0);
            gameMap.newPlayer(0, "a", false, 3, 0);
            gameMap.newPlayer(1, "b", false, 3, 0);
            gameMap.newPlayer(2, "c", false, 3, 0);
            gameMap.newPlayer(3, "d", false, 3, 0);
            gameMap.updateCell(1, 1, new JSONObject("{\"type\": \"base\", \"id\": 0, \"team\": 0, \"members\": [{\"id\": 0}, {\"id\": 1}, {\"id\": 2}]}"), true);
            gameMap.updateCell(1, 1, new JSONObject("{\"type\": \"base\", \"id\": 0, \"team\": 0, \"members\": [{\"id\": 0}, {\"id\": 1}, {\"id\": 2}, {\"id\": 3}]}"), true);

        } catch (JSONException e) {
        }
*/
    }

    private void setUpSocket() {
        App app = (App) this.getApplication();
        mSocket = app.getSocket();
        mSocket.on(Socket.EVENT_CONNECT,onConnect);
        mSocket.on(Socket.EVENT_DISCONNECT,onDisconnect);
        mSocket.on(Socket.EVENT_CONNECT_ERROR, onConnectError);
        mSocket.on(Socket.EVENT_CONNECT_TIMEOUT, onConnectError);
        mSocket.on("onMove", onMove);
        mSocket.on("onCantAttack", onCantAttack);
        mSocket.on("onCantMove", onCantMove);
        mSocket.on("onNewMapItems", onNewMapItems);
        mSocket.on("onRemoveMapItem", onRemoveMapItem);
        mSocket.on("onLoginOk", onLoginOk);
        mSocket.on("onInvalidId", onInvalidId);
        mSocket.on("onPlayerOffline", onPlayerOffline);
        mSocket.on("onPlayerOnline", onPlayerOnline);
        mSocket.on("onLifeChanged", onLifeChanged);
        mSocket.on("onAttack", onAttack);
        mSocket.on("onDied", onDied);
        mSocket.on("onBaseChanged", onBaseChanged);
        mSocket.on("onScoreChanged", onScoreChanged);
        //mSocket.connect();
    }

    private void setUpScreen() {
        metrics = new Metrics(this);
        PAGES_COUNT = Math.round(COLUMNS_COUNT/4);

        // Grid
        setUpGrid();
        gameMap = new GameMap(this);

        // Status bar
        status = (TextView) this.findViewById(R.id.status);

        // Score
        score = (RelativeLayout) this.findViewById(R.id.score);
        score.setY(metrics.getTopFaixa(2));

        // Pagging
        LayoutInflater inflater = getLayoutInflater();
        rgPagging = (RadioGroup) this.findViewById(R.id.rgPagging);
        rgPagging.setY(metrics.getTopFaixa(4));
        rbPages = new RadioButton[PAGES_COUNT];
        for(int i=0; i<PAGES_COUNT; i++){
            rbPages[i] = (RadioButton) inflater.inflate(R.layout.rb_pages, rgPagging, false);
            rbPages[i].setTag(i);
            rgPagging.addView(rbPages[i]);
        }
        rbPages[0].setChecked(true);
        if (PAGES_COUNT == 1) {
            rgPagging.setVisibility(RadioGroup.INVISIBLE);
        }

        // Botões
        controlBar = (LinearLayout) findViewById(R.id.controlBar);
        controlBar.setY(metrics.getTopFaixa(5));
    }
        @Override
    public void onResume() {
        super.onResume();
        mSocket.connect();
    }

    @Override
    public void onPause() {
        super.onPause();
        if (isConnected) {
            mSocket.disconnect();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        //mSocket.disconnect();
        mSocket.off(Socket.EVENT_CONNECT,onConnect);
        mSocket.off(Socket.EVENT_DISCONNECT,onDisconnect);
        mSocket.off(Socket.EVENT_CONNECT_ERROR, onConnectError);
        mSocket.off(Socket.EVENT_CONNECT_TIMEOUT, onConnectError);
        mSocket.off("onMove", onMove);
        mSocket.off("onCantAttack", onCantAttack);
        mSocket.off("onCantMove", onCantMove);
        mSocket.off("onNewMapItems", onNewMapItems);
        mSocket.off("onRemoveMapItem", onRemoveMapItem);
        mSocket.off("onLoginOk", onLoginOk);
        mSocket.off("onInvalidId", onInvalidId);
        mSocket.off("onPlayerOffline", onPlayerOffline);
        mSocket.off("onPlayerOnline", onPlayerOnline);
        mSocket.off("onLifeChanged", onLifeChanged);
        mSocket.off("onAttack", onAttack);
        mSocket.off("onDied", onDied);
        mSocket.off("onBaseChanged", onBaseChanged);
        mSocket.off("onScoreChanged", onScoreChanged);
    }

    private void setUpGrid() {
        hsv = (HorizontalScrollView)findViewById(R.id.myHorizontalScrollView);
        hsv.setY(metrics.getTopFaixa(3));
        hsv.getViewTreeObserver().addOnScrollChangedListener(new ViewTreeObserver.OnScrollChangedListener() {
            @Override
            public void onScrollChanged() {
                changePage(hsv.getScrollX());
            }
        });
        grid = (RelativeLayout)findViewById(R.id.grid);
        grid.getLayoutParams().width = metrics.getGridWidth();
        grid.getLayoutParams().height = metrics.getGridHeight();

        //setUpGridTouch();
    }

    public void setUpGridTouch() {
        grid.setOnTouchListener(new View.OnTouchListener() {
            int startX;
            int startY;
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    startX = (int) event.getX();
                    startY = (int) event.getY();
                }

                if (event.getAction() == MotionEvent.ACTION_UP) {
                    int endX = (int) event.getX();
                    int endY = (int) event.getY();
                    int dX = Math.abs(endX - startX);
                    int dY = Math.abs(endY - startY);

/*
                    Player player = gameMap.playerList.get(gameMap.findPlayer(mPlayerId));
                    if (!isLoggedIn || player.life == 0 || !player.bomb.available ) { return true; }
                    if (Math.sqrt(Math.pow(dX, 2) + Math.pow(dY, 2)) <= ViewConfiguration.get(getApplicationContext()).getScaledTouchSlop()) {
                        int col = metrics.getCol(startX);
                        int row = metrics.getRow(startY);
                        int difCol = Math.abs(col - player.col);
                        int difRow = Math.abs(row - player.row);
                        if ((difCol + difRow) == 1) { // clicou do lado!!!
                            player.bomb.available = false;
                            mSocket.emit("attack", col, row);
                        }
                    }
*/
                }
                return true;
            }
        });
    }

    public void changePage (int x){
        int pag = (x + metrics.w/2) / metrics.w;
        rbPages[pag].setChecked(true);
    }

    public void onRadioButtonClicked(View view) {
        int pag = (int)view.getTag() + 1;
        hsv.smoothScrollTo((pag-1)*metrics.w, 0);
    }

    public void attackOnClick(View v) {
        Player player = gameMap.playerList.get(gameMap.findPlayer(mPlayerId));
        if (!isLoggedIn || player.life == 0 || !player.bomb.available ) { return; }
        player.bomb.available = false;
        status.setText("attacking...");
        switch (v.getId()) {
            case R.id.btnBombLeft:
                mSocket.emit("attack", "L");
                break;
            case R.id.btnBombUp:
                mSocket.emit("attack", "U");
                break;
            case R.id.btnBombDown:
                mSocket.emit("attack", "D");
                break;
            case R.id.btnBombRight:
                mSocket.emit("attack", "R");
                //attack(2,2,3,2);
                break;
        }
    }

    public void moveOnClick(View v) {
        Player player = gameMap.playerList.get(gameMap.findPlayer(mPlayerId));
        if (!isLoggedIn || player.life == 0 || !canMove) { return; }
        canMove = false;
        ImageView moveControlBG = (ImageView) findViewById(R.id.moveControlBG);
        moveControlBG.setColorFilter(colorFilter);
        status.setText("moving...");
        switch (v.getId()) {
            case R.id.btnLeft:
                mSocket.emit("move", "L");
                break;
            case R.id.btnUp:
                mSocket.emit("move", "U");
                break;
            case R.id.btnDown:
                mSocket.emit("move", "D");
                break;
            case R.id.btnRight:
                mSocket.emit("move", "R");
                //attack(2,2,3,2);
                break;
        }
    }

    private void startSignIn() {
        mPlayerName = "";
        Intent intent = new Intent(this, LoginActivity.class);
        startActivityForResult(intent, REQUEST_LOGIN);
    }
/*
    public static int getImageId(Context context, String imageName) {
        return context.getResources().getIdentifier("drawable/" + imageName, null, context.getPackageName());
    }
*/

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
            //Toast.makeText(getApplicationContext(), "escolhido: " + mEmoji, Toast.LENGTH_LONG).show();
        } catch(NumberFormatException nfe) {}

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will automatically handle clicks on the Home/up button, so long as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_leave) {
            //leave();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    /*
    private void leave() {
        mPlayerName = null;
        mSocket.disconnect();
        mSocket.connect();
        startSignIn();
    }
    */


    // scroll para o player aparecer na tela
    protected void scrollToPlayer(int id, float newX) {
        if (id != mPlayerId) { return; }
        int maxScroll = metrics.getMaxScrollPlayerVisible(Math.round(newX));
        int minScroll = metrics.getMinScrollPlayerVisible(Math.round(newX));
        if ( hsv.getScrollX() > maxScroll )  {
            hsv.smoothScrollTo(maxScroll, 0);
        }
        if ( hsv.getScrollX() < minScroll ) {
            hsv.smoothScrollTo(minScroll, 0);
        }
    }

    protected void scoreChanged(int scoreA, int scoreB) {
        TextView tvScoreA = (TextView) this.findViewById(R.id.scoreA);
        TextView tvScoreB = (TextView) this.findViewById(R.id.scoreB);
        tvScoreA.setText("EQUIPE A - " + scoreA);
        tvScoreB.setText(scoreB + " - EQUIPE B");
    }

    protected void updateMoveControl() {
        ImageView moveControlBG = (ImageView) findViewById(R.id.moveControlBG);
        TextView energy = (TextView) findViewById(R.id.energy);
        int level = (int) mEnergy/5000;
        String levelText = (level == 0) ? "  " : level + "";
        energy.setText(levelText);

        if (mEnergy >= 5*5000) { // estoque cheio
            if ( (moveClockAnim != null) && moveClockAnim.isRunning() ) {
                moveClockAnim.pause();
                moveClock.setProgress(5000);
                moveClock.setVisibility(View.INVISIBLE);
            }
            moveControlBG.clearColorFilter();
            canMove = true;
        } else {
            if (mEnergy >= 5000) {
                moveControlBG.clearColorFilter();
                canMove = true;
            } else {
                moveControlBG.setColorFilter(colorFilter);
                canMove = false;
            }

            //Toast.makeText(getApplicationContext(), "getProgress: " + moveClock.getProgress(), Toast.LENGTH_LONG).show();

            if (moveClock.getProgress() == 5000) {
                moveClock.setVisibility(View.VISIBLE);
                moveClockAnim = ObjectAnimator.ofInt(moveClock, "progress", 0, 5000); //5 seg
                moveClockAnim.setDuration(5000); //milliseconds
                moveClockAnim.setInterpolator (new LinearInterpolator());
                moveClockAnim.addListener(new Animator.AnimatorListener() {

                    @Override
                    public void onAnimationStart(Animator animation) {}

                    @Override
                    public void onAnimationRepeat(Animator animation) {}

                    @Override
                    public void onAnimationEnd(Animator animation) {
                        if (mEnergy < 5*5000) {
                            mEnergy += 5000;
                        }
                        //Toast.makeText(getApplicationContext(), "isRunning: " + moveClockAnim.isRunning(), Toast.LENGTH_LONG).show();
                        updateMoveControl();
                    }

                    @Override
                    public void onAnimationCancel(Animator animation) {}
                });

                moveClockAnim.start();
            }

        }
    }

    protected void scrollOneColumn(String direction) { // "L" ou "R"
/*
        int col = metrics.getCol(hsv.getScrollX());
        col = (direction.equals("L")) ? Math.max(col-1, 0) : Math.min(col+1, (COLUMNS_COUNT-1));
        Player player = gameMap.playerList.get(gameMap.findPlayer(mPlayerId));
        //if (Math.abs(col-player.col) > 1) { return; }
        hsv.smoothScrollTo(metrics.getX(col), 0);
*/
/*
        Player player = gameMap.playerList.get(gameMap.findPlayer(mPlayerId));
        int col = (direction.equals("L")) ? Math.max(player.col-1, 1) : Math.min(player.col-2, 13);
        hsv.smoothScrollTo(metrics.getX(col)-metrics.hPadding, 0);
*/
    }

    private void afterConnect() {
        mPlayerId = sharedPref.getInt("id", 0);
        mPlayerToken = sharedPref.getString("token", "");
        if (mPlayerToken.equals("")) {
            if (mPlayerName.equals("")) {
                startSignIn();
            }
            else {
                mSocket.emit("playerLogin", mPlayerName, mEmoji);
            }
        }
        else {
            mSocket.emit("playerReconnect", mPlayerId, mPlayerToken);
        }
        isConnected = true;
    }

    private void afterDisconnect() {
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putInt("id", mPlayerId);
        editor.putString("token", mPlayerToken);
        editor.commit();
        if (isLoggedIn) {
            gameMap.clear();
        }
        isConnected = false;
        isLoggedIn = false;
    }

    private Emitter.Listener onConnect = new Emitter.Listener() {
        @Override
        public void call(Object... args) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
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
                    afterDisconnect();
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

    private Emitter.Listener onLoginOk = new Emitter.Listener() {
        @Override
        public void call(final Object... args) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    try {
                        JSONObject data = (JSONObject) args[0];
                        COLUMNS_COUNT = data.getInt("COLUMNS_COUNT");
                        mPlayerId = data.getInt("id");
                        mPlayerToken = data.getString("token");
                        //int timeToMove = data.getInt("timeToMove");
                        mEnergy = data.getInt("energy");
                        setUpScreen();
                        //isMoving = timeToMove > 0;
                        //canMove = mEnergy >= 5000;
                        updateMoveControl();
                        isLoggedIn = true;
                    } catch (JSONException e) { Log.e("game", Log.getStackTraceString(e)); }
                }
            });
        }
    };

    private Emitter.Listener onNewMapItems = new Emitter.Listener() {
        @Override
        public void call(final Object... args) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    try {
                        JSONArray list = (JSONArray) args[0];
                        for (int i = 0; i < list.length(); i++) {
                            JSONObject data = list.getJSONObject(i);
                            if (data.getString("type").equals("base")) {
                                gameMap.newBase(data.getInt("id"), data.getInt("col"), data.getInt("row"), data.getInt("team"), data.getInt("flag"));
                            }
                            if (data.getString("type").equals("player")) {
                                gameMap.newPlayer(data.getInt("id"), data.getString("name"), data.getInt("emoji"), data.getBoolean("onLine"), data.getInt("life"), data.getInt("team"));
                            }
                            if (data.getString("type").equals("cell")) {
                                gameMap.updateCell(data.getInt("col"), data.getInt("row"), data.getJSONObject("cell"), data.getInt("flag"), true);
                            }
                        }
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
                    JSONObject data = (JSONObject) args[0];
                    try {
                        gameMap.playerMove(data.getInt("id"), data.getInt("colFrom"), data.getInt("rowFrom"), data.getJSONObject("cellFrom"), data.getInt("colTo"), data.getInt("rowTo"), data.getJSONObject("cellTo"), data.getInt("flag"));
                        status.setText("");
                        if (data.getInt("id") == mPlayerId) {
                            mEnergy -= 5000;
                            updateMoveControl();
                        }
                    } catch (JSONException e) { Log.e("game", Log.getStackTraceString(e)); }
                }
            });
        }
    };

    private Emitter.Listener onDied = new Emitter.Listener() {
        @Override
        public void call(final Object... args) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    JSONObject data = (JSONObject) args[0];
                    try {
                        gameMap.playerDie(data.getInt("id"), data.getInt("col"), data.getInt("row"), data.getJSONObject("cell"), data.getInt("flag"));
                    } catch (JSONException e) { Log.e("game", Log.getStackTraceString(e)); }
                }
            });
        }
    };

    private Emitter.Listener onCantAttack = new Emitter.Listener() {
        @Override
        public void call(final Object... args) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Player player = gameMap.playerList.get(gameMap.findPlayer(mPlayerId));
                    player.bomb.available = true;
                    status.setText("onCantAttack");
                }
            });
        }
    };

    private Emitter.Listener onCantMove = new Emitter.Listener() {
        @Override
        public void call(final Object... args) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    canMove = true;
                    status.setText("onCantMove");
                    ImageView moveControlBG = (ImageView) findViewById(R.id.moveControlBG);
                    moveControlBG.clearColorFilter();

                }
            });
        }
    };

    private Emitter.Listener onAttack = new Emitter.Listener() {
        @Override
        public void call(final Object... args) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    JSONObject data = (JSONObject) args[0];
                    try {
                        gameMap.attack(data.getInt("id"), data.getInt("colFrom"), data.getInt("rowFrom"), data.getInt("colTo"), data.getInt("rowTo"));
                        status.setText("");
                    } catch (JSONException e) { Log.e("game", Log.getStackTraceString(e)); }
                }
            });
        }
    };


    private Emitter.Listener onLifeChanged = new Emitter.Listener() {
        @Override
        public void call(final Object... args) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    JSONObject data = (JSONObject) args[0];
                    try {
                        gameMap.playerLifeChanged(data.getInt("id"), data.getInt("life"));
                    } catch (JSONException e) { Log.e("game", Log.getStackTraceString(e)); }
                }
            });
        }
    };

    private Emitter.Listener onRemoveMapItem = new Emitter.Listener() {
        @Override
        public void call(final Object... args) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    JSONObject data = (JSONObject) args[0];
/*
                    try {
                        gameMap.removeMapItem(data.getString("type"), data.getInt("id"));
                    } catch (JSONException e) {
                        return;
                    }
*/
                }
            });
        }
    };

    private Emitter.Listener onPlayerOffline = new Emitter.Listener() {
        @Override
        public void call(final Object... args) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    JSONObject data = (JSONObject) args[0];
                    try {
                        gameMap.playerOffline(data.getInt("id"));
                    } catch (JSONException e) { Log.e("game", Log.getStackTraceString(e)); }
                }
            });
        }
    };

    private Emitter.Listener onPlayerOnline = new Emitter.Listener() {
        @Override
        public void call(final Object... args) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    JSONObject data = (JSONObject) args[0];
                    try {
                        gameMap.playerOnline(data.getInt("id"));
                    } catch (JSONException e) { Log.e("game", Log.getStackTraceString(e)); }
                }
            });
        }
    };

    private Emitter.Listener onInvalidId = new Emitter.Listener() {
        @Override
        public void call(final Object... args) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    //JSONObject data = (JSONObject) args[0];
                    mPlayerId = 0;
                    mPlayerToken = "";
                    afterDisconnect();
                    afterConnect();
                }
            });
        }
    };

    private Emitter.Listener onBaseChanged = new Emitter.Listener() {
        @Override
        public void call(final Object... args) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    JSONObject data = (JSONObject) args[0];
                    try {
                        gameMap.baseChanged(data.getInt("col"), data.getInt("row"), data.getInt("team"));
                    } catch (JSONException e) { Log.e("game", Log.getStackTraceString(e)); }
                }
            });
        }
    };

    private Emitter.Listener onScoreChanged = new Emitter.Listener() {
        @Override
        public void call(final Object... args) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    JSONObject data = (JSONObject) args[0];
                    try {
                        scoreChanged(data.getInt("scoreA"), data.getInt("scoreB"));
                    } catch (JSONException e) { Log.e("game", Log.getStackTraceString(e)); }
                }
            });
        }
    };
}
