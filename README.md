OpenBikeSharing
===============

OpenBikeSharing is an Android application that displays the availability of shared bikes in your
city.

It uses the [CityBikes API](http://api.citybik.es/v2/) that provides data for more than 170 cities
(in 19 countries) and displays those data on an [OpenStreetMap](https://www.openstreetmap.org) layer
thanks to the [osmdroid](https://github.com/osmdroid/osmdroid) library (you can choose between
multiple layers).

Download
--------

OpenBikeSharing is available on
[F-Droid](https://f-droid.org/repository/browse/?fdid=be.brunoparmentier.openbikesharing.app) and
[Google Play](https://play.google.com/store/apps/details?id=be.brunoparmentier.openbikesharing.app).
Signed APK's can also be found on GitHub in the
[Releases](https://github.com/bparmentier/OpenBikeSharing/releases) section.

Contribute
----------

This is my first Android application. I probably don't comply with all the "good practices" and the
code may be a bit messy. So if you have enough courage, take a look at it and tell me what is wrong!

If your language is not supported yet, I will gladly add it if you translate some strings for me.
They are located in `app/src/main/res/values-xx` (there is not much).

As I don't have as many devices as I would like to test this app on (only one, actually), please
report any bug/crash that you may encounter. And feel free to make any suggestions to improve it.

Build
-----

If you use Android Studio, you can import the project directly from GitHub.

Otherwise you can build it from the command line with
[Gradle](https://developer.android.com/sdk/installing/studio-build.html).  
Clone the repo and type:

    ./gradlew build

(You may need to `chmod +x` the `gradlew` script)

If the build fails, try to add the following to `app/build.gradle`:

    android {
        …
        lintOptions {
            abortOnError false
        }
    }

The Gradle script will take care of downloading the necessary libraries and will generate the APK's
in `app/build/outputs/apk`.

Permissions
-----------

The following permissions are needed by OpenBikeSharing (those are the same that are required by
osmdroid):

* ACCESS_COARSE_LOCATION
* ACCESS_FINE_LOCATION
* ACCESS_WIFI_STATE
* ACCESS_NETWORK_STATE
* INTERNET
* WRITE_EXTERNAL_STORAGE

Internet access is needed to download the map tiles and the stations. Location access is only used
to locate you on the map. Access to the SD card is required by osmdroid to cache the tiles.