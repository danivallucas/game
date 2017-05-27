package com.danival.game;


import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.LinearInterpolator;
import android.widget.BaseAdapter;
import android.widget.GridLayout;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class Cell {
    protected MainActivity main;
    public String type;
    public int id;
    public int team;
    public ProgressBar risingFlag;
    public ObjectAnimator risingFlagAnim;
    public List<Integer> members;
    public ImageView icon; // pill, food, fence, etc
    public RelativeLayout cell_grid;
    public RelativeLayout cell_icon;

    public Cell(MainActivity context, String _type, int _id, int _team) {
        main = context;
        type = _type;
        id = _id;
        team = _team;
        members = new ArrayList<Integer>();
        LayoutInflater inflater = main.getLayoutInflater();
        cell_grid = (RelativeLayout) inflater.inflate(R.layout.cell_grid, main.grid, false);
        cell_icon = (RelativeLayout) inflater.inflate(R.layout.cell_icon, main.grid, false);
        icon = (ImageView) cell_icon.findViewById(R.id.cell_icon);
        icon.setVisibility(ImageView.INVISIBLE);
        risingFlag = (ProgressBar) cell_grid.findViewById(R.id.risingFlag);
    }

    public void update() {
        int size = 0;
        int playerId;
        Player player;
        icon.setVisibility(ImageView.INVISIBLE);
        cell_grid.setVisibility(RelativeLayout.INVISIBLE);
        RelativeLayout gridContainer = (RelativeLayout) cell_grid.findViewById(R.id.gridContainer);
        GridLayout grid = (GridLayout) gridContainer.findViewById(R.id.grid);
        grid.removeAllViews();


        if ( type.equals("fence") || type.equals("pill") ) {
            String identifier = "";
            if (type.equals("fence")) {
                identifier = "com.danival.game:drawable/fence4";
                size = (int) main.getResources().getDimension(R.dimen.player_icon_width);
            }
            if (type.equals("pill")) {
                identifier = "com.danival.game:drawable/pill";
                size = (int) main.getResources().getDimension(R.dimen.pill_width);
            }
            int id = main.getResources().getIdentifier(identifier, null, null);
            icon.setImageResource(id);
            icon.getLayoutParams().width = size;
            icon.getLayoutParams().height = size;
            //icon.setLayoutParams(new RelativeLayout.LayoutParams(size, size));

            icon.setVisibility(ImageView.VISIBLE);
        } else
        if ( type.equals("group") && members.size() == 1 ) {
            playerId = members.get(0);
            player = main.gameMap.playerList.get(main.gameMap.findPlayer(playerId));
            player.view.setVisibility(View.VISIBLE);
        } else
        if ( type.equals("base") || members.size() > 1 ) {
            if (type.equals("base")) {
                gridContainer.setBackgroundResource(R.drawable.base_background);
                setLabel("Base " + (id+1));
            }
            if (type.equals("group")) {
                gridContainer.setBackgroundResource(R.drawable.group_background);
                setGroupTeam(team);
            }
            if (members.size() <= 4) {
                grid.setColumnCount(2);
                size = main.metrics.getSizeIcon4x4();
            } else {
                grid.setColumnCount(3);
                size = main.metrics.getSizeIcon9x9();
            }
            for (int i = 0; i < members.size(); i++) {
                playerId = members.get(i);
                player = main.gameMap.playerList.get(main.gameMap.findPlayer(playerId));
                player.view.setVisibility(View.INVISIBLE);
//                player.miniIconContainer.getLayoutParams().width = size;
//                player.miniIconContainer.getLayoutParams().height = size;
                player.miniIconContainer.setLayoutParams(new RelativeLayout.LayoutParams(size, size));
                grid.addView(player.miniIconContainer);
            }
            cell_grid.setVisibility(RelativeLayout.VISIBLE);
        }
        setRisingFlag();
    }

    public void setRisingFlag() {
        if (!type.equals("base")) { return; }
        Base base = main.gameMap.baseList.get(main.gameMap.findBase(id));
        int timeLeft;
        if (members.size() == 0) {
            timeLeft = 0;
        } else {
            timeLeft = Math.round( ( (100 - base.flag) / 10*members.size() ) * 1000 ) ; // miliseg
        }
        if (timeLeft == 0) {
            risingFlag.setVisibility(View.INVISIBLE);
            if (risingFlagAnim != null) {
                risingFlagAnim.cancel();
            }
        } else {
            if (team == 0) {
                risingFlag.setProgressDrawable(main.getResources().getDrawable(R.drawable.rising_flag_a));
            } else {
                risingFlag.setProgressDrawable(main.getResources().getDrawable(R.drawable.rising_flag_b));
            }

            risingFlag.setVisibility(View.VISIBLE);
            risingFlagAnim = ObjectAnimator.ofInt(risingFlag, "progress", (10000-timeLeft), 10000); //10 seg
            risingFlagAnim.setDuration(timeLeft); //milliseconds
            risingFlagAnim.setInterpolator (new LinearInterpolator());
            risingFlagAnim.addListener(new Animator.AnimatorListener() {

                @Override
                public void onAnimationStart(Animator animation) {}

                @Override
                public void onAnimationRepeat(Animator animation) {}

                @Override
                public void onAnimationEnd(Animator animation) {
                    //bomb.setLayerType(View.LAYER_TYPE_NONE, null);
                    risingFlag.setVisibility(View.INVISIBLE);
                }

                @Override
                public void onAnimationCancel(Animator animation) {}
            });

            risingFlagAnim.start();
        }
    }

    public void setLabel(String text) {
        TextView tvLabel = (TextView) cell_grid.findViewById(R.id.label);
        tvLabel.setText(text);
    }

    public void setGroupTeam(int team) {
        TextView tvTeam = (TextView) cell_grid.findViewById(R.id.team);
        switch (team) {
            case -1:
                tvTeam.setText("");
                tvTeam.setBackgroundResource(0);
                break;
            case 0:
                tvTeam.setText("A");
                tvTeam.setBackgroundResource(R.drawable.team_a);
                break;
            case 1:
                tvTeam.setText("B");
                tvTeam.setBackgroundResource(R.drawable.team_b);
                break;
        }
    }


        public void sort() {
        Collections.sort(members, new Comparator<Integer>() {
            @Override
            public int compare(Integer id1, Integer id2) {
                if (id1 == main.mPlayerId) {
                    return -1;
                } else {
                    return id1 - id2;
                }
            }
        });
    }

}
