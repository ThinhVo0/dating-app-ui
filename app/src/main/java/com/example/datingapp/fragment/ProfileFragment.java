package com.example.datingapp.fragment;

import android.content.Context;
import android.content.SharedPreferences;
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
import com.example.datingapp.dto.response.ProfileResponse;
import com.example.datingapp.model.ProfileActionResponse;
import com.example.datingapp.model.ProfileData;
import com.example.datingapp.model.ProfileDetailResponse;
import com.example.datingapp.network.AuthService;
import com.example.datingapp.network.RetrofitClient;
import com.google.android.material.bottomsheet.BottomSheetDialog;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ProfileFragment extends Fragment {
    private static final String TAG = "ProfileFragment";
    private ViewPager2 viewPager;
    private ImageAdapter adapter;
    private List<String> profileIds = new ArrayList<>();
    private List<ProfileData> profileDataList = new ArrayList<>();
    private int currentProfileIndex = 0;
    private ImageButton btnDislike;
    private ImageButton btnLike;
    private String authToken;
    private static final int SWIPE_THRESHOLD = 100;
    private static final int SWIPE_VELOCITY_THRESHOLD = 100;
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_match, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        SharedPreferences sharedPreferences = requireContext().getSharedPreferences("MyPrefs", Context.MODE_PRIVATE);
        authToken = sharedPreferences.getString("authToken", null);

        viewPager = view.findViewById(R.id.viewPager);
        viewPager.setUserInputEnabled(false);

        btnDislike = view.findViewById(R.id.btnDislike);
        btnLike = view.findViewById(R.id.btnLike);
        ImageButton btnChat = view.findViewById(R.id.btnChat);
        ImageButton btnViewDetails = view.findViewById(R.id.btnViewDetails);

        btnDislike.setOnClickListener(v -> performSwipe(view, false));
        btnLike.setOnClickListener(v -> performSwipe(view, true));
        btnChat.setOnClickListener(v -> Toast.makeText(requireContext(), "Chat clicked!", Toast.LENGTH_SHORT).show());
        btnViewDetails.setOnClickListener(v -> showDetailsBottomSheet());

        initializeProfileList(view);
        GestureDetector gestureDetector = new GestureDetector(getContext(), new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
                float diffX = e2.getX() - e1.getX();
                float diffY = e2.getY() - e1.getY();

                if (Math.abs(diffX) > Math.abs(diffY)) {
                    if (Math.abs(diffX) > SWIPE_THRESHOLD && Math.abs(velocityX) > SWIPE_VELOCITY_THRESHOLD) {
                        performSwipe(view, diffX > 0);
                        return true;
                    }
                }
                return false;
            }

            @Override
            public boolean onSingleTapUp(MotionEvent e) {
                float x = e.getX();
                int currentItem = viewPager.getCurrentItem();
                float screenWidth = viewPager.getWidth();

                if (x < screenWidth / 2 && currentItem > 0) {
                    viewPager.setCurrentItem(currentItem - 1, true);
                } else if (x >= screenWidth / 2 && currentItem < adapter.getItemCount() - 1) {
                    viewPager.setCurrentItem(currentItem + 1, true);
                }
                return true;
            }
        });

        viewPager.setOnTouchListener((v, event) -> {
            gestureDetector.onTouchEvent(event);
            return true;
        });
    }

    private void initializeProfileList(View view) {
        profileIds.clear();
        profileDataList.clear();
        currentProfileIndex = 0;

        AuthService authService = RetrofitClient.getClient().create(AuthService.class);

        SharedPreferences filterPrefs = requireContext().getSharedPreferences("FilterPrefs", Context.MODE_PRIVATE);
        String gender = filterPrefs.contains("gender") ? filterPrefs.getString("gender", null) : null;
        Integer minAge = filterPrefs.contains("minAge") ? filterPrefs.getInt("minAge", 18) : null;
        Integer maxAge = filterPrefs.contains("maxAge") ? filterPrefs.getInt("maxAge", 100) : null;
        Double maxDistance = filterPrefs.contains("maxDistance") ? (double) filterPrefs.getFloat("maxDistance", 10.0f) : null;

        Call<ProfileResponse> call = authService.getProfileIds(
                "Bearer " + authToken,
                gender, minAge, maxAge, maxDistance
        );

        call.enqueue(new Callback<ProfileResponse>() {
            @Override
            public void onResponse(Call<ProfileResponse> call, Response<ProfileResponse> response) {
                if (response.isSuccessful() && response.body() != null && response.body().getStatus() == 200) {
                    profileIds = response.body().getData();
                    if (profileIds.isEmpty()) {
                        Toast.makeText(requireContext(), "Không tìm thấy profile nào!", Toast.LENGTH_SHORT).show();
                    } else {
                        loadAllProfiles(view);
                    }
                } else {
                    Toast.makeText(requireContext(), "Không thể tải danh sách profile", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ProfileResponse> call, Throwable t) {
                Toast.makeText(requireContext(), "Lỗi: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadAllProfiles(View view) {
        AuthService authService = RetrofitClient.getClient().create(AuthService.class);
        for (String id : profileIds) {
            Call<ProfileDetailResponse> call = authService.getProfileDetail("Bearer " + authToken, id);
            call.enqueue(new Callback<ProfileDetailResponse>() {
                @Override
                public void onResponse(Call<ProfileDetailResponse> call, Response<ProfileDetailResponse> response) {
                    if (response.isSuccessful() && response.body() != null && response.body().getStatus() == 200) {
                        profileDataList.add(response.body().getData());
                        if (profileDataList.size() == profileIds.size()) {
                            loadProfile(view, profileDataList.get(0));
                        }
                    }
                }

                @Override
                public void onFailure(Call<ProfileDetailResponse> call, Throwable t) {
                    Log.e(TAG, "Error loading profile " + id + ": " + t.getMessage());
                }
            });
        }
    }

    private void performSwipe(View view, boolean isSwipeRight) {
        if (currentProfileIndex >= profileIds.size() || currentProfileIndex >= profileDataList.size()) {
            Toast.makeText(requireContext(), "Không còn profile để swipe", Toast.LENGTH_SHORT).show();
            return;
        }

        String profileId = profileIds.get(currentProfileIndex); // Lấy profileId từ danh sách profileIds

        ImageButton button = isSwipeRight ? btnLike : btnDislike;
        Animation scaleUp = AnimationUtils.loadAnimation(getContext(), R.anim.scale_up);
        scaleUp.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {}

            @Override
            public void onAnimationEnd(Animation animation) {
                Animation swipeAnimation = AnimationUtils.loadAnimation(getContext(),
                        isSwipeRight ? R.anim.swipe_right : R.anim.swipe_left);
                swipeAnimation.setAnimationListener(new Animation.AnimationListener() {
                    @Override
                    public void onAnimationStart(Animation animation) {
                        // Gọi API khi bắt đầu swipe
                        AuthService authService = RetrofitClient.getClient().create(AuthService.class);
                        Call<ProfileActionResponse> call = isSwipeRight
                                ? authService.likeProfile("Bearer " + authToken, profileId)
                                : authService.skipProfile("Bearer " + authToken, profileId);

                        call.enqueue(new Callback<ProfileActionResponse>() {
                            @Override
                            public void onResponse(Call<ProfileActionResponse> call, Response<ProfileActionResponse> response) {
                                if (response.isSuccessful() && response.body() != null && response.body().getStatus() == 200) {
                                    Toast.makeText(requireContext(), response.body().getData(), Toast.LENGTH_SHORT).show();
                                } else {
                                    Toast.makeText(requireContext(), "Lỗi khi thực hiện hành động", Toast.LENGTH_SHORT).show();
                                }
                            }

                            @Override
                            public void onFailure(Call<ProfileActionResponse> call, Throwable t) {
                                Toast.makeText(requireContext(), "Lỗi: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                            }
                        });
                    }

                    @Override
                    public void onAnimationEnd(Animation animation) {
                        nextProfile(view); // Chuyển sang profile tiếp theo sau khi swipe
                        button.setScaleX(1.0f);
                        button.setScaleY(1.0f);
                    }

                    @Override
                    public void onAnimationRepeat(Animation animation) {}
                });
                viewPager.startAnimation(swipeAnimation);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {}
        });
        button.startAnimation(scaleUp);
    }

    private void nextProfile(View view) {
        currentProfileIndex++;
        if (currentProfileIndex >= profileDataList.size()) {
            Toast.makeText(requireContext(), "Đã hết profile!", Toast.LENGTH_SHORT).show();

            // Reset lại trạng thái ban đầu
            profileDataList.clear(); // Xóa danh sách profile đã load
            currentProfileIndex = 0; // Đặt lại chỉ số về 0

            // Quay lại trạng thái mặc định, chưa load data
            resetUI(view);

            return;
        }
        loadProfile(view, profileDataList.get(currentProfileIndex));
    }

    private void resetUI(View view) {
        // Đặt các giá trị về trạng thái mặc định
        TextView tvNameAge = view.findViewById(R.id.tvNameAge);
        TextView tvAddress = view.findViewById(R.id.tvAddress);
        tvNameAge.setText(",0");
        tvAddress.setText("");

        // Reset lại ViewPager
        if (viewPager != null) {
            viewPager.setAdapter(null);
        }

        // Cập nhật giao diện với dữ liệu mặc định (nếu có)
        // Ví dụ: bạn có thể đặt background hoặc hiển thị thông báo nếu cần.
    }
    private void loadProfile(View view, ProfileData data) {
        List<String> imageUrls = new ArrayList<>();
        String[] pics = {data.getPic1(), data.getPic2(), data.getPic3(), data.getPic4(),
                data.getPic5(), data.getPic6(), data.getPic7(), data.getPic8(), data.getPic9()};

        for (String pic : pics) {
            if (pic != null && !pic.isEmpty()) {
                imageUrls.add(pic);
            }
        }

        if (imageUrls.isEmpty()) {
            imageUrls.add("");
        }

        adapter = new ImageAdapter(requireContext(), imageUrls);
        viewPager.setAdapter(adapter);

        TextView tvNameAge = view.findViewById(R.id.tvNameAge);
        TextView tvAddress = view.findViewById(R.id.tvAddress);
        String fullName = data.getFirstName() + " " + data.getLastName();
        tvNameAge.setText(fullName + ", " + data.getAge());
        tvAddress.setText(data.getProvince() != null ? data.getProvince() : "Không xác định");
    }

    private void showDetailsBottomSheet() {
        if (profileDataList.isEmpty() || currentProfileIndex >= profileDataList.size()) {
            return;
        }

        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(requireContext());
        View view = getLayoutInflater().inflate(R.layout.layout_details, null);
        bottomSheetDialog.setContentView(view);

        ProfileData data = profileDataList.get(currentProfileIndex);

        TextView bioText = view.findViewById(R.id.bio_text);
        bioText.setText(data.getBio() != null ? data.getBio() : "Chưa có tiểu sử");

        ((TextView) view.findViewById(R.id.gender_text)).setText(data.getGender() != null ? data.getGender() : "Không xác định");
        ((TextView) view.findViewById(R.id.height_text)).setText(data.getHeight() > 0 ? data.getHeight() + " cm" : "Không xác định");
        ((TextView) view.findViewById(R.id.zodiac_text)).setText(data.getZodiacSign() != null ? data.getZodiacSign() : "Không xác định");
        ((TextView) view.findViewById(R.id.personality_text)).setText(data.getPersonalityType() != null ? data.getPersonalityType() : "Không xác định");

        ((TextView) view.findViewById(R.id.communication_text)).setText(data.getCommunicationStyle() != null ? data.getCommunicationStyle() : "Không xác định");
        ((TextView) view.findViewById(R.id.love_language_text)).setText(data.getLoveLanguage() != null ? data.getLoveLanguage() : "Không xác định");
        ((TextView) view.findViewById(R.id.pet_text)).setText(data.getPetPreference() != null ? data.getPetPreference() : "Không xác định");

        String hobbies = data.getHobbies() != null ? String.join(", ", data.getHobbies()) : "Không có sở thích";
        ((TextView) view.findViewById(R.id.hobbies_text)).setText(hobbies);

        ((TextView) view.findViewById(R.id.drinking_text)).setText(data.getDrinkingHabit() != null ? data.getDrinkingHabit() : "Không xác định");
        ((TextView) view.findViewById(R.id.smoking_text)).setText(data.getSmokingHabit() != null ? data.getSmokingHabit() : "Không xác định");
        ((TextView) view.findViewById(R.id.sleep_text)).setText(data.getSleepingHabit() != null ? data.getSleepingHabit() : "Không xác định");

        bottomSheetDialog.show();
    }
}
