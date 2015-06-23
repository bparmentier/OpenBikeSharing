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

import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;

import java.util.ArrayList;
import java.util.List;

import be.brunoparmentier.openbikesharing.app.R;
import be.brunoparmentier.openbikesharing.app.db.StationsDataSource;
import be.brunoparmentier.openbikesharing.app.models.Station;

public class StationsListAppWidgetService extends RemoteViewsService {
    @Override
    public RemoteViewsFactory onGetViewFactory(Intent intent) {
        return new StationsListAppWidgetFactory(this.getApplicationContext(), intent);
    }
}

class StationsListAppWidgetFactory implements RemoteViewsService.RemoteViewsFactory {
    private List<Station> stations = new ArrayList<>();
    private Context mContext;
    private int mAppWidgetId;
    private StationsDataSource stationsDataSource;

    public StationsListAppWidgetFactory(Context context, Intent intent) {
        mContext = context;
        mAppWidgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,
                AppWidgetManager.INVALID_APPWIDGET_ID);
    }

    @Override
    public void onCreate() {
        // In onCreate() you setup any connections / cursors to your data source. Heavy lifting,
        // for example downloading or creating content etc, should be deferred to onDataSetChanged()
        // or getViewAt(). Taking more than 20 seconds in this call will result in an ANR.
        stationsDataSource = new StationsDataSource(mContext);
        stations = stationsDataSource.getFavoriteStations();
    }

    @Override
    public void onDataSetChanged() {
        // This is triggered when you call AppWidgetManager notifyAppWidgetViewDataChanged
        // on the collection view corresponding to this factory. You can do heaving lifting in
        // here, synchronously. For example, if you need to process an image, fetch something
        // from the network, etc., it is ok to do it here, synchronously. The widget will remain
        // in its current state while work is being done here, so you don't need to worry about
        // locking up the widget.

        stations = stationsDataSource.getFavoriteStations();
    }

    @Override
    public void onDestroy() {
        // In onDestroy() you should tear down anything that was setup for your data source,
        // eg. cursors, connections, etc.
        stations.clear();
    }

    @Override
    public int getCount() {
        return stations.size();
    }

    @Override
    public RemoteViews getViewAt(int position) {
        // We construct a remote views item based on our widget item xml file, and set the
        // text based on the position.
        RemoteViews rv = new RemoteViews(mContext.getPackageName(), R.layout.app_widget_item);
        rv.setTextViewText(R.id.widgetStationNameTitle, stations.get(position).getName());
        rv.setTextViewText(R.id.widgetFreeBikesValue, String.valueOf(stations.get(position).getFreeBikes()));
        rv.setTextViewText(R.id.widgetEmptySlotsValue, String.valueOf(stations.get(position).getEmptySlots()));

        // Next, we set a fill-intent which will be used to fill-in the pending intent template
        // which is set on the collection view in StationsListAppWidgetProvider.
        Bundle extras = new Bundle();
        extras.putInt(StationsListAppWidgetProvider.EXTRA_ITEM, position);
        Intent fillInIntent = new Intent();
        fillInIntent.putExtras(extras);
        rv.setOnClickFillInIntent(R.id.widgetLayout, fillInIntent);

        // Return the remote views object.
        return rv;
    }

    @Override
    public RemoteViews getLoadingView() {
        return null;
    }

    @Override
    public int getViewTypeCount() {
        return 1;
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public boolean hasStableIds() {
        return true;
    }
}
