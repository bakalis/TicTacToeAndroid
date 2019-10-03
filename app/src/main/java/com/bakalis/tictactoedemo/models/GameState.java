package com.bakalis.tictactoedemo.models;

import java.util.ArrayList;

public class GameState {

    //The ArrayList to contain the game's State. Every item in this ArrayList associates
    //an ImageView to a State String.
    private ArrayList<StateAndView> gameState;
    //The current User's mark
    private String mark;

    //Constructor. Only Initializes the ArrayList. Also gives a dummy mark.
    public GameState(){
        this.gameState = new ArrayList<StateAndView>();
        this.mark = "X";
    }

    //Getters and Setters.
    public ArrayList<StateAndView> getGameState() {
        return gameState;
    }

    public void setGameState(ArrayList<StateAndView> gameState) {
        this.gameState = gameState;
    }

    public String getMark() {
        return mark;
    }

    public void setMark(String mark) {
        this.mark = mark;
    }
}
