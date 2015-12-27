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
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
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
import java.util.HashSet;
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

    private static final String BASE_URL = "http://api.citybik.es/v2/networks";
    private static final String PREF_KEY_NETWORK_ID = "network-id";
    private static final String PREF_KEY_NETWORK_LATITUDE = "network-latitude";
    private static final String PREF_KEY_NETWORK_LONGITUDE = "network-longitude";
    private static final String PREF_KEY_FAV_STATIONS = "fav-stations";
    private static final String PREF_KEY_STRIP_ID_STATION = "pref_strip_id_station";
    private static final String PREF_KEY_DB_LAST_UPDATE = "db_last_update";

    private static final int PICK_NETWORK_REQUEST = 1;

    private BikeNetwork bikeNetwork;
    private ArrayList<Station> stations;
    private ArrayList<Station> favStations;
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_stations_list);

        viewPager = (ViewPager) findViewById(R.id.viewPager);
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

            }
        });

        stationsDataSource = new StationsDataSource(this);
        stations = stationsDataSource.getStations();
        favStations = stationsDataSource.getFavoriteStations();

        actionBar = getActionBar();
        actionBar.addTab(actionBar.newTab()
                .setText(getString(R.string.all_stations))
                .setTabListener(this));
        actionBar.addTab(actionBar.newTab()
                .setText(getString(R.string.favorite_stations))
                .setTabListener(this));
        actionBar.setHomeButtonEnabled(false);

        settings = PreferenceManager.getDefaultSharedPreferences(this);

        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
        tabsPagerAdapter = new TabsPagerAdapter(getSupportFragmentManager());
        viewPager.setAdapter(tabsPagerAdapter);
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
                bikeNetwork = (BikeNetwork) savedInstanceState.getSerializable("bikeNetwork");
                stations = (ArrayList<Station>) savedInstanceState.getSerializable("stations");
                favStations = (ArrayList<Station>) savedInstanceState.getSerializable("favStations");
            } else {
                String networkId = settings.getString(PREF_KEY_NETWORK_ID, "");
                String stationUrl = BASE_URL + "/" + networkId;
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
            tabsPagerAdapter.updateAllStationsListFragment(stations);
            tabsPagerAdapter.updateFavoriteStationsFragment(favStations);
            setDBLastUpdateText();

            /* Update automatically if data is more than 10 min old */
            if ((dbLastUpdate != -1) && ((currentTime - dbLastUpdate) > 600000)) {
                String networkId = settings.getString(PREF_KEY_NETWORK_ID, "");
                String stationUrl = BASE_URL + "/" + networkId;
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
        outState.putSerializable("bikeNetwork", bikeNetwork);
        outState.putSerializable("stations", stations);
        outState.putSerializable("favStations", favStations);
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
                String stationUrl = BASE_URL + "/" + networkId;
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
                String stationUrl = BASE_URL + "/" + data.getExtras().getString("network-id");
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
                Log.d(TAG, "Stations downloaded");
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

                    tabsPagerAdapter.updateAllStationsListFragment(stations);
                    tabsPagerAdapter.updateFavoriteStationsFragment(favStations);

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
                }
            }
        }
    }

    private class TabsPagerAdapter extends FragmentPagerAdapter {
        private final int NUM_ITEMS = 2;

        public TabsPagerAdapter(FragmentManager fragmentManager) {
            super(fragmentManager);

            allStationsFragment = StationsListFragment.newInstance(stations);
            favoriteStationsFragment = StationsListFragment.newInstance(favStations);
        }

        @Override
        public int getCount() {
            return NUM_ITEMS;
        }

        @Override
        public Fragment getItem(int position) {
            switch (position) {
                case 0:
                    return allStationsFragment;
                case 1:
                    return favoriteStationsFragment;
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
