package com.mallorcasoftware.user.event;

import com.mallorcasoftware.user.model.User;

public class PasswordResetEvent extends AbstractUserEvent {
    public PasswordResetEvent(User user) {
        super(user);
    }
}
