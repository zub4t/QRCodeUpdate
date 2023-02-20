package com.example.myapplication;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.MediaStore;
import android.Manifest;
import android.util.Log;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.vision.barcode.Barcode;
import com.google.android.gms.vision.barcode.BarcodeDetector;
import com.google.android.gms.vision.text.TextRecognizer;

import org.json.JSONArray;
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
import com.google.zxing.BarcodeFormat;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.DecodeHintType;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.NotFoundException;
import com.google.zxing.RGBLuminanceSource;
import com.google.zxing.Result;
import com.google.zxing.common.HybridBinarizer;
import java.util.EnumMap;
import java.util.Map;
public class MainActivity extends AppCompatActivity implements WorkshopListAdapter.OnWorkshopClickListener {
    private class FetchData extends AsyncTask<String, Void, JSONObject> {

        @Override
        protected JSONObject doInBackground(String... urls) {
            try {
                URL url = new URL(urls[0]);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");

                InputStream inputStream = connection.getInputStream();
                BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));

                StringBuilder jsonData = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    jsonData.append(line);
                }

                return new JSONObject(jsonData.toString());
            } catch (IOException | JSONException e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(JSONObject data) {
            // Parse the JSON data and update the UI
            try {
                parseJsonData(data);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    private static final String WORKSHOPS_URL = "http://192.168.1.5/workshops/";
    private List<String> mTalks = new ArrayList<>();
    private RecyclerView mRecyclerView;
    private static final int CAMERA_REQUEST_CODE = 100;
    private static final int CAMERA_PERMISSION_REQUEST_CODE = 1001;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);



            new FetchData().execute(WORKSHOPS_URL);



    }

    private JSONObject fetchJsonData() throws IOException, JSONException {
        URL url = new URL(WORKSHOPS_URL);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");

        InputStream inputStream = connection.getInputStream();
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));

        StringBuilder jsonData = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            jsonData.append(line);
        }

        return new JSONObject(jsonData.toString());
    }

    private void parseJsonData(JSONObject data) throws JSONException {
        JSONArray day1 = data.getJSONArray("Day1");
        JSONArray day2 = data.getJSONArray("Day2");

        for (int i = 0; i < day1.length(); i++) {
            JSONObject workshop = day1.getJSONObject(i);
            mTalks.add(workshop.getString("Talk"));
        }

        for (int i = 0; i < day2.length(); i++) {
            JSONObject workshop = day2.getJSONObject(i);
            mTalks.add(workshop.getString("Talk"));
        }
        // Set up the RecyclerView with the workshop list
        mRecyclerView = findViewById(R.id.workshop_recycler_view);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        WorkshopListAdapter adapter = new WorkshopListAdapter(mTalks, this);
        mRecyclerView.setAdapter(adapter);
    }

    @Override
    public void onWorkshopClick(int position) {
        // Check if the camera permission has been granted
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            String workshopTitle = mTalks.get(position);

            // Create a new Intent for the camera activity and start it
            Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            startActivityForResult(intent, CAMERA_REQUEST_CODE);
        } else {
            // Permission has not been granted, so request it from the user
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA},
                    CAMERA_PERMISSION_REQUEST_CODE);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == CAMERA_REQUEST_CODE && resultCode == RESULT_OK) {
            // Get the image from the camera intent
            Bundle extras = data.getExtras();
            Bitmap imageBitmap = (Bitmap) extras.get("data");

            // Pass the image to the decodeQRCode function
            decodeQRCode(imageBitmap);
        }
    }
    private void decodeQRCode(Bitmap bitmap) {
        int[] intArray = new int[bitmap.getWidth() * bitmap.getHeight()];
        bitmap.getPixels(intArray, 0, bitmap.getWidth(), 0, 0, bitmap.getWidth(), bitmap.getHeight());
        RGBLuminanceSource source = new RGBLuminanceSource(bitmap.getWidth(), bitmap.getHeight(), intArray);
        BinaryBitmap binaryBitmap = new BinaryBitmap(new HybridBinarizer(source));

        Map<DecodeHintType, Object> hints = new HashMap<>();
        List<BarcodeFormat> formats = new ArrayList<>();
        formats.add(BarcodeFormat.QR_CODE);
        hints.put(DecodeHintType.POSSIBLE_FORMATS, formats);

        MultiFormatReader reader = new MultiFormatReader();
        reader.setHints(hints);

        try {
            Result result = reader.decode(binaryBitmap);
            sendGetRequest(String.format("http://192.168.1.5/score-points-to/%s",result.getText()));
            Toast.makeText(this, result.getText(), Toast.LENGTH_SHORT).show();
        } catch (NotFoundException e) {
            Toast.makeText(this, "Error: QR code not found.", Toast.LENGTH_SHORT).show();
        }
    }
    public void sendGetRequest(String url) {
        new AsyncTask<String, Void, String>() {
            @Override
            protected String doInBackground(String... params) {
                String response = null;
                try {
                    URL url = new URL(params[0]);
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("GET");

                    // Set up the connection
                    conn.setDoOutput(false);
                    conn.setConnectTimeout(5000);
                    conn.setReadTimeout(5000);

                    // Connect to the server and read the response
                    conn.connect();
                    InputStream inputStream = conn.getInputStream();
                    BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
                    StringBuilder data = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        data.append(line);
                    }
                    response = data.toString();

                    // Close the connection
                    conn.disconnect();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                return response;
            }

            @Override
            protected void onPostExecute(String response) {
                if (response != null) {
                    // Handle the response
                    Log.d("Response", response);
                } else {
                    // Handle the error
                    Log.d("Error", "Failed to fetch data");
                }
            }
        }.execute(url);
    }




}
