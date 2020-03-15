package com.example.gpbusapp;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.FragmentActivity;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Color;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MapStyleOptions;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {
    private static final int LOCATION_REQUEST = 500;
    private GoogleMap mMap;
    private LocationManager locationManager;
    ArrayList<LatLng> listPoints;
    private static final String TAG = MapsActivity.class.getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Retrieve the content view that renders the map.
        setContentView(R.layout.activity_maps);

        // Get the SupportMapFragment and register for the callback
        // when the map is ready for use.
        SupportMapFragment mapFragment =
                (SupportMapFragment) getSupportFragmentManager()
                        .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
    }

    /**
     * Manipulates the map when it's available.
     * The API invokes this callback when the map is ready for use.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {

        mMap = googleMap;
        // Add a marker in Sydney and move the camera
        //LatLng terminal = new LatLng(-3.7383303,-38.5849932);
        //LatLng facul = new LatLng(-3.766111, -38.574305);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,new String[] {Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_REQUEST);
            return;
        }
        mMap.getUiSettings().setZoomControlsEnabled(true);
        mMap.setMyLocationEnabled(true);
        mMap.setOnMapClickListener(new GoogleMap.OnMapClickListener(){

            @Override
            public void onMapClick(LatLng latLng) {
                if(listPoints.size()==2){
                    listPoints.clear();
                    mMap.clear();
                }
                listPoints.add(latLng);
                MarkerOptions markerOptions = new MarkerOptions();
                markerOptions.position(latLng);
                if (listPoints.size() ==1){
                    markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN));
                }else{
                    markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED));
                }
                mMap.addMarker(markerOptions);
                if(listPoints.size()==2){
                    String url= getRequestUrl(listPoints.get(0),listPoints.get(1));
                    TaskRequestDirections taskRequestDirections = new TaskRequestDirections();
                    taskRequestDirections.execute(url);
                }
            }
        });

        try {
            // Customise the styling of the base map using a JSON object defined
            // in a raw resource file.
            boolean success = googleMap.setMapStyle(
                    MapStyleOptions.loadRawResourceStyle(
                            this, R.raw.style_json));

            if (!success) {
                Log.e(TAG, "Style parsing failed.");
            }
        } catch (Resources.NotFoundException e) {
            Log.e(TAG, "Can't find style. Error: ", e);
        }
        // Position the map's camera near Sydney, Australia.
        googleMap.moveCamera(CameraUpdateFactory.newLatLng(new LatLng(-34, 151)));
    }

    private String getRequestUrl(LatLng origin, LatLng dest) {
        String str_org = "origin="+ origin.latitude +","+origin.longitude;
        String str_dest ="destination="+dest.latitude+","+dest.longitude;
        String sensor="sensor=false";
        String mode="mode=driving";
        String param = str_org +"&"+ str_dest +"&"+ sensor+ "&" +mode;
        String output = "json";
        String url = "https://maps.googleapis.com/maps/api/directions/" + output + "?" + param + "&key=" + getString(R.string.google_maps_key);
        return url;
    }
    private String requestDirections(String reqUrl) throws IOException {
        String responseString = "";
        InputStream inputStream = null;
        HttpURLConnection httpURLConnection = null;
        try{
            URL url = new URL(reqUrl);
            httpURLConnection = (HttpURLConnection) url.openConnection();
            httpURLConnection.connect();
            inputStream = httpURLConnection.getInputStream();
            InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
            BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
            StringBuffer stringBuffer = new StringBuffer();
            String  line= "";
            while((line=bufferedReader.readLine())!=null){
                stringBuffer.append(line);
            }
            responseString = stringBuffer.toString();
            bufferedReader.close();
            inputStreamReader.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        finally {
            if (inputStream !=null){
                inputStream.close();
            }
            httpURLConnection.disconnect();
        }
        return responseString;
    }
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode){
            case LOCATION_REQUEST:
                if(grantResults.length >0 && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                    mMap.setMyLocationEnabled(true);
                }
                break;
        }
    }
    @SuppressLint("StaticFieldLeak")
    public class TaskRequestDirections extends AsyncTask<String, Void, String> {

        @Override
        protected String doInBackground(String... strings) {
            String responseString = "";
            try {
                responseString =  requestDirections(strings[0]);
            } catch (IOException e) {
                e.printStackTrace();
            }
            return responseString;
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
            TaskParser taskParser = new TaskParser();
            taskParser.execute(s);
        }
        @SuppressLint("StaticFieldLeak")
        public class TaskParser extends AsyncTask<String,Void, List<List<HashMap<String, String>>>>{


            @Override
            protected List<List<HashMap<String, String>>> doInBackground(String... strings) {
                JSONObject jsonObject = null;
                List<List<HashMap<String, String>>> routes = null;
                try {
                    jsonObject = new JSONObject(strings[0]);
                    DirectionsParser directionsParser =  new DirectionsParser();
                    routes = directionsParser.parse(jsonObject);

                } catch (JSONException e) {
                    e.printStackTrace();
                }
                return routes;
            }

            @Override
            protected void onPostExecute(List<List<HashMap<String, String>>> lists) {

                ArrayList points = null;
                PolylineOptions polylineOptions =null;
                for (List<HashMap<String, String>> path: lists){
                    points = new ArrayList();
                    polylineOptions=new PolylineOptions();

                    for(HashMap<String, String> point:path){
                        double lat = Double.parseDouble(point.get("lat"));
                        double lon = Double.parseDouble(point.get("lon"));

                        points.add(new LatLng(lat,lon));
                    }
                    polylineOptions.addAll(points);
                    polylineOptions.width(15);
                    polylineOptions.color(Color.BLUE);
                    polylineOptions.geodesic(true);
                }
                if(polylineOptions!=null){
                    mMap.addPolyline(polylineOptions);
                }else{
                    Toast.makeText(getApplicationContext(),"Direction not found",Toast.LENGTH_SHORT).show();
                }
            }
        }
    }
}

