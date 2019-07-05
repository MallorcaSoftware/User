package com.mallorcasoftware.user.dao;

import com.mallorcasoftware.user.model.User;

import java.util.Optional;

public interface UserDao<T extends User> {
    /**
     * Fetches user by given username.
     *
     * @param username username to fetch user
     * @return user or null
     */
    Optional<T> findByUsername(String username);

    /**
     * Fetches user by given username or given email
     *
     * @param value given value
     * @return user or null
     */
    Optional<T> findByUsernameOrEmail(String value);

    /**
     * Fetches user by given email.
     *
     * @param email email to fetch user
     * @return user or null
     */
    Optional<T> findByEmail(String email);

    /**
     * Fetches user by given id
     *
     * @param id to fetch user
     * @return user or null
     */
    Optional<T> findById(Long id);

    /**
     * Fetches user by given passwordResetToken
     *
     * @param passwordResetToken to for passwordReset
     * @return user or null
     */
    Optional<T> findByPasswordResetToken(String passwordResetToken);

    /**
     * Saves the given user
     *
     * @param user the user to save
     * @return user or null
     */
    T save(T user);
}
