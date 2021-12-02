package main.api.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PostIdRequest {
    @JsonProperty("post_id")
    private int postId;
}
