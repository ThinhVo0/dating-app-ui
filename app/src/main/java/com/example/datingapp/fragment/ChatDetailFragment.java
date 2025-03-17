package com.example.datingapp.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.datingapp.R;
import java.util.ArrayList;
import java.util.List;

public class ChatDetailFragment extends Fragment {

    private RecyclerView rvMessages;
    private MessageAdapter messageAdapter;
    private List<Message> messageList;
    private EditText etMessageInput;
    private ImageButton btnSendMessage;
    private TextView tvChatUserName;
    private String userName;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_chat_detail, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Lấy dữ liệu từ Bundle (tên người dùng)
        if (getArguments() != null) {
            userName = getArguments().getString("userName", "Người dùng");
        }

        // Ánh xạ các thành phần
        rvMessages = view.findViewById(R.id.rvMessages);
        etMessageInput = view.findViewById(R.id.etMessageInput);
        btnSendMessage = view.findViewById(R.id.btnSendMessage);
        tvChatUserName = view.findViewById(R.id.tvChatUserName);

        // Hiển thị tên người dùng
        tvChatUserName.setText(userName);

        // Khởi tạo danh sách tin nhắn giả lập
        initializeMessageList();

        // Cài đặt RecyclerView
        LinearLayoutManager layoutManager = new LinearLayoutManager(requireContext());
        layoutManager.setStackFromEnd(true); // Tin nhắn mới nhất ở dưới cùng
        rvMessages.setLayoutManager(layoutManager);
        messageAdapter = new MessageAdapter(messageList);
        rvMessages.setAdapter(messageAdapter);

        // Xử lý gửi tin nhắn
        btnSendMessage.setOnClickListener(v -> {
            String messageText = etMessageInput.getText().toString().trim();
            if (!messageText.isEmpty()) {
                messageList.add(new Message(messageText, true));
                messageAdapter.notifyItemInserted(messageList.size() - 1);
                rvMessages.scrollToPosition(messageList.size() - 1);
                etMessageInput.setText("");
                // TODO: Gửi tin nhắn đến server hoặc người dùng khác
            }
        });
    }

    private void initializeMessageList() {
        messageList = new ArrayList<>();
        messageList.add(new Message("Xin chào!", false));
        messageList.add(new Message("Chào bạn, bạn khỏe không?", true));
        messageList.add(new Message("Mình khỏe, cảm ơn bạn!", false));
        // Thêm dữ liệu thực tế từ API hoặc nguồn khác nếu cần
    }

    // Model cho tin nhắn
    private static class Message {
        String text;
        boolean isSelf;

        Message(String text, boolean isSelf) {
            this.text = text;
            this.isSelf = isSelf;
        }
    }

    // Adapter cho RecyclerView
    private static class MessageAdapter extends RecyclerView.Adapter<MessageAdapter.MessageViewHolder> {
        private List<Message> messages;

        MessageAdapter(List<Message> messages) {
            this.messages = messages;
        }

        @Override
        public MessageViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_message, parent, false);
            return new MessageViewHolder(view);
        }

        @Override
        public void onBindViewHolder(MessageViewHolder holder, int position) {
            Message message = messages.get(position);
            if (message.isSelf) {
                holder.tvMessageSelf.setText(message.text);
                holder.tvMessageSelf.setVisibility(View.VISIBLE);
                holder.tvMessageOther.setVisibility(View.GONE);
            } else {
                holder.tvMessageOther.setText(message.text);
                holder.tvMessageOther.setVisibility(View.VISIBLE);
                holder.tvMessageSelf.setVisibility(View.GONE);
            }
        }

        @Override
        public int getItemCount() {
            return messages.size();
        }

        static class MessageViewHolder extends RecyclerView.ViewHolder {
            TextView tvMessageSelf, tvMessageOther;

            MessageViewHolder(View itemView) {
                super(itemView);
                tvMessageSelf = itemView.findViewById(R.id.tvMessageSelf);
                tvMessageOther = itemView.findViewById(R.id.tvMessageOther);
            }
        }
    }
}