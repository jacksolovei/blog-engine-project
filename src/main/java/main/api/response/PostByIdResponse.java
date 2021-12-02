package main.api.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import main.dto.CommentDto;
import main.dto.UserDto;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
public class PostByIdResponse {
    private int id;
    private long timestamp;
    private boolean active;
    @JsonIgnoreProperties({"email", "photo", "moderation", "moderationCount", "settings"})
    private UserDto user;
    private String title;
    private String text;
    private int likeCount;
    private int dislikeCount;
    private int viewCount;
    private List<CommentDto> comments;
    private List<String> tags;
}
