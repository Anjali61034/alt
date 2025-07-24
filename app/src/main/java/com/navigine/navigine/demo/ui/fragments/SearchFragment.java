package com.navigine.navigine.demo.ui.fragments;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;
import static com.navigine.navigine.demo.utils.Constants.KEY_VENUE_CATEGORY;
import static com.navigine.navigine.demo.utils.Constants.KEY_VENUE_POINT;
import static com.navigine.navigine.demo.utils.Constants.KEY_VENUE_SUBLOCATION;
import static com.navigine.navigine.demo.utils.Constants.VENUE_FILTER_OFF;
import static com.navigine.navigine.demo.utils.Constants.VENUE_FILTER_ON;
import static com.navigine.navigine.demo.utils.Constants.VENUE_SELECTED;

import android.animation.LayoutTransition;
import android.annotation.SuppressLint;
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
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.DrawableRes;
import androidx.appcompat.widget.SearchView;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.RecyclerView;
import androidx.cardview.widget.CardView;

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

public class SearchFragment extends Fragment {

    public static final String ARG_IS_CANARY_CONNECTED = "is_canary_connected";
    public static final String ARG_VENUE_NAMES = "venue_names";
    public static final String ARG_INITIAL_QUERY = "initial_query";

    private static final String TAG = "SearchFragment";

    private SharedViewModel viewModel = null;

    private SearchView                    mSearchField               = null;
    private LinearLayout              mSearchPanel               = null;
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

    // Add suggestion views
    private TextView mSuggestionsText = null;
    private LinearLayout mSuggestionsLayout = null;
    private CardView mSuggestionCard1 = null;
    private CardView mSuggestionCard2 = null;
    private CardView mSuggestionCard3 = null;
    private CardView mSuggestionCard4 = null;
    private TextView mSuggestionText1 = null;
    private TextView mSuggestionText2 = null;
    private TextView mSuggestionText3 = null;
    private TextView mSuggestionText4 = null;

    private Location    mLocation    = null;

    private List<Venue>             mVenuesList               = new ArrayList<>();
    private List<VenueIconObj>      mFilteredVenueIconsList   = new ArrayList<>();
    private Map<Chip, VenueIconObj> mChipsMap                 = new HashMap<>();

    // Add venues from arguments
    private List<String> mVenueNamesFromArguments = new ArrayList<>();
    private boolean mIsCanaryConnected = false;
    private String mInitialQuery = null;

    private VenueListAdapter                 mVenueListAdapter       = null;
    private VenuesIconsListAdapter           mVenuesIconsListAdapter = null;

    private StateReceiver mStateReceiver = null;
    private IntentFilter mStateReceiverFilter = null;

    public static SearchFragment newInstance(boolean isCanaryConnected, ArrayList<String> venueNames) {
        SearchFragment fragment = new SearchFragment();
        Bundle args = new Bundle();
        args.putBoolean(ARG_IS_CANARY_CONNECTED, isCanaryConnected);
        if (venueNames != null) {
            args.putStringArrayList(ARG_VENUE_NAMES, venueNames);
        }
        fragment.setArguments(args);
        return fragment;
    }

    public static SearchFragment newInstance(boolean isCanaryConnected, ArrayList<String> venueNames, String initialQuery) {
        SearchFragment fragment = new SearchFragment();
        Bundle args = new Bundle();
        args.putBoolean(ARG_IS_CANARY_CONNECTED, isCanaryConnected);
        if (venueNames != null) {
            args.putStringArrayList(ARG_VENUE_NAMES, venueNames);
        }
        if (initialQuery != null) {
            args.putString(ARG_INITIAL_QUERY, initialQuery);
        }
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        initViewModels();
        initBroadcastReceiver();

        // Get arguments data
        getArgumentsData();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_search, container, false);

        initViews(view);
        setViewsParams();
        initAdapters();
        setAdapters();
        setViewsListeners(view);
        setObservers();

        // Set initial suggestions
        setInitialSuggestions();

        // Set initial query if provided
        if (mInitialQuery != null && !mInitialQuery.isEmpty()) {
            mSearchField.post(() -> {
                mSearchField.setQuery(mInitialQuery, false);
                mSearchField.setIconified(false);
                mSearchField.requestFocus();
            });
        }

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        addListeners();
    }

    @Override
    public void onPause() {
        super.onPause();
        removeListeners();
    }

    private void getArgumentsData() {
        Bundle args = getArguments();
        if (args != null) {
            mIsCanaryConnected = args.getBoolean(ARG_IS_CANARY_CONNECTED, false);
            mInitialQuery = args.getString(ARG_INITIAL_QUERY);

            if (mIsCanaryConnected) {
                mVenueNamesFromArguments = args.getStringArrayList(ARG_VENUE_NAMES);
                if (mVenueNamesFromArguments == null) {
                    mVenueNamesFromArguments = new ArrayList<>();
                }
                Log.d(TAG, "Received " + mVenueNamesFromArguments.size() + " venue names from arguments");
            }
        }
    }

    private void setInitialSuggestions() {
        if (mIsCanaryConnected && !mVenueNamesFromArguments.isEmpty()) {
            updateSuggestionsWithVenueNames(mVenueNamesFromArguments);
        } else {
            setDefaultSuggestions();
        }
    }

    private void setDefaultSuggestions() {
        String[] defaults = {"Reception", "Office", "Hall", "Doctor"};
        if (mSuggestionText1 != null) mSuggestionText1.setText(defaults[0]);
        if (mSuggestionText2 != null) mSuggestionText2.setText(defaults[1]);
        if (mSuggestionText3 != null) mSuggestionText3.setText(defaults[2]);
        if (mSuggestionText4 != null) mSuggestionText4.setText(defaults[3]);
    }

    private void updateSuggestionsWithVenueNames(List<String> venueNames) {
        Log.d(TAG, "Updating suggestions with " + venueNames.size() + " venue names");

        if (venueNames.size() > 0) {
            mSuggestionText1.setText(venueNames.get(0));
            Log.d(TAG, "Set suggestion 1: " + venueNames.get(0));
        }
        if (venueNames.size() > 1) {
            mSuggestionText2.setText(venueNames.get(1));
            Log.d(TAG, "Set suggestion 2: " + venueNames.get(1));
        } else {
            mSuggestionText2.setText("Reception");
        }
        if (venueNames.size() > 2) {
            mSuggestionText3.setText(venueNames.get(2));
            Log.d(TAG, "Set suggestion 3: " + venueNames.get(2));
        } else {
            mSuggestionText3.setText("Office");
        }
        if (venueNames.size() > 3) {
            mSuggestionText4.setText(venueNames.get(3));
            Log.d(TAG, "Set suggestion 4: " + venueNames.get(3));
        } else {
            mSuggestionText4.setText("Hall");
        }
    }

    private void setupSuggestionCards() {
        try {
            mSuggestionCard1.setOnClickListener(v -> onSuggestionCardClicked(mSuggestionText1.getText().toString()));
            mSuggestionCard2.setOnClickListener(v -> onSuggestionCardClicked(mSuggestionText2.getText().toString()));
            mSuggestionCard3.setOnClickListener(v -> onSuggestionCardClicked(mSuggestionText3.getText().toString()));
            mSuggestionCard4.setOnClickListener(v -> onSuggestionCardClicked(mSuggestionText4.getText().toString()));
        } catch (Exception e) {
            Log.d(TAG, "Suggestion cards setup error: " + e.getMessage());
        }
    }

    private void onSuggestionCardClicked(String venueName) {
        if (mSearchField != null) {
            mSearchField.setQuery(venueName, false);
            mSearchField.setIconified(false);
            mSearchField.requestFocus();
        }
    }

    @SuppressLint("WrongViewCast")
    private void initViews(View view) {
        mTransparentBackground     = view.findViewById(R.id.search__transparent_bg);
        mSearchLayout              = view.findViewById(R.id.search_layout);
        mVenueListLayout           = view.findViewById(R.id.search__venue_listview);
        mVenueIconsLayout          = view.findViewById(R.id.search__venue_icons);
        mVenueIconsListView        = view.findViewById(R.id.recycler_list_venue_icons);
        mVenueListView             = view.findViewById(R.id.recycler_list_venues);
        mItemDivider               = new MaterialDividerItemDecoration(requireContext(), MaterialDividerItemDecoration.VERTICAL);
        mSearchPanel               = view.findViewById(R.id.search__search_panel);
        mSearchField               = view.findViewById(R.id.search__search_field);
        mSearchBtnClear            = mSearchField.findViewById(R.id.search__search_btn_close);
       mSearchBtn                 = view.findViewById(R.id.search__search_btn);
        mSearchBtnClose            = view.findViewById(R.id.search__search_btn_close);
        mChipsScroll               = view.findViewById(R.id.search__search_chips_scroll);
        mChipGroup                 = view.findViewById(R.id.search__search_chips_group);

        // Initialize suggestion views
        mSuggestionsText           = view.findViewById(R.id.suggestionsText);
        mSuggestionsLayout         = view.findViewById(R.id.suggestionsLayout);
        mSuggestionCard1           = view.findViewById(R.id.suggestionCard1);
        mSuggestionCard2           = view.findViewById(R.id.suggestionCard2);
        mSuggestionCard3           = view.findViewById(R.id.suggestionCard3);
        mSuggestionCard4           = view.findViewById(R.id.suggestionCard4);
        mSuggestionText1           = view.findViewById(R.id.suggestionText1);
        mSuggestionText2           = view.findViewById(R.id.suggestionText2);
        mSuggestionText3           = view.findViewById(R.id.suggestionText3);
        mSuggestionText4           = view.findViewById(R.id.suggestionText4);
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
            mItemDivider.setDividerColor(ContextCompat.getColor(requireContext(), R.color.colorBackground));
            mItemDivider.setLastItemDecorated(false);
            mVenueListView.addItemDecoration(mItemDivider);
        }
    }

    private void setViewsListeners(View view) {
        mTransparentBackground.setOnClickListener(v -> onHandleCancelSearch());

        // Check if start button exists in layout before trying to access it
        View startButton = view.findViewById(R.id.start_button);
        if (startButton != null) {
            startButton.setOnClickListener(v -> openNavigationFragment());
        }

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

        // Setup suggestion card listeners
        setupSuggestionCards();
    }

    private void openNavigationFragment() {
        View rootView = getView();
        if (rootView != null) {
            View fragmentContainer = rootView.findViewById(R.id.fragment_container);
            View greenContainer = rootView.findViewById(R.id.green_container);

            if (fragmentContainer != null && greenContainer != null) {
                fragmentContainer.setVisibility(View.VISIBLE);
                greenContainer.setVisibility(View.GONE);

                getParentFragmentManager()
                        .beginTransaction()
                        .replace(R.id.fragment_container, new NavigationFragment())
                        .addToBackStack(null)
                        .commit();
            }
        }
    }

    private void onHandleSearchQueryChange(String query) {
        if (query.isEmpty()) {
            hideSearchButton();
            showSearchCLoseBtn();
            hideVenueListLayout();
            showVenueIconsLayout();
            showSuggestionsLayout();
            populateVenueIconsLayout();
        } else {
            hideSearchCLoseBtn();
            showSearchButton();
            hideVenueIconsLayout();
            hideSuggestionsLayout();
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

    private void showSuggestionsLayout() {
        if (mSuggestionsText != null) mSuggestionsText.setVisibility(VISIBLE);
        if (mSuggestionsLayout != null) mSuggestionsLayout.setVisibility(VISIBLE);
    }

    private void hideSuggestionsLayout() {
        if (mSuggestionsText != null) mSuggestionsText.setVisibility(GONE);
        if (mSuggestionsLayout != null) mSuggestionsLayout.setVisibility(GONE);
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
        mTransparentBackground.setBackground(ContextCompat.getDrawable(requireContext(), android.R.drawable.screen_background_dark_transparent));
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

                // Update suggestions with venues from location
                updateSuggestionsFromLocation();
            }
        });
    }

    private void updateSuggestionsFromLocation() {
        if (mLocation != null && !mVenuesList.isEmpty()) {
            List<String> venueNames = new ArrayList<>();
            for (Venue venue : mVenuesList) {
                if (venue != null && venue.getName() != null && !venue.getName().trim().isEmpty()) {
                    venueNames.add(venue.getName());
                    if (venueNames.size() >= 4) break; // Limit to 4 suggestions
                }
            }
            if (!venueNames.isEmpty()) {
                updateSuggestionsWithVenueNames(venueNames);
            }
        }
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
            requireContext().registerReceiver(mStateReceiver, mStateReceiverFilter, Context.RECEIVER_NOT_EXPORTED);
        } else {
            ContextCompat.registerReceiver(requireContext(), mStateReceiver, mStateReceiverFilter, ContextCompat.RECEIVER_NOT_EXPORTED);
        }
    }

    private void removeListeners() {
        if (mStateReceiver != null) {
            requireContext().unregisterReceiver(mStateReceiver);
        }
    }

    private void hideVenueLayouts() {
        mVenueListLayout.setVisibility(GONE);
        mVenueIconsLayout.setVisibility(GONE);
        hideSuggestionsLayout();
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
        Chip chip = new Chip(requireContext());
        chip.setText(chipText);
        chip.setChipIcon(ContextCompat.getDrawable(requireContext(), iconRes));
        chip.setCloseIconVisible(true);
        chip.setClickable(true);
        chip.setCheckable(false);
        chip.setCloseIconTint(ContextCompat.getColorStateList(requireContext(), R.color.colorPrimary));
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
                showSuggestionsLayout();
                populateVenueIconsLayout();
            } else {
                hideVenueIconsLayout();
                hideSuggestionsLayout();
                showVenueListLayout();
            }
        } else {
            changeSearchBoxStroke(ColorUtils.COLOR_SECONDARY);
            KeyboardController.hideSoftKeyboard(requireActivity());
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