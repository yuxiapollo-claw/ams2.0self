package com.company.ams.user;

public record UserPasswordChangeCommand(String currentPassword, String newPassword) {}
