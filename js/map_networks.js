var tilesUrl = 'http://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png';
var attribution = 'Map data &copy; <a href="http://openstreetmap.org">OpenStreetMap</a> contributors';
var tiles = L.tileLayer(tilesUrl, {attribution: attribution, maxZoom: 18});

var map = L.map('map', {center: L.latLng(30, 0), zoom: 1, minZoom: 1, layers: [tiles]});
var markers = new L.MarkerClusterGroup({showCoverageOnHover: false});

$.getJSON("http://api.citybik.es/v2/networks", function(data) {
    length = data.networks.length;
    for (var i = 0; i < length; i++) {
        var network = data.networks[i];
        var marker = L.marker([network.location.latitude, network.location.longitude]);
        marker.bindPopup("<strong>" + network.name + "</strong><br />" + network.location.city + " (" + network.company + ")");
        markers.addLayer(marker);
    }

    map.addLayer(markers);
});
