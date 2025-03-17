package com.example.datingapp.fragment;

import android.os.Bundle;
import android.util.Log;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
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
    private List<String> profileJsonList;
    private int currentProfileIndex = 0;
    private ImageButton btnDislike;
    private ImageButton btnLike;

    // Ngưỡng để xác định vuốt (pixels)
    private static final int SWIPE_THRESHOLD = 100;
    private static final int SWIPE_VELOCITY_THRESHOLD = 100;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_match, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Khởi tạo danh sách JSON profiles
        initializeProfileList();

        // Khởi tạo ViewPager2
        viewPager = view.findViewById(R.id.viewPager);
        viewPager.setUserInputEnabled(false); // Tắt vuốt mặc định của ViewPager2

        // Khởi tạo các nút
        btnDislike = view.findViewById(R.id.btnDislike);
        btnLike = view.findViewById(R.id.btnLike);
        ImageButton btnChat = view.findViewById(R.id.btnChat);
        ImageButton btnViewDetails = view.findViewById(R.id.btnViewDetails);

        // Hiển thị profile đầu tiên
        loadProfile(view, profileJsonList.get(currentProfileIndex));

        // Xử lý sự kiện nút
        btnDislike.setOnClickListener(v -> {
            Toast.makeText(requireContext(), "Disliked!", Toast.LENGTH_SHORT).show();
            scaleUpButton(btnDislike, view, false); // Phóng to nút Dislike rồi chuyển profile
        });

        btnLike.setOnClickListener(v -> {
            Toast.makeText(requireContext(), "Liked!", Toast.LENGTH_SHORT).show();
            scaleUpButton(btnLike, view, true); // Phóng to nút Like rồi chuyển profile
        });

        btnChat.setOnClickListener(v -> Toast.makeText(requireContext(), "Chat clicked!", Toast.LENGTH_SHORT).show());

        btnViewDetails.setOnClickListener(v -> showDetailsBottomSheet(profileJsonList.get(currentProfileIndex)));

        // Thêm GestureDetector để xử lý vuốt và click
        GestureDetector gestureDetector = new GestureDetector(getContext(), new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
                float diffX = e2.getX() - e1.getX();
                float diffY = e2.getY() - e1.getY();

                // Kiểm tra vuốt ngang
                if (Math.abs(diffX) > Math.abs(diffY)) {
                    if (Math.abs(diffX) > SWIPE_THRESHOLD && Math.abs(velocityX) > SWIPE_VELOCITY_THRESHOLD) {
                        if (diffX > 0) {
                            // Vuốt sang phải -> Like
                            Toast.makeText(requireContext(), "Liked!", Toast.LENGTH_SHORT).show();
                            scaleUpButton(btnLike, view, true);
                        } else {
                            // Vuốt sang trái -> Dislike
                            Toast.makeText(requireContext(), "Disliked!", Toast.LENGTH_SHORT).show();
                            scaleUpButton(btnDislike, view, false);
                        }
                        return true;
                    }
                }
                return false;
            }

            @Override
            public boolean onSingleTapUp(MotionEvent e) {
                // Lấy vị trí chạm và chiều rộng màn hình
                float x = e.getX();
                float screenWidth = viewPager.getWidth();
                int currentItem = viewPager.getCurrentItem();

                // Chia màn hình làm 2: trái và phải
                if (x < screenWidth / 2) {
                    // Click bên trái -> Quay lại ảnh trước
                    if (currentItem > 0) {
                        viewPager.setCurrentItem(currentItem - 1, true);
                    }
                } else {
                    // Click bên phải -> Chuyển sang ảnh sau
                    if (currentItem < adapter.getItemCount() - 1) {
                        viewPager.setCurrentItem(currentItem + 1, true);
                    }
                }
                return true;
            }
        });

        // Gắn GestureDetector vào ViewPager2
        viewPager.setOnTouchListener((v, event) -> {
            gestureDetector.onTouchEvent(event);
            return true; // Chặn ViewPager2 xử lý vuốt mặc định
        });
    }

    // Phương thức phóng to nút trước khi chuyển profile
    private void scaleUpButton(ImageButton button, View view, boolean isSwipeRight) {
        Animation scaleUp = AnimationUtils.loadAnimation(getContext(), R.anim.scale_up);
        scaleUp.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {}

            @Override
            public void onAnimationEnd(Animation animation) {
                // Sau khi nút phóng to, thực hiện hiệu ứng chuyển profile
                applySwipeAnimation(view, isSwipeRight);
                // Đặt lại kích thước nút về bình thường
                button.setScaleX(1.0f);
                button.setScaleY(1.0f);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {}
        });
        button.startAnimation(scaleUp);
    }

    // Phương thức thêm hiệu ứng khi vuốt profile
    private void applySwipeAnimation(View view, boolean isSwipeRight) {
        Animation animation;
        if (isSwipeRight) {
            animation = AnimationUtils.loadAnimation(getContext(), R.anim.swipe_right);
        } else {
            animation = AnimationUtils.loadAnimation(getContext(), R.anim.swipe_left);
        }

        animation.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {}

            @Override
            public void onAnimationEnd(Animation animation) {
                nextProfile(view); // Chuyển profile sau khi animation kết thúc
            }

            @Override
            public void onAnimationRepeat(Animation animation) {}
        });

        viewPager.startAnimation(animation);
    }

    private void initializeProfileList() {
        profileJsonList = new ArrayList<>();
        profileJsonList.add("{\n" +
                "  \"status\": 200,\n" +
                "  \"message\": \"\",\n" +
                "  \"data\": {\n" +
                "    \"firstName\": \"Trần\",\n" +
                "    \"lastName\": \"Phi Thắng\",\n" +
                "    \"gender\": \"Nam\",\n" +
                "    \"age\": 20,\n" +
                "    \"height\": 170,\n" +
                "    \"bio\": \"Xin chào\",\n" +
                "    \"zodiacSign\": \"Nhân Mã\",\n" +
                "    \"personalityType\": \"INTJ\",\n" +
                "    \"communicationStyle\": \"Nghiện nhắn tin\",\n" +
                "    \"loveLanguage\": \"Những hành động tinh tế\",\n" +
                "    \"petPreference\": \"Chó\",\n" +
                "    \"drinkingHabit\": \"Không dành cho mình\",\n" +
                "    \"smokingHabit\": \"Hút thuốc với bạn bè\",\n" +
                "    \"sleepingHabit\": \"Dậy sớm\",\n" +
                "    \"hobbies\": [\"Bóng đá\"],\n" +
                "    \"street\": null,\n" +
                "    \"district\": null,\n" +
                "    \"province\": \"Long An\",\n" +
                "    \"pic1\": \"http://res.cloudinary.com/dx1irzekz/image/upload/v1741889265/egtjer9vahkmvuxfeh9c.jpg\",\n" +
                "    \"pic2\": \"http://res.cloudinary.com/dx1irzekz/image/upload/v1741889267/ozbr9map5epifjqhoxqd.jpg\"\n" +
                "  }\n" +
                "}");
        profileJsonList.add("{\n" +
                "  \"status\": 200,\n" +
                "  \"message\": \"\",\n" +
                "  \"data\": {\n" +
                "    \"firstName\": \"Thịnh\",\n" +
                "    \"lastName\": \"Xuân\",\n" +
                "    \"gender\": \"Nam\",\n" +
                "    \"age\": 20,\n" +
                "    \"height\": 170,\n" +
                "    \"bio\": \"Tui bị ngu\",\n" +
                "    \"zodiacSign\": \"Thiên Bình\",\n" +
                "    \"personalityType\": \"INTJ\",\n" +
                "    \"communicationStyle\": \"Nghiện nhắn tin\",\n" +
                "    \"loveLanguage\": \"Những hành động tinh tế\",\n" +
                "    \"petPreference\": \"Chó\",\n" +
                "    \"drinkingHabit\": \"Không dành cho mình\",\n" +
                "    \"smokingHabit\": \"Hút thuốc với bạn bè\",\n" +
                "    \"sleepingHabit\": \"Dậy sớm\",\n" +
                "    \"hobbies\": [\"Bóng đá\"],\n" +
                "    \"street\": null,\n" +
                "    \"district\": null,\n" +
                "    \"province\": null,\n" +
                "    \"pic1\": \"http://res.cloudinary.com/dx1irzekz/image/upload/v1742227908/zcljkwc6bl4l2s5nkkm0.jpg\",\n" +
                "    \"pic2\": \"http://res.cloudinary.com/dx1irzekz/image/upload/v1742227910/mg0gedtm9gmldip0llla.jpg\",\n" +
                "    \"pic3\": \"http://res.cloudinary.com/dx1irzekz/image/upload/v1742227930/zgeib1eb3fneogrbtpuu.jpg\"\n" +
                "  }\n" +
                "}");
        profileJsonList.add("{\n" +
                "  \"status\": 200,\n" +
                "  \"message\": \"\",\n" +
                "  \"data\": {\n" +
                "    \"firstName\": \"Doraemon\",\n" +
                "    \"lastName\": \"\",\n" +
                "    \"gender\": \"Nam\",\n" +
                "    \"age\": 18,\n" +
                "    \"height\": 129,\n" +
                "    \"bio\": \"Tui bị ngu\",\n" +
                "    \"zodiacSign\": \"Thiên Bình\",\n" +
                "    \"personalityType\": \"INTJ\",\n" +
                "    \"communicationStyle\": \"Nghiện nhắn tin\",\n" +
                "    \"loveLanguage\": \"Những hành động tinh tế\",\n" +
                "    \"petPreference\": \"Chó\",\n" +
                "    \"drinkingHabit\": \"Không dành cho mình\",\n" +
                "    \"smokingHabit\": \"Hút thuốc với bạn bè\",\n" +
                "    \"sleepingHabit\": \"Dậy sớm\",\n" +
                "    \"hobbies\": [\n" +
                "      \"Bóng đá\"\n" +
                "    ],\n" +
                "    \"street\": null,\n" +
                "    \"district\": null,\n" +
                "    \"province\": null,\n" +
                "    \"pic1\": \"http://res.cloudinary.com/dx1irzekz/image/upload/v1742229865/dffw6i64omeil8nnmf3c.jpg\",\n" +
                "    \"pic2\": \"http://res.cloudinary.com/dx1irzekz/image/upload/v1742229866/an5skilm2xmgufpgr2mw.jpg\",\n" +
                "    \"pic3\": \"http://res.cloudinary.com/dx1irzekz/image/upload/v1742229868/ulautnzwffwpz6bbsuob.jpg\",\n" +
                "    \"pic4\": null,\n" +
                "    \"pic5\": null,\n" +
                "    \"pic6\": null,\n" +
                "    \"pic7\": null,\n" +
                "    \"pic8\": null,\n" +
                "    \"pic9\": null\n" +
                "  }\n" +
                "}");
    }

    private void nextProfile(View view) {
        currentProfileIndex++;
        if (currentProfileIndex < profileJsonList.size()) {
            loadProfile(view, profileJsonList.get(currentProfileIndex));
        } else {
            Toast.makeText(requireContext(), "Hết profile để xem!", Toast.LENGTH_SHORT).show();
            currentProfileIndex = 0;
            loadProfile(view, profileJsonList.get(currentProfileIndex));
        }
    }

    private void loadProfile(View view, String jsonData) {
        List<String> imageUrls = new ArrayList<>();
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

            if (imageUrls.isEmpty()) {
                Log.w(TAG, "No images to display, using placeholder");
                imageUrls.add("");
            }
            adapter = new ImageAdapter(requireContext(), imageUrls);
            viewPager.setAdapter(adapter);

            TextView tvNameAge = view.findViewById(R.id.tvNameAge);
            TextView tvAddress = view.findViewById(R.id.tvAddress);
            String fullName = data.getString("firstName") + " " + data.getString("lastName");
            int age = data.getInt("age");
            tvNameAge.setText(fullName + ", " + age);
            tvAddress.setText(data.isNull("province") ? "Không xác định" : data.getString("province"));

        } catch (Exception e) {
            e.printStackTrace();
            Log.e(TAG, "Exception: " + e.getMessage());
            Toast.makeText(requireContext(), "Error loading profile: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void showDetailsBottomSheet(String jsonData) {
        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(requireContext());
        View view = getLayoutInflater().inflate(R.layout.layout_details, null);
        bottomSheetDialog.setContentView(view);

        try {
            JSONObject jsonObject = new JSONObject(jsonData);
            JSONObject data = jsonObject.getJSONObject("data");

            TextView bioText = view.findViewById(R.id.bio_text);
            bioText.setText(data.getString("bio"));

            TextView genderText = view.findViewById(R.id.gender_text);
            TextView heightText = view.findViewById(R.id.height_text);
            TextView zodiacText = view.findViewById(R.id.zodiac_text);
            TextView personalityText = view.findViewById(R.id.personality_text);
            genderText.setText(data.getString("gender"));
            heightText.setText(data.getInt("height") + "cm");
            zodiacText.setText(data.getString("zodiacSign"));
            personalityText.setText(data.getString("personalityType"));

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