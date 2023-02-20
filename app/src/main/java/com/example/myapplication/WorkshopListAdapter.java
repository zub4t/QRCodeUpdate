package com.example.myapplication;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class WorkshopListAdapter extends RecyclerView.Adapter<WorkshopListAdapter.WorkshopViewHolder> {

    private List<String> mTalks;
    private OnWorkshopClickListener mListener;

    public interface OnWorkshopClickListener {
        void onWorkshopClick(int position);
    }

    public WorkshopListAdapter(List<String> talks, OnWorkshopClickListener listener) {
        mTalks = talks;
        mListener = listener;
    }

    public static class WorkshopViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        public TextView mTalkView;
        public OnWorkshopClickListener mListener;

        public WorkshopViewHolder(@NonNull View itemView, OnWorkshopClickListener listener) {
            super(itemView);
            mListener = listener;
            itemView.setOnClickListener(this);
            mTalkView = itemView.findViewById(R.id.talk_text_view);
        }

        @Override
        public void onClick(View v) {
            mListener.onWorkshopClick(getAdapterPosition());
        }
    }

    @NonNull
    @Override
    public WorkshopViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.workshop_list_item, parent, false);
        return new WorkshopViewHolder(itemView, mListener);
    }

    @Override
    public void onBindViewHolder(@NonNull WorkshopViewHolder holder, int position) {
        String talk = mTalks.get(position);
        holder.mTalkView.setText(talk);
    }

    @Override
    public int getItemCount() {
        return mTalks.size();
    }
}
