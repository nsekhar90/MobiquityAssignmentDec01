package com.mobiquity.mydropbox.ui;

import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
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
import com.mobiquity.mydropbox.ui.fragment.dialog.UploadPictureDialogFragment;
import com.squareup.otto.Bus;
import com.squareup.otto.Subscribe;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

public class HomeScreenActivity extends DropboxActivity implements View.OnClickListener,
        UploadPictureDialogFragment.UploadPictureDialogFragmentDialogActionListener {

    private static final int REQUEST_IMAGE_CAPTURE = 1;

    private String currentPhotoPath;
    private RecyclerView filesRecyclerView;
    private FilesAdapter adapter;
    private CoordinatorLayout homeScreenContainer;
    private ViewSwitcher loginScreenSwitcher;
    private Button loginButton;
    private String photoPathForPicassa;
    private Bus bus;
    private ProgressBar progressBar;

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
    }

    @Override
    protected void onResume() {
        super.onResume();
        bus.register(this);
        loginScreenSwitcher.setDisplayedChild(hasToken() ? 0 : 1);
    }

    @Override
    protected void onPause() {
        super.onPause();
        bus.unregister(this);
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
                UploadPictureDialogFragment uploadPictureDialogFragment = UploadPictureDialogFragment.newInstance(photoPathForPicassa);
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
        new UploadFileTask(this, DropboxClient.files()).execute(fileUri, currentPhotoPath);
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
        if (file != null) {
            viewFileInExternalApp(file);
        }
    }

    @Subscribe
    public void onDownloadFileFailedEvent(OnDownloadFileFailedEvent event) {
        progressBar.setVisibility(View.GONE);
        Toast.makeText(HomeScreenActivity.this,
                "An error has occurred",
                Toast.LENGTH_SHORT)
                .show();
    }

    private class FileClickListener implements  FilesAdapter.FilesAdapterActionClickListener {

        @Override
        public void onFileClicked(DbxFiles.FileMetadata file) {
            downloadFile(file);
        }
    }
}
