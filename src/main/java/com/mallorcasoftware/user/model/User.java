package com.mallorcasoftware.user.model;

import java.util.Date;
import java.util.Locale;

public interface User {
    long getId();

    void setId(long id);

    String getUsername();

    void setUsername(String username);

    String getEmail();

    void setEmail(String email);

    String getPassword();

    void setPassword(String password);

    String getPasswordResetToken();

    void setPasswordResetToken(String passwordResetToken);

    Date getPasswordRequestedAt();

    void setPasswordRequestedAt(Date passwordRequestedAt);

    Locale getLocale();

    void setLocale(Locale locale);
}
