package com.navigine.navigine.demo.ui.activities;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;
import static com.navigine.navigine.demo.utils.Constants.KEY_VENUE_CATEGORY;
import static com.navigine.navigine.demo.utils.Constants.KEY_VENUE_POINT;
import static com.navigine.navigine.demo.utils.Constants.KEY_VENUE_SUBLOCATION;
import static com.navigine.navigine.demo.utils.Constants.VENUE_FILTER_OFF;
import static com.navigine.navigine.demo.utils.Constants.VENUE_FILTER_ON;
import static com.navigine.navigine.demo.utils.Constants.VENUE_SELECTED;

import android.animation.LayoutTransition;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;

import androidx.annotation.DrawableRes;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.divider.MaterialDividerItemDecoration;
import com.navigine.idl.java.Category;
import com.navigine.idl.java.Location;
import com.navigine.idl.java.Sublocation;
import com.navigine.idl.java.Venue;
import com.navigine.navigine.demo.R;
import com.navigine.navigine.demo.adapters.venues.VenueListAdapter;
import com.navigine.navigine.demo.adapters.venues.VenuesIconsListAdapter;
import com.navigine.navigine.demo.models.VenueIconObj;
import com.navigine.navigine.demo.utils.ColorUtils;
import com.navigine.navigine.demo.utils.DimensionUtils;
import com.navigine.navigine.demo.utils.KeyboardController;
import com.navigine.navigine.demo.utils.VenueIconsListProvider;
import com.navigine.navigine.demo.viewmodel.SharedViewModel;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SearchActivity extends AppCompatActivity {

    public static final String EXTRA_IS_CANARY_CONNECTED = "com.navigine.navigine.demo.ui.activities.EXTRA_IS_CANARY_CONNECTED";

    private static final String TAG = "SearchActivity";

    private SharedViewModel viewModel = null;

    private SearchView                    mSearchField               = null;
    private ConstraintLayout              mSearchPanel               = null;
    private LinearLayout                  mSearchLayout              = null;
    private FrameLayout                   mTransparentBackground     = null;
    private FrameLayout                   mVenueListLayout           = null;
    private FrameLayout                   mVenueIconsLayout          = null;
    private ImageView                     mSearchBtnClear            = null;
    private MaterialButton                mSearchBtn                 = null;
    private MaterialButton                mSearchBtnClose            = null;
    private RecyclerView                  mVenueIconsListView        = null;
    private RecyclerView                  mVenueListView             = null;
    private MaterialDividerItemDecoration mItemDivider               = null;
    private HorizontalScrollView          mChipsScroll               = null;
    private ChipGroup                     mChipGroup                 = null;

    private Location    mLocation    = null;

    private List<Venue>             mVenuesList               = new ArrayList<>();
    private List<VenueIconObj>      mFilteredVenueIconsList   = new ArrayList<>();
    private Map<Chip, VenueIconObj> mChipsMap                 = new HashMap<>();

    private VenueListAdapter                 mVenueListAdapter       = null;
    private VenuesIconsListAdapter           mVenuesIconsListAdapter = null;

    private StateReceiver mStateReceiver = null;
    private IntentFilter mStateReceiverFilter = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);

        initViewModels();
        initBroadcastReceiver();
        initViews();
        setViewsParams();
        initAdapters();
        setAdapters();
        setViewsListeners();
        setObservers();
    }

    @Override
    protected void onResume() {
        super.onResume();
        addListeners();
    }

    @Override
    protected void onPause() {
        super.onPause();
        removeListeners();
    }

    private void initViews() {
        mTransparentBackground     = findViewById(R.id.search__transparent_bg);
        mSearchLayout              = findViewById(R.id.search_layout);
        mVenueListLayout           = findViewById(R.id.search__venue_listview);
        mVenueIconsLayout          = findViewById(R.id.search__venue_icons);
        mVenueIconsListView        = findViewById(R.id.recycler_list_venue_icons);
        mVenueListView             = findViewById(R.id.recycler_list_venues);
        mItemDivider               = new MaterialDividerItemDecoration(this, MaterialDividerItemDecoration.VERTICAL);
        mSearchPanel               = findViewById(R.id.search__search_panel);
        mSearchField               = findViewById(R.id.search__search_field);
        mSearchBtnClear            = mSearchField.findViewById(R.id.search__search_btn_close);
        mSearchBtn                 = findViewById(R.id.search__search_btn);
        mSearchBtnClose            = findViewById(R.id.search__search_btn_close);
        mChipsScroll               = findViewById(R.id.search__search_chips_scroll);
        mChipGroup                 = findViewById(R.id.search__search_chips_group);
    }

    private void setViewsParams() {
        if (mSearchBtnClear != null) mSearchBtnClear.setEnabled(false);

        // Initialize LayoutTransition before enabling transition types
        if (mSearchLayout.getLayoutTransition() == null) {
            mSearchLayout.setLayoutTransition(new LayoutTransition());
        }
        mSearchLayout.getLayoutTransition().enableTransitionType(LayoutTransition.CHANGING);

        if (mChipsScroll.getLayoutTransition() == null) {
            mChipsScroll.setLayoutTransition(new LayoutTransition());
        }
        mChipsScroll.getLayoutTransition().enableTransitionType(LayoutTransition.CHANGING);

        if (mSearchPanel.getLayoutTransition() == null) {
            mSearchPanel.setLayoutTransition(new LayoutTransition());
        }
        mSearchPanel.getLayoutTransition().enableTransitionType(LayoutTransition.CHANGING);

        if (mVenueListLayout.getLayoutTransition() == null) {
            mVenueListLayout.setLayoutTransition(new LayoutTransition());
        }
        mVenueListLayout.getLayoutTransition().enableTransitionType(LayoutTransition.CHANGING);

        // Set divider color
        if (mItemDivider != null) {
            mItemDivider.setDividerColor(ContextCompat.getColor(this, R.color.colorBackground));
            mItemDivider.setLastItemDecorated(false);
            mVenueListView.addItemDecoration(mItemDivider);
        }
    }

    private void setViewsListeners() {
        mTransparentBackground.setOnClickListener(v -> onHandleCancelSearch());

        mSearchField.setOnQueryTextFocusChangeListener(this::onSearchBoxFocusChange);

        mSearchField.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                onHandleSearchQueryChange(newText);
                return true;
            }
        });

        mSearchBtnClose.setOnClickListener(v -> onHandleCancelSearch());
    }

    private void onHandleSearchQueryChange(String query) {
        if (query.isEmpty()) {
            hideSearchButton();
            showSearchCLoseBtn();
            hideVenueListLayout();
            showVenueIconsLayout();
            populateVenueIconsLayout();
        } else {
            hideSearchCLoseBtn();
            showSearchButton();
            hideVenueIconsLayout();
            showVenueListLayout();
        }
        filterVenueListByQuery(query);
    }

    private void filterVenueListByQuery(String query) {
        if (mVenueListAdapter != null) {
            mVenueListAdapter.filter(query);
        }
    }

    private void showSearchButton() {
        mSearchBtn.setVisibility(VISIBLE);
    }

    private void hideSearchButton() {
        mSearchBtn.setVisibility(GONE);
    }

    private void showVenueIconsLayout() {
        mVenueIconsLayout.setVisibility(VISIBLE);
    }

    private void hideVenueIconsLayout() {
        mVenueIconsLayout.setVisibility(GONE);
    }

    private void showVenueListLayout() {
        mVenueListLayout.setVisibility(VISIBLE);
    }

    private void hideVenueListLayout() {
        mVenueListLayout.setVisibility(GONE);
    }

    private void hideSearchCLoseBtn() {
        mSearchBtnClose.setVisibility(GONE);
    }

    private void showSearchCLoseBtn() {
        mSearchBtnClose.setVisibility(VISIBLE);
    }

    private void changeSearchBoxStroke(int color) {
        ((GradientDrawable) mSearchField.getBackground()).setStroke(DimensionUtils.STROKE_WIDTH, color);
    }

    private void changeSearchLayoutBackground(int color) {
        mSearchLayout.getBackground().setTint(color);
    }

    private void showTransparentLayout() {
        mTransparentBackground.setBackground(ContextCompat.getDrawable(this, android.R.drawable.screen_background_dark_transparent));
        mTransparentBackground.setClickable(true);
        mTransparentBackground.setFocusable(true);
    }

    private void hideTransparentLayout() {
        mTransparentBackground.setBackground(null);
        mTransparentBackground.setClickable(false);
        mTransparentBackground.setFocusable(false);
    }

    private void setObservers() {
        viewModel.mLocation.observe(this, location -> {
            mLocation = location;
            if (mLocation != null) {
                mVenueListAdapter.clear();
                mVenuesList.clear();
                for (Sublocation sublocation : mLocation.getSublocations()) {
                    mVenuesList.addAll(sublocation.getVenues());
                }
                mVenueListAdapter.submit(mVenuesList, mLocation);
                updateFilteredVenuesIconsList();
            }
        });
    }

    private void initAdapters() {
        mVenuesIconsListAdapter = new VenuesIconsListAdapter();
        mVenueListAdapter       = new VenueListAdapter();
    }

    private void setAdapters() {
        mVenueIconsListView.setAdapter(mVenuesIconsListAdapter);
        mVenueListView.setAdapter(mVenueListAdapter);
    }

    private void initViewModels() {
        viewModel = new ViewModelProvider(this).get(SharedViewModel.class);
    }

    private void initBroadcastReceiver() {
        mStateReceiver = new StateReceiver();
        mStateReceiverFilter = new IntentFilter();

        mStateReceiverFilter.addAction(VENUE_SELECTED);
        mStateReceiverFilter.addAction(VENUE_FILTER_ON);
        mStateReceiverFilter.addAction(VENUE_FILTER_OFF);
    }

    private void addListeners() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(mStateReceiver, mStateReceiverFilter, Context.RECEIVER_NOT_EXPORTED);
        } else {
            ContextCompat.registerReceiver(this, mStateReceiver, mStateReceiverFilter, ContextCompat.RECEIVER_NOT_EXPORTED);
        }
    }

    private void removeListeners() {
        unregisterReceiver(mStateReceiver);
    }

    private void hideVenueLayouts() {
        mVenueListLayout.setVisibility(GONE);
        mVenueIconsLayout.setVisibility(GONE);
    }

    private void onCloseSearch() {
        changeSearchLayoutBackground(Color.TRANSPARENT);
        mSearchField.clearFocus();
        mSearchBtnClose.setVisibility(GONE);
        hideVenueLayouts();
    }

    private void clearSearchQuery() {
        mSearchField.setQuery("", false);
    }

    private void updateFilteredVenuesIconsList() {
        mFilteredVenueIconsList.clear();

        if (mLocation == null) return;

        Map<Integer, String> mCategoriesIdsAll       = new HashMap();
        List<Integer>        mCategoriesIdsCurr      = new ArrayList<>();

        for (Category category : mLocation.getCategories()) {
            mCategoriesIdsAll.put(category.getId(), category.getName());
        }
        for (Venue venue : mVenuesList) {
            mCategoriesIdsCurr.add(venue.getCategoryId());
        }
        mCategoriesIdsAll.keySet().retainAll(mCategoriesIdsCurr);

        for (VenueIconObj venueIconObj : VenueIconsListProvider.VenueIconsList) {
            if (mCategoriesIdsAll.containsValue(venueIconObj.getCategoryName())) {
                mFilteredVenueIconsList.add(new VenueIconObj(venueIconObj.getImageDrawable(), venueIconObj.getCategoryName()));
            }
        }
    }

    private void excludeVenueFromFiltered(VenueIconObj venueIconObj) {
        mStateReceiver.filteredVenues.remove(venueIconObj);
    }

    private void unselectVenueIcon(VenueIconObj venueIconObj) {
        mFilteredVenueIconsList.get(mFilteredVenueIconsList.indexOf(venueIconObj)).setActivated(false);
    }

    private List<VenueIconObj> getFilteredVenuesIconsList() {
        return mFilteredVenueIconsList;
    }

    private void populateVenueIconsLayout() {
        if (mVenueIconsLayout.getVisibility() == VISIBLE)
            mVenuesIconsListAdapter.updateList(getFilteredVenuesIconsList());
    }

    private void onHandleCancelSearch() {
        onCloseSearch();
        hideTransparentLayout();
    }

    private void updateSearchViewWithChips(List<VenueIconObj> venueIconObjs) {
        removeChipsFromGroup();
        mappingChipsToVenueIcons(venueIconObjs);
    }

    private Chip createChip(String chipText, @DrawableRes int iconRes) {
        Chip chip = new Chip(this);
        chip.setText(chipText);
        chip.setChipIcon(ContextCompat.getDrawable(this, iconRes));
        chip.setCloseIconVisible(true);
        chip.setClickable(true);
        chip.setCheckable(false);
        chip.setCloseIconTint(ContextCompat.getColorStateList(this, R.color.colorPrimary));
        chip.setEllipsize(TextUtils.TruncateAt.END);
        chip.setOnCloseIconClickListener(v -> onCancelChip((Chip) v));
        return chip;
    }

    private void addChipToGroup(Chip chip) {
        mChipGroup.addView(chip);
    }

    private void mappingChipsToVenueIcons(List<VenueIconObj> venueIconObjs) {
        for (int i = 0; i < venueIconObjs.size(); i++) {
            VenueIconObj v = venueIconObjs.get(i);
            Chip chip = createChip(v.getCategoryName(), v.getImageDrawable());
            addChipToGroup(chip);
            mChipsMap.put(chip, v);
        }
    }

    private void removeChipsFromGroup() {
        mChipGroup.removeAllViews();
        mChipsMap.clear();
    }

    private void removeChip(Chip chip) {
        mChipGroup.removeView(chip);
        mChipsMap.remove(chip);
    }

    private VenueIconObj getMappingVenueIcon(Chip chip) {
        return mChipsMap.get(chip);
    }

    private void onCancelChip(Chip chip) {
        VenueIconObj venueIconObj = getMappingVenueIcon(chip);
        excludeVenueFromFiltered(venueIconObj);
        unselectVenueIcon(venueIconObj);
        removeChip(chip);
        populateVenueIconsLayout();
        // Apply venue filter if needed
        // applyVenueFilter(mStateReceiver.filteredVenues);
    }

    private void onSearchBoxFocusChange(View v, boolean hasFocus) {
        boolean isQueryEmpty = ((SearchView) v).getQuery().toString().isEmpty();
        if (hasFocus) {
            showTransparentLayout();
            changeSearchLayoutBackground(Color.WHITE);
            changeSearchBoxStroke(ColorUtils.COLOR_PRIMARY);
            showSearchCLoseBtn();
            if (isQueryEmpty) {
                hideVenueListLayout();
                showVenueIconsLayout();
                populateVenueIconsLayout();
            } else {
                hideVenueIconsLayout();
                showVenueListLayout();
            }
        } else {
            changeSearchBoxStroke(ColorUtils.COLOR_SECONDARY);
            KeyboardController.hideSoftKeyboard(this);
        }
    }

    private class StateReceiver extends BroadcastReceiver {

        private ArrayList<VenueIconObj> filteredVenues = null;

        @Override
        public void onReceive(Context context, Intent intent) {
            switch (intent.getAction()) {
                case VENUE_SELECTED:
                    int sublocationId = intent.getIntExtra(KEY_VENUE_SUBLOCATION, 0);
                    float[] point     = intent.getFloatArrayExtra(KEY_VENUE_POINT);
                    // Handle venue selection if needed
                    onCloseSearch();
                    hideTransparentLayout();
                    break;
                case VENUE_FILTER_ON:
                case VENUE_FILTER_OFF:
                    filteredVenues = intent.getParcelableArrayListExtra(KEY_VENUE_CATEGORY);
                    if (filteredVenues != null) {
                        if (intent.getAction().equals(VENUE_FILTER_ON)) {
                            updateSearchViewWithChips(filteredVenues);
                            // Apply venue filter if needed
                            // applyVenueFilter(filteredVenues);
                        }
                        else {
                            removeChipsFromGroup();
                            // Reset venue filter if needed
                            // resetVenueFilter();
                        }
                    }
                    onCloseSearch();
                    hideTransparentLayout();
                    break;
            }
        }
    }
}