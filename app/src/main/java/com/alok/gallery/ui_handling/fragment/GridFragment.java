package com.alok.gallery.ui_handling.fragment;

import android.app.Activity;
import android.app.SharedElementCallback;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.TextUtils;
import android.transition.TransitionInflater;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnLayoutChangeListener;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.alok.gallery.R;
import com.alok.gallery.ui_handling.CommonFunctions;
import com.alok.gallery.ui_handling.MainActivity;
import com.alok.gallery.ui_handling.adapter.GridAdapter;
import com.alok.gallery.ui_handling.api_handling.api_call.ImageApiInterface;
import com.alok.gallery.ui_handling.api_handling.api_call.ImageSearchAPI;
import com.alok.gallery.ui_handling.api_handling.pojo.ImageData;
import com.alok.gallery.ui_handling.api_handling.pojo.Value;
import com.alok.gallery.ui_handling.api_handling.retrofit.APIClient;
import com.alok.gallery.ui_handling.api_handling.retrofit.APIInterface;
import com.alok.gallery.ui_handling.db_handling.DatabaseHelper;
import com.google.android.material.snackbar.Snackbar;
import com.lsjwzh.widget.materialloadingprogressbar.CircleProgressBar;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import retrofit2.Call;


public class GridFragment extends Fragment implements ImageApiInterface.onApiFinishedListener {
    private View view;
    private EditText edt_search_term;
    private RecyclerView recyclerView;
    private CircleProgressBar cir_loader;
    public Activity mContext;
    public LinearLayout loader;
    public APIInterface api;
    public Call<ImageData> call;

    private int numberOfColumns = 2;
    private int offset = 0;
    private int scrollPosition = -1;
    private int countOfRecords = 20;
    private int screenWidth;
    private String searchTerm = null;
    private boolean apiCalling = false;
    private boolean bottomFound = false;
    private int viewFrom = CommonFunctions.Online;
    private int TypeEditText = 1;
    private int TypePagination = 2;

    private List imagesList;
    private ImageApiInterface imageSearchAPI;
    private DatabaseHelper databaseHelper;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        imagesList = new ArrayList();
        mContext = getActivity();
        databaseHelper = new DatabaseHelper(mContext);

        screenWidth = CommonFunctions.screenWidth(mContext);
        setHasOptionsMenu(true);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.fragment_grid, container, false);
        initializeData();
        if (imagesList.size() > 0) {
            setDataInGrid();
        }
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        scrollToPosition(MainActivity.currentPosition);
    }

    /* Initializing Views */
    void initializeData() {
        if (api == null)
            api = APIClient.getClient().create(APIInterface.class);

        imageSearchAPI = new ImageSearchAPI();
        loader = view.findViewById(R.id.loader);
        cir_loader = view.findViewById(R.id.cir_loader);
        edt_search_term = view.findViewById(R.id.edt_search_term);

        recyclerView = view.findViewById(R.id.recycler_view);
        implementingListener();
    }

    /* Listeners */
    void implementingListener() {
        //Search box imeOptions search click
        edt_search_term.setOnEditorActionListener((v, actionId, event) -> {
            if ((event != null && (event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) || (actionId == EditorInfo.IME_ACTION_SEARCH)) {
                checkSearchValidationNCallAPI(TypeEditText);
            }
            return false;
        });

        //Recycler scroll view
        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);
            }

            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);

                GridLayoutManager gridLayoutManager = (GridLayoutManager) recyclerView.getLayoutManager();

                int onScreenVisibleItemCount = gridLayoutManager.getChildCount();
                int totalItemCount = gridLayoutManager.getItemCount();
                int fistVisibleCurrentItem = gridLayoutManager.findFirstVisibleItemPosition();

                scrollPosition = fistVisibleCurrentItem;

                if ((onScreenVisibleItemCount + fistVisibleCurrentItem) >= totalItemCount && !bottomFound && viewFrom == CommonFunctions.Online) {
                    bottomFound = true;
                    scrollPosition = totalItemCount;
                    checkSearchValidationNCallAPI(TypePagination);
                }

            }
        });

        edt_search_term.setOnTouchListener((v, event) -> {
            final int DRAWABLE_RIGHT = 2;
            if (event.getAction() == MotionEvent.ACTION_UP) {
                if (event.getRawX() >= (edt_search_term.getRight() - edt_search_term.getCompoundDrawables()[DRAWABLE_RIGHT].getBounds().width())) {
                    checkSearchValidationNCallAPI(TypeEditText);
                    return true;
                }
            }
            return false;
        });
    }

    void checkSearchValidationNCallAPI(int typeOfSearch) {
        String searchTerm = edt_search_term.getText().toString().trim();
        if (!TextUtils.isEmpty(searchTerm)) {
            //If searching for same item then show error
            if (typeOfSearch == TypeEditText && searchTerm.equalsIgnoreCase(this.searchTerm)) {
                showSnackbar(getString(R.string.same_search));
                bottomFound = false;
                return;
            }
            // Get images list either from server or from DB
            GetImagesList(searchTerm);
        } else bottomFound = false;
    }

    void GetImagesList(String searchTerm) {
        //Hiding Keyboard
        CommonFunctions.HideKeyboard(edt_search_term);

        if (CommonFunctions.checkInternetConnection(mContext)) {
            if (imageSearchAPI != null && !apiCalling) {
                if (this.searchTerm != null && !this.searchTerm.equalsIgnoreCase(searchTerm))
                    offset = 0;

                showLoader();

                imageSearchAPI.imageSearchAPI(mContext, searchTerm, offset, countOfRecords, this, api, call);
            } else bottomFound = false;
        } else {
            bottomFound = false;
            showSnackbar(getString(R.string.no_internet));

            new LoadImageFromDatabaseTask().execute(searchTerm);
        }
    }

    private void scrollToPosition(int position) {
        recyclerView.addOnLayoutChangeListener(new OnLayoutChangeListener() {
            @Override
            public void onLayoutChange(View v, int left, int top, int right, int bottom, int oldLeft, int oldTop, int oldRight, int oldBottom) {
                recyclerView.removeOnLayoutChangeListener(this);
                final RecyclerView.LayoutManager layoutManager = recyclerView.getLayoutManager();
                View viewAtPosition = layoutManager.findViewByPosition(position);
                if (viewAtPosition == null || layoutManager
                        .isViewPartiallyVisible(viewAtPosition, false, true)) {
                    recyclerView.post(() -> layoutManager.scrollToPosition(position));

                }
            }
        });
    }

    private void prepareTransitions() {
        setExitTransition(TransitionInflater.from(getContext())
                .inflateTransition(R.transition.grid_exit_transition));

        setExitSharedElementCallback(
                new SharedElementCallback() {
                    @Override
                    public void onMapSharedElements(List<String> names, Map<String, View> sharedElements) {
                        RecyclerView.ViewHolder selectedViewHolder = recyclerView
                                .findViewHolderForAdapterPosition(MainActivity.currentPosition);
                        if (selectedViewHolder == null || selectedViewHolder.itemView == null) {
                            return;
                        }

                        sharedElements
                                .put(names.get(0), selectedViewHolder.itemView.findViewById(R.id.card_image));
                    }
                });
    }

    private void setExitSharedElementCallback(SharedElementCallback sharedElementCallback) {
    }
    @Override
    public void onApiSuccess(ImageData response, String searchTermItem) {
        hideLoader();
        if (this.searchTerm != null && !this.searchTerm.equalsIgnoreCase(searchTermItem))
            imagesList.clear();

        if (response.getValue().size() > 0) {
            for (Value value : response.getValue()) {
                if (!databaseHelper.isRecordExist(value.getImageId())) {
                    value.setSearchTerm(searchTermItem);
                    new DownloadImage().execute(value);
                }
            }
            imagesList.addAll(response.getValue());
            viewFrom = CommonFunctions.Online;
            searchTerm = searchTermItem;
            setDataInGrid();
            offset = response.getNextOffset();
        }
        bottomFound = false;
    }

    /* API failure and data handling */
    @Override
    public void onApiFailure(String message) {
        hideLoader();
        bottomFound = false;

        if (!message.equalsIgnoreCase(getString(R.string.call_cancelled)))
            showSnackbar(message);
    }

    void setDataInGrid() {
        int gridWidth = screenWidth / numberOfColumns - 50;
        if (imagesList.size() > 0) {
            recyclerView.setAdapter(new GridAdapter(mContext, this, imagesList, gridWidth, viewFrom));
            if (scrollPosition != -1 && offset != 0) {
                scrollToPosition(scrollPosition);
            } else
                scrollPosition = 0;

            recyclerView.setLayoutManager(new GridLayoutManager(getContext(), numberOfColumns));

            //Grid Transitions
            prepareTransitions();
            postponeEnterTransition();

            getActivity().invalidateOptionsMenu();
        } else {
            recyclerView.setAdapter(new GridAdapter(mContext, this, imagesList, gridWidth, viewFrom));
            showSnackbar(getString(R.string.no_data));
        }
    }

    /* To show middle and bottom loaders */
    public void showLoader() {
        loader.setVisibility(offset == 0 ? View.VISIBLE : View.GONE);
        cir_loader.setVisibility(offset > 0 ? View.VISIBLE : View.GONE);
        apiCalling = true;
    }

    /* To hide middle and bottom loaders */
    public void hideLoader() {
        loader.setVisibility(View.GONE);
        cir_loader.setVisibility(View.GONE);
        apiCalling = false;
    }

    public void showSnackbar(String message) {
        Snackbar.make(view, message, Snackbar.LENGTH_SHORT).show();
    }

    /* Options Menu Management */
    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        if (imagesList.size() > 0) {
            inflater.inflate(R.menu.menu_grid_columns_update, menu);
            HandleVisibilityOfMenuOptions(menu);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.column_2:
                setOptionsMenuClick(2);
                return true;
            case R.id.column_3:
                setOptionsMenuClick(3);
                return true;
            case R.id.column_4:
                setOptionsMenuClick(4);
                return true;
            default:
                break;
        }
        return false;
    }

    /* Options Menu's visibility Handling */
    void HandleVisibilityOfMenuOptions(Menu menu) {
        menu.findItem(R.id.column_2).setVisible((numberOfColumns == 2) ? false : true);
        menu.findItem(R.id.column_3).setVisible((numberOfColumns == 3) ? false : true);
        menu.findItem(R.id.column_4).setVisible((numberOfColumns == 4) ? false : true);
    }

    /* Options Menu Click Handling */
    void setOptionsMenuClick(int columns) {
        numberOfColumns = columns;
        setDataInGrid();
    }

    /* DownloadImage and save image Task */
    private class DownloadImage extends AsyncTask<Value, Void, Bitmap> {
        Value value;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected Bitmap doInBackground(Value... values) {
            Bitmap bitmap = null;
            value = values[0];
            try {
                // Download Image from URL
                InputStream input = new java.net.URL(values[0].getThumbnailUrl()).openStream();
                // Decode Bitmap
                bitmap = BitmapFactory.decodeStream(input);
            } catch (Exception e) {
                e.printStackTrace();
            }
            return bitmap;
        }

        @Override
        protected void onPostExecute(Bitmap result) {
            Drawable bitmapDrawable = new BitmapDrawable(getResources(), result);
            databaseHelper.insetImage(bitmapDrawable, value);
        }
    }

    /* Load Data from DB Task */
    private class LoadImageFromDatabaseTask extends AsyncTask<String, Void, List<Value>> {

        protected void onPreExecute() {
            showLoader();
        }

        @Override
        protected List<Value> doInBackground(String... strings) {
            return databaseHelper.getImages(strings[0]);
        }

        protected void onPostExecute(List<Value> valueList) {
            imagesList = new ArrayList(valueList);

            viewFrom = CommonFunctions.DataBase;
            setDataInGrid();

            hideLoader();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (call != null)
            call.cancel();
    }
}
