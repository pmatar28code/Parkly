package com.example.parkly

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.parkly.databinding.ActivityMainBinding
import com.mapbox.mapboxsdk.Mapbox
import com.mapbox.mapboxsdk.maps.MapView
import com.mapbox.mapboxsdk.maps.Style

class MainActivity : AppCompatActivity() {
    companion object{
        const val MAPBOX_KEY =
        "pk.eyJ1IjoicG1hdGFyMjhjb2RlIiwiYSI6ImNrbnA0cGlnczAyN3EydnMyaT" +
        "BwamFsanIifQ.UY2ZY2_ZI5JZDTuBUZIo_g"
    }
    private var mapView:MapView?=null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Mapbox.getInstance(this, MAPBOX_KEY)
        val binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

       mapView = binding.mapView.apply {
           onCreate(savedInstanceState)
           getMapAsync{ mapboxMap ->
               mapboxMap.setStyle(Style.MAPBOX_STREETS) { style ->
                   //do it later
               }
           }
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