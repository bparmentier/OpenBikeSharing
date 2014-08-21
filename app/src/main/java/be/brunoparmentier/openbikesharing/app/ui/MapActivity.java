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
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.Menu;
import android.view.MenuItem;
import android.view.ViewTreeObserver;
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
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider;
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay;

import java.util.ArrayList;

import be.brunoparmentier.openbikesharing.app.BikeNetwork;
import be.brunoparmentier.openbikesharing.app.R;
import be.brunoparmentier.openbikesharing.app.Station;

public class MapActivity extends Activity {

    SharedPreferences settings;
    private BikeNetwork bikeNetwork;
    private ArrayList<Station> stations;
    private MapView map;
    private IMapController mapController;
    private ItemizedOverlay<OverlayItem> stationLocationOverlay;
    private ResourceProxy mResourceProxy;
    private MyLocationNewOverlay myLocationOverlay;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);

        settings = PreferenceManager.getDefaultSharedPreferences(this);
        mResourceProxy = new ResourceProxyImpl(getApplicationContext());

        try {
            bikeNetwork = (BikeNetwork) getIntent().getSerializableExtra("bike-network");
        } catch (NullPointerException e) {
            Toast.makeText(this, R.string.bike_network_downloading, Toast.LENGTH_LONG).show();
            finish();
        }

        stations = bikeNetwork.getStations();

        map = (MapView) findViewById(R.id.mapView);

        /* markers list */
        final ArrayList<OverlayItem> items = new ArrayList<OverlayItem>();

        for (Station station : stations) {
            GeoPoint stationLocation = new GeoPoint((int) (station.getLatitude() * 1000000),
                    (int) (station.getLongitude() * 1000000));
            items.add(new OverlayItem("Title", "Description", stationLocation));
        }

        mapController = map.getController();
        mapController.setZoom(16);

        map.setMultiTouchControls(true);
        map.setBuiltInZoomControls(true);
        map.setMinZoomLevel(3);

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

        /* Stations markers */
        Drawable newMarker = this.getResources().getDrawable(R.drawable.ic_bike);
        stationLocationOverlay = new ItemizedIconOverlay<OverlayItem>(items, newMarker,
                new ItemizedIconOverlay.OnItemGestureListener<OverlayItem>() {

                    @Override
                    public boolean onItemSingleTapUp(int i, OverlayItem overlayItem) {
                        Toast.makeText(MapActivity.this,
                                stations.get(i).getName()
                                        + "\n" + getString(R.string.free_bikes)
                                        + stations.get(i).getFreeBikes()
                                        + "\n" + getString(R.string.empty_slots)
                                        + stations.get(i).getEmptySlots(),
                                Toast.LENGTH_SHORT).show();

                        return true;
                    }

                    @Override
                    public boolean onItemLongPress(int i, OverlayItem overlayItem) {
                        return false;
                    }
                }, mResourceProxy);


        GpsMyLocationProvider imlp = new GpsMyLocationProvider(this.getBaseContext());
        imlp.setLocationUpdateMinDistance(1000);
        imlp.setLocationUpdateMinTime(60000);

        myLocationOverlay = new MyLocationNewOverlay(this.getBaseContext(), imlp, this.map);

        map.getOverlays().add(this.stationLocationOverlay);
        map.getOverlays().add(this.myLocationOverlay);

        myLocationOverlay.enableMyLocation();

        try {
            LocationManager locationManager =
                    (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
            GeoPoint myLocation = new GeoPoint(locationManager
                    .getLastKnownLocation(LocationManager.NETWORK_PROVIDER));
            mapController.animateTo(myLocation);
        } catch (NullPointerException e) {
            mapController.setZoom(13);

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
                    mapController.animateTo(new GeoPoint(bikeNetwork.getLocation().getLatitude(),
                            bikeNetwork.getLocation().getLongitude()));
                }
            });

            Toast.makeText(this, R.string.location_not_found, Toast.LENGTH_LONG).show();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        myLocationOverlay.disableMyLocation();
    }

    @Override
    protected void onResume() {
        super.onResume();
        myLocationOverlay.enableMyLocation();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.map, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        return super.onOptionsItemSelected(item);
    }
}
