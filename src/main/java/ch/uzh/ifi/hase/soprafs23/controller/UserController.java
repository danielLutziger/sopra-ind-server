package ch.uzh.ifi.hase.soprafs23.controller;

import ch.uzh.ifi.hase.soprafs23.entity.User;
import ch.uzh.ifi.hase.soprafs23.rest.dto.UserGetDTO;
import ch.uzh.ifi.hase.soprafs23.rest.dto.UserPostDTO;
import ch.uzh.ifi.hase.soprafs23.rest.dto.UserPutDTO;
import ch.uzh.ifi.hase.soprafs23.rest.mapper.DTOMapper;
import ch.uzh.ifi.hase.soprafs23.service.UserService;
import org.mapstruct.Context;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;

/**
 * User Controller
 * This class is responsible for handling all REST request that are related to
 * the user.
 * The controller will receive the request and delegate the execution to the
 * UserService and finally return the result.
 */
@RestController
public class UserController {
    private final UserService userService;

    UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/users")
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    public List<UserGetDTO> getAllUsers() {
        // fetch all users in the internal representation
        List<User> users = userService.getUsers();
        List<UserGetDTO> userGetDTOs = new ArrayList<>();

        // convert each user to the API representation
        for (User user : users) {
            userGetDTOs.add(DTOMapper.INSTANCE.convertEntityToUserGetDTO(user));
        }
        return userGetDTOs;
    }

    @PostMapping("/users")
    @ResponseStatus(HttpStatus.CREATED)
    @ResponseBody
    public ResponseEntity<?> createUser(@RequestBody UserPostDTO userPostDTO) {
        // convert API user to internal representation
        User userInput = DTOMapper.INSTANCE.convertUserPostDTOtoEntity(userPostDTO);
        // create user
        User createdUser = userService.createUser(userInput);
        // add headers
        HttpHeaders headers = new HttpHeaders();
        headers.add("Access-Control-Expose-Headers", "Access-Token, Uid");
        headers.add("Access-Token", createdUser.getToken());
        // convert internal representation of user back to API
        return ResponseEntity.status(HttpStatus.CREATED).headers(headers).body(DTOMapper.INSTANCE.convertEntityToUserGetDTO(createdUser));
    }

    @GetMapping("/users/{id}")
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    public ResponseEntity<?> getUserById(@PathVariable Long id, @Context HttpServletRequest request) {
        // create user
        User currentUser = userService.getById(id);
        // convert internal representation of user back to API
        HttpHeaders headers = new HttpHeaders();
        headers.add("Access-Control-Expose-Headers", "Edit-Access, Uid");
        headers.add("Edit-Access", String.valueOf(userService.editAccess(currentUser, request)));
        return ResponseEntity.status(HttpStatus.OK).headers(headers).body(DTOMapper.INSTANCE.convertEntityToUserGetDTO(currentUser));
    }

    @PutMapping("/users/{id}")
    public ResponseEntity<?> updateUserById(@PathVariable Long id, @RequestBody UserPutDTO userPutDTO, @Context HttpServletRequest request) {
        User currentUser = userService.getById(id);
        User userUpdates = DTOMapper.INSTANCE.convertUserPutDTOtoEntity(userPutDTO);
        currentUser = userService.updateUser(currentUser, userUpdates, request);
        HttpHeaders headers = new HttpHeaders();
        headers.add("Access-Control-Expose-Headers", "Access-Token, Edit-Access, Uid");
        headers.add("Access-Token", currentUser.getToken());
        headers.add("Edit-Access", "true"); // value can be hardcoded as the check is done in update user
        return ResponseEntity.noContent().headers(headers).build();
    }
}
