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

public class ImageFilesAdapter extends RecyclerView.Adapter<ImageFilesAdapter.MetadataViewHolder> {

    private List<DbxFiles.Metadata> filesList;
    private final Picasso picasso;
    private final FilesAdapterActionClickListener listener;
    private EmptyAdapterListener emptyAdapterListener;

    public void setFiles(List<DbxFiles.Metadata> files) {
        filesList = files;
        notifyDataSetChanged();
    }

    public ImageFilesAdapter(Picasso picasso, FilesAdapterActionClickListener listener, EmptyAdapterListener emptyAdapterListener) {
        this.picasso = picasso;
        this.listener = listener;
        this.emptyAdapterListener = emptyAdapterListener;
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
        metadataViewHolder.bind(filesList.get(i));
    }

    @Override
    public long getItemId(int position) {
        return filesList.get(position).pathLower.hashCode();
    }

    @Override
    public int getItemCount() {
        int count = filesList == null ? 0 : filesList.size();
        emptyAdapterListener.toggleEmptyView(count == 0);
        return count;
    }

    public class MetadataViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        private final TextView imageTitleTextView;
        private final ImageView imageView;
        private DbxFiles.Metadata imageMetaData;

        public MetadataViewHolder(View itemView) {
            super(itemView);
            imageView = (ImageView) itemView.findViewById(R.id.image);
            imageTitleTextView = (TextView) itemView.findViewById(R.id.text);
            itemView.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {

           if (imageMetaData instanceof DbxFiles.FileMetadata) {
                listener.onFileClicked((DbxFiles.FileMetadata) imageMetaData);
            }
        }

        public void bind(DbxFiles.Metadata item) {
            imageMetaData = item;
            imageTitleTextView.setText(imageMetaData.name);
            if (item instanceof DbxFiles.FileMetadata) {
                MimeTypeMap mime = MimeTypeMap.getSingleton();
                String ext = item.name.substring(item.name.indexOf(".") + 1);
                String type = mime.getMimeTypeFromExtension(ext);
                if (type != null && type.startsWith("image/")) {
                    picasso.load(FileThumbnailRequestHandler.buildPicassoUri((DbxFiles.FileMetadata) item))
                            .placeholder(R.drawable.ic_photo_grey_rounded)
                            .error(R.drawable.ic_photo_grey_rounded)
                            .into(imageView);
                } else {
                    picasso.load(R.drawable.ic_insert_drive_file)
                            .noFade()
                            .into(imageView);
                }
            }
        }
    }

    public interface FilesAdapterActionClickListener {

        void onFileClicked(DbxFiles.FileMetadata file);
    }

    public interface EmptyAdapterListener {

        void toggleEmptyView(boolean show);
    }
}
