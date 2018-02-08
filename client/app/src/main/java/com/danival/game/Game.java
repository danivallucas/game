package com.danival.game;

import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class Game {
    protected MainActivity main;
    public List<Player> playerList;
    public List<Food> foodList;
    public List<Bomb> bombList;
    public List<EnergyBall> energyBallList;
    public List<Flag> flagList;

    public Game(MainActivity context) {
        main = context;
        playerList = new ArrayList<Player>();
        foodList = new ArrayList<Food>();
        bombList = new ArrayList<Bomb>();
        energyBallList = new ArrayList<EnergyBall>();
        flagList = new ArrayList<Flag>();
    }

    // *** Player ***

    public int findPlayerIndex(int id) {
        for (int i = 0; i < playerList.size(); i++) {
            if (playerList.get(i).id == id)
                return i;
        }
        return -1;
    }

    public Player getPlayer(int id) {
        return playerList.get(findPlayerIndex(id));
    }


    public Player newPlayer(int id, String name, int emoji, boolean onLine, String status, double lat, double lng, int energy, double flagPoints) {
        Player player = new Player(main, id, name, emoji, onLine, status, lat, lng, energy, flagPoints);
        playerList.add(player);
        return player;
    }

    public void removePlayer(int id) {
        playerList.remove(findPlayerIndex(id));
    }

    // *** Food ***

    public int findFoodIndex(int id) {
        for (int i = 0; i < foodList.size(); i++) {
            if (foodList.get(i).id == id)
                return i;
        }
        return -1;
    }

    public Food getFood(int id) {
        return foodList.get(findFoodIndex(id));
    }


    public Food newFood(int id, int type, double lat, double lng, int energy) {
        Food food = new Food(main, id, type, lat, lng, energy);
        foodList.add(food);
        return food;
    }

    public void removeFood(int id) {
        getFood(id).clear();
        foodList.remove(findFoodIndex(id));
    }

    // *** Bomb ***

    public int findBombIndex(int id) {
        for (int i = 0; i < bombList.size(); i++) {
            if (bombList.get(i).id == id)
                return i;
        }
        return -1;
    }

    public Bomb getBomb(int id) {
        return bombList.get(findBombIndex(id));
    }


    public Bomb newBomb(int id, int type, double lat, double lng, int energy) {
        Bomb bomb = new Bomb(main, id, type, lat, lng, energy);
        bombList.add(bomb);
        return bomb;
    }

    public void removeBomb(int id) {
        getBomb(id).clear();
        bombList.remove(findBombIndex(id));
    }

    // *** EnergyBall ***

    public int findEnergyBallIndex(int id) {
        for (int i = 0; i < energyBallList.size(); i++) {
            if (energyBallList.get(i).id == id)
                return i;
        }
        return -1;
    }

    public EnergyBall getEnergyBall(int id) {
        return energyBallList.get(findEnergyBallIndex(id));
    }


    public EnergyBall newEnergyBall(int id, int type, double lat, double lng, int energy) {
        EnergyBall energyBall = new EnergyBall(main, id, type, lat, lng, energy);
        energyBallList.add(energyBall);
        return energyBall;
    }

    public void removeEnergyBall(int id) {
        getEnergyBall(id).clear();
        energyBallList.remove(findEnergyBallIndex(id));
    }

    // *** Flag ***

    public int findFlagIndex(int id) {
        for (int i = 0; i < flagList.size(); i++) {
            if (flagList.get(i).id == id)
                return i;
        }
        return -1;
    }

    public Flag getFlag(int id) {
        return flagList.get(findFlagIndex(id));
    }


    public Flag newFlag(int id, String type, double lat, double lng, int energy, int wall, int playerId, double points) {
        Flag flag = new Flag(main, id, type, lat, lng, energy, wall, playerId, points);
        flagList.add(flag);
        return flag;
    }

    public void removeFlag(int id) {
        getFlag(id).clear();
        flagList.remove(findFlagIndex(id));
    }

    // *** Clear ***

    public void clear() {
        playerList.clear();
        foodList.clear();
        bombList.clear();
        energyBallList.clear();
        flagList.clear();
        main.mMap.clear();
    }

    public void drawRanking() {
        Collections.sort(playerList, new Comparator<Player>() {
            @Override
            public int compare(Player player1, Player player2) {
                if ( player1.flagPoints > player2.flagPoints ) {
                    return  -1;
                } else
                if ( player1.flagPoints < player2.flagPoints ) {
                    return  1;
                } else {
                    if ( player1.energy > player2.energy) {
                        return  -1;
                    } else {
                        return 1;
                    }
                }

            }
        });
        // Atualiza o ranking
        StringBuilder sb = new StringBuilder();
        int count = 0;
        for (int i = 0; i < playerList.size(); i++) {
            if (!playerList.get(i).status.equals("out")) {
                if (!sb.toString().equals(""))
                    sb.append("\n");
                sb.append(++count + ". Player" + playerList.get(i).id + " (" + playerList.get(i).energy + ")");
                if (playerList.get(i).flagPoints > 0)
                    sb.append(": " + main.format.format(Math.ceil(playerList.get(i).flagPoints)));
            }

        }
        if (sb.toString().equals("")) {
            main.ranking.setVisibility(View.GONE);
        } else {
            main.ranking.setText(sb);
            main.ranking.setVisibility(View.VISIBLE);
        }
    }

}