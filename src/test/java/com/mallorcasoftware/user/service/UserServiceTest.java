package com.mallorcasoftware.user.service;

import com.mallorcasoftware.user.BaseTest;
import com.mallorcasoftware.user.dao.UserDao;
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
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Optional;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;

public class UserServiceTest extends BaseTest {

    @Mock
    private UserDao userDao;

    @Mock
    private UserValidator userValidator;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private TokenGenerator tokenGenerator;

    @Mock
    private UserListener userListener;

    private UserService<User> userService;

    @Override
    public void initMocks() {
        super.initMocks();

        userService = new UserService<User>(userDao, userValidator, passwordEncoder, tokenGenerator, 300);
        userService.addUserListener(userListener);
    }

    @Test(expected = UserAlreadyExistException.class)
    public void shouldThrowUserAlreadyExistExceptionIfCreatesDuplicateUser() throws UserAlreadyExistException {
        String expectedUsername = "testUsername";
        User user = Mockito.mock(User.class);

        when(user.getUsername()).thenReturn(expectedUsername);
        when(userDao.findByUsername(expectedUsername)).thenReturn(Optional.of(user));

        userService.createUser(new CreateUser<>(user, ""));

        verify(userDao).findByUsername(expectedUsername);
    }

    @Test
    public void shouldEncodePasswordOnCreateUser() throws UserAlreadyExistException {
        String expectedUsername = "testUsername";
        String expectedPassword = "testPassword";
        String expectedEncodedPassword = "encodedPassword";
        User user = Mockito.mock(User.class);
        CreateUser<User> createUser = new CreateUser<>(user, expectedPassword);

        when(user.getUsername()).thenReturn(expectedUsername);
        when(passwordEncoder.encode(expectedPassword)).thenReturn(expectedEncodedPassword);
        when(userDao.findByUsername(expectedUsername)).thenReturn(Optional.empty());

        userService.createUser(createUser);

        verify(userValidator).validatePassword(expectedPassword);
        verify(passwordEncoder).encode(expectedPassword);
        verify(user).setPassword(expectedEncodedPassword);
    }

    @Test
    public void shouldSaveUserOnCreateUser() throws UserAlreadyExistException {
        String expectedUsername = "testUsername";
        User user = Mockito.mock(User.class);

        when(user.getUsername()).thenReturn(expectedUsername);
        when(userDao.findByUsername(expectedUsername)).thenReturn(Optional.empty());

        userService.createUser(new CreateUser<>(user, "expectedPassword"));

        verify(userDao).save(user);
    }

    @Test
    public void shouldCallListenerOnCreateUser() throws UserAlreadyExistException {
        String expectedUsername = "testUsername";
        User user = Mockito.mock(User.class);

        when(userDao.findByUsername(expectedUsername)).thenReturn(Optional.empty());
        when(user.getUsername()).thenReturn(expectedUsername);

        userService.createUser(new CreateUser<>(user, "expectedPassword"));

        verify(userListener, times(1)).onCreateUser(any(UserCreatedEvent.class));
    }

    @Test
    public void shouldFindUserById() {
        Long expectedId = 12L;
        User expectedUser = Mockito.mock(User.class);

        when(userDao.findById(expectedId)).thenReturn(Optional.of(expectedUser));

        User user = userService.findUser(expectedId);

        assertEquals(expectedUser, user);
        verify(userDao, times(1)).findById(expectedId);
    }

    @Test
    public void shouldFindUserByUsername() {
        String expectedUsername = "testUsername";
        User expectedUser = Mockito.mock(User.class);

        when(userDao.findByUsername(expectedUsername)).thenReturn(Optional.of(expectedUser));

        User user = userService.findUser(expectedUsername);

        assertEquals(expectedUser, user);
        verify(userDao, times(1)).findByUsername(expectedUsername);
    }

    @Test
    public void shouldFindUserByUsernameOrEmail() {
        String expectedUsername = "testUsername";
        User expectedUser = Mockito.mock(User.class);

        when(userDao.findByUsernameOrEmail(expectedUsername, expectedUsername)).thenReturn(Optional.of(expectedUser));

        User user = userService.findUserByUsernameOrEmail(expectedUsername);

        assertEquals(expectedUser, user);
        verify(userDao, times(1)).findByUsernameOrEmail(expectedUsername, expectedUsername);
    }

    @Test(expected = UserNotFoundException.class)
    public void shouldThrowUserNotFoundExceptionOnRequestPasswordReset() throws UserNotFoundException {
        String usernameOrEmail = "testMail";

        when(userDao.findByUsernameOrEmail(usernameOrEmail, usernameOrEmail)).thenReturn(Optional.empty());

        userService.requestPasswordReset(usernameOrEmail);

        verify(userDao, times(1)).findByUsernameOrEmail(usernameOrEmail, usernameOrEmail);
    }

    @Test
    public void shouldSetPasswordResetTokenAndSaveOnRequestPasswordReset() throws UserNotFoundException {
        String usernameOrEmail = "testMail";
        String expectedToken = "expectedToken";
        User expectedUser = Mockito.mock(User.class);

        when(expectedUser.getEmail()).thenReturn(usernameOrEmail);
        when(userDao.findByUsernameOrEmail(usernameOrEmail, usernameOrEmail)).thenReturn(Optional.of(expectedUser));
        when(tokenGenerator.generateToken(usernameOrEmail)).thenReturn(expectedToken);

        userService.requestPasswordReset(usernameOrEmail);

        verify(userDao, times(1)).findByUsernameOrEmail(usernameOrEmail, usernameOrEmail);
        verify(tokenGenerator, times(1)).generateToken(usernameOrEmail);
        verify(expectedUser, times(1)).setPasswordResetToken(expectedToken);
        verify(userDao, times(1)).save(expectedUser);
    }

    @Test
    public void shouldCallListenerOnRequestPasswordReset() throws UserNotFoundException {
        String usernameOrEmail = "testMail";
        String expectedToken = "expectedToken";
        User expectedUser = Mockito.mock(User.class);

        when(expectedUser.getEmail()).thenReturn(usernameOrEmail);
        when(userDao.findByUsernameOrEmail(usernameOrEmail, usernameOrEmail)).thenReturn(Optional.of(expectedUser));
        when(tokenGenerator.generateToken(usernameOrEmail)).thenReturn(expectedToken);

        userService.requestPasswordReset(usernameOrEmail);

        verify(userListener, times(1)).onRequestPasswordReset(any(RequestPasswordResetEvent.class));
    }

    @Test(expected = UserNotFoundException.class)
    public void shouldThrowUserNotFoundExceptionOnPasswordReset() throws UserNotFoundException, PasswordConfirmationNotMatchException, PasswordResetTokenNotValidException {
        String token = "testToken";
        String password = "password";
        String passwordConfirmation = "password";

        when(userDao.findByPasswordResetToken(token)).thenReturn(Optional.empty());

        userService.passwordReset(token, password, passwordConfirmation);

        verify(userDao, times(1)).findByPasswordResetToken(token);
    }

    @Test(expected = PasswordResetTokenNotValidException.class)
    public void shouldThrowPasswordResetTokenNotValidExceptionOnPasswordResetIfTokenNotEquals() throws UserNotFoundException, PasswordConfirmationNotMatchException, PasswordResetTokenNotValidException {
        String token = "testToken";
        String wrongToken = "testToken1";
        String password = "password";
        String passwordConfirmation = "password";
        User expectedUser = Mockito.mock(User.class);

        when(expectedUser.getPasswordResetToken()).thenReturn(wrongToken);
        when(userDao.findByPasswordResetToken(token)).thenReturn(Optional.of(expectedUser));

        userService.passwordReset(token, password, passwordConfirmation);

        verify(userDao, times(1)).findByPasswordResetToken(token);
    }

    @Test(expected = PasswordResetTokenNotValidException.class)
    public void shouldThrowPasswordResetTokenNotValidExceptionOnPasswordResetIfTokenIsExpired() throws UserNotFoundException, PasswordConfirmationNotMatchException, PasswordResetTokenNotValidException, ParseException {
        String token = "testToken";
        String password = "password";
        String passwordConfirmation = "password";
        User expectedUser = Mockito.mock(User.class);
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        Date passwordRequestedAt = sdf.parse("2015-05-26");

        when(expectedUser.getPasswordResetToken()).thenReturn(token);
        when(expectedUser.getPasswordRequestedAt()).thenReturn(passwordRequestedAt);
        when(userDao.findByPasswordResetToken(token)).thenReturn(Optional.of(expectedUser));

        userService.passwordReset(token, password, passwordConfirmation);

        verify(userDao, times(1)).findByPasswordResetToken(token);
    }

    @Test(expected = PasswordConfirmationNotMatchException.class)
    public void shouldThrowPasswordResetTokenNotValidExceptionOnPasswordResetIfPasswordConfirmationDoesNotMatch() throws UserNotFoundException, PasswordConfirmationNotMatchException, PasswordResetTokenNotValidException {
        String token = "testToken";
        String password = "password";
        String passwordConfirmation = "password123";
        Date passwordRequestedAt = new Date();
        User expectedUser = Mockito.mock(User.class);

        when(expectedUser.getPasswordResetToken()).thenReturn(token);
        when(expectedUser.getPasswordRequestedAt()).thenReturn(passwordRequestedAt);
        when(userDao.findByPasswordResetToken(token)).thenReturn(Optional.of(expectedUser));

        userService.passwordReset(token, password, passwordConfirmation);

        verify(userDao, times(1)).findByPasswordResetToken(token);
    }

    @Test
    public void shouldEncodePasswordAndSaveOnPasswordReset() throws UserNotFoundException, PasswordConfirmationNotMatchException, PasswordResetTokenNotValidException {
        String token = "testToken";
        String password = "password";
        String passwordConfirmation = "password";
        String encodedPassword = "encodedPassword";
        Date passwordRequestedAt = new Date();
        User expectedUser = Mockito.mock(User.class);

        when(expectedUser.getPasswordResetToken()).thenReturn(token);
        when(expectedUser.getPasswordRequestedAt()).thenReturn(passwordRequestedAt);
        when(userDao.findByPasswordResetToken(token)).thenReturn(Optional.of(expectedUser));
        when(passwordEncoder.encode(password)).thenReturn(encodedPassword);

        userService.passwordReset(token, password, passwordConfirmation);

        verify(expectedUser, times(1)).setPassword(encodedPassword);
        verify(userDao, times(1)).findByPasswordResetToken(token);
        verify(userDao, times(1)).save(expectedUser);
    }

    @Test
    public void shouldCallListenerOnPasswordReset() throws UserNotFoundException, PasswordConfirmationNotMatchException, PasswordResetTokenNotValidException {
        String token = "testToken";
        String password = "password";
        String passwordConfirmation = "password";
        String encodedPassword = "encodedPassword";
        Date passwordRequestedAt = new Date();
        User expectedUser = Mockito.mock(User.class);

        when(expectedUser.getPasswordResetToken()).thenReturn(token);
        when(expectedUser.getPasswordRequestedAt()).thenReturn(passwordRequestedAt);
        when(userDao.findByPasswordResetToken(token)).thenReturn(Optional.of(expectedUser));
        when(passwordEncoder.encode(password)).thenReturn(encodedPassword);

        userService.passwordReset(token, password, passwordConfirmation);

        verify(expectedUser, times(1)).setPassword(encodedPassword);
        verify(userDao, times(1)).findByPasswordResetToken(token);
        verify(userDao, times(1)).save(expectedUser);
        verify(userListener, times(1)).onPasswordReset(any(PasswordResetEvent.class));
    }

    @Test(expected = PasswordConfirmationNotMatchException.class)
    public void shouldThrowPasswordConfirmationNotMatchExceptionOnChangePasswordIfPasswordConfirmationDoesNotMatch() throws PasswordConfirmationNotMatchException {
        User user = Mockito.mock(User.class);
        String password = "password";
        String passwordConfirmation = "password123";

        userService.changePassword(user, password, passwordConfirmation);
    }

    @Test
    public void shouldEncodePasswordAndSaveOnChangePassword() throws PasswordConfirmationNotMatchException {
        User user = Mockito.mock(User.class);
        String password = "password";
        String passwordConfirmation = "password";
        String encodedPassword = "encodedPassword";

        when(passwordEncoder.encode(password)).thenReturn(encodedPassword);

        userService.changePassword(user, password, passwordConfirmation);

        verify(user, times(1)).setPassword(encodedPassword);
        verify(userDao, times(1)).save(user);
    }

    @Test
    public void shouldUpdateUser() {
        User expectedUser = Mockito.mock(User.class);

        userService.updateUser(expectedUser);

        verify(userDao, times(1)).save(expectedUser);
    }
}
