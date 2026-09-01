package com.hassan.anomaly;

public record AuthResponse(String token, String username, long expiresInMinutes) {}