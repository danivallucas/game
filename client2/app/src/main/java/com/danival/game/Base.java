package com.danival.game;

public class Base {
  protected MainActivity main;
  public int id;
  public int team;
  public int flag;
  public int col;
  public int row;

  public Base(MainActivity context, int _id, int _team, int _flag, int _col, int _row) {
    main = context;
    id = _id;
    team = _team;
    flag = _flag;
    col = _col;
    row = _row;
  }

}
