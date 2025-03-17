package com.example.datingapp.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.datingapp.R;
import java.util.ArrayList;
import java.util.List;

public class NotificationsFragment extends Fragment {

    private RecyclerView rvNotifications;
    private NotificationAdapter notificationAdapter;
    private List<Notification> notificationList;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_notification, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Ánh xạ RecyclerView
        rvNotifications = view.findViewById(R.id.rvNotifications);

        // Khởi tạo danh sách thông báo giả lập
        initializeNotificationList();

        // Cài đặt RecyclerView
        rvNotifications.setLayoutManager(new LinearLayoutManager(requireContext()));
        notificationAdapter = new NotificationAdapter(notificationList);
        rvNotifications.setAdapter(notificationAdapter);
    }

    private void initializeNotificationList() {
        notificationList = new ArrayList<>();
        notificationList.add(new Notification("Tin nhắn mới", "Thịnh Xuân đã gửi bạn một tin nhắn.", "1 giờ trước"));
        notificationList.add(new Notification("Khớp đôi", "Bạn đã ghép đôi với Thịnh Xuân!", "2 giờ trước"));
        notificationList.add(new Notification("Lượt thích mới", "Bạn có một lượt thích mới!", "3 giờ trước"));
        // Thêm dữ liệu thực tế từ API hoặc nguồn khác nếu cần
    }

    // Model cho thông báo
    private static class Notification {
        String title;
        String content;
        String time;

        Notification(String title, String content, String time) {
            this.title = title;
            this.content = content;
            this.time = time;
        }
    }

    // Adapter cho RecyclerView
    private static class NotificationAdapter extends RecyclerView.Adapter<NotificationAdapter.NotificationViewHolder> {
        private List<Notification> notifications;

        NotificationAdapter(List<Notification> notifications) {
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
            holder.tvTitle.setText(notification.title);
            holder.tvContent.setText(notification.content);
            holder.tvTime.setText(notification.time);
        }

        @Override
        public int getItemCount() {
            return notifications.size();
        }

        static class NotificationViewHolder extends RecyclerView.ViewHolder {
            TextView tvTitle, tvContent, tvTime;

            NotificationViewHolder(View itemView) {
                super(itemView);
                tvTitle = itemView.findViewById(R.id.tvNotificationTitle);
                tvContent = itemView.findViewById(R.id.tvNotificationContent);
                tvTime = itemView.findViewById(R.id.tvNotificationTime);
            }
        }
    }
}