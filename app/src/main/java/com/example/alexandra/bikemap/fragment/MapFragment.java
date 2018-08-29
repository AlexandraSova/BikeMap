package com.example.alexandra.bikemap.fragment;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.TextView;
import android.widget.Toast;

import com.example.alexandra.bikemap.CurrentCity;
import com.example.alexandra.bikemap.MainActivity;
import com.example.alexandra.bikemap.R;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.UiSettings;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;
import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.GridLabelRenderer;
import com.jjoe64.graphview.series.BarGraphSeries;
import com.jjoe64.graphview.series.DataPoint;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

public class MapFragment extends Fragment implements
        OnMapReadyCallback {

    public static final int MY_PERMISSIONS_REQUEST_LOCATION = 100;
    FusedLocationProviderClient mFusedLocationClient;
    LocationCallback mLocationCallback;
    LocationRequest mLocationRequest;
    Location mLastLocation;
    private GraphView graphView;
    private Context context;
    private GoogleMap mMap;
    private LatLng origin = null;
    private LatLng dest = null;
    private View view;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Bundle args = getArguments();
        if (args != null) {
            Bundle originBundle = args.getBundle("origin");
            Bundle destBundle = args.getBundle("dest");

            if (originBundle != null & destBundle != null) {
                origin = new LatLng(originBundle.getDouble("lat"),
                        originBundle.getDouble("lng"));
                dest = new LatLng(destBundle.getDouble("lat"),
                        destBundle.getDouble("lng"));
            }
        }

        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(context);
        mLocationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                for (Location location : locationResult.getLocations()) {
                    mLastLocation = location;
                    LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 11));
                }
            }
        };
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.fragment_map, container, false);
        context = getActivity();
        graphView = view.findViewById(R.id.graph);
        SupportMapFragment mapFragment = (SupportMapFragment) this.getChildFragmentManager()
                .findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }
        return view;
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        //инициализация карты
        mMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
        UiSettings uiSettings = mMap.getUiSettings();
        uiSettings.setCompassEnabled(true);
        uiSettings.setZoomControlsEnabled(true);

        //сделать активной ту часть карты, которая находится под тулбаром
        DisplayMetrics displayMetrics = this.getResources().getDisplayMetrics();
        int height = displayMetrics.heightPixels;
        int size = (int) (height * 0.1);
        mMap.setPadding(0, size, 0, 0);

        if (origin == null & dest == null) {
            setDefaultLocation();
        } else {
            RouteBuilder routeBuilder = new RouteBuilder(mMap, context);
            routeBuilder.getRoute();
        }

        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(120000); // две минуты
        mLocationRequest.setFastestInterval(120000);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);


        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(context,
                    Manifest.permission.ACCESS_FINE_LOCATION)
                    == PackageManager.PERMISSION_GRANTED) {
                //разрешение уже дано
                mFusedLocationClient.requestLocationUpdates(mLocationRequest, mLocationCallback, Looper.myLooper());
                mMap.setMyLocationEnabled(true);
            } else {
                //запрос разрешения на определение местоположения
                checkLocationPermission();
            }
        } else {
            mFusedLocationClient.requestLocationUpdates(mLocationRequest, mLocationCallback, Looper.myLooper());
            mMap.setMyLocationEnabled(true);
        }

        mMap.setOnMyLocationButtonClickListener(new GoogleMap.OnMyLocationButtonClickListener() {
            @Override
            public boolean onMyLocationButtonClick() {
                if (!isEnabledGPS(context)) {
                    showGPSOnDialog();
                }
                return false;
            }
        });
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        this.context = getActivity();
    }

    @Override
    public void onPause() {
        super.onPause();
        //прекратить обновление местоположения, когда активность больше не активна
        if (mFusedLocationClient != null) {
            mFusedLocationClient.removeLocationUpdates(mLocationCallback);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String permissions[], @NonNull int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_LOCATION: {
                // Если запрос не отклонен и массив результатов не пуст
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    // Разрешение дано
                    if (ContextCompat.checkSelfPermission(context,
                            Manifest.permission.ACCESS_FINE_LOCATION)
                            == PackageManager.PERMISSION_GRANTED) {
                        mFusedLocationClient.requestLocationUpdates(mLocationRequest, mLocationCallback, Looper.myLooper());
                        mMap.setMyLocationEnabled(true);
                    }

                } else {
                    // разрешение не дано
                    Toast.makeText(context, "Доступ запрещен", Toast.LENGTH_LONG).show();
                }
            }
            // другие 'case' для других разрешений
        }

    }

    private void checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(context,
                Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {

            // Нужно ли показывать обоснование запроса разрешения
            if (ActivityCompat.shouldShowRequestPermissionRationale((MainActivity) context,
                    Manifest.permission.ACCESS_FINE_LOCATION)) {

                //Показать обоснования для запроса разрешения
                new AlertDialog.Builder(context)
                        .setTitle("Включите определение местоположения")
                        .setMessage("Приложению BikeMap необходимо разрешение на определение местоположения, " +
                                "чтобы вы имели доступ ко всем его возможностям.")
                        .setPositiveButton("ОК", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                //Запросить разрешение после утвердительного ответа пользователя
                                ActivityCompat.requestPermissions((MainActivity) context,
                                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                                        MY_PERMISSIONS_REQUEST_LOCATION);
                            }
                        })
                        .create()
                        .show();


            } else {
                // Обоснований не нужно, можно просто запросить разрешение
                ActivityCompat.requestPermissions((MainActivity) context,
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                        MY_PERMISSIONS_REQUEST_LOCATION);
            }
        }
    }

    private boolean isEnabledGPS(Context context) {
        boolean result = false;
        LocationManager locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        if (locationManager != null) {
            result = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        }
        return result;
    }

    private void showGPSOnDialog() {
        new AlertDialog.Builder(context)
                .setTitle("GPS отключен")
                .setMessage("Чтобы определить местоположение, включите GPS в настройках.")
                .setPositiveButton("НАСТРОЙКИ", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        startActivity(new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS));
                    }
                })
                .create()
                .show();
    }

    private int getKeyId(String key) {
        return getResources().
                getIdentifier(key, "string", context.getPackageName());
    }

    private void setDefaultLocation() {
        CurrentCity provider = new CurrentCity(context);
        LatLng city = provider.getCity().second;
        LatLng defaultLocation;

        if (city != null) {
            defaultLocation = new LatLng(city.latitude, city.longitude);
        } else {
            defaultLocation = new LatLng(55.7872884, 49.1218577);
        }
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(defaultLocation, 13));
    }

    private void createGraphView(Collection<Double> elevations) {
        setHelpText("");
        DataPoint[] points = new DataPoint[elevations.size()];
        int i = 0;
        for (double e : elevations) {
            points[i] = new DataPoint(i, e);
            i++;
        }
        BarGraphSeries<DataPoint> series = new BarGraphSeries<>(points);
        series.setColor(Color.parseColor("#008080"));

        graphView.setVisibility(View.VISIBLE);

        GridLabelRenderer gridLabelRenderer = graphView.getGridLabelRenderer();

        gridLabelRenderer.setHorizontalLabelsVisible(false);
        gridLabelRenderer.setVerticalAxisTitle(" ");
        gridLabelRenderer.setLabelVerticalWidth(0);

        graphView.addSeries(series);
    }

    private void showMessage(String text) {
        Toast toast = Toast.makeText(context.getApplicationContext(),
                text, Toast.LENGTH_SHORT);
        toast.show();
    }

    private void setHelpText(String str) {
        TextView helpText = view.findViewById(R.id.help_text);
        helpText.setText(str);
        if (!str.equals("")) {
            Animation anim = AnimationUtils.loadAnimation(context, R.anim.help_text_animation);
            anim.setRepeatMode(Animation.REVERSE);
            anim.setRepeatCount(Animation.INFINITE);
            helpText.startAnimation(anim);
        }
    }

    private class RouteBuilder {
        private GoogleMap mMap;
        private Context context;

        private RouteBuilder(GoogleMap map, Context context) {
            this.mMap = map;
            this.context = context;
        }

        private String getDirectionsRouteUrl(LatLng origin, LatLng dest, String newKey) {

            String str_origin = "origin=" + origin.latitude + "," + origin.longitude;
            String str_dest = "destination=" + dest.latitude + "," + dest.longitude;
            String sensor = "sensor=false";
            String mode = "mode=walking";
            String parameters = str_origin + "&" + str_dest + "&" + sensor + "&" + mode;
            if (newKey != null) {
                String key = "key=" + newKey;
                parameters = str_origin + "&" + str_dest + "&" + sensor + "&" + mode + "&" + key;
            }
            String output = "json";
            return "https://maps.googleapis.com/maps/api/directions/" + output + "?" + parameters;
        }

        private String downloadRouteUrl(String strUrl) throws IOException {
            String data = "";
            HttpURLConnection urlConnection;
            URL url = new URL(strUrl);
            urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.connect();
            try (InputStream iStream = urlConnection.getInputStream()) {
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
                urlConnection.disconnect();
            }
            return data;
        }

        private void drawRoute(List<HashMap<String, String>> routePath) {
            PolylineOptions lineOptions;
            ArrayList<LatLng> routePoints;

            setHelpText("");
            routePoints = new ArrayList<>();
            for (int i = 0; i < routePath.size(); i++) {
                HashMap<String, String> point = routePath.get(i);
                double lat = Double.parseDouble(point.get("lat"));
                double lng = Double.parseDouble(point.get("lng"));
                LatLng position = new LatLng(lat, lng);
                routePoints.add(position);
            }

            lineOptions = new PolylineOptions();
            lineOptions.width(4f).color(Color.GRAY);
            lineOptions.addAll(routePoints);

            LatLngBounds.Builder latLngBuilder = new LatLngBounds.Builder();
            for (int i = 0; i < routePoints.size(); i++) {
                if (i == 0) {
                    MarkerOptions startMarkerOptions = new MarkerOptions()
                            .position(routePoints.get(i)).flat(true)
                            .icon(BitmapDescriptorFactory.fromResource(R.drawable.ma));
                    mMap.addMarker(startMarkerOptions);
                } else if (i == routePoints.size() - 1) {
                    MarkerOptions endMarkerOptions = new MarkerOptions()
                            .position(routePoints.get(i)).flat(true)
                            .icon(BitmapDescriptorFactory.fromResource(R.drawable.mb));
                    mMap.addMarker(endMarkerOptions);
                }
                latLngBuilder.include(routePoints.get(i));
            }
            mMap.addPolyline(lineOptions);
            int size = (int) (context.getResources().getDisplayMetrics().widthPixels * 0.9);
            LatLngBounds latLngBounds = latLngBuilder.build();
            CameraUpdate track = CameraUpdateFactory.newLatLngBounds(latLngBounds, size, size, 25);
            mMap.moveCamera(track);
        }

        private void getRoute() {
            setHelpText("Строим маршрут...");
            String url = getDirectionsRouteUrl(origin, dest, null);
            Route route = new Route();
            route.execute(url);
        }

        private class Route extends AsyncTask<String, Integer, Pair<List<HashMap<String, String>>, String>> {
            @Override
            protected Pair<List<HashMap<String, String>>, String> doInBackground(String... url) {

                String jsonData = "";
                String status = "";

                try {
                    jsonData = downloadRouteUrl(url[0]);
                } catch (Exception e) {
                    Log.d("Background Task", e.toString());
                }

                JSONObject jObject;
                List<HashMap<String, String>> route = null;

                if (!jsonData.equals("")) {
                    try {
                        jObject = new JSONObject(jsonData);
                        status = jObject.getString("status");
                        DirectionsJSONParser parser = new DirectionsJSONParser();
                        route = parser.parse(jObject);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                return new Pair<>(route, status);
            }

            @Override
            protected void onPostExecute(Pair<List<HashMap<String, String>>, String> result) {
                List<HashMap<String, String>> routePath = result.first;
                String responseStatus = result.second;

                if (!responseStatus.toLowerCase().equals("OVER_QUERY_LIMIT".toLowerCase())) {
                    if (responseStatus.toLowerCase().equals("OK".toLowerCase())) {
                        drawRoute(routePath);
                        ElevationsBuilder elevations = new ElevationsBuilder(context);
                        elevations.showElevations();
                    } else if (responseStatus.toLowerCase().equals("OVER_QUERY_LIMIT".toLowerCase())) {
                        setDefaultLocation();
                        showMessage("Слишком много запросов (маршрут).\nПопробуйте снова.");
                    } else if (responseStatus.toLowerCase().equals("NOT_FOUND".toLowerCase())) {
                        setDefaultLocation();
                        showMessage("Не удалось найти место.\nПопробуйте снова.");
                    } else if (responseStatus.toLowerCase().equals("ZERO_RESULTS".toLowerCase())) {
                        setDefaultLocation();
                        showMessage("Не удалось проложить маршрут.\nПопробуйте снова.");
                    } else if (responseStatus.toLowerCase().equals("REQUEST_DENIED".toLowerCase())) {
                        setDefaultLocation();
                        showMessage("Отказано в доступе.\nПопробуйте снова.");
                    } else if (responseStatus.toLowerCase().equals("UNKNOWN_ERROR".toLowerCase())) {
                        setDefaultLocation();
                        showMessage("Неизвестная ошибка.\nПопробуйте снова.");
                    } else {
                        showMessage("Неизвестная ошибка.\nПопробуйте снова.");
                        setDefaultLocation();
                    }
                } else {
                    int i = 2;
                    while (responseStatus.toLowerCase().equals("OVER_QUERY_LIMIT".toLowerCase()) & i <= 10) {
                        String key = context.getResources().getString(getKeyId("key" + i));
                        String url = getDirectionsRouteUrl(origin, dest, key);
                        Route route = new Route();
                        route.execute(url);
                        i++;
                    }
                }
            }
        }

        private class DirectionsJSONParser {

            public List<HashMap<String, String>> parse(JSONObject jObject) {

                List<HashMap<String, String>> route = null;
                JSONArray jRoutes;
                JSONArray jLegs;
                JSONArray jSteps;

                try {
                    jRoutes = jObject.getJSONArray("routes");

                    jLegs = ((JSONObject) jRoutes.get(0)).getJSONArray("legs");
                    route = new ArrayList<>();

                    for (int j = 0; j < jLegs.length(); j++) {
                        jSteps = ((JSONObject) jLegs.get(j)).getJSONArray("steps");

                        for (int k = 0; k < jSteps.length(); k++) {
                            String polyline;
                            polyline = (String) ((JSONObject) ((JSONObject) jSteps.get(k)).get("polyline")).get("points");
                            List<LatLng> list = decodePoly(polyline);

                            for (int l = 0; l < list.size(); l++) {
                                HashMap<String, String> hm = new HashMap<>();
                                hm.put("lat", Double.toString((list.get(l)).latitude));
                                hm.put("lng", Double.toString((list.get(l)).longitude));
                                route.add(hm);
                            }
                        }
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                }

                return route;
            }

            private List<LatLng> decodePoly(String encoded) {

                ArrayList<LatLng> polyline = new ArrayList<>();
                int index = 0, len = encoded.length();
                int lat = 0, lng = 0;

                while (index < len) {
                    int b, shift = 0, result = 0;
                    do {
                        b = encoded.charAt(index++) - 63;
                        result |= (b & 0x1f) << shift;
                        shift += 5;
                    } while (b >= 0x20);
                    int dlat = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
                    lat += dlat;

                    shift = 0;
                    result = 0;
                    do {
                        b = encoded.charAt(index++) - 63;
                        result |= (b & 0x1f) << shift;
                        shift += 5;
                    } while (b >= 0x20);
                    int dlng = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
                    lng += dlng;

                    LatLng item = new LatLng((((double) lat / 1E5)),
                            (((double) lng / 1E5)));
                    polyline.add(item);
                }

                return polyline;
            }
        }
    }

    private class ElevationsBuilder {

        private Context context;

        private ElevationsBuilder(Context context) {
            this.context = context;
        }

        private String getDirectionsElevationUrl(LatLng origin, LatLng dest, String key) {
            String path = "path=" +
                    origin.latitude + "," + origin.longitude + "|" +
                    dest.latitude + "," + dest.longitude;
            String samples = "samples=256";
            String parameters = path + "&" + samples;
            if (key != null) {
                String newKey = "key=" + key;
                parameters = path + "&" + samples + "&" + newKey;
            }
            String output = "json";
            return "https://maps.googleapis.com/maps/api/elevation/" + output + "?" + parameters;
        }

        private String downloadElevationUrl(String strUrl) throws IOException {
            String data = "";
            HttpURLConnection urlConnection;
            URL url = new URL(strUrl);
            urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.connect();
            try (InputStream iStream = urlConnection.getInputStream()) {
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
                urlConnection.disconnect();
            }
            return data;
        }

        private void showElevations() {
            setHelpText("Вычисляем высоты..");
            String url = getDirectionsElevationUrl(origin, dest, null);
            Elevations elevations = new Elevations();
            elevations.execute(url);
        }

        private class Elevations extends AsyncTask<String, Integer, Pair<Collection<Double>, String>> {
            @Override
            protected Pair<Collection<Double>, String> doInBackground(String... url) {
                String jsonData = "";
                String status = "";

                try {
                    jsonData = downloadElevationUrl(url[0]);
                } catch (Exception e) {
                    Log.d("Background Task", e.toString());
                }

                JSONObject jObject;
                Collection<Double> elevations = new ArrayList<>();

                if (!jsonData.equals("")) {
                    try {
                        jObject = new JSONObject(jsonData);
                        status = jObject.getString("status");
                        ElevationsJSONParser parser = new ElevationsJSONParser();
                        elevations = parser.parse(jObject);

                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                return new Pair<>(elevations, status);
            }

            @Override
            protected void onPostExecute(Pair<Collection<Double>, String> result) {
                Collection<Double> elevationsArray = result.first;
                String responseStatus = result.second;

                if (!responseStatus.toLowerCase().equals("OVER_QUERY_LIMIT".toLowerCase())) {
                    if (responseStatus.toLowerCase().equals("OK".toLowerCase())) {
                        createGraphView(elevationsArray);
                    } else if (responseStatus.toLowerCase().equals("OVER_QUERY_LIMIT".toLowerCase())) {
                        showMessage("Слишком много запросов.\nПопробуйте снова.");
                    } else if (responseStatus.toLowerCase().equals("INVALID_REQUEST".toLowerCase())) {
                        showMessage("Oшибка запроса.\nПопробуйте снова.");
                    } else if (responseStatus.toLowerCase().equals("REQUEST_DENIED".toLowerCase())) {
                        showMessage("Запрос не выполнен.\nПопробуйте снова.");
                    } else if (responseStatus.toLowerCase().equals("UNKNOWN_ERROR".toLowerCase())) {
                        showMessage("Неизвесная ошибка.\nПопробуйте снова.");
                    } else {
                        showMessage("Неизвесная ошибка.\nПопробуйте снова.");
                    }
                } else {
                    int i = 2;
                    while (responseStatus.toLowerCase().equals("OVER_QUERY_LIMIT".toLowerCase()) & i <= 10) {
                        String key = context.getResources().getString(getKeyId("key" + i));
                        String url = getDirectionsElevationUrl(origin, dest, key);
                        Elevations elevations = new Elevations();
                        elevations.execute(url);
                        i++;
                    }
                }
            }
        }

        private class ElevationsJSONParser {
            private Collection<Double> parse(JSONObject jObject) {

                ArrayList<Double> elevations = null;
                JSONArray jResults;

                try {
                    jResults = jObject.getJSONArray("results");
                    elevations = new ArrayList<>();

                    for (int i = 0; i < jResults.length(); i++) {
                        JSONObject item = (JSONObject) jResults.get(i);
                        double elevation = item.getDouble("elevation");
                        elevations.add(elevation);
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                }

                return elevations;
            }
        }
    }
}