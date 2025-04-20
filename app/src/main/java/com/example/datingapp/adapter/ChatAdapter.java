package com.example.datingapp.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.datingapp.R;
import com.example.datingapp.dto.response.UserInfoResponse;

import java.util.List;

public class ChatAdapter extends RecyclerView.Adapter<ChatAdapter.ChatViewHolder> {
    private List<UserInfoResponse> users;
    private OnItemClickListener listener;

    public interface OnItemClickListener {
        void onItemClick(String userName, String userId, String userAvatar);
    }

    public ChatAdapter(List<UserInfoResponse> users, OnItemClickListener listener) {
        this.users = users;
        this.listener = listener;
    }

    @Override
    public ChatViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_chat_icon, parent, false);
        return new ChatViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ChatViewHolder holder, int position) {
        UserInfoResponse user = users.get(position);
        String fullName = user.getFirstName() + " " + user.getLastName();

        Glide.with(holder.itemView.getContext())
                .load(user.getPic1() != null && !user.getPic1().isEmpty() ? user.getPic1() : "https://via.placeholder.com/150")
                .circleCrop()
                .into(holder.ivUserAvatar);
    }

    @Override
    public int getItemCount() {
        return users.size();
    }

    class ChatViewHolder extends RecyclerView.ViewHolder {
        ImageView ivUserAvatar;

        ChatViewHolder(View itemView) {
            super(itemView);
            ivUserAvatar = itemView.findViewById(R.id.ivUserAvatar);

            itemView.setOnClickListener(v -> {
                UserInfoResponse user = users.get(getAdapterPosition());
                String fullName = user.getFirstName() + " " + user.getLastName();
                String userId = user.getUserId();
                String userAvatar = user.getPic1();
                listener.onItemClick(fullName, userId, userAvatar);
            });
        }
    }
}