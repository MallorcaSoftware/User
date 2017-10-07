package com.mallorcasoftware.user.service.token;

import java.math.BigInteger;
import java.security.SecureRandom;

public class RandomTokenGenerator implements TokenGenerator {
    private SecureRandom random = new SecureRandom();

    public String generateToken(String salt) {
        String token = new BigInteger(130, random).toString(80);
        return token;
    }
}
