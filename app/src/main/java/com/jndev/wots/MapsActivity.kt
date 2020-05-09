package com.jndev.wots

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Typeface
import android.location.Location
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.github.kittinunf.fuel.Fuel
import com.github.kittinunf.fuel.core.FuelError
import com.github.kittinunf.fuel.core.extensions.jsonBody
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import com.google.android.gms.maps.*
import com.google.android.gms.maps.GoogleMap.InfoWindowAdapter
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import org.json.JSONArray

@Suppress("CAST_NEVER_SUCCEEDS")
class MapsActivity : AppCompatActivity(), OnMapReadyCallback, GoogleMap.OnMarkerClickListener, GoogleMap.OnInfoWindowClickListener {
    // Class will implement the on marker click listener interface
    private lateinit var map: GoogleMap // The map that we will be using
    private lateinit var marker: Marker // Markers
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
        map.setInfoWindowAdapter(object : InfoWindowAdapter {
            override fun getInfoWindow(arg0: Marker): View? {
                return null
            }

            override fun getInfoContents(marker: Marker): View {
                val info = LinearLayout(applicationContext)
                info.orientation = LinearLayout.VERTICAL
                val title = TextView(applicationContext)
                title.setTextColor(Color.BLACK)
                title.setGravity(Gravity.CENTER)
                title.setTypeface(null, Typeface.BOLD)
                title.setText(marker.title)
                val snippet = TextView(applicationContext)
                snippet.setTextColor(Color.GRAY)
                snippet.setText(marker.snippet)
                info.addView(title)
                info.addView(snippet)
                return info
            }
        })
        googleMap.uiSettings.isZoomControlsEnabled = false // This disables zoom controls on the map
        googleMap.uiSettings.isScrollGesturesEnabled = false // Disables scrolling on the map
        googleMap.setOnMarkerClickListener(this) //MapsActivity callback triggered when user clicks a marker
        setUpMap() //initializes map once location permissions have been granted (see below).
        Fuel.get("http://167.71.177.246:80/setPins")
            .responseString { request, response, result ->
                try {
                    val res = result.get()
                    val coordinates = JSONArray(res)
                    println(coordinates)
                    for (i in 0 until coordinates.length()) {
                        val pin = coordinates.getJSONObject(i)
                        println(coordinates)
                        println(pin["x_coor"])
                        println(pin["y_coor"])
                        println(pin["title"])
                        println(pin["message"])
                        println(pin["name"])
                        val x = pin["x_coor"] as Double
                        val y = pin["y_coor"] as Double
                        val setUpPins = LatLng(x, y)
                        val pinOptions = MarkerOptions()
                            .position(setUpPins)
                            .title(pin["title"].toString())
                            .snippet(pin["message"].toString() + "\n" + "By: " + pin["name"].toString())
                        map.addMarker(pinOptions)
                    }
                } catch (e: FuelError) {
                    println(e)
                }
            }
    }

    override fun onMarkerClick(p0: Marker?) = false

    private fun placeMarkerOnMap(location: LatLng) {

        // Creates an object that sets the user's current location as the marker position
        val markerOptions = MarkerOptions().position(location)
        // Adds the marker to the map
        val buttonClicked = findViewById<Button>(R.id.button1)
        buttonClicked.setOnClickListener {
            marker = map.addMarker(markerOptions) // Marker added to map, dialog box displayed

            val dialog = AlertDialog.Builder(this)
            val alert = dialog.create()

            val layout = LinearLayout(this)
            layout.orientation = LinearLayout.VERTICAL

            val titleBox = EditText(this)
            titleBox.hint = "Location Name"
            layout.addView(titleBox) // Notice this is an add method

            val descriptionBox = EditText(this)
            descriptionBox.hint = "Leave Message"
            layout.addView(descriptionBox) // Another add method

            val nameBox = EditText(this)
            nameBox.hint = "Enter Your Name"
            layout.addView(nameBox) // Another add method

            val positive = Button(this)
            positive.text = "Confirm"
            layout.addView(positive)
            positive.setOnClickListener {
                var x = lastLocation.latitude;
                var y = lastLocation.longitude;
                var title = titleBox.text;
                var message = descriptionBox.text;
                var name = nameBox.text;
                println("The title entered: $title")
                println("The message entered: $message")
                println("X: $x")
                println("Y: $y")
                println("Name: $name")
                Toast.makeText(this, "Submitted.", Toast.LENGTH_LONG).show()

                Fuel.get("http://167.71.177.246:80/writeMessage")
                    .responseString{ req, res, result ->
                        try {
                            println("Received.")
                            println(result.get())
                        }
                        catch(e:FuelError){
                            println(e)
                        }
                    }
                Fuel.post("http://167.71.177.246:80/writeMessage")
                    .jsonBody("{ \"x\": \"$x\",  \"y\": \"$y\", \"title\" : \"$title\", \"message\" : \"$message\", \"name\" : \"$name\" }")
                    .also { println(it) }
                    .response { result -> println(result) } //is this right?
                alert.dismiss()
            }

            val negative = Button(this)
            negative.text = "Cancel"
            layout.addView(negative)
            negative.setOnClickListener {
                alert.dismiss()
                marker.remove()
            }

            alert.setView(layout) // Again this is a set method, not add
            alert.show()
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
        locationRequest.interval = 3000
        // Sets location accuracy to high accuracy
        locationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY

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

    override fun onInfoWindowClick(p0: Marker?) {
        Toast.makeText(this, "testing marker click", Toast.LENGTH_SHORT).show()
    }
}

