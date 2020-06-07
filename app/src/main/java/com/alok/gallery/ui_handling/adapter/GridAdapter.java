package com.alok.gallery.ui_handling.adapter;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.transition.TransitionSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;

import com.alok.gallery.R;
import com.alok.gallery.ui_handling.CommonFunctions;
import com.alok.gallery.ui_handling.MainActivity;
import com.alok.gallery.ui_handling.api_handling.pojo.Value;
import com.alok.gallery.ui_handling.fragment.ImagePagerFragment;
import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;

import java.io.Serializable;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;


public class GridAdapter extends RecyclerView.Adapter<GridAdapter.ImageViewHolder> {
    List<Value> arrayImages;
    int gridWidth;
    int viewFrom;
    Context mContext;

    private interface ViewHolderListener {

        void onLoadCompleted(ImageView view, int adapterPosition);

        void onItemClicked(View view, int adapterPosition, List<Value> values, int viewFrom);
    }

    private final ViewHolderListener viewHolderListener;

    public GridAdapter(Context mContext, Fragment fragment, List<Value> arrayImages, int gridWidth, int viewFrom) {
        this.gridWidth = gridWidth;
        this.mContext = mContext;
        this.viewFrom = viewFrom;
        this.arrayImages = arrayImages;
        this.viewHolderListener = new ViewHolderListenerImpl(fragment);
    }

    @Override
    public ImageViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.image_card, parent, false);
        return new ImageViewHolder(mContext, view, viewHolderListener, arrayImages, gridWidth, viewFrom);
    }

    @Override
    public void onBindViewHolder(ImageViewHolder holder, int position) {
        holder.onBind();
    }

    @Override
    public int getItemCount() {
        return arrayImages.size();
    }

    private static class ViewHolderListenerImpl implements ViewHolderListener {

        private Fragment fragment;
        private AtomicBoolean enterTransitionStarted;

        ViewHolderListenerImpl(Fragment fragment) {
            this.fragment = fragment;
            this.enterTransitionStarted = new AtomicBoolean();
        }

        @Override
        public void onLoadCompleted(ImageView view, int position) {
            if (MainActivity.currentPosition != position) {
                return;
            }
            if (enterTransitionStarted.getAndSet(true)) {
                return;
            }
            fragment.startPostponedEnterTransition();
        }

        @Override
        public void onItemClicked(View view, int position, List<Value> values, int viewFrom) {
            MainActivity.currentPosition = position;
            ((TransitionSet) fragment.getExitTransition()).excludeTarget(view, true);

            Fragment imagePagerFragment = new Fragment();
            Bundle bundle = new Bundle();
            bundle.putSerializable(CommonFunctions.imagesData, (Serializable) values);
            bundle.putInt(CommonFunctions.viewFrom, viewFrom);
            imagePagerFragment.setArguments(bundle);

            ImageView transitioningView = view.findViewById(R.id.card_image);
            fragment.getFragmentManager()
                    .beginTransaction()
                    .setReorderingAllowed(true)
                    .addSharedElement(transitioningView, transitioningView.getTransitionName())
                    .replace(R.id.fragment_container, imagePagerFragment, ImagePagerFragment.class
                            .getSimpleName())
                    .addToBackStack(null)
                    .commit();
        }
    }
    static class ImageViewHolder extends RecyclerView.ViewHolder implements
            View.OnClickListener {
        List<Value> arrayImages;
        int gridWidth;
        int viewFrom;
        Context mContext;
        private final ImageView image;
        private final ViewHolderListener viewHolderListener;

        ImageViewHolder(Context mContext, View itemView,
                        ViewHolderListener viewHolderListener, List<Value> arrayImages, int gridWidth, int viewFrom) {
            super(itemView);
            this.gridWidth = gridWidth;
            this.mContext = mContext;
            this.viewFrom = viewFrom;
            this.arrayImages = arrayImages;
            this.image = itemView.findViewById(R.id.card_image);
            this.viewHolderListener = viewHolderListener;
            itemView.findViewById(R.id.card_view).setOnClickListener(this);
        }

        void onBind() {
            int adapterPosition = getAdapterPosition();
            // Set Image from URL
            setImage(adapterPosition);
            //Set layout params to adjust square image for dynamic column changes
            image.setLayoutParams(new FrameLayout.LayoutParams(gridWidth, gridWidth));
            // Set the string value of the image resource as the unique transition name for the view.
            image.setTransitionName(arrayImages.get(adapterPosition).getThumbnailUrl());
        }

        void setImage(final int adapterPosition) {
            if (viewFrom == CommonFunctions.Online) {
                //Set image from URL
                Picasso.with(mContext)
                        .load(arrayImages.get(adapterPosition).getThumbnailUrl())
                        .into(image, new Callback() {
                            @Override
                            public void onSuccess() {
                                viewHolderListener.onLoadCompleted(image, adapterPosition);
                            }

                            @Override
                            public void onError() {
                                viewHolderListener.onLoadCompleted(image, adapterPosition);
                            }
                        });
            } else {
                // Set image from local DB
                byte[] byteArray = arrayImages.get(adapterPosition).getImageByteArray();
                Bitmap bmp = BitmapFactory.decodeByteArray(byteArray, 0, byteArray.length);
                image.setImageBitmap(bmp);
                viewHolderListener.onLoadCompleted(image, adapterPosition);
            }
        }

        @Override
        public void onClick(View view) {
            // Let the listener start the ImagePagerFragment.
            viewHolderListener.onItemClicked(view, getAdapterPosition(), arrayImages, viewFrom);
        }
    }

}