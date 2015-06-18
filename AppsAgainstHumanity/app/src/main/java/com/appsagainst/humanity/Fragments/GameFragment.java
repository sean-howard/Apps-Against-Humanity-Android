package com.appsagainst.humanity.Fragments;

/**
 * Created by User on 09/05/2015.
 */

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.appsagainst.humanity.Events.AllCardsSubmitted;
import com.appsagainst.humanity.Events.GotInitialWhiteCardIDs;
import com.appsagainst.humanity.Events.SelectBlackCardPlayer;
import com.appsagainst.humanity.Events.SubmitWhiteCardToServer;
import com.appsagainst.humanity.Events.WinnerChosen;
import com.appsagainst.humanity.Global;
import com.appsagainst.humanity.Managers.DatabaseManager;
import com.appsagainst.humanity.POJO.BlackCard;
import com.appsagainst.humanity.POJO.WhiteCard;
import com.appsagainst.humanity.Protocol.Game;
import com.appsagainst.humanity.R;
import com.squareup.otto.Subscribe;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import butterknife.ButterKnife;
import butterknife.InjectView;

public class GameFragment extends Fragment {

    private final String TAG = "GameFragment";

    @InjectView(R.id.listView)
    ListView listView;

    @InjectView(R.id.blackText)
    TextView blackText;

    private ArrayAdapter<WhiteCard> whiteCardAdapter;

    ArrayList<String> al;
    Game game;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_game, container, false);
        ButterKnife.inject(this, view);
        return view;

    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        Global.getInstance().bus.register(this);

        Bundle b = getArguments();
        game = (Game)b.getSerializable("game");

        if(game.isHost){
            game.gameClient.selectBlackCardPlayer(game.players.get(game.currentPlayerNumber).uniqueID);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        ButterKnife.reset(this);
    }

    @Subscribe
    public void blackCardPlayerSelected(SelectBlackCardPlayer selectBlackCardPlayer){

        Log.d(TAG, "BLACK CARD PLAYER SELECTED");

        if(selectBlackCardPlayer.uniqueID.equals(Global.getInstance().uniqueID)){
            Log.d(TAG, "IS BLACK PLAYER");

            game.isBlackCardPlayer = true;

            if(game.currentWhiteCards.size() == 0){
                distributeInitialWhiteCards();
            } else {
                //Top up cards
            }

            if(whiteCardAdapter != null){
                whiteCardAdapter = new ArrayAdapter<WhiteCard>(getActivity(),android.R.layout.simple_list_item_1, android.R.id.text1, new ArrayList<WhiteCard>());
                listView.setAdapter(whiteCardAdapter);
            }

            getBlackCard();
        } else {
            game.isBlackCardPlayer = false;

            blackText.setText("");

            if(whiteCardAdapter != null){
                whiteCardAdapter.clear();
                whiteCardAdapter.notifyDataSetChanged();
            }

            displayWhiteCards();

        }
    }

    @Subscribe
    public void whiteCardSubmitted(SubmitWhiteCardToServer submitWhiteCardToServer){
        if(game.isBlackCardPlayer){
            game.submittedWhiteCards.put(submitWhiteCardToServer.uniqueID, DatabaseManager.getWhiteCardByID(getActivity(), submitWhiteCardToServer.whitecardID));

            if(game.submittedWhiteCards.size() == game.players.size()-1){
                game.gameClient.allCardsSubmitted();
            }
        }
    }

    @Subscribe
    public void allCardsSubmitted(AllCardsSubmitted allCardsSubmitted){
        if(game.isBlackCardPlayer){
            List<WhiteCard> cards = new ArrayList<>(game.submittedWhiteCards.values());

            whiteCardAdapter = new ArrayAdapter<WhiteCard>(getActivity(),android.R.layout.simple_list_item_1, android.R.id.text1, cards);
            listView.setAdapter(whiteCardAdapter);
            listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    game.gameClient.chooseWinner(getKeyByValue(game.submittedWhiteCards, whiteCardAdapter.getItem(position)), whiteCardAdapter.getItem(position).getId());
                }
            });
        } else {

        }
    }

    public <String, WhiteCard> String getKeyByValue(Map<String, WhiteCard> map, WhiteCard value) {
        for (Map.Entry<String, WhiteCard> entry : map.entrySet()) {
            if (Objects.equals(value, entry.getValue())) {
                return entry.getKey();
            }
        }
        return null;
    }

    @Subscribe
    public void winnerChosen(WinnerChosen winnerChosen){
        if(game.isBlackCardPlayer){
            game.currentPlayerNumber++;

            if(game.currentPlayerNumber >= game.players.size()){
                game.currentPlayerNumber = 0;
            }

            game.gameClient.selectBlackCardPlayer(game.players.get(game.currentPlayerNumber).uniqueID);
        } else {
            if(winnerChosen.uniqueID.equals(Global.getInstance().uniqueID)){
                Toast.makeText(getActivity(), "You WON", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(getActivity(), "You LOST", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Subscribe
    public void gotInitialWhiteCards(GotInitialWhiteCardIDs gotInitialWhiteCardIDs){
        Log.d(TAG, "GOT INITIAL WHITE CARDS");
        for(int i = 0; i<gotInitialWhiteCardIDs.cardIDs.size(); i++){
            game.currentWhiteCards.add(DatabaseManager.getWhiteCardByID(getActivity(), gotInitialWhiteCardIDs.cardIDs.get(i)));
        }

        if(!game.isBlackCardPlayer){
            displayWhiteCards();
        }
    }

    public void getBlackCard(){
        Log.d(TAG, "GETTING BLACK CARD");

        DatabaseManager.getRandomBlackCard(getActivity());
        displayBlackCard(DatabaseManager.getRandomBlackCard(getActivity()));
    }

    public void distributeInitialWhiteCards(){
        try {
            int numberOfPlayers = game.players.size();

            HashMap<String, ArrayList<Integer>> cardIDs = new HashMap<>();

            for (int i = 0; i < numberOfPlayers; i++) {
                cardIDs.put(game.players.get(i).uniqueID, DatabaseManager.getSetOfRandomWhiteCardsIDs(getActivity(), 5));
            }

            game.gameClient.distributeWhiteCards(cardIDs);
        } catch (Exception e){
            e.printStackTrace();
        }

    }

    public void displayBlackCard(BlackCard blackCard){
        blackText.setText(blackCard.getText());
    }

    public void displayWhiteCards(){
        whiteCardAdapter = new ArrayAdapter<WhiteCard>(getActivity(),android.R.layout.simple_list_item_1, android.R.id.text1, game.currentWhiteCards);
        listView.setAdapter(whiteCardAdapter);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                game.gameClient.selectCard(whiteCardAdapter.getItem(position).getId());

                game.currentWhiteCards.remove(position);
                whiteCardAdapter.notifyDataSetChanged();
            }
        });
    }
}