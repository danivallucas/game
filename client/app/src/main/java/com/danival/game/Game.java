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

import com.google.android.gms.maps.model.LatLng;

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
        int playerIndex = findPlayerIndex(id);
        return (playerIndex == -1) ? null : playerList.get(playerIndex);
    }


    public Player newPlayer(int id, String name, int emoji, boolean onLine, String status, LatLng position, int energy, double flagPoints, int energyToRestore) {
        Player player = getPlayer(id);
        if (player == null) {
            player = new Player(main, id, name, emoji, onLine, status, position, energy, flagPoints, energyToRestore);
            playerList.add(player);
        }
        return player;
    }

    public void removePlayer(int id) {
        int playerIndex = findPlayerIndex(id);
        if (playerIndex == -1) return;
        playerList.remove(playerIndex);
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
        int foodIndex = findFoodIndex(id);
        return (foodIndex == -1) ? null : foodList.get(foodIndex);
    }


    public Food newFood(int id, int type, LatLng position, int energy) {
        Food food = getFood(id);
        if (food == null) {
            food = new Food(main, id, type, position, energy);
            foodList.add(food);
        }
        return food;
    }

    public void removeFood(int id) {
        int foodIndex = findFoodIndex(id);
        if (foodIndex == -1) return;
        getFood(id).clear();
        foodList.remove(foodIndex);
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
        int bombIndex = findBombIndex(id);
        return (bombIndex == -1) ? null : bombList.get(bombIndex);
    }

    public Bomb newBomb(int id, int type, int player, LatLng position, int energy) {
        Bomb bomb = getBomb(id);
        if (bomb == null) {
            bomb = new Bomb(main, id, type, player, position, energy);
            bombList.add(bomb);
        }
        return bomb;
    }

    public void removeBomb(int id) {
        int bombIndex = findBombIndex(id);
        if (bombIndex == -1) return;
        getBomb(id).clear();
        bombList.remove(bombIndex);
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
        int energyBallIndex = findEnergyBallIndex(id);
        return (energyBallIndex == -1) ? null : energyBallList.get(energyBallIndex);
    }


    public EnergyBall newEnergyBall(int id, int type, LatLng position, int energy) {
        EnergyBall energyBall = getEnergyBall(id);
        if (energyBall == null) {
            energyBall = new EnergyBall(main, id, type, position, energy);
            energyBallList.add(energyBall);
        }
        return energyBall;
    }

    public void removeEnergyBall(int id) {
        int energyBallIndex = findEnergyBallIndex(id);
        if (energyBallIndex == -1) return;
        getEnergyBall(id).clear();
        energyBallList.remove(energyBallIndex);
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
        int flagIndex = findFlagIndex(id);
        return (flagIndex == -1) ? null : flagList.get(flagIndex);
    }


    public Flag newFlag(int id, String type, LatLng position, int energy, int wall, int playerId, double points) {
        Flag flag = getFlag(id);
        if (flag == null) {
            flag = new Flag(main, id, type, position, energy, wall, playerId, points);
            flagList.add(flag);
        }
        return flag;
    }

    public void removeFlag(int id) {
        int flagIndex = findFlagIndex(id);
        if (flagIndex == -1) return;
        getFlag(id).clear();
        flagList.remove(flagIndex);
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
                //sb.append(++count + ". Player " + playerList.get(i).id + " (" + playerList.get(i).energy + ")");
                //sb.append(++count + ". Player " + playerList.get(i).id);
                count++;
                if ( (count > 10) && (playerList.get(i).id != main.mPlayerId) ) continue;
                sb.append(count + ". " + playerList.get(i).name);
                if (playerList.get(i).flagPoints > 0)
                    sb.append(": " + main.format.format(Math.ceil(playerList.get(i).flagPoints)));
            }

        }
        if (sb.toString().equals("")) {
            //main.ranking.setVisibility(View.GONE);
        } else {
            main.ranking.setText(sb);
            //main.ranking.setVisibility(View.VISIBLE);
        }
        //main.checkPlayerListVisibility();
    }

}