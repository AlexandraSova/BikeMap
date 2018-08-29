package com.example.alexandra.bikemap.parser;


import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;
import android.util.Pair;

import com.google.android.gms.maps.model.LatLng;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutionException;

public class LocationNameToLatLngParser {

    private Context context;

    public LocationNameToLatLngParser(Context context) {
        this.context = context;
    }

    public LatLng parse(String locationName) {
        Geocoder geocoder = new Geocoder();
        return geocoder.getLatLng(locationName);
    }

    private class Geocoder {

        private String downloadUrl(String strUrl) throws IOException {
            String data = "";
            InputStream iStream = null;
            HttpURLConnection urlConnection = null;
            try {
                URL url = new URL(strUrl);
                //urlConnection.setInstanceFollowRedirects(false);
                urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection.connect();
                iStream = urlConnection.getInputStream();
                BufferedReader br = new BufferedReader(new InputStreamReader(iStream));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = br.readLine()) != null) {
                    sb.append(line);
                }
                data = sb.toString();
                br.close();

            } catch (Exception e) {
                Log.d("Exception url", e.toString());
            } finally {
                if (iStream != null) {
                    iStream.close();
                    urlConnection.disconnect();
                }
            }
            return data;
        }

        private String getUrl(String address, String key) {
            String str_origin = "https://maps.googleapis.com/maps/api/geocode/";
            String parameters = "address=" + address.replace(",", ",+").replace(" ", "");
            String output = "json";
            if (key != null) {
                String newKey = "key=" + key;
                parameters = str_origin + output + "?" + "&" + parameters + "&" + newKey;
            }
            return str_origin + output + "?" + parameters;
        }

        private LatLng getLatLng(String address) {
            String responseStatus;
            LatLng result = null;

            Pair<String, LatLng> response = getGeocodResponse(address, null);
            if (response != null) {
                responseStatus = response.first;
                result = response.second;

                int i = 2;
                while (responseStatus.toLowerCase().equals("OVER_QUERY_LIMIT".toLowerCase()) & i <= 10) {
                    String newKey = context.getResources().getString(getKeyId("key" + i));
                    response = getGeocodResponse(address, newKey);
                    responseStatus = response.first;
                    result = response.second;
                    i++;
                }

            }
            return result;
        }

        private Pair<String, LatLng> getGeocodResponse(String address, String key) {
            Geocod geocod = new Geocod();
            String url;
            Pair<String, LatLng> response = null;

            try {
                url = getUrl(address, key);
                geocod.execute(url);
                response = geocod.get();

            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (ExecutionException e) {
                e.printStackTrace();
            }

            return response;
        }

        private int getKeyId(String key) {
            return context.getResources().
                    getIdentifier(key, "string", context.getPackageName());
        }

        private class Geocod extends AsyncTask<String, Integer, Pair<String, LatLng>> {
            @Override
            protected Pair<String, LatLng> doInBackground(String... url) {
                GeocodParser parser = new GeocodParser();
                Pair<String, LatLng> result = null;
                LatLng latLng;
                String status;

                String jsonData = "";

                try {
                    jsonData = downloadUrl(url[0]);
                } catch (Exception e) {
                    Log.d("Background Task", e.toString());
                }

                JSONObject jObject;

                try {
                    jObject = new JSONObject(jsonData);
                    latLng = parser.parse(jObject);
                    status = jObject.getString("status");
                    result = new Pair<>(status, latLng);
                } catch (Exception e) {
                    e.printStackTrace();
                }

                return result;
            }
        }
    }

    private class GeocodParser {

        private LatLng parse(JSONObject jObject) {

            LatLng result = null;
            JSONArray jResult;
            JSONObject jGeometry;
            JSONObject jLocation;

            try {
                jResult = jObject.getJSONArray("results");
                jGeometry = jResult.getJSONObject(0).getJSONObject("geometry");
                jLocation = jGeometry.getJSONObject("location");
                result = new LatLng(jLocation.getDouble("lat"), jLocation.getDouble("lng"));
            } catch (JSONException e) {
                e.printStackTrace();
            } catch (Exception e) {
                e.printStackTrace();
            }

            return result;
        }
    }
}
