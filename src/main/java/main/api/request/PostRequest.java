package main.api.request;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class PostRequest {
    private long timestamp;
    private boolean active;
    private String title;
    private List<String> tags;
    private String text;
}
