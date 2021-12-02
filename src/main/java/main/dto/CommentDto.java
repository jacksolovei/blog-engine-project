package main.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CommentDto {
    private int id;
    private long timestamp;
    private String text;
    @JsonIgnoreProperties({"email", "moderation", "moderationCount", "settings"})
    private UserDto user;
}
