package com.invoiceflow.ai.mapper;

import com.invoiceflow.ai.dto.user.UserProfileResponse;
import com.invoiceflow.ai.dto.user.UserProfileUpdateRequest;
import com.invoiceflow.ai.entity.User;

public final class UserMapper {
    private UserMapper() { }

    public static UserProfileResponse toResponse(User user) {
        return new UserProfileResponse(user.getId(), user.getFirstName(), user.getLastName(), user.getCompanyName(),
                user.getEmail(), user.getPhoneNumber(), user.getCountry(), user.getCreatedAt(), user.getUpdatedAt());
    }

    public static void updateEntity(UserProfileUpdateRequest request, User user) {
        user.updateProfile(request.firstName(), request.lastName(), request.companyName(), request.email(),
                request.phoneNumber(), request.country());
    }
}
