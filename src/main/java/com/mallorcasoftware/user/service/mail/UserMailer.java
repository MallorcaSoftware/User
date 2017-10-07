package com.mallorcasoftware.user.service.mail;

import com.mallorcasoftware.user.model.User;

public interface UserMailer {
    void sendUserRegistrationMail(User user);
    void sendPasswordResetMail(User user);
    void sendPasswordResetedMail(User user);
}
