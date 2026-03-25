package com.example.eaibackend.controller;

import com.example.eaibackend.config.JwtUtil;
import com.example.eaibackend.model.User;
import com.example.eaibackend.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/user")
public class UserController {

    @Autowired
    private UserService userService;

    @Autowired
    private JwtUtil jwtUtil;

    @PostMapping("/register")
    public User register(@RequestParam String username,
                        @RequestParam String password,
                        @RequestParam String userType,
                        @RequestParam String name,
                        @RequestParam(required = false) String grade,
                        @RequestParam(required = false) String subject,
                        @RequestParam(required = false) String school) {
        return userService.registerUser(username, password, userType, name, grade, subject, school);
    }

    @PostMapping("/login")
    public Map<String, Object> login(@RequestParam String username, @RequestParam String password) {
        User user = userService.loginUser(username, password);
        String token = jwtUtil.generateToken(user.getId(), user.getUsername(), user.getUserType());
        Map<String, Object> response = new HashMap<>();
        response.put("user", user);
        response.put("userType", user.getUserType());
        response.put("token", token);
        return response;
    }

    @GetMapping("/info/{id}")
    public User getUserInfo(@PathVariable Integer id) {
        return userService.getUserById(id);
    }
}
