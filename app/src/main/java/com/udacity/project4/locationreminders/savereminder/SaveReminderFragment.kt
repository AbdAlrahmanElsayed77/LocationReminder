package com.udacity.project4.locationreminders.savereminder

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.PendingIntent
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import androidx.databinding.DataBindingUtil
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import com.google.android.material.snackbar.Snackbar
import com.udacity.project4.BuildConfig
import com.udacity.project4.R
import com.udacity.project4.base.BaseFragment
import com.udacity.project4.base.NavigationCommand
import com.udacity.project4.databinding.FragmentSaveReminderBinding
import com.udacity.project4.locationreminders.geofence.GEOFENCE_EVENT_ACTION
import com.udacity.project4.locationreminders.geofence.GeofenceBroadcastReceiver
import com.udacity.project4.locationreminders.reminderslist.ReminderDataItem
import com.udacity.project4.locationreminders.savereminder.selectreminderlocation.*
import com.udacity.project4.utils.setDisplayHomeAsUpEnabled
import org.koin.androidx.viewmodel.ext.android.sharedViewModel

private const val TAG = "SaveReminderFragment"

class SaveReminderFragment : BaseFragment() {
    //Get the view model this time as a single to be shared with the another fragment
    override val _viewModel: SaveReminderViewModel by sharedViewModel()
    private lateinit var binding: FragmentSaveReminderBinding
    private lateinit var dataReminder: ReminderDataItem

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {

        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_save_reminder, container, false)

        setDisplayHomeAsUpEnabled(true)
        binding.viewModel = _viewModel
        return binding.root
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.lifecycleOwner = this
        binding.selectLocation.setOnClickListener {
            _viewModel.navigationCommand.value =
                NavigationCommand.To(SaveReminderFragmentDirections.actionSaveReminderFragmentToSelectLocationFragment())
        }

        binding.saveReminder.setOnClickListener {
            val title = _viewModel.reminderTitle.value
            val description = _viewModel.reminderDescription.value
            val location = _viewModel.reminderSelectedLocationStr.value
            val latitude = _viewModel.latitude.value
            val longitude = _viewModel.longitude.value

            dataReminder = ReminderDataItem(title, description, location, latitude, longitude)
            checkPermissions()
        }
    }

    @SuppressLint("MissingPermission")
    private fun addGeofence(reminderData: ReminderDataItem) {
        val geofence = Geofence.Builder().setRequestId(reminderData.id).setCircularRegion(
                reminderData.latitude!!,
                reminderData.longitude!!,
                DEFAULT_METRES_OF_RADIUS
            )
            .setExpirationDuration(Geofence.NEVER_EXPIRE)
            .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER)
            .build()

        val geofenceRequest = GeofencingRequest.Builder()
            .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER).addGeofence(geofence).build()

        val intent = Intent(requireContext(), GeofenceBroadcastReceiver::class.java)
        intent.action = GEOFENCE_EVENT_ACTION

        val pendingIntent = PendingIntent.getBroadcast(requireContext(), 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)
        val client = LocationServices.getGeofencingClient(requireContext())

        client.addGeofences(geofenceRequest, pendingIntent)?.run {
            addOnSuccessListener {
                Log.d(TAG, "Added geofence for reminder with id ${reminderData.id} successfully.")
            }
            addOnFailureListener {
                _viewModel.showErrorMessage.postValue(getString(R.string.error_adding_geofence))
                it.message?.let { message ->
                    Log.w(TAG, message)
                }
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun checkPermissions() {
        if (allPermissionApproved()) { checkLocationSettings{isLocationOn ->
                if (isLocationOn)
                    if (_viewModel.validateAndSaveReminder(dataReminder)) {
                        addGeofence(dataReminder)
                    }
                    else checkLocationSettings()
            }
        } else {
            requestAllLocationPermissions()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        //make sure to clear the view model after destroy, as it's a single view model.
        _viewModel.onClear()
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun allPermissionApproved(): Boolean {
        val foregroundPermission =
            PackageManager.PERMISSION_GRANTED ==
                    ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
        val backgroundPermission = if (runningQOrLater) {
            PackageManager.PERMISSION_GRANTED ==
                    ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_BACKGROUND_LOCATION)
        } else {
            true
        }
        return foregroundPermission && backgroundPermission
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == TURN_LOCATION_ON) {
            if (resultCode == Activity.RESULT_OK) {
                checkLocationSettings()
            }else {
                checkLocationSettings(false)
            }
        }
    }


    @RequiresApi(Build.VERSION_CODES.Q)
    private fun requestAllLocationPermissions() {
        if (allPermissionApproved())
            return
        var permissionsArray = arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
        val resultCode = when {
            runningQOrLater -> {
                permissionsArray += Manifest.permission.ACCESS_BACKGROUND_LOCATION
                REQUEST_ALL_PERMISSIONS_CODE
            }
            else -> REQUEST_FOREGROUND_PERMISSIONS_CODE
        }
        requestPermissions(permissionsArray, resultCode)
    }


    @RequiresApi(Build.VERSION_CODES.Q)
    private fun checkLocationSettings(resolve: Boolean = true, isLocationOn : (Boolean) -> Unit = {}) {
        val locationRequest = LocationRequest.create().apply {
            priority = LocationRequest.PRIORITY_LOW_POWER
        }
        val builder = LocationSettingsRequest.Builder().addLocationRequest(locationRequest)

        val clientSettings = LocationServices.getSettingsClient(requireActivity())
        val settingsLocationResponse = clientSettings.checkLocationSettings(builder.build())

        settingsLocationResponse.addOnFailureListener { exception ->
            if (exception is ResolvableApiException && resolve) {
                try {
                    startIntentSenderForResult(
                        exception.resolution.intentSender,
                        TURN_LOCATION_ON,
                        null,
                        0,
                        0,
                        0,
                        null
                    )
                } catch (sendEx: IntentSender.SendIntentException) { }
            } else {
                isLocationOn.invoke(false)
                Snackbar.make(binding.root, R.string.location_required_error, Snackbar.LENGTH_LONG)
                    .setAction(android.R.string.ok) {
                    checkLocationSettings()
                }.show()
            }
        }
        settingsLocationResponse.addOnCompleteListener {
            if (it.isSuccessful) {
                isLocationOn.invoke(true)
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        if (grantResults.isEmpty() ||
            grantResults[PERMISSION_INDEX_OF_LOCATION] == PackageManager.PERMISSION_DENIED ||
            (requestCode == REQUEST_ALL_PERMISSIONS_CODE &&
                    grantResults[PERMISSION_INDEX_OF_BACKGROUND_LOCATION] ==
                    PackageManager.PERMISSION_DENIED)
        ) {
            Snackbar.make(binding.root, R.string.permission_denied_explanation, Snackbar.LENGTH_INDEFINITE)
                .setAction(R.string.settings) {
                    startActivity(Intent().apply {
                        action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                        data = Uri.fromParts("package", BuildConfig.APPLICATION_ID, null)
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    })
                }.show()
        } else {
            checkLocationSettings()
        }
    }
}
