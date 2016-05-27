package com.radiant.radiantclient;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by hnamkoong on 16. 5. 5..
 */
public class SendActivity extends AppCompatActivity {

    ListView listView;
    ArrayAdapter<String> adapter;
    List<String> values = new ArrayList<String>();


    private TimerTask mTask;
    private Timer mTimer = new Timer();
    int maxSec=3;
    int sec=maxSec;

    int count=0;

    TextView timerTV;
    TextView countTV;
    TextView filesizeTV;

    RequestQueue queue;

    String bundleId;
    String localStoreFilePath;
    JSONObject jo;
    JSONArray data;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_send);

        Intent intent = getIntent();
        bundleId = intent.getStringExtra("bundleId");

        final Button stop_button = (Button) findViewById(R.id.stop_button);
        stop_button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                stopSurvey();
            }
        });

        timerTV = (TextView) findViewById(R.id.timer);
        countTV = (TextView) findViewById(R.id.count);
        filesizeTV = (TextView) findViewById(R.id.size);

        listView = (ListView) findViewById(R.id.list);
        adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, android.R.id.text1, values);
        listView.setAdapter(adapter);


        if(bundleId.equals("-1")) {
            localStoreFilePath = intent.getStringExtra("localStoreFilePath");
            try {
                File json_file = new File(localStoreFilePath);
                FileInputStream fin = new FileInputStream(json_file);
                int c;
                String temp="";

                while( (c = fin.read()) != -1){
                    temp = temp + Character.toString((char)c);
                }

                jo = new JSONObject(temp);
                data = jo.getJSONArray("data");

//                JSONArray ad = (JSONArray) jo.get("data");
//                fin.close();
//
//                FileOutputStream fos = new FileOutputStream(json_file);
//                fos.write(jo.toString().getBytes());
//                fos.close();

            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (JSONException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        else {
            filesizeTV.setVisibility(View.GONE);
        }


        queue = Volley.newRequestQueue(this);

        mTask = new TimerTask() {
            @Override
            public void run() {
                sec--;
                SendActivity.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        timerTV.setText(Integer.toString(sec) + "s");
                    }
                });
                if(sec < 1) {
                    sec = maxSec;
                    SendActivity.this.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                scanWifi();
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                            adapter.notifyDataSetChanged();
                            sec = maxSec;
                            timerTV.setText(Integer.toString(sec) + "s");
                        }
                    });
                }
//                Log.d("dede", "timer go on");
            }
        };
        sec++;
        mTimer.schedule(mTask, 0, 1000);

        try {
            scanWifi();
        } catch (JSONException e) {
            e.printStackTrace();
        }
        adapter.notifyDataSetChanged();


    }

    private void scanWifi() throws JSONException {
        WifiManager wifi;
        wifi = (WifiManager) getSystemService(Context.WIFI_SERVICE);
        if (wifi.isWifiEnabled() == false)
        {
            Toast.makeText(getApplicationContext(), "wifi is disabled. making it enabled", Toast.LENGTH_LONG).show();
            wifi.setWifiEnabled(true);
        }

        JSONObject sample = new JSONObject();
//        Long tsLong = System.currentTimeMillis()/1000;
//        String ts = tsLong.toString();

        sample.put("bundleId", Integer.parseInt(bundleId));

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
        java.util.Date currenTimeZone=new java.util.Date(System.currentTimeMillis());
        String ts = sdf.format(currenTimeZone);


        sample.put("timestamp", ts);
//        Log.d("dede", "timestamp : " + ts);
//        Log.d("dede", "bundleId : " + bundleId);

        List<ScanResult> results = wifi.getScanResults();
        values.clear();
        int cnt = 1;
        String value;
        JSONArray apList = new JSONArray();
        for(ScanResult sr : results) {
            value = Integer.toString(cnt) + "\nSSID: " + sr.SSID + "\n";
            value += "BSSID: " + sr.BSSID + "\n";
            value += "capabilities: " + sr.capabilities + "\n";
            value += "level: " + sr.level + "\n";
            value += "frequency: " + sr.frequency + "\n";
//            CharSequence as = sr.venueName;
//            value += "timestamp: " + Long.toString(sr.timestamp) + "\n";
//            int a = sr.channelWidth;
//            value += "channelWidth: " + Integer.toString(sr.channelWidth) + "\n";

//            value += sr.toString();
            values.add(value);

            JSONObject ap = new JSONObject();
            ap.put("SSID", sr.SSID);
            ap.put("BSSID", sr.BSSID);
            ap.put("capabilities", sr.capabilities);
            ap.put("level", sr.level);
            ap.put("frequency", sr.frequency);
            apList.put(ap);
            cnt++;
        }
        sample.put("WiFiList", apList);
        Log.d("dede", sample.toString());

        count++;
        countTV.setText("#" + Integer.toString(count));

//        Log.d("dede", bundleId);
        if(bundleId.equals("-1")) {
            data.put(sample);
            int size = jo.toString().length() / 1024;
            filesizeTV.setText(Integer.toString(size) + "KB");
        }
        else {
            String url = "http://ec2-54-191-70-38.us-west-2.compute.amazonaws.com:8100/sample";


            JsonObjectRequest jsonRequest = new JsonObjectRequest(Request.Method.POST, url, sample,
                    new Response.Listener<JSONObject>() {
                        @Override
                        public void onResponse(JSONObject response) {
                            Log.d("dede", "Response Body : " + response.toString());
                            Toast.makeText(getApplicationContext(), "Send Success", Toast.LENGTH_SHORT).show();
                        }
                    },
                    new Response.ErrorListener() {
                        @Override
                        public void onErrorResponse(VolleyError error) {
                            Log.d("dede", "error?" + error.toString());
                            Toast.makeText(getApplicationContext(), "Send Fail", Toast.LENGTH_SHORT).show();
                        }
                    });

            queue.add(jsonRequest);
        }

//        Log.d("dede", "wifi scan!");
    }


    private void writeJsonToFile(JSONObject jo) {
        try {
            File json_file = new File(localStoreFilePath);
            FileOutputStream fos = new FileOutputStream(json_file);
            fos.write(jo.toString().getBytes());
            fos.close();

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    private void stopSurvey() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        String title = " lists have been sent to server.";
        if(bundleId.equals("-1")) {
            title = " lists will be stored in local";
        }

        builder.setTitle("Do you really want to stop?")
                .setMessage(Integer.toString(count) + title)
                .setPositiveButton("CONFIRM", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        mTimer.purge();
                        mTimer.cancel();

                        // when save in store
                        if (bundleId.equals("-1")) {
                            writeJsonToFile(jo);
                        }

                        finish();
                    }
                })
                .setNegativeButton("CANCEL", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                    }
                });

        AlertDialog dialog = builder.create();
        dialog.show();
    }

    @Override
    public void onBackPressed() {
        stopSurvey();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }
}