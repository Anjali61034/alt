package com.navigine.navigine.demo.adapters.debug;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;

import com.navigine.idl.java.SignalMeasurement;
import com.navigine.navigine.demo.R;

import java.util.Locale;

public class DebugAdapterEddystone extends DebugAdapterBaseExpanded<DebugViewHolderBaseBeacons, SignalMeasurement> {


    @NonNull
    @Override
    public DebugViewHolderBaseBeacons onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = null;
        switch (viewType) {
            case 0:
                view = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_item_round_top_debug_beacons, parent, false);
                break;
            case 1:
                view = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_item_round_bottom_debug_beacons, parent, false);
                break;
            default:
                view = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_item_debug_beacons, parent, false);
                break;
        }
        return new DebugViewHolderBaseBeacons(view);
    }

    @Override
    public void onBindViewHolder(@NonNull DebugViewHolderBaseBeacons holder, int position) {
        if (position == 0) {
            holder.title.setText(String.format(Locale.ENGLISH, "EDDYSTONE (%d), entries/sec: %.1f", mCurrentList.size(), (float) mCurrentList.size()));
            super.onBindViewHolder(holder, position);
        } else {
            try {
                SignalMeasurement result = mCurrentList.get(position - 1);
                String id = result.getId();

                // Display the formatted identifier (namespace and instance for Eddystone UID)
                holder.uuid.setText(id);
                holder.rssi.setText(String.format(Locale.ENGLISH, "%.1f", result.getRssi()));
                holder.distance.setText(String.format(Locale.ENGLISH, "%.1fm", result.getDistance()));
            } catch (Exception e) {
                holder.uuid.setText("---");
                holder.rssi.setText("---");
                holder.distance.setText("---");
            }
        }
    }
}
