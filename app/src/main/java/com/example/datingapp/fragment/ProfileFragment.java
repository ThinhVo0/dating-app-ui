package com.example.datingapp.fragment;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.widget.ViewPager2;
import com.example.datingapp.R;
import com.example.datingapp.adapter.ImageAdapter;
import com.google.android.material.bottomsheet.BottomSheetDialog;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class ProfileFragment extends Fragment {
    private static final String TAG = "ProfileFragment";
    private ViewPager2 viewPager;
    private ImageAdapter adapter;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.main, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Danh sách URL hình ảnh từ JSON
        List<String> imageUrls = new ArrayList<>();
        String jsonData = "{\n" +
                "  \"status\": 200,\n" +
                "  \"message\": \"\",\n" +
                "  \"data\": {\n" +
                "    \"street\": null,\n" +
                "    \"district\": null,\n" +
                "    \"province\": \"Thành Ph  Chí Minh\",\n" +
                "    \"pic1\": \"https://res.cloudinary.com/dx1irzekz/image/upload/v1741889436/nlvhyjnkuooeytegdfoq.jpg\",\n" +
                "    \"pic2\": \"https://res.cloudinary.com/dx1irzekz/image/upload/v1741889436/nlvhyjnkuooeytegdfoq.jpg\",\n" +
                "    \"pic3\": null,\n" +
                "    \"pic4\": null,\n" +
                "    \"pic5\": null,\n" +
                "    \"pic6\": null,\n" +
                "    \"pic7\": null,\n" +
                "    \"pic8\": null,\n" +
                "    \"pic9\": null,\n" +
                "    \"firstName\": \"Phi\",\n" +
                "    \"lastName\": \"Thắng\",\n" +
                "    \"hobbies\": [\n" +
                "      \"Bóng đá\"\n" +
                "    ],\n" +
                "    \"gender\": \"Nam\",\n" +
                "    \"age\": 20,\n" +
                "    \"height\": 171,\n" +
                "    \"bio\": \"Thắng đẹp trai\",\n" +
                "    \"zodiacSign\": \"Nhân Mã\",\n" +
                "    \"personalityType\": \"INTJ\",\n" +
                "    \"communicationStyle\": \"Nghiện nhắn tin\",\n" +
                "    \"loveLanguage\": \"Những hành động tinh tế\",\n" +
                "    \"petPreference\": \"Chó\",\n" +
                "    \"drinkingHabit\": \"Không dành cho mình\",\n" +
                "    \"smokingHabit\": \"Hút thuốc với bạn bè\",\n" +
                "    \"sleepingHabit\": \"Dậy sớm\"\n" +
                "  }\n" +
                "}";

        try {
            JSONObject jsonObject = new JSONObject(jsonData);
            JSONObject data = jsonObject.getJSONObject("data");

            for (int i = 1; i <= 9; i++) {
                String picKey = "pic" + i;
                if (data.has(picKey) && !data.isNull(picKey)) {
                    String picUrl = data.getString(picKey);
                    if (picUrl != null && !picUrl.isEmpty()) {
                        imageUrls.add(picUrl);
                    }
                }
            }





            Log.d(TAG, "Image URLs: " + imageUrls);

            // Khởi tạo ViewPager2
            viewPager = view.findViewById(R.id.viewPager);
            if (imageUrls.isEmpty()) {
                Log.w(TAG, "No images to display, using placeholder");
                imageUrls.add(""); // Thêm URL rỗng để tránh lỗi, hoặc xử lý theo cách khác
            }
            adapter = new ImageAdapter(requireContext(), imageUrls);
            viewPager.setAdapter(adapter);

            // Điền thông tin cơ bản (Tên, Tuổi, Địa chỉ)
            TextView tvNameAge = view.findViewById(R.id.tvNameAge);
            TextView tvAddress = view.findViewById(R.id.tvAddress);
            String fullName = data.getString("firstName") + " " + data.getString("lastName");
            int age = data.getInt("age");
            tvNameAge.setText(fullName + ", " + age);
            tvAddress.setText(data.isNull("province") ? "Không xác định" : data.getString("province"));

        } catch (Exception e) {
            e.printStackTrace();
            Log.e(TAG, "Exception: " + e.getMessage());
            Toast.makeText(requireContext(), "Error loading images or basic info: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }

        // Xử lý nút "Xem chi tiết"
        ImageButton btnViewDetails = view.findViewById(R.id.btnViewDetails);
        btnViewDetails.setOnClickListener(v -> showDetailsBottomSheet(jsonData));

        // Xử lý nút tương tác
        ImageButton btnDislike = view.findViewById(R.id.btnDislike);
        ImageButton btnLike = view.findViewById(R.id.btnLike);
        ImageButton btnChat = view.findViewById(R.id.btnChat);

        btnDislike.setOnClickListener(v -> Toast.makeText(requireContext(), "Disliked!", Toast.LENGTH_SHORT).show());
        btnLike.setOnClickListener(v -> Toast.makeText(requireContext(), "Liked!", Toast.LENGTH_SHORT).show());
        btnChat.setOnClickListener(v -> Toast.makeText(requireContext(), "Chat clicked!", Toast.LENGTH_SHORT).show());
    }

    private void showDetailsBottomSheet(String jsonData) {
        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(requireContext());
        View view = getLayoutInflater().inflate(R.layout.layout_details, null);
        bottomSheetDialog.setContentView(view);

        try {
            JSONObject jsonObject = new JSONObject(jsonData);
            JSONObject data = jsonObject.getJSONObject("data");

            // Điền dữ liệu cho phần "Tiểu sử"
            TextView bioText = view.findViewById(R.id.bio_text);
            bioText.setText(data.getString("bio"));

            // Điền dữ liệu cho phần "Thông tin chính"
            TextView genderText = view.findViewById(R.id.gender_text);
            TextView heightText = view.findViewById(R.id.height_text);
            TextView zodiacText = view.findViewById(R.id.zodiac_text);
            TextView personalityText = view.findViewById(R.id.personality_text);
            genderText.setText(data.getString("gender"));
            heightText.setText(data.getInt("height") + "cm");
            zodiacText.setText(data.getString("zodiacSign"));
            personalityText.setText(data.getString("personalityType"));

            // Điền dữ liệu cho phần "Thông tin thêm về tôi"
            TextView communicationText = view.findViewById(R.id.communication_text);
            TextView loveLanguageText = view.findViewById(R.id.love_language_text);
            TextView petText = view.findViewById(R.id.pet_text);
            TextView hobbiesText = view.findViewById(R.id.hobbies_text);
            communicationText.setText(data.getString("communicationStyle"));
            loveLanguageText.setText(data.getString("loveLanguage"));
            petText.setText(data.getString("petPreference"));
            JSONArray hobbiesArray = data.getJSONArray("hobbies");
            StringBuilder hobbies = new StringBuilder();
            for (int i = 0; i < hobbiesArray.length(); i++) {
                hobbies.append(hobbiesArray.getString(i));
                if (i < hobbiesArray.length() - 1) hobbies.append(", ");
            }
            hobbiesText.setText(hobbies.toString());

            // Điền dữ liệu cho phần "Phong cách sống"
            TextView drinkingText = view.findViewById(R.id.drinking_text);
            TextView smokingText = view.findViewById(R.id.smoking_text);
            TextView sleepText = view.findViewById(R.id.sleep_text);
            drinkingText.setText(data.getString("drinkingHabit"));
            smokingText.setText(data.getString("smokingHabit"));
            sleepText.setText(data.getString("sleepingHabit"));

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(requireContext(), "Error loading profile data: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }

        bottomSheetDialog.show();
    }
}