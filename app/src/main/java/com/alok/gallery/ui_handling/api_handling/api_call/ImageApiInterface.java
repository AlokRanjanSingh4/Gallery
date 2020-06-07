package com.alok.gallery.ui_handling.api_handling.api_call;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.alok.gallery.ui_handling.api_handling.pojo.ImageData;
import com.alok.gallery.ui_handling.api_handling.retrofit.APIInterface;

import retrofit2.Call;

public interface ImageApiInterface {

    interface onApiFinishedListener {
        @Nullable
        View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                          @Nullable Bundle savedInstanceState);

        void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState);

        void onApiSuccess(ImageData response, String searchTerm);

        void onApiFailure(String message);
    }

    void cancelRequest(Context mContext, ImageApiInterface.onApiFinishedListener listener);

    void imageSearchAPI(Context mContext, String searchTerm, int offset, int count, ImageApiInterface.onApiFinishedListener listener, APIInterface api,
                        Call<ImageData> call);
}