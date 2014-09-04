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
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.ViewTreeObserver;
import android.widget.TextView;
import android.widget.Toast;

import org.osmdroid.ResourceProxy;
import org.osmdroid.api.IMapController;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.util.ResourceProxyImpl;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.ItemizedIconOverlay;
import org.osmdroid.views.overlay.ItemizedOverlay;
import org.osmdroid.views.overlay.OverlayItem;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import be.brunoparmentier.openbikesharing.app.R;
import be.brunoparmentier.openbikesharing.app.Station;

public class StationActivity extends Activity {
    private final String PREF_FAV_STATIONS = "fav-stations";
    private final String TAG = "StationActivity";
    private SharedPreferences settings;
    private Station station;
    private MapView map;
    private IMapController mapController;
    private ItemizedOverlay<OverlayItem> stationLocationOverlay;
    private ResourceProxy mResourceProxy;
    private MenuItem favStar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        settings = PreferenceManager.getDefaultSharedPreferences(this);
        mResourceProxy = new ResourceProxyImpl(getApplicationContext());

        setContentView(R.layout.activity_station);
        station = (Station) getIntent().getSerializableExtra("station");

        map = (MapView) findViewById(R.id.mapView);
        final GeoPoint stationLocation = new GeoPoint((int) (station.getLatitude() * 1000000),
                (int) (station.getLongitude() * 1000000));

        mapController = map.getController();
        mapController.setZoom(16);

        /* map tile source */
        String mapLayer = settings.getString("pref_map_layer", "");
        if (mapLayer.equals("mapnik")) {
            map.setTileSource(TileSourceFactory.MAPNIK);
        } else if (mapLayer.equals("cyclemap")) {
            map.setTileSource(TileSourceFactory.CYCLEMAP);
        } else if (mapLayer.equals("osmpublictransport")) {
            map.setTileSource(TileSourceFactory.PUBLIC_TRANSPORT);
        } else if (mapLayer.equals("mapquestosm")) {
            map.setTileSource(TileSourceFactory.MAPQUESTOSM);
        } else {
            map.setTileSource(TileSourceFactory.DEFAULT_TILE_SOURCE);
        }

        map.setMultiTouchControls(true);

        /* Station marker */
        final ArrayList<OverlayItem> items = new ArrayList<OverlayItem>();
        items.add(new OverlayItem("Title", "Description", stationLocation));
        Drawable newMarker = this.getResources().getDrawable(R.drawable.ic_bike);
        this.stationLocationOverlay = new ItemizedIconOverlay<OverlayItem>(items, newMarker,
                new ItemizedIconOverlay.OnItemGestureListener<OverlayItem>() {

                    @Override
                    public boolean onItemSingleTapUp(int i, OverlayItem overlayItem) {

                        return false;
                    }

                    @Override
                    public boolean onItemLongPress(int i, OverlayItem overlayItem) {
                        return false;
                    }
                }, mResourceProxy);

        this.map.getOverlays().add(this.stationLocationOverlay);

        TextView stationName = (TextView) findViewById(R.id.stationName);
        TextView stationEmptySlots = (TextView) findViewById(R.id.stationEmptySlots);
        TextView stationFreeBikes = (TextView) findViewById(R.id.stationFreeBikes);

        stationName.setText(station.getName());
        stationEmptySlots.append(String.valueOf(station.getEmptySlots()));
        stationFreeBikes.append(String.valueOf(station.getFreeBikes()));

        /* Fix for osmdroid 4.2: map was centered at offset (0,0) */
        ViewTreeObserver vto = map.getViewTreeObserver();
        vto.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                    map.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                } else {
                    map.getViewTreeObserver().removeGlobalOnLayoutListener(this);
                }
                mapController.animateTo(stationLocation);
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.station, menu);

        favStar = menu.findItem(R.id.action_favorite);
        if (isFavorite()) {
            favStar.setIcon(R.drawable.ic_action_important);
        } else {
            favStar.setIcon(R.drawable.ic_action_not_important);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_directions) {
            Intent intent = new Intent(Intent.ACTION_VIEW, getStationLocationUri());
            PackageManager packageManager = getPackageManager();
            List<ResolveInfo> activities = packageManager.queryIntentActivities(intent, 0);
            boolean isIntentSafe = activities.size() > 0;
            if (isIntentSafe) {
                startActivity(intent);
            } else {
                Toast.makeText(this, getString(R.string.no_nav_application), Toast.LENGTH_LONG).show();
            }
            return true;
        } else if (id == R.id.action_favorite) {
            setFavorite(!isFavorite());
            return true;
        } else if (id == android.R.id.home) {
            this.finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private Uri getStationLocationUri() {
        return Uri.parse("geo:" + station.getLatitude() + "," + station.getLongitude());
    }

    private boolean isFavorite() {
        Set<String> favorites = settings.getStringSet(PREF_FAV_STATIONS, new HashSet<String>());
        Log.d(TAG, "Station in favorites: " + favorites.contains(station.getId()));
        return (favorites.contains(station.getId()));
    }

    private void setFavorite(boolean favorite) {
        SharedPreferences.Editor editor = settings.edit();
        Set<String> favorites = new HashSet<String>(settings.getStringSet(PREF_FAV_STATIONS,
                new HashSet<String>()));

        if (favorite) {
            favorites.add(station.getId());
            editor.putStringSet(PREF_FAV_STATIONS, favorites);
            editor.commit();
            favStar.setIcon(R.drawable.ic_action_important);
            Toast.makeText(StationActivity.this,
                    getString(R.string.station_added_to_favorites), Toast.LENGTH_SHORT).show();
            Log.d(TAG, "station fav'ed");
        } else {
            favorites.remove(station.getId());
            editor.putStringSet(PREF_FAV_STATIONS, favorites);
            editor.commit();
            favStar.setIcon(R.drawable.ic_action_not_important);
            Toast.makeText(StationActivity.this,
                    getString(R.string.stations_removed_from_favorites), Toast.LENGTH_SHORT).show();
            Log.d(TAG, "station un-fav'ed");
        }
    }
}
