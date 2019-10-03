package com.bakalis.tictactoedemo.models;


//This enumeration describes the Possible States that a Cell (block) can have.
public enum CellStates {
    blank("blank"), X("X"), O("O");

    private String cellState;

    public String getCellState(){
        return this.cellState;
    }

    private CellStates(String cellState){
        this.cellState = cellState;
    }

}
