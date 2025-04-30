package com.example.datingapp.fragment;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.content.res.ResourcesCompat;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.widget.ViewPager2;

import com.example.datingapp.R;
import com.example.datingapp.activity.LoginActivity;
import com.example.datingapp.adapter.ImageAdapter;
import com.example.datingapp.dto.request.ReportDto;
import com.example.datingapp.dto.response.ApiResponse;
import com.example.datingapp.dto.response.ProfileResponse;
import com.example.datingapp.model.ProfileActionResponse;
import com.example.datingapp.model.ProfileData;
import com.example.datingapp.model.ProfileDetailResponse;
import com.example.datingapp.model.Report;
import com.example.datingapp.network.AuthService;
import com.example.datingapp.network.RetrofitClient;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ProfileFragment extends Fragment {
    private static final String TAG = "ProfileFragment";
    private ViewPager2 viewPager;
    private ImageAdapter adapter;
    private List<String> profileIds = new ArrayList<>();
    private Map<String, ProfileData> profileDataMap = new HashMap<>();
    private int currentProfileIndex = 0;
    private ImageButton btnDislike;
    private ImageButton btnLike;
    private String authToken;
    private float initialX, initialY;
    private boolean isDragging = false;
    private LinearLayout imageIndicatorContainer; // Container cho các thanh ngang
    private List<View> indicatorViews = new ArrayList<>(); // Danh sách các thanh ngang

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_match, container, false);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt("currentProfileIndex", currentProfileIndex);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (savedInstanceState != null) {
            currentProfileIndex = savedInstanceState.getInt("currentProfileIndex", 0);
        }

        SharedPreferences sharedPreferences = requireContext().getSharedPreferences("MyPrefs", Context.MODE_PRIVATE);
        authToken = sharedPreferences.getString("authToken", null);

        viewPager = view.findViewById(R.id.viewPager);
        viewPager.setUserInputEnabled(false);

        btnDislike = view.findViewById(R.id.btnDislike);
        btnLike = view.findViewById(R.id.btnLike);
        ImageButton btnChat = view.findViewById(R.id.btnChat);
        ImageButton btnViewDetails = view.findViewById(R.id.btnViewDetails);
        imageIndicatorContainer = view.findViewById(R.id.imageIndicatorContainer); // Khởi tạo container

        btnDislike.setOnClickListener(v -> performSwipe(view, false));
        btnLike.setOnClickListener(v -> performSwipe(view, true));
        btnChat.setOnClickListener(v -> Toast.makeText(requireContext(), "Chat clicked!", Toast.LENGTH_SHORT).show());
        btnViewDetails.setOnClickListener(v -> showDetailsBottomSheet());

        View profileCard = view.findViewById(R.id.profileCard);
        profileCard.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    initialX = event.getRawX();
                    initialY = event.getRawY();
                    isDragging = true;
                    v.getParent().requestDisallowInterceptTouchEvent(true);
                    return true;

                case MotionEvent.ACTION_MOVE:
                    if (isDragging) {
                        float deltaX = event.getRawX() - initialX;
                        float deltaY = event.getRawY() - initialY;

                        profileCard.setTranslationX(deltaX);
                        profileCard.setTranslationY(deltaY);

                        float rotation = deltaX / profileCard.getWidth() * 30;
                        profileCard.setRotation(rotation);

                        float alpha = 1.0f - Math.abs(deltaX) / (profileCard.getWidth() * 0.7f);
                        profileCard.setAlpha(Math.max(0.4f, Math.min(1.0f, alpha)));

                        float scale = 1.0f + Math.min(Math.abs(deltaX) / profileCard.getWidth(), 0.3f);
                        if (deltaX < 0) {
                            btnDislike.setScaleX(scale);
                            btnDislike.setScaleY(scale);
                            btnLike.setScaleX(1.0f);
                            btnLike.setScaleY(1.0f);
                        } else if (deltaX > 0) {
                            btnLike.setScaleX(scale);
                            btnLike.setScaleY(scale);
                            btnDislike.setScaleX(1.0f);
                            btnDislike.setScaleY(1.0f);
                        }
                    }
                    return true;

                case MotionEvent.ACTION_UP:
                    if (isDragging) {
                        isDragging = false;
                        v.getParent().requestDisallowInterceptTouchEvent(false);

                        float translationX = profileCard.getTranslationX();
                        float viewWidth = profileCard.getWidth();
                        float threshold = viewWidth * 0.5f;

                        btnDislike.setScaleX(1.0f);
                        btnDislike.setScaleY(1.0f);
                        btnLike.setScaleX(1.0f);
                        btnLike.setScaleY(1.0f);

                        float deltaX = event.getRawX() - initialX;
                        float deltaY = event.getRawY() - initialY;
                        if (Math.abs(deltaX) < 20 && Math.abs(deltaY) < 20) {
                            handleImageNavigation(event.getX(), viewWidth);
                        } else if (Math.abs(translationX) > threshold) {
                            boolean isSwipeRight = translationX > 0;
                            performSwipe(view, isSwipeRight);
                        } else {
                            profileCard.animate()
                                    .translationX(0)
                                    .translationY(0)
                                    .rotation(0)
                                    .alpha(1.0f)
                                    .setDuration(300)
                                    .setInterpolator(new AccelerateDecelerateInterpolator())
                                    .start();
                        }
                    }
                    return true;
            }
            return false;
        });

        // Đăng ký callback để cập nhật thanh ngang chỉ số ảnh
        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                updateImageIndicator(position);
            }
        });

        // Kiểm tra trạng thái đăng nhập
        if (authToken == null) {
            Toast.makeText(requireContext(), "Vui lòng đăng nhập để tiếp tục", Toast.LENGTH_LONG).show();
            startActivity(new Intent(requireContext(), LoginActivity.class));
            requireActivity().finish();
            return;
        }

        // Tải danh sách profile
        initializeProfileList(view);
    }

    @Override
    public void onResume() {
        super.onResume();
        // Làm mới danh sách profile khi quay lại từ FilterFragment
        View view = getView();
        if (view != null) {
            initializeProfileList(view);
        }
    }

    private void handleImageNavigation(float touchX, float viewWidth) {
        if (adapter == null) return;
        int currentItem = viewPager.getCurrentItem();
        int itemCount = adapter.getItemCount();

        if (touchX < viewWidth / 2) {
            if (currentItem > 0) {
                viewPager.setCurrentItem(currentItem - 1, true);
            }
        } else {
            if (currentItem < itemCount - 1) {
                viewPager.setCurrentItem(currentItem + 1, true);
            }
        }
    }

    private void setupImageIndicators(int count) {
        imageIndicatorContainer.removeAllViews();
        indicatorViews.clear();

        if (count <= 1) return; // Không hiển thị thanh ngang nếu chỉ có 1 ảnh

        float density = getResources().getDisplayMetrics().density;
        int indicatorHeight = (int) (4 * density); // Chiều cao mỗi thanh ngang (tăng lên 4dp)
        int indicatorMargin = (int) (2 * density); // Khoảng cách giữa các thanh (giảm xuống 2dp)

        // Tính chiều rộng của container (ViewPager2 width trừ padding)
        int containerWidth = imageIndicatorContainer.getWidth();
        if (containerWidth == 0) {
            // Nếu container chưa được đo, sử dụng chiều rộng của ViewPager2
            containerWidth = viewPager.getWidth() - (int) (32 * density); // Trừ padding 16dp mỗi bên
        }

        // Tính tổng khoảng cách giữa các thanh
        int totalMargin = indicatorMargin * (count - 1);
        // Tính chiều rộng mỗi thanh sao cho tổng chiều rộng các thanh và khoảng cách vừa với container
        int indicatorWidth = (containerWidth - totalMargin) / count;

        for (int i = 0; i < count; i++) {
            View indicator = new View(requireContext());
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(indicatorWidth, indicatorHeight);
            params.setMargins(indicatorMargin, 0, indicatorMargin, 0);
            indicator.setLayoutParams(params);
            indicator.setBackgroundColor(i == 0 ? 0xFFFFFFFF : 0x80FFFFFF); // Màu trắng cho thanh hiện tại, màu mờ cho các thanh khác
            imageIndicatorContainer.addView(indicator);
            indicatorViews.add(indicator);
        }
    }

    private void updateImageIndicator(int position) {
        for (int i = 0; i < indicatorViews.size(); i++) {
            indicatorViews.get(i).setBackgroundColor(i == position ? 0xFFFFFFFF : 0x80FFFFFF);
        }
    }

    private void initializeProfileList(View view) {
        profileIds.clear();
        profileDataMap.clear();
        currentProfileIndex = 0;

        AuthService authService = RetrofitClient.getClient().create(AuthService.class);

        SharedPreferences filterPrefs = requireContext().getSharedPreferences("FilterPrefs", Context.MODE_PRIVATE);
        String gender = filterPrefs.getString("gender", null);
        Integer minAge = filterPrefs.getInt("minAge", 18);
        Integer maxAge = filterPrefs.getInt("maxAge", 100);
        Double maxDistance = (double) filterPrefs.getFloat("maxDistance", 10.0f);

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
                        resetUI(view);
                    } else {
                        loadAllProfiles(view);
                    }
                } else {
                    Toast.makeText(requireContext(), "Không thể tải danh sách profile", Toast.LENGTH_SHORT).show();
                    resetUI(view);
                }
            }

            @Override
            public void onFailure(Call<ProfileResponse> call, Throwable t) {
                Toast.makeText(requireContext(), "Lỗi: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                resetUI(view);
            }
        });
    }

    private void loadAllProfiles(View view) {
        AuthService authService = RetrofitClient.getClient().create(AuthService.class);
        profileDataMap.clear();
        int[] loadedCount = {0};

        for (String id : profileIds) {
            Call<ProfileDetailResponse> call = authService.getProfileDetail("Bearer " + authToken, id);
            call.enqueue(new Callback<ProfileDetailResponse>() {
                @Override
                public void onResponse(Call<ProfileDetailResponse> call, Response<ProfileDetailResponse> response) {
                    loadedCount[0]++;
                    if (response.isSuccessful() && response.body() != null && response.body().getStatus() == 200) {
                        profileDataMap.put(id, response.body().getData());
                    } else {
                        Log.e(TAG, "Failed to load profile " + id);
                    }
                    if (loadedCount[0] == profileIds.size()) {
                        if (!profileDataMap.isEmpty()) {
                            currentProfileIndex = 0;
                            loadProfile(view, profileIds.get(0));
                        } else {
                            resetUI(view);
                        }
                    }
                }

                @Override
                public void onFailure(Call<ProfileDetailResponse> call, Throwable t) {
                    loadedCount[0]++;
                    Log.e(TAG, "Error loading profile " + id + ": " + t.getMessage());
                    Toast.makeText(requireContext(), "Lỗi tải profile: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                    if (loadedCount[0] == profileIds.size()) {
                        if (!profileDataMap.isEmpty()) {
                            currentProfileIndex = 0;
                            loadProfile(view, profileIds.get(0));
                        } else {
                            resetUI(view);
                        }
                    }
                }
            });
        }
    }

    private void performSwipe(View view, boolean isSwipeRight) {
        if (currentProfileIndex >= profileIds.size() || profileDataMap.isEmpty()) {
            Toast.makeText(requireContext(), "Không còn profile để swipe", Toast.LENGTH_SHORT).show();
            return;
        }

        String profileId = profileIds.get(currentProfileIndex);
        if (!profileDataMap.containsKey(profileId)) {
            Log.e(TAG, "Profile data not found for ID: " + profileId);
            Toast.makeText(requireContext(), "Lỗi đồng bộ profile, đang tải lại...", Toast.LENGTH_SHORT).show();
            initializeProfileList(view);
            return;
        }

        View profileCard = view.findViewById(R.id.profileCard);
        ImageButton button = isSwipeRight ? btnLike : btnDislike;

        Animation scaleUp = AnimationUtils.loadAnimation(getContext(), R.anim.scale_up);
        scaleUp.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {}

            @Override
            public void onAnimationEnd(Animation animation) {
                profileCard.animate()
                        .translationX(isSwipeRight ? profileCard.getWidth() : -profileCard.getWidth())
                        .translationY(0)
                        .rotation(isSwipeRight ? 30 : -30)
                        .alpha(0)
                        .setDuration(400)
                        .setInterpolator(new AccelerateDecelerateInterpolator())
                        .withEndAction(() -> {
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

                            profileCard.setTranslationX(0);
                            profileCard.setTranslationY(0);
                            profileCard.setRotation(0);
                            profileCard.setAlpha(1.0f);
                            btnDislike.setScaleX(1.0f);
                            btnDislike.setScaleY(1.0f);
                            btnLike.setScaleX(1.0f);
                            btnLike.setScaleY(1.0f);
                            nextProfile(view);
                            button.setScaleX(1.0f);
                            button.setScaleY(1.0f);
                        })
                        .start();
            }

            @Override
            public void onAnimationRepeat(Animation animation) {}
        });
        button.startAnimation(scaleUp);
    }

    private void nextProfile(View view) {
        currentProfileIndex++;
        if (currentProfileIndex >= profileIds.size()) {
            Toast.makeText(requireContext(), "Đã hết profile!", Toast.LENGTH_SHORT).show();
            profileIds.clear();
            profileDataMap.clear();
            currentProfileIndex = 0;
            resetUI(view);
            initializeProfileList(view);
            return;
        }
        loadProfile(view, profileIds.get(currentProfileIndex));
    }

    private void resetUI(View view) {
        TextView tvNameAge = view.findViewById(R.id.tvNameAge);
        TextView tvAddress = view.findViewById(R.id.tvAddress);
        tvNameAge.setText("User not found");
        tvAddress.setText("");

        // Xóa các thanh ngang khi reset UI
        imageIndicatorContainer.removeAllViews();
        indicatorViews.clear();

        if (viewPager != null) {
            viewPager.setAdapter(null);
        }
    }

    private void loadProfile(View view, String profileId) {
        if (!profileDataMap.containsKey(profileId)) {
            Log.e(TAG, "Profile data not found for ID: " + profileId);
            resetUI(view);
            initializeProfileList(view);
            return;
        }

        ProfileData data = profileDataMap.get(profileId);
        currentProfileIndex = profileIds.indexOf(profileId);

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

        // Thiết lập các thanh ngang chỉ số ảnh
        // Đợi layout hoàn thành để lấy kích thước chính xác
        viewPager.post(() -> {
            setupImageIndicators(imageUrls.size());
            updateImageIndicator(0); // Cập nhật thanh đầu tiên
        });

        TextView tvNameAge = view.findViewById(R.id.tvNameAge);
        TextView tvAddress = view.findViewById(R.id.tvAddress);
        String fullName = data.getFirstName() + " " + data.getLastName();
        tvNameAge.setText(fullName + ", " + data.getAge());
        tvAddress.setText(data.getProvince() != null ? data.getProvince() : "Không xác định");
    }

    private void showDetailsBottomSheet() {
        if (currentProfileIndex >= profileIds.size() || !profileDataMap.containsKey(profileIds.get(currentProfileIndex))) {
            return;
        }

        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(requireContext());
        View view = getLayoutInflater().inflate(R.layout.layout_details, null);
        bottomSheetDialog.setContentView(view);

        ProfileData data = profileDataMap.get(profileIds.get(currentProfileIndex));

        TextView bioText = view.findViewById(R.id.bio_text);
        bioText.setText(data.getBio() != null ? "\"" + data.getBio() + "\"" : "\"Chưa có tiểu sử\"");

        ((TextView) view.findViewById(R.id.gender_text)).setText(data.getGender() != null ? data.getGender() : "Không xác định");
        ((TextView) view.findViewById(R.id.height_text)).setText(data.getHeight() > 0 ? data.getHeight() + " cm" : "Không xác định");
        ((TextView) view.findViewById(R.id.zodiac_text)).setText(data.getZodiacSign() != null ? data.getZodiacSign() : "Không xác định");
        ((TextView) view.findViewById(R.id.personality_text)).setText(data.getPersonalityType() != null ? data.getPersonalityType() : "Không xác định");

        ((TextView) view.findViewById(R.id.communication_text)).setText(data.getCommunicationStyle() != null ? data.getCommunicationStyle() : "Không xác định");
        ((TextView) view.findViewById(R.id.love_language_text)).setText(data.getLoveLanguage() != null ? data.getLoveLanguage() : "Không xác định");
        ((TextView) view.findViewById(R.id.pet_text)).setText(data.getPetPreference() != null ? data.getPetPreference() : "Không xác định");

        com.google.android.flexbox.FlexboxLayout hobbiesContainer = view.findViewById(R.id.hobbies_container);
        List<String> hobbies = data.getHobbies();
        float density = getResources().getDisplayMetrics().density;
        if (hobbies != null && !hobbies.isEmpty()) {
            for (String hobby : hobbies) {
                TextView hobbyTextView = new TextView(requireContext());
                hobbyTextView.setText(hobby);
                hobbyTextView.setTextSize(14);
                hobbyTextView.setTypeface(ResourcesCompat.getFont(requireContext(), R.font.roboto));
                hobbyTextView.setBackgroundResource(R.drawable.modern_bubble_background);

                int paddingHorizontal = (int) (8 * density);
                int paddingVertical = (int) (4 * density);
                hobbyTextView.setPadding(paddingHorizontal, paddingVertical, paddingHorizontal, paddingVertical);

                com.google.android.flexbox.FlexboxLayout.LayoutParams params =
                        new com.google.android.flexbox.FlexboxLayout.LayoutParams(
                                ViewGroup.LayoutParams.WRAP_CONTENT,
                                ViewGroup.LayoutParams.WRAP_CONTENT
                        );
                params.setMargins(0, 0, (int) (12 * density), (int) (12 * density));
                hobbyTextView.setLayoutParams(params);

                hobbiesContainer.addView(hobbyTextView);
            }
        } else {
            TextView noHobbiesText = new TextView(requireContext());
            noHobbiesText.setText("Không có sở thích");
            noHobbiesText.setTextSize(14);
            noHobbiesText.setTypeface(ResourcesCompat.getFont(requireContext(), R.font.roboto));
            noHobbiesText.setBackgroundResource(R.drawable.modern_bubble_background);

            int paddingHorizontal = (int) (8 * density);
            int paddingVertical = (int) (4 * density);
            noHobbiesText.setPadding(paddingHorizontal, paddingVertical, paddingHorizontal, paddingVertical);

            com.google.android.flexbox.FlexboxLayout.LayoutParams params =
                    new com.google.android.flexbox.FlexboxLayout.LayoutParams(
                            ViewGroup.LayoutParams.WRAP_CONTENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT
                    );
            params.setMargins(0, 0, (int) (12 * density), (int) (12 * density));
            noHobbiesText.setLayoutParams(params);

            hobbiesContainer.addView(noHobbiesText);
        }

        ((TextView) view.findViewById(R.id.drinking_text)).setText(data.getDrinkingHabit() != null ? data.getDrinkingHabit() : "Không xác định");
        ((TextView) view.findViewById(R.id.smoking_text)).setText(data.getSmokingHabit() != null ? data.getSmokingHabit() : "Không xác định");
        ((TextView) view.findViewById(R.id.sleep_text)).setText(data.getSleepingHabit() != null ? data.getSleepingHabit() : "Không xác định");

        // Xử lý nút báo cáo
        Button reportButton = view.findViewById(R.id.buttonReport);
        reportButton.setOnClickListener(v -> {
            // Đóng bottom sheet
            bottomSheetDialog.dismiss();

            // Hiển thị dialog để nhập lý do báo cáo
            AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
            builder.setTitle("Báo cáo người dùng");

            // Thêm EditText để nhập lý do
            final EditText reasonInput = new EditText(requireContext());
            reasonInput.setHint("Nhập lý do báo cáo");
            reasonInput.setMinHeight((int) (48 * getResources().getDisplayMetrics().density));
            builder.setView(reasonInput);

            builder.setPositiveButton("Gửi", (dialog, which) -> {
                String reason = reasonInput.getText().toString().trim();
                if (reason.isEmpty()) {
                    Toast.makeText(requireContext(), "Vui lòng nhập lý do", Toast.LENGTH_SHORT).show();
                    return;
                }

                // Gọi API trực tiếp
                AuthService authService = RetrofitClient.getClient().create(AuthService.class);
                String reportedUserId = profileIds.get(currentProfileIndex);
                Log.d("ProfileFragment", "Auth Token: " + authToken);
                Log.d("ProfileFragment", "Reported User ID: " + reportedUserId);
                ReportDto reportDto = new ReportDto(reportedUserId, reason);
                Log.d("ProfileFragment", "Sending report: " + new Gson().toJson(reportDto));
                Call<ApiResponse<Report>> call = authService.sendReport("Bearer " + authToken, reportDto);

                call.enqueue(new Callback<ApiResponse<Report>>() {
                    @Override
                    public void onResponse(Call<ApiResponse<Report>> call, Response<ApiResponse<Report>> response) {
                        Log.d("ProfileFragment", "HTTP Status Code (sendReport): " + response.code());
                        if (response.isSuccessful() && response.body() != null && response.body().getStatus() == 200) {
                            String message = response.body().getMessage() != null ? response.body().getMessage() : "Báo cáo thành công";
                            Log.d("ProfileFragment", "Report successful: " + message);
                            Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();

                            // Loại bỏ profile đã báo cáo khỏi danh sách
                            String reportedProfileId = profileIds.get(currentProfileIndex);
                            Log.d("ProfileFragment", "Removing reported profile ID: " + reportedProfileId);
                            profileIds.remove(currentProfileIndex);
                            profileDataMap.remove(reportedProfileId);

                            // Gọi API skipProfile để đánh dấu người dùng đã bị skip
                            Call<ProfileActionResponse> skipCall = authService.skipProfile("Bearer " + authToken, reportedProfileId);
                            skipCall.enqueue(new Callback<ProfileActionResponse>() {
                                @Override
                                public void onResponse(Call<ProfileActionResponse> call, Response<ProfileActionResponse> response) {
                                    Log.d("ProfileFragment", "HTTP Status Code (skipProfile): " + response.code());
                                    if (response.isSuccessful() && response.body() != null && response.body().getStatus() == 200) {
                                        Log.d("ProfileFragment", "Skip successful: " + response.body().getData());
                                    } else {
                                        Log.e("ProfileFragment", "Failed to skip profile after report");
                                        if (response.errorBody() != null) {
                                            try {
                                                Log.e("ProfileFragment", "Skip error response: " + response.errorBody().string());
                                            } catch (Exception e) {
                                                Log.e("ProfileFragment", "Error parsing skip errorBody: " + e.getMessage(), e);
                                            }
                                        }
                                    }
                                    // Gọi nextProfile để chuyển sang profile tiếp theo
                                    Log.d("ProfileFragment", "Calling nextProfile to remove reported profile");
                                    nextProfile(getView());
                                }

                                @Override
                                public void onFailure(Call<ProfileActionResponse> call, Throwable t) {
                                    Log.e("ProfileFragment", "Skip API call failed: " + t.getMessage(), t);
                                    // Vẫn gọi nextProfile để đảm bảo giao diện được cập nhật
                                    Log.d("ProfileFragment", "Calling nextProfile despite skip failure");
                                    nextProfile(getView());
                                }
                            });
                        } else {
                            if (response.code() == 401) {
                                Toast.makeText(requireContext(), "Phiên đăng nhập hết hạn, vui lòng đăng nhập lại", Toast.LENGTH_LONG).show();
                                dialog.dismiss();
                                return;
                            }
                            String errorMessage = "Gửi báo cáo thất bại";
                            if (response.errorBody() != null) {
                                try {
                                    errorMessage = response.errorBody().string();
                                    Log.e("ProfileFragment", "Error response: " + errorMessage);
                                } catch (Exception e) {
                                    Log.e("ProfileFragment", "Error parsing errorBody: " + e.getMessage(), e);
                                }
                            } else if (response.body() != null) {
                                Log.e("ProfileFragment", "Response body: " + new Gson().toJson(response.body()));
                                errorMessage = "Status: " + response.body().getStatus() + ", Message: " + response.body().getMessage();
                            }
                            Toast.makeText(requireContext(), "Lỗi: " + errorMessage, Toast.LENGTH_LONG).show();
                            dialog.dismiss();
                        }
                    }

                    @Override
                    public void onFailure(Call<ApiResponse<Report>> call, Throwable t) {
                        Log.e("ProfileFragment", "Report API call failed: " + t.getMessage(), t);
                        Toast.makeText(requireContext(), "Lỗi: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                        dialog.dismiss();
                    }
                });
            });

            builder.setNegativeButton("Hủy", (dialog, which) -> dialog.cancel());
            builder.show();
        });

        bottomSheetDialog.show();
    }
}