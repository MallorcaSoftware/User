package com.mallorcasoftware.user.listener;

import com.mallorcasoftware.user.event.PasswordChangedEvent;
import com.mallorcasoftware.user.event.PasswordResetEvent;
import com.mallorcasoftware.user.event.RequestPasswordResetEvent;
import com.mallorcasoftware.user.event.UserCreatedEvent;

public interface UserListener {
    void onCreateUser(UserCreatedEvent userCreatedEvent);

    void onChangePassword(PasswordChangedEvent passwordChangedEvent);

    void onRequestPasswordReset(RequestPasswordResetEvent requestPasswordResetEvent);

    void onPasswordReset(PasswordResetEvent passwordResetEvent);
}
