package main.api.response;

import lombok.Getter;
import lombok.Setter;

import java.util.*;

@Getter
@Setter
public class CalendarResponse {
    Set<String> years;
    Map<String, Integer> posts;
}
