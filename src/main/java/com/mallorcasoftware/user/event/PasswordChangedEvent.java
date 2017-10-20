package com.mallorcasoftware.user.event;

import com.mallorcasoftware.user.model.User;

public class PasswordChangedEvent extends AbstractUserEvent {
    public PasswordChangedEvent(User user) {
        super(user);
    }
}
