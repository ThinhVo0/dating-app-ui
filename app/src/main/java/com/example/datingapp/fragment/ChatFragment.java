package com.example.datingapp.fragment;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.datingapp.R;
import com.example.datingapp.activity.ChatDetailActivity;
import com.example.datingapp.dto.ConversationSummaryDTO;
import com.example.datingapp.dto.response.ApiResponse;
import com.example.datingapp.dto.response.UserInfoResponse;
import com.example.datingapp.network.AuthService;
import com.example.datingapp.network.RetrofitClient;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ChatFragment extends Fragment {

    private static final String TAG = "ChatFragment";
    private RecyclerView rvChatList, rvConversationList;
    private ChatAdapter chatAdapter;
    private ConversationAdapter conversationAdapter;
    private List<UserInfoResponse> matchedUsers;
    private List<ConversationSummaryDTO> conversationSummaries;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_chat, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Initialize horizontal RecyclerView for matched users
        rvChatList = view.findViewById(R.id.rvChatList);
        LinearLayoutManager chatLayoutManager = new LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false);
        rvChatList.setLayoutManager(chatLayoutManager);
        matchedUsers = new ArrayList<>();
        chatAdapter = new ChatAdapter(matchedUsers);
        rvChatList.setAdapter(chatAdapter);

        // Initialize vertical RecyclerView for conversation summaries
        rvConversationList = view.findViewById(R.id.rvConversationList);
        LinearLayoutManager conversationLayoutManager = new LinearLayoutManager(requireContext());
        rvConversationList.setLayoutManager(conversationLayoutManager);
        conversationSummaries = new ArrayList<>();
        conversationAdapter = new ConversationAdapter(conversationSummaries);
        rvConversationList.setAdapter(conversationAdapter);

        // Load initial data
        loadMatchedUsers();
        loadConversationSummaries();
    }

    @Override
    public void onResume() {
        super.onResume();
        // Refresh conversation summaries when returning to the fragment
        loadConversationSummaries();
    }

    private void loadMatchedUsers() {
        SharedPreferences sharedPreferences = requireContext().getSharedPreferences("MyPrefs", requireContext().MODE_PRIVATE);
        String authToken = sharedPreferences.getString("authToken", null);

        if (authToken == null) {
            Toast.makeText(requireContext(), "Vui lòng đăng nhập để xem danh sách", Toast.LENGTH_SHORT).show();
            return;
        }

        AuthService authService = RetrofitClient.getClient().create(AuthService.class);
        Call<ApiResponse<List<UserInfoResponse>>> call = authService.getMatchedUsers("Bearer " + authToken);

        call.enqueue(new Callback<ApiResponse<List<UserInfoResponse>>>() {
            @Override
            public void onResponse(Call<ApiResponse<List<UserInfoResponse>>> call, Response<ApiResponse<List<UserInfoResponse>>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().getStatus() == 200) {
                    matchedUsers.clear();
                    matchedUsers.addAll(response.body().getData());
                    chatAdapter.notifyDataSetChanged();

                    if (matchedUsers.isEmpty()) {
                        Toast.makeText(requireContext(), "Chưa có ai match với bạn!", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(requireContext(), "Không thể tải danh sách", Toast.LENGTH_SHORT).show();
                    Log.e(TAG, "Response code: " + response.code());
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<List<UserInfoResponse>>> call, Throwable t) {
                Toast.makeText(requireContext(), "Lỗi: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                Log.e(TAG, "API call failed: " + t.getMessage());
            }
        });
    }

    private void loadConversationSummaries() {
        SharedPreferences sharedPreferences = requireContext().getSharedPreferences("MyPrefs", requireContext().MODE_PRIVATE);
        String authToken = sharedPreferences.getString("authToken", null);

        if (authToken == null) {
            Toast.makeText(requireContext(), "Vui lòng đăng nhập để xem danh sách tin nhắn", Toast.LENGTH_SHORT).show();
            return;
        }

        AuthService authService = RetrofitClient.getClient().create(AuthService.class);
        Call<List<ConversationSummaryDTO>> call = authService.getConversationSummaries("Bearer " + authToken);

        call.enqueue(new Callback<List<ConversationSummaryDTO>>() {
            @Override
            public void onResponse(Call<List<ConversationSummaryDTO>> call, Response<List<ConversationSummaryDTO>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    conversationSummaries.clear();
                    conversationSummaries.addAll(response.body());
                    conversationAdapter.notifyDataSetChanged();

                    if (conversationSummaries.isEmpty()) {
                        Toast.makeText(requireContext(), "Chưa có tin nhắn nào!", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(requireContext(), "Không thể tải danh sách tin nhắn", Toast.LENGTH_SHORT).show();
                    Log.e(TAG, "Response code: " + response.code());
                }
            }

            @Override
            public void onFailure(Call<List<ConversationSummaryDTO>> call, Throwable t) {
                Toast.makeText(requireContext(), "Lỗi: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                Log.e(TAG, "API call failed: " + t.getMessage());
            }
        });
    }

    private void openChatDetailActivity(String userName, String userId, String userAvatar) {
        Intent intent = new Intent(requireContext(), ChatDetailActivity.class);
        intent.putExtra("userName", userName);
        intent.putExtra("userId", userId);
        intent.putExtra("userAvatar", userAvatar);
        startActivity(intent);
    }

    // Adapter for horizontal matched users (using item_chat_icon.xml)
    private class ChatAdapter extends RecyclerView.Adapter<ChatAdapter.ChatViewHolder> {
        private List<UserInfoResponse> users;

        ChatAdapter(List<UserInfoResponse> users) {
            this.users = users;
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

            Glide.with(requireContext())
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
                    openChatDetailActivity(fullName, userId, userAvatar);
                });
            }
        }
    }

    // Adapter for vertical conversation summaries (using item_chat.xml)
    private class ConversationAdapter extends RecyclerView.Adapter<ConversationAdapter.ConversationViewHolder> {
        private List<ConversationSummaryDTO> summaries;

        ConversationAdapter(List<ConversationSummaryDTO> summaries) {
            this.summaries = summaries;
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
            Glide.with(requireContext())
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

            // Set unread count
            if (summary.getUnreadCount() > 0) {
                holder.tvUnreadCount.setText(String.valueOf(summary.getUnreadCount()));
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
                    openChatDetailActivity(summary.getName(), summary.getUserId(), summary.getProfilePicture());
                });
            }
        }
    }
}