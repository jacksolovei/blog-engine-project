package main.service;

import lombok.AllArgsConstructor;
import main.api.response.TagListResponse;
import main.api.response.TagResponseProjection;
import main.repository.TagRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
public class TagService {
    private final TagRepository tagRepository;

    public TagListResponse getTags(String query) {
        TagListResponse tagListResponse = new TagListResponse();
        List<TagResponseProjection> tags = tagRepository.findAllTags();
        if (query == null) {
            tagListResponse.setTags(tags);
        } else {
            tagListResponse.setTags(tags.stream()
                    .filter(t -> t.getName().equals(query))
                    .collect(Collectors.toList()));
        }
        return tagListResponse;
    }
}
