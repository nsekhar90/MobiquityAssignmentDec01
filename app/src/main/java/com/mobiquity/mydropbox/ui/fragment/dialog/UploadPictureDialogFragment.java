package com.mobiquity.mydropbox.ui.fragment.dialog;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

import com.mobiquity.mydropbox.R;
import com.squareup.picasso.Picasso;

import java.io.File;

public class UploadPictureDialogFragment extends DialogFragment {

    private static final String KEY_PICTURE_URI = "PICTURE_URI";
    private UploadPictureDialogFragmentDialogActionListener listener;
    private String pictureName;

    public static UploadPictureDialogFragment newInstance(String picturePath) {
        UploadPictureDialogFragment uploadPictureDialogFragment = new UploadPictureDialogFragment();
        Bundle bundle = new Bundle();
        bundle.putString(KEY_PICTURE_URI, picturePath);
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
        if (getArguments() != null) {
            pictureName = getArguments().getString(KEY_PICTURE_URI);
        }
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        View dialogView = getActivity().getLayoutInflater().inflate(R.layout.dialog_fragment_upload_picture, null);

        ImageView capturedImageView = (ImageView) dialogView.findViewById(R.id.upload_picture_fragment_image);

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
