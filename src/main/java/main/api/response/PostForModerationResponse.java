package main.api.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import main.dto.UserDto;

@Getter
@Setter
@AllArgsConstructor
public class PostForModerationResponse {
    private int id;
    private long timestamp;
    private String title;
    private String announce;
    private int likeCount;
    private int dislikeCount;
    private int commentCount;
    private int viewCount;
    @JsonIgnoreProperties({"email", "photo", "moderation", "moderationCount", "settings"})
    private UserDto user;

}
