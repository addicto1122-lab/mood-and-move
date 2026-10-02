package com.moodandmove.user.service;

import com.moodandmove.common.domain.type.ActivityStyle;
import com.moodandmove.common.domain.type.EnvironmentType;
import com.moodandmove.common.domain.type.SocialType;
import com.moodandmove.user.domain.entity.Hobby;
import com.moodandmove.user.domain.entity.User;
import com.moodandmove.user.domain.entity.UserHobby;
import com.moodandmove.user.domain.entity.UserPreference;
import com.moodandmove.user.domain.type.AgeGroup;
import com.moodandmove.user.domain.type.Gender;
import com.moodandmove.user.dto.response.HobbyResponse;
import com.moodandmove.user.dto.response.PreferenceResponse;
import com.moodandmove.user.repository.HobbyRepository;
import com.moodandmove.user.repository.UserHobbyRepository;
import com.moodandmove.user.repository.UserPreferenceRepository;
import com.moodandmove.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final HobbyRepository hobbyRepository;
    private final UserHobbyRepository userHobbyRepository;
    private final UserPreferenceRepository userPreferenceRepository;
    private final PasswordEncoder passwordEncoder;


    @Transactional
    public void changePassword(
            Long userId,
            String currentPassword,
            String newPassword
    ) {
        User user = userRepository.findById(userId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "사용자를 찾을 수 없습니다."
                        )
                );

        if (!passwordEncoder.matches(
                currentPassword,
                user.getPasswordHash()
        )) {
            throw new IllegalArgumentException(
                    "현재 비밀번호가 올바르지 않습니다."
            );
        }

        if (passwordEncoder.matches(
                newPassword,
                user.getPasswordHash()
        )) {
            throw new IllegalArgumentException(
                    "현재 비밀번호와 다른 비밀번호를 입력해주세요."
            );
        }

        String encodedPassword =
                passwordEncoder.encode(newPassword);

        user.updatePassword(encodedPassword);

        user.increaseTokenVersion();
    }


    @Transactional
    public void completeOnboarding(
            Long userId,
            AgeGroup ageGroup,
            Gender gender,
            List<Long> hobbyIds
    ) {
        User user = userRepository.findById(userId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "사용자를 찾을 수 없습니다."
                        )
                );

        if (ageGroup == null) {
            throw new IllegalArgumentException(
                    "나이대를 선택해주세요."
            );
        }

        if (gender == null) {
            throw new IllegalArgumentException(
                    "성별을 선택해주세요."
            );
        }

        if (hobbyIds == null || hobbyIds.isEmpty()) {
            throw new IllegalArgumentException(
                    "좋아하는 활동을 하나 이상 선택해주세요."
            );
        }


        List<Long> distinctHobbyIds =
                hobbyIds.stream()
                        .distinct()
                        .toList();

        List<Hobby> hobbies =
                hobbyRepository.findAllById(distinctHobbyIds);


        if (hobbies.size() != distinctHobbyIds.size()) {
            throw new IllegalArgumentException(
                    "존재하지 않는 취미가 포함되어 있습니다."
            );
        }


        userHobbyRepository.deleteAllByUser_Id(userId);
        userHobbyRepository.flush();


        List<UserHobby> userHobbies =
                hobbies.stream()
                        .map(hobby ->
                                UserHobby.create(user, hobby)
                        )
                        .toList();

        userHobbyRepository.saveAll(userHobbies);


        if (userPreferenceRepository
                .findByUser_Id(userId)
                .isEmpty()) {

            UserPreference preference =
                    UserPreference.createDefault(user);

            userPreferenceRepository.save(preference);
        }


        user.completeOnboarding(
                ageGroup,
                gender
        );
    }


    @Transactional
    public void updatePreference(
            Long userId,
            List<Long> hobbyIds,
            ActivityStyle activityStyle,
            EnvironmentType activityEnvironment,
            SocialType socialPreference,
            Integer defaultAvailableMinutes
    ) {
        User user = userRepository.findById(userId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "사용자를 찾을 수 없습니다."
                        )
                );

        if (hobbyIds == null || hobbyIds.isEmpty()) {
            throw new IllegalArgumentException(
                    "좋아하는 활동을 하나 이상 선택해주세요."
            );
        }

        List<Long> distinctHobbyIds =
                hobbyIds.stream()
                        .distinct()
                        .toList();

        List<Hobby> hobbies =
                hobbyRepository.findAllById(distinctHobbyIds);

        if (hobbies.size() != distinctHobbyIds.size()) {
            throw new IllegalArgumentException(
                    "존재하지 않는 취미가 포함되어 있습니다."
            );
        }

        userHobbyRepository.deleteAllByUser_Id(userId);
        userHobbyRepository.flush();

        List<UserHobby> userHobbies =
                hobbies.stream()
                        .map(hobby ->
                                UserHobby.create(user, hobby)
                        )
                        .toList();

        userHobbyRepository.saveAll(userHobbies);


        UserPreference preference =
                userPreferenceRepository
                        .findByUser_Id(userId)
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "사용자 선호 정보를 찾을 수 없습니다."
                                )
                        );

        preference.updatePreference(
                activityStyle,
                activityEnvironment,
                socialPreference,
                defaultAvailableMinutes
        );
    }

    @Transactional
    public void updateProfile(
            Long userId,
            String nickname,
            AgeGroup ageGroup,
            Gender gender
    ) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        user.updateProfile(
                nickname,
                ageGroup,
                gender
        );
    }

    @Transactional(readOnly = true)
    public PreferenceResponse getPreferences(Long userId) {

        List<Long> hobbyIds =
                userHobbyRepository.findAllByUser_Id(userId)
                        .stream()
                        .map(userHobby ->
                                userHobby.getHobby().getId()
                        )
                        .toList();

        UserPreference preference =
                userPreferenceRepository.findByUser_Id(userId)
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "사용자 선호 정보를 찾을 수 없습니다."
                                )
                        );

        return new PreferenceResponse(
                hobbyIds,
                preference.getActivityStyle(),
                preference.getActivityEnvironment(),
                preference.getSocialPreference(),
                preference.getDefaultAvailableMinutes()
        );
    }

    @Transactional(readOnly = true)
    public List<HobbyResponse> getHobbies() {
        return hobbyRepository
                .findAllByActiveTrueOrderByIdAsc()
                .stream()
                .map(hobby -> new HobbyResponse(
                        hobby.getId(),
                        hobby.getName(),
                        hobby.getCategory()
                ))
                .toList();
    }
}