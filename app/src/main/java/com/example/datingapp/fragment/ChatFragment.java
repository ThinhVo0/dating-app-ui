package com.example.datingapp.fragment;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.datingapp.R;
import com.example.datingapp.dto.response.ApiResponse;
import com.example.datingapp.dto.response.UserInfoResponse;
import com.example.datingapp.network.AuthService;
import com.example.datingapp.network.RetrofitClient;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;

public class ChatFragment extends Fragment {

    private static final String TAG = "ChatFragment";
    private RecyclerView rvChatList;
    private ChatAdapter chatAdapter;
    private List<UserInfoResponse> matchedUsers;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_chat, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        rvChatList = view.findViewById(R.id.rvChatList);

        // Thiết lập RecyclerView cuộn ngang
        LinearLayoutManager layoutManager = new LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false);
        rvChatList.setLayoutManager(layoutManager);

        // Khởi tạo danh sách rỗng
        matchedUsers = new ArrayList<>();
        chatAdapter = new ChatAdapter(matchedUsers);
        rvChatList.setAdapter(chatAdapter);

        // Load dữ liệu từ API
        loadMatchedUsers();
    }

    private void loadMatchedUsers() {
        // Lấy token từ SharedPreferences
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
            public void onResponse(Call<ApiResponse<List<UserInfoResponse>>> call, retrofit2.Response<ApiResponse<List<UserInfoResponse>>> response) {
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

    // Adapter cho RecyclerView
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

            // Load ảnh đại diện bằng Glide
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
                    Bundle bundle = new Bundle();
                    bundle.putString("userName", user.getFirstName() + " " + user.getLastName());
                    bundle.putString("userId", user.getUserId());

                    NavController navController = Navigation.findNavController(v);
                    navController.navigate(R.id.action_chat_to_chat_detail, bundle);
                });
            }
        }
    }
}