package com.android.example.lora_walkie_talkie

import android.bluetooth.BluetoothDevice
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.Adapter

// Adapted from https://android.googlesource.com/platform/development/+/7167a054a8027f75025c865322fa84791a9b3bd1/samples/BluetoothLeGatt/src/com/example/bluetooth/le/DeviceScanActivity.java

// Adapter for holding devices found through scanning.
class LeDeviceListAdapter : Adapter<LeDeviceListAdapter.ViewHolder>() {
    private val mLeDevices: ArrayList<BluetoothDevice>
    private val selected_position = -1

    init {
        mLeDevices = ArrayList()
    }

    fun addDevice(device: BluetoothDevice) {
        if (!mLeDevices.contains(device)) {
//            mLeDevices.add(device)
            mLeDevices.add(device)
//            notifyItemInserted(mLeDevices.size - 1)

            notifyDataSetChanged()
        }
    }

    fun getDevice(position: Int): BluetoothDevice {
        return mLeDevices[position]
    }

    fun clear() {
        val num = mLeDevices.size
        mLeDevices.clear()
        notifyItemRangeRemoved(0, num)
    }
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder.from(parent)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = mLeDevices[position]
        holder.bind(item)
    }

    override fun getItemCount(): Int {
        return mLeDevices.size
    }

    override fun getItemId(i: Int): Long {
        return i.toLong()
    }

    class ViewHolder private constructor(itemView: View) : RecyclerView.ViewHolder(itemView), View.OnClickListener{
        var deviceName: TextView? = itemView.findViewById(R.id.device_name)
        var deviceAddress: TextView? = itemView.findViewById(R.id.characteristics)

        fun bind(item: BluetoothDevice) {
            deviceName!!.text = item.name
            deviceAddress!!.text = item.address
        }

        companion object {
            fun from(parent: ViewGroup): ViewHolder {
                val layoutInflater = LayoutInflater.from(parent.context)
                val view = layoutInflater
                    .inflate(R.layout.listitem_device, parent, false)
                return ViewHolder(view)
            }
        }

        override fun onClick(view: View?) {
            // Below line is just like a safety check, because sometimes holder could be null,
            // in that case, getAdapterPosition() will return RecyclerView.NO_POSITION
            if (getAdapterPosition() == RecyclerView.NO_POSITION) return;

            // Updating old as well as new positions

//            selected_position = getAdapterPosition();
//            notifyItemChanged(selected_position);

            // Do your another stuff for your onClick
        }

    }


}