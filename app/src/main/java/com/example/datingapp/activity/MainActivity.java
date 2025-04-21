package com.example.datingapp.activity;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.example.datingapp.R;
import com.example.datingapp.dto.ConversationSummaryDTO;
import com.example.datingapp.dto.MessageDTO;
import com.example.datingapp.dto.Notification;
import com.example.datingapp.dto.response.ApiResponse;
import com.example.datingapp.dto.response.ProfileResponse;
import com.example.datingapp.fragment.ChatFragment;
import com.example.datingapp.fragment.LikeYouFragment;
import com.example.datingapp.fragment.NotificationsFragment;
import com.example.datingapp.fragment.ProfileFragment;
import com.example.datingapp.fragment.SettingsFragment;
import com.example.datingapp.network.AuthService;
import com.example.datingapp.network.RetrofitClient;
import com.google.android.material.badge.BadgeDrawable;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompFrameHandler;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;

import java.lang.reflect.Type;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private AuthService authService;
    private WebSocketStompClient stompClient;
    private StompSession stompSession;
    private ExecutorService executorService;
    private ScheduledExecutorService reconnectExecutor;
    private boolean isConnecting = false;
    private static final int RECONNECT_DELAY_SECONDS = 5;
    private String currentUserId;
    private String authToken;
    private BroadcastReceiver badgeUpdateReceiver;
    private BroadcastReceiver fetchProfileReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }
        setContentView(R.layout.activity_main);

        // Initialize Retrofit service
        authService = RetrofitClient.getClient().create(AuthService.class);

        // Lấy userId và authToken
        SharedPreferences prefs = getSharedPreferences("MyPrefs", MODE_PRIVATE);
        currentUserId = prefs.getString("userId", null);
        authToken = prefs.getString("authToken", null);

        if (currentUserId == null || authToken == null) {
            Toast.makeText(this, "Vui lòng đăng nhập lại", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        // Thiết lập WebSocket
        executorService = Executors.newSingleThreadExecutor();
        reconnectExecutor = Executors.newScheduledThreadPool(1);
        setupWebSocket(authToken);

        // Thiết lập BottomNavigationView
        BottomNavigationView bottomNavigationView = findViewById(R.id.menu_navigation);
        bottomNavigationView.setOnItemSelectedListener(item -> {
            Fragment selectedFragment = null;
            int itemId = item.getItemId();
            if (itemId == R.id.nav_liked) {
                selectedFragment = new LikeYouFragment();
            } else if (itemId == R.id.nav_chat) {
                selectedFragment = new ChatFragment();
            } else if (itemId == R.id.nav_profile) {
                selectedFragment = new ProfileFragment();
            } else if (itemId == R.id.nav_notify) {
                selectedFragment = new NotificationsFragment();
            } else if (itemId == R.id.nav_setting) {
                selectedFragment = new SettingsFragment();
            }

            if (selectedFragment != null) {
                openFragment(selectedFragment);
            }
            return true;
        });

        // Mở fragment mặc định
        if (savedInstanceState == null) {
            openFragment(new ProfileFragment());
        }

        // Xử lý click vào filter_icon
        ImageView filterIcon = findViewById(R.id.filter_icon);
        filterIcon.setOnClickListener(v -> {
            startActivity(new Intent(MainActivity.this, FilterActivity.class));
        });

        // Xử lý click vào FloatingActionButton
        FloatingActionButton fab = findViewById(R.id.fab_center);
        fab.setOnClickListener(v -> {
            openFragment(new ProfileFragment());
            Toast.makeText(MainActivity.this, "Chuyển đến trang hồ sơ!", Toast.LENGTH_SHORT).show();
        });

        // Lấy thông tin hồ sơ người dùng
        fetchUserProfile();

        // Cập nhật huy hiệu ban đầu
        int unreadNotificationCount = prefs.getInt("unreadNotificationCount", 0);
        updateNotificationBadge(unreadNotificationCount);

        // Tải số tin nhắn chưa đọc ban đầu
        loadInitialUnreadMessageCount(authToken);

        // Đăng ký BroadcastReceiver để nhận cập nhật badge
        badgeUpdateReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if ("UPDATE_CHAT_BADGE".equals(intent.getAction())) {
                    int unreadMessageCount = intent.getIntExtra("unreadMessageCount", 0);
                    updateChatBadge(unreadMessageCount);
                    Log.i(TAG, "Received badge update broadcast: " + unreadMessageCount);
                }
            }
        };
        LocalBroadcastManager.getInstance(this).registerReceiver(
                badgeUpdateReceiver, new IntentFilter("UPDATE_CHAT_BADGE"));

        // Đăng ký BroadcastReceiver để nhận yêu cầu fetch profile
        fetchProfileReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if ("FETCH_USER_PROFILE".equals(intent.getAction())) {
                    Log.d(TAG, "Received fetch profile broadcast");
                    fetchUserProfile();
                }
            }
        };
        LocalBroadcastManager.getInstance(this).registerReceiver(
                fetchProfileReceiver, new IntentFilter("FETCH_USER_PROFILE"));
    }

    private void loadInitialUnreadMessageCount(String authToken) {
        AuthService authService = RetrofitClient.getClient().create(AuthService.class);
        Call<List<ConversationSummaryDTO>> call = authService.getConversationSummaries("Bearer " + authToken);

        call.enqueue(new Callback<List<ConversationSummaryDTO>>() {
            @Override
            public void onResponse(Call<List<ConversationSummaryDTO>> call, Response<List<ConversationSummaryDTO>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    int totalUnreadMessages = 0;
                    for (ConversationSummaryDTO summary : response.body()) {
                        totalUnreadMessages += summary.getUnreadCount();
                    }
                    SharedPreferences prefs = getSharedPreferences("MyPrefs", MODE_PRIVATE);
                    SharedPreferences.Editor editor = prefs.edit();
                    editor.putInt("unreadMessageCount", totalUnreadMessages);
                    editor.apply();
                    updateChatBadge(totalUnreadMessages);
                    Log.i(TAG, "Initial unread message count: " + totalUnreadMessages);
                } else {
                    Log.e(TAG, "Failed to load conversation summaries: " + response.code());
                    Toast.makeText(MainActivity.this, "Không thể tải số tin nhắn chưa đọc", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<List<ConversationSummaryDTO>> call, Throwable t) {
                Log.e(TAG, "Load conversation summaries failed: " + t.getMessage(), t);
                Toast.makeText(MainActivity.this, "Lỗi: " + t.getMessage(), Toast.LENGTH_SHORT).show();
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
                StompSession session = stompClient.connect(wsUrl, new StompSessionHandlerAdapter() {
                    @Override
                    public void afterConnected(StompSession session, StompHeaders connectedHeaders) {
                        Log.i(TAG, "Connected to WebSocket with session ID: " + session.getSessionId());
                        stompSession = session;
                        isConnecting = false;

                        String notificationTopic = "/topic/notifications";
                        Log.d(TAG, "Subscribing to topic: " + notificationTopic);
                        session.subscribe(notificationTopic, new StompFrameHandler() {
                            @Override
                            public Type getPayloadType(StompHeaders headers) {
                                return Notification.class;
                            }

                            @Override
                            public void handleFrame(StompHeaders headers, Object payload) {
                                if (payload instanceof Notification) {
                                    Notification notification = (Notification) payload;
                                    Log.i(TAG, "Received notification: " + notification.getContent());

                                    if (notification.getUserId() != null &&
                                            notification.getUserId().equals(currentUserId)) {
                                        runOnUiThread(() -> {
                                            SharedPreferences prefs = getSharedPreferences("MyPrefs", MODE_PRIVATE);
                                            int unreadCount = prefs.getInt("unreadNotificationCount", 0) + 1;
                                            SharedPreferences.Editor editor = prefs.edit();
                                            editor.putInt("unreadNotificationCount", unreadCount);
                                            editor.apply();
                                            updateNotificationBadge(unreadCount);
                                            Log.i(TAG, "Updated badge with unread count: " + unreadCount);
                                        });
                                    }
                                } else {
                                    Log.e(TAG, "Invalid payload type: " + (payload != null ? payload.getClass().getName() : "null"));
                                }
                            }
                        });

                        String messageTopic = "/topic/messages";
                        Log.d(TAG, "Subscribing to topic: " + messageTopic);
                        session.subscribe(messageTopic, new StompFrameHandler() {
                            @Override
                            public Type getPayloadType(StompHeaders headers) {
                                return MessageDTO.class;
                            }

                            @Override
                            public void handleFrame(StompHeaders headers, Object payload) {
                                if (payload instanceof MessageDTO) {
                                    MessageDTO message = (MessageDTO) payload;
                                    Log.i(TAG, "Received message: " + message.getContent());

                                    if (message.getReceiverId().equals(currentUserId) || message.getSenderId().equals(currentUserId)) {
                                        runOnUiThread(() -> {
                                            // Tải lại số tin nhắn chưa đọc từ API
                                            loadInitialUnreadMessageCount(authToken);
                                            // Gửi broadcast để thông báo tin nhắn mới
                                            Intent intent = new Intent("NEW_MESSAGE");
                                            LocalBroadcastManager.getInstance(MainActivity.this).sendBroadcast(intent);
                                        });
                                    }
                                } else {
                                    Log.e(TAG, "Invalid payload type: " + (payload != null ? payload.getClass().getName() : "null"));
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
                        scheduleReconnect(authToken, wsUrl);
                    }
                }, connectHeaders).get();

            } catch (Exception e) {
                Log.e(TAG, "Failed to connect WebSocket: " + e.getMessage(), e);
                runOnUiThread(() ->
                        Toast.makeText(MainActivity.this, "Không thể kết nối WebSocket: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                );
                scheduleReconnect(authToken, wsUrl);
            } finally {
                isConnecting = false;
            }
        });
    }

    private void scheduleReconnect(String authToken, String wsUrl) {
        if (!isConnecting) {
            Log.i(TAG, "Scheduling WebSocket reconnect in " + RECONNECT_DELAY_SECONDS + " seconds");
            reconnectExecutor.schedule(() -> connectWebSocket(authToken, wsUrl), RECONNECT_DELAY_SECONDS, TimeUnit.SECONDS);
        }
    }

    public void updateNotificationBadge(int unreadCount) {
        BottomNavigationView bottomNavigationView = findViewById(R.id.menu_navigation);
        BadgeDrawable badge = bottomNavigationView.getOrCreateBadge(R.id.nav_notify);

        if (unreadCount > 0) {
            badge.setVisible(true);
            badge.setNumber(unreadCount);
            badge.setBackgroundColor(ContextCompat.getColor(this, R.color.red));
        } else {
            badge.setVisible(false);
        }
    }

    public void updateChatBadge(int unreadCount) {
        BottomNavigationView bottomNavigationView = findViewById(R.id.menu_navigation);
        BadgeDrawable badge = bottomNavigationView.getOrCreateBadge(R.id.nav_chat);

        if (unreadCount > 0) {
            badge.setVisible(true);
            badge.setNumber(unreadCount);
            badge.setBackgroundColor(ContextCompat.getColor(this, R.color.red));
        } else {
            badge.setVisible(false);
        }
    }

    private void openFragment(Fragment fragment) {
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction transaction = fragmentManager.beginTransaction();
        transaction.replace(R.id.nav_host_fragment, fragment);
        transaction.addToBackStack(null);
        transaction.commit();
    }

    private void fetchUserProfile() {
        SharedPreferences sharedPreferences = getSharedPreferences("MyPrefs", MODE_PRIVATE);
        String authToken = sharedPreferences.getString("authToken", null);
        String userId = sharedPreferences.getString("userId", null);

        if (authToken == null || userId == null) {
            Log.e(TAG, "No auth token or userId found, redirecting to login");
            Toast.makeText(this, "Vui lòng đăng nhập lại", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        Call<ApiResponse<ProfileResponse>> call = authService.getUserProfile("Bearer " + authToken, userId);
        call.enqueue(new Callback<ApiResponse<ProfileResponse>>() {
            @Override
            public void onResponse(Call<ApiResponse<ProfileResponse>> call, Response<ApiResponse<ProfileResponse>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().getStatus() == 200) {
                    ProfileResponse profile = response.body().getData();
                    Log.d(TAG, "Profile fetched successfully: " + profile.getFirstName());

                    SharedPreferences.Editor editor = sharedPreferences.edit();
                    editor.putString("firstName", profile.getFirstName());
                    editor.putString("lastName", profile.getLastName());
                    editor.putString("gender", profile.getGender());
                    editor.putInt("age", profile.getAge());
                    editor.putInt("height", profile.getHeight());
                    editor.putString("bio", profile.getBio());
                    editor.putString("zodiacSign", profile.getZodiacSign());
                    editor.putString("personalityType", profile.getPersonalityType());
                    editor.putString("communicationStyle", profile.getCommunicationStyle());
                    editor.putString("loveLanguage", profile.getLoveLanguage());
                    editor.putString("petPreference", profile.getPetPreference());
                    editor.putString("drinkingHabit", profile.getDrinkingHabit());
                    editor.putString("smokingHabit", profile.getSmokingHabit());
                    editor.putString("sleepingHabit", profile.getSleepingHabit());
                    editor.putString("hobbies", profile.getHobbies() != null ? String.join(",", profile.getHobbies()) : "");
                    editor.putString("pic1", profile.getPic1());
                    editor.putString("pic2", profile.getPic2());
                    editor.putString("pic3", profile.getPic3());
                    editor.putString("pic4", profile.getPic4());
                    editor.putString("pic5", profile.getPic5());
                    editor.putString("pic6", profile.getPic6());
                    editor.putString("pic7", profile.getPic7());
                    editor.putString("pic8", profile.getPic8());
                    editor.putString("pic9", profile.getPic9());
                    editor.apply();

                    Log.d(TAG, "Profile data stored in SharedPreferences");
                } else {
                    Log.e(TAG, "Failed to fetch profile: " + response.message());
                    Toast.makeText(MainActivity.this, "Không thể tải thông tin hồ sơ", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<ProfileResponse>> call, Throwable t) {
                Log.e(TAG, "Error fetching profile: " + t.getMessage());
                Toast.makeText(MainActivity.this, "Lỗi kết nối: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
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
        // Hủy đăng ký BroadcastReceiver
        if (badgeUpdateReceiver != null) {
            LocalBroadcastManager.getInstance(this).unregisterReceiver(badgeUpdateReceiver);
        }
        if (fetchProfileReceiver != null) {
            LocalBroadcastManager.getInstance(this).unregisterReceiver(fetchProfileReceiver);
        }
    }
}