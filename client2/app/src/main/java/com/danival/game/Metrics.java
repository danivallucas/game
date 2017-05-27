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

        playerWidth = (int) main.getResources().getDimension(R.dimen.player_icon_width);
        //int playerLifeHeight = Math.round(8*density);
        //int playerLifeMargin = Math.round(2*density);
        int playerMarginTop = (int) main.getResources().getDimension(R.dimen.badge_margin); // espaço para o badge
        int playerIconHeight = (int) main.getResources().getDimension(R.dimen.player_icon_height);
        int playerNameHeight = (int) main.getResources().getDimension(R.dimen.player_name_height);
        playerHeight = playerMarginTop + playerIconHeight + playerNameHeight;

        faixa1 = Math.round(25*density); // status do cel
        faixa2 = Math.round(36*density); // score
        faixa4 = Math.round(30*density); // paginação
        faixa5 = (int) main.getResources().getDimension(R.dimen.controls_container); // Barra de controle (botões de atacar, mover, etc)
        faixa3 = h - (faixa1 + faixa2  + faixa4 + faixa5); // grid

        hSpace = (w - 4*playerWidth)/ 4; // espaço que sobra divide por 4 (3 no meio e 1 dividido em 2 nos cantos)
        hPadding = hSpace/2; // cantos
        vSpace = (faixa3 - 5*playerHeight)/ 4;
    }

    public int getTopFaixa(int faixa) {
        int top = 0;
        switch (faixa) {
            case 1:
                top = 0;
                break;
            case 2:
                top = faixa1;
                break;
            case 3:
                top = faixa1 + faixa2;
                break;
            case 4:
                top = faixa1 + faixa2 + faixa3;
                break;
            case 5:
                top = faixa1 + faixa2 + faixa3 +faixa4;
                break;
        }
        return top;
    }

    public int getGridWidth() {
        return Math.round(w*main.PAGES_COUNT);
    }

    public int getGridHeight() {
        return faixa3;
    }

/*
    public int getGridBgX(int i) {
        return hPadding + ((playerWidth + hSpace) * (i%main.COLUMNS_COUNT));
    }

    public int getGridBgY(int i) {
        return ((playerHeight + vSpace) * (i/main.COLUMNS_COUNT));
    }

    public int getGridBgN(int x, int y) {
        return (y-1)*main.COLUMNS_COUNT + (x-1); // posição no array gridBG
    }
*/

    public int getCol(int x) {
        return (x / (playerWidth + hSpace)) + 1;
    }

    public int getRow(int y) {
        return ( (y + Math.round(vSpace/2)) / (playerHeight + vSpace) ) + 1;
    }

    public int getX(int col) {
        return hPadding + ((playerWidth + hSpace) * (col-1));
    }

    public int getY(int row) {
        return ((playerHeight + vSpace) * (row-1));
    }

    // Scroll máximo em que o player continue visível na tela
    public int getMaxScrollPlayerVisible(int x) {
        return x - hPadding - (playerWidth + hSpace);
    }

    // Scroll mínimo em que o player continue visível na tela
    public int getMinScrollPlayerVisible(int x) {
        return x - hPadding - 2*(playerWidth + hSpace);
    }

    public int getSizeIcon4x4() {
        int gridMargin = (int) main.getResources().getDimension(R.dimen.cell_grid_margin);
        return Math.round( (playerWidth-(2*gridMargin)) / 2);
    }

    public int getSizeIcon9x9() {
        int gridMargin = (int) main.getResources().getDimension(R.dimen.cell_grid_margin);
        return Math.round( (playerWidth-(2*gridMargin)) / 3);
    }

}
