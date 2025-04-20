package com.example.datingapp.activity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.datingapp.R;
import com.example.datingapp.adapter.MessageAdapter;
import com.example.datingapp.dto.MessageDTO;
import com.example.datingapp.network.AuthService;
import com.example.datingapp.network.RetrofitClient;

import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompFrameHandler;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;
import org.springframework.web.socket.sockjs.client.SockJsClient;
import org.springframework.web.socket.sockjs.client.Transport;
import org.springframework.web.socket.sockjs.client.WebSocketTransport;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ChatDetailActivity extends AppCompatActivity {

    private static final String TAG = "ChatDetailActivity";
    private RecyclerView rvMessages;
    private MessageAdapter messageAdapter;
    private List<MessageDTO> messageList;
    private EditText etMessageInput;
    private ImageButton btnSendMessage, btnBack;
    private TextView tvChatUserName;
    private ImageView ivChatUserAvatar;
    private String userName;
    private String targetUserId;
    private String currentUserId;
    private String userAvatar;
    private WebSocketStompClient stompClient;
    private StompSession stompSession;
    private ExecutorService executorService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat_detail);

        executorService = Executors.newSingleThreadExecutor();

        // Lấy dữ liệu từ Intent
        Intent intent = getIntent();
        userName = intent.getStringExtra("userName");
        targetUserId = intent.getStringExtra("userId");
        userAvatar = intent.getStringExtra("userAvatar");

        // Lấy currentUserId và authToken từ SharedPreferences
        SharedPreferences prefs = getSharedPreferences("MyPrefs", MODE_PRIVATE);
        currentUserId = prefs.getString("userId", null);
        String authToken = prefs.getString("authToken", null);

        if (currentUserId == null || authToken == null || targetUserId == null) {
            Toast.makeText(this, "Vui lòng đăng nhập lại", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Khởi tạo view
        rvMessages = findViewById(R.id.rvMessages);
        etMessageInput = findViewById(R.id.etMessageInput);
        btnSendMessage = findViewById(R.id.btnSendMessage);
        btnBack = findViewById(R.id.btnBack);
        tvChatUserName = findViewById(R.id.tvChatUserName);
        ivChatUserAvatar = findViewById(R.id.ivChatUserAvatar);
        tvChatUserName.setText(userName);

        // Load avatar
        Glide.with(this)
                .load(userAvatar != null && !userAvatar.isEmpty() ? userAvatar : "https://via.placeholder.com/150")
                .circleCrop()
                .into(ivChatUserAvatar);

        // Thiết lập RecyclerView
        messageList = new ArrayList<>();
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setStackFromEnd(true);
        rvMessages.setLayoutManager(layoutManager);
        messageAdapter = new MessageAdapter(messageList, currentUserId);
        rvMessages.setAdapter(messageAdapter);

        // Đánh dấu tin nhắn đã đọc
        markMessagesAsRead(authToken);

        // Thiết lập WebSocket và load tin nhắn
        setupWebSocket(authToken);
        loadInitialMessages(authToken);

        // Sự kiện gửi tin nhắn
        btnSendMessage.setOnClickListener(v -> sendMessage());

        // Sự kiện nút back
        btnBack.setOnClickListener(v -> finish());
    }

    private void markMessagesAsRead(String authToken) {
        AuthService authService = RetrofitClient.getClient().create(AuthService.class);
        Call<Void> call = authService.markMessagesAsRead("Bearer " + authToken, targetUserId);

        call.enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    Log.d(TAG, "Messages marked as read for user: " + targetUserId);
                } else {
                    Log.e(TAG, "Failed to mark messages as read: " + response.code());
                    Toast.makeText(ChatDetailActivity.this, "Không thể đánh dấu tin nhắn đã đọc", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                Log.e(TAG, "Mark messages as read failed: " + t.getMessage(), t);
                Toast.makeText(ChatDetailActivity.this, "Lỗi: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setupWebSocket(String authToken) {
        List<Transport> transports = new ArrayList<>();
        transports.add(new WebSocketTransport(new StandardWebSocketClient()));
        SockJsClient sockJsClient = new SockJsClient(transports);
        stompClient = new WebSocketStompClient(sockJsClient);
        stompClient.setMessageConverter(new MappingJackson2MessageConverter());

        String wsUrl = "http://10.0.2.2:8080/ws"; // SockJS URL

        executorService.execute(() -> {
            try {
                StompHeaders connectHeaders = new StompHeaders();
                connectHeaders.add("Authorization", "Bearer " + authToken);

                StompSession session = stompClient.connect(wsUrl, new StompSessionHandlerAdapter() {
                    @Override
                    public void afterConnected(StompSession session, StompHeaders connectedHeaders) {
                        Log.i(TAG, "Connected to WebSocket");
                        stompSession = session;
                        session.subscribe("/user/queue/messages", new StompFrameHandler() {
                            @Override
                            public Type getPayloadType(StompHeaders headers) {
                                return MessageDTO.class;
                            }

                            @Override
                            public void handleFrame(StompHeaders headers, Object payload) {
                                MessageDTO message = (MessageDTO) payload;
                                Log.i(TAG, "Received message: " + message.toString());
                                if (!messageList.stream().anyMatch(m -> m.getId() != null && m.getId().equals(message.getId()))) {
                                    runOnUiThread(() -> {
                                        messageList.add(message);
                                        messageAdapter.notifyItemInserted(messageList.size() - 1);
                                        rvMessages.scrollToPosition(messageList.size() - 1);
                                        Log.i(TAG, "Updated UI with message: " + message.getContent());
                                    });
                                } else {
                                    Log.i(TAG, "Message already exists: " + message.getId());
                                }
                            }
                        });
                    }

                    @Override
                    public void handleException(StompSession session, StompCommand command, StompHeaders headers, byte[] payload, Throwable exception) {
                        Log.e(TAG, "WebSocket error: " + exception.getMessage(), exception);
                    }

                    @Override
                    public void handleTransportError(StompSession session, Throwable exception) {
                        Log.e(TAG, "Transport error: " + exception.getMessage(), exception);
                    }
                }, connectHeaders).get();
            } catch (Exception e) {
                Log.e(TAG, "Failed to connect WebSocket: " + e.getMessage(), e);
                runOnUiThread(() ->
                        Toast.makeText(ChatDetailActivity.this, "Không thể kết nối WebSocket: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                );
            }
        });
    }

    private void loadInitialMessages(String authToken) {
        AuthService authService = RetrofitClient.getClient().create(AuthService.class);
        Call<List<MessageDTO>> call = authService.getConversation("Bearer " + authToken, currentUserId, targetUserId);

        call.enqueue(new Callback<List<MessageDTO>>() {
            @Override
            public void onResponse(Call<List<MessageDTO>> call, Response<List<MessageDTO>> response) {
                Log.d(TAG, "loadInitialMessages response code: " + response.code());
                if (response.isSuccessful() && response.body() != null) {
                    Log.d(TAG, "Messages loaded: " + response.body().size());
                    messageList.clear();
                    messageList.addAll(response.body());
                    messageAdapter.notifyDataSetChanged();
                    rvMessages.scrollToPosition(messageList.size() - 1);
                } else {
                    Log.e(TAG, "Failed to load messages: " + response.code() + ", body: " + (response.errorBody() != null ? response.errorBody().toString() : "null"));
                    Toast.makeText(ChatDetailActivity.this, "Không thể tải tin nhắn: " + response.code(), Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<List<MessageDTO>> call, Throwable t) {
                Log.e(TAG, "Load messages failed: " + t.getMessage(), t);
                Toast.makeText(ChatDetailActivity.this, "Lỗi: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void sendMessage() {
        String messageText = etMessageInput.getText().toString().trim();
        if (!messageText.isEmpty() && stompSession != null && stompSession.isConnected()) {
            MessageDTO messageDTO = new MessageDTO();
            messageDTO.setSenderId(currentUserId);
            messageDTO.setReceiverId(targetUserId);
            messageDTO.setContent(messageText);
            messageDTO.setSendTime(new java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault()).format(new java.util.Date()));

            // Thêm tin nhắn vào danh sách ngay lập tức
            messageList.add(messageDTO);
            messageAdapter.notifyItemInserted(messageList.size() - 1);
            rvMessages.scrollToPosition(messageList.size() - 1);
            etMessageInput.setText("");

            executorService.execute(() -> {
                try {
                    stompSession.send("/app/chat", messageDTO);
                    Log.i(TAG, "Message sent: " + messageText);
                } catch (Exception e) {
                    Log.e(TAG, "Failed to send message: " + e.getMessage(), e);
                    runOnUiThread(() ->
                            Toast.makeText(ChatDetailActivity.this, "Lỗi gửi tin nhắn", Toast.LENGTH_SHORT).show()
                    );
                }
            });
        } else {
            Toast.makeText(this, "Không thể gửi tin nhắn", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (stompClient != null && stompClient.isRunning()) {
            stompClient.stop();
        }
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
        }
    }
}