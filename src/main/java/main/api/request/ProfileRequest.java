package main.api.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ProfileRequest {
    private String name;
    private String email;
    private String password;
    private int removePhoto;
    private String photo;
}
