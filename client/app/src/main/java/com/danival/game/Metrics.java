package com.danival.game;

import android.util.DisplayMetrics;

public class Metrics {
    protected MainActivity main;
    public float density; // densidade da tela (4 para o Galaxy S6)
    public int w; // width (1440 parra o Galaxy S6)
    public int h; // height (2560 parra o Galaxy S6)
    public int hSpace;
    public int hPadding;
    public int vPadding;
    public int vSpace;
    public int playerWidth;
    public int playerHeight;
    // Divisão da tela em 5 faixas
    protected int faixa1; // Barra de status
    protected int faixa2; // Barra de topo do game
    protected int faixa3; // Barra do meio (grid) -> é o que sobra
    protected int faixa4; // Barra de paginação do grid (radioButtons)
    protected int faixa5; // Barra de rodapé (botões)

    public Metrics(MainActivity context) {
        main = context;
        DisplayMetrics displayMetrics = main.getResources().getDisplayMetrics();
        density = displayMetrics.density;
        w = displayMetrics.widthPixels;
        h = displayMetrics.heightPixels;

    }

}
