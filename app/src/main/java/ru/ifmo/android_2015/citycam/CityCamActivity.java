package ru.ifmo.android_2015.citycam;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.util.JsonReader;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Date;
import java.util.List;

import ru.ifmo.android_2015.citycam.model.City;
import ru.ifmo.android_2015.citycam.webcams.Webcams;

/**
 * Экран, показывающий веб-камеру одного выбранного города.
 * Выбранный город передается в extra параметрах.
 */
public class CityCamActivity extends AppCompatActivity {

    /**
     * Обязательный extra параметр - объект City, камеру которого надо показать.
     */
    public static final String EXTRA_CITY = "city";

    private City city;
    private  DownloadFileTask downloadTask;

    private ImageView camImageView;
    private ProgressBar progressView;
    private TextView title;
    private TextView webcamid;

    @Override
    public Object onRetainCustomNonConfigurationInstance() {
        // Этот метод вызывается при смене конфигурации,
        // когда текущий объект Activity уничтожается. Объект,
        // который мы вернем, не будет уничтожен, и его можно
        // будет использовать в новом объекте Activity
        return downloadTask;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        city = getIntent().getParcelableExtra(EXTRA_CITY);
        if (city == null) {
            Log.w(TAG, "City object not provided in extra parameter: " + EXTRA_CITY);
            finish();
        }

        setContentView(R.layout.activity_city_cam);
        camImageView = (ImageView) findViewById(R.id.cam_image);
        progressView = (ProgressBar) findViewById(R.id.progress);
        title = (TextView) findViewById(R.id.title);
        webcamid = (TextView) findViewById(R.id.webcamid);

        getSupportActionBar().setTitle(city.name);

        progressView.setVisibility(View.VISIBLE);

        // Здесь должен быть код, инициирующий асинхронную загрузку изображения с веб-камеры
        // в выбранном городе.

        if(savedInstanceState != null) {
            downloadTask = (DownloadFileTask) getLastCustomNonConfigurationInstance();
        }

        if(downloadTask == null) {
            downloadTask = new DownloadFileTask(this);
            downloadTask.execute();
        } else {
            downloadTask.attachActivity(this);
        }
    }

    private class MyCam {
        String preview;
        String title;
        String webcamid;

        MyCam(String pr, String t, String id) {
            preview = pr;
            title = t;
            webcamid = id;
        }
    }

    class DownloadFileTask extends AsyncTask<Void, Void, Integer> {

        private CityCamActivity activity;
        private Bitmap preview = null;
        MyCam cam = new MyCam(null, null, null);

        DownloadFileTask(CityCamActivity act) {
            this.activity = act;
            this.preview = null;
        }

        void attachActivity(CityCamActivity act) {
            this.activity = act;
            updateView();
        }

        void updateView() {
            if(preview != null) {
                activity.progressView.setVisibility(View.INVISIBLE);
                activity.camImageView.setImageBitmap(preview);
                activity.title.setText(cam.title);
                activity.webcamid.setText("camera id - " + cam.webcamid);
            } else {
                Toast toast = Toast.makeText(getApplicationContext(),
                        "Continue downloading", Toast.LENGTH_SHORT);
                toast.show();
            }
        }

        @Override
        protected Integer doInBackground(Void... params) {
            URL cur_url;

            try {
                cur_url = Webcams.createNearbyUrl(activity.city.latitude, activity.city.longitude);
            } catch (Exception ex) {
                Log.e(TAG, "URL ERROR!" + ex.getMessage(), ex);
                return 1;
            }

            try {
                downloadFile(cur_url, cam);
                preview = downloadImage(new URL(cam.preview));
                return 0;
            } catch (Exception ex) {
                Log.e(TAG, "Error downloading file: " + ex, ex);
                return 1;
            }
        }

        @Override
        protected void onPostExecute(Integer resultCode) {
            // this method executes in UI thread
            if(preview != null) {
                activity.progressView.setVisibility(View.INVISIBLE);
                activity.camImageView.setImageBitmap(preview);
                activity.title.setText(cam.title);
                activity.webcamid.setText("camera id - " + cam.webcamid);
            } else {
                Toast toast = Toast.makeText(getApplicationContext(),
                        "No cameras in this city", Toast.LENGTH_SHORT);
                toast.show();
            }
        }

    }

    static void downloadFile(URL curURL, MyCam cam) throws IOException {
        HttpURLConnection conn = null;
        InputStream in = null;

        try {
            conn = (HttpURLConnection) curURL.openConnection();
            in = conn.getInputStream();
            if (conn.getResponseCode() != HttpURLConnection.HTTP_OK) {
                throw new IOException("Bad response from server: " + conn.getResponseMessage());
            }

            JsonReader reader = new JsonReader(new InputStreamReader(in));

            reader.beginObject();
            while (reader.hasNext() && !reader.nextName().equals("webcams")) {
                reader.skipValue();
            }

            reader.beginObject();
            while (reader.hasNext() && !reader.nextName().equals("webcam")) {
                reader.skipValue();
            }

            reader.beginArray();
            reader.beginObject();

            String preview = null;
            String title = null;
            String webcamid = null;

            while (reader.hasNext()) {
                String name = reader.nextName();
                switch(name) {
                    case "preview_url" : {
                        preview = reader.nextString();
                        break;
                    }
                    case "title" : {
                        title = reader.nextString();
                        break;
                    }
                    case "webcamid" : {
                        webcamid = reader.nextString();
                        break;
                    }
                    default :
                        reader.skipValue();
                }
            }
            reader.close();
            cam.preview = preview;
            cam.title = title;
            cam.webcamid = webcamid;
        } finally {
            if(in != null) {
                try {
                    in.close();
                } catch (Exception ignore) {

                }
            }
            if(conn != null) {
                conn.disconnect();
            }
        }
    }

    private Bitmap downloadImage(URL url) throws IOException {
        if (url == null) return null;
        HttpURLConnection connection = null;
        InputStream inputStream = null;
        try {
            connection = (HttpURLConnection) url.openConnection();
            if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                throw new IOException("Bad response from server: " + connection.getResponseMessage());
            }
            inputStream = connection.getInputStream();
            return BitmapFactory.decodeStream(inputStream);
        } catch (IOException e) {
            Log.e(TAG, e.getMessage());
            return null;
        } finally {
            if (inputStream != null) {
                inputStream.close();
            }

            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    private static final String TAG = "CityCam";
}
