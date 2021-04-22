package com.raywenderlich.placebook.ui


import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.location.Location
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.WindowManager
import android.widget.ProgressBar
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.core.app.ActivityCompat
import androidx.core.app.ActivityCompat.startActivityForResult
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.gms.common.GooglePlayServicesNotAvailableException
import com.google.android.gms.common.GooglePlayServicesRepairableException
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.location.*

import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.model.RectangularBounds
import com.google.android.libraries.places.api.net.FetchPhotoRequest
import com.google.android.libraries.places.api.net.FetchPlaceRequest
import com.google.android.libraries.places.api.net.PlacesClient
import com.google.android.libraries.places.widget.Autocomplete
import com.google.android.libraries.places.widget.model.AutocompleteActivityMode
import com.raywenderlich.placebook.R
import com.raywenderlich.placebook.adapter.BookmarkInfoWindowAdapter
import com.raywenderlich.placebook.adapter.BookmarkListAdapter
import com.raywenderlich.placebook.viewmodel.MapsViewModel
import kotlinx.android.synthetic.main.activity_bookmark_details.toolbar
import kotlinx.android.synthetic.main.activity_maps.*
import kotlinx.android.synthetic.main.drawer_view_maps.*
import kotlinx.android.synthetic.main.main_view_maps.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch


class MapsActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var map: GoogleMap
    private lateinit var fusedLocationClient:
            FusedLocationProviderClient     //Fused Location Provider Client
    //private var locationRequest: LocationRequest? = null  // object for location accuracy
    private lateinit var placesClient: PlacesClient
    private val mapsViewModel by viewModels<MapsViewModel>()  // class var to hold MapsViewModel
    private lateinit var bookmarkListAdapter: BookmarkListAdapter
    private var markers = HashMap<Long, Marker>()  // initialize hash map to bookmark id


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_maps)
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager
                .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
        setupLocationClient()
        setupToolbar()
        setupPlacesClient() // PlacesClient initialized after activity is created
        setupNavigationDrawer() // Setup Navigation drawer
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */


    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap
        setupMapListeners()
        createBookmarkObserver()
        getCurrentLocation()

        // Assign custom InfoWindowAdapter to map
        //map.setInfoWindowAdapter(BookmarkInfoWindowAdapter(this))
        // New logic- to call displayPoi method when place on map is tapped
        // and display name and phone number of POI on screen
        //map.setOnPoiClickListener {
            //displayPoi(it)
    }


    private fun setupMapListeners() {
        map.setInfoWindowAdapter(BookmarkInfoWindowAdapter(this))
        map.setOnPoiClickListener {
            displayPoi(it)
        }
        map.setOnInfoWindowClickListener { handleInfoWindowClick(it) }
        fab.setOnClickListener {
            searchAtCurrentLocation()
        }
        map.setOnMapLongClickListener { latLng ->
            newBookmark(latLng)
        }
    }

    // Create Places client- uses api key variable for Places API call
    private fun setupPlacesClient() {
        Places.initialize(getApplicationContext(),
                getString(R.string.google_maps_key));
        placesClient = Places.createClient(this);
    }

    private fun setupLocationClient() {
        fusedLocationClient =
                LocationServices.getFusedLocationProviderClient(this)
    }

    private fun addPlaceMarker(
            bookmark: MapsViewModel.BookmarkView): Marker? {
        val marker = map.addMarker(MarkerOptions()
                .position(bookmark.location)
                .title(bookmark.name)
                .snippet(bookmark.phone)
                .icon(bookmark.categoryResourceId?.let {
                    BitmapDescriptorFactory.fromResource(it)
                })
                .alpha(0.8f))
        marker.tag = bookmark

        bookmark.id?.let { markers.put(it, marker) } // Add new entry for marker added to map

        return marker
    }

    private fun createBookmarkObserver() {
        // 1
        mapsViewModel.getBookmarkMarkerViews()?.observe(
                this, Observer<List<MapsViewModel.BookmarkView>> {
            // 2
            map.clear()
            markers.clear()
            // 3
            it?.let {
                displayAllBookmarks(it)
                bookmarkListAdapter.setBookmarkData(it) // Update adapter when bookmarks change
            }
        })
    }

    // To display bookmarks
    private fun displayAllBookmarks(
            bookmarks: List<MapsViewModel.BookmarkView>) {
        for (bookmark in bookmarks) {
            addPlaceMarker(bookmark)
        }
    }

    // Method to get users current location
    private fun getCurrentLocation() {
        // 1
        if (ActivityCompat.checkSelfPermission(this,
                        Manifest.permission.ACCESS_FINE_LOCATION) !=
                PackageManager.PERMISSION_GRANTED) {
            // 2
            requestLocationPermissions()
        } else {

            map.isMyLocationEnabled = true  // Display Blue dot, rotate option and target icon to recenter map
            // 3
            fusedLocationClient.lastLocation.addOnCompleteListener {
                val location = it.result
                if (location != null) {
                    // 4
                    val latLng = LatLng(location.latitude,
                            location.longitude)

                    //map.clear() // Remove previous marker for new location

                    // 5
                    //map.addMarker(MarkerOptions().position(latLng)
                    //        .title("You are here!"))
                    // 6
                    val update = CameraUpdateFactory.newLatLngZoom(latLng,
                            16.0f)
                    // 7
                    map.moveCamera(update)
                } else {
                    // 8
                    Log.e(TAG, "No location found")
                }
            }
        }
    }

    override fun onRequestPermissionsResult(
            requestCode: Int,
            permissions: Array<String>,
            grantResults: IntArray) {
        if (requestCode == REQUEST_LOCATION) {
            if (grantResults.size == 1 && grantResults[0] ==
                    PackageManager.PERMISSION_GRANTED) {
                getCurrentLocation()
            } else {
                Log.e(TAG, "Location permission denied")
            }
        }
    }

    // Permission for location access method
    private fun requestLocationPermissions() {
        ActivityCompat.requestPermissions(this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                REQUEST_LOCATION)
    }




    // Methods for Fetching details about a place
    private fun displayPoi(pointOfInterest: PointOfInterest) {
        showProgress()
        // Identifies place of interest
        displayPoiGetPlaceStep(pointOfInterest)
    }

    private fun displayPoiGetPlaceStep(pointOfInterest: PointOfInterest) {
        val placeId = pointOfInterest.placeId
        // Create field mask- to contain attributes of place identified
        val placeFields = listOf(Place.Field.ID,
                Place.Field.NAME,
                Place.Field.PHONE_NUMBER,
                Place.Field.PHOTO_METADATAS,
                Place.Field.ADDRESS,
                Place.Field.LAT_LNG,
                Place.Field.TYPES)
        // Use placeId and placeFields to create fetch request
        val request = FetchPlaceRequest
            .builder(placeId, placeFields)
            .build()
        // Fetch place details with placesClient
        placesClient.fetchPlace(request)
            .addOnSuccessListener { response ->
                // 5
                val place = response.place
                displayPoiGetPhotoStep(place)
            }.addOnFailureListener { exception ->
                // Use failure listener to catch exceptions - error handling
                if (exception is ApiException) {
                    val statusCode = exception.statusCode
                    Log.e(
                        TAG,
                        "Place not found: " +
                                exception.message + ", " +
                                "statusCode: " + statusCode
                    )
                    hideProgress()
                }
            }
    }

    // Method to retrieve photo of POI
    private fun displayPoiGetPhotoStep(place: Place) {
        // 1
        val photoMetadata = place
                .getPhotoMetadatas()?.get(0)
        // 2
        if (photoMetadata == null) {
            displayPoiDisplayStep(place, null)  // Pass place object w/ null photo
            return
        }
        // 3
        val photoRequest = FetchPhotoRequest
                .builder(photoMetadata)
                .setMaxWidth(resources.getDimensionPixelSize(
                        R.dimen.default_image_width))
                .setMaxHeight(resources.getDimensionPixelSize(
                        R.dimen.default_image_height))
                .build()
        // 4
        placesClient.fetchPhoto(photoRequest)
                .addOnSuccessListener { fetchPhotoResponse ->
                    val bitmap = fetchPhotoResponse.bitmap
                    displayPoiDisplayStep(place, bitmap)  // Pass place object with bitmap image bmp
                }.addOnFailureListener { exception ->
                    if (exception is ApiException) {
                        val statusCode = exception.statusCode
                        Log.e(TAG,
                                "Place not found: " +
                                        exception.message + ", " +
                                        "statusCode: " + statusCode)
                    }
                    hideProgress()
                }
    }

    // Display marker with place details and photo of POI
    private fun displayPoiDisplayStep(place: Place, photo: Bitmap?)
    {
        hideProgress()
        val marker = map.addMarker(MarkerOptions()
            .position(place.latLng as LatLng)
            .title(place.name)
            .snippet(place.phoneNumber)
        )
        marker?.tag = PlaceInfo(place, photo)

        marker?.showInfoWindow()
    }

    private fun handleInfoWindowClick(marker: Marker) {
        when (marker.tag) {
            is MapsActivity.PlaceInfo -> {
                val placeInfo = (marker.tag as PlaceInfo)
                if (placeInfo.place != null && placeInfo.image != null) {
                    GlobalScope.launch {
                        mapsViewModel.addBookmarkFromPlace(placeInfo.place,
                            placeInfo.image)
                    }
                }
                marker.remove();
            }
            is MapsViewModel.BookmarkView -> {
                val bookmarkMarkerView = (marker.tag as
                        MapsViewModel.BookmarkView)
                marker.hideInfoWindow()
                bookmarkMarkerView.id?.let {
                    startBookmarkDetails(it)
                }
            }
        }
    }

    private fun startBookmarkDetails(bookmarkId: Long) {
        val intent = Intent(this, BookmarkDetailsActivity::class.java)
        intent.putExtra(EXTRA_BOOKMARK_ID, bookmarkId)
        startActivity(intent)
    }

    private fun setupToolbar() {
        setSupportActionBar(toolbar)
        val toggle = ActionBarDrawerToggle(
                this, drawerLayout, toolbar,
                R.string.open_drawer, R.string.close_drawer)
        toggle.syncState()
    }


    // Sets up adapter for the bookmark recycler view
    private fun setupNavigationDrawer() {
        val layoutManager = LinearLayoutManager(this)
        bookmarkRecyclerView.layoutManager = layoutManager
        bookmarkListAdapter = BookmarkListAdapter(null, this)
        bookmarkRecyclerView.adapter = bookmarkListAdapter
    }

    // Method to Zoom map to specific location helper
    private fun updateMapToLocation(location: Location) {
        val latLng = LatLng(location.latitude, location.longitude)
        map.animateCamera(
                CameraUpdateFactory.newLatLngZoom(latLng, 16.0f))
    }


    // Moves map to bookmark location, uses updateMapToLocation to zoom on map to bookmark location.
    fun moveToBookmark(bookmark: MapsViewModel.BookmarkView) {
        // 1
        drawerLayout.closeDrawer(drawerView)
        // 2
        val marker = markers[bookmark.id]
        // 3
        marker?.showInfoWindow()
        // 4
        val location = Location("")
        location.latitude = bookmark.location.latitude
        location.longitude = bookmark.location.longitude
        updateMapToLocation(location)
    }


    private fun searchAtCurrentLocation() {

        val placeFields = listOf(
                Place.Field.ID,
                Place.Field.NAME,
                Place.Field.PHONE_NUMBER,
                Place.Field.PHOTO_METADATAS,
                Place.Field.LAT_LNG,
                Place.Field.ADDRESS,
                Place.Field.TYPES)

        val bounds = RectangularBounds.newInstance(map.projection.visibleRegion.latLngBounds)
        try {
            val intent = Autocomplete.IntentBuilder(
                    AutocompleteActivityMode.OVERLAY, placeFields)
                    .setLocationBias(bounds)
                    .build(this)
            startActivityForResult(intent, AUTOCOMPLETE_REQUEST_CODE)
        } catch (e: GooglePlayServicesRepairableException) {
            Log.e("MAPS", "searchAtCurrentLocation",e )
            Toast.makeText(this, "Problems Searching", Toast.LENGTH_LONG).show()
        } catch (e: GooglePlayServicesNotAvailableException) {
            Log.e("MAPS", "searchAtCurrentLocation",e )
            Toast.makeText(this, "Problems Searching. Google Play Not available", Toast.LENGTH_LONG).show()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int,
                                  data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        // 1
        when (requestCode) {
            AUTOCOMPLETE_REQUEST_CODE ->
                // 2
                if (resultCode == Activity.RESULT_OK && data != null) {
                    // 3
                    val place = Autocomplete.getPlaceFromIntent(data)
                    // 4
                    val location = Location("")
                    location.latitude = place.latLng?.latitude ?: 0.0
                    location.longitude = place.latLng?.longitude ?: 0.0
                    updateMapToLocation(location)
                    showProgress()
                    // 5
                    displayPoiGetPhotoStep(place)
                }
        }
    }


    private fun newBookmark(latLng: LatLng) {
        GlobalScope.launch {
            val bookmarkId = mapsViewModel.addBookmark(latLng)
            bookmarkId?.let {
                startBookmarkDetails(it)
            }
        }
    }

    private fun disableUserInteraction() {
        window.setFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
    }
    private fun enableUserInteraction() {
        window.clearFlags(
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
    }

    private fun showProgress() {
        progressBar.visibility = ProgressBar.VISIBLE
        disableUserInteraction()
    }
    private fun hideProgress() {
        progressBar.visibility = ProgressBar.GONE
        enableUserInteraction()
    }


    companion object {
        const val EXTRA_BOOKMARK_ID = "com.raywenderlich.placebook.EXTRA_BOOKMARK_ID"
        private const val REQUEST_LOCATION = 1
        private const val TAG = "MapsActivity"
        private const val AUTOCOMPLETE_REQUEST_CODE = 2
    }


    class PlaceInfo(val place: Place? = null,
                val image: Bitmap? = null)
}