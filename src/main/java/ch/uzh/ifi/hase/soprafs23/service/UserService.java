package ch.uzh.ifi.hase.soprafs23.service;

import ch.uzh.ifi.hase.soprafs23.constant.UserStatus;
import ch.uzh.ifi.hase.soprafs23.entity.User;
import ch.uzh.ifi.hase.soprafs23.repository.UserRepository;
import ch.uzh.ifi.hase.soprafs23.security.jtw.JwtUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Date;
import java.util.List;
import java.util.Optional;

/**
 * User Service
 * This class is the "worker" and responsible for all functionality related to
 * the user
 * (e.g., it creates, modifies, deletes, finds). The result will be passed back
 * to the caller.
 */
@Service
@Transactional
public class UserService {

    private final Logger log = LoggerFactory.getLogger(UserService.class);

    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;
    @Autowired
    public UserService(@Qualifier("userRepository") UserRepository userRepository, JwtUtil jwtUtil) {
        this.userRepository = userRepository;
        this.jwtUtil = jwtUtil;
    }

    public List<User> getUsers() {
        return this.userRepository.findAll();
    }

    public User getById(Long id) {
        Optional<User> selectedUser = userRepository.findById(id);
        if (selectedUser.isPresent()) {
            return selectedUser.get();
        }
        else {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, String.format("User does not exist"));
        }
    }

    public User createUser(User newUser) {
        newUser.setToken(jwtUtil.generateToken(newUser));
        newUser.setStatus(UserStatus.OFFLINE);
        checkIfUserExists(newUser);
        newUser.setCreationDate(new Date());
        BCryptPasswordEncoder b = new BCryptPasswordEncoder();
        newUser.setPassword(b.encode(newUser.getPassword()));
        newUser = userRepository.save(newUser);
        userRepository.flush();

        log.debug("Created Information for User: {}", newUser);
        return newUser;
    }

    public User getLoginUser(User u) {
        Optional<User> existingUser = userRepository.findByUsername(u.getUsername());
        if (existingUser.isPresent() && existingUser.get().getPassword().equals(u.getPassword())) {
            //update user status to online
            //TODO: set user as online
            // as the user was already found, the username matches
            return existingUser.get();
        }
        else {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, String.format("The provided username does not exist or password does not match"));
        }
    }

    public User getLogoutUser(User u){
        Optional<User> existingUser = userRepository.findByUsername(u.getUsername());
        //TODO: update uesr to offline
        return existingUser.get();
    }

    /**
     * This is a helper method that will check the uniqueness criteria of the
     * username and the name
     * defined in the User entity. The method will do nothing if the input is unique
     * and throw an error otherwise.
     *
     * @param userToBeCreated
     * @throws org.springframework.web.server.ResponseStatusException
     * @see User
     */
    private void checkIfUserExists(User userToBeCreated) {
        Optional<User> userByUsername = userRepository.findByUsername(userToBeCreated.getUsername());
        String baseErrorMessage = "The %s provided %s not unique. Therefore, the user could not be created! Thus the username aleady exists";
        if (userByUsername.isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, String.format(baseErrorMessage, "username", "is"));
        }
    }
}
