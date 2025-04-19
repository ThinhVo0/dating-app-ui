package com.example.datingapp.fragment;

import android.content.Context;
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
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.content.res.ResourcesCompat;
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
    private float initialX, initialY;
    private boolean isDragging = false;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Use the updated layout
        return inflater.inflate(R.layout.fragment_match, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        SharedPreferences sharedPreferences = requireContext().getSharedPreferences("MyPrefs", Context.MODE_PRIVATE);
        authToken = sharedPreferences.getString("authToken", null);

        viewPager = view.findViewById(R.id.viewPager);
        viewPager.setUserInputEnabled(false); // Disable default swipe

        btnDislike = view.findViewById(R.id.btnDislike);
        btnLike = view.findViewById(R.id.btnLike);
        ImageButton btnChat = view.findViewById(R.id.btnChat);
        ImageButton btnViewDetails = view.findViewById(R.id.btnViewDetails);

        btnDislike.setOnClickListener(v -> performSwipe(view, false));
        btnLike.setOnClickListener(v -> performSwipe(view, true));
        btnChat.setOnClickListener(v -> Toast.makeText(requireContext(), "Chat clicked!", Toast.LENGTH_SHORT).show());
        btnViewDetails.setOnClickListener(v -> showDetailsBottomSheet());

        // Handle touch for swipe and image navigation on the entire profile card
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

                        // Move card
                        profileCard.setTranslationX(deltaX);
                        profileCard.setTranslationY(deltaY);

                        // Rotate card
                        float rotation = deltaX / profileCard.getWidth() * 30; // Max 30 degrees
                        profileCard.setRotation(rotation);

                        // Fade card
                        float alpha = 1.0f - Math.abs(deltaX) / (profileCard.getWidth() * 0.7f);
                        profileCard.setAlpha(Math.max(0.4f, Math.min(1.0f, alpha)));

                        // Scale buttons
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
                        float threshold = viewWidth * 0.5f; // 50% threshold

                        // Reset button scales
                        btnDislike.setScaleX(1.0f);
                        btnDislike.setScaleY(1.0f);
                        btnLike.setScaleX(1.0f);
                        btnLike.setScaleY(1.0f);

                        // Check if it's a swipe or a tap
                        float deltaX = event.getRawX() - initialX;
                        float deltaY = event.getRawY() - initialY;
                        if (Math.abs(deltaX) < 20 && Math.abs(deltaY) < 20) {
                            // Handle tap for image navigation
                            handleImageNavigation(event.getX(), viewWidth);
                        } else if (Math.abs(translationX) > threshold) {
                            // Perform swipe if dragged beyond threshold
                            boolean isSwipeRight = translationX > 0;
                            performSwipe(view, isSwipeRight);
                        } else {
                            // Animate back to original position
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

        initializeProfileList(view);
    }

    private void handleImageNavigation(float touchX, float viewWidth) {
        int currentItem = viewPager.getCurrentItem();
        int itemCount = adapter.getItemCount();

        // Left half: previous image, right half: next image
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

        String profileId = profileIds.get(currentProfileIndex);
        View profileCard = view.findViewById(R.id.profileCard);
        ImageButton button = isSwipeRight ? btnLike : btnDislike;

        Animation scaleUp = AnimationUtils.loadAnimation(getContext(), R.anim.scale_up);
        scaleUp.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {}

            @Override
            public void onAnimationEnd(Animation animation) {
                // Swipe animation
                profileCard.animate()
                        .translationX(isSwipeRight ? profileCard.getWidth() : -profileCard.getWidth())
                        .translationY(0)
                        .rotation(isSwipeRight ? 30 : -30)
                        .alpha(0)
                        .setDuration(400)
                        .setInterpolator(new AccelerateDecelerateInterpolator())
                        .withEndAction(() -> {
                            // Call API
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

                            // Reset position and load next profile
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
        if (currentProfileIndex >= profileDataList.size()) {
            Toast.makeText(requireContext(), "Đã hết profile!", Toast.LENGTH_SHORT).show();
            profileDataList.clear();
            currentProfileIndex = 0;
            resetUI(view);
            return;
        }
        loadProfile(view, profileDataList.get(currentProfileIndex));
    }

    private void resetUI(View view) {
        TextView tvNameAge = view.findViewById(R.id.tvNameAge);
        TextView tvAddress = view.findViewById(R.id.tvAddress);
        tvNameAge.setText("User not found");
        tvAddress.setText("");

        if (viewPager != null) {
            viewPager.setAdapter(null);
        }
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
            imageUrls.add(""); // Placeholder for empty images
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

        // Cài đặt các trường khác
        TextView bioText = view.findViewById(R.id.bio_text);
        bioText.setText(data.getBio() != null ? data.getBio() : "Chưa có tiểu sử");

        ((TextView) view.findViewById(R.id.gender_text)).setText(data.getGender() != null ? data.getGender() : "Không xác định");
        ((TextView) view.findViewById(R.id.height_text)).setText(data.getHeight() > 0 ? data.getHeight() + " cm" : "Không xác định");
        ((TextView) view.findViewById(R.id.zodiac_text)).setText(data.getZodiacSign() != null ? data.getZodiacSign() : "Không xác định");
        ((TextView) view.findViewById(R.id.personality_text)).setText(data.getPersonalityType() != null ? data.getPersonalityType() : "Không xác định");

        ((TextView) view.findViewById(R.id.communication_text)).setText(data.getCommunicationStyle() != null ? data.getCommunicationStyle() : "Không xác định");
        ((TextView) view.findViewById(R.id.love_language_text)).setText(data.getLoveLanguage() != null ? data.getLoveLanguage() : "Không xác định");
        ((TextView) view.findViewById(R.id.pet_text)).setText(data.getPetPreference() != null ? data.getPetPreference() : "Không xác định");

        // Xử lý sở thích động với FlexboxLayout
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

                // Chuyển đổi padding từ dp sang pixel
                int paddingHorizontal = (int) (8 * density);
                int paddingVertical = (int) (4 * density);
                hobbyTextView.setPadding(paddingHorizontal, paddingVertical, paddingHorizontal, paddingVertical);

                // Thiết lập layout params cho Flexbox
                com.google.android.flexbox.FlexboxLayout.LayoutParams params =
                        new com.google.android.flexbox.FlexboxLayout.LayoutParams(
                                ViewGroup.LayoutParams.WRAP_CONTENT,
                                ViewGroup.LayoutParams.WRAP_CONTENT
                        );
                params.setMargins(0, 0, (int) (12 * density), (int) (12 * density)); // Margin phải 12dp, dưới 12dp
                hobbyTextView.setLayoutParams(params);

                hobbiesContainer.addView(hobbyTextView);
            }
        } else {
            TextView noHobbiesText = new TextView(requireContext());
            noHobbiesText.setText("Không có sở thích");
            noHobbiesText.setTextSize(14);
            noHobbiesText.setTypeface(ResourcesCompat.getFont(requireContext(), R.font.roboto));
            noHobbiesText.setBackgroundResource(R.drawable.modern_bubble_background);

            // Chuyển đổi padding từ dp sang pixel
            int paddingHorizontal = (int) (8 * density);
            int paddingVertical = (int) (4 * density);
            noHobbiesText.setPadding(paddingHorizontal, paddingVertical, paddingHorizontal, paddingVertical);

            com.google.android.flexbox.FlexboxLayout.LayoutParams params =
                    new com.google.android.flexbox.FlexboxLayout.LayoutParams(
                            ViewGroup.LayoutParams.WRAP_CONTENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT
                    );
            params.setMargins(0, 0, (int) (12 * density), (int) (12 * density)); // Margin phải 12dp, dưới 12dp
            noHobbiesText.setLayoutParams(params);

            hobbiesContainer.addView(noHobbiesText);
        }

        ((TextView) view.findViewById(R.id.drinking_text)).setText(data.getDrinkingHabit() != null ? data.getDrinkingHabit() : "Không xác định");
        ((TextView) view.findViewById(R.id.smoking_text)).setText(data.getSmokingHabit() != null ? data.getSmokingHabit() : "Không xác định");
        ((TextView) view.findViewById(R.id.sleep_text)).setText(data.getSleepingHabit() != null ? data.getSleepingHabit() : "Không xác định");

        bottomSheetDialog.show();
    }
}