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

public class GameMap {
    protected MainActivity main;
    public Cell[][] grid;
    public List<Player> playerList;
    public List<Base> baseList;

    public GameMap(MainActivity context) {
        main = context;
        grid = new Cell[5][main.COLUMNS_COUNT];
        playerList = new ArrayList<Player>();
        baseList = new ArrayList<Base>();

        for(int row=0; row<5; row++)
            for(int col=0; col<main.COLUMNS_COUNT; col++) {
                grid[row][col] = new Cell(main, "group", -1, -1);
                grid[row][col].cell_icon.setX(main.metrics.getX(col+1));
                grid[row][col].cell_icon.setY(main.metrics.getY(row+1));
                main.grid.addView(grid[row][col].cell_icon);
                grid[row][col].cell_grid.setX(main.metrics.getX(col+1));
                grid[row][col].cell_grid.setY(main.metrics.getY(row+1));
                main.grid.addView(grid[row][col].cell_grid);
            }
    }

    public int findBase(int id) {
        for (int i = 0; i < baseList.size(); i++) {
            if (baseList.get(i).id == id)
                return i;
        }
        return -1;
    }

    public int findPlayer(int id) {
      for (int i = 0; i < playerList.size(); i++) {
          if (playerList.get(i).id == id)
                return i;
      }
      return -1;
    }

    public void removePlayer(int id) {
      playerList.remove(findPlayer(id));
    }

    public void newBase(int id, int col, int row, int team, int flag) {
        baseList.add(new Base(main, id, team, flag, col, row));
        grid[row-1][col-1].setGroupTeam(team);
    }

    public void newPlayer(int id, String name, int emoji, boolean onLine, int life, int team) {
        playerList.add(new Player(main, id, name, emoji, onLine, life, team));
    }

    public void playerLifeChanged(int id, int life) {
        playerList.get(findPlayer(id)).lifeChanged(life);
    }

    public void playerOffline(int id) {
        playerList.get(findPlayer(id)).offline();
    }

    public void playerOnline(int id) {
        playerList.get(findPlayer(id)).online();
    }

    public void playerMove(int id, int colFrom, int rowFrom, JSONObject cellFrom, int colTo, int rowTo, JSONObject cellTo, int flag) {
        boolean fromGroup = (grid[rowFrom-1][colFrom-1].type.equals("base")) || (grid[rowFrom-1][colFrom-1].members.size() > 1);
        updateCell(colFrom, rowFrom, cellFrom, flag, true);
        updateCell(colTo, rowTo, cellTo, flag, false); // não atualiza o view agora, só depois que a animação terminar
        boolean toGroup = (grid[rowTo-1][colTo-1].type.equals("base")) || (grid[rowTo-1][colTo-1].members.size() > 1);
        playerList.get(findPlayer(id)).move(colFrom, rowFrom, colTo, rowTo, fromGroup, toGroup);
    }

    public void playerDie(int id, int col, int row, JSONObject cell, int flag) {
        Player player = playerList.get(findPlayer(id));
        player.die();
        updateCell(col, row, cell, flag, true);
    }

    public void updateCell(int col, int row, JSONObject cell, int flag, boolean updateView) {
        try {
            if (cell.getString("type").equals("base")) {
                Base base = baseList.get(findBase(cell.getInt("id")));
                base.flag = flag;
            }
            grid[row-1][col-1].type = cell.getString("type");
            grid[row-1][col-1].id = cell.getInt("id");
            grid[row-1][col-1].team = cell.getInt("team");
            grid[row-1][col-1].members.clear();
            JSONArray memberList = (JSONArray) cell.getJSONArray("members");
            for (int i = 0; i < memberList.length(); i++) {
                JSONObject data = memberList.getJSONObject(i);
                int id = data.getInt("id");
                Player player = playerList.get(findPlayer(id));
                if (updateView) {
                    player.setPosition(col, row);
                }
                main.scrollToPlayer(id, main.metrics.getX(col));
                grid[row-1][col-1].members.add(id);
            }
            grid[row-1][col-1].sort();
            if (updateView) {
                grid[row-1][col-1].update();
            }
        } catch (JSONException e) { Log.e("game", Log.getStackTraceString(e)); }
    }

    public void baseChanged(int col, int row, int team) {
        grid[row-1][col-1].setGroupTeam(team);
    }


    public void attack(int id, int colFrom, int rowFrom, int colTo, int rowTo) {
        int xFrom = main.metrics.getX(colFrom);
        int yFrom = main.metrics.getY(rowFrom);
        int xTo = main.metrics.getX(colTo);
        int yTo = main.metrics.getY(rowTo);

        Player player = playerList.get(findPlayer(id));
        player.bomb.launch(xFrom, yFrom, xTo, yTo);
    }


/*
    public void removeMapItem(String type, int id) {
        switch (type) {
            case "player":
                main.grid.removeView(findPlayer(id));
                removePlayer(id);
                break;
            case "fence":
                main.grid.removeView(findFence(id));
                removeFence(id);
                break;
            case "pill":
                main.grid.removeView(findPill(id));
                removePill(id);
                break;
        }
    }
*/

    public void clear() {
        for ( Player player: playerList) {
            player.clear();
        }
        playerList.clear();
        baseList.clear();

        // limpa a tela
        for(int row=0; row<5; row++)
            for(int col=0; col<main.COLUMNS_COUNT; col++) {
                grid[row][col].icon.setVisibility(ImageView.INVISIBLE);
                grid[row][col].cell_grid.setVisibility(RelativeLayout.INVISIBLE);
            }

    }
}
