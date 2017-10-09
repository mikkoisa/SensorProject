package com.example.mikko.sensorproject.autocomplete;

import android.app.Activity;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.example.mikko.R;
import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.List;


public class Autocomplete {

    private ListView suggestionList;
    private ArrayAdapter<String> adapter;
    private List<String> suggestionListItems;

    //instance of predictions that holds EVERYTHING from gson
    private Predictions predictions;
    
    //saves suggestion json to re-enable same suggestions after orientation change (state saving)
    public String autocompleteJson;
    private Activity a;


    public Autocomplete(Activity a) {

        this.a = a;
        suggestionListItems = new ArrayList<>();
        suggestionList = (ListView) a.findViewById(R.id.suggestions);
        // suggestionList = (ListView) getSupportActionBar().getCustomView().findViewById(R.id.suggestions);
        adapter = new ArrayAdapter<>(a, android.R.layout.simple_list_item_1, suggestionListItems);
        suggestionList.setAdapter(adapter);
    }
public void clearList() {
    suggestionListItems.clear();
}
    public void setVisibility(int i) {
        suggestionList.setVisibility(i);
    }
    public int getVisibility() {
        return suggestionList.getVisibility();

    }

    public int getSuggestionsAmount() {
        return suggestionListItems.size();
    }
public String getSuggestion(int id) {
    return suggestionListItems.get(id);
}

public void emptyList(){
    suggestionListItems.clear();
}
    public String trimQueryOnClick(int position) {
        //when suggestion is clicked, a trimmed query will be submitted to api to make it work better.
        //it can remove commas and country from the query
        if (PreferenceManager.getDefaultSharedPreferences(a).getBoolean("autocomplete", true)) {
            if (predictions == null) {
                Gson gson = new Gson();
                predictions = gson.fromJson(autocompleteJson, Predictions.class);
            }

            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < predictions.getPredictions().get(position).getTerms().size() - 1; i++) {
                sb.append(predictions.getPredictions().get(position).getTerms().get(i).getValue());
                if (i < predictions.getPredictions().get(position).getTerms().size() - 2) {
                    sb.append(" ");
                }
            }

            Log.i("inio", sb.toString());
            return sb.toString();
        }
        return null;
    }

    public ListView getSuggestionList() {
        return suggestionList;
    }
    public List getSuggestionListItems() {
        return suggestionListItems;
    }

    public void addToList(String sug) {
        suggestionListItems.add(sug);
    }

    //populates list with new suggestions from api
    //gson is used to transform json in pojos
    public void populateList(String json) {
        if (PreferenceManager.getDefaultSharedPreferences(a).getBoolean("autocomplete", true)) {


            Gson gson = new Gson();
            predictions = gson.fromJson(json , Predictions.class);
            autocompleteJson = json;
            suggestionListItems.clear();
            for (int i = 0; i < predictions.getPredictions().size(); i++) {
                suggestionListItems.add(predictions.getPredictions().get(i).getDescription());
            }
            adapter.notifyDataSetChanged();
        }
    }
}
