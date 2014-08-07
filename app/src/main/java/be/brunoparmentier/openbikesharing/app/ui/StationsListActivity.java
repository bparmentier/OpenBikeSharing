/*
 * Copyright (c) 2014 Bruno Parmentier. This file is part of OpenBikeSharing.
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

package be.brunoparmentier.openbikesharing.app.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;

import be.brunoparmentier.openbikesharing.app.BikeNetwork;
import be.brunoparmentier.openbikesharing.app.R;
import be.brunoparmentier.openbikesharing.app.Station;
import be.brunoparmentier.openbikesharing.app.utils.OBSException;
import be.brunoparmentier.openbikesharing.app.utils.parser.BikeNetworkParser;


public class StationsListActivity extends Activity {
    private final String BASE_URL = "http://api.citybik.es/v2/networks";
    private ListView listView;
    private BikeNetwork bikeNetwork;
    private ArrayList<Station> stations;
    //private String networkId;
    private final String PREFERENCES_FILE = "Preferences";
    private final String PREF_NETWORK_ID_LABEL = "network-id";
    private final String TAG = "StationsListActivity";
    private boolean firstRun;

    private Menu optionsMenu;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_stations_list);
        SharedPreferences settings = getSharedPreferences(PREFERENCES_FILE, 0);
        listView = (ListView) findViewById(R.id.stationsListView);

        firstRun = settings.getString(PREF_NETWORK_ID_LABEL, "").isEmpty();

        if (firstRun) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setMessage(R.string.welcome_dialog_message);
            builder.setTitle(R.string.welcome_dialog_title);
            builder.setPositiveButton(R.string.welcome_dialog_ok, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    Intent intent = new Intent(StationsListActivity.this, SettingsActivity.class);
                    startActivity(intent);
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
            String stationUrl = BASE_URL + "/" + settings.getString(PREF_NETWORK_ID_LABEL, "");
            new JSONDownloadTask().execute(stationUrl);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        this.optionsMenu = menu;
        getMenuInflater().inflate(R.menu.stations_list, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            Intent intent = new Intent(this, SettingsActivity.class);
            startActivity(intent);
            return true;
        } else if (id == R.id.action_refresh) {
            String networkId = getSharedPreferences(PREFERENCES_FILE, 0).getString(PREF_NETWORK_ID_LABEL, "");
            Log.e(TAG, "URL: " + BASE_URL + "/" + networkId);
            new JSONDownloadTask().execute(BASE_URL + "/" + networkId);
            return true;
        } else if (id == R.id.action_map) {
            if (bikeNetwork != null) {
                Intent intent = new Intent(this, MapActivity.class);
                intent.putExtra("bike-network", bikeNetwork);
                startActivity(intent);
            } else {
                Toast.makeText(this, R.string.bike_network_downloading, Toast.LENGTH_LONG).show();
            }
        }
        return super.onOptionsItemSelected(item);
    }

    private void setRefreshActionButtonState(final boolean refreshing) {
        if (optionsMenu != null) {
            final MenuItem refreshItem = optionsMenu.findItem(R.id.action_refresh);
            if (refreshItem != null) {
                if (refreshing) {
                    refreshItem.setActionView(R.layout.actionbar_indeterminate_progress);
                    refreshItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
                } else {
                    refreshItem.setActionView(null);
                    refreshItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
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
                Log.e(TAG, response.toString());
                return response.toString();
            } catch (IOException e) {
                error = e;
                return e.getMessage();
            }
        }

        @Override
        protected void onPostExecute(String result) {
            if (error != null) {
                Toast.makeText(getApplicationContext(),
                        getApplicationContext().getResources().getString(R.string.connection_error),
                        Toast.LENGTH_SHORT);
                setRefreshActionButtonState(false);
            } else {

                try {
                    JSONObject jsonObject = new JSONObject(result);

                    /* parse result */
                    BikeNetworkParser bikeNetworkParser = new BikeNetworkParser(jsonObject);
                    bikeNetwork = bikeNetworkParser.getNetwork();
                    stations = bikeNetwork.getStations();

                    Collections.sort(stations);

                    final ArrayList<String> list = new ArrayList<String>();
                    for (int i = 0; i < stations.size(); ++i) {
                        list.add(stations.get(i).getName());
                    }
                    ArrayAdapter<String> stationsList = new ArrayAdapter<String>(StationsListActivity.this,
                            android.R.layout.simple_list_item_1, list);

                    listView.setAdapter(stationsList);
                    listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                        @Override
                        public void onItemClick(AdapterView<?> parent, View view,
                                                int position, long id) {
                            Intent intent = new Intent(StationsListActivity.this, StationActivity.class);
                            intent.putExtra("station", stations.get(position));
                            startActivity(intent);
                        }
                    });

                } catch (JSONException e) {
                    Log.e(StationsListActivity.class.toString(), e.getMessage());
                    Toast.makeText(StationsListActivity.this,
                            e.getLocalizedMessage(), Toast.LENGTH_LONG).show();
                } catch (OBSException e) {
                    Log.e(StationsListActivity.class.toString(), e.getMessage());
                    Toast.makeText(StationsListActivity.this,
                            e.getLocalizedMessage(), Toast.LENGTH_LONG).show();
                } finally {
                    setRefreshActionButtonState(false);
                }
            }
        }
    }
}
