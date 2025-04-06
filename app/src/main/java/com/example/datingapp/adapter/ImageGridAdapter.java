package com.example.datingapp.adapter;

import android.content.Context;
import android.graphics.RenderEffect;
import android.graphics.Shader;
import android.os.Build;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.example.datingapp.R;

import java.util.List;

public class ImageGridAdapter extends BaseAdapter {
    private Context context;
    private List<String> imageUrls; // Danh sách URL ảnh (pic1)
    private List<Integer> ageList;  // Danh sách tuổi

    public ImageGridAdapter(Context context, List<String> imageUrls, List<Integer> ageList) {
        this.context = context;
        this.imageUrls = imageUrls;
        this.ageList = ageList;
    }

    @Override
    public int getCount() {
        return imageUrls.size();
    }

    @Override
    public Object getItem(int position) {
        return imageUrls.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;
        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(R.layout.item_like, parent, false);
            holder = new ViewHolder();
            holder.imageView = convertView.findViewById(R.id.imageView);
            holder.textAge = convertView.findViewById(R.id.textAge);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        // Tải ảnh từ URL bằng Glide
        String imageUrl = imageUrls.get(position);
        Log.d("ImageGridAdapter", "Loading image at position " + position + ": " + imageUrl);
        Glide.with(context)
                .load(imageUrl)
                .placeholder(R.drawable.load) // Ảnh placeholder khi đang tải
                .error(R.drawable.ic_dislike) // Ảnh hiển thị khi load thất bại
                .into(holder.imageView);

        // Hiển thị tuổi
        if (ageList != null && position < ageList.size()) {
            holder.textAge.setText(String.valueOf(ageList.get(position)));
        } else {
            holder.textAge.setText(""); // Nếu không có tuổi, để trống
        }

        // Áp dụng hiệu ứng blur (chỉ trên Android 12+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            RenderEffect blurEffect = RenderEffect.createBlurEffect(90f, 90f, Shader.TileMode.CLAMP);
            holder.imageView.setRenderEffect(blurEffect);
        }

        return convertView;
    }

    static class ViewHolder {
        ImageView imageView;
        TextView textAge;
    }
}