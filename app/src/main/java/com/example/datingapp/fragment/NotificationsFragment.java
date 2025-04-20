package com.example.datingapp.fragment;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.datingapp.R;
import com.example.datingapp.adapter.NotificationAdapter;
import com.example.datingapp.dto.Notification;
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
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class NotificationsFragment extends Fragment {

    private static final String TAG = "NotificationsFragment";
    private RecyclerView rvNotifications;
    private NotificationAdapter notificationAdapter;
    private List<Notification> notificationList;
    private String currentUserId;
    private WebSocketStompClient stompClient;
    private StompSession stompSession;
    private ExecutorService executorService;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_notification, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        executorService = Executors.newSingleThreadExecutor();

        SharedPreferences prefs = requireContext().getSharedPreferences("MyPrefs", requireContext().MODE_PRIVATE);
        currentUserId = prefs.getString("userId", null);
        String authToken = prefs.getString("authToken", null);

        if (currentUserId == null || authToken == null) {
            Toast.makeText(requireContext(), "Vui lòng đăng nhập lại", Toast.LENGTH_SHORT).show();
            return;
        }

        rvNotifications = view.findViewById(R.id.rvNotifications);
        notificationList = new ArrayList<>();
        rvNotifications.setLayoutManager(new LinearLayoutManager(requireContext()));
        notificationAdapter = new NotificationAdapter(notificationList);
        rvNotifications.setAdapter(notificationAdapter);

        loadNotifications(authToken);
        setupWebSocket(authToken);
    }

    private void loadNotifications(String authToken) {
        AuthService authService = RetrofitClient.getClient().create(AuthService.class);
        Call<List<Notification>> call = authService.getNotifications("Bearer " + authToken, currentUserId);

        call.enqueue(new Callback<List<Notification>>() {
            @Override
            public void onResponse(Call<List<Notification>> call, Response<List<Notification>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    notificationList.clear();
                    notificationList.addAll(response.body());
                    Collections.reverse(notificationList);
                    notificationAdapter.notifyDataSetChanged();
                    Log.i(TAG, "Loaded " + notificationList.size() + " notifications");
                } else {
                    Log.e(TAG, "Failed to load notifications: " + response.code());
                    Toast.makeText(requireContext(), "Không thể tải thông báo: " + response.code(), Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<List<Notification>> call, Throwable t) {
                Log.e(TAG, "Load notifications failed: " + t.getMessage(), t);
                Toast.makeText(requireContext(), "Lỗi: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setupWebSocket(String authToken) {
        List<Transport> transports = new ArrayList<>();
        transports.add(new WebSocketTransport(new StandardWebSocketClient()));
        SockJsClient sockJsClient = new SockJsClient(transports);
        stompClient = new WebSocketStompClient(sockJsClient);
        stompClient.setMessageConverter(new MappingJackson2MessageConverter());

        String wsUrl = "http://192.168.1.100:8080/ws"; // Thay bằng IP máy bạn

        executorService.execute(() -> {
            try {
                StompHeaders connectHeaders = new StompHeaders();
                connectHeaders.add("Authorization", "Bearer " + authToken);

                StompSession session = stompClient.connect(wsUrl, new StompSessionHandlerAdapter() {
                    @Override
                    public void afterConnected(StompSession session, StompHeaders connectedHeaders) {
                        Log.i(TAG, "Connected to WebSocket with session ID: " + session.getSessionId());
                        stompSession = session;
                        session.subscribe("/user/queue/notifications", new StompFrameHandler() {
                            @Override
                            public Type getPayloadType(StompHeaders headers) {
                                return Notification.class;
                            }

                            @Override
                            public void handleFrame(StompHeaders headers, Object payload) {
                                Notification notification = (Notification) payload;
                                Log.i(TAG, "Received realtime notification: " + notification.getContent());
                                requireActivity().runOnUiThread(() -> {
                                    notificationList.add(0, notification); // Thêm vào đầu danh sách
                                    notificationAdapter.notifyItemInserted(0);
                                    rvNotifications.scrollToPosition(0);
                                    Log.i(TAG, "Updated UI with notification: " + notification.getContent());
                                });
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
                requireActivity().runOnUiThread(() ->
                        Toast.makeText(requireContext(), "Không thể kết nối WebSocket: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                );
            }
        });
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (stompClient != null && stompClient.isRunning()) {
            stompClient.stop();
        }
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
        }
    }
}