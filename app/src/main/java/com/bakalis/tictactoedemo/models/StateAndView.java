package com.bakalis.tictactoedemo.models;

import android.widget.ImageView;


public class StateAndView {

//The instances of this class associate an ImageView to its State String.
private ImageView imageView;
//Can be an "X", and "O" or "blank".
private String state;


    //Simple Constructors.
    public StateAndView(){}

    public StateAndView(ImageView imageView, String state){
        this.imageView = imageView;
        this.state = state;
    }



    //Getters and Setters.
    public ImageView getImageView() {
        return imageView;
    }

    public void setImageView(ImageView imageView) {
        this.imageView = imageView;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

}
