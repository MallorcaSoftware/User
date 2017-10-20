package com.mallorcasoftware.user.event;

import com.mallorcasoftware.user.model.User;

public class UserCreatedEvent extends AbstractUserEvent {
    public UserCreatedEvent(User user) {
        super(user);
    }
}
