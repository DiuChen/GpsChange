package com.dafy.gpschange

import android.content.Context
import android.graphics.BitmapFactory
import android.location.Location
import android.location.LocationManager
import android.os.Bundle
import android.os.SystemClock
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.amap.api.maps.AMap
import com.amap.api.maps.CameraUpdateFactory
import com.amap.api.maps.model.BitmapDescriptorFactory
import com.amap.api.maps.model.LatLng
import com.amap.api.maps.model.MarkerOptions
import com.amap.api.maps.model.animation.ScaleAnimation
import com.amap.api.services.district.DistrictSearch
import com.amap.api.services.district.DistrictSearchQuery
import kotlinx.android.synthetic.main.activity_main.*
import java.util.*

class MainActivity : AppCompatActivity(), AMap.OnMapClickListener {
    private val mLocManager by lazy { getSystemService(Context.LOCATION_SERVICE) as LocationManager }
    private val districtSearch: DistrictSearch by lazy { DistrictSearch(this) }
    private var mockLocation = false
    private var myLocation: LatLng? = null
    private val mAMap by lazy { mapView.map }
    private val requestMarker by lazy {
        val requestMarkerOptions = MarkerOptions()
        requestMarkerOptions.icon(
            BitmapDescriptorFactory.fromBitmap(
                BitmapFactory.decodeResource(
                    resources,
                    R.mipmap.ic_a_location
                )
            )
        )
        val scaleAnimation = ScaleAnimation(0f, 1f, 0f, 1f)
        scaleAnimation.setDuration(500)
        val requestMarker = mAMap.addMarker(requestMarkerOptions)
        requestMarker.setAnimation(scaleAnimation)
        requestMarker
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        mapView.onCreate(savedInstanceState)

        locationBtn.setOnClickListener {
            if (mockLocation) {
                stopMockLocation()
                locationBtn.text = "开始定位"
            } else {
                if (myLocation != null) {
                    try {
                        mLocManager.addTestProvider(
                            LocationManager.GPS_PROVIDER,
                            false,
                            false,
                            false,
                            false,
                            true,
                            true,
                            true,
                            0,
                            5
                        )
                    } catch (e: Exception) {
                        Log.d("tag",e.toString())
                        Toast.makeText(this, "请启动模拟位置服务", Toast.LENGTH_SHORT).show()
                        return@setOnClickListener
                    }
                    startMockLocation(myLocation!!)
                    locationBtn.text = "停止定位"
                } else {
                    Toast.makeText(this, "请选择位置", Toast.LENGTH_SHORT).show()
                }
            }
        }
        mAMap.setOnMapClickListener(this)
        districtSearch.setOnDistrictSearchListener { districtResult ->
            if (districtResult.district.isNotEmpty()) {
                val latLonPoint = districtResult.district[0].center
                mAMap.moveCamera(CameraUpdateFactory.changeLatLng(LatLng(latLonPoint.latitude,latLonPoint.longitude) ))
            }
        }
        searchBtn.setOnClickListener {
            val key = searchEt.text.toString()
            if (key.isNotEmpty()){
                val districtSearchQuery = DistrictSearchQuery()
                districtSearchQuery.keywords = key
                districtSearch.query = districtSearchQuery
                districtSearch.searchDistrictAsyn()
            }
        }
    }

    //展示请求订单位置
    private fun showRequestLocation(requestLatLng: LatLng) {
        requestMarker.position = requestLatLng
        requestMarker.setToTop()
        mAMap.moveCamera(CameraUpdateFactory.changeLatLng(requestLatLng))
        requestMarker.startAnimation()
    }

    override fun onResume() {
        super.onResume()
        mapView.onResume()
    }

    override fun onPause() {
        super.onPause()
        mapView.onPause()
    }

    override fun onDestroy() {
        super.onDestroy()
        mapView.onDestroy()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        mapView.onSaveInstanceState(outState)
    }

    private fun startMockLocation(latLng: LatLng) {
        mLocManager.setTestProviderEnabled(LocationManager.GPS_PROVIDER, true)
        mockLocation = true
        val setLatLng = Coordtransform.GCJ02ToWGS84(latLng.longitude, latLng.latitude)
        val location = Location(LocationManager.GPS_PROVIDER)
        Thread {
            while (mockLocation) {
                location.longitude = setLatLng[0]   // 经度（度）
                location.latitude = setLatLng[1]  // 维度（度）
                location.altitude = 30.0    // 高程（米）
                location.bearing = 180.0f   // 方向（度）
                location.speed = 0f    //速度（米/秒）
                location.accuracy = 0.1f   // 精度（米）
                location.time = Date().time   // 本地时间
                location.elapsedRealtimeNanos = SystemClock.elapsedRealtimeNanos()
                mLocManager.setTestProviderLocation(LocationManager.GPS_PROVIDER, location)
                Log.d("tag","------------")
                Thread.sleep(500)
            }
        }.start()
        Toast.makeText(this, "已开始定位", Toast.LENGTH_SHORT).show()
    }

    private fun stopMockLocation() {
        mockLocation = false
        try {
            mLocManager.removeTestProvider(LocationManager.GPS_PROVIDER)
        } catch (e: Exception) {
            Toast.makeText(this, "请启动模拟位置服务", Toast.LENGTH_SHORT).show()
            return
        }
        Toast.makeText(this, "停止定位", Toast.LENGTH_SHORT).show()
    }

    override fun onMapClick(p0: LatLng) {
        if (!mockLocation) {
            myLocation = p0
            showRequestLocation(p0)
        }
    }
}
