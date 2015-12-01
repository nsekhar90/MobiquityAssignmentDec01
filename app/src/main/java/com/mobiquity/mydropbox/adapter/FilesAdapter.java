package com.mobiquity.mydropbox.adapter;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.MimeTypeMap;
import android.widget.ImageView;
import android.widget.TextView;

import com.dropbox.core.v2.DbxFiles;
import com.mobiquity.mydropbox.FileThumbnailRequestHandler;
import com.mobiquity.mydropbox.R;
import com.squareup.picasso.Picasso;

import java.util.List;

public class FilesAdapter extends RecyclerView.Adapter<FilesAdapter.MetadataViewHolder> {

    private List<DbxFiles.Metadata> mFiles;
    private final Picasso picasso;
    private final FilesAdapterActionClickListener listener;

    public void setFiles(List<DbxFiles.Metadata> files) {
        mFiles = files;
        notifyDataSetChanged();
    }

    public FilesAdapter(Picasso picasso, FilesAdapterActionClickListener listener) {
        this.picasso = picasso;
        this.listener = listener;
    }

    @Override
    public MetadataViewHolder onCreateViewHolder(ViewGroup viewGroup, int i) {
        Context context = viewGroup.getContext();
        View view = LayoutInflater.from(context)
                .inflate(R.layout.row_image_item, viewGroup, false);
        return new MetadataViewHolder(view);
    }

    @Override
    public void onBindViewHolder(MetadataViewHolder metadataViewHolder, int i) {
        metadataViewHolder.bind(mFiles.get(i));
    }

    @Override
    public long getItemId(int position) {
        return mFiles.get(position).pathLower.hashCode();
    }

    @Override
    public int getItemCount() {
        return mFiles == null ? 0 : mFiles.size();
    }

    public class MetadataViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        private final TextView mTextView;
        private final ImageView mImageView;
        private DbxFiles.Metadata mItem;

        public MetadataViewHolder(View itemView) {
            super(itemView);
            mImageView = (ImageView) itemView.findViewById(R.id.image);
            mTextView = (TextView) itemView.findViewById(R.id.text);
            itemView.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {

           if (mItem instanceof DbxFiles.FileMetadata) {
                listener.onFileClicked((DbxFiles.FileMetadata) mItem);
            }
        }

        public void bind(DbxFiles.Metadata item) {
            mItem = item;
            mTextView.setText(mItem.name);

            // Load based on file path
            // Prepending a scheme to get it to
            // be picked up by DropboxPicassoRequestHandler

            if (item instanceof DbxFiles.FileMetadata) {
                MimeTypeMap mime = MimeTypeMap.getSingleton();
                String ext = item.name.substring(item.name.indexOf(".") + 1);
                String type = mime.getMimeTypeFromExtension(ext);
                if (type != null && type.startsWith("image/")) {
                    picasso.load(FileThumbnailRequestHandler.buildPicassoUri((DbxFiles.FileMetadata) item))
                            .placeholder(R.drawable.ic_photo_grey_600_36dp)
                            .error(R.drawable.ic_photo_grey_600_36dp)
                            .into(mImageView);
                } else {
                    picasso.load(R.drawable.ic_insert_drive_file_blue_36dp)
                            .noFade()
                            .into(mImageView);
                }
            } else if (item instanceof DbxFiles.FolderMetadata) {
                picasso.load(R.drawable.ic_folder_blue_36dp)
                        .noFade()
                        .into(mImageView);
            }
        }
    }

    public interface FilesAdapterActionClickListener {

        void onFileClicked(DbxFiles.FileMetadata file);
    }
}
