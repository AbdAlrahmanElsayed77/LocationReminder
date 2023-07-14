package com.udacity.project4.locationreminders.savereminder.selectreminderlocation

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity.RESULT_OK
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageManager
import android.location.Location
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.*
import androidx.core.app.ActivityCompat
import androidx.core.content.res.ResourcesCompat
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.LocationSettingsRequest
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.GoogleMap.OnMyLocationButtonClickListener
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.google.android.material.snackbar.Snackbar
import com.udacity.project4.BuildConfig
import com.udacity.project4.R
import com.udacity.project4.base.BaseFragment
import com.udacity.project4.base.NavigationCommand
import com.udacity.project4.databinding.FragmentSelectLocationBinding
import com.udacity.project4.locationreminders.savereminder.SaveReminderViewModel
import com.udacity.project4.utils.setDisplayHomeAsUpEnabled
import org.koin.androidx.viewmodel.ext.android.sharedViewModel

const val PERMISSION_INDEX_OF_LOCATION = 0
const val PERMISSION_INDEX_OF_BACKGROUND_LOCATION = 1
const val DEFAULT_METRES_OF_RADIUS = 300f
const val REQUEST_ALL_PERMISSIONS_CODE = 33
const val REQUEST_FOREGROUND_PERMISSIONS_CODE = 34
const val TURN_LOCATION_ON = 29
val runningQOrLater = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q

class SelectLocationFragment : BaseFragment(), OnMapReadyCallback, OnMyLocationButtonClickListener {

    //Use Koin to get the view model of the SaveReminder
    private var map: GoogleMap? = null
    private var locationMarkerSelected: Marker? = null
    private var locationCircleSelected: Circle? = null
    override val _viewModel: SaveReminderViewModel by sharedViewModel()
    private lateinit var binding: FragmentSelectLocationBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_select_location, container, false)

        binding.lifecycleOwner = this
        binding.viewModel = _viewModel
        binding.lifecycleOwner = this

        setHasOptionsMenu(true)
        setDisplayHomeAsUpEnabled(true)
        setUpMap()

        binding.SaveButton.setOnClickListener {
            onLocationSelected()
        }

        _viewModel.locationSelected.observe(viewLifecycleOwner, Observer {
            if (it != null) {
                locationMarkerSelected?.position = it.latLng
                locationCircleSelected?.center = it.latLng
                setCameraTo(it.latLng)
                addMarkerCurrentLocation(it.latLng)
            }
        })
        return binding.root
    }

    private fun setUpMap() {
        val mapFragment = childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }

    private fun onLocationSelected() {
        _viewModel.navigationCommand.postValue(NavigationCommand.Back)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.map_options, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem) = when (item.itemId) {
        R.id.normal_map -> {
            map?.mapType = GoogleMap.MAP_TYPE_NORMAL
            true
        }
        R.id.hybrid_map -> {
            map?.mapType = GoogleMap.MAP_TYPE_HYBRID
            true
        }
        R.id.satellite_map -> {
            map?.mapType = GoogleMap.MAP_TYPE_SATELLITE
            true
        }
        R.id.terrain_map -> {
            map?.mapType = GoogleMap.MAP_TYPE_TERRAIN
            true
        }
        else -> super.onOptionsItemSelected(item)
    }

    @SuppressLint("MissingPermission")
    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap
        map?.setOnMyLocationButtonClickListener(this)
        map?.setMapStyle(MapStyleOptions.loadRawResourceStyle(requireContext(), R.raw.map_style))
        _viewModel.selectedPOI.value.let {
            if (it == null) {
                if (foregroundLocationPermissionApproved()) {
                    checkDeviceLocationSettings()
                    map?.isMyLocationEnabled = true
                } else {
                    requestForegroundLocationPermissions()
                }
            }
        }
        drawCircleOnMap()
        map?.setOnMapClickListener {
            _viewModel.selectedLocation(it)
        }
        map?.setOnPoiClickListener {
            _viewModel.selectedLocation(it)
        }
    }

    private fun addMarkerCurrentLocation(latLng: LatLng) {
        locationMarkerSelected?.remove()
        val markerOptions = MarkerOptions()
            .position(latLng)
            .title(getString(R.string.dropped_pin))
            .draggable(true)
        locationMarkerSelected = map?.addMarker(markerOptions)
    }

    override fun onMyLocationButtonClick(): Boolean {
        checkDeviceLocationSettings()
        return false
    }

    @SuppressLint("MissingPermission")
    private fun moveToMyLocation() {
        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())
        fusedLocationClient.lastLocation
            .addOnSuccessListener { location: Location? ->
                // Got last known location. In some rare situations this can be null.
                if (location != null) {
                    val locationLatLng = LatLng(location.latitude, location.longitude)
                    _viewModel.selectedLocation(locationLatLng)
                } else {
                    _viewModel.selectedLocation(PointOfInterest(map?.cameraPosition?.target, null, null))
                }

            }
    }
    override fun onResume() {
        super.onResume()
        checkDeviceLocationSettings()
    }

    private fun drawCircleOnMap() {
        val circleOptions = CircleOptions()
            .center(map?.cameraPosition?.target)
            .fillColor(ResourcesCompat.getColor(resources, R.color.geo_fence_fill, null))
            .strokeColor(ResourcesCompat.getColor(resources, R.color.geo_fence_stork, null))
            .strokeWidth(4f)
            .radius(DEFAULT_METRES_OF_RADIUS.toDouble())
        locationCircleSelected = map?.addCircle(circleOptions)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == TURN_LOCATION_ON) {
            if (resultCode == RESULT_OK) {
                checkDeviceLocationSettings()
            } else {
                checkDeviceLocationSettings(false)
            }
        }
    }

    private fun setCameraTo(latLng: LatLng) {
        val cameraPosition =
            CameraPosition.fromLatLngZoom(latLng, 15f)
        val cameraUpdate = CameraUpdateFactory.newCameraPosition(cameraPosition)
        map?.animateCamera(cameraUpdate)
    }

    private fun foregroundLocationPermissionApproved(): Boolean =
        PackageManager.PERMISSION_GRANTED == ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
    private fun requestForegroundLocationPermissions() {
        if (foregroundLocationPermissionApproved())
            return

        val arrayPermissions = arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
        val codeResult = REQUEST_FOREGROUND_PERMISSIONS_CODE
        requestPermissions(arrayPermissions, codeResult)

    }

    private fun checkDeviceLocationSettings(resolve: Boolean = true) {
        val locationRequest = LocationRequest.create().apply {
            priority = LocationRequest.PRIORITY_LOW_POWER
        }
        val builder = LocationSettingsRequest.Builder().addLocationRequest(locationRequest)
        val clientSettings = LocationServices.getSettingsClient(requireActivity())
        val settingsResponseLocation = clientSettings.checkLocationSettings(builder.build())
        settingsResponseLocation.addOnFailureListener { exception ->
            if (exception is ResolvableApiException && resolve) {
                try { startIntentSenderForResult(exception.resolution.intentSender, TURN_LOCATION_ON,
                        null,
                        0,
                        0,
                        0,
                        null
                    )
                } catch (sendEx: IntentSender.SendIntentException) { }
            } else {
                Snackbar.make(binding.root, R.string.location_required_error, Snackbar.LENGTH_LONG).setAction(android.R.string.ok) {
                    checkDeviceLocationSettings()
                    moveToMyLocation()
                }.show()
            }
        }
        settingsResponseLocation.addOnCompleteListener {
            if (it.isSuccessful) {
                moveToMyLocation()
            }
        }
    }

    @SuppressLint("MissingPermission")
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        if (grantResults.isEmpty() || grantResults[PERMISSION_INDEX_OF_LOCATION] == PackageManager.PERMISSION_DENIED) {
            // Permission denied.
            Snackbar.make(binding.root, R.string.permission_denied_explanation, Snackbar.LENGTH_INDEFINITE)
                .setAction(R.string.settings) {
                    // Displays App settings screen.
                    startActivity(Intent().apply {
                        action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                        data = Uri.fromParts("package", BuildConfig.APPLICATION_ID, null)
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    })
                }.show()

        } else {
            checkDeviceLocationSettings()
            map?.isMyLocationEnabled = true
            moveToMyLocation()
        }
    }
}
