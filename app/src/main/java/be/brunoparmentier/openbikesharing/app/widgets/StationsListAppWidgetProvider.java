/*
 * Copyright (c) 2015 Bruno Parmentier. This file is part of OpenBikeSharing.
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

package be.brunoparmentier.openbikesharing.app.widgets;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.RemoteViews;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.ParseException;
import java.util.ArrayList;

import be.brunoparmentier.openbikesharing.app.R;
import be.brunoparmentier.openbikesharing.app.activities.StationsListActivity;
import be.brunoparmentier.openbikesharing.app.db.StationsDataSource;
import be.brunoparmentier.openbikesharing.app.models.BikeNetwork;
import be.brunoparmentier.openbikesharing.app.models.Station;
import be.brunoparmentier.openbikesharing.app.parsers.BikeNetworkParser;


/**
 * Implementation of App Widget functionality.
 */
public class StationsListAppWidgetProvider extends AppWidgetProvider {
    private static final String TAG = StationsListAppWidgetProvider.class.getSimpleName();

    private static final String BASE_URL = "http://api.citybik.es/v2/networks";
    private static final String PREF_KEY_NETWORK_ID = "network-id";
    private static final String PREF_KEY_DB_LAST_UPDATE = "db_last_update";

    public static final String EXTRA_ITEM = "be.brunoparmentier.openbikesharing.app.widget.EXTRA_ITEM";
    public static final String EXTRA_REFRESH_LIST_ONLY =
            "be.brunoparmentier.openbikesharing.app.widget.EXTRA_REFRESH_LIST_ONLY";

    private ArrayList<Station> stations;
    private Context mContext;

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        Log.d(TAG, "onUpdate");

        // update each of the widgets with the remote adapter
        for (int appWidgetId : appWidgetIds) {
            RemoteViews rv = new RemoteViews(context.getPackageName(), R.layout.app_widget);

            // The empty view is displayed when the collection has no items. It should be a sibling
            // of the collection view.
            rv.setEmptyView(R.id.widgetStationsList, R.id.widgetEmptyView);

            // Here we setup the intent which points to the StationsListAppWidgetService which will
            // provide the views for this collection.
            Intent intent = new Intent(context, StationsListAppWidgetService.class);
            intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
            // When intents are compared, the extras are ignored, so we need to embed the extras
            // into the data so that the extras will not be ignored.
            intent.setData(Uri.parse(intent.toUri(Intent.URI_INTENT_SCHEME)));
            rv.setRemoteAdapter(R.id.widgetStationsList, intent);

            // Click on the refresh button updates the stations
            final Intent refreshIntent = new Intent(context, StationsListAppWidgetProvider.class);
            refreshIntent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
            final PendingIntent refreshPendingIntent = PendingIntent.getBroadcast(context, 0,
                    refreshIntent, PendingIntent.FLAG_UPDATE_CURRENT);
            rv.setOnClickPendingIntent(R.id.widgetRefreshButton, refreshPendingIntent);

            // Click on the widget title launches application
            final Intent openAppIntent = new Intent(Intent.ACTION_MAIN);
            openAppIntent.addCategory(Intent.CATEGORY_LAUNCHER);
            openAppIntent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
            openAppIntent.setComponent(new ComponentName(context.getPackageName(),
                    StationsListActivity.class.getCanonicalName()));
            final PendingIntent openAppPendingIntent = PendingIntent.getActivity(context, 0,
                    openAppIntent, PendingIntent.FLAG_UPDATE_CURRENT);
            rv.setOnClickPendingIntent(R.id.widgetTitle, openAppPendingIntent);

            appWidgetManager.updateAppWidget(appWidgetId, rv);
        }
        super.onUpdate(context, appWidgetManager, appWidgetIds);
    }

    @Override
    public void onEnabled(Context context) {
        // Enter relevant functionality for when the first widget is created
        super.onEnabled(context);
    }

    @Override
    public void onDisabled(Context context) {
        // Enter relevant functionality for when the last widget is disabled
        super.onDisabled(context);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);

        Log.d(TAG, intent.getAction() + " received");
        mContext = context;

        if (intent.getAction().equals(AppWidgetManager.ACTION_APPWIDGET_UPDATE)) {
            if (intent.getBooleanExtra(EXTRA_REFRESH_LIST_ONLY, false)) {
                /* Update widget list with data from database */
                final AppWidgetManager mgr = AppWidgetManager.getInstance(mContext);
                final ComponentName cn = new ComponentName(mContext, StationsListAppWidgetProvider.class);
                mgr.notifyAppWidgetViewDataChanged(mgr.getAppWidgetIds(cn), R.id.widgetStationsList);
            } else {
                /* Download new data then update widget list */
                String networkId = PreferenceManager.getDefaultSharedPreferences(context).getString(PREF_KEY_NETWORK_ID, "");
                String stationUrl = BASE_URL + "/" + networkId;

                new JSONDownloadTask().execute(stationUrl);
            }
        }
    }

    private class JSONDownloadTask extends AsyncTask<String, Void, String> {

        @Override
        protected String doInBackground(String... urls) {
            if (urls[0].isEmpty()) {
                return null;
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
                Log.d(TAG, e.getMessage());
                return e.getMessage();
            }
        }

        @Override
        protected void onPostExecute(final String result) {
            if (result == null) {
                Log.d(TAG, "Result NOK");
            } else {
                try {
                    /* parse result */
                    BikeNetworkParser bikeNetworkParser = new BikeNetworkParser(result, true);
                    BikeNetwork bikeNetwork = bikeNetworkParser.getNetwork();

                    stations = bikeNetwork.getStations();

                    StationsDataSource stationsDataSource = new StationsDataSource(mContext);
                    stationsDataSource.storeStations(stations);

                    PreferenceManager.getDefaultSharedPreferences(mContext).edit()
                            .putLong(PREF_KEY_DB_LAST_UPDATE, System.currentTimeMillis())
                            .apply();

                    final AppWidgetManager mgr = AppWidgetManager.getInstance(mContext);
                    final ComponentName cn = new ComponentName(mContext, StationsListAppWidgetProvider.class);
                    mgr.notifyAppWidgetViewDataChanged(mgr.getAppWidgetIds(cn), R.id.widgetStationsList);

                } catch (ParseException e) {
                    Log.e(TAG, e.getMessage());
                }
            }
        }
    }
}

