package com.alex.find.adapter;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.alex.find.R;
import com.alex.find.bean.Device;

import java.util.List;

public class DevicesRecyclerViewAdapter extends RecyclerView.Adapter<DevicesRecyclerViewAdapter.ViewHolder> {

    private final List<Device> mDevices;
    private OnListInteractionListener mListener;

    public DevicesRecyclerViewAdapter(List<Device> items, OnListInteractionListener listener) {
        mDevices = items;
        mListener = listener;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.devices_list_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, int position) {
        holder.mItem = mDevices.get(position);
        holder.mIdView.setText(String.valueOf(position + 1));
        holder.mTitleView.setText(mDevices.get(position).content);
        holder.mDetailView.setText(mDevices.get(position).details);

        holder.mPlayView.setText(mDevices.get(position).getStatus());
        if ("播放".equals(mDevices.get(position).getStatus())) {
            holder.mPlayView.setClickable(true);
        } else {
            holder.mPlayView.setClickable(false);
        }

        holder.mPlayView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mListener.onListInteraction(holder.mItem);
            }
        });
    }

    @Override
    public int getItemCount() {
        return mDevices.size();
    }

    public interface OnListInteractionListener {
        // TODO: Update argument type and name
        void onListInteraction(Device item);
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        public final View mView;
        public final TextView mIdView;
        public final TextView mTitleView;
        public final TextView mDetailView;
        public final TextView mPlayView;
        public Device mItem;

        public ViewHolder(View view) {
            super(view);
            mView = view;
            mIdView = view.findViewById(R.id.id);
            mTitleView = view.findViewById(R.id.tv_title);
            mDetailView = view.findViewById(R.id.tv_detail);
            mPlayView = view.findViewById(R.id.action_play);
        }

        @Override
        public String toString() {
            return super.toString() + " '" + mTitleView.getText() + "'";
        }
    }
}
