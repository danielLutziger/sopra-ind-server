package ch.uzh.ifi.hase.soprafs23.rest.dto;

import javax.validation.constraints.NotBlank;
import java.util.Date;

public class UserPostDTO {

    @NotBlank
    private String username;
    @NotBlank
    private String password;
    private Date birthday;


    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public Date getBirthday() {
        return birthday;
    }

    public void setBirthday(Date birthday) {
        this.birthday = birthday;
    }
}
