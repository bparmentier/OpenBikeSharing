/*
 * Copyright (c) 2014-2015 Bruno Parmentier. This file is part of OpenBikeSharing.
 *
 * OpenBikeSharing is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * OpenBikeSharing is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with OpenBikeSharing.  If not, see <http://www.gnu.org/licenses/>.
 */

package be.brunoparmentier.openbikesharing.app.activities;

import android.app.ActionBar;
import android.app.AlertDialog;
import android.app.FragmentTransaction;
import android.app.SearchManager;
import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.MatrixCursor;
import android.location.Location;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.SwipeRefreshLayout;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.SearchView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.DateFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import be.brunoparmentier.openbikesharing.app.R;
import be.brunoparmentier.openbikesharing.app.adapters.SearchStationAdapter;
import be.brunoparmentier.openbikesharing.app.db.StationsDataSource;
import be.brunoparmentier.openbikesharing.app.fragments.StationsListFragment;
import be.brunoparmentier.openbikesharing.app.models.BikeNetwork;
import be.brunoparmentier.openbikesharing.app.models.Station;
import be.brunoparmentier.openbikesharing.app.parsers.BikeNetworkParser;
import be.brunoparmentier.openbikesharing.app.widgets.StationsListAppWidgetProvider;


public class StationsListActivity extends FragmentActivity implements ActionBar.TabListener {
    private static final String TAG = StationsListActivity.class.getSimpleName();

    private static final String DEFAULT_API_URL = "http://api.citybik.es/v2/";
    private static final String PREF_KEY_API_URL = "pref_api_url";
    private static final String PREF_KEY_NETWORK_ID = "network-id";
    private static final String PREF_KEY_NETWORK_LATITUDE = "network-latitude";
    private static final String PREF_KEY_NETWORK_LONGITUDE = "network-longitude";
    private static final String PREF_KEY_FAV_STATIONS = "fav-stations";
    private static final String PREF_KEY_STRIP_ID_STATION = "pref_strip_id_station";
    private static final String PREF_KEY_DB_LAST_UPDATE = "db_last_update";
    private static final String PREF_KEY_DEFAULT_TAB = "pref_default_tab";

    private static final String KEY_BIKE_NETWORK = "bikeNetwork";
    private static final String KEY_STATIONS = "stations";
    private static final String KEY_FAV_STATIONS = "favStations";
    private static final String KEY_NEARBY_STATIONS = "nearbyStations";
    private static final String KEY_NETWORK_ID = "network-id";

    private static final int PICK_NETWORK_REQUEST = 1;

    private BikeNetwork bikeNetwork;
    private ArrayList<Station> stations;
    private ArrayList<Station> favStations;
    private ArrayList<Station> nearbyStations;
    private StationsDataSource stationsDataSource;

    private JSONDownloadTask jsonDownloadTask;

    private SharedPreferences settings;

    private Menu optionsMenu;
    private ActionBar actionBar;
    private SearchView searchView;
    private ViewPager viewPager;
    private TabsPagerAdapter tabsPagerAdapter;

    private StationsListFragment allStationsFragment;
    private StationsListFragment favoriteStationsFragment;
    private StationsListFragment nearbyStationsFragment;

    private SwipeRefreshLayout refreshLayout;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_stations_list);

        refreshLayout = (SwipeRefreshLayout) findViewById(R.id.swipe_container);
        refreshLayout.setColorSchemeResources(R.color.bike_red,R.color.parking_blue_dark);
        refreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                executeDownloadTask();
            }
        });
        viewPager = (ViewPager) findViewById(R.id.viewPager);
        viewPager.setOffscreenPageLimit(2);
        viewPager.setOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

            }

            @Override
            public void onPageSelected(int position) {
                actionBar.setSelectedNavigationItem(position);
            }

            @Override
            public void onPageScrollStateChanged(int state) {
                /* as explained on
                http://stackoverflow.com/questions/25978462/swiperefreshlayout-viewpager-limit-horizontal-scroll-only
                 */
                refreshLayout.setEnabled(state == ViewPager.SCROLL_STATE_IDLE);

            }
        });

        stationsDataSource = new StationsDataSource(this);
        stations = stationsDataSource.getStations();
        favStations = stationsDataSource.getFavoriteStations();
        nearbyStations = new ArrayList<>();

        tabsPagerAdapter = new TabsPagerAdapter(getSupportFragmentManager());
        viewPager.setAdapter(tabsPagerAdapter);

        settings = PreferenceManager.getDefaultSharedPreferences(this);

        actionBar = getActionBar();
        int defaultTabIndex = Integer.valueOf(settings.getString(PREF_KEY_DEFAULT_TAB, "0"));
        for (int i = 0; i < 3; i++) {
            ActionBar.Tab tab = actionBar.newTab();
            tab.setTabListener(this);
            tab.setText(tabsPagerAdapter.getPageTitle(i));
            actionBar.addTab(tab, (defaultTabIndex == i));
        }
        actionBar.setHomeButtonEnabled(false);
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);

        boolean firstRun = settings.getString(PREF_KEY_NETWORK_ID, "").isEmpty();
        setDBLastUpdateText();

        if (firstRun) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setMessage(R.string.welcome_dialog_message);
            builder.setTitle(R.string.welcome_dialog_title);
            builder.setPositiveButton(R.string.welcome_dialog_ok, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    Intent intent = new Intent(StationsListActivity.this, BikeNetworksListActivity.class);
                    startActivityForResult(intent, PICK_NETWORK_REQUEST);
                }
            });
            builder.setNegativeButton(R.string.welcome_dialog_cancel, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    finish();
                }
            });
            AlertDialog dialog = builder.create();
            dialog.show();
        } else {
            if (savedInstanceState != null) {
                bikeNetwork = (BikeNetwork) savedInstanceState.getSerializable(KEY_BIKE_NETWORK);
                stations = (ArrayList<Station>) savedInstanceState.getSerializable(KEY_STATIONS);
                favStations = (ArrayList<Station>) savedInstanceState.getSerializable(KEY_FAV_STATIONS);
                nearbyStations = (ArrayList<Station>) savedInstanceState.getSerializable(KEY_NEARBY_STATIONS);
            } else {
                String networkId = settings.getString(PREF_KEY_NETWORK_ID, "");
                String stationUrl = settings.getString(PREF_KEY_API_URL, DEFAULT_API_URL)
                        + "networks/" + networkId;
                jsonDownloadTask = new JSONDownloadTask();
                jsonDownloadTask.execute(stationUrl);
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        if ((jsonDownloadTask != null && jsonDownloadTask.getStatus() == AsyncTask.Status.FINISHED)) {
            long dbLastUpdate = settings.getLong(PREF_KEY_DB_LAST_UPDATE, -1);
            long currentTime = System.currentTimeMillis();

            /* Refresh list with latest data from database */
            stations = stationsDataSource.getStations();
            favStations = stationsDataSource.getFavoriteStations();
            setNearbyStations(stations);
            tabsPagerAdapter.updateAllStationsListFragment(stations);
            tabsPagerAdapter.updateFavoriteStationsFragment(favStations);
            tabsPagerAdapter.updateNearbyStationsFragment(nearbyStations);
            setDBLastUpdateText();

            /* Update automatically if data is more than 10 min old */
            if ((dbLastUpdate != -1) && ((currentTime - dbLastUpdate) > 600000)) {
                String networkId = settings.getString(PREF_KEY_NETWORK_ID, "");
                String stationUrl = settings.getString(PREF_KEY_API_URL, DEFAULT_API_URL)
                        + "networks/" + networkId;
                jsonDownloadTask = new JSONDownloadTask();
                jsonDownloadTask.execute(stationUrl);
            }
        }
    }

    private void setDBLastUpdateText() {
        TextView lastUpdate = (TextView) findViewById(R.id.dbLastUpdate);
        long dbLastUpdate = settings.getLong(PREF_KEY_DB_LAST_UPDATE, -1);

        if (dbLastUpdate == -1) {
            lastUpdate.setText(String.format(getString(R.string.db_last_update),
                    getString(R.string.db_last_update_never)));
        } else {
            lastUpdate.setText(String.format(getString(R.string.db_last_update),
                    DateUtils.formatSameDayTime(dbLastUpdate, System.currentTimeMillis(),
                            DateFormat.DEFAULT, DateFormat.DEFAULT)));
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putSerializable(KEY_BIKE_NETWORK, bikeNetwork);
        outState.putSerializable(KEY_STATIONS, stations);
        outState.putSerializable(KEY_FAV_STATIONS, favStations);
        outState.putSerializable(KEY_NEARBY_STATIONS, nearbyStations);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        this.optionsMenu = menu;
        getMenuInflater().inflate(R.menu.stations_list, menu);

        if (jsonDownloadTask != null &&
                (jsonDownloadTask.getStatus() == AsyncTask.Status.PENDING
                || jsonDownloadTask.getStatus() == AsyncTask.Status.RUNNING)) {
            setRefreshActionButtonState(true);

        }

        SearchManager manager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);

        searchView = (SearchView) menu.findItem(R.id.action_search).getActionView();
        searchView.setSearchableInfo(manager.getSearchableInfo(getComponentName()));
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {

            @Override
            public boolean onQueryTextSubmit(String s) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String s) {
                loadData(s);
                return true;
            }
        });

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        switch (id) {
            case R.id.action_settings:
                Intent settingsIntent = new Intent(this, SettingsActivity.class);
                startActivity(settingsIntent);
                return true;
            case R.id.action_refresh:
                String networkId = PreferenceManager
                        .getDefaultSharedPreferences(this)
                        .getString(PREF_KEY_NETWORK_ID, "");
                String stationUrl = settings.getString(PREF_KEY_API_URL, DEFAULT_API_URL)
                        + "networks/" + networkId;
                jsonDownloadTask = new JSONDownloadTask();
                jsonDownloadTask.execute(stationUrl);
                return true;
            case R.id.action_map:
                Intent mapIntent = new Intent(this, MapActivity.class);
                startActivity(mapIntent);
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == PICK_NETWORK_REQUEST) {
            Log.d(TAG, "PICK_NETWORK_REQUEST");
            if (resultCode == RESULT_OK) {
                Log.d(TAG, "RESULT_OK");
                String stationUrl = settings.getString(PREF_KEY_API_URL, DEFAULT_API_URL)
                        + "networks/" + data.getExtras().getString(KEY_NETWORK_ID);
                jsonDownloadTask = new JSONDownloadTask();
                jsonDownloadTask.execute(stationUrl);
            }
        }
    }

    @Override
    public void onTabSelected(ActionBar.Tab tab, FragmentTransaction fragmentTransaction) {
        viewPager.setCurrentItem(tab.getPosition());
    }

    @Override
    public void onTabUnselected(ActionBar.Tab tab, FragmentTransaction fragmentTransaction) {

    }

    @Override
    public void onTabReselected(ActionBar.Tab tab, FragmentTransaction fragmentTransaction) {

    }

    private void loadData(String query) {
        ArrayList<Station> queryStations = new ArrayList<>();
        String[] columns = new String[]{"_id", "text"};
        Object[] temp = new Object[]{0, "default"};

        MatrixCursor cursor = new MatrixCursor(columns);

        if (stations != null) {
            for (int i = 0; i < stations.size(); i++) {
                Station station = stations.get(i);
                if (station.getName().toLowerCase().contains(query.toLowerCase())) {
                    temp[0] = i;
                    temp[1] = station.getName();
                    cursor.addRow(temp);
                    queryStations.add(station);
                }
            }
        }

        searchView.setSuggestionsAdapter(new SearchStationAdapter(this, cursor, queryStations));

    }

    private void setRefreshActionButtonState(final boolean refreshing) {
        if (optionsMenu != null) {
            final MenuItem refreshItem = optionsMenu.findItem(R.id.action_refresh);
            if (refreshItem != null) {
                if (refreshing) {
                    refreshItem.setActionView(R.layout.actionbar_indeterminate_progress);
                } else {
                    refreshItem.setActionView(null);
                }
            }
        }
    }
    //put here the code to update the bikes data
    private void executeDownloadTask(){
        String networkId = PreferenceManager
                .getDefaultSharedPreferences(this)
                .getString(PREF_KEY_NETWORK_ID, "");
        String stationUrl = settings.getString(PREF_KEY_API_URL, DEFAULT_API_URL)
                + "networks/" + networkId;
        jsonDownloadTask = new JSONDownloadTask();
        jsonDownloadTask.execute(stationUrl);
    }


    private void setNearbyStations(List<Station> stations) {
        final double radius = 0.01;
        nearbyStations = new ArrayList<>();
        LocationManager locationManager =
                (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)){
            final Location userLocation = locationManager
                    .getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
            if (userLocation != null) {
                for (Station station : stations) {
                    if ((station.getLatitude() > userLocation.getLatitude() - radius)
                            && (station.getLatitude() < userLocation.getLatitude() + radius)
                            && (station.getLongitude() > userLocation.getLongitude() - radius)
                            && (station.getLongitude() < userLocation.getLongitude() + radius)) {
                        nearbyStations.add(station);
                    }
                }
                Collections.sort(nearbyStations, new Comparator<Station>() {

                    @Override
                    public int compare(Station station1, Station station2) {
                        float[] result1 = new float[3];
                        Location.distanceBetween(userLocation.getLatitude(), userLocation.getLongitude(),
                                station1.getLatitude(), station1.getLongitude(), result1);
                        Float distance1 = result1[0];

                        float[] result2 = new float[3];
                        Location.distanceBetween(userLocation.getLatitude(), userLocation.getLongitude(),
                                station2.getLatitude(), station2.getLongitude(), result2);
                        Float distance2 = result2[0];

                        return distance1.compareTo(distance2);
                    }
                });
            } else {
                // TODO: update location?
            }
        }
    }

    private class JSONDownloadTask extends AsyncTask<String, Void, String> {

        Exception error;

        @Override
        protected void onPreExecute() {
            setRefreshActionButtonState(true);
        }

        @Override
        protected String doInBackground(String... urls) {
            if (urls[0].isEmpty()) {
                finish();
            }
            try {
                StringBuilder response = new StringBuilder();

                URL url = new URL(urls[0]);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                if (conn.getResponseCode() == HttpURLConnection.HTTP_OK) {
                    BufferedReader input = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                    String strLine;
                    while ((strLine = input.readLine()) != null) {
                        response.append(strLine);
                    }
                    input.close();
                }
                return response.toString();
            } catch (IOException e) {
                error = e;
                Log.d(TAG, e.getMessage());
                return e.getMessage();
            }
        }

        @Override
        protected void onPostExecute(final String result) {
            if (error != null) {
                Log.d(TAG, error.getMessage());
                Toast.makeText(getApplicationContext(),
                        getApplicationContext().getResources().getString(R.string.connection_error),
                        Toast.LENGTH_SHORT).show();
                setRefreshActionButtonState(false);
                refreshLayout.setRefreshing(false);
            } else {
                try {
                    /* parse result */
                    boolean stripId = settings.getBoolean(PREF_KEY_STRIP_ID_STATION, false);
                    BikeNetworkParser bikeNetworkParser = new BikeNetworkParser(result, stripId);
                    bikeNetwork = bikeNetworkParser.getNetwork();

                    stations = bikeNetwork.getStations();
                    Collections.sort(stations);
                    stationsDataSource.storeStations(stations);
                    favStations = stationsDataSource.getFavoriteStations();

                    settings.edit()
                            .putLong(PREF_KEY_DB_LAST_UPDATE, System.currentTimeMillis())
                            .apply();
                    setDBLastUpdateText();

                    setNearbyStations(stations);

                    tabsPagerAdapter.updateAllStationsListFragment(stations);
                    tabsPagerAdapter.updateFavoriteStationsFragment(favStations);
                    tabsPagerAdapter.updateNearbyStationsFragment(nearbyStations);

                    Intent refreshWidgetIntent = new Intent(getApplicationContext(),
                            StationsListAppWidgetProvider.class);
                    refreshWidgetIntent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
                    refreshWidgetIntent.putExtra(StationsListAppWidgetProvider.EXTRA_REFRESH_LIST_ONLY, true);
                    sendBroadcast(refreshWidgetIntent);

                    upgradeAppToVersion13();
                } catch (ParseException e) {
                    Log.e(TAG, e.getMessage());
                    Toast.makeText(StationsListActivity.this,
                            R.string.json_error, Toast.LENGTH_LONG).show();
                } finally {
                    setRefreshActionButtonState(false);
                    refreshLayout.setRefreshing(false);
                }
            }
        }
    }

    private class TabsPagerAdapter extends FragmentPagerAdapter {
        private static final int NUM_ITEMS = 3;

        public TabsPagerAdapter(FragmentManager fragmentManager) {
            super(fragmentManager);

            allStationsFragment = StationsListFragment.newInstance(stations,
                    R.string.no_stations);
            favoriteStationsFragment = StationsListFragment.newInstance(favStations,
                    R.string.no_favorite_stations);
            nearbyStationsFragment = StationsListFragment.newInstance(nearbyStations,
                    R.string.no_nearby_stations);
        }

        @Override
        public int getCount() {
            return NUM_ITEMS;
        }

        @Override
        public Fragment getItem(int position) {
            switch (position) {
                case 0:
                    return nearbyStationsFragment;
                case 1:
                    return favoriteStationsFragment;
                case 2:
                    return allStationsFragment;
                default:
                    return null;
            }
        }

        @Override
        public CharSequence getPageTitle(int position) {
            switch (position) {
                case 0:
                    return getString(R.string.nearby_stations);
                case 1:
                    return getString(R.string.favorite_stations);
                case 2:
                    return getString(R.string.all_stations);
                default:
                    return null;
            }
        }

        public void updateAllStationsListFragment(ArrayList<Station> stations) {
            allStationsFragment.updateStationsList(stations);
        }

        public void updateFavoriteStationsFragment(ArrayList<Station> stations) {
            favoriteStationsFragment.updateStationsList(stations);
        }

        public void updateNearbyStationsFragment(ArrayList<Station> stations) {
            nearbyStationsFragment.updateStationsList(stations);
        }
    }

    private void upgradeAppToVersion13() {
        if (settings.contains(PREF_KEY_FAV_STATIONS)) {
            Set<String> favorites = settings.getStringSet(PREF_KEY_FAV_STATIONS, new HashSet<String>());

            for (String favorite : favorites) {
                stationsDataSource.addFavoriteStation(favorite);
            }

            settings.edit().remove(PREF_KEY_FAV_STATIONS).apply();
        }

        if (!settings.contains(PREF_KEY_NETWORK_LATITUDE) || !settings.contains(PREF_KEY_NETWORK_LONGITUDE)) {
            settings.edit()
                    .putLong(PREF_KEY_NETWORK_LATITUDE,
                            Double.doubleToRawLongBits(bikeNetwork.getLocation().getLatitude()))
                    .putLong(PREF_KEY_NETWORK_LONGITUDE,
                            Double.doubleToRawLongBits(bikeNetwork.getLocation().getLongitude()))
                    .apply();
        }
    }
}
