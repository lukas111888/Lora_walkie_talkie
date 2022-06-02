package com.android.example.lora_walkie_talkie

import java.util.*

/*
 * Copyright (C) 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */



/**
 * This class includes a small subset of standard GATT attributes for demonstration purposes.
 */
object SampleGattAttributes {
    private val attributes: HashMap<String?, String?> = HashMap<String?, String?>()
    var TEMPERATURE_MEASUREMENT = "00002a1c-0000-1000-8000-00805f9b34fb"
    var TEMPERATURE_MEASUREMENT_UUID = UUID.fromString("00002a1c-0000-1000-8000-00805f9b34fb")
    var SWITCH_STATE = "00002a05-0000-1000-8000-00805f9b34fb"
    var SWITCH_STATE_UUID = UUID.fromString("00002a05-0000-1000-8000-00805f9b34fb")
    var MICRCOCONTROLLER_SERVICE = "0000180a-0000-1000-8000-00805f9b34fb"
    var MICRCOCONTROLLER_SERVICE_UUID = UUID.fromString("0000180a-0000-1000-8000-00805f9b34fb")
    val CHARACTERISTIC_UPDATE_NOTIFICATION_DESCRIPTOR_UUID =
        UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")

    var CLIENT_CHARACTERISTIC_CONFIG = "00002902-0000-1000-8000-00805f9b34fb"
    fun lookup(uuid: String?, defaultName: String): String {
        val name = attributes[uuid]
        return name ?: defaultName
    }

    init {
        // Sample Services.
        attributes["0000180d-0000-1000-8000-00805f9b34fb"] = "Microcontroller Service"
        attributes["0000180a-0000-1000-8000-00805f9b34fb"] = "Device Information Service"
        // Sample Characteristics.
        attributes[SWITCH_STATE] = "Switch statement"
        attributes["00002a29-0000-1000-8000-00805f9b34fb"] = "Manufacturer Name String"
    }
}