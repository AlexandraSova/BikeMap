package com.example.alexandra.bikemap;

import android.content.Context;
import android.os.AsyncTask;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.ListView;
import android.widget.TextView;

import com.example.alexandra.bikemap.parser.PlaceJSONParser;
import com.example.alexandra.bikemap.urlBuilder.UrlBuilder;
import com.google.android.gms.maps.model.LatLng;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;

public class PlaceAutoComplete {
    private OnAutocompleteInteractionListener mListener;
    private UrlBuilder urlBuilder;
    private LatLng place = null;
    private AutoCompleteTextView autoCompleteTextView;
    private ArrayList<String> autocompleteValues = null;
    private ArrayAdapter<String> adapter;
    private ListView placesList;
    private Context context;

    /**
     * @param c        context
     * @param textView поле ввода, для которого выводить подсказки
     * @param l        listener
     * @param b        url builder для построения запросов для автоподсказок
     * @param list     ListView - элемент разметки для вывода подсказок
     */
    public void setPlaceAutoComplete(Context c, AutoCompleteTextView textView,
                                     OnAutocompleteInteractionListener l, UrlBuilder b, ListView list) {
        mListener = l;
        context = c;
        urlBuilder = b;
        placesList = list;
        autoCompleteTextView = textView;
        autoCompleteTextView.setThreshold(1);

        autoCompleteTextView.addTextChangedListener(new TextWatcher() {

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                placeAutoComplete(s.toString());
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count,
                                          int after) {
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        autoCompleteTextView.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (!hasFocus) {
                    if (place == null) {
                        ArrayList<String> places = autocompleteValues;
                        if (places != null) {
                            if (places.size() > 0) {
                                String locationName = places.get(0);
                                setPlaceValue(locationName);
                            }
                        } else {
                            autoCompleteTextView.setText("");
                        }
                    }
                }
            }
        });
    }

    private void placeAutoComplete(String s) {
        class PlaceTask extends AsyncTask<String, Void, String> {

            /**
             * отправляет запрос с началом ввода и получает ответ
             *
             * @param place начало ввода места
             * @return string - ответ (Json, нужно парсить)
             */
            @Override
            protected String doInBackground(String... place) {

                String url = urlBuilder.build(place[0]);

                String data = null;
                try {
                    data = downloadUrl(url);
                } catch (IOException e) {
                    Log.d("Background Task", e.toString());
                }
                return data;
            }

            /**
             * Парсит список мест (ответ гугл) для автоподсказок в формате Json в ArrayList<String>
             * Работает с результатами запроса:
             * 1) Выводит на экран список подсказок
             * 2) ждет, когда пользователь выберет нужное место
             * 3) вызывает внешний метод для установки значения места
             *
             * @param result string - JSON
             */
            @Override
            protected void onPostExecute(String result) {

                class ParserTask extends AsyncTask<String, Integer, ArrayList<String>> {
                    private JSONObject jObject;

                    /**
                     * Парсит список мест (ответ гугл) для автоподсказок в формате Json в ArrayList<String>
                     *
                     * @param jsonData список в формате Json
                     * @return список в формате ArrayList<String>
                     */
                    @Override
                    protected ArrayList<String> doInBackground(String... jsonData) {

                        ArrayList<String> places = null;
                        PlaceJSONParser placeJsonParser = new PlaceJSONParser();

                        try {
                            jObject = new JSONObject(jsonData[0]);
                            places = new ArrayList<>(placeJsonParser.parse(jObject));

                        } catch (JSONException e) {
                            Log.d("Exception", e.toString());
                        }
                        return places;
                    }

                    /**
                     * Работает с результатами запроса:
                     * 1) Выводит на экран список подсказок
                     * 2) ждет, когда пользователь выберет нужное место
                     * 3) вызывает внешний метод для установки значения места
                     *
                     * @param result список мест для подсказок
                     */
                    @Override
                    protected void onPostExecute(ArrayList<String> result) {
                        autocompleteValues = result;

                        //создание представлений для списка подсказок
                        adapter = new ArrayAdapter<String>(context,
                                android.R.layout.simple_list_item_1, autocompleteValues);
                        placesList.setAdapter(adapter);

                        placesList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                            @Override
                            public void onItemClick(AdapterView<?> parent, View v, int position, long id) {
                                String locationName = ((TextView) v).getText().toString();

                                //вызов внешнего метода
                                setPlaceValue(locationName);
                            }
                        });
                    }
                }

                super.onPostExecute(result);
                ParserTask parserTask = new ParserTask();
                parserTask.execute(result);
            }

        }

        PlaceTask placesTaskStart = new PlaceTask();
        placesTaskStart.execute(s);
    }

    private String downloadUrl(String strUrl) throws IOException {
        String data = "";
        InputStream iStream = null;
        HttpURLConnection urlConnection = null;
        try {
            URL url = new URL(strUrl);
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
            Log.d("downloading url", e.toString());
        } finally {
            if (iStream != null) {
                iStream.close();
                urlConnection.disconnect();
            }
        }
        return data;
    }

    private void placeListClear() {
        if (autocompleteValues != null) {
            autocompleteValues.clear();
            adapter.notifyDataSetChanged();
        }
    }

    private void setPlaceValue(String locationName) {
        autoCompleteTextView.setText(locationName);
        mListener.onAutocompleteInteraction(locationName);
        placeListClear();
    }

    public interface OnAutocompleteInteractionListener {
        void onAutocompleteInteraction(String locationName);
    }

}
