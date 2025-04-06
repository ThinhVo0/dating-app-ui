package com.example.datingapp.fragment;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

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
import java.util.List;
import java.util.stream.Collectors;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ProfileUpdateFragment extends Fragment {

    private static final String TAG = "ProfileUpdateFragment";
    private TextInputEditText etFirstName, etLastName, etAge, etHeight, etBio;
    private RadioGroup rgGender;
    private FlexboxLayout flHobbies;
    private Spinner spZodiac, spPersonality, spCommunication, spLoveLanguage,
            spPetPreference, spDrinking, spSmoking, spSleeping;
    private Button btnSave, btnUploadImages;
    private GridLayout glProfileImages;
    private List<Hobbies> selectedHobbies = new ArrayList<>();
    private AuthService authService;
    private String authToken;
    private List<Uri> selectedImageUris = new ArrayList<>();
    private static final int STORAGE_PERMISSION_CODE = 100;

    private final ActivityResultLauncher<Intent> imagePickerLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                Log.d(TAG, "imagePickerLauncher: Result received, resultCode = " + result.getResultCode());
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    Intent data = result.getData();
                    selectedImageUris.clear();
                    if (data.getClipData() != null) {
                        int count = Math.min(data.getClipData().getItemCount(), 9);
                        Log.d(TAG, "imagePickerLauncher: Multiple images selected, count = " + count);
                        for (int i = 0; i < count; i++) {
                            Uri imageUri = data.getClipData().getItemAt(i).getUri();
                            selectedImageUris.add(imageUri);
                            Log.d(TAG, "imagePickerLauncher: Added image URI = " + imageUri);
                        }
                    } else if (data.getData() != null) {
                        selectedImageUris.add(data.getData());
                        Log.d(TAG, "imagePickerLauncher: Single image selected, URI = " + data.getData());
                    }
                    displaySelectedImages();
                    uploadImagesToServer();
                } else {
                    Log.d(TAG, "imagePickerLauncher: Image selection failed or cancelled");
                }
            }
    );

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Log.d(TAG, "onCreateView: Starting");
        View view = inflater.inflate(R.layout.fragment_profile_update, container, false);
        SharedPreferences sharedPreferences = requireContext().getSharedPreferences("MyPrefs", Context.MODE_PRIVATE);
        authToken = sharedPreferences.getString("authToken", null);
        Log.d(TAG, "onCreateView: authToken = " + authToken);

        // Initialize views
        etFirstName = view.findViewById(R.id.etFirstName);
        etLastName = view.findViewById(R.id.etLastName);
        etAge = view.findViewById(R.id.etAge);
        etHeight = view.findViewById(R.id.etHeight);
        etBio = view.findViewById(R.id.etBio);
        rgGender = view.findViewById(R.id.rgGender);
        flHobbies = view.findViewById(R.id.flHobbies);
        spZodiac = view.findViewById(R.id.spZodiac);
        spPersonality = view.findViewById(R.id.spPersonality);
        spCommunication = view.findViewById(R.id.spCommunication);
        spLoveLanguage = view.findViewById(R.id.spLoveLanguage);
        spPetPreference = view.findViewById(R.id.spPetPreference);
        spDrinking = view.findViewById(R.id.spDrinking);
        spSmoking = view.findViewById(R.id.spSmoking);
        spSleeping = view.findViewById(R.id.spSleeping);
        btnSave = view.findViewById(R.id.btnSave);
        btnUploadImages = view.findViewById(R.id.btnUploadImages);
        glProfileImages = view.findViewById(R.id.glProfileImages);
        Log.d(TAG, "onCreateView: Views initialized");

        // Initialize Retrofit service
        authService = RetrofitClient.getClient().create(AuthService.class);
        Log.d(TAG, "onCreateView: Retrofit service initialized");

        // Setup adapters
        setupHobbiesFlexbox();
        setupSpinners();
        Log.d(TAG, "onCreateView: Adapters set up");

        // Setup listeners
        btnSave.setOnClickListener(v -> {
            Log.d(TAG, "btnSave: Clicked");
            saveProfile();
        });
        btnUploadImages.setOnClickListener(v -> {
            Log.d(TAG, "btnUploadImages: Clicked");
            checkStoragePermissionAndOpenPicker();
        });

        Log.d(TAG, "onCreateView: Completed");
        return view;
    }

    private String[] getPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) { // API 33+
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
        if (ContextCompat.checkSelfPermission(requireContext(), permissions[0]) == PackageManager.PERMISSION_GRANTED) {
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
                Toast.makeText(getContext(), "Quyền truy cập bị từ chối. Vào cài đặt để bật.", Toast.LENGTH_LONG).show();
                Intent intent = new Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                intent.setData(Uri.parse("package:" + requireContext().getPackageName()));
                startActivity(intent);
            }
        }
    }

    private void openImagePicker() {
        Log.d(TAG, "openImagePicker: Starting");
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        imagePickerLauncher.launch(Intent.createChooser(intent, "Chọn ảnh"));
        Log.d(TAG, "openImagePicker: Intent launched");
    }

    private void displaySelectedImages() {
        Log.d(TAG, "displaySelectedImages: Starting, selectedImageUris size = " + selectedImageUris.size());
        for (int i = 0; i < glProfileImages.getChildCount(); i++) {
            ImageView imageView = (ImageView) glProfileImages.getChildAt(i);
            if (i < selectedImageUris.size()) {
                imageView.setImageURI(selectedImageUris.get(i));
                imageView.setVisibility(View.VISIBLE);
                Log.d(TAG, "displaySelectedImages: Set image at index " + i + " with URI = " + selectedImageUris.get(i));
            } else {
                imageView.setVisibility(View.GONE);
                Log.d(TAG, "displaySelectedImages: Hid image at index " + i);
            }
        }
        Log.d(TAG, "displaySelectedImages: Completed");
    }

    private void uploadImagesToServer() {
        Log.d(TAG, "uploadImagesToServer: Starting");
        if (selectedImageUris.isEmpty()) {
            Log.d(TAG, "uploadImagesToServer: No images selected");
            Toast.makeText(getContext(), "Vui lòng chọn ít nhất một ảnh", Toast.LENGTH_SHORT).show();
            return;
        }

        List<MultipartBody.Part> parts = new ArrayList<>();
        for (Uri uri : selectedImageUris) {
            try {
                String filePath = RealPathUtil.getRealPath(requireContext(), uri);
                if (filePath == null) {
                    Log.e(TAG, "uploadImagesToServer: Failed to get file path for URI = " + uri);
                    Toast.makeText(getContext(), "Không thể lấy đường dẫn ảnh", Toast.LENGTH_SHORT).show();
                    return;
                }
                File file = new File(filePath);
                Log.d(TAG, "uploadImagesToServer: Preparing file = " + file.getAbsolutePath());
                RequestBody requestFile = RequestBody.create(MediaType.parse("image/*"), file);
                MultipartBody.Part body = MultipartBody.Part.createFormData("files", file.getName(), requestFile);
                parts.add(body);
                Log.d(TAG, "uploadImagesToServer: Added file part = " + file.getName());
            } catch (Exception e) {
                Log.e(TAG, "uploadImagesToServer: Error preparing file, " + e.getMessage());
                Toast.makeText(getContext(), "Lỗi khi chuẩn bị ảnh: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                return;
            }
        }

        Log.d(TAG, "uploadImagesToServer: Sending request with " + parts.size() + " parts");
        Call<ApiResponse<Album>> call = authService.uploadImages("Bearer " + authToken, parts);
        call.enqueue(new Callback<ApiResponse<Album>>() {
            @Override
            public void onResponse(Call<ApiResponse<Album>> call, Response<ApiResponse<Album>> response) {
                Log.d(TAG, "uploadImagesToServer: Response received, code = " + response.code());
                if (response.isSuccessful() && response.body() != null) {
                    Log.d(TAG, "uploadImagesToServer: Upload successful");
                    Toast.makeText(getContext(), "Upload ảnh thành công", Toast.LENGTH_SHORT).show();
                } else {
                    Log.d(TAG, "uploadImagesToServer: Upload failed, message = " + response.message());
                    Toast.makeText(getContext(), "Upload thất bại: " + response.message(), Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<Album>> call, Throwable t) {
                Log.e(TAG, "uploadImagesToServer: Network error, " + t.getMessage());
                Toast.makeText(getContext(), "Lỗi kết nối: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setupHobbiesFlexbox() {
        Log.d(TAG, "setupHobbiesFlexbox: Starting");
        Hobbies[] hobbiesValues = Hobbies.values();
        for (Hobbies hobby : hobbiesValues) {
            CheckBox checkBox = new CheckBox(getContext());
            checkBox.setText(hobby.getDisplayName());
            checkBox.setPadding(8, 8, 8, 8);
            checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (isChecked) {
                    selectedHobbies.add(hobby);
                    Log.d(TAG, "setupHobbiesFlexbox: Added hobby = " + hobby.getDisplayName());
                } else {
                    selectedHobbies.remove(hobby);
                    Log.d(TAG, "setupHobbiesFlexbox: Removed hobby = " + hobby.getDisplayName());
                }
            });
            FlexboxLayout.LayoutParams params = new FlexboxLayout.LayoutParams(
                    FlexboxLayout.LayoutParams.WRAP_CONTENT,
                    FlexboxLayout.LayoutParams.WRAP_CONTENT
            );
            params.setMargins(8, 8, 8, 8);
            checkBox.setLayoutParams(params);
            flHobbies.addView(checkBox);
        }
        Log.d(TAG, "setupHobbiesFlexbox: Completed");
    }

    private void setupSpinners() {
        Log.d(TAG, "setupSpinners: Starting");
        ArrayAdapter<ZodiacSign> zodiacAdapter = new ArrayAdapter<>(getContext(),
                android.R.layout.simple_spinner_item, ZodiacSign.values());
        zodiacAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spZodiac.setAdapter(zodiacAdapter);

        ArrayAdapter<PersonalityType> personalityAdapter = new ArrayAdapter<>(getContext(),
                android.R.layout.simple_spinner_item, PersonalityType.values());
        personalityAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spPersonality.setAdapter(personalityAdapter);

        ArrayAdapter<CommunicationStyle> communicationAdapter = new ArrayAdapter<>(getContext(),
                android.R.layout.simple_spinner_item, CommunicationStyle.values());
        communicationAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spCommunication.setAdapter(communicationAdapter);

        ArrayAdapter<LoveLanguage> loveLanguageAdapter = new ArrayAdapter<>(getContext(),
                android.R.layout.simple_spinner_item, LoveLanguage.values());
        loveLanguageAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spLoveLanguage.setAdapter(loveLanguageAdapter);

        ArrayAdapter<PetPreference> petAdapter = new ArrayAdapter<>(getContext(),
                android.R.layout.simple_spinner_item, PetPreference.values());
        petAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spPetPreference.setAdapter(petAdapter);

        ArrayAdapter<DrinkingHabit> drinkingAdapter = new ArrayAdapter<>(getContext(),
                android.R.layout.simple_spinner_item, DrinkingHabit.values());
        drinkingAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spDrinking.setAdapter(drinkingAdapter);

        ArrayAdapter<SmokingHabit> smokingAdapter = new ArrayAdapter<>(getContext(),
                android.R.layout.simple_spinner_item, SmokingHabit.values());
        smokingAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spSmoking.setAdapter(smokingAdapter);

        ArrayAdapter<SleepingHabit> sleepingAdapter = new ArrayAdapter<>(getContext(),
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
                Toast.makeText(getContext(), "Tuổi phải lớn hơn hoặc bằng 18", Toast.LENGTH_SHORT).show();
                return;
            }
            profile.setAge(age);
            Log.d(TAG, "saveProfile: Age = " + age);
        } catch (NumberFormatException e) {
            Log.e(TAG, "saveProfile: Invalid age, " + e.getMessage());
            Toast.makeText(getContext(), "Vui lòng nhập tuổi hợp lệ", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            int height = Integer.parseInt(etHeight.getText().toString().trim());
            if (height < 100) {
                Log.d(TAG, "saveProfile: Height < 100");
                Toast.makeText(getContext(), "Chiều cao phải lớn hơn 100 cm", Toast.LENGTH_SHORT).show();
                return;
            }
            profile.setHeight(height);
            Log.d(TAG, "saveProfile: Height = " + height);
        } catch (NumberFormatException e) {
            Log.e(TAG, "saveProfile: Invalid height, " + e.getMessage());
            Toast.makeText(getContext(), "Vui lòng nhập chiều cao hợp lệ", Toast.LENGTH_SHORT).show();
            return;
        }

        String bio = etBio.getText().toString().trim();
        if (bio.length() > 50) {
            Log.d(TAG, "saveProfile: Bio too long, length = " + bio.length());
            Toast.makeText(getContext(), "Tiểu sử không được quá 50 ký tự", Toast.LENGTH_SHORT).show();
            return;
        }
        profile.setBio(bio);
        Log.d(TAG, "saveProfile: Bio = " + bio);

        int selectedGenderId = rgGender.getCheckedRadioButtonId();
        if (selectedGenderId != -1) {
            RadioButton selectedRadio = getView().findViewById(selectedGenderId);
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
            Toast.makeText(getContext(), "Không tìm thấy token, vui lòng đăng nhập lại", Toast.LENGTH_SHORT).show();
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
                    Toast.makeText(getContext(), "Cập nhật hồ sơ thành công", Toast.LENGTH_SHORT).show();
                } else {
                    Log.d(TAG, "saveProfile: Update failed, message = " + response.message());
                    Toast.makeText(getContext(), "Cập nhật thất bại: " + response.message(), Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<Void>> call, Throwable t) {
                Log.e(TAG, "saveProfile: Network error, " + t.getMessage());
                Toast.makeText(getContext(), "Lỗi kết nối: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}