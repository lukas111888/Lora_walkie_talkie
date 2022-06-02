package com.android.example.lora_walkie_talkie

import android.bluetooth.BluetoothDevice
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ListAdapter
import android.widget.TextView
import androidx.recyclerview.selection.ItemDetailsLookup
import androidx.recyclerview.selection.SelectionTracker
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView


class BluetoothDeviceListAdapter: androidx.recyclerview.widget.ListAdapter<BluetoothDevice,
        BluetoothDeviceListAdapter.ItemViewHolder>(DiffCallback()) {

    // Using the device ID as a selection id. In retrospect, just use the id/position.
    var tracker: SelectionTracker<String>? = null

    init {
        setHasStableIds(true)
    }

    fun addDevice(device: BluetoothDevice) {
        if (!currentList.contains(device)) {
            val items = mutableListOf<BluetoothDevice>()
            items.addAll(currentList)
            items.add(device)
            submitList(items)
        }
    }

    fun getDevice(position: Int): BluetoothDevice {
        return currentList[position]
    }

    fun getDeviceFromAddress(address: String): BluetoothDevice? {
        return currentList.find { it.address == address }
    }

    override fun getItem(position: Int): BluetoothDevice {
        return super.getItem(position)
    }

    override fun getItemId(position: Int): Long = position.toLong()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemViewHolder {
        return ItemViewHolder(
            LayoutInflater.from(parent.context)
                .inflate(R.layout.listitem_device, parent, false)
        )
    }

    override fun onBindViewHolder(holder: ItemViewHolder, position: Int) {
        val number = currentList[position]
        tracker?.let {
            holder.bind(number, it.isSelected(currentList[position].address))//position.toLong()))
        }
    }

    inner class ItemViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var deviceName: TextView? = itemView.findViewById(R.id.device_name)
        var deviceAddress: TextView? = itemView.findViewById(R.id.device_address)
        var layout: LinearLayout? = itemView.findViewById(R.id.layout)
        lateinit var address: String

        fun bind(item: BluetoothDevice, isSelected: Boolean) = with(itemView) {
            if (isSelected){
                layout?.setBackgroundColor(Color.parseColor("#FF5722"))
            } else {
                layout?.setBackgroundColor(Color.parseColor("#00BCD4"))
            }
            address = item.address
            if (item.name == null) {
             deviceName!!.text = "[no name]"
            } else {
                deviceName!!.text = item.name
            }
            deviceAddress!!.text = item.address
        }

        fun getItemDetails(): ItemDetailsLookup.ItemDetails<String> =
            object : ItemDetailsLookup.ItemDetails<String>() {
                override fun getPosition(): Int = bindingAdapterPosition
                override fun getSelectionKey(): String? = address
            }
    }

    class DiffCallback : DiffUtil.ItemCallback<BluetoothDevice>() {
        override fun areItemsTheSame(oldItem: BluetoothDevice, newItem: BluetoothDevice): Boolean {
            return oldItem.address == newItem.address
        }

        override fun areContentsTheSame(oldItem: BluetoothDevice, newItem: BluetoothDevice): Boolean {
            return oldItem == newItem
        }
    }

}

