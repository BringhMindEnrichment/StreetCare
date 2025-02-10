package org.brightmindenrichment.street_care.ui.community

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Paint
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.core.view.isVisible
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import org.brightmindenrichment.street_care.R
import org.brightmindenrichment.street_care.databinding.FragmentCommunityBinding
import org.brightmindenrichment.street_care.ui.community.adapter.CommunityActivityAdapter
import org.brightmindenrichment.street_care.ui.community.model.CommunityActivityObject
import org.brightmindenrichment.street_care.ui.community.model.CommunityPageName
import org.brightmindenrichment.street_care.ui.community.viewModel.CommunityViewModel
import org.brightmindenrichment.street_care.ui.visit.VisitDataAdapter
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.tasks.Task
import com.google.firebase.firestore.QuerySnapshot
import com.google.firebase.firestore.DocumentSnapshot
import org.brightmindenrichment.street_care.util.Queries.getUpcomingEventsQueryUpTo50
import org.brightmindenrichment.street_care.util.Queries.getHelpRequestDefaultQueryUpTo50
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.cancel
import java.util.*


private val TAG = "COMMUNITY_FRAGMENT"
private const val REQUEST_CODE_RECOVER_PLAY_SERVICES = 1001
class CommunityFragment : Fragment(), OnMapReadyCallback  {

    private lateinit var binding: FragmentCommunityBinding
    //private lateinit var cityTextView: TextView
    private lateinit var allActivitiesBtn: Button
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var viewModel: CommunityViewModel
    private lateinit var adapter: CommunityActivityAdapter
    private val permissionId = 2
    private val visitDataAdapter = VisitDataAdapter()

    // Map properties - using SupportMapFragment instead of MapView
    private lateinit var mMap: GoogleMap

    // Marker data for map
    private data class MarkerData(
        val position: LatLng,
        val title: String,
        val description: String,
        val markerColor: Float
    )
    private var cachedEvents: List<MarkerData>? = null
    private var cachedHelpRequests: List<MarkerData>? = null

    // Geocode for Maps
    private val coroutineScope = CoroutineScope(Dispatchers.Main + Job())

    val activityModel = CommunityActivityObject.Builder()
        .setLocation("BOS")
        .setTime("05/01/2023")
        .setDescription("Start an Activity")
        .build()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentCommunityBinding.inflate(inflater, container, false)

      // cityTextView = binding.cityTextView
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireContext())

        // Initialize map
        val mapFragment = childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        binding.pastEventComponent.setOnClickListener {
            findNavController().navigate(R.id.communityEventFragment, Bundle().apply {
                //putBoolean("isPastEvents", true)
                putString("pageTitle", it.context.getString(R.string.past_events))
                putSerializable("communityPageName", CommunityPageName.PAST_EVENTS)
            })
        }

        binding.upcomingEventComponent.setOnClickListener {
            findNavController().navigate(R.id.communityEventFragment, Bundle().apply {
                //putBoolean("isPastEvents", false)
                putString("pageTitle", it.context.getString(R.string.future_events))
                putSerializable("communityPageName", CommunityPageName.UPCOMING_EVENTS)
            })
        }

        binding.helpRequestsComponent.setOnClickListener {
            Log.d("debug", "click help requests icon on community page")
            findNavController().navigate(R.id.communityHelpRequestFragment, Bundle().apply {
                //putBoolean("isPastEvents", false)
                putString("pageTitle", context?.getString(R.string.help_request))

            })
        }

        setHelpComponentListener()
        setRequestComponentListener()
        return binding.root
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        mMap.uiSettings.isZoomControlsEnabled = true

        if (checkPermissions()) {
                if (isLocationEnabled()) {
                if (ActivityCompat.checkSelfPermission(
                        requireContext(),
                        Manifest.permission.ACCESS_FINE_LOCATION
                    ) == PackageManager.PERMISSION_GRANTED) {
                    mMap.isMyLocationEnabled = true

                    // Center map on user's location
                    fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                        if (location != null) {
                            val currentLatLng = LatLng(location.latitude, location.longitude)
                            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 11f))
                        }
                    }
                }
            }
            else{
                // Move camera to Boston location - default location
                val defaultLocation = LatLng(42.333774, -71.064937) // Boston
                googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(defaultLocation, 11f))
                }
        }
        loadEvents()
        loadHelpRequests()
    }

    private fun addMarkerToMap(markerData: MarkerData) {
        activity?.runOnUiThread {
            mMap.addMarker(MarkerOptions()
                .position(markerData.position)
                .title(markerData.title)
                .snippet(markerData.description)
                .icon(BitmapDescriptorFactory.defaultMarker(markerData.markerColor)))
        }
    }

    private suspend fun getMarkerDataFromLocation(
        geocoder: Geocoder,
        location: String,
        title: String,
        description: String,
        markerColor: Float
    ): MarkerData? {
        return try {
            val addresses = geocoder.getFromLocationName(location, 1)
            if (addresses?.isNotEmpty() == true) {
                val address = addresses[0]
                val eventLocation = LatLng(
                    address.latitude,
                    address.longitude
                )
                MarkerData(
                    eventLocation,
                    title,
                    description,
                    markerColor
                )
            } else null
        } catch (e: Exception) {
            null
        }
    }

    private fun loadMapData(
        isEvent: Boolean,
        cached: List<MarkerData>?,
        updateCache: (List<MarkerData>) -> Unit,
        query: () -> Task<QuerySnapshot>,
        getMarkerColor: (document: DocumentSnapshot) -> Float
    ) {
        if (!binding.mapLoadingContainer.isVisible) {
            binding.mapLoadingContainer.visibility = View.VISIBLE
        }

        if (cached != null) {
            cached.forEach { addMarkerToMap(it) }
            binding.mapLoadingContainer.visibility = View.GONE
            return
        }

        query().addOnSuccessListener { querySnapshot ->
            coroutineScope.launch(Dispatchers.IO) {
                val markerDataList = mutableListOf<MarkerData>()
                val geocoder = Geocoder(requireContext())

                val deferredResults = querySnapshot.documents.map { document ->
                    async {
                        val locationMap = document.get("location") as? HashMap<*, *>
                        val address = buildString {
                            append(locationMap?.get("street") ?: "")
                            append(", ")
                            append(locationMap?.get("city") ?: "")
                            append(", ")
                            append(locationMap?.get("state") ?: "")
                            append(" ")
                            append(locationMap?.get("zipcode") ?: "")
                        }.trim()

                        //skip creating markers for documents with empty or invalid location data
                        if (address.isEmpty() || address == ", ,  ") {
                            return@async null
                        }
                        val title = document.getString("title") ?: if(isEvent) "Event" else "Help Request"
                        val description = document.getString("description") ?: ""

                        getMarkerDataFromLocation(
                            geocoder,
                            address,
                            title,
                            description,
                            getMarkerColor(document)
                        )
                    }
                }

                processMarkerResults(deferredResults, markerDataList) {
                    updateCache(markerDataList)
                }
            }
        }.addOnFailureListener {
            binding.mapLoadingContainer.visibility = View.GONE
        }
    }

    private fun loadEvents() = loadMapData(
        isEvent = true,
        cached = cachedEvents,
        updateCache = { cachedEvents = it },
        query = { getUpcomingEventsQueryUpTo50().get() },
        getMarkerColor = { BitmapDescriptorFactory.HUE_YELLOW }
    )

    private fun loadHelpRequests() = loadMapData(
        isEvent = false,
        cached = cachedHelpRequests,
        updateCache = { cachedHelpRequests = it },
        query = { getHelpRequestDefaultQueryUpTo50().get() },
        getMarkerColor = { document ->
            if (document.getBoolean("isHelpNeeded") == true)
                BitmapDescriptorFactory.HUE_GREEN
            else
                BitmapDescriptorFactory.HUE_RED
        }
    )

    private suspend fun processMarkerResults(
        deferredResults: List<Deferred<MarkerData?>>,
        markerDataList: MutableList<MarkerData>,
        onComplete: () -> Unit
    ) {
        deferredResults.awaitAll()
            .filterNotNull()
            .forEach { markerData ->
                markerDataList.add(markerData)
                withContext(Dispatchers.Main) {
                    if (isAdded) {
                        addMarkerToMap(markerData)
                    }
                }
            }

        withContext(Dispatchers.Main) {
            if (isAdded) {
                onComplete()
                binding.mapLoadingContainer.visibility = View.GONE
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        coroutineScope.cancel()
    }

    private fun setEventListener(){

    }

    private fun setRequestComponentListener(){
        binding.requestComponent.setOnClickListener {
            val bundle = Bundle()
            bundle.putString(NavigationUtil.FRAGMENT_KEY, NavigationUtil.FRAGMENT_REQUEST)
            findNavController().navigate(R.id.communityHelpFragment,bundle)
        }

    }

    private fun setHelpComponentListener(){
        binding.helpComponent.setOnClickListener {
            val bundle = Bundle()
            bundle.putString(NavigationUtil.FRAGMENT_KEY, NavigationUtil.FRAGMENT_HELP)
            findNavController().navigate(R.id.communityHelpFragment,bundle)
        }
    }

   override fun onStart() {
        super.onStart()
        if (checkGooglePlayServices()) {
            getLocation()
        }
   }

    @SuppressLint("MissingPermission", "SetTextI18n")
    private fun getLocation() {
        if (checkPermissions()) {
            if (isLocationEnabled()) {
                fusedLocationClient.lastLocation.addOnCompleteListener(requireActivity()) { task ->
                    val location: Location? = task.result
                    if (location != null) {
                        Log.d(TAG,"not null location")
                        val geocoder = Geocoder(requireContext(), Locale.getDefault())
                        val list: List<Address> = geocoder
                            .getFromLocation(location.latitude, location.longitude, 1)
                                as List<Address>
//                        cityTextView.paintFlags = cityTextView.paintFlags or
//                                Paint.UNDERLINE_TEXT_FLAG
//                        cityTextView.text = list[0].locality
                    }else{
                        Log.d(TAG,"null location")
                    }
                }
            } else {
                Toast.makeText(requireContext(), R.string.turn_on_location, Toast.LENGTH_LONG).show()
              /* val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                startActivity(intent)
               */
            }
        } else {
            requestPermissions()
        }
    }

    private fun checkGooglePlayServices(): Boolean {
        val googleApiAvailability = GoogleApiAvailability.getInstance()
        val checkGooglePlayServices =
            googleApiAvailability.isGooglePlayServicesAvailable(requireContext())
        if (checkGooglePlayServices != ConnectionResult.SUCCESS) {
            /*
        * Google Play Services is missing or update is required
        *  return code could be
        * SUCCESS,
        * SERVICE_MISSING, SERVICE_VERSION_UPDATE_REQUIRED,
        * SERVICE_DISABLED, SERVICE_INVALID.
        */
            if (googleApiAvailability.isUserResolvableError(checkGooglePlayServices)) {
                googleApiAvailability.getErrorDialog(
                    requireActivity(),
                    checkGooglePlayServices,
                    REQUEST_CODE_RECOVER_PLAY_SERVICES
                )!!
                    .show()
            }
            return false
        }
        return true
    }
    private fun isLocationEnabled(): Boolean {
        val locationManager: LocationManager =
            requireContext().getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
                || locationManager.isProviderEnabled(
            LocationManager.NETWORK_PROVIDER
        )
    }
    private fun checkPermissions(): Boolean {
        return ActivityCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED ||
                ActivityCompat.checkSelfPermission(
                    requireContext(),
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
    }
    private fun requestPermissions() {
        requestPermissions(
            arrayOf(
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_FINE_LOCATION
            ),
            permissionId
        )
    }



    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
//        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == permissionId) {
            if (grantResults.isNotEmpty()
                && grantResults[0] == PackageManager.PERMISSION_GRANTED
            ) {
                //update UI
                getLocation()
            }
        }
    }


}