package main.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.Setter;

import java.util.Date;

@Getter
@Setter
public class UserDto {
    private int id;
    private String name;
    private String email;
    private String photo;
    @JsonIgnore
    private Date regTime;
    @JsonIgnore
    private String password;
    private boolean moderation;
    private int moderationCount;
    private boolean settings;
}
