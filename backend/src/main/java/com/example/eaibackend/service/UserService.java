package com.example.eaibackend.service;

import com.example.eaibackend.model.User;
import com.example.eaibackend.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    // 用户注册
    public void registerUser(String username, String password) {
        if (userRepository.findByUsername(username).isPresent()) {
            throw new RuntimeException("Username already exists");
        }

        User user = new User();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(password));

        userRepository.save(user);
    }

    // 用户登录验证
    public User loginUser(String username, String password) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // 先检查是否是BCrypt加密的密码
        if (user.getPassword().length() >= 60 && user.getPassword().startsWith("$2a$")) {
            // BCrypt加密的密码
            if (!passwordEncoder.matches(password, user.getPassword())) {
                throw new RuntimeException("Invalid credentials");
            }
        } else {
            // 明文密码（兼容旧数据）
            if (!password.equals(user.getPassword())) {
                throw new RuntimeException("Invalid credentials");
            }
        }

        return user;
    }
}
