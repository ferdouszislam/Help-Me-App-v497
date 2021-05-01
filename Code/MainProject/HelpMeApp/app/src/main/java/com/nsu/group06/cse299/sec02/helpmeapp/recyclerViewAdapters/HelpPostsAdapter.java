package com.nsu.group06.cse299.sec02.helpmeapp.recyclerViewAdapters;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.nsu.group06.cse299.sec02.helpmeapp.R;
import com.nsu.group06.cse299.sec02.helpmeapp.database.Database;
import com.nsu.group06.cse299.sec02.helpmeapp.database.firebase_database.FirebaseRDBApiEndPoint;
import com.nsu.group06.cse299.sec02.helpmeapp.database.firebase_database.FirebaseRDBRealtime;
import com.nsu.group06.cse299.sec02.helpmeapp.models.HelpPost;
import com.nsu.group06.cse299.sec02.helpmeapp.utils.NosqlDatabasePathUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

public class HelpPostsAdapter extends RecyclerView.Adapter<HelpPostsAdapter.ViewHolder> {

    private static final String TAG = "HPA-debug";

    // calling activity/fragment
    private Context mContext;

    // calling activity/fragment callbacks
    private CallerCallbacks mCallerCallbacks;

    // model
    private ArrayList<HelpPost> mHelpPosts;

    public HelpPostsAdapter(Context mContext, CallerCallbacks mCallerCallbacks, ArrayList<HelpPost> helpPosts) {
        this.mContext = mContext;
        this.mCallerCallbacks = mCallerCallbacks;

        // help posts are expected to be in-> posts with earlier date first
        // so we are reversing to get-> posts with latest date first
        Collections.reverse(helpPosts);
        this.mHelpPosts = helpPosts;

        if(!mHelpPosts.isEmpty()) mCallerCallbacks.onListNotEmpty();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.help_post_item_view, parent, false);
        return new HelpPostsAdapter.ViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {

        HelpPost helpPost = mHelpPosts.get(position);

        holder.timeTextView.setText(helpPost.getTimeStamp());

        if(helpPost.getAddress()!=null && !helpPost.getAddress().isEmpty()) {
            holder.addressTextView.setText(helpPost.getAddress());
        }
        else holder.addressTextView.setText(R.string.no_address);

        holder.contentTextView.setText(helpPost.getContent());

        if(helpPost.getPhotoURL()!=null && !helpPost.getPhotoURL().isEmpty()){

            holder.photoImageView.setVisibility(View.VISIBLE);
            Glide.with(mContext)
                    .load(helpPost.getPhotoURL())
                    .override(300, 300)
                    .fitCenter() // scale to fit entire image within ImageView
                    .error(R.drawable.ftl_image_placeholder)
                    .into(holder.photoImageView);
        }

        holder.showLocationButton.setOnClickListener(v -> mCallerCallbacks.onShowLocationClick(helpPost));
    }

    @Override
    public int getItemCount() {
        return mHelpPosts.size();
    }

    // interface to communicate with the calling Activity/Fragment
    public interface CallerCallbacks{

        void onShowLocationClick(HelpPost helpPost);
        void onListNotEmpty();
        void onError();
    }

    // Provide a direct reference to each of the views within a data item
    // Used to cache the views within the item layout for fast access
    public class ViewHolder extends RecyclerView.ViewHolder {
        // Your holder should contain a member variable
        // for any view that will be set as you render a row
        public TextView timeTextView, addressTextView, contentTextView;
        public ImageView photoImageView;
        public Button showLocationButton;

        // We also create a constructor that accepts the entire item row
        // and does the view lookups to find each subview
        public ViewHolder(View itemView) {
            // Stores the itemView in a public final member variable that can be used
            // to access the context from any ViewHolder instance.
            super(itemView);

            timeTextView = itemView.findViewById(R.id.time_helpPostItemView_TextView);
            addressTextView = itemView.findViewById(R.id.address_helpPostItemView_TextView);
            contentTextView = itemView.findViewById(R.id.content_helpPostItemView_TextView);
            photoImageView = itemView.findViewById(R.id.photo_helpPostItemView_ImageView);
            showLocationButton = itemView.findViewById(R.id.showLocation_helpPostItemView_Button);
        }
    }
}
