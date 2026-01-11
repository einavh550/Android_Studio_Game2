package com.example.android_studio_game.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.android_studio_game.R
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.material.textview.MaterialTextView

class MapFragment : Fragment(), OnMapReadyCallback {

    private lateinit var map_LBL_hint: MaterialTextView

    private var googleMap: GoogleMap? = null
    private var pendingLatLng: LatLng? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val v = inflater.inflate(R.layout.fragment_map, container, false)
        findViews(v)
        return v
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val mapFragment = childFragmentManager
            .findFragmentById(R.id.map_FCV_google) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }

    private fun findViews(v: View) {
        map_LBL_hint = v.findViewById(R.id.map_LBL_hint)
    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map

        pendingLatLng?.let {
            moveMarkerAndCamera(it)
            pendingLatLng = null
        }
    }

    fun zoom(lat: Double, lon: Double) {
        val latLng = LatLng(lat, lon)

        if (googleMap == null) {
            pendingLatLng = latLng
            return
        }

        moveMarkerAndCamera(latLng)
    }

    private fun moveMarkerAndCamera(latLng: LatLng) {
        map_LBL_hint.visibility = View.GONE

        googleMap?.clear()
        googleMap?.addMarker(MarkerOptions().position(latLng).title("High Score Location"))
        googleMap?.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 12f))
    }
}
