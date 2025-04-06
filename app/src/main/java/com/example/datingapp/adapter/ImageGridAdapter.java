package com.example.datingapp.adapter;

import android.content.Context;
import android.graphics.RenderEffect;
import android.graphics.Shader;
import android.os.Build;
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
    private List<String> imageUrls; // Thay từ List<Integer> sang List<String> để chứa URL
    private List<Integer> ageList;

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
        Glide.with(context)
                .load(imageUrls.get(position))
                .into(holder.imageView);

        // Hiển thị tuổi
        if (ageList != null && position < ageList.size()) {
            holder.textAge.setText(String.valueOf(ageList.get(position)));
        }


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