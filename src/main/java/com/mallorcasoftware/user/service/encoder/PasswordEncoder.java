package com.mallorcasoftware.user.service.encoder;

public interface PasswordEncoder {
    String encode(CharSequence value);
}
