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
import com.mallorcasoftware.user.model.User;
import com.mallorcasoftware.user.service.encoder.PasswordEncoder;
import com.mallorcasoftware.user.service.token.TokenGenerator;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;

public class UserServiceTest extends BaseTest {
    @Mock
    private UserDao userDao;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private TokenGenerator tokenGenerator;

    @Mock
    private UserListener userListener;

    private UserService userService;

    @Override
    public void initMocks() {
        super.initMocks();

        userService = new UserService(userDao, passwordEncoder, tokenGenerator, 300);
        userService.addUserListener(userListener);
    }

    @Test(expected = UserAlreadyExistException.class)
    public void shouldThrowUserAlreadyExistExceptionIfCreatesDuplicateUser() throws UserAlreadyExistException {
        String expectedUsername = "testUsername";
        User user = new User();
        user.setUsername(expectedUsername);

        when(userDao.findByUsername(expectedUsername)).thenReturn(user);

        userService.createUser(user);

        verify(userDao, times(1)).findByUsername(expectedUsername);
    }

    @Test
    public void shouldEncodePasswordOnCreateUser() throws UserAlreadyExistException {
        String expectedUsername = "testUsername";
        String expectedPassword = "testPassword";
        String expectedEncodedPassword = "encodedPassword";
        User user = Mockito.mock(User.class);

        when(user.getUsername()).thenReturn(expectedUsername);
        when(user.getPassword()).thenReturn(expectedPassword);
        when(passwordEncoder.encode(expectedPassword)).thenReturn(expectedEncodedPassword);

        userService.createUser(user);

        verify(passwordEncoder, times(1)).encode(expectedPassword);
        verify(user, times(1)).setPassword(expectedEncodedPassword);
    }

    @Test
    public void shouldSaveUserOnCreateUser() throws UserAlreadyExistException {
        String expectedUsername = "testUsername";
        User user = Mockito.mock(User.class);

        when(user.getUsername()).thenReturn(expectedUsername);

        userService.createUser(user);

        verify(userDao, times(1)).save(user);
    }

    @Test
    public void shouldCallListenerOnCreateUser() throws UserAlreadyExistException {
        String expectedUsername = "testUsername";
        User user = Mockito.mock(User.class);

        when(user.getUsername()).thenReturn(expectedUsername);

        userService.createUser(user);

        verify(userListener, times(1)).onCreateUser(any(UserCreatedEvent.class));
    }

    @Test
    public void shouldFindUserById() {
        Long expectedId = 12L;
        User expectedUser = Mockito.mock(User.class);

        when(userDao.findById(expectedId)).thenReturn(expectedUser);

        User user = userService.findUser(expectedId);

        assertEquals(expectedUser, user);
        verify(userDao, times(1)).findById(expectedId);
    }

    @Test
    public void shouldFindUserByUsername() {
        String expectedUsername = "testUsername";
        User expectedUser = Mockito.mock(User.class);

        when(userDao.findByUsername(expectedUsername)).thenReturn(expectedUser);

        User user = userService.findUser(expectedUsername);

        assertEquals(expectedUser, user);
        verify(userDao, times(1)).findByUsername(expectedUsername);
    }

    @Test
    public void shouldFindUserByUsernameOrEmail() {
        String expectedUsername = "testUsername";
        User expectedUser = Mockito.mock(User.class);

        when(userDao.findByUsernameOrEmail(expectedUsername)).thenReturn(expectedUser);

        User user = userService.findUserByUsernameOrEmail(expectedUsername);

        assertEquals(expectedUser, user);
        verify(userDao, times(1)).findByUsernameOrEmail(expectedUsername);
    }

    @Test(expected = UserNotFoundException.class)
    public void shouldThrowUserNotFoundExceptionOnRequestPasswordReset() throws UserNotFoundException {
        String usernameOrEmail = "testMail";

        userService.requestPasswordReset(usernameOrEmail);

        verify(userDao, times(1)).findByUsernameOrEmail(usernameOrEmail);
    }

    @Test
    public void shouldSetPasswordResetTokenAndSaveOnRequestPasswordReset() throws UserNotFoundException {
        String usernameOrEmail = "testMail";
        String expectedToken = "expectedToken";
        User expectedUser = Mockito.mock(User.class);

        when(expectedUser.getEmail()).thenReturn(usernameOrEmail);
        when(userDao.findByUsernameOrEmail(usernameOrEmail)).thenReturn(expectedUser);
        when(tokenGenerator.generateToken(usernameOrEmail)).thenReturn(expectedToken);

        userService.requestPasswordReset(usernameOrEmail);

        verify(userDao, times(1)).findByUsernameOrEmail(usernameOrEmail);
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
        when(userDao.findByUsernameOrEmail(usernameOrEmail)).thenReturn(expectedUser);
        when(tokenGenerator.generateToken(usernameOrEmail)).thenReturn(expectedToken);

        userService.requestPasswordReset(usernameOrEmail);

        verify(userListener, times(1)).onRequestPasswordReset(any(RequestPasswordResetEvent.class));
    }

    @Test(expected = UserNotFoundException.class)
    public void shouldThrowUserNotFoundExceptionOnPasswordReset() throws UserNotFoundException, PasswordConfirmationNotMatchException, PasswordResetTokenNotValidException {
        String token = "testToken";
        String password = "password";
        String passwordConfirmation = "password";

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
        when(userDao.findByPasswordResetToken(token)).thenReturn(expectedUser);

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
        when(userDao.findByPasswordResetToken(token)).thenReturn(expectedUser);

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
        when(userDao.findByPasswordResetToken(token)).thenReturn(expectedUser);

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
        when(userDao.findByPasswordResetToken(token)).thenReturn(expectedUser);
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
        when(userDao.findByPasswordResetToken(token)).thenReturn(expectedUser);
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