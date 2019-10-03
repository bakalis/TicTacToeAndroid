package com.bakalis.tictactoedemo;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bakalis.tictactoedemo.models.CellStates;
import com.bakalis.tictactoedemo.models.GameState;
import com.bakalis.tictactoedemo.models.StateAndView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

import ua.naiksoftware.stomp.Stomp;
import ua.naiksoftware.stomp.StompClient;
import ua.naiksoftware.stomp.dto.StompMessage;

public class MainActivity extends AppCompatActivity {



    //The GameState item that describes the Game's State at any point.
    private GameState gameState;
    //Informs the user when it is his turn to play.
    private boolean myTurn=true;
    //This is true when two users are connected and the game may begin.
    private boolean gameSet=false;
    //This is true when two other users are already playing a game and another user tries to connect.
    private boolean gamePlayed = false;
    //This is true when the initial communication with the server is finished and we received the initial GameState.
    private boolean variablesInitialized = false;
    //This is true when a win condition is satisfied or all moves are made without one.
    private boolean gameOver = false;
    //The url where the WebSocket Server is located and the WebSocket endpoint.
    private String url = "ws://192.168.1.4:8080/tictactoe/websocket";
    //The Button to Restart the Game when a game is finished.
    private Button rcBtn;
    //The StompClient object.
    private StompClient stompClient;





    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle("Tic Tac Toe Demo");
        setContentView(R.layout.activity_main);
        setUpImageViews();
        initiateConnection();
    }






    //Gets references for all the Views in the Activity. Sets up OnClickListeners.
    public void setUpImageViews(){

        ArrayList<ImageView> images = new ArrayList<ImageView>();
        //Creates the GameState object.
        gameState = new GameState();

        //Adds every ImageView on the Activity in an ArrayList.
        images.add((ImageView) findViewById(R.id.img1));
        images.add((ImageView) findViewById(R.id.img2));
        images.add((ImageView) findViewById(R.id.img3));
        images.add((ImageView) findViewById(R.id.img4));
        images.add((ImageView) findViewById(R.id.img5));
        images.add((ImageView) findViewById(R.id.img6));
        images.add((ImageView) findViewById(R.id.img7));
        images.add((ImageView) findViewById(R.id.img8));
        images.add((ImageView) findViewById(R.id.img9));

        rcBtn = (Button) findViewById(R.id.rcButton);

        //Calls the reset method to Reset the Activity when the rcBtn is pressed.
        rcBtn.setOnClickListener((View v) -> reset());

        //This onClickListener is called when an ImageView is pressed.
        View.OnClickListener gameOnClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //Does nothing if another game is played.
                if(gamePlayed){
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(getApplicationContext(), "A game is played right now!", Toast.LENGTH_LONG).show();
                        }
                    });
                    return;
                }
                //Does nothing if the initial server communication is not over yet.
                if(!variablesInitialized){
                    return;
                }
                //Does nothing if the game is already over.
                if(gameOver){
                    return;
                }
                //Does nothing but inform you with a Toast text when you are the only user connected. Two users are required for the game to begin.
                if(!gameSet){
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(getApplicationContext(), "Wait for another player to Connect!", Toast.LENGTH_LONG).show();
                        }
                    });
                    return;
                }
                ImageView img = (ImageView) v;
                StateAndView state = getStateAndViewByImageView(img);
                //When a user is trying to play out of turn, it just informs him that it's not his time to play and does nothing more.
                if(!myTurn){
                    //This happens only when blank cells are clicked. The user can click non-blank cells at any point.
                    if(state.getState().equals(CellStates.blank.getCellState())){
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(getApplicationContext(),"Wait for your turn!", Toast.LENGTH_LONG).show();
                            }
                        });
                    }
                    return;
                }
                //User makes a move only when he presses a blank cell on his turn.
                if(state.getState().equals(CellStates.blank.getCellState())){
                    //If the user's mark is an X or an O, updates the state accordingly.
                    if(gameState.getMark().equals(CellStates.X.getCellState())){
                        state.setState(CellStates.X.getCellState());
                    }else if(gameState.getMark().equals(CellStates.O.getCellState())){
                        state.setState(CellStates.O.getCellState());
                    }
                    //User has made his move so the boolean goes back to false.
                    myTurn = false;
                    //Sends the updated state to the /app/maingame application Destination.
                    stompClient.send("/app/maingame", prepareGameStateString()).subscribe();
                }

            }
        };

        //Initalizes the GameState object for every ImageView in the Activity.
        for(ImageView im : images){
            gameState.getGameState().add(new StateAndView(im,"blank"));
            im.setOnClickListener(gameOnClickListener);
        }
    }






    //Creates a JSON Object String which contains the gameState as String Array and the Mark.
    public String prepareGameStateString(){
        String s = "{\"gameState\":[";
        for (int i =0;i<gameState.getGameState().size()-1;i++){
            s+="\""+gameState.getGameState().get(i).getState()+"\",";
        }
        s+="\""+gameState.getGameState().get(gameState.getGameState().size()-1).getState()+"\"]";
        s+= ", \"mark\": \""+gameState.getMark()+"\"}";
        return s;
    }






    //Returns the StateAndView object that contains a certain ImageView.
    public StateAndView getStateAndViewByImageView(ImageView img){
        for(StateAndView state : gameState.getGameState()){
            if(state.getImageView().equals(img)){
                return state;
            }
        }
        return null;
    }







    //Connects to the Server and Sends the message to connect the user to the game.
    public void initiateConnection(){
        //Connects to the WebSocket Server.
        stompClient = Stomp.over(Stomp.ConnectionProvider.OKHTTP, url);
        stompClient.connect();
        //Callback functions for various lifecycle events.
        stompClient.lifecycle().subscribe(lifecycleEvent -> {
            switch (lifecycleEvent.getType()) {
                //Is called when the Stomp Connection is initiated.
                case OPENED:
                    Log.d("TAG", "Stomp connection opened");
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            rcBtn.setVisibility(View.INVISIBLE);
                        }
                    });
                    break;
                //Is called if an error occurs in the Stomp communication.
                case ERROR:
                    Log.d("TAG", "error : " + lifecycleEvent.getException());
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(getApplicationContext(), lifecycleEvent.getException().toString(), Toast.LENGTH_LONG).show();
                        }
                    });
                    break;
                //Is Called when the Stomp Connection is closed.
                case CLOSED:
                    Log.d("TAG", "Stomp connection closed");
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            rcBtn.setVisibility(View.VISIBLE);
                        }
                    });
                    break;
            }
        });

        //Subscribes the application to the initialize topic where the initial game data is sent.
        stompClient.topic("/user/queue/initialize").subscribe((StompMessage stompMessage) -> initializeGameVariables(stompMessage) , throwable -> Log.d("tag","Error! "+ throwable.getLocalizedMessage()));
        //Subscribes the application to the matchset topic where the information that two users are connected and the match may begin is sent.
        stompClient.topic("/topic/matchset").subscribe((StompMessage stompMessage) -> gameSet=true , throwable -> Log.d("tag","Error! "+ throwable.getLocalizedMessage()));
        //Sends a message to the /app/connect application destination, where the Server gets informed that the application wants to connect to a game.
        stompClient.send("/app/connect","Attempting to connect").subscribe();
    }







    //Handles the Message the Server Sends from the initialize topic.
    private void initializeGameVariables(StompMessage message) {
        JSONObject payload = null;
        try {
            payload = new JSONObject(message.getPayload());

            //If the mark the Server sent is "blank" then another game is played right now.
            if(payload.getString("mark").matches("blank")){
                //Booleans are set accordingly
                gamePlayed = true;
                variablesInitialized = false;
                //A Toast text is shown to inform the user that a game is currently being played.
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(getApplicationContext(), "A game is played right now!", Toast.LENGTH_LONG).show();
                    }
                });
                return;
            }
            //Enters the mark the Server sent to the application to the GameState. X is given the right to play first.
            if(payload.getString("mark").matches("X")){
                gameState.setMark("X");
                myTurn = true;
            }
            else if(payload.getString("mark").matches("O")){
                gameState.setMark("O");
                myTurn = false;
            }else{
                //If by accident something is wrong and another mark value is passed we want to do nothing more.
                return;
            }
            //The initial communication with the Server is done and the initial variables needed are all set.
            variablesInitialized = true;
            //Subscribes the application to the tictactoe topic, where the messages regarding the game are sent.
            stompClient.topic("/topic/tictactoe").subscribe((StompMessage stompMessage) -> handleMessages(stompMessage) , throwable -> Log.d("tag","Error! "+ throwable.getLocalizedMessage()));
            //Subscribes the application to the isgameover topic, where a message will be sent when the game is over.
            stompClient.topic("/topic/isgameover").subscribe((StompMessage stompMessage) -> handleGameOver(stompMessage) , throwable -> Log.d("tag","Error! "+ throwable.getLocalizedMessage()));

        } catch (JSONException e) {
            e.printStackTrace();
        }

    }






    //This method is called when a message arrives on the tictactoe topic.
    public void handleMessages(StompMessage stompMessage){

        try {
            //Gets the gameState Array of Strings with the current game's state, that the Server sends in JSON format.
            JSONObject jsonObject = new JSONObject(stompMessage.getPayload());
            JSONArray array = jsonObject.getJSONArray("gameState");
            //Updates the application's GameState object with the received values.
            for(int i=0;i<array.length();i++){
                String object = (String) array.get(i);
                gameState.getGameState().get(i).setState(object);
            }
            //The Server sends the Mark that made the last move along with the GameState.
            String mark = jsonObject.getString("mark");

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    //Updates the ImageViews according to the GameState object's new values.
                    for(int j=0;j<gameState.getGameState().size();j++){
                        //Blank Cells are not modified at this point.
                        if(!(gameState.getGameState().get(j).getState()).equals(CellStates.blank.getCellState())){
                            //Blank Cells' ImageViews had 0 alpha. to draw an image on them we set it to 1.
                            gameState.getGameState().get(j).getImageView().setAlpha(1f);
                            //If the Cell's new state is X, we draw an X image.
                            if((gameState.getGameState().get(j).getState()).equals(CellStates.X.getCellState())){
                                gameState.getGameState().get(j).getImageView().setImageResource(R.drawable.x);
                            //If the Cell's new state is O, we draw an O image.
                            }else if((gameState.getGameState().get(j).getState()).equals(CellStates.O.getCellState())) {
                                gameState.getGameState().get(j).getImageView().setImageResource(R.drawable.o);
                            }
                        }
                    }
                }
            });

            //The GameState mark holds the value of the user's initial mark, where the String mark we got from the message
            //holds the mark that made the last move. Thus, we use that to determine whose turn is next.
            if(gameState.getMark().equals(mark)){
                myTurn=false;
            }else{
                myTurn = true;
            }

        } catch (JSONException e) {
            e.printStackTrace();
        }

    }






    //This method is called when a message from the isgameover topic is received.
    private void handleGameOver(StompMessage stompMessage) {

        try {
            //The winner of the game is sent in JSON format.
            JSONObject object = new JSONObject(stompMessage.getPayload());
            String winner = object.getString("winner");
            //Creates a Toast text to inform the user who won, if someone did.
            if(winner.matches("X")) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(getApplicationContext(), "X Wins!", Toast.LENGTH_LONG).show();

                    }
                });
            }else if(winner.matches("O")) {

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(getApplicationContext(), "O Wins!", Toast.LENGTH_LONG).show();

                    }
                });
            }else if(winner.matches("blank")) {

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(getApplicationContext(), "Noone Wins! :/", Toast.LENGTH_LONG).show();

                    }
                });
            }
            //Since the game is over the gameOver boolean is set to true, so the user can't make any other moves.
            gameOver = true;
            //Resets the Layout for the ImageViews to go blank again and the Reset Button to be drawn.
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    setContentView(R.layout.activity_main);
                    setUpImageViews();
                }
            });

        } catch (JSONException e) {
            e.printStackTrace();
        }
    }






    //When the Reset Button is pressed we Recreate the Activity for a new game to be played.
    public void reset(){  this.recreate();  }






    //Gets called when the Activity is Destroyed. Disconnects the Stomp Client.
    @Override
    protected void onDestroy() {
        super.onDestroy();
        stompClient.disconnect();
    }

}
