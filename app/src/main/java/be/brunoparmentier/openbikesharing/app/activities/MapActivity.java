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

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.NavUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import org.osmdroid.api.IMapController;
import org.osmdroid.bonuspack.clustering.GridMarkerClusterer;
import org.osmdroid.events.MapEventsReceiver;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.MapEventsOverlay;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.infowindow.InfoWindow;
import org.osmdroid.views.overlay.infowindow.MarkerInfoWindow;
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider;
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay;

import java.util.ArrayList;

import be.brunoparmentier.openbikesharing.app.R;
import be.brunoparmentier.openbikesharing.app.db.StationsDataSource;
import be.brunoparmentier.openbikesharing.app.models.Station;
import be.brunoparmentier.openbikesharing.app.models.StationStatus;

public class MapActivity extends Activity implements MapEventsReceiver {
    private static final String TAG = "MapActivity";
    private static final String MAP_CURRENT_ZOOM_KEY = "map-current-zoom";
    private static final String MAP_CENTER_LAT_KEY = "map-center-lat";
    private static final String MAP_CENTER_LON_KEY = "map-center-lon";

    private static final String PREF_KEY_NETWORK_LATITUDE = "network-latitude";
    private static final String PREF_KEY_NETWORK_LONGITUDE = "network-longitude";
    private static final String PREF_KEY_MAP_LAYER = "pref_map_layer";
    private static final String KEY_STATION = "station";
    private static final String MAP_LAYER_MAPNIK = "mapnik";
    private static final String MAP_LAYER_CYCLEMAP = "cyclemap";
    private static final String MAP_LAYER_OSMPUBLICTRANSPORT = "osmpublictransport";

    private MapView map;
    private IMapController mapController;
    private MyLocationNewOverlay myLocationOverlay;
    private StationMarkerInfoWindow stationMarkerInfoWindow;
    private StationsDataSource stationsDataSource;

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(MAP_CURRENT_ZOOM_KEY, map.getZoomLevel());
        outState.putDouble(MAP_CENTER_LAT_KEY, map.getMapCenter().getLatitude());
        outState.putDouble(MAP_CENTER_LON_KEY, map.getMapCenter().getLongitude());
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);
        getActionBar().setDisplayHomeAsUpEnabled(true);

        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);

        stationsDataSource = new StationsDataSource(this);
        ArrayList<Station> stations = stationsDataSource.getStations();

        map = (MapView) findViewById(R.id.mapView);
        stationMarkerInfoWindow = new StationMarkerInfoWindow(R.layout.bonuspack_bubble, map);

        /* handling map events */
        MapEventsOverlay mapEventsOverlay = new MapEventsOverlay(this, this);
        map.getOverlays().add(0, mapEventsOverlay);

        /* markers list */
        GridMarkerClusterer stationsMarkers = new GridMarkerClusterer();
        Drawable clusterIconD = getResources().getDrawable(R.drawable.marker_cluster);
        Bitmap clusterIcon = ((BitmapDrawable) clusterIconD).getBitmap();
        map.getOverlays().add(stationsMarkers);
        stationsMarkers.setIcon(clusterIcon);
        stationsMarkers.setGridSize(100);

        for (final Station station : stations) {
            stationsMarkers.add(createStationMarker(station));
        }
        map.invalidate();

        map.setMultiTouchControls(true);
        map.setBuiltInZoomControls(true);
        map.setMinZoomLevel(3);

        /* map tile source */
        String mapLayer = settings.getString(PREF_KEY_MAP_LAYER, "");
        switch (mapLayer) {
            case MAP_LAYER_MAPNIK:
                map.setTileSource(TileSourceFactory.MAPNIK);
                break;
            case MAP_LAYER_CYCLEMAP:
                map.setTileSource(TileSourceFactory.CYCLEMAP);
                break;
            case MAP_LAYER_OSMPUBLICTRANSPORT:
                map.setTileSource(TileSourceFactory.PUBLIC_TRANSPORT);
                break;
            default:
                map.setTileSource(TileSourceFactory.DEFAULT_TILE_SOURCE);
                break;
        }

        GpsMyLocationProvider imlp = new GpsMyLocationProvider(this.getBaseContext());
        imlp.setLocationUpdateMinDistance(1000);
        imlp.setLocationUpdateMinTime(60000);
        map.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

            }
        });

        myLocationOverlay = new MyLocationNewOverlay(imlp, this.map);
        myLocationOverlay.enableMyLocation();
        map.getOverlays().add(this.myLocationOverlay);

        mapController = map.getController();
        if (savedInstanceState != null) {
            mapController.setZoom(savedInstanceState.getInt(MAP_CURRENT_ZOOM_KEY));
            mapController.setCenter(new GeoPoint(savedInstanceState.getDouble(MAP_CENTER_LAT_KEY),
                    savedInstanceState.getDouble(MAP_CENTER_LON_KEY)));
        } else {
            LocationManager locationManager =
                    (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
            Location userLocation = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
            if (userLocation != null) {
                mapController.setZoom(16);
                mapController.animateTo(new GeoPoint(userLocation));
            } else {
                double bikeNetworkLatitude = Double.longBitsToDouble(settings.getLong(PREF_KEY_NETWORK_LATITUDE, 0));
                double bikeNetworkLongitude = Double.longBitsToDouble(settings.getLong(PREF_KEY_NETWORK_LONGITUDE, 0));
                mapController.setZoom(13);
                mapController.setCenter(new GeoPoint(bikeNetworkLatitude, bikeNetworkLongitude));

                Toast.makeText(this, R.string.location_not_found, Toast.LENGTH_LONG).show();
            }
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
                    Log.e(TAG, "Location not found");
                    return true;
                }
            case android.R.id.home:
                NavUtils.navigateUpFromSameTask(this);
                return true;
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
        marker.setTitle(station.getName());
        marker.setSnippet(String.valueOf(station.getFreeBikes())); // free bikes
        if (station.getEmptySlots() != -1) {
            marker.setSubDescription(String.valueOf(station.getEmptySlots())); // empty slots
        }

        /* Marker icon */
        int emptySlots = station.getEmptySlots();
        int freeBikes = station.getFreeBikes();

        if ((emptySlots == 0 && freeBikes == 0) || station.getStatus() == StationStatus.CLOSED) {
            marker.setIcon(getResources().getDrawable(R.drawable.ic_station_marker_unavailable));
        } else {
            double ratio = (double) freeBikes / (double) (freeBikes + emptySlots);
            if (freeBikes == 0) {
                marker.setIcon(getResources().getDrawable(R.drawable.ic_station_marker0));
            } else if (freeBikes >= 1 && ratio <= 0.3) {
                marker.setIcon(getResources().getDrawable(R.drawable.ic_station_marker25));
            } else if (ratio > 0.3 && ratio < 0.7) {
                marker.setIcon(getResources().getDrawable(R.drawable.ic_station_marker50));
            } else if (ratio >= 0.7 && emptySlots >= 1) {
                marker.setIcon(getResources().getDrawable(R.drawable.ic_station_marker75));
            } else if (emptySlots == 0 || emptySlots == -1) {
                marker.setIcon(getResources().getDrawable(R.drawable.ic_station_marker100));
            }
        }

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
            if (markerStation.getEmptySlots() == -1) {
                ImageView emptySlotsLogo = (ImageView) getView().findViewById(R.id.bubble_emptyslots_logo);
                emptySlotsLogo.setVisibility(View.GONE);
            }
            layout.setClickable(true);
            layout.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Intent intent = new Intent(MapActivity.this, StationActivity.class);
                    intent.putExtra(KEY_STATION, markerStation);
                    startActivity(intent);
                }
            });
        }
    }
}
