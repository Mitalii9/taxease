package com.taxease.user.service;

import com.taxease.auth.dto.AuthResponse;
import com.taxease.auth.dto.LoginRequest;
import com.taxease.auth.dto.RegisterRequest;
import com.taxease.security.JwtUtil;
import com.taxease.user.dto.CreateUserRequest;
import com.taxease.user.dto.UserDTO;
import com.taxease.user.model.Role;
import com.taxease.user.model.User;
import com.taxease.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Email already registered: " + request.getEmail());
        }
        User user = User.builder()
                .name(request.getName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .phone(request.getPhone())
                .role(Role.USER)
                .build();
        user = userRepository.save(user);
        return toAuthResponse(user, jwtUtil.generateToken(user.getEmail()));
    }

    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new IllegalArgumentException("Invalid email or password"));
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new IllegalArgumentException("Invalid email or password");
        }
        return toAuthResponse(user, jwtUtil.generateToken(user.getEmail()));
    }

    public UserDTO getProfile(String email) {
        return userRepository.findByEmail(email).map(this::toDTO)
                .orElseThrow(() -> new RuntimeException("User not found: " + email));
    }

    public List<UserDTO> findAll() {
        return userRepository.findAll().stream().map(this::toDTO).toList();
    }

    public UserDTO findById(String id) {
        return userRepository.findById(id).map(this::toDTO)
                .orElseThrow(() -> new RuntimeException("User not found: " + id));
    }

    @Transactional
    public UserDTO create(CreateUserRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Email already registered: " + request.getEmail());
        }
        User user = User.builder()
                .name(request.getName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .phone(request.getPhone())
                .role(Role.USER)
                .build();
        return toDTO(userRepository.save(user));
    }

    @Transactional
    public void delete(String id) {
        userRepository.deleteById(id);
    }

    private UserDTO toDTO(User user) {
        return UserDTO.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .phone(user.getPhone())
                .role(user.getRole())
                .createdAt(user.getCreatedAt())
                .build();
    }

    private AuthResponse toAuthResponse(User user, String token) {
        return AuthResponse.builder()
                .token(token)
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .role(user.getRole().name())
                .build();
    }
}
