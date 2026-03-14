package com.example.eaibackend.controller;

import com.example.eaibackend.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/user")
public class UserController {

    @Autowired
    private UserService userService;

    // 用户注册接口
    @PostMapping("/register")
    public String register(@RequestParam String username, @RequestParam String password) {
        userService.registerUser(username, password);
        return "User registered successfully!";
    }

    // 用户登录接口
    @PostMapping("/login")
    public com.example.eaibackend.model.User login(@RequestParam String username, @RequestParam String password) {
        return userService.loginUser(username, password);
    }
}
