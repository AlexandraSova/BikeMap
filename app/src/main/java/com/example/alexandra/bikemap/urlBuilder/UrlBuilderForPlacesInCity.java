package com.example.alexandra.bikemap.urlBuilder;

import android.content.Context;

import com.example.alexandra.bikemap.CurrentCity;
import com.example.alexandra.bikemap.R;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

public class UrlBuilderForPlacesInCity extends UrlBuilder {

    private Context context;

    public UrlBuilderForPlacesInCity(Context context)
    {
        this.context = context;
    }

    @Override
    public String build(String s)
    {
        CurrentCity currentCity = new CurrentCity(context);
        String city = currentCity.getCity().first;

        //установка префикса для поиска подскозок в определенном городе
        if (city != null) {
            String[] prefix = city.split(",");
            s = prefix[0] + ", " + s;
        }

        //заполнение полей запроса
        String key = "key=" + context.getResources().getString(R.string.key1);
        String input = "";
        String sensor = "sensor=false";
        String output = "json";
        try {
            input = "input=" + URLEncoder.encode(s, "utf-8");
        } catch (UnsupportedEncodingException e1) {
            e1.printStackTrace();
        }
        String parameters = input + "&" + sensor + "&" + key;

        //составление полной строки запроса
        return  "https://maps.googleapis.com/maps/api/place/autocomplete/" + output + "?" + parameters;
    }
}
