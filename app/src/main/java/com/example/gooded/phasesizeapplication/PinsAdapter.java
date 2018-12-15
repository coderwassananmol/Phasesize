package com.example.gooded.phasesizeapplication;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.List;

/**
 * Created by root on 15/12/18.
 */

public class PinsAdapter extends RecyclerView.Adapter<PinsAdapter.PinsViewHolder> {
    class PinsViewHolder extends RecyclerView.ViewHolder {
        private final TextView pinsItemView;

        private PinsViewHolder(View itemView) {
            super(itemView);
            pinsItemView = itemView.findViewById(R.id.textView);
        }
    }

    private final LayoutInflater mInflater;
    private List<Pins> pinsList; // Cached copy of words

    PinsAdapter(Context context) { mInflater = LayoutInflater.from(context); }

    @Override
    public PinsViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = mInflater.inflate(R.layout.recyclerview_item, parent, false);
        return new PinsViewHolder(itemView);
    }

    public double[] getGeoLocation(int position) {
        Pins currentPin = pinsList.get(position);
        return new double[]{currentPin.getLatitude(),currentPin.getLongitude()};
    }

    @Override
    public void onBindViewHolder(PinsViewHolder holder, int position) {
        if (pinsList != null) {
            Pins current = pinsList.get(position);
            holder.pinsItemView.setText(current.getLatitude()+","+current.getLongitude() + ": " + current.getId());
        } else {
            // Covers the case of data not being ready yet.
            holder.pinsItemView.setText("No Pins");
        }
    }

    void setPins(List<Pins> pins) {
        pinsList = pins;
        notifyDataSetChanged();
    }

    //getItemCount() is called many times, and when it is first called,
    // pinsList has not been updated (means initially, it's null, and we can't return null).
    @Override
    public int getItemCount() {
        if (pinsList != null)
            return pinsList.size();
        else return 0;
    }
}
