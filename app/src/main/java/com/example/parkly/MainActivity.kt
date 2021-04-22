package com.example.parkly

import android.annotation.SuppressLint
import android.location.Location
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.drawable.toBitmap
import com.example.parkly.databinding.ActivityMainBinding
import com.mapbox.android.core.permissions.PermissionsListener
import com.mapbox.android.core.permissions.PermissionsManager
import com.mapbox.geojson.Point
import com.mapbox.mapboxsdk.Mapbox
import com.mapbox.mapboxsdk.geometry.LatLng
import com.mapbox.mapboxsdk.location.LocationComponentActivationOptions
import com.mapbox.mapboxsdk.location.LocationComponentOptions
import com.mapbox.mapboxsdk.location.modes.CameraMode
import com.mapbox.mapboxsdk.location.modes.RenderMode
import com.mapbox.mapboxsdk.maps.MapView
import com.mapbox.mapboxsdk.maps.MapboxMap
import com.mapbox.mapboxsdk.maps.Style
import com.mapbox.mapboxsdk.style.layers.PropertyFactory
import com.mapbox.mapboxsdk.style.layers.SymbolLayer
import com.mapbox.mapboxsdk.style.sources.GeoJsonSource
import com.mapbox.mapboxsdk.style.sources.Source

class MainActivity : AppCompatActivity(),PermissionsListener {
    companion object{
       private const val MAPBOX_KEY = "pk.eyJ1IjoicG1hdGFyMjhjb2RlIiwiYSI6ImNrbnJ4anpzYTBuMzkyb3Bob3lwNjI3bTcifQ.1vv5YfZsK6KtKLd_cG7CQw"
       private const val PIN_IMAGE = "PIN_IMAGE"
       private const val CAR_LOCATION_SOURCE = "CAR_LOCATION_SOURCE"
       private const val CAR_LOCATION_SYMBOL = "CAR_LOCATION_SYMBOL"
    }
    private var mapView:MapView ? =null
    private var map: MapboxMap ? =null
    private var permissionsManager : PermissionsManager ? = null
    private var parkedCarLocation : Point? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Mapbox.getInstance(this, MAPBOX_KEY)
        val binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        mapView = binding.mapView.apply {
            onCreate(savedInstanceState)
            getMapAsync{ mapboxMap ->
                map = mapboxMap
                mapboxMap.setStyle(Style.MAPBOX_STREETS) { style ->
                    val pinImage = resources.getDrawable(R.drawable.ic_pin, null)
                    style.addImage(PIN_IMAGE, pinImage)
                }

                map?.addOnMapLongClickListener { latLng ->
                    val point = Point.fromLngLat(latLng.longitude, latLng.latitude)
                    map?.getStyle {
                        var image = addParkedCar(it, point)
                        binding.carFav.setImageResource(image)
                    }
                    true
                }
            }
        }

        binding.usersLocationFab.setOnClickListener {
            map?.getStyle { enableUserLocation(it) }
        }

        binding.carFav.setOnClickListener {
            map?.getStyle {
                val lastKownLocation = map?.locationComponent?.lastKnownLocation ?: return@getStyle
                val point =
                    Point.fromLngLat(lastKownLocation.longitude, lastKownLocation.latitude)
                var image = addParkedCar(it,point)
                binding.carFav.setImageResource(image)
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun enableUserLocation(style:Style) {
        if (PermissionsManager.areLocationPermissionsGranted(this)) {
            var locationComponentOptions = LocationComponentOptions.builder(this)
                    .pulseEnabled(true)
                    .build()
            var locationComponentActivationOptions = LocationComponentActivationOptions.builder(this,style)
                    .locationComponentOptions(locationComponentOptions)
                    .build()
            map?.locationComponent?.apply {
                activateLocationComponent(locationComponentActivationOptions)
                isLocationComponentEnabled = true
                setCameraMode(CameraMode.TRACKING,2000L,12.0,null,null,null)
                RenderMode.COMPASS
            }
        } else {
            permissionsManager = PermissionsManager(this)
            permissionsManager?.requestLocationPermissions(this)
        }
    }

    override fun onExplanationNeeded(permissionsToExplain: MutableList<String>?) {
        AlertDialog.Builder(this)
                .setMessage(R.string.location_permission_explanation)
                .setPositiveButton(android.R.string.ok,null)
                .setNegativeButton(android.R.string.cancel,null)
    }

    fun addParkedCar(style:Style, latLng: Point):Int{
        enableUserLocation(style)
        return if(parkedCarLocation == null) {
            parkedCarLocation = latLng
            val geoJsonStyle = GeoJsonSource(CAR_LOCATION_SOURCE, parkedCarLocation)
            val symbolLayer = SymbolLayer(CAR_LOCATION_SYMBOL, CAR_LOCATION_SOURCE).apply {
                withProperties(PropertyFactory.iconImage(PIN_IMAGE))
            }
            style.apply {

                addSource(geoJsonStyle)
                addLayer(symbolLayer)
            }
            R.drawable.found_car
        }else{
            style.apply {
                removeLayer(CAR_LOCATION_SYMBOL)
                removeSource(CAR_LOCATION_SOURCE)
            }
            parkedCarLocation = null
            R.drawable.park_car
        }

    }

    override fun onPermissionResult(granted: Boolean) {
        if(granted){
            map?.getStyle { enableUserLocation(it) }
        }
    }

    override fun onStart() {
        super.onStart()
        mapView?.onStart()
    }

    override fun onResume() {
        super.onResume()
        mapView?.onResume()
    }

    override fun onPause() {
        super.onPause()
        mapView?.onPause()
    }

    override fun onStop() {
        super.onStop()
        mapView?.onStop()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        mapView?.onSaveInstanceState(outState)
    }

    override fun onLowMemory() {
        super.onLowMemory()
        mapView?.onLowMemory()
    }

    override fun onDestroy() {
        super.onDestroy()
        mapView?.onDestroy()
    }
}