var map = L.map('map').setView([30, 0], 1);
var tilesUrl = 'http://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png';
var attribution = 'Map data &copy; <a href="http://openstreetmap.org">OpenStreetMap</a> contributors';

L.tileLayer(tilesUrl, {attribution: attribution, maxZoom: 18}).addTo(map);

$.getJSON("http://api.citybik.es/v2/networks", function(data) {
    length = data.networks.length;
    for (var i = 0; i < length; i++) {
        var network = data.networks[i];
        L.marker([network.location.latitude, network.location.longitude])
            .addTo(map)
            .bindPopup("<strong>" + network.name + "</strong><br />" + network.location.city + " (" + network.company + ")");
    }
});
