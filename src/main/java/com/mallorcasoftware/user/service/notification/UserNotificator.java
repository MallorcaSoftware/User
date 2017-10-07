package com.mallorcasoftware.user.service.notification;

import com.mallorcasoftware.user.model.User;

public interface UserNotificator {
    void sendUserRegistrationNotification(User user);
    void sendPasswordResetNotification(User user);
    void sendPasswordResetedNotification(User user);
}
