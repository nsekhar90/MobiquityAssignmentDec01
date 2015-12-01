package com.mobiquity.mydropbox.ui.fragment.dialog;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.mobiquity.mydropbox.R;
import com.squareup.picasso.Picasso;

import java.io.File;

public class UploadPictureDialogFragment extends DialogFragment {

    private static final String KEY_PICTURE_URI = "PICTURE_URI";
    private static final String KEY_LATITUDE = "KEY_LATITUDE";
    private static final String KEY_LONGITUDE = "KEY_LONGITUDE";
    private static final String KEY_CITY = "KEY_CITY";

    private UploadPictureDialogFragmentDialogActionListener listener;
    private String pictureName;
    private double latitude;
    private double longitude;
    private String cityName;

    public static UploadPictureDialogFragment newInstance(String picturePath, double latitude, double longitude, String city) {
        UploadPictureDialogFragment uploadPictureDialogFragment = new UploadPictureDialogFragment();
        Bundle bundle = new Bundle();
        bundle.putString(KEY_PICTURE_URI, picturePath);
        bundle.putDouble(KEY_LATITUDE, latitude);
        bundle.putDouble(KEY_LONGITUDE, longitude);
        bundle.putString(KEY_CITY, city);
        uploadPictureDialogFragment.setArguments(bundle);
        uploadPictureDialogFragment.setStyle(DialogFragment.STYLE_NORMAL, R.style.UploadDialogTheme);
        return uploadPictureDialogFragment;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        try {
            listener = (UploadPictureDialogFragmentDialogActionListener) activity;
        } catch (ClassCastException exception) {
            throw new ClassCastException(activity.toString() + " must implement "
                    + UploadPictureDialogFragmentDialogActionListener.class.getSimpleName());
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle bundle = getArguments();
        if (bundle != null) {
            pictureName = bundle.getString(KEY_PICTURE_URI);
            cityName = bundle.getString(KEY_CITY);
            latitude = bundle.getDouble(KEY_LATITUDE);
            longitude = bundle.getDouble(KEY_LONGITUDE);
        }
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        View dialogView = getActivity().getLayoutInflater().inflate(R.layout.dialog_fragment_upload_picture, null);

        ImageView capturedImageView = (ImageView) dialogView.findViewById(R.id.upload_picture_fragment_image);

        TextView latitudeTextView = (TextView) dialogView.findViewById(R.id.image_latitude_textview);
        TextView longitudeTextView = (TextView) dialogView.findViewById(R.id.image_longitude_textview);
        TextView cityTextView = (TextView) dialogView.findViewById(R.id.image_city_textview);

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

        File file = new File(pictureName);
        Picasso.with(getActivity()).setIndicatorsEnabled(true);
        Picasso.with(getActivity())
                .load(file)
                .fit()
                .centerCrop()
                .into(capturedImageView);

        Button okButton = (Button) dialogView.findViewById(R.id.ok_button);
        Button cancelButton = (Button) dialogView.findViewById(R.id.cancel_button);

        okButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                listener.onUploadClicked(pictureName);
                dismiss();
            }
        });

        cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dismiss();
            }
        });

        return new AlertDialog.Builder(getActivity())
                .setView(dialogView)
                .setCancelable(false)
                .setView(dialogView)
                .create();
    }


    public interface UploadPictureDialogFragmentDialogActionListener {

        void onUploadClicked(String currentPhotoPath);

    }
}
