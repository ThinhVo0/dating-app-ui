package com.example.datingapp.activity;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.bumptech.glide.Glide;
import com.example.datingapp.R;
import com.example.datingapp.dto.request.Album;
import com.example.datingapp.dto.response.ApiResponse;
import com.example.datingapp.enums.CommunicationStyle;
import com.example.datingapp.enums.DrinkingHabit;
import com.example.datingapp.enums.Gender;
import com.example.datingapp.enums.Hobbies;
import com.example.datingapp.enums.LoveLanguage;
import com.example.datingapp.enums.PersonalityType;
import com.example.datingapp.enums.PetPreference;
import com.example.datingapp.enums.SleepingHabit;
import com.example.datingapp.enums.SmokingHabit;
import com.example.datingapp.enums.ZodiacSign;
import com.example.datingapp.model.ProfileUpdateDTO;
import com.example.datingapp.network.AuthService;
import com.example.datingapp.network.RetrofitClient;
import com.example.datingapp.util.RealPathUtil;
import com.google.android.flexbox.FlexboxLayout;
import com.google.android.material.textfield.TextInputEditText;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ProfileUpdateActivity extends AppCompatActivity {

    private static final String TAG = "ProfileUpdateActivity";
    private TextInputEditText etFirstName, etLastName, etAge, etHeight, etBio;
    private RadioGroup rgGender;
    private FlexboxLayout flHobbies;
    private Spinner spZodiac, spPersonality, spCommunication, spLoveLanguage,
            spPetPreference, spDrinking, spSmoking, spSleeping;
    private Button btnSave;
    private ImageButton btnBack;
    private List<Hobbies> selectedHobbies = new ArrayList<>();
    private AuthService authService;
    private String authToken;
    private static final int STORAGE_PERMISSION_CODE = 100;
    private int currentImagePosition = -1;
    private SharedPreferences sharedPreferences;

    private final ActivityResultLauncher<Intent> imagePickerLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                Log.d(TAG, "imagePickerLauncher: Result received, resultCode = " + result.getResultCode());
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Intent data = result.getData();
                    Uri imageUri = data.getData();
                    if (imageUri != null) {
                        uploadImageToServer(imageUri, "pic" + currentImagePosition);
                        updateImageView(imageUri);
                    }
                } else {
                    Log.d(TAG, "imagePickerLauncher: Image selection failed or cancelled");
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile_update);

        // Setup Toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }

        // Initialize SharedPreferences
        sharedPreferences = getSharedPreferences("MyPrefs", MODE_PRIVATE);
        authToken = sharedPreferences.getString("authToken", null);
        Log.d(TAG, "onCreate: authToken = " + authToken);

        // Initialize views
        btnBack = findViewById(R.id.btnBack);
        etFirstName = findViewById(R.id.etFirstName);
        etLastName = findViewById(R.id.etLastName);
        etAge = findViewById(R.id.etAge);
        etHeight = findViewById(R.id.etHeight);
        etBio = findViewById(R.id.etBio);
        rgGender = findViewById(R.id.rgGender);
        flHobbies = findViewById(R.id.flHobbies);
        spZodiac = findViewById(R.id.spZodiac);
        spPersonality = findViewById(R.id.spPersonality);
        spCommunication = findViewById(R.id.spCommunication);
        spLoveLanguage = findViewById(R.id.spLoveLanguage);
        spPetPreference = findViewById(R.id.spPetPreference);
        spDrinking = findViewById(R.id.spDrinking);
        spSmoking = findViewById(R.id.spSmoking);
        spSleeping = findViewById(R.id.spSleeping);
        btnSave = findViewById(R.id.btnSave);
        Log.d(TAG, "onCreate: Views initialized");

        // Back button listener
        btnBack.setOnClickListener(v -> finish());

        // Initialize Retrofit service
        authService = RetrofitClient.getClient().create(AuthService.class);
        Log.d(TAG, "onCreate: Retrofit service initialized");

        // Setup adapters
        setupHobbiesFlexbox();
        setupSpinners();
        setupImageClickListeners();
        Log.d(TAG, "onCreate: Adapters and listeners set up");

        // Load profile data
        loadProfileData();

        // Setup save button listener
        btnSave.setOnClickListener(v -> {
            Log.d(TAG, "btnSave: Clicked");
            saveProfile();
        });

        Log.d(TAG, "onCreate: Completed");
    }

    private void loadProfileData() {
        // Load text fields
        etFirstName.setText(sharedPreferences.getString("firstName", ""));
        etLastName.setText(sharedPreferences.getString("lastName", ""));
        etAge.setText(String.valueOf(sharedPreferences.getInt("age", 0)));
        etHeight.setText(String.valueOf(sharedPreferences.getInt("height", 0)));
        etBio.setText(sharedPreferences.getString("bio", ""));

        // Load gender
        String gender = sharedPreferences.getString("gender", "");
        if ("Nam".equals(gender)) {
            rgGender.check(R.id.rbMale);
        } else if ("Nữ".equals(gender)) {
            rgGender.check(R.id.rbFemale);
        }

        // Load hobbies
        String hobbiesStr = sharedPreferences.getString("hobbies", "");
        if (!hobbiesStr.isEmpty()) {
            List<String> hobbiesList = Arrays.asList(hobbiesStr.split(","));
            for (int i = 0; i < flHobbies.getChildCount(); i++) {
                TextView hobbyView = (TextView) flHobbies.getChildAt(i);
                String hobbyText = hobbyView.getText().toString();
                if (hobbiesList.contains(hobbyText)) {
                    hobbyView.setSelected(true);
                    selectedHobbies.add(Hobbies.fromDisplayName(hobbyText));
                }
            }
        }

        // Load spinners with display name mapping
        setSpinnerSelection(spZodiac, ZodiacSign.values(), sharedPreferences.getString("zodiacSign", ""), ZodiacSign::getDisplayName);
        setSpinnerSelection(spPersonality, PersonalityType.values(), sharedPreferences.getString("personalityType", ""), PersonalityType::toString);
        setSpinnerSelection(spCommunication, CommunicationStyle.values(), sharedPreferences.getString("communicationStyle", ""), CommunicationStyle::getDisplayName);
        setSpinnerSelection(spLoveLanguage, LoveLanguage.values(), sharedPreferences.getString("loveLanguage", ""), LoveLanguage::getDisplayName);
        setSpinnerSelection(spPetPreference, PetPreference.values(), sharedPreferences.getString("petPreference", ""), PetPreference::getDisplayName);
        setSpinnerSelection(spDrinking, DrinkingHabit.values(), sharedPreferences.getString("drinkingHabit", ""), DrinkingHabit::getDisplayName);
        setSpinnerSelection(spSmoking, SmokingHabit.values(), sharedPreferences.getString("smokingHabit", ""), SmokingHabit::getDisplayName);
        setSpinnerSelection(spSleeping, SleepingHabit.values(), sharedPreferences.getString("sleepingHabit", ""), SleepingHabit::getDisplayName);

        // Load images
        for (int i = 1; i <= 9; i++) {
            String picUrl = sharedPreferences.getString("pic" + i, null);
            if (picUrl != null && !picUrl.isEmpty()) {
                int imageViewId = getResources().getIdentifier("img" + i, "id", getPackageName());
                ImageView imageView = findViewById(imageViewId);
                if (imageView != null) {
                    Glide.with(this)
                            .load(picUrl)
                            .placeholder(R.drawable.ic_dislike)
                            .into(imageView);
                }
            }
        }
    }

    private <T> void setSpinnerSelection(Spinner spinner, T[] values, String displayValue, java.util.function.Function<T, String> displayNameMapper) {
        if (displayValue != null && !displayValue.isEmpty()) {
            ArrayAdapter<T> adapter = (ArrayAdapter<T>) spinner.getAdapter();
            for (int i = 0; i < values.length; i++) {
                if (displayNameMapper.apply(values[i]).equals(displayValue)) {
                    spinner.setSelection(i);
                    break;
                }
            }
        }
    }

    private String[] getPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return new String[]{Manifest.permission.READ_MEDIA_IMAGES};
        } else {
            return new String[]{Manifest.permission.READ_EXTERNAL_STORAGE};
        }
    }

    private void checkStoragePermissionAndOpenPicker() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            openImagePicker();
            return;
        }
        String[] permissions = getPermissions();
        if (ContextCompat.checkSelfPermission(this, permissions[0]) == PackageManager.PERMISSION_GRANTED) {
            openImagePicker();
        } else {
            requestPermissions(permissions, STORAGE_PERMISSION_CODE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == STORAGE_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.d(TAG, "onRequestPermissionsResult: Permission granted");
                openImagePicker();
            } else {
                Log.d(TAG, "onRequestPermissionsResult: Permission denied");
                Toast.makeText(this, "Quyền truy cập bị từ chối. Vào cài đặt để bật.", Toast.LENGTH_LONG).show();
                Intent intent = new Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                intent.setData(Uri.parse("package:" + getPackageName()));
                startActivity(intent);
            }
        }
    }

    private void openImagePicker() {
        Log.d(TAG, "openImagePicker: Starting");
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        imagePickerLauncher.launch(Intent.createChooser(intent, "Chọn ảnh"));
        Log.d(TAG, "openImagePicker: Intent launched");
    }

    private void setupImageClickListeners() {
        Log.d(TAG, "setupImageClickListeners: Starting");
        for (int i = 1; i <= 9; i++) {
            int frameLayoutId = getResources().getIdentifier("flImg" + i, "id", getPackageName());
            FrameLayout frameLayout = findViewById(frameLayoutId);
            final int position = i;
            if (frameLayout != null) {
                frameLayout.setOnClickListener(v -> {
                    Log.d(TAG, "setupImageClickListeners: Image position " + position + " clicked");
                    currentImagePosition = position;
                    checkStoragePermissionAndOpenPicker();
                });
            } else {
                Log.e(TAG, "setupImageClickListeners: FrameLayout flImg" + i + " not found");
            }
        }
        Log.d(TAG, "setupImageClickListeners: Completed");
    }

    private void updateImageView(Uri imageUri) {
        Log.d(TAG, "updateImageView: Updating image at position " + currentImagePosition);
        int imageViewId = getResources().getIdentifier("img" + currentImagePosition, "id", getPackageName());
        ImageView imageView = findViewById(imageViewId);
        if (imageView != null) {
            imageView.setImageURI(imageUri);
            Log.d(TAG, "updateImageView: Image set successfully");
        }
    }

    private void uploadImageToServer(Uri imageUri, String position) {
        Log.d(TAG, "uploadImageToServer: Starting upload for position " + position);
        String filePath = RealPathUtil.getRealPath(this, imageUri);
        if (filePath == null) {
            Log.e(TAG, "uploadImageToServer: Failed to get file path for URI = " + imageUri);
            Toast.makeText(this, "Không thể lấy đường dẫn ảnh", Toast.LENGTH_SHORT).show();
            return;
        }

        File file = new File(filePath);
        RequestBody requestFile = RequestBody.create(MediaType.parse("image/*"), file);
        MultipartBody.Part filePart = MultipartBody.Part.createFormData("file", file.getName(), requestFile);
        RequestBody positionPart = RequestBody.create(MediaType.parse("text/plain"), position);

        Log.d(TAG, "uploadImageToServer: Sending request for file " + file.getName());
        Call<ApiResponse<Album>> call = authService.uploadImage("Bearer " + authToken, filePart, positionPart);
        call.enqueue(new Callback<ApiResponse<Album>>() {
            @Override
            public void onResponse(Call<ApiResponse<Album>> call, Response<ApiResponse<Album>> response) {
                Log.d(TAG, "uploadImageToServer: Response received, code = " + response.code());
                if (response.isSuccessful() && response.body() != null) {
                    Log.d(TAG, "uploadImageToServer: Upload successful");
                    Toast.makeText(ProfileUpdateActivity.this, "Upload ảnh thành công", Toast.LENGTH_SHORT).show();
                    // Gửi Broadcast để yêu cầu fetch profile
                    sendFetchProfileBroadcast();
                } else {
                    Log.d(TAG, "uploadImageToServer: Upload failed, message = " + response.message());
                    Toast.makeText(ProfileUpdateActivity.this, "Upload thất bại: " + response.message(), Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<Album>> call, Throwable t) {
                Log.e(TAG, "uploadImageToServer: Network error, " + t.getMessage());
                Toast.makeText(ProfileUpdateActivity.this, "Lỗi kết nối: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void sendFetchProfileBroadcast() {
        Intent intent = new Intent("FETCH_USER_PROFILE");
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
        Log.d(TAG, "sendFetchProfileBroadcast: Broadcast sent");
    }

    private void setupHobbiesFlexbox() {
        Log.d(TAG, "setupHobbiesFlexbox: Starting");
        Hobbies[] hobbiesValues = Hobbies.values();
        for (Hobbies hobby : hobbiesValues) {
            TextView hobbyView = (TextView) android.view.LayoutInflater.from(this)
                    .inflate(R.layout.hobby_item, flHobbies, false);
            hobbyView.setText(hobby.getDisplayName());
            hobbyView.setOnClickListener(v -> {
                hobbyView.setSelected(!hobbyView.isSelected());
                if (hobbyView.isSelected()) {
                    selectedHobbies.add(hobby);
                    Log.d(TAG, "setupHobbiesFlexbox: Added hobby = " + hobby.getDisplayName());
                } else {
                    selectedHobbies.remove(hobby);
                    Log.d(TAG, "setupHobbiesFlexbox: Removed hobby = " + hobby.getDisplayName());
                }
            });
            flHobbies.addView(hobbyView);
        }
        Log.d(TAG, "setupHobbiesFlexbox: Completed");
    }

    private void setupSpinners() {
        Log.d(TAG, "setupSpinners: Starting");
        ArrayAdapter<ZodiacSign> zodiacAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, ZodiacSign.values());
        zodiacAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spZodiac.setAdapter(zodiacAdapter);

        ArrayAdapter<PersonalityType> personalityAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, PersonalityType.values());
        personalityAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spPersonality.setAdapter(personalityAdapter);

        ArrayAdapter<CommunicationStyle> communicationAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, CommunicationStyle.values());
        communicationAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spCommunication.setAdapter(communicationAdapter);

        ArrayAdapter<LoveLanguage> loveLanguageAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, LoveLanguage.values());
        loveLanguageAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spLoveLanguage.setAdapter(loveLanguageAdapter);

        ArrayAdapter<PetPreference> petAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, PetPreference.values());
        petAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spPetPreference.setAdapter(petAdapter);

        ArrayAdapter<DrinkingHabit> drinkingAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, DrinkingHabit.values());
        drinkingAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spDrinking.setAdapter(drinkingAdapter);

        ArrayAdapter<SmokingHabit> smokingAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, SmokingHabit.values());
        smokingAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spSmoking.setAdapter(smokingAdapter);

        ArrayAdapter<SleepingHabit> sleepingAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, SleepingHabit.values());
        sleepingAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spSleeping.setAdapter(sleepingAdapter);
        Log.d(TAG, "setupSpinners: Completed");
    }

    private void saveProfile() {
        Log.d(TAG, "saveProfile: Starting");
        ProfileUpdateDTO profile = new ProfileUpdateDTO();

        profile.setFirstName(etFirstName.getText().toString().trim());
        profile.setLastName(etLastName.getText().toString().trim());
        Log.d(TAG, "saveProfile: FirstName = " + profile.getFirstName() + ", LastName = " + profile.getLastName());

        try {
            int age = Integer.parseInt(etAge.getText().toString().trim());
            if (age < 18) {
                Log.d(TAG, "saveProfile: Age < 18");
                Toast.makeText(this, "Tuổi phải lớn hơn hoặc bằng 18", Toast.LENGTH_SHORT).show();
                return;
            }
            profile.setAge(age);
            Log.d(TAG, "saveProfile: Age = " + age);
        } catch (NumberFormatException e) {
            Log.e(TAG, "saveProfile: Invalid age, " + e.getMessage());
            Toast.makeText(this, "Vui lòng nhập tuổi hợp lệ", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            int height = Integer.parseInt(etHeight.getText().toString().trim());
            if (height < 100) {
                Log.d(TAG, "saveProfile: Height < 100");
                Toast.makeText(this, "Chiều cao phải lớn hơn 100 cm", Toast.LENGTH_SHORT).show();
                return;
            }
            profile.setHeight(height);
            Log.d(TAG, "saveProfile: Height = " + height);
        } catch (NumberFormatException e) {
            Log.e(TAG, "saveProfile: Invalid height, " + e.getMessage());
            Toast.makeText(this, "Vui lòng nhập chiều cao hợp lệ", Toast.LENGTH_SHORT).show();
            return;
        }

        String bio = etBio.getText().toString().trim();
        if (bio.length() > 50) {
            Log.d(TAG, "saveProfile: Bio too long, length = " + bio.length());
            Toast.makeText(this, "Tiểu sử không được quá 50 ký tự", Toast.LENGTH_SHORT).show();
            return;
        }
        profile.setBio(bio);
        Log.d(TAG, "saveProfile: Bio = " + bio);

        int selectedGenderId = rgGender.getCheckedRadioButtonId();
        if (selectedGenderId != -1) {
            RadioButton selectedRadio = findViewById(selectedGenderId);
            String genderText = selectedRadio.getText().toString();
            if (genderText.equals("Nam")) {
                profile.setGender(Gender.MALE.name());
            } else if (genderText.equals("Nữ")) {
                profile.setGender(Gender.FEMALE.name());
            }
            Log.d(TAG, "saveProfile: Gender = " + profile.getGender());
        }

        profile.setHobbies(selectedHobbies.stream().map(Hobbies::name).collect(Collectors.toList()));
        profile.setZodiacSign(((ZodiacSign) spZodiac.getSelectedItem()).name());
        profile.setPersonalityType(((PersonalityType) spPersonality.getSelectedItem()).name());
        profile.setCommunicationStyle(((CommunicationStyle) spCommunication.getSelectedItem()).name());
        profile.setLoveLanguage(((LoveLanguage) spLoveLanguage.getSelectedItem()).name());
        profile.setPetPreference(((PetPreference) spPetPreference.getSelectedItem()).name());
        profile.setDrinkingHabit(((DrinkingHabit) spDrinking.getSelectedItem()).name());
        profile.setSmokingHabit(((SmokingHabit) spSmoking.getSelectedItem()).name());
        profile.setSleepingHabit(((SleepingHabit) spSleeping.getSelectedItem()).name());
        Log.d(TAG, "saveProfile: Hobbies = " + profile.getHobbies());
        Log.d(TAG, "saveProfile: Zodiac = " + profile.getZodiacSign());
        Log.d(TAG, "saveProfile: Personality = " + profile.getPersonalityType());

        if (authToken == null) {
            Log.d(TAG, "saveProfile: No auth token");
            Toast.makeText(this, "Không tìm thấy token, vui lòng đăng nhập lại", Toast.LENGTH_SHORT).show();
            return;
        }

        Log.d(TAG, "saveProfile: Sending profile update request");
        Call<ApiResponse<Void>> call = authService.updateProfile("Bearer " + authToken, profile);
        call.enqueue(new Callback<ApiResponse<Void>>() {
            @Override
            public void onResponse(Call<ApiResponse<Void>> call, Response<ApiResponse<Void>> response) {
                Log.d(TAG, "saveProfile: Response received, code = " + response.code());
                if (response.isSuccessful()) {
                    Log.d(TAG, "saveProfile: Update successful");
                    Toast.makeText(ProfileUpdateActivity.this, "Cập nhật hồ sơ thành công", Toast.LENGTH_SHORT).show();

                    // Update SharedPreferences with new data
                    SharedPreferences.Editor editor = sharedPreferences.edit();
                    editor.putString("firstName", profile.getFirstName());
                    editor.putString("lastName", profile.getLastName());
                    editor.putInt("age", profile.getAge());
                    editor.putInt("height", profile.getHeight());
                    editor.putString("bio", profile.getBio());
                    editor.putString("gender", profile.getGender() != null ? (profile.getGender().equals(Gender.MALE.name()) ? "Nam" : "Nữ") : "");
                    String hobbiesStr = selectedHobbies.stream()
                            .map(Hobbies::getDisplayName)
                            .collect(Collectors.joining(","));
                    editor.putString("hobbies", hobbiesStr);
                    editor.putString("zodiacSign", ((ZodiacSign) spZodiac.getSelectedItem()).getDisplayName());
                    editor.putString("personalityType", ((PersonalityType) spPersonality.getSelectedItem()).toString());
                    editor.putString("communicationStyle", ((CommunicationStyle) spCommunication.getSelectedItem()).getDisplayName());
                    editor.putString("loveLanguage", ((LoveLanguage) spLoveLanguage.getSelectedItem()).getDisplayName());
                    editor.putString("petPreference", ((PetPreference) spPetPreference.getSelectedItem()).getDisplayName());
                    editor.putString("drinkingHabit", ((DrinkingHabit) spDrinking.getSelectedItem()).getDisplayName());
                    editor.putString("smokingHabit", ((SmokingHabit) spSmoking.getSelectedItem()).getDisplayName());
                    editor.putString("sleepingHabit", ((SleepingHabit) spSleeping.getSelectedItem()).getDisplayName());
                    editor.apply();

                    // Gửi Broadcast để yêu cầu fetch profile
                    sendFetchProfileBroadcast();

                    // Finish activity
                    finish();
                } else {
                    Log.d(TAG, "saveProfile: Update failed, message = " + response.message());
                    Toast.makeText(ProfileUpdateActivity.this, "Cập nhật thất bại: " + response.message(), Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<Void>> call, Throwable t) {
                Log.e(TAG, "saveProfile: Network error, " + t.getMessage());
                Toast.makeText(ProfileUpdateActivity.this, "Lỗi kết nối: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}