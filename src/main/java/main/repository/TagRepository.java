package main.repository;

import main.api.response.TagResponseProjection;
import main.model.Tag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TagRepository extends JpaRepository<Tag, Integer> {

    @Query(value = "SELECT tags.name, ROUND(((SELECT COUNT(*) FROM tag2post WHERE tags.id = tag2post.tag_id) " +
            "/ (SELECT COUNT(*) FROM posts WHERE is_active = 1 " +
            "AND moderation_status = 'ACCEPTED' AND time < NOW())) " +
            "* (1 / ((SELECT COUNT(*) AS 'count' FROM posts JOIN tag2post ON posts.id = tag2post.post_id " +
            "JOIN tags ON tags.id = tag2post.tag_id WHERE posts.is_active = 1 AND moderation_status = 'ACCEPTED' " +
            "AND time < NOW() GROUP BY tags.name ORDER BY count DESC LIMIT 1) " +
            "/ (SELECT COUNT(*) FROM posts WHERE is_active = 1 AND moderation_status = 'ACCEPTED' " +
            "AND time < NOW()))), 2) AS weight FROM tags JOIN tag2post ON tags.id = tag2post.tag_id " +
            "JOIN posts ON tag2post.post_id = posts.id " +
            "WHERE is_active = 1 AND moderation_status = 'ACCEPTED' " +
            "AND time < NOW() GROUP BY tags.name",
            nativeQuery = true)
    List<TagResponseProjection> findAllTags();

    Optional<Tag> findTagByName(String name);
}
