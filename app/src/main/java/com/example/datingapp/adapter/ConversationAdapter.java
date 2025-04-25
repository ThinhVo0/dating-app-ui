package com.example.datingapp.adapter;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.datingapp.R;
import com.example.datingapp.dto.ConversationSummaryDTO;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ConversationAdapter extends RecyclerView.Adapter<ConversationAdapter.ConversationViewHolder> {
    private static final String TAG = "ConversationAdapter";
    private List<ConversationSummaryDTO> summaries;
    private OnItemClickListener listener;

    public interface OnItemClickListener {
        void onItemClick(String userName, String userId, String userAvatar);
    }

    public ConversationAdapter(List<ConversationSummaryDTO> summaries, OnItemClickListener listener) {
        this.summaries = summaries;
        this.listener = listener;
    }

    @Override
    public ConversationViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_chat, parent, false);
        return new ConversationViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ConversationViewHolder holder, int position) {
        ConversationSummaryDTO summary = summaries.get(position);

        // Load avatar
        Glide.with(holder.itemView.getContext())
                .load(summary.getProfilePicture() != null && !summary.getProfilePicture().isEmpty()
                        ? summary.getProfilePicture()
                        : "https://via.placeholder.com/150")
                .circleCrop()
                .into(holder.ivUserAvatar);

        // Set name and latest message
        holder.tvUserName.setText(summary.getName());
        holder.tvLastMessage.setText(summary.getLatestMessage());

        // Format and set message time
        try {
            SimpleDateFormat isoFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS", Locale.getDefault());
            Date date = isoFormat.parse(summary.getLatestMessageTime());
            SimpleDateFormat displayFormat = new SimpleDateFormat("hh:mm a", Locale.getDefault());
            holder.tvMessageTime.setText(displayFormat.format(date));
        } catch (ParseException e) {
            holder.tvMessageTime.setText(summary.getLatestMessageTime());
            Log.e(TAG, "Date parsing error: " + e.getMessage());
        }

        // Set unread count với 9+ nếu >9
        int count = summary.getUnreadCount();
        if (count > 0) {
            holder.tvUnreadCount.setText(count > 9 ? "9+" : String.valueOf(count));
            holder.tvUnreadCount.setVisibility(View.VISIBLE);
        } else {
            holder.tvUnreadCount.setVisibility(View.GONE);
        }

    }

    @Override
    public int getItemCount() {
        return summaries.size();
    }

    class ConversationViewHolder extends RecyclerView.ViewHolder {
        ImageView ivUserAvatar;
        TextView tvUserName, tvLastMessage, tvMessageTime, tvUnreadCount;

        ConversationViewHolder(View itemView) {
            super(itemView);
            ivUserAvatar = itemView.findViewById(R.id.ivUserAvatar);
            tvUserName = itemView.findViewById(R.id.tvUserName);
            tvLastMessage = itemView.findViewById(R.id.tvLastMessage);
            tvMessageTime = itemView.findViewById(R.id.tvMessageTime);
            tvUnreadCount = itemView.findViewById(R.id.tvUnreadCount);

            itemView.setOnClickListener(v -> {
                ConversationSummaryDTO summary = summaries.get(getAdapterPosition());
                listener.onItemClick(summary.getName(), summary.getUserId(), summary.getProfilePicture());
            });
        }
    }
}