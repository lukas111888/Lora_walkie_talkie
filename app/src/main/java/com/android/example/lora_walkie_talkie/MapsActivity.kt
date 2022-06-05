package com.android.example.lora_walkie_talkie


import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.*
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.location.LocationRequest
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.location.LocationManagerCompat.getCurrentLocation
import androidx.recyclerview.selection.SelectionTracker
import com.android.example.lora_walkie_talkie.databinding.ActivityMapsBinding
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.LocationServices.getFusedLocationProviderClient
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import kotlin.math.roundToInt


class MapsActivity : AppCompatActivity(), OnMapReadyCallback, LocationListener{
    private var lat: Double= 0.0
    private var long: Double= 0.0
    private lateinit var mMap: GoogleMap
    private lateinit var binding: ActivityMapsBinding
    private lateinit var locationManager: LocationManager
    private lateinit var tvGpsLocation: TextView
    private val locationPermissionCode = 2
    private var BLEconnection = false
    private val TAG = "MainActivity"
    private val REQUEST_ENABLE_BT: Int = 42
    var bluetoothManager: BluetoothManager? = null
    var bluetoothAdapter: BluetoothAdapter? = null
    private var bluetoothLeScanner: BluetoothLeScanner? = null
    private var scanning = false
    private val handler = Handler()
    var bluetoothGatt: BluetoothGatt? = null
    var number = 0
    var count = 0
    var MyMarker: Marker?=null
    var TheirMarker:Marker?=null
    //var sender: BluetoothGattCharacteristic

    // Stops scanning after 10 seconds.
    private val SCAN_PERIOD: Long = 10000
    private val mLocationRequest: LocationRequest? = null

    private lateinit var myBtDeviceListAdapter: BluetoothDeviceListAdapter
    private lateinit var tracker: SelectionTracker<String>
    private var selectedDevice: BluetoothDevice? = null

    private lateinit var myBtGattListAdapter: GattListAdapter
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    var loop_killer = 0



    @RequiresApi(Build.VERSION_CODES.S)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()

        binding = ActivityMapsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        fusedLocationClient = getFusedLocationProviderClient(this)

        title = "KotlinApp"
        // BLE+GPS
        val button: Button = findViewById(R.id.button)
        button.setOnClickListener {
            if (!BLEconnection){
                scanLeDevice()
                Toast.makeText(baseContext, "Connected!", Toast.LENGTH_LONG).show()
                getLastKnownLocation()
            }else if (lat != null){
                if (binding.button.isEnabled and binding.button2.isEnabled){
                    update("G$lat,$long")
                    Toast.makeText(baseContext, "button", Toast.LENGTH_LONG).show()
                }
                getLocation()
            }
        }

        //TXT
        val button2: Button = findViewById(R.id.button2)
        button2.setOnClickListener {
            val editText: EditText = findViewById(R.id.MessageText)
            val msg=editText.text.toString()
            number = 1+number
            update("T$msg:$number")
            Toast.makeText(baseContext, "button2", Toast.LENGTH_LONG).show()
        }

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        ActivityCompat.requestPermissions(this,
            arrayOf( Manifest.permission.BLUETOOTH, Manifest.permission.BLUETOOTH_ADMIN,
                Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT), 1)

        // Get Bluetooth stuff
        this.bluetoothManager = getSystemService(BluetoothManager::class.java)
        bluetoothAdapter = this.bluetoothManager!!.getAdapter()
        bluetoothLeScanner = bluetoothAdapter?.bluetoothLeScanner
        // Set up the UI
        myBtDeviceListAdapter = BluetoothDeviceListAdapter()
        myBtGattListAdapter = GattListAdapter()


        if (bluetoothAdapter != null) {
            if (!bluetoothAdapter!!.isEnabled) {
                val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                if (ActivityCompat.checkSelfPermission(
                        this,
                        Manifest.permission.BLUETOOTH_CONNECT
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    // TODO: Consider calling ActivityCompat#requestPermissions
                    return
                }
                startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT)
            }
        }
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
        mMap = googleMap
        var sydney = LatLng(31.0, 151.0)
        // Add a marker in Sydney and move the camera
        if (lat != null) {
            sydney = LatLng(lat, long)
        }
        MyMarker=mMap.addMarker(MarkerOptions().position(sydney).title("My Location"))
        mMap.moveCamera(CameraUpdateFactory.newLatLng(sydney))
    }

    //https://developer.android.com/training/location/retrieve-current
    @SuppressLint("MissingPermission")
    private fun getLastKnownLocation() {
        fusedLocationClient.lastLocation
            .addOnSuccessListener { location : Location? ->
                // Got last known location. In some rare situations this can be null.
                if (location != null) {
                    Log.e("gpsLat",location.latitude.toString())
                    Log.e("gpsLong",location.longitude.toString())
                    addLocMap(location.latitude,location.longitude,"My Location")
                    lat = (location.latitude* 1000000.0).roundToInt()/1000000.0
                    long = (location.longitude* 1000000.0).roundToInt()/1000000.0
                    Log.e(TAG, "getLastKnownLocation:$long")
                }
            }
    }

    private fun getLocation() {
        count = 0
        locationManager = getSystemService(LOCATION_SERVICE) as LocationManager
        if ((ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED)) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), locationPermissionCode)
        }
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1500, 0f, this)
    }

    override fun onLocationChanged(location: Location) {
        Log.e(TAG, "LOCATION UPDATE!!!")
        lat = (location.latitude* 1000000.0).roundToInt()/1000000.0
        long = (location.longitude* 1000000.0).roundToInt()/1000000.0
        tvGpsLocation = findViewById(R.id.textView)
        tvGpsLocation.text = "Latitude: " + location.latitude + " , Longitude: " + location.longitude
        locationManager.removeUpdates(this)
        addLocMap(lat,long,"My Location")
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == locationPermissionCode) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Permission Granted", Toast.LENGTH_SHORT).show()
            }
            else {
                Toast.makeText(this, "Permission Denied", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun addLocMap(alat:Double,along:Double,pin_title:String) {
        runOnUiThread{
            if(pin_title=="My Location"){
                MyMarker?.remove()
                var current_location = LatLng(alat, along)
                Log.e(TAG, "Location:$current_location")
                MyMarker=mMap.addMarker(MarkerOptions().position(current_location ).title(pin_title))
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(current_location,10f))}
            if (pin_title=="Received"){
                TheirMarker?.remove()
                var current_location = LatLng(alat, along)
                Log.e(TAG, "Received:$current_location")
                TheirMarker=mMap.addMarker(MarkerOptions().position(current_location ).title(pin_title))
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(current_location,10f))
            }
        }
    }


    @SuppressLint("MissingPermission")
    private fun scanLeDevice() {
        if (!scanning) {
            // Stops scanning after a pre-defined scan period.
            handler.postDelayed(stopLeScan(), SCAN_PERIOD)
            // Start the scan
            scanning = true
            bluetoothLeScanner?.startScan(leScanCallback)
        } else {
            // Will hit here if we are already scanning
            scanning = false
            bluetoothLeScanner?.stopScan(leScanCallback)
        }
    }

    @SuppressLint("MissingPermission")
    private fun stopLeScan() = {
        if (scanning != false) {
            scanning = false
            bluetoothLeScanner?.stopScan(leScanCallback)
        }
    }

    // Device scan callback.
    private val leScanCallback: ScanCallback = object : ScanCallback() {
        @SuppressLint("MissingPermission")
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            super.onScanResult(callbackType, result)
            myBtDeviceListAdapter.addDevice(result.device)
            // It will stop scan after found device and auto connected
            if (result.device.name == "UW LoRa B") {
                selectedDevice = result.device
                Log.e("BLE", "UW LoRa B")
                connectToDevice()
                handler.post(
                    stopLeScan()
                )
                BLEconnection = true
            }
        }
    }

    private val bluetoothGattCallback = object : BluetoothGattCallback() {
        @SuppressLint("MissingPermission")
        override fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                // successfully connected to the GATT Server
                Log.i(TAG, "Starting service discovery")
                bluetoothGatt?.discoverServices()
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                // disconnected from the GATT Server
                Log.i(TAG, "disconnected from the GATT Server")
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.i(TAG, "Services discovered: ")
                Log.i(TAG, gatt?.services.toString())
                checkAndConnectToHRM(bluetoothGatt?.services)
            } else {
                Log.w(TAG, "onServicesDiscovered received: $status")
            }
        }
        override fun onCharacteristicChanged(
            gatt: BluetoothGatt?,
            characteristic: BluetoothGattCharacteristic?
        ) {
            super.onCharacteristicChanged(gatt, characteristic)
            Log.e("ALL", characteristic?.getStringValue(0).toString())
            characteristic?.getStringValue(0)?.substring(0, 1)?.let {
                when(it){
                    "G" ->{
                        val value = characteristic.getStringValue(0).substring(1)
                        val middle = value.indexOf(",")
                        val anther_lat = value.substring(0,middle)
                        val anther_long = value.substring(middle+1)
                        Log.e("Get GPS: ","G"+value)

                        (anther_lat.toDouble() != lat).let{
                            update("G" + lat.toString() + "," + long.toString())
                            addLocMap(anther_lat.toDouble(),anther_long.toDouble(),"Received")
                        }
                        (anther_lat.toDouble() == lat).let{
                            handler.post {
                                binding.button.isEnabled = true
                                binding.button2.isEnabled = true
                            }
                        }
                    }
                    "T" ->{
                        val msg = "msg:"+characteristic.getStringValue(0).substring(1)
                        Log.e("msg:", msg)
                        binding.textView.text = msg
                    }
                    else -> Log.e(TAG, it)
                }
            }

            if(characteristic?.getStringValue(0)?.substring(0, 4)=="DoNe") {
                Log.e(TAG, "msg to another phone is done!")
                handler.post{
                    binding.button.isEnabled = true
                    binding.button2.isEnabled = true
                }
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun checkAndConnectToHRM(services: List<BluetoothGattService>?) {
        Log.e(TAG, "Checking for Arduino Service")
        services?.forEach { service ->
            if (service.uuid == SampleGattAttributes.MICRCOCONTROLLER_SERVICE_UUID){
                Log.e(TAG, "Found Arduino Service")
                val characteristic = service.getCharacteristic(SampleGattAttributes.SWITCH_STATE_UUID)
                bluetoothGatt?.readCharacteristic(characteristic)
                //Log.e("0", "characteristic?.value?.get(0)?.toUByte().toString()")

                // First, call setCharacteristicNotification to enable notification.
                if (!bluetoothGatt?.setCharacteristicNotification(characteristic, true)!!) {
                    // Stop if the characteristic notification setup failed.
                    Log.e("BLE", "characteristic notification setup failed")
                    return
                }
                Thread.sleep(10)

                val characteristic2 = service.getCharacteristic(SampleGattAttributes.TEMPERATURE_MEASUREMENT_UUID)
                bluetoothGatt?.readCharacteristic(characteristic2)
                //Log.e("0", "characteristic?.value?.get(0)?.toUByte().toString()")

                // First, call setCharacteristicNotification to enable notification.
                if (!bluetoothGatt?.setCharacteristicNotification(characteristic2, true)!!) {
                    // Stop if the characteristic notification setup failed.
                    Log.e("BLE", "characteristic notification setup failed")
                    return
                }

                // Then, write a descriptor to the btGatt to enable notification
                val descriptor =
                    characteristic.getDescriptor(SampleGattAttributes.CHARACTERISTIC_UPDATE_NOTIFICATION_DESCRIPTOR_UUID)
                descriptor.value =
                    BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                bluetoothGatt!!.writeDescriptor(descriptor)
                // When the characteristic value changes, the Gatt callback will be notified
                /*
                // Then, write a descriptor to the btGatt to enable notification
                val descriptor2 =
                    characteristic2.getDescriptor(SampleGattAttributes.CHARACTERISTIC_UPDATE_NOTIFICATION_DESCRIPTOR_UUID)
                descriptor2.value =
                    BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                bluetoothGatt!!.writeDescriptor(descriptor2)
                // When the characteristic value changes, the Gatt callback will be notified

                 */
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun connectToDevice() {
        if (selectedDevice != null) {
            bluetoothAdapter?.let { adapter ->
                try {
                    val device = adapter.getRemoteDevice(selectedDevice!!.address)
                    // connect to the GATT server on the device
                    bluetoothGatt = device.connectGatt(this, true, bluetoothGattCallback)
                    binding.button.isEnabled = true
                    binding.button2.isEnabled = true
                    return
                } catch (exception: IllegalArgumentException) {
                    Log.w(TAG, "Device not found with provided address.  Unable to connect.")
                    return
                }
            } ?: run {
                Log.w(TAG, "BluetoothAdapter not initialized")
                return
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun update(gps:String) {
        handler.post{
            var sender=bluetoothGatt?.getService(SampleGattAttributes.MICRCOCONTROLLER_SERVICE_UUID
            )?.getCharacteristic(SampleGattAttributes.TEMPERATURE_MEASUREMENT_UUID)
            sender?.setValue(gps)
            val result_state=bluetoothGatt?.writeCharacteristic(sender)
            Log.e("Send to BLE", result_state.toString())
        }
        handler.post{
            binding.button.isEnabled = false
            binding.button2.isEnabled = false
        }
    }
    

    fun concatenate(vararg string: String?): String {
        var result = ""
        string.forEach { result = result.plus(it) }
        return result
    }

}