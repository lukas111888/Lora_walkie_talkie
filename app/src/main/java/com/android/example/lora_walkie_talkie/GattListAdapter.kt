package com.android.example.lora_walkie_talkie

import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattService
import android.os.Build
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import java.util.stream.Collectors

class GattListAdapter:  ListAdapter<BluetoothGattService, GattListAdapter.ItemViewHolder>(GattListAdapter.DiffCallback())  {

    private val TAG: String = "GattListAdapter"

    init {
        setHasStableIds(true)
    }

    fun addService(service: BluetoothGattService) {
        if (!currentList.contains(service)) {
            val items = mutableListOf<BluetoothGattService>()
            items.addAll(currentList)
            items.add(service)
            submitList(items)
        }
    }

    fun addServices(services: List<BluetoothGattService>) {
        val items = mutableListOf<BluetoothGattService>()
        items.addAll(currentList)
        services.forEach { service ->
            if (!currentList.contains(service)) {
               items.add(service)
            }
        }
        submitList(items)
    }

    fun characteristicsToString(service: BluetoothGattService): String {
        val gattCharacteristicGroupData: ArrayList<HashMap<String, String>> = arrayListOf()
        val gattCharacteristics = service.characteristics
        val charas: MutableList<BluetoothGattCharacteristic> = mutableListOf()


        val charString = buildString{
            // Loops through available Characteristics.
            gattCharacteristics.forEach { gattCharacteristic ->
//                Log.i(TAG, "Characteristic UUID: " + gattCharacteristic.uuid.toString())
//                Log.i(TAG, gattCharacteristic.descriptors.toString())
                append(gattCharacteristic.uuid.toString())
                appendLine()
            }
        }
        return charString
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemViewHolder {
        return ItemViewHolder(
            LayoutInflater.from(parent.context)
                .inflate(R.layout.listitem_service, parent, false)
        )
    }

    override fun onBindViewHolder(holder: ItemViewHolder, position: Int) {
        holder.bind(currentList[position], false)//position.toLong()))
    }

    fun clearServices() {
        submitList(mutableListOf<BluetoothGattService>())
    }

    inner class ItemViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var serviceTextView: TextView? = itemView.findViewById(R.id.device_name)
        var charTextView: TextView? = itemView.findViewById(R.id.characteristics)

        fun bind(item: BluetoothGattService, isSelected: Boolean) = with(itemView) {
            serviceTextView!!.text = item.uuid.toString()
            charTextView!!.text = characteristicsToString(item)
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<BluetoothGattService>(){

        override fun areItemsTheSame(
            oldItem: BluetoothGattService,
            newItem: BluetoothGattService
        ): Boolean {
            return oldItem.uuid == newItem.uuid
        }

        override fun areContentsTheSame(
            oldItem: BluetoothGattService,
            newItem: BluetoothGattService
        ): Boolean {
            return oldItem.uuid == newItem.uuid
        }
    }
}