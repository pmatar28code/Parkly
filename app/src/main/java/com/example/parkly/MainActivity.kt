package com.example.parkly

import android.annotation.SuppressLint
import android.graphics.Color
import android.location.Location
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.drawable.toBitmap
import com.example.parkly.databinding.ActivityMainBinding
import com.mapbox.android.core.permissions.PermissionsListener
import com.mapbox.android.core.permissions.PermissionsManager
import com.mapbox.api.directions.v5.DirectionsCriteria
import com.mapbox.api.directions.v5.MapboxDirections
import com.mapbox.api.directions.v5.models.DirectionsResponse
import com.mapbox.core.constants.Constants
import com.mapbox.geojson.LineString
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
import com.mapbox.mapboxsdk.style.layers.LineLayer
import com.mapbox.mapboxsdk.style.layers.Property
import com.mapbox.mapboxsdk.style.layers.PropertyFactory
import com.mapbox.mapboxsdk.style.layers.SymbolLayer
import com.mapbox.mapboxsdk.style.sources.GeoJsonSource
import com.mapbox.mapboxsdk.style.sources.Source
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class MainActivity : AppCompatActivity(),PermissionsListener {
    companion object{
        private const val MAPBOX_KEY = "pk.eyJ1IjoicG1hdGFyMjhjb2RlIiwiYSI6ImNrbnJ4anpzYTBuMzkyb3Bob3lwNjI3bTcifQ.1vv5YfZsK6KtKLd_cG7CQw"
        private const val PIN_IMAGE = "PIN_IMAGE"
        private const val CAR_LOCATION_SOURCE = "CAR_LOCATION_SOURCE"
        private const val CAR_LOCATION_SYMBOL = "CAR_LOCATION_SYMBOL"
        private const val ROUTE_SOURCE = "ROUTE_SOURCE"
        private const val ROUTE_LAYER = "ROUTE_LAYER"
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

                val routeLayer = LineLayer(ROUTE_LAYER, ROUTE_SOURCE).apply {
                    setProperties(
                        PropertyFactory.lineCap(Property.LINE_CAP_ROUND),
                        PropertyFactory.lineJoin(Property.LINE_JOIN_ROUND),
                        PropertyFactory.lineWidth(5f),
                        PropertyFactory.lineColor(Color.BLUE)

                    )
                }
                
                map?.getStyle { style ->
                    style.addSource(GeoJsonSource(ROUTE_SOURCE))
                    style.addLayer(routeLayer)
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

        binding.directionsFab.setOnClickListener {
            getDirections()
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

    private fun getDirections(){
        val lastKnownLocation = map?.locationComponent?.lastKnownLocation ?: return
        if(parkedCarLocation == null){
            return
        }
        val origin = Point.fromLngLat(lastKnownLocation.longitude,lastKnownLocation.latitude)
        val destination = parkedCarLocation!!

        val client = MapboxDirections.builder()
            .origin(origin)
            .destination(destination)
            .accessToken(MAPBOX_KEY)
            .overview(DirectionsCriteria.OVERVIEW_SIMPLIFIED)
            .profile(DirectionsCriteria.PROFILE_WALKING)
            .build()

        client.enqueueCall(object : Callback<DirectionsResponse>{
            override fun onFailure(call: Call<DirectionsResponse>, t: Throwable) {
                Log.e("ERROR NO DIRECTIONS", " $t")
            }

            override fun onResponse(
                call: Call<DirectionsResponse>,
                response: Response<DirectionsResponse>
            ) {
                if(response.body() == null || response.body()!!.routes().size < 1){
                    Log.e("Error","No routes found")
                }else{

                    handleDirectionsResponse(response.body() as DirectionsResponse)
                }
            }

        })

    }

    private fun handleDirectionsResponse(response : DirectionsResponse){
        val currentRoute = response.routes()[0]
        map?.getStyle {style ->
            val geometry = currentRoute.geometry() ?: return@getStyle
            val source = style.getSourceAs<GeoJsonSource>(ROUTE_SOURCE)
            source?.setGeoJson(LineString.fromPolyline(geometry,Constants.PRECISION_6))
            style.getLayerAs<LineLayer>(ROUTE_LAYER)?.setProperties(
                PropertyFactory.visibility(Property.VISIBLE)
            )
        }
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

                getLayerAs<LineLayer>(ROUTE_LAYER)?.setProperties(
                PropertyFactory.visibility(Property.NONE)
                )
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