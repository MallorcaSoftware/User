package com.mallorcasoftware.user.model;

public class CreateUser<T extends User> {
    private T user;
    private String plainPassword;

    public CreateUser(T user, String plainPassword) {
        this.user = user;
        this.plainPassword = plainPassword;
    }

    public T getUser() {
        return user;
    }

    public String getPlainPassword() {
        return plainPassword;
    }
}
