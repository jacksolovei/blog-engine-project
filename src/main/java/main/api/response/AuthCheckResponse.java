package main.api.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.Setter;
import main.dto.UserDto;

@Getter
@Setter
public class AuthCheckResponse {
    private boolean result;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private UserDto user;
}
