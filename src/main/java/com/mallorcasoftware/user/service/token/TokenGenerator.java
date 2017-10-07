package com.mallorcasoftware.user.service.token;

public interface TokenGenerator {
    String generateToken(String salt);
}
