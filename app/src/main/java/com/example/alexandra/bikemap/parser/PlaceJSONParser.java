package com.example.alexandra.bikemap.parser;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collection;

/**
 * Парсит ответ списка мест для автоподсказок в формате Json в ArrayList<String>
 */
public class PlaceJSONParser {
    /**
     * Парсит список мест (ответ гугл) для автоподсказок в формате Json в ArrayList<String>
     *
     * @param jObject список в формате JSON
     * @return oписок в формате ArrayList<String>
     */
    public Collection<String> parse(JSONObject jObject) {

        JSONArray jPlaces = null;
        try {
            jPlaces = jObject.getJSONArray("predictions");
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return getPlaces(jPlaces);
    }

    private Collection<String> getPlaces(JSONArray jPlaces) {
        int placesCount = jPlaces.length();
        ArrayList<String> placesList = new ArrayList<>();
        String place;

        for (int i = 0; i < placesCount; i++) {
            try {
                place = getPlace((JSONObject) jPlaces.get(i));
                placesList.add(place);

            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        return placesList;
    }

    private String getPlace(JSONObject jPlace) {

        String place="";

        try {
            place = jPlace.getString("description");

        } catch (NullPointerException | ClassCastException | JSONException e) {
            e.printStackTrace();
        }
        return place;
    }
}
