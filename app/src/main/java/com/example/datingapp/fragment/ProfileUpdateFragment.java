package com.example.datingapp.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.fragment.app.Fragment;

import com.example.datingapp.R;
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
import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.List;

public class ProfileUpdateFragment extends Fragment {

    private TextInputEditText etFirstName, etLastName, etAge, etHeight, etBio;
    private RadioGroup rgGender;
    private ListView lvHobbies;
    private Spinner spZodiac, spPersonality, spCommunication, spLoveLanguage,
            spPetPreference, spDrinking, spSmoking, spSleeping;
    private Button btnSave;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile_update, container, false);

        // Initialize views
        etFirstName = view.findViewById(R.id.etFirstName);
        etLastName = view.findViewById(R.id.etLastName);
        etAge = view.findViewById(R.id.etAge);
        etHeight = view.findViewById(R.id.etHeight);
        etBio = view.findViewById(R.id.etBio);
        rgGender = view.findViewById(R.id.rgGender);
        lvHobbies = view.findViewById(R.id.lvHobbies);
        spZodiac = view.findViewById(R.id.spZodiac);
        spPersonality = view.findViewById(R.id.spPersonality);
        spCommunication = view.findViewById(R.id.spCommunication);
        spLoveLanguage = view.findViewById(R.id.spLoveLanguage);
        spPetPreference = view.findViewById(R.id.spPetPreference);
        spDrinking = view.findViewById(R.id.spDrinking);
        spSmoking = view.findViewById(R.id.spSmoking);
        spSleeping = view.findViewById(R.id.spSleeping);
        btnSave = view.findViewById(R.id.btnSave);

        // Setup adapters
        setupHobbiesListView();
        setupSpinners();

        // Setup save button listener
        btnSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveProfile();
            }
        });

        return view;
    }

    private void setupHobbiesListView() {
        Hobbies[] hobbiesValues = Hobbies.values();
        ArrayAdapter<Hobbies> adapter = new ArrayAdapter<Hobbies>(getContext(),
                R.layout.hobby_item, R.id.cbHobby, hobbiesValues) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                View view = super.getView(position, convertView, parent);
                CheckBox checkBox = view.findViewById(R.id.cbHobby);
                checkBox.setText(hobbiesValues[position].getDisplayName());
                return view;
            }
        };
        lvHobbies.setAdapter(adapter);
    }

    private void setupSpinners() {
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
    }

    private void saveProfile() {
        ProfileUpdateDTO profile = new ProfileUpdateDTO();

        // First Name and Last Name
        profile.setFirstName(etFirstName.getText().toString().trim());
        profile.setLastName(etLastName.getText().toString().trim());

        // Age
        try {
            int age = Integer.parseInt(etAge.getText().toString().trim());
            if (age < 18) {
                Toast.makeText(getContext(), "Tuổi phải lớn hơn hoặc bằng 18", Toast.LENGTH_SHORT).show();
                return;
            }
            profile.setAge(age);
        } catch (NumberFormatException e) {
            Toast.makeText(getContext(), "Vui lòng nhập tuổi hợp lệ", Toast.LENGTH_SHORT).show();
            return;
        }

        // Height
        try {
            int height = Integer.parseInt(etHeight.getText().toString().trim());
            if (height < 100) {
                Toast.makeText(getContext(), "Chiều cao phải lớn hơn 100 cm", Toast.LENGTH_SHORT).show();
                return;
            }
            profile.setHeight(height);
        } catch (NumberFormatException e) {
            Toast.makeText(getContext(), "Vui lòng nhập chiều cao hợp lệ", Toast.LENGTH_SHORT).show();
            return;
        }

        // Bio
        String bio = etBio.getText().toString().trim();
        if (bio.length() > 50) {
            Toast.makeText(getContext(), "Tiểu sử không được quá 50 ký tự", Toast.LENGTH_SHORT).show();
            return;
        }
        profile.setBio(bio);

        // Gender
        int selectedGenderId = rgGender.getCheckedRadioButtonId();
        if (selectedGenderId != -1) {
            RadioButton selectedRadio = getView().findViewById(selectedGenderId);
            String genderText = selectedRadio.getText().toString();
            if (genderText.equals("Nam")) {
                profile.setGender(Gender.MALE);
            } else if (genderText.equals("Nữ")) {
                profile.setGender(Gender.FEMALE);
            }
        }

        // Hobbies
        List<Hobbies> selectedHobbies = new ArrayList<>();
        for (int i = 0; i < lvHobbies.getCount(); i++) {
            if (lvHobbies.isItemChecked(i)) {
                selectedHobbies.add((Hobbies) lvHobbies.getItemAtPosition(i));
            }
        }
        profile.setHobbies(selectedHobbies);

        // Spinners
        profile.setZodiacSign((ZodiacSign) spZodiac.getSelectedItem());
        profile.setPersonalityType((PersonalityType) spPersonality.getSelectedItem());
        profile.setCommunicationStyle((CommunicationStyle) spCommunication.getSelectedItem());
        profile.setLoveLanguage((LoveLanguage) spLoveLanguage.getSelectedItem());
        profile.setPetPreference((PetPreference) spPetPreference.getSelectedItem());
        profile.setDrinkingHabit((DrinkingHabit) spDrinking.getSelectedItem());
        profile.setSmokingHabit((SmokingHabit) spSmoking.getSelectedItem());
        profile.setSleepingHabit((SleepingHabit) spSleeping.getSelectedItem());

        // TODO: Send profile to server or handle the data
        Toast.makeText(getContext(), "Đã lưu hồ sơ", Toast.LENGTH_SHORT).show();
    }
}