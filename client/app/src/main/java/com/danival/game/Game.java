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
    public List<Flag> flagList;

    public Game(MainActivity context) {
        main = context;
        playerList = new ArrayList<Player>();
        foodList = new ArrayList<Food>();
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


    public Player newPlayer(int id, String name, int emoji, boolean onLine, String status, double lat, double lng, long energy) {
        Player player = new Player(main, id, name, emoji, onLine, status, lat, lng, energy);
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


    public Food newFood(int id, int type, double lat, double lng, long energy) {
        Food food = new Food(main, id, type, lat, lng, energy);
        foodList.add(food);
        return food;
    }

    public void removeFood(int id) {
        getFood(id).clear();
        foodList.remove(findFoodIndex(id));
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


    public Flag newFlag(int id, String city, String country, long population, double lat, double lng, long wall, int playerId) {
        Flag flag = new Flag(main, id, city, country, population, lat, lng, wall, playerId);
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
        flagList.clear();
    }

    public void drawRanking() {
        Collections.sort(playerList, new Comparator<Player>() {
            @Override
            public int compare(Player player1, Player player2) {
                if ( (player1.energy + player1.flagPoints) > (player2.energy + player2.flagPoints) )
                    return  -1;
                return 1;
            }
        });
        // Atualiza o ranking
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < playerList.size(); i++) {
            if (!playerList.get(i).status.equals("out")) {
                if (!sb.toString().equals(""))
                    sb.append("\n");
                sb.append("Player " + playerList.get(i).id + ": " + (playerList.get(i).energy + playerList.get(i).flagPoints));
            }

        }
        main.status.setText(sb);
    }

}