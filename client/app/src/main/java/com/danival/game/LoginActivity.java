package com.danival.game;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import io.socket.client.Socket;


public class LoginActivity extends Activity {

    private int previousSelectedPosition = 0;
    private GridView gridView;
    private App app;
    protected Socket mSocket;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        app = (App) this.getApplication();
        mSocket = app.getSocket();

        final EditText mUsernameView = (EditText) findViewById(R.id.username_input);
        Button signInButton = (Button) findViewById(R.id.sign_in_button);
        signInButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent();
                intent.putExtra("mPlayerName", mUsernameView.getText().toString().trim());
                intent.putExtra("mEmoji", previousSelectedPosition);
                setResult(RESULT_OK, intent);
                finish();
            }
        });

        gridView = (GridView) findViewById(R.id.grid_emoji);
        gridView.setAdapter(new ImageAdapter(this));
        gridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                view.setBackgroundColor(Color.parseColor("#FFFFFF"));
                ImageView previousSelectedView = (ImageView) gridView.getChildAt(previousSelectedPosition);
                previousSelectedView.setBackground(null);
                previousSelectedPosition = position;
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        app.setTag(1);

        // AUTO LOGIN
/*
        Intent intent = new Intent();
        intent.putExtra("mPlayerName", "Danival");
        intent.putExtra("mEmoji", 0);
        setResult(RESULT_OK, intent);
        finish();
*/
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (!isFinishing()) {
            mSocket.disconnect();
        }
    }

    public class ImageAdapter extends BaseAdapter {
        private Context mContext;

        // Constructor
        public ImageAdapter(Context c){
            mContext = c;
        }

        @Override
        public int getCount() {
            return 104;
        }

        @Override
        public Object getItem(int position) {
            return null;
        }

        @Override
        public long getItemId(int position) {
            return 0;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ImageView imageView = new ImageView(mContext);
            String id = String.format("%03d", position+1);
            int idEmoji = LoginActivity.this.getResources().getIdentifier("com.danival.game:drawable/" + "emoji" + id, null, null);
            imageView.setImageResource(idEmoji);
            imageView.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
            int size = (int) getResources().getDimension(R.dimen.emoji_login);
            imageView.setLayoutParams(new GridView.LayoutParams(size, size));
            if (position == 0) {
                imageView.setBackgroundColor(Color.parseColor("#FFFFFF"));
            }
            return imageView;
        }
    }

}



