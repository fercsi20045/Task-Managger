package com.taskmanager.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class RegisterRequest {
    @NotBlank(message = "Felhasználónév kötelező")
    @Size(min = 3, max = 50)
    private String username;

    @NotBlank(message = "Email kötelező")
    private String email;

    @NotBlank(message = "Jelszó kötelező")
    @Size(min = 6, message = "A jelszó minimum 6 karakter")
    private String password;

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
}