package com.alok.gallery.ui_handling.fragment;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.alok.gallery.R;
import com.alok.gallery.ui_handling.CommonFunctions;
import com.alok.gallery.ui_handling.api_handling.pojo.Value;
import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;


public class ImageFragment extends Fragment {

    private static final String KEY_IMAGE_RES = "app.test.myassignment.key.imageRes";
    private static final String KEY_IMAGE_BYTE = "app.test.myassignment.key.imageByte";

    public static ImageFragment newInstance(Value drawableRes, int viewFrom) {
        ImageFragment fragment = new ImageFragment();
        Bundle argument = new Bundle();
        argument.putString(KEY_IMAGE_RES, drawableRes.getThumbnailUrl());
        argument.putByteArray(KEY_IMAGE_BYTE, drawableRes.getImageByteArray());
        argument.putInt(CommonFunctions.viewFrom, viewFrom);
        fragment.setArguments(argument);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.fragment_image, container, false);

        Bundle arguments = getArguments();
        String imageRes = arguments.getString(KEY_IMAGE_RES);
        int viewFrom = arguments.getInt(CommonFunctions.viewFrom);

        view.findViewById(R.id.image).setTransitionName(imageRes);
        if (viewFrom == CommonFunctions.Online) {
            Picasso.with(getContext())
                    .load(imageRes)
                    .into((ImageView) view.findViewById(R.id.imagee), new Callback() {
                        @Override
                        public void onSuccess() {
                            getParentFragment().startPostponedEnterTransition();
                        }

                        @Override
                        public void onError() {
                            getParentFragment().startPostponedEnterTransition();
                        }
                    });
        } else {
            byte[] byteArray = arguments.getByteArray(KEY_IMAGE_BYTE);
            Bitmap bmp = BitmapFactory.decodeByteArray(byteArray, 0, byteArray.length);
            Drawable bitmapDrawable = new BitmapDrawable(getContext().getResources(), bmp);
            ((ImageView) view.findViewById(R.id.image)).setImageDrawable(bitmapDrawable);
            getParentFragment().startPostponedEnterTransition();
        }
        return view;
    }
}
