package main.service;

import lombok.AllArgsConstructor;
import main.api.request.CommentRequest;
import main.api.response.CommentResponse;
import main.api.response.RegResponse;
import main.model.Post;
import main.model.PostComment;
import main.model.User;
import main.repository.CommentRepository;
import main.repository.PostRepository;
import org.springframework.stereotype.Service;

import java.security.Principal;
import java.util.*;

@Service
@AllArgsConstructor
public class CommentService {
    public static final int MIN_TEXT_LENGTH = 10;

    private final CommentRepository commentRepository;
    private final PostRepository postRepository;
    private final ApiPostService apiPostService;

    private Map<String, String> getErrors(CommentRequest commentRequest) {
        Map<String, String> errors = new HashMap<>();
        Optional<Post> postOptional = postRepository.findById(commentRequest.getPostId());
        if (postOptional.isEmpty()) {
            errors.put("post", "Пост не найден");
        }
        if (commentRequest.getParentId() != null) {
            int parentId = Integer.parseInt(commentRequest.getParentId());
            Optional<PostComment> parentComment = commentRepository.findById(parentId);
            if (parentComment.isEmpty()) {
                errors.put("comment", "Комментарий не найден");
            }
        }
        String text = commentRequest.getText();
        if (text.length() < MIN_TEXT_LENGTH) {
            errors.put("text", "Текст публикации слишком короткий");
        }
        return errors;
    }

    public boolean checkComment(CommentRequest commentRequest) {
        Map<String, String> errors = getErrors(commentRequest);
        return errors.isEmpty();
    }

    public RegResponse getErrorResponse(CommentRequest commentRequest) {
        RegResponse errorResponse = new RegResponse();
        Map<String, String> errors = getErrors(commentRequest);
        errorResponse.setResult(false);
        errorResponse.setErrors(errors);
        return errorResponse;
    }

    public CommentResponse saveComment(CommentRequest commentRequest, Principal principal) {
        CommentResponse commentResponse = new CommentResponse();
        User user = apiPostService.getAuthorizedUser(principal);
        int postId = commentRequest.getPostId();
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new NoSuchElementException("Post " + postId + " not found"));
        PostComment comment = new PostComment();
        if (commentRequest.getParentId() != null) {
            int parentId = Integer.parseInt(commentRequest.getParentId());
            comment.setParentId(parentId);
        }
        comment.setTime(new Date());
        comment.setText(commentRequest.getText());
        comment.setUser(user);
        comment.setPost(post);
        commentRepository.save(comment);
        commentResponse.setId(comment.getId());
        return commentResponse;
    }
}
