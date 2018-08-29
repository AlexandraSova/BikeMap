package com.example.alexandra.bikemap.urlBuilder;

import android.content.Context;

import com.example.alexandra.bikemap.R;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

public class UrlBuilderForCities extends UrlBuilder {

    private Context context;

    public UrlBuilderForCities(Context context)
    {
        this.context = context;
    }
    @Override
    public String build(String s) {
        String key = "key=" + context.getResources().getString(R.string.key2);
        String input = "";
        try {
            input = "input=" + URLEncoder.encode(s, "utf-8");
        } catch (UnsupportedEncodingException e1) {
            e1.printStackTrace();
        }
        String types = "types=(cities)";
        String sensor = "sensor=false";
        String parameters = input + "&" + types + "&" + sensor + "&" + key;
        String output = "json";
        return  "https://maps.googleapis.com/maps/api/place/autocomplete/" + output + "?" + parameters;
    }
}
