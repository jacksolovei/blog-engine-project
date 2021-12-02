package main.api.response;

import lombok.Getter;
import lombok.Setter;
import main.dto.PostDto;

import java.util.List;

@Getter
@Setter
public class ApiPostListResponse {
    private long count;
    private List<PostDto> posts;
}
