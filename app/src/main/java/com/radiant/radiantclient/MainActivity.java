package com.radiant.radiantclient;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Environment;
import android.os.Handler;
import android.provider.MediaStore;
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

//                                                            Toast a = Toast.makeText(getApplicationContext(), "Sample Send Success", Toast.LENGTH_LONG);


//                                                            toast.cancel();
//                                                            toast.show();
//                                                            Handler handler = new Handler();
//                                                            handler.postDelayed(new Runnable() {
//                                                                @Override
//                                                                public void run() {
//                                                                    toast.cancel();
//                                                                }
//                                                            }, 300);
                                                        }
                                                    },
                                                    new Response.ErrorListener() {
                                                        @Override
                                                        public void onErrorResponse(VolleyError error) {
                                                            Log.d("dede", "error2 " + error.toString());

//                                                            ftoast.cancel();
//                                                            ftoast.show();
//                                                            Handler handler = new Handler();
//                                                            handler.postDelayed(new Runnable() {
//                                                                @Override
//                                                                public void run() {
//                                                                    ftoast.cancel();
//                                                                }
//                                                            }, 300);

//                                                            Toast.makeText(getApplicationContext(), "Sample Send Fail", Toast.LENGTH_SHORT).show();
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
//                    File newFile = new File(file.getAbsolutePath() + "_SENT");
//                    file.renameTo(newFile);
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

        start_button = (Button) findViewById(R.id.start_button);
        restaurant_title = (EditText) findViewById(R.id.restaurant_title);
        description = (EditText) findViewById(R.id.description);
        sendTV = (TextView) findViewById(R.id.send_label);
        send_button = (Button) findViewById(R.id.send_button);

        queue = Volley.newRequestQueue(this);


//        String newJsonFilePath = newJsonFilePath();
//        Log.d("dede", newJsonFilePath);
//        File json_file = new File(newJsonFilePath);
//        try {
//            json_file.createNewFile();
//            JSONObject jo = new JSONObject();
//            jo.put("restaurantName", "aa");
//            jo.put("bundleDescription", "bb");
//            jo.put("data", new JSONArray());
//            writeJsonToFile(json_file, jo);
//
//        } catch (IOException e) {
//            e.printStackTrace();
//        } catch (JSONException e) {
//            e.printStackTrace();
//        }


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
                    Log.d("dede", newJsonFilePath);
                    File json_file = new File(newJsonFilePath);
                    try {
                        json_file.createNewFile();
                        JSONObject jo = new JSONObject();
                        jo.put("restaurantName", rt);
                        jo.put("bundleDescription", desc);
                        Log.d("dede", "data create");
                        jo.put("data", new JSONArray());
                        Log.d("dede", "data create done");
                        writeJsonToFile(json_file, jo);

                    } catch (IOException e) {
                        e.printStackTrace();
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }

                    Intent intent = new Intent(MainActivity.this, SendActivity.class);
                    intent.putExtra("bundleId", "-1");
                    intent.putExtra("localStoreFilePath", newJsonFilePath);
                    startActivity(intent);
                    return;
                }

                String url ="http://ec2-54-191-70-38.us-west-2.compute.amazonaws.com:8100/restaurant/" + rt;

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
        });
    }


    private JSONObject getJsonFromFile(File json_file) {
        try {


            FileInputStream fis = new FileInputStream(json_file);
            byte[] data = new byte[fis.available()];
            while (fis.read(data) != -1){;}
            String temp = new String(data, "UTF-8");
            fis.close();


//            int c;
//            String temp="";
//            int count=0;
//
//            while( (c = fin.read()) != -1){
//                temp = temp + Character.toString((char)c);
//                count++;
//                Log.d("dede", Integer.toString(count));
//            }
//            fin.close();
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
                Log.d("dede", "YES");
                dir.mkdir();
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



// okhttp
/*
        final Thread thread = new Thread(new Runnable() {
            public void run() {
                OkHttpClient client = new OkHttpClient();
                RequestBody body = RequestBody.create(MediaType.parse("application/json; charset=utf-8"), "aa");
                Request request = new Request.Builder()
//                        .url("http://www.naver.com")
                        .post(body)
                        .url("http://ec2-54-191-70-38.us-west-2.compute.amazonaws.com:8100/bundle")
                        .build();

                try {
                    Response response = client.newCall(request).execute();
                    Log.d("dede", response.body().string());
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
        thread.start();


                String fileName = "MyFile";
        String content = "hello world";

//        FileOutputStream outputStream = null;
        try {
//            outputStream = openFileOutput("/de/ff", Context.MODE_PRIVATE);
//            outputStream.write(content.getBytes());
//            outputStream.close();


            String ext = Environment.getExternalStorageState();
            if (ext.equals(Environment.MEDIA_MOUNTED)) {
                File root = Environment.getExternalStorageDirectory().getAbsoluteFile();
                File dir = new File(root.getAbsolutePath(), "Radiant");
                if(dir.exists() == false) {
                    dir.mkdir();
                }

                File json_file = new File(dir.getAbsolutePath(), "abc.txt");
                if(json_file.exists() == false) {
                    json_file.createNewFile();
                }

//                FileInputStream fis = openFileInput(json_file.getAbsolutePath());
//                byte[] data = new byte[fis.available()];
//                while (fis.read(data) != -1){;}
//                fis.close();

//                Log.d("dede", json_file.getAbsolutePath());
//                FileOutputStream fos = new FileOutputStream(json_file);
//                JSONObject a = new JSONObject();
//                a.put("a", "cd");
//                a.put("b", "de");
//                a.put("c", "cd");
//                String str = a.toString();
//                fos.write(str.getBytes());
//                fos.close();

                FileInputStream fin = new FileInputStream(json_file);
                int c;
                String temp="";

                while( (c = fin.read()) != -1){
                    temp = temp + Character.toString((char)c);
                }
                Log.d("dede", temp);
                JSONObject newo = new JSONObject(temp);
                Log.d("dede", newo.getString("a"));
                fin.close();








//                Log.d("dede", root.getAbsolutePath());
//                getMediaContentFolder(root);
//
//                nListAdapter.addFiles(folders);
            }

//            File file = new File("/home/device/", "test.txt");
////            file.createNewFile();
//            if(file.exists()) {
//                Log.d("dede", "file exist");
//                Log.d("dede", file.getAbsolutePath());
//            }
//            else {
//                Log.d("dede", "file not exist");
//            }

//            FileOutputStream fos = openFileOutput("test.txt", Context.MODE_PRIVATE);
//            String str = "Android File IO Test";
//            fos.write(str.getBytes());
//            fos.close();
//
//            FileInputStream fis = openFileInput("test.txt");
//            byte[] data = new byte[fis.available()];
//            while (fis.read(data) != -1){;}
//            fis.close();
//            Log.d("dede", str.getBytes().toString());
//            Log.d("dede", data.toString());
        } catch (Exception e) {
            e.printStackTrace();
            Log.d("dede", e.toString());
        }


        */
