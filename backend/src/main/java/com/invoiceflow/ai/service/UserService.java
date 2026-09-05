package com.invoiceflow.ai.service;

import com.invoiceflow.ai.dto.user.UserProfileResponse;
import com.invoiceflow.ai.dto.user.UserProfileUpdateRequest;
import com.invoiceflow.ai.entity.User;
import com.invoiceflow.ai.exception.EmailAlreadyExistsException;
import com.invoiceflow.ai.exception.UserNotFoundException;
import com.invoiceflow.ai.mapper.UserMapper;
import com.invoiceflow.ai.repository.UserRepository;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public UserProfileResponse findById(UUID userId) {
        return UserMapper.toResponse(getUserById(userId));
    }

    public UserProfileResponse findByEmail(String email) {
        return UserMapper.toResponse(getUserByEmail(email));
    }

    public UserProfileResponse createUser(User user) {
        if (emailExists(user.getEmail())) {
            throw new EmailAlreadyExistsException(user.getEmail());
        }

        return UserMapper.toResponse(userRepository.save(user));
    }

    public UserProfileResponse updateProfile(UUID userId, UserProfileUpdateRequest request) {
        User user = getUserById(userId);
        if (!user.getEmail().equals(request.email()) && emailExists(request.email())) {
            throw new EmailAlreadyExistsException(request.email());
        }

        UserMapper.updateEntity(request, user);
        return UserMapper.toResponse(userRepository.save(user));
    }

    public boolean emailExists(String email) {
        return userRepository.existsByEmail(email);
    }

    public User getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException(email));
    }

    public User getUserById(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));
    }

    public void updatePasswordHash(UUID userId, String passwordHash) {
        User user = getUserById(userId);
        user.updatePasswordHash(passwordHash);
        userRepository.save(user);
    }
}
