package com.mobiquity.mydropbox.ui;

import android.app.FragmentTransaction;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.app.NotificationCompat;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.mobiquity.mydropbox.DropboxApp;
import com.mobiquity.mydropbox.DropboxClient;
import com.mobiquity.mydropbox.R;
import com.mobiquity.mydropbox.networking.task.UploadFileTask;
import com.mobiquity.mydropbox.notification.INotifier;
import com.squareup.otto.Bus;
import com.squareup.picasso.Picasso;

import java.io.File;

public class UploadImageActivity extends AppCompatActivity implements OnMapReadyCallback, View.OnClickListener {

    private String pathForPicasso;
    private String pathForUploading;
    private double latitude;
    private double longitude;
    private String cityName;

    private Bus bus;

    private static final String KEY_PATH_FOR_PICASSO = "KEY_PATH_FOR_PICASSO";
    private static final String KEY_PATH_FOR_UPLOADING = "KEY_PATH_FOR_UPLOADING";
    private static final String KEY_LATITUDE = "KEY_LATITUDE";
    private static final String KEY_LONGITUDE = "KEY_LONGITUDE";
    private static final String KEY_CITY = "KEY_CITY";


    public static void start(Context context, String pathForUploading, String pathForPicasso, double latitude, double longitude, String city) {
        Intent intent = new Intent(context, UploadImageActivity.class);
        intent.putExtra(KEY_PATH_FOR_PICASSO, pathForPicasso);
        intent.putExtra(KEY_LATITUDE, latitude);
        intent.putExtra(KEY_LONGITUDE, longitude);
        intent.putExtra(KEY_CITY, city);
        intent.putExtra(KEY_PATH_FOR_UPLOADING, pathForUploading);
        context.startActivity(intent);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_upload_image);
        bus = DropboxApp.getBus();
        Bundle bundle = getIntent().getExtras();
        if (bundle != null) {
            pathForPicasso = bundle.getString(KEY_PATH_FOR_PICASSO);
            latitude = bundle.getDouble(KEY_LATITUDE);
            longitude = bundle.getDouble(KEY_LONGITUDE);
            cityName = bundle.getString(KEY_CITY);
            pathForUploading = bundle.getString(KEY_PATH_FOR_UPLOADING);
        }

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        ImageView capturedImageView = (ImageView) findViewById(R.id.upload_picture_fragment_image);
        FrameLayout mapContainer = (FrameLayout) findViewById(R.id.map);

        if (latitude != 0 && longitude != 0) {
            mapContainer.setVisibility(View.VISIBLE);
            MapFragment mapFragment = MapFragment.newInstance();
            FragmentTransaction fragmentTransaction =
                    getFragmentManager().beginTransaction();
            fragmentTransaction.add(R.id.map, mapFragment);
            fragmentTransaction.commit();
            mapFragment.getMapAsync(this);
        }

        File file = new File(pathForPicasso);
        Picasso.with(this).setIndicatorsEnabled(true);
        Picasso.with(this)
                .load(file)
                .fit()
                .centerCrop()
                .into(capturedImageView);

        TextView latitudeTextView = (TextView) findViewById(R.id.image_latitude_textview);
        TextView longitudeTextView = (TextView) findViewById(R.id.image_longitude_textview);
        TextView cityTextView = (TextView) findViewById(R.id.image_city_textview);

        if (cityName != null) {
            cityTextView.setVisibility(View.VISIBLE);
            cityTextView.setText(String.format(getString(R.string.image_city_text), cityName));
        }

        if (latitude != 0) {
            latitudeTextView.setVisibility(View.VISIBLE);
            latitudeTextView.setText(String.format(getString(R.string.image_latitude_text), String.valueOf(latitude)));
        }

        if (longitude != 0) {
            longitudeTextView.setVisibility(View.VISIBLE);
            longitudeTextView.setText(String.format(getString(R.string.image_latitude_text), String.valueOf(longitude)));
        }

        Button okButton = (Button) findViewById(R.id.upload_button);
        okButton.setOnClickListener(this);
        Button cancelButton = (Button) findViewById(R.id.cancel_button);
        cancelButton.setOnClickListener(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        bus.register(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        bus.unregister(this);
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        googleMap.setMapType(GoogleMap.MAP_TYPE_HYBRID);
        googleMap.addMarker(new MarkerOptions()
                .position(new LatLng(latitude, longitude))
                .title(cityName));
        googleMap.moveCamera(CameraUpdateFactory.newLatLng(new LatLng(latitude, longitude)));
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.upload_button:
                onUploadClicked();
                break;
            case R.id.cancel_button:
                finish();
                break;
        }
    }

    private void onUploadClicked() {
        showNotification();
        uploadFile(pathForUploading);
    }

    private void uploadFile(String fileUri) {
        finish();
        new UploadFileTask(this, DropboxClient.files()).execute(fileUri, cityName);
    }


    private void showNotification() {
        Intent resultIntent = new Intent(this, HomeScreenActivity.class);
        PendingIntent resultPendingIntent = PendingIntent.getActivity(this, 0, resultIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        android.support.v4.app.NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(this)
                        .setSmallIcon(R.drawable.ic_insert_drive_file)
                        .setContentTitle(getString(R.string.app_name))
                        .setProgress(0, 0, true)
                        .setContentIntent(resultPendingIntent)
                        .setContentText(getString(R.string.upload_in_progress));
        NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        notificationManager.notify(INotifier.UPLOAD_IMAGE_NOTIFICATION_ID, mBuilder.build());
    }
}
