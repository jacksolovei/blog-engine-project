package main.repository;

import main.api.response.CalendarPostProjection;
import main.model.Post;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PostRepository extends JpaRepository<Post, Integer> {

    @Query(value = "SELECT * FROM posts WHERE is_active = 1 AND moderation_status = 'ACCEPTED' " +
            "AND time <= NOW() ORDER BY time DESC",
            countQuery = "SELECT COUNT(*) FROM posts WHERE is_active = 1 " +
                    "AND moderation_status = 'ACCEPTED' AND time <= NOW()",
            nativeQuery = true)
    Page<Post> findRecentPosts(Pageable pageable);

    @Query(value = "SELECT * FROM posts WHERE is_active = 1 AND moderation_status = 'ACCEPTED' " +
            "AND time <= NOW()",
            nativeQuery = true)
    List<Post> findActivePosts();

    @Query(value = "SELECT * FROM posts WHERE is_active = 1 AND moderation_status = 'ACCEPTED' " +
            "AND time <= NOW() ORDER BY time ASC",
            countQuery = "SELECT COUNT(*) FROM posts WHERE is_active = 1 " +
                    "AND moderation_status = 'ACCEPTED' AND time <= NOW()",
            nativeQuery = true)
    Page<Post> findEarlyPosts(Pageable pageable);

    @Query(value = "SELECT * FROM posts WHERE is_active = 1 " +
            "AND moderation_status = 'ACCEPTED' AND posts.time <= NOW() " +
            "ORDER BY (SELECT COUNT(*) FROM post_comments WHERE post_id = posts.id) DESC",
            countQuery = "SELECT COUNT(*) FROM posts WHERE is_active = 1 " +
                    "AND moderation_status = 'ACCEPTED' AND posts.time <= NOW()",
            nativeQuery = true)
    Page<Post> findPopularPosts(Pageable pageable);

    @Query(value = "SELECT * FROM posts WHERE is_active = 1 AND moderation_status = 'ACCEPTED' " +
            "AND posts.time <= NOW() ORDER BY (SELECT COUNT(*) FROM post_votes " +
            "WHERE post_id = posts.id AND value = 1) DESC",
            countQuery = "SELECT COUNT(*) FROM posts WHERE is_active = 1 " +
                    "AND moderation_status = 'ACCEPTED' AND posts.time <= NOW()",
            nativeQuery = true)
    Page<Post> findBestPosts(Pageable pageable);

    @Query(value = "SELECT COUNT(*) FROM posts WHERE is_active = 1 " +
            "AND moderation_status = 'ACCEPTED' AND time <= NOW()",
            nativeQuery = true)
    int findActivePostsCount();

    @Query(value = "SELECT COUNT(post_comments.id) FROM posts " +
            "JOIN post_comments ON posts.id = post_comments.post_id WHERE posts.id = :postId " +
            "AND posts.is_active = 1 AND posts.moderation_status = 'ACCEPTED' " +
            "AND posts.time <= NOW()", nativeQuery = true)
    int findPostCommentsCount(@Param("postId") int postId);

    @Query(value = "SELECT COUNT(post_votes.id) FROM posts " +
            "JOIN post_votes ON posts.id = post_votes.post_id WHERE posts.id = :postId " +
            "AND posts.is_active = 1 AND posts.moderation_status = 'ACCEPTED' " +
            "AND posts.time <= NOW() AND post_votes.value = 1",
            nativeQuery = true)
    int findPostLikesCount(@Param("postId") int postId);

    @Query(value = "SELECT COUNT(post_votes.id) FROM posts " +
            "JOIN post_votes ON posts.id = post_votes.post_id WHERE posts.id = :postId " +
            "AND posts.is_active = 1 AND posts.moderation_status = 'ACCEPTED' " +
            "AND posts.time <= NOW() AND post_votes.value = -1",
            nativeQuery = true)
    int findPostDislikesCount(@Param("postId") int postId);

    @Query(value = "SELECT COUNT(*) FROM posts WHERE is_active = 1 AND moderation_status = 'NEW'",
            nativeQuery = true)
    int findUnmoderatedPostsCount();

    @Query(value = "SELECT * FROM posts WHERE is_active = 1 " +
            "AND moderation_status = 'ACCEPTED' AND time <= NOW() " +
            "AND LOCATE(:query, CONCAT(text, title)) > 0 ORDER BY time DESC",
            countQuery = "SELECT COUNT(*) FROM posts WHERE is_active = 1 " +
                    "AND moderation_status = 'ACCEPTED' AND time <= NOW() " +
                    "AND LOCATE(:query, CONCAT(text, title)) > 0",
            nativeQuery = true)
    Page<Post> findPostsByQuery(Pageable pageable, @Param("query") String query);

    @Query(value = "SELECT DATE_FORMAT(posts.time, '%Y-%m-%d') AS date, " +
            "COUNT(DATE_FORMAT(posts.time, '%Y-%m-%d')) AS count FROM posts WHERE is_active = 1 " +
            "AND moderation_status = 'ACCEPTED' AND time <= NOW() " +
            "GROUP BY DATE_FORMAT(posts.time, '%Y-%m-%d') " +
            "ORDER BY DATE_FORMAT(posts.time, '%Y-%m-%d')",
            nativeQuery = true)
    List<CalendarPostProjection> findPostsInCalendar();

    @Query(value = "SELECT * FROM posts WHERE is_active = 1 " +
            "AND moderation_status = 'ACCEPTED' AND time <= NOW() " +
            "AND DATE_FORMAT(posts.time, '%Y-%m-%d') = :date " +
            "ORDER BY posts.time DESC",
            nativeQuery = true,
            countQuery = "SELECT COUNT(*) FROM posts WHERE is_active = 1 " +
                    "AND moderation_status = 'ACCEPTED' AND time <= NOW() " +
                    "AND DATE_FORMAT(posts.time, '%Y-%m-%d') = :date")
    Page<Post> findPostsByDate(Pageable pageable, @Param("date") String date);

    @Query(value = "SELECT * FROM posts " +
            "JOIN tag2post ON posts.id = tag2post.post_id " +
            "JOIN tags ON tags.id = tag2post.tag_id WHERE is_active = 1 " +
            "AND moderation_status = 'ACCEPTED' AND posts.time <= NOW() " +
            "AND tags.name = :tag ORDER BY posts.time DESC",
            nativeQuery = true,
            countQuery = "SELECT COUNT(*) FROM posts " +
                    "JOIN tag2post ON posts.id = tag2post.post_id " +
                    "JOIN tags ON tags.id = tag2post.tag_id WHERE is_active = 1 " +
                    "AND moderation_status = 'ACCEPTED' AND posts.time <= NOW() " +
                    "AND tags.name = :tag")
    Page<Post> findPostsByTag(Pageable pageable, @Param("tag") String tag);

    @Query(value = "SELECT * FROM posts WHERE is_active = 1 " +
            "AND moderation_status = 'ACCEPTED' AND time <= NOW() AND id = :id", nativeQuery = true)
    Optional<Post> findActivePostById(@Param("id") int id);

    @Query(value = "SELECT * FROM posts JOIN users ON posts.user_id = users.id " +
            "WHERE posts.is_active = 0 AND users.email = :email",
            nativeQuery = true,
            countQuery = "SELECT COUNT(*) FROM posts JOIN users ON posts.user_id = users.id " +
                    "WHERE posts.is_active = 0 AND users.email = :email")
    Page<Post> findInactivePosts(Pageable pageable, @Param("email") String email);

    @Query(value = "SELECT * FROM posts JOIN users ON posts.user_id = users.id " +
            "WHERE posts.is_active = 1 AND moderation_status = 'NEW' " +
            "AND users.email = :email", nativeQuery = true,
            countQuery = "SELECT COUNT(*) FROM posts JOIN users ON posts.user_id = users.id " +
                    "WHERE posts.is_active = 1 AND moderation_status = 'NEW' " +
                    "AND users.email = :email")
    Page<Post> findPendingPosts(Pageable pageable, @Param("email") String email);

    @Query(value = "SELECT * FROM posts JOIN users ON posts.user_id = users.id " +
            "WHERE posts.is_active = 1 AND moderation_status = 'DECLINED' " +
            "AND users.email = :email", nativeQuery = true,
            countQuery = "SELECT COUNT(*) FROM posts JOIN users ON posts.user_id = users.id " +
                    "WHERE posts.is_active = 1 AND moderation_status = 'DECLINED' " +
                    "AND users.email = :email")
    Page<Post> findDeclinedPosts(Pageable pageable, @Param("email") String email);

    @Query(value = "SELECT * FROM posts JOIN users ON posts.user_id = users.id " +
            "WHERE posts.is_active = 1 AND moderation_status = 'ACCEPTED' " +
            "AND users.email = :email", nativeQuery = true,
            countQuery = "SELECT COUNT(*) FROM posts JOIN users ON posts.user_id = users.id " +
                    "WHERE posts.is_active = 1 AND moderation_status = 'ACCEPTED' " +
                    "AND users.email = :email")
    Page<Post> findPublishedPosts(Pageable pageable, @Param("email") String email);

    @Query(value = "SELECT * FROM posts WHERE is_active = 1 AND moderation_status = 'NEW'",
            nativeQuery = true,
            countQuery = "SELECT COUNT(*) FROM posts WHERE is_active = 1 " +
                    "AND moderation_status = 'NEW'")
    Page<Post> findNewPosts(Pageable pageable);

    @Query(value = "SELECT * FROM posts JOIN users ON posts.user_id = users.id " +
            "WHERE posts.is_active = 1 AND moderation_status = 'ACCEPTED' " +
            "AND users.email = :email", nativeQuery = true)
    List<Post> findUserActivePosts(@Param("email") String email);

    @Query(value = "SELECT * FROM posts WHERE is_active = 1 AND moderation_status = 'ACCEPTED' " +
            "AND moderator_id = :moderator_id", nativeQuery = true,
            countQuery = "SELECT COUNT(*) FROM posts WHERE is_active = 1 " +
                    "AND moderation_status = 'ACCEPTED' " +
                    "AND moderator_id = :moderator_id")
    Page<Post> findAcceptedPostsByModerator(Pageable pageable,
                                            @Param("moderator_id") int moderatorId);

    @Query(value = "SELECT * FROM posts WHERE is_active = 1 AND moderation_status = 'DECLINED' " +
            "AND moderator_id = :moderator_id", nativeQuery = true,
            countQuery = "SELECT COUNT(*) FROM posts WHERE is_active = 1 " +
                    "AND moderation_status = 'DECLINED' " +
                    "AND moderator_id = :moderator_id")
    Page<Post> findDeclinedPostsByModerator(Pageable pageable,
                                            @Param("moderator_id") int moderatorId);
}
