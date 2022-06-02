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
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.widget.Button
import android.widget.Toast
import androidx.constraintlayout.motion.widget.Debug.getLocation
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.android.example.lora_walkie_talkie.databinding.ActivityMapsBinding


import android.widget.TextView
import androidx.recyclerview.selection.SelectionPredicates
import androidx.recyclerview.selection.SelectionTracker
import androidx.recyclerview.selection.StorageStrategy
import androidx.recyclerview.widget.LinearLayoutManager


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
    //var sender: BluetoothGattCharacteristic

    // Stops scanning after 10 seconds.
    private val SCAN_PERIOD: Long = 10000

    private lateinit var myBtDeviceListAdapter: BluetoothDeviceListAdapter
    private lateinit var tracker: SelectionTracker<String>
    private var selectedDevice: BluetoothDevice? = null

    private lateinit var myBtGattListAdapter: GattListAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()

        binding = ActivityMapsBinding.inflate(layoutInflater)
        setContentView(binding.root)



        title = "KotlinApp"
        val button: Button = findViewById(R.id.button)
        button.setOnClickListener {
            if (!BLEconnection){
                scanLeDevice()
            }
            getLocation()
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
        /*
        binding.recyclerViewDevices.layoutManager = LinearLayoutManager(this)
        binding.recyclerViewDevices.adapter = myBtDeviceListAdapter
        binding.recyclerViewGatt.layoutManager = LinearLayoutManager(this)
        binding.recyclerViewGatt.adapter = myBtGattListAdapter

        // Setup the selection tracker (more UI stuff)
        tracker = SelectionTracker.Builder(
            "mySelection",
            binding.recyclerViewDevices,
            MyItemKeyProvider(myBtDeviceListAdapter),
            MyItemDetailsLookup(binding.recyclerViewDevices),
            StorageStrategy.createStringStorage()
        ).withSelectionPredicate(
            SelectionPredicates.createSelectSingleAnything()
        ).build()

        myBtDeviceListAdapter.tracker = tracker
        tracker?.addObserver(
            object : SelectionTracker.SelectionObserver<String>() {
                override fun onSelectionChanged() {
                    super.onSelectionChanged()
                    myBtGattListAdapter.clearServices()
                    val items = tracker?.selection!!.size()
                    if (items > 0) {
                        selectedDevice = myBtDeviceListAdapter.getDeviceFromAddress(
                            tracker?.selection.elementAt(0)
                        )
                    } else {
                        selectedDevice = null
                    }
                }
            })
        */

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

        // If we have no bluetooth, don't scan for BT devices

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
        mMap.addMarker(MarkerOptions().position(sydney).title("Marker in Sydney"))
        mMap.moveCamera(CameraUpdateFactory.newLatLng(sydney))
    }

    private fun getLocation() {
        locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        if ((ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED)) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), locationPermissionCode)
        }
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 5000, 5f, this)
    }
    override fun onLocationChanged(location: Location) {
        lat = location.latitude
        long = location.longitude
        tvGpsLocation = findViewById(R.id.textView)
        tvGpsLocation.text = "Latitude: " + location.latitude + " , Longitude: " + location.longitude
        update(tvGpsLocation.text.toString())
        addLocMap(lat,long,"Here")
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

    private fun addLocMap(lat:Double,long:Double,pin_title:String) {
        var current_location = LatLng(lat, long)
        mMap.addMarker(MarkerOptions().position(current_location ).title("Marker in Sydney"))
        mMap.moveCamera(CameraUpdateFactory.newLatLng(current_location ))
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
            if (result.device.name == "UW Thermo-Clicker") {
                selectedDevice = result.device
                Log.e("BLE", "UW Thermo-Clicker")
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
    }
    @SuppressLint("MissingPermission")
    private fun checkAndConnectToHRM(services: List<BluetoothGattService>?) {
        Log.i(TAG, "Checking for Arduino Service")
        services?.forEach { service ->
            if (service.uuid == SampleGattAttributes.MICRCOCONTROLLER_SERVICE_UUID){
                Log.i(TAG, "Found Arduino Service")
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

                // Then, write a descriptor to the btGatt to enable notification
                val descriptor2 =
                    characteristic2.getDescriptor(SampleGattAttributes.CHARACTERISTIC_UPDATE_NOTIFICATION_DESCRIPTOR_UUID)
                descriptor2.value =
                    BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                bluetoothGatt!!.writeDescriptor(descriptor2)
                // When the characteristic value changes, the Gatt callback will be notified

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
            Log.e("BLE", result_state.toString())
            number = 1+number

        }
        //bluetoothGatt?.disconnect()
        //binding.buttonDisconnect.isEnabled = false
        //binding.buttonConnect.isEnabled = true
    }
}