package com.mallorcasoftware.user.event;

import com.mallorcasoftware.user.model.User;

public abstract class AbstractUserEvent {
    private User user;

    public AbstractUserEvent(User user) {
        this.user = user;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }
}
