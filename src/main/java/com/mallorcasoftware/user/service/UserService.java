package com.mallorcasoftware.user.service;

import com.mallorcasoftware.user.dao.UserDao;
import com.mallorcasoftware.user.exception.PasswordConfirmationNotMatchException;
import com.mallorcasoftware.user.exception.PasswordResetTokenNotValidException;
import com.mallorcasoftware.user.exception.UserAlreadyExistException;
import com.mallorcasoftware.user.exception.UserNotFoundException;
import com.mallorcasoftware.user.model.User;
import com.mallorcasoftware.user.service.encoder.PasswordEncoder;
import com.mallorcasoftware.user.service.notification.UserNotificator;
import com.mallorcasoftware.user.service.token.TokenGenerator;

import java.util.Date;

public class UserService {
    private UserDao userDao;

    private PasswordEncoder passwordEncoder;

    private TokenGenerator tokenGenerator;

    private UserNotificator userNotificator;

    private Integer passwordResetTokenTtl = 300;

    public UserService(UserDao userDao, PasswordEncoder passwordEncoder, TokenGenerator tokenGenerator, UserNotificator userNotificator) {
        this.userDao = userDao;
        this.passwordEncoder = passwordEncoder;
        this.tokenGenerator = tokenGenerator;
        this.userNotificator = userNotificator;
    }

    public UserService(UserDao userDao, PasswordEncoder passwordEncoder, TokenGenerator tokenGenerator, UserNotificator userNotificator, Integer passwordResetTokenTtl) {
        this.userDao = userDao;
        this.passwordEncoder = passwordEncoder;
        this.tokenGenerator = tokenGenerator;
        this.userNotificator = userNotificator;
        this.passwordResetTokenTtl = passwordResetTokenTtl;
    }

    public User createUser(User user) throws UserAlreadyExistException {
        if (userDao.findByUsername(user.getUsername()) != null) {
            throw new UserAlreadyExistException();
        }

        user.setPassword(passwordEncoder.encode(user.getPassword()));

        userDao.save(user);
        userNotificator.sendUserRegistrationNotification(user);

        return user;
    }

    public User findUser(Long id) {
        return userDao.findById(id);
    }

    public User findUser(String username) {
        return userDao.findByUsername(username);
    }

    public User findUserByUsernameOrEmail(String value) {
        return userDao.findByUsernameOrEmail(value);
    }

    public void requestPasswordReset(String usernameOrEmail) throws UserNotFoundException {
        User user = userDao.findByUsernameOrEmail(usernameOrEmail);

        if (user == null) {
            throw new UserNotFoundException();
        }

        user.setPasswordResetToken(tokenGenerator.generateToken(user.getEmail()));
        user.setPasswordRequestedAt(new Date());

        userDao.save(user);

        userNotificator.sendPasswordResetNotification(user);
    }

    public void passwordReset(String token, String password, String passwordConfirmation) throws UserNotFoundException, PasswordResetTokenNotValidException, PasswordConfirmationNotMatchException {
        User user = userDao.findByPasswordResetToken(token);

        if (user == null) {
            throw new UserNotFoundException();
        }

        if (!user.getPasswordResetToken().equals(token)) {
            throw new PasswordResetTokenNotValidException();
        }

        Date currentDate = new Date();
        long diffSeconds = (currentDate.getTime() - user.getPasswordRequestedAt().getTime()) / 1000;

        if (diffSeconds > passwordResetTokenTtl) {
            throw new PasswordResetTokenNotValidException();
        }

        if (!password.equals(passwordConfirmation)) {
            throw new PasswordConfirmationNotMatchException();
        }

        user.setPassword(passwordEncoder.encode(passwordConfirmation));

        userDao.save(user);

        userNotificator.sendPasswordResetedNotification(user);
    }

    public void changePassword(User user, String password, String passwordConfirmation) throws PasswordConfirmationNotMatchException {
        if (!password.equals(passwordConfirmation)) {
            throw new PasswordConfirmationNotMatchException();
        }

        user.setPassword(passwordEncoder.encode(passwordConfirmation));

        userDao.save(user);
    }

    public void updateUser(User user) {
        userDao.save(user);
    }
}