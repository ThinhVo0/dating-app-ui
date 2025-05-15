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
import android.widget.RadioButton;
import android.widget.RadioGroup;
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
    private boolean isGuest;
    private float initialX, initialY;
    private boolean isDragging = false;
    private LinearLayout imageIndicatorContainer;
    private List<View> indicatorViews = new ArrayList<>();

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
        isGuest = sharedPreferences.getBoolean("isGuest", false);

        viewPager = view.findViewById(R.id.viewPager);
        viewPager.setUserInputEnabled(false);

        btnDislike = view.findViewById(R.id.btnDislike);
        btnLike = view.findViewById(R.id.btnLike);
        ImageButton btnChat = view.findViewById(R.id.btnChat);
        ImageButton btnViewDetails = view.findViewById(R.id.btnViewDetails);
        imageIndicatorContainer = view.findViewById(R.id.imageIndicatorContainer);

        btnDislike.setOnClickListener(v -> {
            if (isGuest) {
                redirectToLogin();
            } else {
                performSwipe(view, false);
            }
        });

        btnLike.setOnClickListener(v -> {
            if (isGuest) {
                redirectToLogin();
            } else {
                performSwipe(view, true);
            }
        });

        btnChat.setOnClickListener(v -> {
            if (isGuest) {
                redirectToLogin();
            } else {
                Toast.makeText(requireContext(), "Chat clicked!", Toast.LENGTH_SHORT).show();
            }
        });

        btnViewDetails.setOnClickListener(v -> {
            if (isGuest) {
                redirectToLogin();
            } else {
                showDetailsBottomSheet();
            }
        });

        View profileCard = view.findViewById(R.id.profileCard);
        profileCard.setOnTouchListener((v, event) -> {
            if (isGuest) {
                redirectToLogin();
                return true;
            }
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

        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                updateImageIndicator(position);
            }
        });

        if (isGuest) {
            loadGuestProfile(view);
        } else if (authToken == null) {
            Toast.makeText(requireContext(), "Vui lòng đăng nhập để tiếp tục", Toast.LENGTH_LONG).show();
            startActivity(new Intent(requireContext(), LoginActivity.class));
            requireActivity().finish();
        } else {
            initializeProfileList(view);
        }
    }

    private void redirectToLogin() {
        Toast.makeText(requireContext(), "Vui lòng đăng nhập để thực hiện hành động này", Toast.LENGTH_SHORT).show();
        SharedPreferences.Editor editor = requireContext().getSharedPreferences("MyPrefs", Context.MODE_PRIVATE).edit();
        editor.putBoolean("isGuest", false);
        editor.apply();
        startActivity(new Intent(requireContext(), LoginActivity.class));
        requireActivity().finish();
    }

    private void loadGuestProfile(View view) {
        ProfileData guestProfile = ProfileData.builder()
                .firstName("Khách")
                .lastName("")
                .age(25)
                .province("Hà Nội")
                .pic1("https://dongvat.edu.vn/upload/2025/01/meo-cute-meme-69.webp")
                .build();

        profileIds.clear();
        profileDataMap.clear();
        String guestId = "guest_id";
        profileIds.add(guestId);
        profileDataMap.put(guestId, guestProfile);

        loadProfile(view, guestId);
    }

    @Override
    public void onResume() {
        super.onResume();
        View view = getView();
        if (view != null && !isGuest) {
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

        if (count <= 1) return;

        float density = getResources().getDisplayMetrics().density;
        int indicatorHeight = (int) (4 * density);
        int indicatorMargin = (int) (2 * density);

        int containerWidth = imageIndicatorContainer.getWidth();
        if (containerWidth == 0) {
            containerWidth = viewPager.getWidth() - (int) (32 * density);
        }

        int totalMargin = indicatorMargin * (count - 1);
        int indicatorWidth = (containerWidth - totalMargin) / count;

        for (int i = 0; i < count; i++) {
            View indicator = new View(requireContext());
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(indicatorWidth, indicatorHeight);
            params.setMargins(indicatorMargin, 0, indicatorMargin, 0);
            indicator.setLayoutParams(params);
            indicator.setBackgroundColor(i == 0 ? 0xFFFFFFFF : 0x80FFFFFF);
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
        if (isGuest) {
            redirectToLogin();
            return;
        }
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
        if (isGuest) {
            redirectToLogin();
            return;
        }
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
            if (!isGuest) {
                initializeProfileList(view);
            }
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

        viewPager.post(() -> {
            setupImageIndicators(imageUrls.size());
            updateImageIndicator(0);
        });

        TextView tvNameAge = view.findViewById(R.id.tvNameAge);
        TextView tvAddress = view.findViewById(R.id.tvAddress);
        String fullName = data.getFirstName() + " " + data.getLastName();
        tvNameAge.setText(fullName + ", " + data.getAge());
        tvAddress.setText(data.getProvince() != null ? data.getProvince() : "Không xác định");
    }

    private void showDetailsBottomSheet() {
        if (isGuest) {
            redirectToLogin();
            return;
        }
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

        Button reportButton = view.findViewById(R.id.buttonReport);
        reportButton.setOnClickListener(v -> {
            if (isGuest) {
                redirectToLogin();
                return;
            }
            bottomSheetDialog.dismiss();
            showReportDialog();
        });

        bottomSheetDialog.show();
    }

    private void showReportDialog() {
        // Inflate custom layout for report dialog
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_report_user, null);

        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Báo cáo người dùng");
        builder.setView(dialogView);

        // Initialize views
        RadioGroup reportReasonGroup = dialogView.findViewById(R.id.reportReasonGroup);
        EditText customReasonInput = dialogView.findViewById(R.id.customReasonInput);
        RadioButton customReasonRadio = dialogView.findViewById(R.id.radioCustomReason);

        // Handle showing/hiding the custom reason input field
        reportReasonGroup.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.radioCustomReason) {
                customReasonInput.setVisibility(View.VISIBLE);
                customReasonInput.requestFocus();
            } else {
                customReasonInput.setVisibility(View.GONE);
            }
        });

        // Create and show dialog
        AlertDialog alertDialog = builder.create();

        // Set up buttons
        dialogView.findViewById(R.id.btnCancel).setOnClickListener(v -> alertDialog.dismiss());

        dialogView.findViewById(R.id.btnSubmit).setOnClickListener(v -> {
            String reason;
            int selectedId = reportReasonGroup.getCheckedRadioButtonId();

            if (selectedId == -1) {
                Toast.makeText(requireContext(), "Vui lòng chọn lý do báo cáo", Toast.LENGTH_SHORT).show();
                return;
            }

            if (selectedId == R.id.radioCustomReason) {
                reason = customReasonInput.getText().toString().trim();
                if (reason.isEmpty()) {
                    Toast.makeText(requireContext(), "Vui lòng nhập lý do báo cáo", Toast.LENGTH_SHORT).show();
                    return;
                }
            } else {
                RadioButton selectedRadioButton = dialogView.findViewById(selectedId);
                reason = selectedRadioButton.getText().toString();
            }

            // Show confirmation before submitting
            new AlertDialog.Builder(requireContext())
                    .setTitle("Xác nhận báo cáo")
                    .setMessage("Bạn chắc chắn muốn báo cáo người dùng này với lý do: \"" + reason + "\"?")
                    .setPositiveButton("Xác nhận", (dialog, which) -> submitReport(reason, alertDialog))
                    .setNegativeButton("Hủy", null)
                    .show();
        });

        alertDialog.show();
    }

    private void submitReport(String reason, AlertDialog parentDialog) {
        // Show loading progress
        View loadingView = getLayoutInflater().inflate(R.layout.dialog_loading, null);
        AlertDialog loadingDialog = new AlertDialog.Builder(requireContext())
                .setView(loadingView)
                .setCancelable(false)
                .create();
        loadingDialog.show();

        // Call API to report user
        AuthService authService = RetrofitClient.getClient().create(AuthService.class);
        String reportedUserId = profileIds.get(currentProfileIndex);
        ReportDto reportDto = new ReportDto(reportedUserId, reason);
        Log.d(TAG, "Sending report: " + new Gson().toJson(reportDto));
        Call<ApiResponse<Report>> call = authService.sendReport("Bearer " + authToken, reportDto);

        call.enqueue(new Callback<ApiResponse<Report>>() {
            @Override
            public void onResponse(Call<ApiResponse<Report>> call, Response<ApiResponse<Report>> response) {
                loadingDialog.dismiss();
                parentDialog.dismiss();

                Log.d(TAG, "HTTP Status Code (sendReport): " + response.code());
                if (response.isSuccessful() && response.body() != null && response.body().getStatus() == 200) {
                    String message = response.body().getMessage() != null ? response.body().getMessage() : "Báo cáo thành công";
                    Log.d(TAG, "Report successful: " + message);

                    // Show success dialog
                    new AlertDialog.Builder(requireContext())
                            .setTitle("Báo cáo thành công")
                            .setMessage("Cảm ơn bạn đã báo cáo. Chúng tôi sẽ xem xét nhanh chóng.")
                            .setPositiveButton("Đóng", (dialog, which) -> {
                                profileIds.remove(currentProfileIndex);
                                profileDataMap.remove(reportedUserId);
                                nextProfile(getView());
                            })
                            .setCancelable(false)
                            .show();
                } else {
                    if (response.code() == 401) {
                        Toast.makeText(requireContext(), "Phiên đăng nhập hết hạn, vui lòng đăng nhập lại", Toast.LENGTH_LONG).show();
                        return;
                    }
                    String errorMessage = "Gửi báo cáo thất bại";
                    if (response.errorBody() != null) {
                        try {
                            errorMessage = response.errorBody().string();
                            Log.e(TAG, "Error response: " + errorMessage);
                        } catch (Exception e) {
                            Log.e(TAG, "Error parsing errorBody: " + e.getMessage(), e);
                        }
                    } else if (response.body() != null) {
                        Log.e(TAG, "Response body: " + new Gson().toJson(response.body()));
                        errorMessage = "Status: " + response.body().getStatus() + ", Message: " + response.body().getMessage();
                    }

                    // Show error dialog
                    new AlertDialog.Builder(requireContext())
                            .setTitle("Lỗi")
                            .setMessage("Không thể gửi báo cáo: " + errorMessage)
                            .setPositiveButton("Đóng", null)
                            .show();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<Report>> call, Throwable t) {
                loadingDialog.dismiss();
                parentDialog.dismiss();

                Log.e(TAG, "Report API call failed: " + t.getMessage(), t);

                // Show error dialog
                new AlertDialog.Builder(requireContext())
                        .setTitle("Lỗi kết nối")
                        .setMessage("Không thể gửi báo cáo: " + t.getMessage())
                        .setPositiveButton("Thử lại", (dialog, which) -> showReportDialog())
                        .setNegativeButton("Hủy", null)
                        .show();
            }
        });
    }
}