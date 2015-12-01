package com.mobiquity.mydropbox.ui.fragment.dialog;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import com.mobiquity.mydropbox.R;

import java.io.File;

public class ImageOptionsDialogFragment extends DialogFragment {

    private static final String KEY_PICTURE_URI = "KEY_PICTURE_URI";
    private ImageOptionsDialogFragmentActionListener listener;
    private File pictureFile;

    public static ImageOptionsDialogFragment newInstance(File pictureFile) {
        ImageOptionsDialogFragment imageOptionsDialogFragment = new ImageOptionsDialogFragment();
        Bundle bundle = new Bundle();
        bundle.putSerializable(KEY_PICTURE_URI, pictureFile);
        imageOptionsDialogFragment.setArguments(bundle);
        return imageOptionsDialogFragment;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        try {
            listener = (ImageOptionsDialogFragmentActionListener) activity;
        } catch (ClassCastException exception) {
            throw new ClassCastException(activity.toString() + " must implement "
                    + ImageOptionsDialogFragmentActionListener.class.getSimpleName());
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            pictureFile = (File) getArguments().getSerializable(KEY_PICTURE_URI);
        }
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        View dialogView = getActivity().getLayoutInflater().inflate(R.layout.fragment_image_options, null);

        TextView shareTextView = (TextView) dialogView.findViewById(R.id.image_share);
        TextView imageViewTextView = (TextView) dialogView.findViewById(R.id.image_view);

        shareTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                listener.onShareClicked(pictureFile);
            }
        });

        imageViewTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                listener.OnViewClicked(pictureFile);
            }
        });

        return new AlertDialog.Builder(getActivity())
                .setView(dialogView)
                .setCancelable(false)
                .setView(dialogView)
                .create();
    }

    public interface ImageOptionsDialogFragmentActionListener {

        void onShareClicked(File file);

        void OnViewClicked(File file);
    }
}
