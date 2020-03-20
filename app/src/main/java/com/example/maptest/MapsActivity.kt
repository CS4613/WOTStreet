package com.example.maptest

import android.app.Activity
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.location.Location
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.RelativeLayout
import androidx.core.app.ActivityCompat
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*

import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import kotlinx.android.synthetic.main.activity_maps.*
import java.io.IOException

class MapsActivity : AppCompatActivity(), OnMapReadyCallback, GoogleMap.OnMarkerClickListener {
    // Class will implement the on marker click listener interface
    private lateinit var map: GoogleMap // The map that we will be using
    private lateinit var fusedLocationClient: FusedLocationProviderClient // Will be used later to get location
    private lateinit var lastLocation: Location // Retrieves last known location of user
    private lateinit var locationCallback: LocationCallback // Callback for location tracking
    private lateinit var locationRequest: LocationRequest // Request updated location state
    private var locationUpdateState = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_maps)
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        //Testing other features: We are wanting to replace last location with the new location and update map
        ///Working as of 3/20 - JH

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(p0: LocationResult) {
                super.onLocationResult(p0)
                lastLocation = p0.lastLocation

                placeMarkerOnMap(LatLng(lastLocation.latitude, lastLocation.longitude))
            }
        }
        createLocationRequest()
    }

    /**
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap
        googleMap.getUiSettings().setZoomControlsEnabled(true) // This enables zoom controls on the map
        googleMap.setOnMarkerClickListener(this) //MapsActivity callback triggered when user clicks a marker
        setUpMap() //initializes map once location permissions have been granted (see below).
    }

    override fun onMarkerClick(p0: Marker?) = false

    private fun placeMarkerOnMap(location: LatLng) {
        // Creates an object that sets the user's current location as the marker position
        val markerOptions = MarkerOptions().position(location)
        // Adds the marker to the map
        val buttonClicked = findViewById<Button>(R.id.button1)
        buttonClicked.setOnClickListener {
            map.addMarker(markerOptions)
        }
    }

    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1 // Used to request location permission
        private const val REQUEST_CHECK_SETTINGS = 2 // Request code that will be passed to onActivityResult
    }

    private fun setUpMap() {
        if (ActivityCompat.checkSelfPermission(this,
                android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_PERMISSION_REQUEST_CODE
            )
            // Method checks location permissions granted. If not, location permissions are requested.
            return
        }

        map.isMyLocationEnabled = true // If location permission enabled, permission will not be requested again.

        fusedLocationClient.lastLocation.addOnSuccessListener(this) { location ->
            // Got last known location. Null checking added for rare cases.
            if (location != null) {
                lastLocation = location // If null, get the location.
                val currentLatLng = LatLng(location.latitude, location.longitude) // Retrieves current location
                placeMarkerOnMap(currentLatLng) // When map is set up, marker will be shown
                map.animateCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 17f)) // Animates camera zoom level
            }
        }
    }

    private fun startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(this,
                android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_PERMISSION_REQUEST_CODE)
            // Same as method above in setUpMap()
            return
        }
        // This will constantly call and update the users current location
        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, null /* Looper */)
    }

    private fun createLocationRequest() {
        // Create an instance of LocationRequest and handle changes to state of user's location
        locationRequest = LocationRequest()
        // The rate in which the app will receive location updates
        locationRequest.setInterval(3000)
        // Sets location accuracy to high accuracy
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)

        val builder = LocationSettingsRequest.Builder()
            .addLocationRequest(locationRequest)

        // Task to check location settings
        val client = LocationServices.getSettingsClient(this)
        val task = client.checkLocationSettings(builder.build())

        // If successful you can initiate a location request
        task.addOnSuccessListener {
            locationUpdateState = true
            startLocationUpdates()
        }
        task.addOnFailureListener { e ->
            // If not successful, the location settings have issues which must be resolved.
            // A dialog box will be displayed to the user to fix these settings.
            if (e is ResolvableApiException) {
                // Location settings are not satisfied, but this can be fixed
                // by showing the user a dialog.
                try {
                    // Show the dialog by calling startResolutionForResult(),
                    // and check the result in onActivityResult().
                    e.startResolutionForResult(this@MapsActivity,
                        REQUEST_CHECK_SETTINGS)
                } catch (sendEx: IntentSender.SendIntentException) {
                    // Ignore the error.
                }
            }
        }
    }

    // Used if app is on background
    override fun onPause() {
        super.onPause()
        // fusedLocationClient.removeLocationUpdates(locationCallback)
        locationRequest.setInterval(10000)
        locationRequest.setPriority(LocationRequest.PRIORITY_NO_POWER)
    }

    // If app is in the foreground
    public override fun onResume() {
        super.onResume()
        if (!locationUpdateState) {
            startLocationUpdates()
        }
    }

    // If permissions are good, location updates will begin
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CHECK_SETTINGS) {
            if (resultCode == Activity.RESULT_OK) {
                locationUpdateState = true
                startLocationUpdates()
            }
        }
    }
}
