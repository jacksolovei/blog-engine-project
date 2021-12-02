package main.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TagDto {
    @JsonIgnore
    private int id;
    private String name;
    private double weight;
}
