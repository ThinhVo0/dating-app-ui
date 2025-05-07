package com.example.datingapp.activity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.PopupWindow;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.PopupMenu;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.datingapp.R;
import com.example.datingapp.adapter.EmojiAdapter;
import com.example.datingapp.adapter.MessageAdapter;
import com.example.datingapp.dto.ConversationSummaryDTO;
import com.example.datingapp.dto.MessageDTO;
import com.example.datingapp.dto.request.ReportDto;
import com.example.datingapp.dto.response.ApiResponse;
import com.example.datingapp.model.Report;
import com.example.datingapp.network.AuthService;
import com.example.datingapp.network.RetrofitClient;
import com.google.gson.Gson;

import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompFrameHandler;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ChatDetailActivity extends AppCompatActivity {

    private static final String TAG = "ChatDetailActivity";
    private RecyclerView rvMessages, rvEmojis;
    private MessageAdapter messageAdapter;
    private EmojiAdapter emojiAdapter;
    private List<MessageDTO> messageList;
    private List<String> emojiList;
    private EditText etMessageInput;
    private ImageButton btnSendMessage, btnBack, btnMenu, btnEmoji;
    private TextView tvChatUserName;
    private ImageView ivChatUserAvatar;
    private View emojiContainer;
    private boolean isEmojiShowing = false;
    private String userName;
    private String targetUserId;
    private String currentUserId;
    private String userAvatar;
    private WebSocketStompClient stompClient;
    private StompSession stompSession;
    private ExecutorService executorService;
    private ScheduledExecutorService reconnectExecutor;
    private boolean isConnecting = false;
    private static final int RECONNECT_DELAY_SECONDS = 5;
    private String authToken; // Thêm authToken để sử dụng trong báo cáo

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat_detail);

        executorService = Executors.newSingleThreadExecutor();
        reconnectExecutor = Executors.newScheduledThreadPool(1);

        // Lấy dữ liệu từ Intent
        Intent intent = getIntent();
        userName = intent.getStringExtra("userName");
        targetUserId = intent.getStringExtra("userId");
        userAvatar = intent.getStringExtra("userAvatar");

        // Lấy currentUserId và authToken từ SharedPreferences
        SharedPreferences prefs = getSharedPreferences("MyPrefs", MODE_PRIVATE);
        currentUserId = prefs.getString("userId", null);
        authToken = prefs.getString("authToken", null);

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
        btnMenu = findViewById(R.id.btnMenu);
        btnEmoji = findViewById(R.id.btnEmoji);
        emojiContainer = findViewById(R.id.emojiContainer);
        rvEmojis = findViewById(R.id.rvEmojis);
        tvChatUserName = findViewById(R.id.tvChatUserName);
        ivChatUserAvatar = findViewById(R.id.ivChatUserAvatar);
        tvChatUserName.setText(userName);

        // Load avatar
        Glide.with(this)
                .load(userAvatar != null && !userAvatar.isEmpty() ? userAvatar : "https://via.placeholder.com/150")
                .circleCrop()
                .into(ivChatUserAvatar);

        // Thiết lập RecyclerView tin nhắn
        messageList = new ArrayList<>();
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setStackFromEnd(true);
        rvMessages.setLayoutManager(layoutManager);
        messageAdapter = new MessageAdapter(messageList, currentUserId);
        rvMessages.setAdapter(messageAdapter);

        // Thiết lập RecyclerView emoji
        setupEmojiRecyclerView();

        // Đánh dấu tin nhắn đã đọc
        markMessagesAsRead(authToken);

        // Thiết lập WebSocket và load tin nhắn
        setupWebSocket(authToken);
        loadInitialMessages(authToken);

        // Sự kiện gửi tin nhắn
        btnSendMessage.setOnClickListener(v -> sendMessage());

        // Sự kiện nút back
        btnBack.setOnClickListener(v -> finish());

        // Sự kiện nút ba chấm
        btnMenu.setOnClickListener(v -> showPopupMenu(v));
        
        // Sự kiện hiển thị/ẩn emoji picker
        btnEmoji.setOnClickListener(v -> toggleEmojiPicker());
        
        // Ẩn emoji picker khi nhấn vào trường nhập liệu
        etMessageInput.setOnClickListener(v -> {
            if (isEmojiShowing) {
                toggleEmojiPicker();
            }
        });
    }
    
    private void setupEmojiRecyclerView() {
        // Tạo danh sách emoji
        emojiList = new ArrayList<>();
        // Thêm một số emoji phổ biến
        addCommonEmojis();
        
        // Thiết lập adapter và layout cho RecyclerView emoji
        GridLayoutManager gridLayoutManager = new GridLayoutManager(this, 5);
        rvEmojis.setLayoutManager(gridLayoutManager);
        emojiAdapter = new EmojiAdapter(emojiList, this::insertEmoji);
        rvEmojis.setAdapter(emojiAdapter);
    }
    
    private void addCommonEmojis() {
        // Thêm một số emoji phổ biến
        String[] commonEmojis = {
            "😀", "😁", "😂", "🤣", "😃", "😄", "😅", "😆", "😉", "😊", 
            "😋", "😎", "😍", "😘", "🥰", "😗", "😙", "😚", "☺️", "🙂",
            "🤗", "🤩", "🤔", "🤨", "😐", "😑", "😶", "🙄", "😏", "😣",
            "❤️", "💔", "💖", "💘", "💝", "💓", "💗", "💞", "❣️", "💕",
            "👍", "👎", "✌️", "👊", "✋", "🤚", "👋", "👏", "🙌", "🤝"
        };
        emojiList.addAll(Arrays.asList(commonEmojis));
    }
    
    private void toggleEmojiPicker() {
        isEmojiShowing = !isEmojiShowing;
        emojiContainer.setVisibility(isEmojiShowing ? View.VISIBLE : View.GONE);
        
        // Thay đổi icon của nút emoji
        if (isEmojiShowing) {
            btnEmoji.setImageResource(android.R.drawable.ic_menu_close_clear_cancel);
        } else {
            btnEmoji.setImageResource(android.R.drawable.ic_menu_gallery);
        }
    }
    
    private void insertEmoji(String emoji) {
        // Thêm emoji vào vị trí con trỏ
        int cursorPosition = etMessageInput.getSelectionStart();
        Editable editable = etMessageInput.getText();
        editable.insert(cursorPosition, emoji);
    }

    private void showPopupMenu(View v) {
        // Inflate the popup menu layout
        View popupView = getLayoutInflater().inflate(R.layout.popup_menu_custom, null);
        
        // Create the popup window
        int width = LinearLayout.LayoutParams.WRAP_CONTENT;
        int height = LinearLayout.LayoutParams.WRAP_CONTENT;
        final PopupWindow popupWindow = new PopupWindow(popupView, width, height, true);
        
        // Set animation style
        popupWindow.setAnimationStyle(android.R.style.Animation_Dialog);
        
        // Set elevation for Lollipop and above
        popupWindow.setElevation(20);
        
        // Set touch interceptor to dismiss the popup when clicking outside
        popupView.setOnTouchListener((v1, event) -> {
            popupWindow.dismiss();
            return true;
        });
        
        // Set click listeners for the menu items
        LinearLayout reportItem = popupView.findViewById(R.id.menuItemReport);
        reportItem.setOnClickListener(view -> {
            showReportDialog();
            popupWindow.dismiss();
        });
        
        LinearLayout blockItem = popupView.findViewById(R.id.menuItemBlock);
        blockItem.setOnClickListener(view -> {
            showBlockConfirmationDialog();
            popupWindow.dismiss();
        });
        
        // Show the popup window below the view or above it if there's not enough space below
        popupWindow.showAsDropDown(v, 0, 0);
    }
    
    private void showReportDialog() {
        // Inflate custom layout for report dialog
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_report_user, null);
        
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Báo cáo người dùng");
        builder.setView(dialogView);
        
        // Initialize views
        RadioGroup reportReasonGroup = dialogView.findViewById(R.id.reportReasonGroup);
        EditText customReasonInput = dialogView.findViewById(R.id.customReasonInput);
        RadioButton customReasonRadio = dialogView.findViewById(R.id.radioCustomReason);
        
        // Handle showing/hiding the custom reason input field
        reportReasonGroup.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.radioCustomReason) {
                customReasonInput.setVisibility(View.VISIBLE);
                customReasonInput.requestFocus();
            } else {
                customReasonInput.setVisibility(View.GONE);
            }
        });
        
        // Create and show dialog
        AlertDialog alertDialog = builder.create();
        
        // Set up buttons
        dialogView.findViewById(R.id.btnCancel).setOnClickListener(v -> alertDialog.dismiss());
        
        dialogView.findViewById(R.id.btnSubmit).setOnClickListener(v -> {
            String reason;
            int selectedId = reportReasonGroup.getCheckedRadioButtonId();
            
            if (selectedId == -1) {
                Toast.makeText(this, "Vui lòng chọn lý do báo cáo", Toast.LENGTH_SHORT).show();
                return;
            }
            
            if (selectedId == R.id.radioCustomReason) {
                reason = customReasonInput.getText().toString().trim();
                if (reason.isEmpty()) {
                    Toast.makeText(this, "Vui lòng nhập lý do báo cáo", Toast.LENGTH_SHORT).show();
                    return;
                }
            } else {
                RadioButton selectedRadioButton = dialogView.findViewById(selectedId);
                reason = selectedRadioButton.getText().toString();
            }
            
            // Show confirmation before submitting
            new AlertDialog.Builder(this)
                .setTitle("Xác nhận báo cáo")
                .setMessage("Bạn chắc chắn muốn báo cáo người dùng này với lý do: \"" + reason + "\"?")
                .setPositiveButton("Xác nhận", (dialog, which) -> submitReport(reason, alertDialog))
                .setNegativeButton("Hủy", null)
                .show();
        });
        
        alertDialog.show();
    }
    
    private void submitReport(String reason, AlertDialog parentDialog) {
        // Show loading progress
        View loadingView = getLayoutInflater().inflate(R.layout.dialog_loading, null);
        AlertDialog loadingDialog = new AlertDialog.Builder(this)
            .setView(loadingView)
            .setCancelable(false)
            .create();
        loadingDialog.show();
        
        // Call API to report user
        AuthService authService = RetrofitClient.getClient().create(AuthService.class);
        ReportDto reportDto = new ReportDto(targetUserId, reason);
        Log.d(TAG, "Sending report: " + new Gson().toJson(reportDto));
        Call<ApiResponse<Report>> call = authService.sendReport("Bearer " + authToken, reportDto);

        call.enqueue(new Callback<ApiResponse<Report>>() {
            @Override
            public void onResponse(Call<ApiResponse<Report>> call, Response<ApiResponse<Report>> response) {
                loadingDialog.dismiss();
                parentDialog.dismiss();
                
                Log.d(TAG, "HTTP Status Code (sendReport): " + response.code());
                if (response.isSuccessful() && response.body() != null && response.body().getStatus() == 200) {
                    String message = response.body().getMessage() != null ? response.body().getMessage() : "Báo cáo thành công";
                    Log.d(TAG, "Report successful: " + message);
                    
                    // Show success dialog
                    new AlertDialog.Builder(ChatDetailActivity.this)
                        .setTitle("Báo cáo thành công")
                        .setMessage("Cảm ơn bạn đã báo cáo. Chúng tôi sẽ xem xét nhanh chóng.")
                        .setPositiveButton("Đóng", (dialog, which) -> finish())
                        .setCancelable(false)
                        .show();
                } else {
                    if (response.code() == 401) {
                        Toast.makeText(ChatDetailActivity.this, "Phiên đăng nhập hết hạn, vui lòng đăng nhập lại", Toast.LENGTH_LONG).show();
                        return;
                    }
                    String errorMessage = "Gửi báo cáo thất bại";
                    if (response.errorBody() != null) {
                        try {
                            errorMessage = response.errorBody().string();
                            Log.e(TAG, "Error response: " + errorMessage);
                        } catch (Exception e) {
                            Log.e(TAG, "Error parsing errorBody: " + e.getMessage(), e);
                        }
                    } else if (response.body() != null) {
                        Log.e(TAG, "Response body: " + new Gson().toJson(response.body()));
                        errorMessage = "Status: " + response.body().getStatus() + ", Message: " + response.body().getMessage();
                    }
                    
                    // Show error dialog
                    new AlertDialog.Builder(ChatDetailActivity.this)
                        .setTitle("Lỗi")
                        .setMessage("Không thể gửi báo cáo: " + errorMessage)
                        .setPositiveButton("Đóng", null)
                        .show();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<Report>> call, Throwable t) {
                loadingDialog.dismiss();
                parentDialog.dismiss();
                
                Log.e(TAG, "Report API call failed: " + t.getMessage(), t);
                
                // Show error dialog
                new AlertDialog.Builder(ChatDetailActivity.this)
                    .setTitle("Lỗi kết nối")
                    .setMessage("Không thể gửi báo cáo: " + t.getMessage())
                    .setPositiveButton("Thử lại", (dialog, which) -> showReportDialog())
                    .setNegativeButton("Hủy", null)
                    .show();
            }
        });
    }

    private void showBlockConfirmationDialog() {
        new AlertDialog.Builder(this)
            .setTitle("Chặn người dùng")
            .setMessage("Bạn có chắc chắn muốn chặn người dùng này? Họ sẽ không thể nhắn tin cho bạn nữa.")
            .setPositiveButton("Chặn", (dialog, which) -> {
                // TODO: Implement block user API call
                Toast.makeText(this, "Đã chặn người dùng", Toast.LENGTH_SHORT).show();
                finish();
            })
            .setNegativeButton("Hủy", null)
            .show();
    }

    private void markMessagesAsRead(String authToken) {
        AuthService authService = RetrofitClient.getClient().create(AuthService.class);
        Call<Void> call = authService.markMessagesAsRead("Bearer " + authToken, targetUserId);

        call.enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    Log.d(TAG, "Messages marked as read for user: " + targetUserId);
                    // Tải lại số tin nhắn chưa đọc từ API
                    Call<List<ConversationSummaryDTO>> summaryCall = authService.getConversationSummaries("Bearer " + authToken);
                    summaryCall.enqueue(new Callback<List<ConversationSummaryDTO>>() {
                        @Override
                        public void onResponse(Call<List<ConversationSummaryDTO>> call, Response<List<ConversationSummaryDTO>> response) {
                            if (response.isSuccessful() && response.body() != null) {
                                int totalUnreadMessages = 0;
                                for (ConversationSummaryDTO summary : response.body()) {
                                    totalUnreadMessages += summary.getUnreadCount();
                                }
                                // Lưu số tin nhắn chưa đọc vào SharedPreferences
                                SharedPreferences prefs = getSharedPreferences("MyPrefs", MODE_PRIVATE);
                                SharedPreferences.Editor editor = prefs.edit();
                                editor.putInt("unreadMessageCount", totalUnreadMessages);
                                editor.apply();
                                Log.i(TAG, "Updated unread message count: " + totalUnreadMessages);

                                // Gửi broadcast để cập nhật badge
                                Intent badgeIntent = new Intent("UPDATE_CHAT_BADGE");
                                badgeIntent.putExtra("unreadMessageCount", totalUnreadMessages);
                                LocalBroadcastManager.getInstance(ChatDetailActivity.this).sendBroadcast(badgeIntent);

                                // Gửi broadcast để làm mới danh sách conversationSummaries
                                Intent refreshIntent = new Intent("MESSAGES_READ");
                                LocalBroadcastManager.getInstance(ChatDetailActivity.this).sendBroadcast(refreshIntent);
                            } else {
                                Log.e(TAG, "Failed to load conversation summaries: " + response.code());
                                Toast.makeText(ChatDetailActivity.this, "Không thể cập nhật số tin nhắn chưa đọc", Toast.LENGTH_SHORT).show();
                            }
                        }

                        @Override
                        public void onFailure(Call<List<ConversationSummaryDTO>> call, Throwable t) {
                            Log.e(TAG, "Failed to load conversation summaries: " + t.getMessage(), t);
                            Toast.makeText(ChatDetailActivity.this, "Lỗi: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
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
        stompClient = new WebSocketStompClient(new StandardWebSocketClient());
        stompClient.setMessageConverter(new MappingJackson2MessageConverter());

        String wsUrl = getString(R.string.websocket_url);
        Log.d(TAG, "Connecting to WebSocket URL: " + wsUrl);

        if (wsUrl == null || wsUrl.isEmpty()) {
            Log.e(TAG, "WebSocket URL is null or empty");
            Toast.makeText(this, "URL WebSocket không được cấu hình", Toast.LENGTH_LONG).show();
            return;
        }

        connectWebSocket(authToken, wsUrl);
    }

    private void connectWebSocket(String authToken, String wsUrl) {
        if (isConnecting || (stompSession != null && stompSession.isConnected())) {
            return;
        }
        isConnecting = true;

        executorService.execute(() -> {
            try {
                StompHeaders connectHeaders = new StompHeaders();
                Log.d(TAG, "Connecting with headers: " + connectHeaders);

                StompSession session = stompClient.connect(wsUrl, new StompSessionHandlerAdapter() {
                    @Override
                    public void afterConnected(StompSession session, StompHeaders connectedHeaders) {
                        Log.i(TAG, "Connected to WebSocket with headers: " + connectedHeaders);
                        stompSession = session;
                        isConnecting = false;

                        String subscriptionTopic = "/topic/messages";
                        Log.d(TAG, "Subscribing to topic: " + subscriptionTopic);
                        session.subscribe(subscriptionTopic, new StompFrameHandler() {
                            @Override
                            public Type getPayloadType(StompHeaders headers) {
                                return MessageDTO.class;
                            }

                            @Override
                            public void handleFrame(StompHeaders headers, Object payload) {
                                Log.i(TAG, "Received headers: " + headers);
                                Log.i(TAG, "Raw payload: " + (payload != null ? payload.toString() : "null"));
                                try {
                                    if (payload instanceof MessageDTO) {
                                        MessageDTO message = (MessageDTO) payload;
                                        Log.i(TAG, "Received message: id=" + message.getId() +
                                                ", content=" + message.getContent());

                                        // Lọc tin nhắn cho cuộc trò chuyện hiện tại
                                        if (isMessageForCurrentConversation(message)) {
                                            if (!isMessageDuplicate(message)) {
                                                runOnUiThread(() -> {
                                                    messageList.add(message);
                                                    messageAdapter.notifyItemInserted(messageList.size() - 1);
                                                    rvMessages.scrollToPosition(messageList.size() - 1);
                                                });
                                            } else {
                                                Log.i(TAG, "Duplicate message ignored: " + message.getContent());
                                            }
                                        }
                                    } else {
                                        Log.e(TAG, "Invalid payload type: " + (payload != null ? payload.getClass().getName() : "null"));
                                    }
                                } catch (Exception e) {
                                    Log.e(TAG, "Error processing message: " + e.getMessage(), e);
                                }
                            }
                        });
                    }

                    @Override
                    public void handleException(StompSession session, StompCommand command, StompHeaders headers, byte[] payload, Throwable exception) {
                        Log.e(TAG, "STOMP exception: " + exception.getMessage(), exception);
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
                scheduleReconnect(authToken, wsUrl);
            } finally {
                isConnecting = false;
            }
        });
    }

    private boolean isMessageForCurrentConversation(MessageDTO message) {
        return (message.getSenderId().equals(currentUserId) && message.getReceiverId().equals(targetUserId)) ||
                (message.getSenderId().equals(targetUserId) && message.getReceiverId().equals(currentUserId));
    }

    private void scheduleReconnect(String authToken, String wsUrl) {
        if (!isConnecting) {
            Log.i(TAG, "Scheduling WebSocket reconnect in " + RECONNECT_DELAY_SECONDS + " seconds");
            reconnectExecutor.schedule(() -> connectWebSocket(authToken, wsUrl), RECONNECT_DELAY_SECONDS, TimeUnit.SECONDS);
        }
    }

    private boolean isMessageDuplicate(MessageDTO message) {
        if (message.getId() != null) {
            return messageList.stream().anyMatch(m -> m.getId() != null && m.getId().equals(message.getId()));
        }
        return messageList.stream().anyMatch(m ->
                m.getContent() != null &&
                        m.getContent().equals(message.getContent()) &&
                        m.getSenderId() != null &&
                        m.getSenderId().equals(message.getSenderId())
        );
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
            messageDTO.setSendTime(new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss").format(new java.util.Date()));

            etMessageInput.setText("");

            executorService.execute(() -> {
                try {
                    stompSession.send("/app/chat", messageDTO);
                    Log.i(TAG, "Message sent: " + messageText);
                } catch (Exception e) {
                    Log.e(TAG, "Failed to send message: " + e.getMessage(), e);
                    runOnUiThread(() ->
                            Toast.makeText(ChatDetailActivity.this, "Lỗi gửi tin nhắn: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                    );
                }
            });
        } else {
            Toast.makeText(this, "Không thể gửi tin nhắn. Kiểm tra kết nối hoặc nội dung.", Toast.LENGTH_SHORT).show();
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
        if (reconnectExecutor != null && !reconnectExecutor.isShutdown()) {
            reconnectExecutor.shutdown();
        }
    }
}