package com.mobiquity.mydropbox.ui;

import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.webkit.MimeTypeMap;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Toast;
import android.widget.ViewSwitcher;

import com.dropbox.core.v2.DbxFiles;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationServices;
import com.mobiquity.mydropbox.Auth;
import com.mobiquity.mydropbox.DropboxApp;
import com.mobiquity.mydropbox.DropboxClient;
import com.mobiquity.mydropbox.PicassoClient;
import com.mobiquity.mydropbox.R;
import com.mobiquity.mydropbox.adapter.FilesAdapter;
import com.mobiquity.mydropbox.event.OnDataLoadFailedEvent;
import com.mobiquity.mydropbox.event.OnDataLoadedEvent;
import com.mobiquity.mydropbox.event.OnDownloadFileFailedEvent;
import com.mobiquity.mydropbox.event.OnDownloadFileSuccessEvent;
import com.mobiquity.mydropbox.event.OnUploadFailedEvent;
import com.mobiquity.mydropbox.event.OnUploadSuccessfulEvent;
import com.mobiquity.mydropbox.networking.task.DownloadFileTask;
import com.mobiquity.mydropbox.networking.task.ListFolderTask;
import com.mobiquity.mydropbox.networking.task.UploadFileTask;
import com.mobiquity.mydropbox.ui.fragment.dialog.ImageOptionsDialogFragment;
import com.mobiquity.mydropbox.ui.fragment.dialog.UploadPictureDialogFragment;
import com.squareup.otto.Bus;
import com.squareup.otto.Subscribe;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class HomeScreenActivity extends DropboxActivity implements View.OnClickListener,
        UploadPictureDialogFragment.UploadPictureDialogFragmentDialogActionListener,
        GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener,
        ImageOptionsDialogFragment.ImageOptionsDialogFragmentActionListener {

    private static final int REQUEST_IMAGE_CAPTURE = 1;
    // Request code to use when launching the resolution activity
    private static final int REQUEST_RESOLVE_ERROR = 1001;

    private String currentPhotoPath;
    private RecyclerView filesRecyclerView;
    private FilesAdapter adapter;
    private CoordinatorLayout homeScreenContainer;
    private ViewSwitcher loginScreenSwitcher;
    private Button loginButton;
    private String photoPathForPicassa;
    private Bus bus;
    private ProgressBar progressBar;

    private GoogleApiClient googleApiClient;
    private boolean resolvingGooglePlayConnectionError = false;
    private double lastKnownLatitude = 0;
    private double lastKnownLongitude = 0;
    private String lastKnownCity;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        bus = DropboxApp.getBus();
        setContentView(R.layout.activity_home_screen);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton takePictureButton = (FloatingActionButton) findViewById(R.id.fab);
        takePictureButton.setOnClickListener(this);

        filesRecyclerView = (RecyclerView) findViewById(R.id.images_recycler_view);
        homeScreenContainer = (CoordinatorLayout) findViewById(R.id.home_screen_container);
        loginScreenSwitcher = (ViewSwitcher) findViewById(R.id.login_view_switcher);
        progressBar = (ProgressBar) findViewById(R.id.download_progress_bar);

        filesRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        loginButton = (Button) findViewById(R.id.login_button);
        loginButton.setOnClickListener(this);

        loginScreenSwitcher.setDisplayedChild(hasToken() ? 0 : 1);

        googleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
    }

    @Override
    protected void onResume() {
        super.onResume();
        bus.register(this);
        loginScreenSwitcher.setDisplayedChild(hasToken() ? 0 : 1);
        googleApiClient.connect();
    }

    @Override
    protected void onPause() {
        super.onPause();
        bus.unregister(this);
        googleApiClient.disconnect();
    }

    @Override
    protected void loadData() {
        progressBar.setVisibility(View.VISIBLE);
        adapter = new FilesAdapter(PicassoClient.getPicasso(), new FileClickListener());
        new ListFolderTask(DropboxClient.files()).execute("");
        filesRecyclerView.setAdapter(adapter);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_home_screen, menu);
        return true;
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.fab:
                dispatchTakePictureIntent();
                break;
            case R.id.login_button:
                Auth.startOAuth2Authentication(this, DropboxApp.DROPBOX_APP_KEY);
                break;
        }
    }

    private void downloadFile(DbxFiles.FileMetadata file) {
        progressBar.setVisibility(View.VISIBLE);
        new DownloadFileTask(HomeScreenActivity.this, DropboxClient.files()).execute(file);
    }

    private void viewFileInExternalApp(File result) {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        MimeTypeMap mime = MimeTypeMap.getSingleton();
        String ext = result.getName().substring(result.getName().indexOf(".") + 1);
        String type = mime.getMimeTypeFromExtension(ext);

        intent.setDataAndType(Uri.fromFile(result), type);

        // Check for a handler first to avoid a crash
        PackageManager manager = getPackageManager();
        List<ResolveInfo> resolveInfo = manager.queryIntentActivities(intent, 0);
        if (resolveInfo.size() > 0) {
            startActivity(intent);
        }
    }

    private void dispatchTakePictureIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            File photoFile = null;
            try {
                photoFile = createImageFile();
            } catch (IOException ex) {
                Snackbar.make(homeScreenContainer, R.string.generic_error_message, Snackbar.LENGTH_SHORT).show();
            }
            if (photoFile != null) {
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(photoFile));
                startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
            }
        } else {
            Snackbar.make(homeScreenContainer, R.string.no_camera_app_error_message, Snackbar.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_IMAGE_CAPTURE) {
            Uri uri = null;
            if (data != null) {
                uri = data.getData();
            }
            if (uri == null && currentPhotoPath != null) {
                uri = Uri.fromFile(new File(currentPhotoPath));
                UploadPictureDialogFragment uploadPictureDialogFragment =
                        UploadPictureDialogFragment.newInstance(photoPathForPicassa, lastKnownLatitude, lastKnownLongitude, lastKnownCity);
                uploadPictureDialogFragment.show(getFragmentManager(), "UPLOAD_DIALOG_FRAGMENT_TAG");
            }
        }
    }

    private File createImageFile() throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(
                imageFileName,
                ".png",
                storageDir
        );

        currentPhotoPath = "file:" + image.getAbsolutePath();
        photoPathForPicassa = image.getAbsolutePath();
        return image;
    }

    @Override
    public void onUploadClicked(String currentPhotoPath) {
        if (hasToken()) {
            Log.e("findMe", "calling upload file");
            uploadFile(this.currentPhotoPath);
        } else {
            Auth.startOAuth2Authentication(this, DropboxApp.DROPBOX_APP_KEY);
        }
    }

    private void uploadFile(String fileUri) {
        progressBar.setVisibility(View.VISIBLE);
        new UploadFileTask(this, DropboxClient.files()).execute(fileUri, lastKnownCity);
    }

    @Subscribe
    public void onUploadCompleted(OnUploadSuccessfulEvent result) {
        progressBar.setVisibility(View.GONE);
        Snackbar.make(homeScreenContainer, R.string.upload_successful, Snackbar.LENGTH_SHORT).show();
    }

    @Subscribe
    public void onUploadFailedEvent(OnUploadFailedEvent event) {
        if (event.getException() != null) {
            Snackbar.make(homeScreenContainer, R.string.generic_error_message, Snackbar.LENGTH_SHORT).show();
        }
        progressBar.setVisibility(View.GONE);
    }


    @Subscribe
    public void onDataLoadedEvent(OnDataLoadedEvent event) {
        progressBar.setVisibility(View.GONE);
        adapter.setFiles(event.getResult().entries);
    }

    @Subscribe
    public void onDataLoadFailedEvent(OnDataLoadFailedEvent event) {
        progressBar.setVisibility(View.GONE);
        Toast.makeText(HomeScreenActivity.this,
                "An error has occurred",
                Toast.LENGTH_SHORT)
                .show();
    }

    @Subscribe
    public void onDownloadFileSuccessEvent(OnDownloadFileSuccessEvent event) {
        progressBar.setVisibility(View.GONE);
        File file = event.getFile();
        ImageOptionsDialogFragment imageOptionsDialogFragment = ImageOptionsDialogFragment.newInstance(file);
        imageOptionsDialogFragment.show(getFragmentManager(), "ImageOptionsDialogFragment_TAG");
        }

    @Subscribe
    public void onDownloadFileFailedEvent(OnDownloadFileFailedEvent event) {
        progressBar.setVisibility(View.GONE);
        Toast.makeText(HomeScreenActivity.this,
                "An error has occurred",
                Toast.LENGTH_SHORT)
                .show();
    }

    @Override
    public void onConnected(Bundle bundle) {
        Location lastKnownLocation = LocationServices.FusedLocationApi.getLastLocation(googleApiClient);
        if (lastKnownLocation != null) {
            lastKnownLongitude = lastKnownLocation.getLongitude();
            lastKnownLatitude = lastKnownLocation.getLatitude();

            Geocoder geocoder = new Geocoder(getBaseContext(), Locale.getDefault());
            List<Address> addresses = null;
            try {
                addresses = geocoder.getFromLocation(lastKnownLatitude, lastKnownLongitude, 1);
            } catch (IOException e) {
                e.printStackTrace();
            }
            if (addresses != null) {
                if (addresses.size() > 0) {
                    lastKnownCity = addresses.get(0).getLocality();
                }
            }
        }
    }

    @Override
    public void onConnectionSuspended(int i) {
        Snackbar.make(homeScreenContainer, R.string.unable_to_connect_to_play_services_message, Snackbar.LENGTH_SHORT).show();
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Snackbar.make(homeScreenContainer, R.string.unable_to_connect_to_play_services_message, Snackbar.LENGTH_SHORT).show();
        if (resolvingGooglePlayConnectionError) {
            return;
        } else if (connectionResult.hasResolution()) {
            try {
                resolvingGooglePlayConnectionError = true;
                connectionResult.startResolutionForResult(this, REQUEST_RESOLVE_ERROR);
            } catch (IntentSender.SendIntentException e) {
                googleApiClient.connect();
            }
        } else {
            Snackbar.make(homeScreenContainer, R.string.unable_to_connect_to_play_services_message, Snackbar.LENGTH_SHORT).show();
            resolvingGooglePlayConnectionError = false;
        }

    }

    @Override
    public void onShareClicked(File file) {
        if (file != null) {
            Intent shareIntent = new Intent();
            shareIntent.setAction(Intent.ACTION_SEND);
            shareIntent.putExtra(Intent.EXTRA_STREAM, Uri.parse(file.toString()));
            shareIntent.setType("image/jpeg");
            List<ResolveInfo> resInfo = getPackageManager().queryIntentActivities(shareIntent, 0);
            if (resInfo != null) {
                for (ResolveInfo resolveInfo : resInfo) {
                    if (resolveInfo.activityInfo.packageName.contains("facebook")) {
                        shareIntent.setPackage(resolveInfo.activityInfo.packageName);
                        startActivity(Intent.createChooser(shareIntent, "Share"));
                        break;
                    }
                }
            } else {
                Snackbar.make(homeScreenContainer, R.string.install_facebook_app, Snackbar.LENGTH_SHORT)
                        .setAction(R.string.install, new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                Intent intent = new Intent(Intent.ACTION_VIEW);
                                intent.setData(Uri.parse(String.format(getString(R.string.play_store_uri_format), "com.facebook.katana")));
                            }
                        }).show();
            }
        }
    }

    @Override
    public void OnViewClicked(File file) {
        viewFileInExternalApp(file);
    }

    private class FileClickListener implements FilesAdapter.FilesAdapterActionClickListener {

        @Override
        public void onFileClicked(DbxFiles.FileMetadata file) {
            downloadFile(file);
        }
    }
}
