package com.danival.game;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;


public class Player {
    private ColorMatrix colorMatrix;
    private ColorMatrixColorFilter colorFilter;
    protected MainActivity main;
    public int id;
    public String name;
    public int emoji;
    public boolean onLine;
    public int life;
    public int team;
    public int col;
    public int row;
    public View view;
    public ImageView miniIcon;
    public RelativeLayout miniIconContainer;
    public Bomb bomb;

    public Player(MainActivity context, int _id, String _name, int _emoji, boolean _onLine, int _life, int _team) {
        main = context;
        id = _id;
        name = _name;
        emoji = _emoji;
        onLine = _onLine;
        life = _life;
        team = _team;
        bomb = new Bomb(main);

        colorMatrix = new ColorMatrix();
        colorMatrix.setSaturation(0);
        colorFilter = new ColorMatrixColorFilter(colorMatrix);

        LayoutInflater inflater = main.getLayoutInflater();
        view = inflater.inflate(R.layout.player, main.grid, false);
        ImageView ivPlayerIcon = (ImageView) view.findViewById(R.id.player_icon);
        TextView tvPlayerName = (TextView) view.findViewById(R.id.player_name);
        String emojiIcon = String.format("%03d", emoji+1);
        int idEmoji = main.getResources().getIdentifier("com.danival.game:drawable/emoji" + emojiIcon, null, null);
        tvPlayerName.setText(name);
        ivPlayerIcon.setImageResource(idEmoji);
        view.setVisibility(View.INVISIBLE);
        main.grid.addView(view);

/*
        int padding = Math.round(1*main.metrics.density);
        miniIcon = new ImageView(main);
        miniIcon.setImageResource(idEmoji);
        miniIcon.setPadding(padding, padding, padding, padding);
*/
        miniIconContainer = (RelativeLayout) inflater.inflate(R.layout.player_mini_icon, main.grid, false);
        miniIcon = (ImageView) miniIconContainer.findViewById(R.id.mini_icon);
        miniIcon.setImageResource(idEmoji);

        if (!onLine) {
            ivPlayerIcon.setColorFilter(colorFilter);
            miniIcon.setColorFilter(colorFilter);
        }

        TextView tvPlayerLife = (TextView) view.findViewById(R.id.player_life);
        if (team == 0) {
            tvPlayerLife.setBackgroundResource(R.drawable.life_a);
        } else {
            tvPlayerLife.setBackgroundResource(R.drawable.life_b);
        }

        lifeChanged(life);
    }

    public void setPosition(int _col, int _row) {
        col = _col;
        row = _row;
        view.setX(main.metrics.getX(col));
        view.setY(main.metrics.getY(row));
    }

    public void move(int colFrom, int rowFrom, int colTo, int rowTo, final boolean fromGroup, final boolean toGroup) {
        final float oldX = main.metrics.getX(colFrom);
        final float oldY = main.metrics.getY(rowFrom);
        final float newX = main.metrics.getX(colTo);
        final float newY = main.metrics.getY(rowTo);
        final int playerId = id;
        col = colTo;
        row = rowTo;
        view.setVisibility(View.VISIBLE);
        view.animate().x(newX).y(newY).setDuration(500).withEndAction(
                new Runnable() {
                    @Override
                    public void run() {
                        if (toGroup) {
                            view.setVisibility(View.INVISIBLE);
                        }
                        main.scrollToPlayer(playerId, newX);
                        main.gameMap.grid[row-1][col-1].update();
/*
                        if (main.mPlayerId == playerId) {
                            main.isMoving = false;
                        }
*/
                    }
                });
    }

    public void lifeChanged(int _life) {
        life = _life;
        TextView playerLife = (TextView) view.findViewById(R.id.player_life);
        playerLife.setText(life + "");
    }

    public void offline() {
        ImageView imgView = (ImageView) view.findViewById(R.id.player_icon);
        imgView.setColorFilter(colorFilter);
        miniIcon.setColorFilter(colorFilter);
    }

    public void online() {
        ImageView imgView = (ImageView) view.findViewById(R.id.player_icon);
        imgView.clearColorFilter();
        miniIcon.clearColorFilter();
    }

    public void die() {
        view.setVisibility(View.INVISIBLE);
    }

    public void clear() {
        main.grid.removeView(view);
        bomb.clear();
    }

}
