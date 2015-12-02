package com.mobiquity.mydropbox.ui;

import android.app.NotificationManager;
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
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.webkit.MimeTypeMap;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ViewSwitcher;

import com.dropbox.core.v2.DbxFiles;
import com.flipboard.bottomsheet.BottomSheetLayout;
import com.flipboard.bottomsheet.commons.MenuSheetView;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationServices;
import com.mobiquity.mydropbox.Auth;
import com.mobiquity.mydropbox.DropboxApp;
import com.mobiquity.mydropbox.DropboxClient;
import com.mobiquity.mydropbox.PicassoClient;
import com.mobiquity.mydropbox.R;
import com.mobiquity.mydropbox.adapter.ImageFilesAdapter;
import com.mobiquity.mydropbox.event.OnDownloadFileFailedEvent;
import com.mobiquity.mydropbox.event.OnDownloadFileSuccessEvent;
import com.mobiquity.mydropbox.event.OnImageFilesLoadFailedEvent;
import com.mobiquity.mydropbox.event.OnImageFilesLoadedEvent;
import com.mobiquity.mydropbox.event.OnUploadFailedEvent;
import com.mobiquity.mydropbox.event.OnUploadSuccessfulEvent;
import com.mobiquity.mydropbox.networking.task.DownloadFileTask;
import com.mobiquity.mydropbox.networking.task.ListItemsInFolderTask;
import com.mobiquity.mydropbox.notification.INotifier;
import com.squareup.otto.Bus;
import com.squareup.otto.Subscribe;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class HomeScreenActivity extends DropboxActivity implements View.OnClickListener,
        GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener,
        ImageFilesAdapter.EmptyAdapterListener, ImageFilesAdapter.FilesAdapterActionClickListener {

    private static final int REQUEST_IMAGE_CAPTURE = 1;
    // Request code to use when launching the resolution activity
    private static final int REQUEST_RESOLVE_ERROR = 1001;

    private static final String KEY_PHOTO_PATH_FOR_PICASSO = "KEY_PHOTO_PATH_FOR_PICASSO";
    private static final String KEY_PHOTO_PATH_FOR_UPLOADING = "KEY_PHOTO_PATH_FOR_UPLOADING";

    private RecyclerView filesRecyclerView;
    private ImageFilesAdapter adapter;
    private CoordinatorLayout homeScreenContainer;
    private ViewSwitcher loginScreenSwitcher;
    private ViewSwitcher loadingViewSwitcher;
    private Bus bus;
    private Toolbar toolbar;

    private String currentPhotoPathForUploading;
    private String currentPhotoPathForPicasso;

    private GoogleApiClient googleApiClient;
    private boolean resolvingGooglePlayConnectionError = false;
    private double lastKnownLatitude = 0;
    private double lastKnownLongitude = 0;
    private String lastKnownCity;
    private LinearLayout emptyViewForRecyclerView;
    private BottomSheetLayout bottomSheetLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        bus = DropboxApp.getBus();
        setContentView(R.layout.activity_home_screen);
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton takePictureButton = (FloatingActionButton) findViewById(R.id.fab);
        takePictureButton.setOnClickListener(this);

        filesRecyclerView = (RecyclerView) findViewById(R.id.images_recycler_view);
        homeScreenContainer = (CoordinatorLayout) findViewById(R.id.home_screen_container);
        loginScreenSwitcher = (ViewSwitcher) findViewById(R.id.login_view_switcher);
        emptyViewForRecyclerView = (LinearLayout) findViewById(R.id.empty_view_for_recycler_view);

        filesRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        Button loginButton = (Button) findViewById(R.id.login_button);
        loginButton.setOnClickListener(this);

        bottomSheetLayout = (BottomSheetLayout) findViewById(R.id.bottom_sheet);
        loadingViewSwitcher = (ViewSwitcher) findViewById(R.id.loading_view_switcher);

        loadingViewSwitcher.setDisplayedChild(hasToken() ? 0 : 1);
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
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_home_screen, menu);
        return true;
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(KEY_PHOTO_PATH_FOR_PICASSO, currentPhotoPathForPicasso);
        outState.putString(KEY_PHOTO_PATH_FOR_UPLOADING, currentPhotoPathForUploading);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        if (savedInstanceState != null) {
            currentPhotoPathForPicasso = savedInstanceState.getString(KEY_PHOTO_PATH_FOR_PICASSO);
            currentPhotoPathForUploading = savedInstanceState.getString(KEY_PHOTO_PATH_FOR_UPLOADING);
        }
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

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            Uri uri = null;
            if (data != null) {
                uri = data.getData();
            }
            if (uri == null && currentPhotoPathForUploading != null) {
                UploadImageActivity.start(this, currentPhotoPathForUploading, currentPhotoPathForPicasso, lastKnownLatitude, lastKnownLongitude, lastKnownCity);
            }
        }
    }

    @Override
    protected void loadData() {
        loadingViewSwitcher.setDisplayedChild(0);
        adapter = new ImageFilesAdapter(PicassoClient.getPicasso(), this, this);
        new ListItemsInFolderTask(DropboxClient.files()).execute("");
        filesRecyclerView.setAdapter(adapter);
    }

    @Subscribe
    public void onDataLoadedEvent(OnImageFilesLoadedEvent event) {
        switchToRecyclerView();
        //update only if size of list changes
        if (adapter.getItemCount() != event.getResult().entries.size()) {
            adapter.setFiles(event.getResult().entries);
        }
    }

    @Subscribe
    public void onDataLoadFailedEvent(OnImageFilesLoadFailedEvent event) {
        switchToRecyclerView();
        Snackbar.make(homeScreenContainer, R.string.generic_error_message, Snackbar.LENGTH_SHORT).show();
    }

    @Subscribe
    public void onDownloadFileSuccessEvent(OnDownloadFileSuccessEvent event) {
        switchToRecyclerView();
        File file = event.getFileContainer().getFile();
        if (event.getFileContainer().isShare()) {
            onShareClicked(file);
        } else {
            viewFileInExternalApp(file);
        }
    }

    @Subscribe
    public void onDownloadFileFailedEvent(OnDownloadFileFailedEvent event) {
        switchToRecyclerView();
        Snackbar.make(homeScreenContainer, R.string.generic_error_message, Snackbar.LENGTH_SHORT).show();
    }

    @Subscribe
    public void onUploadCompleted(OnUploadSuccessfulEvent result) {
        NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        notificationManager.cancel(INotifier.UPLOAD_IMAGE_NOTIFICATION_ID);
        loadData();
        Snackbar.make(homeScreenContainer, R.string.upload_successful, Snackbar.LENGTH_SHORT).show();
    }

    @Subscribe
    public void onUploadFailedEvent(OnUploadFailedEvent event) {
        NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        notificationManager.cancel(INotifier.UPLOAD_IMAGE_NOTIFICATION_ID);
        Snackbar.make(homeScreenContainer, R.string.generic_error_message, Snackbar.LENGTH_SHORT).show();
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

    private void onShareClicked(File file) {
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
    public void toggleEmptyView(boolean show) {
        emptyViewForRecyclerView.setVisibility(show ? View.VISIBLE : View.GONE);
    }

    @Override
    public void onFileClicked(DbxFiles.FileMetadata file) {
        showMenuSheet(file);
    }

    private void switchToRecyclerView() {
        toolbar.setTitle(R.string.app_name);
        loadingViewSwitcher.setDisplayedChild(1);
    }

    private void downloadFile(DbxFiles.FileMetadata file, boolean isShareSelected) {
        toolbar.setTitle(R.string.downloading);
        loadingViewSwitcher.setDisplayedChild(1);
        new DownloadFileTask(HomeScreenActivity.this, DropboxClient.files()).execute(file, isShareSelected);
    }

    private File createImageFile() throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = timeStamp + "_";
        File storageDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(
                imageFileName,
                ".png",
                storageDir
        );

        currentPhotoPathForUploading = "file:" + image.getAbsolutePath();
        currentPhotoPathForPicasso = image.getAbsolutePath();
        return image;
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

    private void showMenuSheet(final DbxFiles.FileMetadata metadata) {
        MenuSheetView menuSheetView =
                new MenuSheetView(this, MenuSheetView.MenuType.LIST, R.string.image_options_bottom_sheet_title, new MenuSheetView.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        if (bottomSheetLayout.isSheetShowing()) {
                            bottomSheetLayout.dismissSheet();
                        }
                        switch (item.getItemId()) {
                            case R.id.image_view:
                                downloadFile(metadata, false);
                                return true;
                            case R.id.image_share_on_fb:
                                downloadFile(metadata, true);
                                return true;
                        }
                        return false;
                    }
                });
        menuSheetView.inflateMenu(R.menu.image_options);
        bottomSheetLayout.showWithSheetView(menuSheetView);
    }

}
