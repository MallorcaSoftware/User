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
import com.mallorcasoftware.user.model.User;
import com.mallorcasoftware.user.service.encoder.PasswordEncoder;
import com.mallorcasoftware.user.service.token.TokenGenerator;

import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import javax.validation.Validator;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;

public class UserService<T extends User> {

    private Validator validator;

    private UserDao<T> userDao;

    private PasswordEncoder passwordEncoder;

    private TokenGenerator tokenGenerator;

    private Integer passwordResetTokenTtl = 300;

    private List<UserListener> userListeners = new ArrayList<UserListener>();

    public UserService(Validator validator, UserDao<T> userDao, PasswordEncoder passwordEncoder,
                       TokenGenerator tokenGenerator, Integer passwordResetTokenTtl) {
        this.validator = validator;
        this.userDao = userDao;
        this.passwordEncoder = passwordEncoder;
        this.tokenGenerator = tokenGenerator;
        this.passwordResetTokenTtl = passwordResetTokenTtl;
    }

    public UserService(Validator validator, UserDao<T> userDao, PasswordEncoder passwordEncoder,
                       TokenGenerator tokenGenerator, Integer passwordResetTokenTtl, List<UserListener> userListeners) {
        this.validator = validator;
        this.userDao = userDao;
        this.passwordEncoder = passwordEncoder;
        this.tokenGenerator = tokenGenerator;
        this.passwordResetTokenTtl = passwordResetTokenTtl;
        this.userListeners = userListeners;
    }

    public T createUser(T user) throws UserAlreadyExistException, ConstraintViolationException {
        if (userDao.findByUsername(user.getUsername()).isPresent()) {
            throw new UserAlreadyExistException();
        }

        validateUser(user);

        user.setPassword(passwordEncoder.encode(user.getPlainPassword()));

        userDao.save(user);

        for (UserListener userListener : userListeners) {
            userListener.onCreateUser(new UserCreatedEvent(user));
        }

        return user;
    }

    public T findUser(Long id) {
        return userDao.findById(id).orElse(null);
    }

    public T findUser(String username) {
        return userDao.findByUsername(username).orElse(null);
    }

    public T findUserByUsernameOrEmail(String value) {
        return userDao.findByUsernameOrEmail(value).orElse(null);
    }

    public void requestPasswordReset(String usernameOrEmail) throws UserNotFoundException {
        T user = userDao.findByUsernameOrEmail(usernameOrEmail).orElse(null);

        if (user == null) {
            throw new UserNotFoundException();
        }

        user.setPasswordResetToken(tokenGenerator.generateToken(user.getEmail()));
        user.setPasswordRequestedAt(new Date());

        userDao.save(user);

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

        user.setPlainPassword(passwordConfirmation);

        validateUser(user);
        user.setPassword(passwordEncoder.encode(user.getPlainPassword()));

        userDao.save(user);

        for (UserListener userListener : userListeners) {
            userListener.onPasswordReset(new PasswordResetEvent(user));
        }
    }

    public void changePassword(T user, String password, String passwordConfirmation) throws PasswordConfirmationNotMatchException, ConstraintViolationException {
        if (!password.equals(passwordConfirmation)) {
            throw new PasswordConfirmationNotMatchException();
        }

        user.setPlainPassword(passwordConfirmation);
        validateUser(user);
        user.setPassword(passwordEncoder.encode(user.getPlainPassword()));

        userDao.save(user);

        for (UserListener userListener : userListeners) {
            userListener.onChangePassword(new PasswordChangedEvent(user));
        }
    }

    public void updateUser(T user) {
        userDao.save(user);
    }

    public void addUserListener(UserListener userListener) {
        userListeners.add(userListener);
    }

    private void validateUser(T user) throws ConstraintViolationException {
        Set<ConstraintViolation<T>> constraintViolations = validator.validate(user);
        if (!constraintViolations.isEmpty()) {
            throw new ConstraintViolationException(constraintViolations);
        }
    }
}
