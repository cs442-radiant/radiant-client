package com.radiant.radiantclient;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Environment;
import android.os.Handler;
import android.provider.MediaStore;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
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

//import okhttp3.MediaType;
//import okhttp3.OkHttpClient;
//import okhttp3.Request;
//import okhttp3.RequestBody;
//import okhttp3.Response;

public class MainActivity extends AppCompatActivity {

    RequestQueue queue;

    Button send_button;
    Button start_button;
    EditText restaurant_title;
    EditText description;
    TextView sendTV;

    int startType = 0;

    int unsentcount = 0;

    JSONObject jobj;

    int successCount = 0;


    // Storage Permissions
    private static final int REQUEST_EXTERNAL_STORAGE = 1;
    private static String[] PERMISSIONS_STORAGE = {
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.CHANGE_WIFI_STATE,
            Manifest.permission.INTERNET,
            Manifest.permission.ACCESS_WIFI_STATE,
            Manifest.permission.WAKE_LOCK,
            Manifest.permission.SYSTEM_ALERT_WINDOW,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION

    };

    /**
     * Checks if the app has permission to write to device storage
     *
     * If the app does not has permission then the user will be prompted to grant permissions
     *
     * @param activity
     */
    public static void verifyStoragePermissions(Activity activity) {
        ActivityCompat.requestPermissions(
                activity,
                PERMISSIONS_STORAGE,
                REQUEST_EXTERNAL_STORAGE
        );
    }

    @Override
    protected void onResume() {
        super.onResume();

        refreshCount();
    }

    private void refreshCount() {
        unsentcount = 0;
        String ext = Environment.getExternalStorageState();
        if (ext.equals(Environment.MEDIA_MOUNTED)) {
            File root = Environment.getExternalStorageDirectory().getAbsoluteFile();
            File dir = new File(root.getAbsolutePath(), "Radiant");
            if (dir.exists() == false) {
                dir.mkdir();
            }
            if(dir.listFiles() != null) {
                for (File file : dir.listFiles()) {
                    if (!file.getName().contains("SENT")) {
                        unsentcount++;
                    }
                }
                sendTV.setText(Integer.toString(unsentcount) + " unsent samples");
            }
        }
    }


    private void handleSend() {
        String ext = Environment.getExternalStorageState();
        if (ext.equals(Environment.MEDIA_MOUNTED)) {
            File root = Environment.getExternalStorageDirectory().getAbsoluteFile();
            File dir = new File(root.getAbsolutePath(), "Radiant");
            if (dir.exists() == false) {
                dir.mkdir();
            }
            successCount = 0;
            for (File file : dir.listFiles()) {
                if(!file.getName().contains("SENT")) {
                    Log.d("dede", file.getAbsolutePath());
                    jobj = getJsonFromFile(file);

                    Log.d("dede", jobj.toString());
//                    if(true)
//                        return;


                    String url = null;
                    JSONObject jsonBody = null;
                    try {
                        url = "http://ec2-54-191-70-38.us-west-2.compute.amazonaws.com:8100/bundle";

                        jsonBody = new JSONObject();
                        jsonBody.put("restaurantName", jobj.getString("restaurantName"));
                        jsonBody.put("bundleDescription", jobj.getString("bundleDescription"));

                    } catch (JSONException e) {
                        e.printStackTrace();
                    }

                    final Toast toast = Toast.makeText(getApplicationContext(), "Sample Send Success", Toast.LENGTH_SHORT);
                    final Toast ftoast = Toast.makeText(getApplicationContext(), "Sample Send Fail", Toast.LENGTH_SHORT);

                    JsonObjectRequest jsonRequest = new JsonObjectRequest(Request.Method.POST, url, jsonBody,
                            new Response.Listener<JSONObject>() {
                                @Override
                                public void onResponse(JSONObject rjo) {
                                    try {
                                        Log.d("dede", "json response " + rjo.toString());
                                        String bundleId = rjo.getString("bundleId");
                                        JSONArray data = jobj.getJSONArray("data");
                                        for(int i=0; i<data.length(); i++) {
                                            JSONObject sample = data.getJSONObject(i);
                                            sample.put("bundleId", Integer.parseInt(bundleId));
                                            Log.d("dede", sample.toString());
                                            String url = "http://ec2-54-191-70-38.us-west-2.compute.amazonaws.com:8100/sample";
                                            JsonObjectRequest jsonRequest = new JsonObjectRequest(Request.Method.POST, url, sample,
                                                    new Response.Listener<JSONObject>() {
                                                        @Override
                                                        public void onResponse(JSONObject response) {
                                                            Log.d("dede", "Response Body : " + response.toString());
                                                            toast.cancel();
                                                            toast.show();
                                                        }
                                                    },
                                                    new Response.ErrorListener() {
                                                        @Override
                                                        public void onErrorResponse(VolleyError error) {
                                                            Log.d("dede", "error2 " + error.toString());
                                                            ftoast.cancel();
                                                            ftoast.show();
                                                        }
                                                    });

                                            queue.add(jsonRequest);

                                        }

                                    } catch (JSONException e) {
                                        e.printStackTrace();
                                    }

                                }
                            },
                            new Response.ErrorListener() {
                                @Override
                                public void onErrorResponse(VolleyError error) {
                                    Log.d("dede", "error1 " + error.toString());
                                }
                            });

                    queue.add(jsonRequest);
                    File newFile = new File(file.getAbsolutePath() + "_SENT");
                    file.renameTo(newFile);
                    refreshCount();
                }
            }
        }
        refreshCount();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d("dede", "create first");
        setContentView(R.layout.activity_main);

        verifyStoragePermissions(this);

        start_button = (Button) findViewById(R.id.start_button);
        restaurant_title = (EditText) findViewById(R.id.restaurant_title);
        description = (EditText) findViewById(R.id.description);
        sendTV = (TextView) findViewById(R.id.send_label);
        send_button = (Button) findViewById(R.id.send_button);

        queue = Volley.newRequestQueue(this);

        send_button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                handleSend();
            }
        });

        start_button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {

                if(startType == 0)
                    return;

                String rt = restaurant_title.getText().toString();
                String desc = description.getText().toString();
                if(rt == "") {
                    return;
                }

                if(startType == 2) {
                    String newJsonFilePath = newJsonFilePath();
                    File json_file = new File(newJsonFilePath);
                    try {
                        json_file.createNewFile();
                        JSONObject jo = new JSONObject();
                        jo.put("restaurantName", rt);
                        jo.put("bundleDescription", desc);
                        jo.put("data", new JSONArray());
                        writeJsonToFile(json_file, jo);

                    } catch (IOException e) {
                        Log.d("dede", "11 "+ e.toString());
                        e.printStackTrace();
                    } catch (JSONException e) {
                        Log.d("dede", "22 "+ e.toString());
                        e.printStackTrace();
                    }

                    Intent intent = new Intent(MainActivity.this, SendActivity.class);
                    intent.putExtra("bundleId", "-1");
                    intent.putExtra("localStoreFilePath", newJsonFilePath);
                    startActivity(intent);
                }

                if(startType == 1) {
                    String url = "http://ec2-54-191-70-38.us-west-2.compute.amazonaws.com:8100/restaurant/" + rt;

                    StringRequest stringRequest = new StringRequest(Request.Method.GET, url,
                            new Response.Listener<String>() {
                                @Override
                                public void onResponse(String response) {

                                    handleRestaurantQuery(response);
                                }
                            }, new Response.ErrorListener() {
                        @Override
                        public void onErrorResponse(VolleyError error) {
                            AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                            builder.setTitle("Restaurant Name Query Fail")
                                    .setMessage(error.toString())
                                    .setPositiveButton("CONFIRM", new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog, int id) {
                                        }
                                    })
                                    .setNegativeButton("CANCEL", new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog, int id) {
                                        }
                                    });

                            AlertDialog dialog = builder.create();
                            dialog.show();
                        }
                    });

                    queue.add(stringRequest);
                }
            }
        });
    }


    private JSONObject getJsonFromFile(File json_file) {
        try {


            FileInputStream fis = new FileInputStream(json_file);
            byte[] data = new byte[fis.available()];
            while (fis.read(data) != -1){;}
            String temp = new String(data, "UTF-8");
            fis.close();
            return new JSONObject(temp);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (JSONException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    private void writeJsonToFile(File json_file, JSONObject jo) {
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(json_file);
            fos.write(jo.toString().getBytes());
            fos.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private String newJsonFilePath() {
        String ext = Environment.getExternalStorageState();
        if (ext.equals(Environment.MEDIA_MOUNTED)) {
            File root = Environment.getExternalStorageDirectory().getAbsoluteFile();
            File dir = new File(root.getAbsolutePath(), "Radiant");
            Log.d("dede", dir.getAbsolutePath());
            if (dir.exists() == false) {
                if(dir.mkdir() == true) {
                    Log.d("dede", "SUCCESS");
                }
                else {
                    Log.d("dede", "Fail");
                }
            }
            else {
                Log.d("dede", "NO");
            }

            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            java.util.Date currenTimeZone=new java.util.Date(System.currentTimeMillis());
            String ts = sdf.format(currenTimeZone);


            return dir.getAbsolutePath() + "/" + ts;
        }
        return "temp.txt";
    }

    private void handleRestaurantQuery(String response) {
        try {
            JSONObject json = new JSONObject(response);
            boolean exists = json.getBoolean("exists");
            String title;
            String content;

            if(exists == true) {
                title = "Restaurant already exists";
            }
            else {
                title = "New Restaurant";
            }
            content = "Do you want to continue?";

            AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
            builder.setTitle(title)
                    .setMessage(content)
                    .setPositiveButton("CONFIRM", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            String rt = restaurant_title.getText().toString();
                            String desc = description.getText().toString();

                            String url = "http://ec2-54-191-70-38.us-west-2.compute.amazonaws.com:8100/bundle";

                            final JSONObject jsonBody = new JSONObject();
                            try {
                                jsonBody.put("restaurantName", rt);
                                jsonBody.put("bundleDescription", desc);
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }

                            Log.d("dede", "Request Body : " + jsonBody.toString());

                            JsonObjectRequest jsonRequest = new JsonObjectRequest(Request.Method.POST, url, jsonBody,
                                    new Response.Listener<JSONObject>() {
                                        @Override
                                        public void onResponse(JSONObject response) {
                                            Log.d("dede", "Response Body : " + response.toString());
                                            try {
                                                Intent intent = new Intent(MainActivity.this, SendActivity.class);
                                                intent.putExtra("bundleId", response.getString("bundleId"));
                                                startActivity(intent);
                                            } catch (JSONException e) {
                                                e.printStackTrace();
                                            }
                                        }
                                    },
                                    new Response.ErrorListener() {
                                        @Override
                                        public void onErrorResponse(VolleyError error) {
                                            Log.d("dede", error.toString());
                                            AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                                            builder.setTitle("Bundle Id Query Fail")
                                                    .setMessage(error.toString())
                                                    .setPositiveButton("CONFIRM", new DialogInterface.OnClickListener() {
                                                        public void onClick(DialogInterface dialog, int id) {
                                                        }
                                                    })
                                                    .setNegativeButton("CANCEL", new DialogInterface.OnClickListener() {
                                                        public void onClick(DialogInterface dialog, int id) {
                                                        }
                                                    });

                                            AlertDialog dialog = builder.create();
                                            dialog.show();                                        }
                                    });

                            queue.add(jsonRequest);
                        }
                    })
                    .setNegativeButton("CANCEL", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                        }
                    });

            AlertDialog dialog = builder.create();
            dialog.show();


        } catch (JSONException e) {
            e.printStackTrace();
        }
    }
    public void onRadioButtonClicked(View view) {
        // Is the button now checked?
        boolean checked = ((RadioButton) view).isChecked();

        // Check which radio button was clicked
        switch(view.getId()) {
            case R.id.radio_pirates:
                if (checked)
                    startType = 1;
                    break;
            case R.id.radio_ninjas:
                if (checked)
                    startType = 2;
                    break;
        }
    }
}
