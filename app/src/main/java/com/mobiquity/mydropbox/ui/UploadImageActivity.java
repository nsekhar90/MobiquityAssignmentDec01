package com.mobiquity.mydropbox.ui;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
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
import com.mobiquity.mydropbox.event.OnUploadFailedEvent;
import com.mobiquity.mydropbox.event.OnUploadSuccessfulEvent;
import com.mobiquity.mydropbox.networking.task.UploadFileTask;
import com.squareup.otto.Bus;
import com.squareup.otto.Subscribe;
import com.squareup.picasso.Picasso;

import java.io.File;

public class UploadImageActivity extends AppCompatActivity implements OnMapReadyCallback {

    private String pathForPicassa;
    private String pathForUploading;
    private double latitude;
    private double longitude;
    private String cityName;

    private Bus bus;
    private Toolbar toolbar;
    private ProgressBar progressBar;
    private RelativeLayout uploadImageContainer;

    private static final String KEY_PATH_FOR_PICASSA = "KEY_PATH_PICASSA";
    private static final String KEY_PATH_FOR_UPLOADING = "KEY_PATH_FOR_UPLOADING";
    private static final String KEY_LATITUDE = "KEY_LATITUDE";
    private static final String KEY_LONGITUDE = "KEY_LONGITUDE";
    private static final String KEY_CITY = "KEY_CITY";


    public static void start(Context context, String pathForUploading, String pathForPicassa, double latitude, double longitude, String city) {
        Intent intent = new Intent(context, UploadImageActivity.class);
        intent.putExtra(KEY_PATH_FOR_PICASSA, pathForPicassa);
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
            pathForPicassa = bundle.getString(KEY_PATH_FOR_PICASSA);
            latitude = bundle.getDouble(KEY_LATITUDE);
            longitude = bundle.getDouble(KEY_LONGITUDE);
            cityName = bundle.getString(KEY_CITY);
            pathForUploading = bundle.getString(KEY_PATH_FOR_UPLOADING);
        }

        toolbar = (Toolbar) findViewById(R.id.toolbar);
        progressBar = (ProgressBar) findViewById(R.id.download_progress_bar);
        uploadImageContainer = (RelativeLayout) findViewById(R.id.upload_image_container);

        setSupportActionBar(toolbar);

        ImageView capturedImageView = (ImageView) findViewById(R.id.upload_picture_fragment_image);

        MapFragment mapFragment = (MapFragment) getFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        File file = new File(pathForPicassa);
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

        Button okButton = (Button) findViewById(R.id.ok_button);
        Button cancelButton = (Button) findViewById(R.id.cancel_button);

        okButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onUploadClicked();
            }
        });

        cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

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

    public void onUploadClicked() {
        uploadFile(pathForUploading);
    }

    private void uploadFile(String fileUri) {
        toolbar.setTitle(R.string.uploading);
        progressBar.setVisibility(View.VISIBLE);
        new UploadFileTask(this, DropboxClient.files()).execute(fileUri, cityName);
    }

    @Subscribe
    public void onUploadCompleted(OnUploadSuccessfulEvent result) {
        resetToolbarTitle();
        Snackbar.make(uploadImageContainer, R.string.upload_successful, Snackbar.LENGTH_SHORT).show();
        finish();
    }

    @Subscribe
    public void onUploadFailedEvent(OnUploadFailedEvent event) {
        resetToolbarTitle();
        if (event.getException() != null) {
            Snackbar.make(uploadImageContainer, R.string.generic_error_message, Snackbar.LENGTH_SHORT).show();
        }
        finish();
    }

    private void resetToolbarTitle() {
        toolbar.setTitle(R.string.app_name);
        progressBar.setVisibility(View.GONE);
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        googleMap.setMapType(GoogleMap.MAP_TYPE_HYBRID);
        googleMap.addMarker(new MarkerOptions()
                .position(new LatLng(latitude, longitude))
                .title(cityName));
        googleMap.moveCamera(CameraUpdateFactory.newLatLng(new LatLng(latitude, longitude)));
    }
}
