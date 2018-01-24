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
import java.util.List;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class Game {
    protected MainActivity main;
    public List<Player> playerList;
    public List<Food> foodList;

    public Game(MainActivity context) {
        main = context;
        playerList = new ArrayList<Player>();
        foodList = new ArrayList<Food>();
    }

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


    public Food newFood(int id, double lat, double lng, long energy) {
        Food food = new Food(main, id, lat, lng, energy);
        foodList.add(food);
        return food;
    }

    public void removeFood(int id) {
        getFood(id).clear();
        foodList.remove(findFoodIndex(id));
    }

    public void clear() {
        playerList.clear();
    }
}