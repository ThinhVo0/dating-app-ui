package com.example.datingapp.fragment;

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
import com.example.datingapp.model.ProfileData;
import com.example.datingapp.model.ProfileDetailResponse;
import com.example.datingapp.model.ProfileResponse;
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
    SharedPreferences sharedPreferences = requireContext().getSharedPreferences("MyPrefs", requireContext().MODE_PRIVATE);
    
    private final String authToken = sharedPreferences.getString("authToken", null);
    private static final int SWIPE_THRESHOLD = 100;
    private static final int SWIPE_VELOCITY_THRESHOLD = 100;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_match, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

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
        AuthService authService = RetrofitClient.getClient().create(AuthService.class);
        Call<ProfileResponse> call = authService.getProfileIds(authToken);
        call.enqueue(new Callback<ProfileResponse>() {
            @Override
            public void onResponse(Call<ProfileResponse> call, Response<ProfileResponse> response) {
                if (response.isSuccessful() && response.body() != null && response.body().getStatus() == 200) {
                    profileIds = response.body().getData();
                    loadAllProfiles(view);
                } else {
                    Toast.makeText(requireContext(), "Failed to load profile IDs", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ProfileResponse> call, Throwable t) {
                Log.e(TAG, "Error loading profile IDs: " + t.getMessage());
                Toast.makeText(requireContext(), "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadAllProfiles(View view) {
        AuthService authService = RetrofitClient.getClient().create(AuthService.class);
        for (String id : profileIds) {
            Call<ProfileDetailResponse> call = authService.getProfileDetail(authToken, id);
            call.enqueue(new Callback<ProfileDetailResponse>() {
                @Override
                public void onResponse(Call<ProfileDetailResponse> call, Response<ProfileDetailResponse> response) {
                    if (response.isSuccessful() && response.body() != null && response.body().getStatus() == 200) {
                        profileDataList.add(response.body().getData());
                        if (profileDataList.size() == 1) {
                            loadProfile(view, profileDataList.get(0));
                        }
                    }
                }

                @Override
                public void onFailure(Call<ProfileDetailResponse> call, Throwable t) {
                    Log.e(TAG, "Error loading profile: " + t.getMessage());
                }
            });
        }
    }

    private void performSwipe(View view, boolean isSwipeRight) {
        Toast.makeText(requireContext(), isSwipeRight ? "Liked!" : "Disliked!", Toast.LENGTH_SHORT).show();

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
                    public void onAnimationStart(Animation animation) {}

                    @Override
                    public void onAnimationEnd(Animation animation) {
                        nextProfile(view);
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
            currentProfileIndex = 0;
            Toast.makeText(requireContext(), "Hết profile, quay lại đầu!", Toast.LENGTH_SHORT).show();
        }
        if (!profileDataList.isEmpty()) {
            loadProfile(view, profileDataList.get(currentProfileIndex));
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
        if (profileDataList.isEmpty() || currentProfileIndex >= profileDataList.size()) return;

        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(requireContext());
        View view = getLayoutInflater().inflate(R.layout.layout_details, null);
        bottomSheetDialog.setContentView(view);

        ProfileData data = profileDataList.get(currentProfileIndex);

        try {
            // Tiểu sử
            TextView bioText = view.findViewById(R.id.bio_text);
            bioText.setText(data.getBio() != null ? data.getBio() : "Chưa có tiểu sử");

            // Thông tin chính
            TextView genderText = view.findViewById(R.id.gender_text);
            TextView heightText = view.findViewById(R.id.height_text);
            TextView zodiacText = view.findViewById(R.id.zodiac_text);
            TextView personalityText = view.findViewById(R.id.personality_text);

            genderText.setText(data.getGender() != null ? data.getGender() : "Không xác định");
            heightText.setText(data.getHeight() > 0 ? data.getHeight() + " cm" : "Không xác định");
            zodiacText.setText(data.getZodiacSign() != null ? data.getZodiacSign() : "Không xác định");
            personalityText.setText(data.getPersonalityType() != null ? data.getPersonalityType() : "Không xác định");

            // Thông tin thêm
            TextView communicationText = view.findViewById(R.id.communication_text);
            TextView loveLanguageText = view.findViewById(R.id.love_language_text);
            TextView petText = view.findViewById(R.id.pet_text);
            TextView hobbiesText = view.findViewById(R.id.hobbies_text);

            communicationText.setText(data.getCommunicationStyle() != null ? data.getCommunicationStyle() : "Không xác định");
            loveLanguageText.setText(data.getLoveLanguage() != null ? data.getLoveLanguage() : "Không xác định");
            petText.setText(data.getPetPreference() != null ? data.getPetPreference() : "Không xác định");
            String hobbies = data.getHobbies() != null ? String.join(", ", data.getHobbies()) : "Không có sở thích";
            hobbiesText.setText(hobbies);

            // Phong cách sống
            TextView drinkingText = view.findViewById(R.id.drinking_text);
            TextView smokingText = view.findViewById(R.id.smoking_text);
            TextView sleepText = view.findViewById(R.id.sleep_text);

            drinkingText.setText(data.getDrinkingHabit() != null ? data.getDrinkingHabit() : "Không xác định");
            smokingText.setText(data.getSmokingHabit() != null ? data.getSmokingHabit() : "Không xác định");
            sleepText.setText(data.getSleepingHabit() != null ? data.getSleepingHabit() : "Không xác định");

        } catch (Exception e) {
            Log.e(TAG, "Error loading details: " + e.getMessage());
            Toast.makeText(requireContext(), "Error loading details", Toast.LENGTH_SHORT).show();
        }

        bottomSheetDialog.show();
    }
}