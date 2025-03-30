package com.example.datingapp.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.datingapp.R;
import java.util.ArrayList;
import java.util.List;

public class ChatFragment extends Fragment {

    private RecyclerView rvChatList;
    private ChatAdapter chatAdapter;
    private List<Chat> chatList;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_chat, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Ánh xạ RecyclerView
        rvChatList = view.findViewById(R.id.rvChatList);

        // Khởi tạo danh sách chat giả lập
        initializeChatList();

        // Cài đặt RecyclerView
        rvChatList.setLayoutManager(new LinearLayoutManager(requireContext()));
        chatAdapter = new ChatAdapter(chatList);
        rvChatList.setAdapter(chatAdapter);
    }

    private void initializeChatList() {
        chatList = new ArrayList<>();
        chatList.add(new Chat("Phi Thắng", "Xin chào, bạn khỏe không?", "10:30 AM", 2));
        chatList.add(new Chat("Thịnh Xuân", "Hôm nay gặp nhau nhé!", "9:15 AM", 0));
        chatList.add(new Chat("Lan Anh", "Cảm ơn bạn đã thích mình.", "Hôm qua", 1));
        // Thêm dữ liệu thực tế từ API hoặc nguồn khác nếu cần
    }

    // Model cho chat
    private static class Chat {
        String userName;
        String lastMessage;
        String messageTime;
        int unreadCount;

        Chat(String userName, String lastMessage, String messageTime, int unreadCount) {
            this.userName = userName;
            this.lastMessage = lastMessage;
            this.messageTime = messageTime;
            this.unreadCount = unreadCount;
        }
    }

    // Adapter cho RecyclerView
    private static class ChatAdapter extends RecyclerView.Adapter<ChatAdapter.ChatViewHolder> {
        private static List<Chat> chats;

        ChatAdapter(List<Chat> chats) {
            this.chats = chats;
        }

        @Override
        public ChatViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_chat, parent, false);
            return new ChatViewHolder(view);
        }

        @Override
        public void onBindViewHolder(ChatViewHolder holder, int position) {
            Chat chat = chats.get(position);
            holder.tvUserName.setText(chat.userName);
            holder.tvLastMessage.setText(chat.lastMessage);
            holder.tvMessageTime.setText(chat.messageTime);

            // Hiển thị badge nếu có tin nhắn chưa đọc
            if (chat.unreadCount > 0) {
                holder.tvUnreadCount.setText(String.valueOf(chat.unreadCount));
                holder.tvUnreadCount.setVisibility(View.VISIBLE);
            } else {
                holder.tvUnreadCount.setVisibility(View.GONE);
            }

            // TODO: Load avatar từ URL nếu có (dùng Glide hoặc Picasso)
        }

        @Override
        public int getItemCount() {
            return chats.size();
        }

        static class ChatViewHolder extends RecyclerView.ViewHolder {
            TextView tvUserName, tvLastMessage, tvMessageTime, tvUnreadCount;
            ImageView ivUserAvatar;

            ChatViewHolder(View itemView) {
                super(itemView);
                tvUserName = itemView.findViewById(R.id.tvUserName);
                tvLastMessage = itemView.findViewById(R.id.tvLastMessage);
                tvMessageTime = itemView.findViewById(R.id.tvMessageTime);
                tvUnreadCount = itemView.findViewById(R.id.tvUnreadCount);
                ivUserAvatar = itemView.findViewById(R.id.ivUserAvatar);

                // Sự kiện click để mở cuộc trò chuyện
                itemView.setOnClickListener(v -> {
                    // Tạo Bundle để gửi dữ liệu
                    Bundle bundle = new Bundle();
                    bundle.putString("userName", chats.get(getAdapterPosition()).userName);

                    // Điều hướng bằng NavController
                    NavController navController = Navigation.findNavController(v);
                    navController.navigate(R.id.action_chat_to_chat_detail, bundle);
                });
            }
        }
    }
}