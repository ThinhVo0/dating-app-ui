package com.example.datingapp.adapter;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.example.datingapp.R;
import com.example.datingapp.dto.MessageDTO;

import java.util.List;

public class MessageAdapter extends RecyclerView.Adapter<MessageAdapter.MessageViewHolder> {
    private static final String TAG = "MessageAdapter";
    private List<MessageDTO> messages;
    private String currentUserId;

    public MessageAdapter(List<MessageDTO> messages, String currentUserId) {
        this.messages = messages;
        this.currentUserId = currentUserId;
    }

    @Override
    public MessageViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_message, parent, false);
        return new MessageViewHolder(view);
    }

    @Override
    public void onBindViewHolder(MessageViewHolder holder, int position) {
        MessageDTO message = messages.get(position);
        boolean isSelf = message.getSenderId().equals(currentUserId);

        Log.d(TAG, "Binding message at position " + position + ": " + message.getContent() + ", isSelf: " + isSelf);

        if (isSelf) {
            holder.selfMessageLayout.setVisibility(View.VISIBLE);
            holder.otherMessageLayout.setVisibility(View.GONE);
            holder.tvMessageSelf.setText(message.getContent());
            holder.tvTimeSelf.setText(message.getSendTime());
        } else {
            holder.selfMessageLayout.setVisibility(View.GONE);
            holder.otherMessageLayout.setVisibility(View.VISIBLE);
            holder.tvMessageOther.setText(message.getContent());
            holder.tvTimeOther.setText(message.getSendTime());
        }
    }

    @Override
    public int getItemCount() {
        Log.d(TAG, "Message count: " + messages.size());
        return messages.size();
    }

    static class MessageViewHolder extends RecyclerView.ViewHolder {
        LinearLayout selfMessageLayout, otherMessageLayout;
        TextView tvMessageSelf, tvMessageOther, tvTimeSelf, tvTimeOther;

        MessageViewHolder(View itemView) {
            super(itemView);
            selfMessageLayout = itemView.findViewById(R.id.selfMessageLayout);
            otherMessageLayout = itemView.findViewById(R.id.otherMessageLayout);
            tvMessageSelf = itemView.findViewById(R.id.tvMessageSelf);
            tvMessageOther = itemView.findViewById(R.id.tvMessageOther);
            tvTimeSelf = itemView.findViewById(R.id.tvTimeSelf);
            tvTimeOther = itemView.findViewById(R.id.tvTimeOther);
        }
    }
}