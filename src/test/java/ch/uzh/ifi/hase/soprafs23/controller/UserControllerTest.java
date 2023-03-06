package ch.uzh.ifi.hase.soprafs23.controller;

import ch.uzh.ifi.hase.soprafs23.constant.UserStatus;
import ch.uzh.ifi.hase.soprafs23.entity.User;
import ch.uzh.ifi.hase.soprafs23.repository.UserRepository;
import ch.uzh.ifi.hase.soprafs23.rest.dto.UserGetDTO;
import ch.uzh.ifi.hase.soprafs23.rest.dto.UserPostDTO;
import ch.uzh.ifi.hase.soprafs23.rest.dto.UserPutDTO;
import ch.uzh.ifi.hase.soprafs23.rest.mapper.DTOMapper;
import ch.uzh.ifi.hase.soprafs23.security.jtw.JwtAuthenticationEntryPoint;
import ch.uzh.ifi.hase.soprafs23.security.jtw.JwtUtil;
import ch.uzh.ifi.hase.soprafs23.service.UserDetailsServiceImpl;
import ch.uzh.ifi.hase.soprafs23.service.UserService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.web.server.ResponseStatusException;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * UserControllerTest
 * This is a WebMvcTest which allows to test the UserController i.e. GET/POST
 * request without actually sending them over the network.
 * This tests if the UserController works.
 */

@WebMvcTest(UserController.class)
@WithMockUser
public class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserService userService;

    @MockBean
    private JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;

    @MockBean
    private JwtUtil jwtUtil;

    @MockBean
    private UserDetailsServiceImpl userDetailsService;

    @MockBean
    private AuthenticationManager authenticationManager;

    @Mock
    private UserRepository userRepository;

    @Test
    public void givenUsers_whenGetUsers_thenReturnJsonArray() throws Exception {
        // given
        User user = new User();
        user.setUsername("firstname@lastname");
        user.setStatus(UserStatus.OFFLINE);

        List<User> allUsers = Collections.singletonList(user);

        // this mocks the UserService -> we define above what the userService should
        // return when getUsers() is called
        given(userService.getUsers()).willReturn(allUsers);

        // when
        MockHttpServletRequestBuilder getRequest = get("/users").contentType(MediaType.APPLICATION_JSON);

        // then
        mockMvc.perform(getRequest).andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].username", is(user.getUsername())))
                .andExpect(jsonPath("$[0].status", is(user.getStatus().toString())));
    }

    @Test
    public void createUser_validInput_userCreated() throws Exception {
        // given
        User user = new User();
        user.setId(1L);
        user.setUsername("testUsername");
        user.setToken("1");
        user.setStatus(UserStatus.ONLINE);
        user.setPassword("asdf");

        UserPostDTO userPostDTO = new UserPostDTO();
        userPostDTO.setUsername("testUsername");
        userPostDTO.setPassword("asdf");

        given(userService.createUser(Mockito.any())).willReturn(user);

        // when/then -> do the request + validate the result
        MockHttpServletRequestBuilder postRequest = post("/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(asJsonString(userPostDTO));

        // then
        mockMvc.perform(postRequest)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", is(user.getId().intValue())))
                .andExpect(jsonPath("$.username", is(user.getUsername())))
                .andExpect(jsonPath("$.status", is(user.getStatus().toString())));
    }

    @Test
    public void select_ValidUser_UserReturned() throws Exception {
        // create a single user
        User user = new User();
        user.setId(1L);
        user.setUsername("testUsername");
        user.setStatus(UserStatus.ONLINE);

        given(userService.getById(1L)).willReturn(user);

        // when/then -> do the request + validate the result
        MockHttpServletRequestBuilder getRequest = get("/users/1");

        // then
        mockMvc.perform(getRequest)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(user.getId().intValue())))
                .andExpect(jsonPath("$.username", is(user.getUsername())))
                .andExpect(jsonPath("$.status", is(user.getStatus().toString())));
    }

    @Test
    public void select_InvalidUser_ErrorThrown() throws Exception {
        given(userService.getById(2L)).willThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "User with id 2 does not exist"));
        //userService = new UserService(userRepository, jwtTokenUtil);
        //Mockito.when(userService.getById(2L)).thenCallRealMethod();
        //Mockito.when(userRepository.findById(2L)).thenReturn(null);

        MockHttpServletRequestBuilder getRequest = get("/users/2");

        // then
        mockMvc.perform(getRequest)
                .andExpect(result -> assertTrue(result.getResolvedException() instanceof ResponseStatusException))
                .andExpect(result -> assertEquals(HttpStatus.NOT_FOUND, ((ResponseStatusException) result.getResolvedException()).getStatus()))
                .andExpect(result -> assertEquals("404 NOT_FOUND \"User with id 2 does not exist\"", ((ResponseStatusException) result.getResolvedException()).getMessage()));
    }

    @Test
    public void updateUser_ValidUserUpdated() throws Exception {
        // given
        User user = new User();
        user.setId(1L);
        user.setUsername("user1");
        user.setToken("1");
        user.setStatus(UserStatus.ONLINE);
        user.setPassword("asdf");

        UserPutDTO userPutDto = new UserPutDTO();
        userPutDto.setUsername("testUsername");
        userPutDto.setBirthday(new Date());

        User updatedUser = new User();
        updatedUser.setId(1L);
        updatedUser.setUsername("testUsername");
        updatedUser.setToken("12345");
        updatedUser.setStatus(UserStatus.ONLINE);
        updatedUser.setPassword("asdf");
        updatedUser.setBirthday(new Date());

        // Metaprogramming the mocks Set newJwtUtil using reflection
        Field jwt = UserService.class.getDeclaredField("jwtUtil");
        jwt.setAccessible(true);
        jwt.set(userService, jwtUtil);
        Field repo = UserService.class.getDeclaredField("userRepository");
        repo.setAccessible(true);
        repo.set(userService, userRepository);

        Mockito.when(jwtUtil.generateToken((User) Mockito.any())).thenReturn("12345");
        Mockito.when(userRepository.findById(Mockito.any())).thenReturn(Optional.of(user));
        Mockito.when(userRepository.save(Mockito.any())).thenReturn(updatedUser);

        Mockito.when(userService.getById(Mockito.any())).thenReturn(user);
        Mockito.when(userService.editAccess(Mockito.any(), Mockito.any())).thenReturn(true);
        Mockito.when(userService.updateUser(Mockito.any(), Mockito.any(), Mockito.any())).thenCallRealMethod();

        // when/then -> do the request + validate the result
        MockHttpServletRequestBuilder putRequest = put("/users/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(asJsonString(userPutDto));

        // then
        mockMvc.perform(putRequest)
                .andExpect(status().isNoContent());
    }

    @Test
    public void updateInvalidUser_ErrorThrown() throws Exception {
        given(userService.getById(2L)).willThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "User with id 2 does not exist"));

        UserPutDTO userPutDto = new UserPutDTO();
        userPutDto.setUsername("testUsername");

        MockHttpServletRequestBuilder putRequest = put("/users/2")
                .contentType(MediaType.APPLICATION_JSON)
                .content(asJsonString(userPutDto));

        // then
        mockMvc.perform(putRequest)
                .andExpect(result -> assertTrue(result.getResolvedException() instanceof ResponseStatusException))
                .andExpect(result -> assertEquals(HttpStatus.NOT_FOUND, ((ResponseStatusException) result.getResolvedException()).getStatus()))
                .andExpect(result -> assertEquals("404 NOT_FOUND \"User with id 2 does not exist\"", ((ResponseStatusException) result.getResolvedException()).getMessage()));
    }


    /**
     * Helper Method to convert userPostDTO into a JSON string such that the input
     * can be processed
     * Input will look like this: {"name": "Test User", "username": "testUsername"}
     *
     * @param object
     * @return string
     */
    private String asJsonString(final Object object) {
        try {
            return new ObjectMapper().writeValueAsString(object);
        }
        catch (JsonProcessingException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    String.format("The request body could not be created.%s", e.toString()));
        }
    }
}