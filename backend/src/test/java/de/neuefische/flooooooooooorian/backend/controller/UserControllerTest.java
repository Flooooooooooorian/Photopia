package de.neuefische.flooooooooooorian.backend.controller;

import de.neuefische.flooooooooooorian.backend.config.EmailConfig;
import de.neuefische.flooooooooooorian.backend.dto.login.LoginJWTDto;
import de.neuefische.flooooooooooorian.backend.dto.user.ProfileDto;
import de.neuefische.flooooooooooorian.backend.dto.user.UserDto;
import de.neuefische.flooooooooooorian.backend.model.Location;
import de.neuefische.flooooooooooorian.backend.repository.LocationRepository;
import de.neuefische.flooooooooooorian.backend.security.dto.UserCreationDto;
import de.neuefische.flooooooooooorian.backend.security.dto.UserLoginDto;
import de.neuefische.flooooooooooorian.backend.security.model.User;
import de.neuefische.flooooooooooorian.backend.security.repository.UserRepository;
import de.neuefische.flooooooooooorian.backend.security.service.JwtUtilsService;
import de.neuefische.flooooooooooorian.backend.utils.LocationMapper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.http.*;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.TestPropertySource;

import java.util.List;
import java.util.Optional;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = "jwt.secret=testSecret")
class UserControllerTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate testRestTemplate;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private LocationRepository locationRepository;

    @Autowired
    private JwtUtilsService jwtUtilsService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @MockBean
    private EmailConfig emailConfig;

    private JavaMailSenderImpl javaMailSender = mock(JavaMailSenderImpl.class);

    @BeforeEach
    public void clearDb() {
        userRepository.deleteAll();
    }

    @Test
    void validLogin() {
        User user = User.builder()
                .email("test@test.com")
                .avatar_url("avatar_url")
                .enabled(true)
                .full_name("fullname")
                .role("User")
                .password(passwordEncoder.encode("test"))
                .build();

        UserLoginDto userLoginDto = new UserLoginDto(user.getEmail(), "test");

        userRepository.save(user);

        ResponseEntity<LoginJWTDto> response = testRestTemplate.exchange("http://localhost:" + port + "/user/login", HttpMethod.POST, new HttpEntity<>(userLoginDto), LoginJWTDto.class);

        assertThat(response.getStatusCode(), is(HttpStatus.OK));
        assertThat(response.getBody(), notNullValue());
        try {
            jwtUtilsService.parseClaim(response.getBody().getJwt());
        }
        catch (io.jsonwebtoken.MalformedJwtException e) {
            Assertions.fail(e.getMessage());
        }
    }

    @Test
    void notValidPasswordLogin() {
        User user = User.builder()
                .email("test@test.com")
                .avatar_url("avatar_url")
                .enabled(true)
                .full_name("fullname")
                .role("User")
                .password(passwordEncoder.encode("test"))
                .build();

        UserLoginDto userLoginDto = new UserLoginDto(user.getEmail(), "not_valid");

        userRepository.save(user);

        ResponseEntity<String> response = testRestTemplate.exchange("http://localhost:" + port + "/user/login", HttpMethod.POST, new HttpEntity<>(userLoginDto), String.class);

        assertThat(response.getStatusCode(), is(HttpStatus.BAD_REQUEST));
        try {
            jwtUtilsService.parseClaim(response.getBody());
            Assertions.fail();
        }
        catch (io.jsonwebtoken.MalformedJwtException ignored) {

        }
    }

    @Test
    void notValidEmailLogin() {
        User user = User.builder()
                .email("test@test.com")
                .avatar_url("avatar_url")
                .enabled(true)
                .full_name("fullname")
                .role("User")
                .password(passwordEncoder.encode("test"))
                .build();

        UserLoginDto userLoginDto = new UserLoginDto("nonExistingEmail", "test");

        userRepository.save(user);

        ResponseEntity<String> response = testRestTemplate.exchange("http://localhost:" + port + "/user/login", HttpMethod.POST, new HttpEntity<>(userLoginDto), String.class);

        assertThat(response.getStatusCode(), is(HttpStatus.BAD_REQUEST));
        try {
            jwtUtilsService.parseClaim(response.getBody());
            Assertions.fail();
        }
        catch (io.jsonwebtoken.MalformedJwtException ignored) {

        }
    }

    @ParameterizedTest(name = "Password {0}")
    @CsvSource({"Too short, T3s!, false",
            "Too long, T3s!Password!IsToLongForThisShitButNotSureHowLongIsTooLongForAPassword?Is128CharactersAsMaximumLenghtResonableOrShouldItBeLonger?, false",
            "No Lowercase Letter, T3E!PASSWORD, false",
            "No Uppercase Letter, t3s!password, false",
            "No Special Character, T3stPassword, false",
            "No Number, Tes!Password, false",
            "With Withspace, T3s! Password, false",
            "Valid, T3s!PA7sw0rd, true",
    })
    void registeUserPasswordValidation(String textRepresantation, String password, boolean result) {
        UserCreationDto userCreationDto = new UserCreationDto("test@test.com", password, "fullname");
        User expected = User.builder()
                .email(userCreationDto.getEmail())
                .full_name(userCreationDto.getName())
                .role("User")
                .build();

        when(emailConfig.getJavaMailSender()).thenReturn(javaMailSender);

        ResponseEntity<UserDto> response = testRestTemplate.exchange("http://localhost:" + port + "/user/register", HttpMethod.POST, new HttpEntity<>(userCreationDto), UserDto.class);

        assertThat(response.getStatusCode() == HttpStatus.OK, is(result));
        if (result) {
            assertThat(response.getBody(), notNullValue());

            assertThat(response.getBody(), is(UserDto.builder().full_name(expected.getFull_name()).avatar_url(expected.getAvatar_url()).build()));
        }

    }

    @Test
    void registerValidNewUser() {
        UserCreationDto userCreationDto = new UserCreationDto("test@test.com", "test_Password_12412@!", "fullname");
        User expected = User.builder()
                .email(userCreationDto.getEmail())
                .full_name(userCreationDto.getName())
                .role("User")
                .build();

        when(emailConfig.getJavaMailSender()).thenReturn(javaMailSender);

        ResponseEntity<UserDto> response = testRestTemplate.exchange("http://localhost:" + port + "/user/register", HttpMethod.POST, new HttpEntity<>(userCreationDto), UserDto.class);


        assertThat(response.getStatusCode(), is(HttpStatus.OK));
        assertThat(response.getBody(), is(UserDto.builder().full_name(expected.getFull_name()).avatar_url(expected.getAvatar_url()).build()));
    }

    @Test
    void registerNotValidEmailUser() {
        UserCreationDto userCreationDto = new UserCreationDto("test@test.com", "test_password", "fullname");

        User user = User.builder()
                .email(userCreationDto.getEmail())
                .full_name("other name")
                .password(passwordEncoder.encode("other password"))
                .role("User")
                .enabled(true)
                .build();

        userRepository.save(user);

        ResponseEntity<User> response = testRestTemplate.exchange("http://localhost:" + port + "/user/register", HttpMethod.POST, new HttpEntity<>(userCreationDto), User.class);

        assertThat(response.getStatusCode(), is(HttpStatus.BAD_REQUEST));
    }

    @Test
    void getProfile() {
        HttpHeaders headers = getHttpHeaderWithAuthToken();

        User user = userRepository.findUserByEmail("test_email").get();

        User user2 = userRepository.save(User.builder().id("u2").email("second_mail").enabled(true).role("User").build());

        Location l1 = Location.builder().id("l1").title("title 1").owner(user).build();
        Location l2 = Location.builder().id("l2").title("title 1").owner(user).build();
        Location l3 = Location.builder().id("l3").title("title 1").owner(user2).build();
        locationRepository.save(l1);
        locationRepository.save(l2);
        locationRepository.save(l3);

        user.setFavoriteLocationIds(List.of(l1.getId(), l3.getId()));
        userRepository.save(user);

        ResponseEntity<ProfileDto> response = testRestTemplate.exchange("/user/profile", HttpMethod.GET, new HttpEntity<>(headers), ProfileDto.class);

        assertThat(response.getStatusCode(), is(HttpStatus.OK));
        assertThat(response.getBody(), notNullValue());
        assertThat(response.getBody().getUser(), is(UserDto.builder().full_name(user.getFull_name()).avatar_url(user.getAvatar_url()).build()));
        assertThat(response.getBody().getLocations(), containsInAnyOrder(LocationMapper.toLocationDto(l1, java.util.Optional.of(user)), LocationMapper.toLocationDto(l2, Optional.of(user))));
        assertThat(response.getBody().getFavorites(), containsInAnyOrder(LocationMapper.toLocationDto(l1, java.util.Optional.of(user)), LocationMapper.toLocationDto(l3, Optional.of(user))));
    }

    private HttpHeaders getHttpHeaderWithAuthToken() {
        userRepository.save(User.builder().enabled(true).email("test_email").role("User").password(passwordEncoder.encode("test_password")).build());
        UserLoginDto loginData = new UserLoginDto("test_email", "test_password");
        ResponseEntity<LoginJWTDto> tokenResponse = testRestTemplate.postForEntity("http://localhost:" + port + "/user/login", loginData, LoginJWTDto.class);
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(tokenResponse.getBody().getJwt());
        return headers;
    }
}
