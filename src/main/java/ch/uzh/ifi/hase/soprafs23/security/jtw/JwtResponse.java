package ch.uzh.ifi.hase.soprafs23.security.jtw;

public class JwtResponse {

    private final Long id;
    private final String token;

    public JwtResponse(Long id, String token) {
        this.id = id;
        this.token = token;
    }

    public Long getId() {
        return id;
    }

    public String getToken() {
        return token;
    }
}