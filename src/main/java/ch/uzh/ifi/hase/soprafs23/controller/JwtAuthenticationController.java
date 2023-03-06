package ch.uzh.ifi.hase.soprafs23.controller;

import ch.uzh.ifi.hase.soprafs23.entity.User;
import ch.uzh.ifi.hase.soprafs23.rest.dto.UserPostDTO;
import ch.uzh.ifi.hase.soprafs23.rest.mapper.DTOMapper;
import ch.uzh.ifi.hase.soprafs23.security.jtw.JwtResponse;
import ch.uzh.ifi.hase.soprafs23.security.jtw.JwtUtil;
import ch.uzh.ifi.hase.soprafs23.service.UserDetailsServiceImpl;
import ch.uzh.ifi.hase.soprafs23.service.UserService;
import org.mapstruct.Context;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.net.URI;

@RestController
public class JwtAuthenticationController {

    @Autowired
    private AuthenticationManager authenticationManager;

    private final UserService userService;

    public JwtAuthenticationController(UserService userService) {
        this.userService = userService;
    }


    @PostMapping("/login")
    public ResponseEntity<?> createAuthenticationToken(@RequestBody UserPostDTO authenticationRequest) throws AuthenticationException {

        try {
            authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(authenticationRequest.getUsername(), authenticationRequest.getPassword()));
        } catch (AuthenticationException e){
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User with username "+ authenticationRequest.getUsername()+ " does not exist");
        }
        User loginUser = userService.getByUsername(authenticationRequest.getUsername());
        userService.getLoginUser(loginUser);
        HttpHeaders headers = new HttpHeaders();
        headers.add("Access-Control-Expose-Headers", "Access-Token, Uid");
        headers.add("Access-Token", loginUser.getToken());
        return ResponseEntity.status(HttpStatus.OK).headers(headers).body(DTOMapper.INSTANCE.convertEntityToUserGetDTO(loginUser));
    }
    @PutMapping("/signout")
    public ResponseEntity<?> logoutUser(@Context HttpServletRequest request) throws AuthenticationException {
        try{userService.getLogoutUser(request);}
        catch (Exception e){
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Your userid got manipulated!");
        }
        return ResponseEntity.ok().build();
    }
}
