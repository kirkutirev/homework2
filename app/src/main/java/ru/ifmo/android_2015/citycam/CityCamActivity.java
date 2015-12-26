package ru.ifmo.android_2015.citycam;

import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

import ru.ifmo.android_2015.citycam.model.City;

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
    private TextView titleTextView;

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
        titleTextView = (TextView) findViewById(R.id.textView);

        titleTextView.setText("Downloading...");
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


    class DownloadFileTask extends AsyncTask<Void, Void, Integer> {

        private CityCamActivity activity;

        DownloadFileTask(CityCamActivity act) {
            this.activity = act;
        }

        void attachActivity(CityCamActivity act) {
            this.activity = act;
            updateView();
        }

        void updateView() {
            if(activity != null) {
                activity.titleTextView.setText("Yeah, baby!");
                activity.progressView.setProgress(100);
            }
        }

        @Override
        protected Integer doInBackground(Void... params) {
            try {
                // downloadFile(downloadURL, destFile);
                for(int i = 0; i < 1_000_000_000; i++);

                return 0;
            } catch (Exception ex) {
                Log.e(TAG, "Error downloading file: " + ex, ex);
                return 1;
            }
        }

        @Override
        protected void onPostExecute(Integer resultCode) {
            // this method executes in UI thread
            titleTextView.setText(resultCode == 0 ? "Done!" : "Error!");
            progressView.setVisibility(View.INVISIBLE);
        }

    }

    static void downloadFile(String curURL, ImageView image) throws IOException {
        HttpURLConnection conn = (HttpURLConnection) new URL(curURL).openConnection();

        int contentLenght = conn.getContentLength();
        int receivedLength = 0;

    }

    private static final String TAG = "CityCam";
}
