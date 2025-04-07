package com.example.datingapp.fragment;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.datingapp.R;
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
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ChatDetailFragment extends Fragment {

    private static final String TAG = "ChatDetailFragment";
    private RecyclerView rvMessages;
    private MessageAdapter messageAdapter;
    private List<MessageDTO> messageList;
    private EditText etMessageInput;
    private ImageButton btnSendMessage;
    private TextView tvChatUserName;
    private String userName;
    private String targetUserId;
    private String currentUserId;
    private WebSocketStompClient stompClient;
    private StompSession stompSession;
    private ExecutorService executorService;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_chat_detail, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        executorService = Executors.newSingleThreadExecutor();

        if (getArguments() != null) {
            userName = getArguments().getString("userName", "Người dùng");
            targetUserId = getArguments().getString("userId");
        }

        SharedPreferences prefs = requireContext().getSharedPreferences("MyPrefs", requireContext().MODE_PRIVATE);
        currentUserId = prefs.getString("userId", null);
        String authToken = prefs.getString("authToken", null);

        if (currentUserId == null || authToken == null) {
            Toast.makeText(requireContext(), "Vui lòng đăng nhập lại", Toast.LENGTH_SHORT).show();
            return;
        }

        rvMessages = view.findViewById(R.id.rvMessages);
        etMessageInput = view.findViewById(R.id.etMessageInput);
        btnSendMessage = view.findViewById(R.id.btnSendMessage);
        tvChatUserName = view.findViewById(R.id.tvChatUserName);
        tvChatUserName.setText(userName);

        messageList = new ArrayList<>();
        LinearLayoutManager layoutManager = new LinearLayoutManager(requireContext());
        layoutManager.setStackFromEnd(true);
        rvMessages.setLayoutManager(layoutManager);
        messageAdapter = new MessageAdapter(messageList);
        rvMessages.setAdapter(messageAdapter);

        setupWebSocket(authToken);
        loadInitialMessages(authToken);

        btnSendMessage.setOnClickListener(v -> sendMessage());
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
                                    requireActivity().runOnUiThread(() -> {
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
                requireActivity().runOnUiThread(() ->
                        Toast.makeText(requireContext(), "Không thể kết nối WebSocket: " + e.getMessage(), Toast.LENGTH_SHORT).show()
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
                if (response.isSuccessful() && response.body() != null) {
                    messageList.clear();
                    messageList.addAll(response.body());
                    messageAdapter.notifyDataSetChanged();
                    rvMessages.scrollToPosition(messageList.size() - 1);
                } else {
                    Log.e(TAG, "Failed to load messages: " + response.code());
                    Toast.makeText(requireContext(), "Không thể tải tin nhắn: " + response.code(), Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<List<MessageDTO>> call, Throwable t) {
                Log.e(TAG, "Load messages failed: " + t.getMessage(), t);
                Toast.makeText(requireContext(), "Lỗi: " + t.getMessage(), Toast.LENGTH_SHORT).show();
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
                    requireActivity().runOnUiThread(() ->
                            Toast.makeText(requireContext(), "Lỗi gửi tin nhắn", Toast.LENGTH_SHORT).show()
                    );
                }
            });
        } else {
            Toast.makeText(requireContext(), "Không thể gửi tin nhắn", Toast.LENGTH_SHORT).show();
        }
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

    private class MessageAdapter extends RecyclerView.Adapter<MessageAdapter.MessageViewHolder> {
        private List<MessageDTO> messages;

        MessageAdapter(List<MessageDTO> messages) {
            this.messages = messages;
        }

        @Override
        public MessageViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_message, parent, false);
            return new MessageViewHolder(view);
        }

        @Override
        public void onBindViewHolder(MessageViewHolder holder, int position) {
            MessageDTO message = messages.get(position);
            boolean isSelf = message.getSenderId().equals(currentUserId);

            if (isSelf) {
                holder.selfMessageLayout.setVisibility(View.VISIBLE);
                holder.otherMessageLayout.setVisibility(View.GONE);
                holder.tvMessageSelf.setText(message.getContent());
                holder.tvTimeSelf.setText(message.getSendTime());
            } else {
                holder.selfMessageLayout.setVisibility(View.GONE);
                holder.otherMessageLayout.setVisibility(View.VISIBLE);
                holder.tvMessageOther.setText(message.getContent());
                holder.tvTimeOther.setText(message.getSendTime());
            }
        }

        @Override
        public int getItemCount() {
            return messages.size();
        }

        class MessageViewHolder extends RecyclerView.ViewHolder {
            LinearLayout selfMessageLayout, otherMessageLayout;
            TextView tvMessageSelf, tvMessageOther, tvTimeSelf, tvTimeOther;

            MessageViewHolder(View itemView) {
                super(itemView);
                selfMessageLayout = itemView.findViewById(R.id.selfMessageLayout);
                otherMessageLayout = itemView.findViewById(R.id.otherMessageLayout);
                tvMessageSelf = itemView.findViewById(R.id.tvMessageSelf);
                tvMessageOther = itemView.findViewById(R.id.tvMessageOther);
                tvTimeSelf = itemView.findViewById(R.id.tvTimeSelf);
                tvTimeOther = itemView.findViewById(R.id.tvTimeOther);
            }
        }
    }
}