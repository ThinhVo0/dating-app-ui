package com.example.datingapp.adapter;

import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class EmojiAdapter extends RecyclerView.Adapter<EmojiAdapter.EmojiViewHolder> {
    private List<String> emojiList;
    private EmojiClickListener listener;

    public interface EmojiClickListener {
        void onEmojiClick(String emoji);
    }

    public EmojiAdapter(List<String> emojiList, EmojiClickListener listener) {
        this.emojiList = emojiList;
        this.listener = listener;
    }

    @Override
    public EmojiViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        TextView textView = new TextView(parent.getContext());
        textView.setTextSize(24);
        textView.setPadding(16, 16, 16, 16);
        textView.setGravity(android.view.Gravity.CENTER);
        textView.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));
        return new EmojiViewHolder(textView);
    }

    @Override
    public void onBindViewHolder(EmojiViewHolder holder, int position) {
        String emoji = emojiList.get(position);
        holder.textView.setText(emoji);

        holder.textView.setOnClickListener(v -> {
            listener.onEmojiClick(emoji);
        });
    }

    @Override
    public int getItemCount() {
        return emojiList.size();
    }

    class EmojiViewHolder extends RecyclerView.ViewHolder {
        TextView textView;

        public EmojiViewHolder(View itemView) {
            super(itemView);
            textView = (TextView) itemView;
        }
    }
}