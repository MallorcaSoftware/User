package com.mallorcasoftware.user.service;

import com.mallorcasoftware.user.dao.UserDao;
import com.mallorcasoftware.user.event.PasswordChangedEvent;
import com.mallorcasoftware.user.event.PasswordResetEvent;
import com.mallorcasoftware.user.event.RequestPasswordResetEvent;
import com.mallorcasoftware.user.event.UserCreatedEvent;
import com.mallorcasoftware.user.exception.PasswordConfirmationNotMatchException;
import com.mallorcasoftware.user.exception.PasswordResetTokenNotValidException;
import com.mallorcasoftware.user.exception.UserAlreadyExistException;
import com.mallorcasoftware.user.exception.UserNotFoundException;
import com.mallorcasoftware.user.listener.UserListener;
import com.mallorcasoftware.user.model.CreateUser;
import com.mallorcasoftware.user.model.User;
import com.mallorcasoftware.user.service.encoder.PasswordEncoder;
import com.mallorcasoftware.user.service.token.TokenGenerator;
import com.mallorcasoftware.user.service.validator.UserValidator;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class UserService<T extends User> {

    private UserDao<T> userDao;

    private UserValidator userValidator;

    private PasswordEncoder passwordEncoder;

    private TokenGenerator tokenGenerator;

    private Integer passwordResetTokenTtl = 300;

    private List<UserListener> userListeners = new ArrayList<UserListener>();

    public UserService(UserDao<T> userDao, UserValidator userValidator, PasswordEncoder passwordEncoder,
                       TokenGenerator tokenGenerator, Integer passwordResetTokenTtl) {
        this.userDao = userDao;
        this.userValidator = userValidator;
        this.passwordEncoder = passwordEncoder;
        this.tokenGenerator = tokenGenerator;
        this.passwordResetTokenTtl = passwordResetTokenTtl;
    }

    public UserService(UserDao<T> userDao, PasswordEncoder passwordEncoder,
                       TokenGenerator tokenGenerator, Integer passwordResetTokenTtl, List<UserListener> userListeners) {
        this.userDao = userDao;
        this.passwordEncoder = passwordEncoder;
        this.tokenGenerator = tokenGenerator;
        this.passwordResetTokenTtl = passwordResetTokenTtl;
        this.userListeners = userListeners;
    }

    public T createUser(CreateUser<T> createUser) throws UserAlreadyExistException {
        if (userDao.findByUsername(createUser.getUser().getUsername()).isPresent()) {
            throw new UserAlreadyExistException();
        }

        userValidator.validatePassword(createUser.getPlainPassword());

        createUser.getUser().setPassword(passwordEncoder.encode(createUser.getPlainPassword()));

        userDao.saveUser(createUser.getUser());

        for (UserListener userListener : userListeners) {
            userListener.onCreateUser(new UserCreatedEvent(createUser.getUser()));
        }

        return createUser.getUser();
    }

    public T findUser(Long id) {
        return userDao.findById(id).orElse(null);
    }

    public T findUser(String username) {
        return userDao.findByUsername(username).orElse(null);
    }

    public T findUserByUsernameOrEmail(String value) {
        return userDao.findByUsernameOrEmail(value, value).orElse(null);
    }

    public void requestPasswordReset(String usernameOrEmail) throws UserNotFoundException {
        T user = userDao.findByUsernameOrEmail(usernameOrEmail, usernameOrEmail).orElse(null);

        if (user == null) {
            throw new UserNotFoundException();
        }

        user.setPasswordResetToken(tokenGenerator.generateToken(user.getEmail()));
        user.setPasswordRequestedAt(new Date());

        userDao.saveUser(user);

        for (UserListener userListener : userListeners) {
            userListener.onRequestPasswordReset(new RequestPasswordResetEvent(user));
        }
    }

    public void passwordReset(String token, String password, String passwordConfirmation) throws UserNotFoundException, PasswordResetTokenNotValidException, PasswordConfirmationNotMatchException {
        T user = userDao.findByPasswordResetToken(token).orElse(null);

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

        userValidator.validatePassword(passwordConfirmation);
        user.setPassword(passwordEncoder.encode(passwordConfirmation));

        userDao.saveUser(user);

        for (UserListener userListener : userListeners) {
            userListener.onPasswordReset(new PasswordResetEvent(user));
        }
    }

    public void changePassword(T user, String password, String passwordConfirmation) throws PasswordConfirmationNotMatchException {
        if (!password.equals(passwordConfirmation)) {
            throw new PasswordConfirmationNotMatchException();
        }

        userValidator.validatePassword(passwordConfirmation);
        user.setPassword(passwordEncoder.encode(passwordConfirmation));

        userDao.saveUser(user);

        for (UserListener userListener : userListeners) {
            userListener.onChangePassword(new PasswordChangedEvent(user));
        }
    }

    public void updateUser(T user) {
        userDao.saveUser(user);
    }

    public void addUserListener(UserListener userListener) {
        userListeners.add(userListener);
    }
}
