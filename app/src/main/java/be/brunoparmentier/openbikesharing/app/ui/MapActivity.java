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
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.LinearLayout;
import android.widget.Toast;

import org.osmdroid.api.IMapController;
import org.osmdroid.bonuspack.clustering.GridMarkerClusterer;
import org.osmdroid.bonuspack.overlays.InfoWindow;
import org.osmdroid.bonuspack.overlays.MapEventsOverlay;
import org.osmdroid.bonuspack.overlays.MapEventsReceiver;
import org.osmdroid.bonuspack.overlays.Marker;
import org.osmdroid.bonuspack.overlays.MarkerInfoWindow;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider;
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay;

import java.util.ArrayList;

import be.brunoparmentier.openbikesharing.app.BikeNetwork;
import be.brunoparmentier.openbikesharing.app.R;
import be.brunoparmentier.openbikesharing.app.Station;

public class MapActivity extends Activity implements MapEventsReceiver {

    private SharedPreferences settings;
    private BikeNetwork bikeNetwork;
    private ArrayList<Station> stations;
    private MapView map;
    private IMapController mapController;
    private MyLocationNewOverlay myLocationOverlay;
    private StationMarkerInfoWindow stationMarkerInfoWindow;
    private GridMarkerClusterer stationsMarkers;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);

        settings = PreferenceManager.getDefaultSharedPreferences(this);

        try {
            bikeNetwork = (BikeNetwork) getIntent().getSerializableExtra("bike-network");
        } catch (NullPointerException e) {
            Toast.makeText(this, R.string.bike_network_downloading, Toast.LENGTH_LONG).show();
            finish();
        }

        stations = bikeNetwork.getStations();

        map = (MapView) findViewById(R.id.mapView);
        stationMarkerInfoWindow = new StationMarkerInfoWindow(R.layout.bonuspack_bubble, map);

        /* handling map events */
        MapEventsOverlay mapEventsOverlay = new MapEventsOverlay(this, this);
        map.getOverlays().add(0, mapEventsOverlay);

        /* markers list */
        stationsMarkers = new GridMarkerClusterer(this);
        Drawable clusterIconD = getResources().getDrawable(R.drawable.marker_cluster);
        Bitmap clusterIcon = ((BitmapDrawable) clusterIconD).getBitmap();
        map.getOverlays().add(stationsMarkers);
        stationsMarkers.setIcon(clusterIcon);
        stationsMarkers.setGridSize(100);

        for (final Station station : stations) {
            stationsMarkers.add(createStationMarker(station));
        }
        map.invalidate();

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

        GpsMyLocationProvider imlp = new GpsMyLocationProvider(this.getBaseContext());
        imlp.setLocationUpdateMinDistance(1000);
        imlp.setLocationUpdateMinTime(60000);

        myLocationOverlay = new MyLocationNewOverlay(this.getBaseContext(), imlp, this.map);
        map.getOverlays().add(this.myLocationOverlay);

        myLocationOverlay.enableMyLocation();

        try {
            LocationManager locationManager =
                    (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
            GeoPoint userLocation = new GeoPoint(locationManager
                    .getLastKnownLocation(LocationManager.NETWORK_PROVIDER));
            mapController.animateTo(userLocation);
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
        switch (item.getItemId()) {
            case R.id.action_my_location:
                try {
                    LocationManager locationManager =
                            (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
                    GeoPoint userLocation = new GeoPoint(locationManager
                            .getLastKnownLocation(LocationManager.NETWORK_PROVIDER));
                    mapController.animateTo(userLocation);
                    return true;
                } catch (NullPointerException ex) {
                    Toast.makeText(this, getString(R.string.location_not_found), Toast.LENGTH_LONG).show();
                    Log.e("MapActivity", "Location not found");
                    return true;
                }
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public boolean singleTapConfirmedHelper(GeoPoint geoPoint) {
        InfoWindow.closeAllInfoWindowsOn(map);
        return true;
    }

    @Override
    public boolean longPressHelper(GeoPoint geoPoint) {
        return false;
    }

    private Marker createStationMarker(Station station) {
        GeoPoint stationLocation = new GeoPoint((int) (station.getLatitude() * 1000000),
                (int) (station.getLongitude() * 1000000));
        Marker marker = new Marker(map);
        marker.setRelatedObject(station);
        marker.setInfoWindow(stationMarkerInfoWindow);
        marker.setPosition(stationLocation);
        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER);
        marker.setIcon(getResources().getDrawable(R.drawable.ic_bike));
        marker.setTitle(station.getName());
        marker.setSnippet(String.valueOf(station.getFreeBikes())); // free bikes
        marker.setSubDescription(String.valueOf(station.getEmptySlots())); // empty slots

        return marker;
    }

    private class StationMarkerInfoWindow extends MarkerInfoWindow {

        public StationMarkerInfoWindow(int layoutResId, final MapView mapView) {
            super(layoutResId, mapView);
        }

        @Override
        public void onOpen(Object item) {
            Marker marker = (Marker) item;
            final Station markerStation = (Station) marker.getRelatedObject();
            super.onOpen(item);
            closeAllInfoWindowsOn(map);

            LinearLayout layout = (LinearLayout) getView().findViewById(R.id.map_bubble_layout);
            layout.setClickable(true);
            layout.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Intent intent = new Intent(MapActivity.this, StationActivity.class);
                    intent.putExtra("station", markerStation);
                    startActivity(intent);
                }
            });
        }
    }
}
