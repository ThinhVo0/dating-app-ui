package com.example.datingapp.fragment;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.fragment.app.Fragment;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.datingapp.R;
import com.example.datingapp.activity.ChatDetailActivity;
import com.example.datingapp.adapter.ChatAdapter;
import com.example.datingapp.adapter.ConversationAdapter;
import com.example.datingapp.dto.ConversationSummaryDTO;
import com.example.datingapp.dto.response.ApiResponse;
import com.example.datingapp.dto.response.UserInfoResponse;
import com.example.datingapp.network.AuthService;
import com.example.datingapp.network.RetrofitClient;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

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
    private String currentUserId;
    private BroadcastReceiver messageReceiver;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_chat, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Get currentUserId and authToken
        SharedPreferences prefs = requireContext().getSharedPreferences("MyPrefs", requireContext().MODE_PRIVATE);
        currentUserId = prefs.getString("userId", null);
        String authToken = prefs.getString("authToken", null);

        if (currentUserId == null || authToken == null) {
            Toast.makeText(requireContext(), "Vui lòng đăng nhập lại", Toast.LENGTH_SHORT).show();
            return;
        }

        // Initialize horizontal RecyclerView for matched users
        rvChatList = view.findViewById(R.id.rvChatList);
        LinearLayoutManager chatLayoutManager = new LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false);
        rvChatList.setLayoutManager(chatLayoutManager);
        matchedUsers = new ArrayList<>();
        chatAdapter = new ChatAdapter(matchedUsers, this::openChatDetailActivity);
        rvChatList.setAdapter(chatAdapter);

        // Initialize vertical RecyclerView for conversation summaries
        rvConversationList = view.findViewById(R.id.rvConversationList);
        LinearLayoutManager conversationLayoutManager = new LinearLayoutManager(requireContext());
        rvConversationList.setLayoutManager(conversationLayoutManager);
        conversationSummaries = new ArrayList<>();
        conversationAdapter = new ConversationAdapter(conversationSummaries, this::openChatDetailActivity);
        rvConversationList.setAdapter(conversationAdapter);

        // Load initial data
        loadMatchedUsers(authToken);
        loadConversationSummaries(authToken);

        // Đăng ký BroadcastReceiver để nhận tin nhắn mới và tin nhắn đã đọc
        messageReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if ("NEW_MESSAGE".equals(intent.getAction()) || "MESSAGES_READ".equals(intent.getAction())) {
                    Log.i(TAG, "Received broadcast: " + intent.getAction());
                    loadConversationSummaries(authToken);
                }
            }
        };
        IntentFilter filter = new IntentFilter();
        filter.addAction("NEW_MESSAGE");
        filter.addAction("MESSAGES_READ");
        LocalBroadcastManager.getInstance(requireContext()).registerReceiver(messageReceiver, filter);
    }

    @Override
    public void onResume() {
        super.onResume();
        // Refresh conversation summaries when returning to the fragment
        SharedPreferences prefs = requireContext().getSharedPreferences("MyPrefs", requireContext().MODE_PRIVATE);
        String authToken = prefs.getString("authToken", null);
        if (authToken != null) {
            loadConversationSummaries(authToken);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // Hủy đăng ký BroadcastReceiver
        if (messageReceiver != null) {
            LocalBroadcastManager.getInstance(requireContext()).unregisterReceiver(messageReceiver);
        }
    }

    private void loadMatchedUsers(String authToken) {
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

    private void loadConversationSummaries(String authToken) {
        AuthService authService = RetrofitClient.getClient().create(AuthService.class);
        Call<List<ConversationSummaryDTO>> call = authService.getConversationSummaries("Bearer " + authToken);

        call.enqueue(new Callback<List<ConversationSummaryDTO>>() {
            @Override
            public void onResponse(Call<List<ConversationSummaryDTO>> call, Response<List<ConversationSummaryDTO>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    conversationSummaries.clear();
                    conversationSummaries.addAll(response.body());

                    // Sắp xếp conversationSummaries theo latestMessageTime (giảm dần)
                    Collections.sort(conversationSummaries, (c1, c2) -> {
                        try {
                            // Parse latestMessageTime thành LocalDateTime
                            LocalDateTime t1 = c1.getLatestMessageTime() != null
                                    ? LocalDateTime.parse(c1.getLatestMessageTime())
                                    : LocalDateTime.MIN;
                            LocalDateTime t2 = c2.getLatestMessageTime() != null
                                    ? LocalDateTime.parse(c2.getLatestMessageTime())
                                    : LocalDateTime.MIN;
                            return t2.compareTo(t1); // Giảm dần: t2 trước t1 nếu t2 mới hơn
                        } catch (Exception e) {
                            Log.e(TAG, "Error parsing latestMessageTime: " + e.getMessage());
                            return 0; // Giữ nguyên thứ tự nếu có lỗi
                        }
                    });

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
}