package com.example.datingapp.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.example.datingapp.R;
import com.example.datingapp.dto.Notification;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class NotificationAdapter extends RecyclerView.Adapter<NotificationAdapter.NotificationViewHolder> {
    private List<Notification> notifications;

    public NotificationAdapter(List<Notification> notifications) {
        this.notifications = notifications;
    }

    @Override
    public NotificationViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_notification, parent, false);
        return new NotificationViewHolder(view);
    }

    @Override
    public void onBindViewHolder(NotificationViewHolder holder, int position) {
        Notification notification = notifications.get(position);
        switch (notification.getType()) {
            case "like":
                holder.tvTitle.setImageResource(R.drawable.ic_notify_like);
                break;
            case "match":
                holder.tvTitle.setImageResource(R.drawable.ic_notify_match);
                break;
            case "message":
                holder.tvTitle.setImageResource(R.drawable.ic_notify_chat);
                break;
            default:
                holder.tvTitle.setImageResource(R.drawable.btn_notify);
                break;
        }
        holder.tvContent.setText(notification.getContent());
        holder.tvTime.setText(formatTime(notification.getCreatedAt()));
    }

    @Override
    public int getItemCount() {
        return notifications.size();
    }

    private String formatTime(String createdAt) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault());
            Date date = sdf.parse(createdAt);

            long diffMillis = System.currentTimeMillis() - date.getTime();
            long minutes = diffMillis / (1000 * 60);
            long hours = minutes / 60;
            long days = hours / 24;

            if (minutes < 1) {
                return "Vừa xong";
            } else if (minutes < 60) {
                return minutes + " phút trước";
            } else if (hours < 24) {
                return hours + " giờ trước";
            } else if (days == 1) {
                return "Hôm qua";
            } else if (days < 7) {
                return days + " ngày trước";
            } else {
                // Hiển thị định dạng ngày tháng đầy đủ
                SimpleDateFormat outputFormat = new SimpleDateFormat("dd 'thg' MM, yyyy", Locale.getDefault());
                return outputFormat.format(date);
            }
        } catch (Exception e) {
            return createdAt;
        }
    }

    static class NotificationViewHolder extends RecyclerView.ViewHolder {
        ImageView tvTitle;
        TextView tvContent, tvTime;

        NotificationViewHolder(View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.imgNotificationIcon);
            tvContent = itemView.findViewById(R.id.tvNotificationContent);
            tvTime = itemView.findViewById(R.id.tvNotificationTime);
        }
    }
}