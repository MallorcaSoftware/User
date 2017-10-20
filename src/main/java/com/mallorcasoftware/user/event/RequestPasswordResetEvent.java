package com.mallorcasoftware.user.event;

import com.mallorcasoftware.user.model.User;

public class RequestPasswordResetEvent extends AbstractUserEvent {
    public RequestPasswordResetEvent(User user) {
        super(user);
    }
}
