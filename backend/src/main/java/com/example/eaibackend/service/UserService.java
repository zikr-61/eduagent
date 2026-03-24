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
    public User registerUser(String username, String password, String userType, String name, 
                            String grade, String subject, String school) {
        if (userRepository.findByUsername(username).isPresent()) {
            throw new RuntimeException("Username already exists");
        }

        User user = new User();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(password));
        user.setUserType(userType);
        user.setName(name);
        
        if ("student".equals(userType)) {
            user.setGrade(grade);
        } else if ("teacher".equals(userType)) {
            user.setSubject(subject);
            user.setSchool(school);
        }

        return userRepository.save(user);
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

    // 根据ID获取用户
    public User getUserById(Integer id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }
}
