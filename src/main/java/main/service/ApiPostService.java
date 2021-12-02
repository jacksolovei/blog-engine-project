package main.service;

import lombok.AllArgsConstructor;
import main.api.request.ModerationRequest;
import main.api.request.PostIdRequest;
import main.api.request.PostRequest;
import main.api.response.*;
import main.dto.CommentDto;
import main.dto.PostDto;
import main.dto.UserDto;
import main.model.*;
import main.repository.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.security.Principal;
import java.util.*;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
public class ApiPostService {
    public static final String RECENT_MODE = "recent";
    public static final int MIN_TITLE_LENGTH = 3;
    public static final int MIN_TEXT_LENGTH = 50;

    private final PostRepository postRepository;
    private final UserRepository userRepository;
    private final SettingsRepository settingsRepository;
    private final PostVoteRepository postVoteRepository;
    private final TagRepository tagRepository;
    private final MapperService mapperService;
    private final AuthCheckService authCheckService;

    public ApiPostListResponse getPosts(int offset, int limit, String mode) {
        ApiPostListResponse apiList = new ApiPostListResponse();
        List<Post> posts = new ArrayList<>();
        Pageable pageable = PageRequest.of(offset / limit, limit);
        Page<Post> page;
        switch (mode) {
            case "popular":
                page = postRepository.findPopularPosts(pageable);
                break;
            case "early":
                page = postRepository.findEarlyPosts(pageable);
                break;
            case "best":
                page = postRepository.findBestPosts(pageable);
                break;
            default:
                page = postRepository.findRecentPosts(pageable);
        }
        posts.addAll(page.getContent());
        apiList.setCount(page.getTotalElements());
        List<PostDto> postDtoList = posts.stream().map(mapperService::convertPostToDto)
                .collect(Collectors.toList());
        apiList.setPosts(postDtoList);
        return apiList;
    }

    public ApiPostListResponse getPostsByQuery(int offset, int limit, String query) {
        if (query == null || query.matches("\\s+")) {
            return getPosts(offset, limit, RECENT_MODE);
        }
        ApiPostListResponse apiList = new ApiPostListResponse();
        Pageable pageable = PageRequest.of(offset / limit, limit);
        Page<Post> page = postRepository.findPostsByQuery(pageable, query);
        apiList.setPosts(page.getContent().stream().map(mapperService::convertPostToDto)
                .collect(Collectors.toList()));
        apiList.setCount(page.getTotalElements());
        return apiList;
    }

    public ApiPostListResponse getPostsByDate(int offset, int limit, String date) {
        ApiPostListResponse apiList = new ApiPostListResponse();
        Pageable pageable = PageRequest.of(offset / limit, limit);
        Page<Post> page = postRepository.findPostsByDate(pageable, date);
        apiList.setPosts(page.getContent().stream().map(mapperService::convertPostToDto)
                .collect(Collectors.toList()));
        apiList.setCount(page.getTotalElements());
        return apiList;
    }

    public ApiPostListResponse getPostsByTag(int offset, int limit, String tag) {
        ApiPostListResponse apiList = new ApiPostListResponse();
        Pageable pageable = PageRequest.of(offset / limit, limit);
        Page<Post> page = postRepository.findPostsByTag(pageable, tag);
        apiList.setPosts(page.getContent().stream().map(mapperService::convertPostToDto)
                .collect(Collectors.toList()));
        apiList.setCount(page.getTotalElements());
        return apiList;
    }

    public Optional<Post> getOptionalPostById(int id, Principal principal) {
        if (principal != null) {
            return postRepository.findById(id);
        }
        return postRepository.findActivePostById(id);
    }

    public PostByIdResponse getPostResponseById(Post post, Principal principal) {
        AuthCheckResponse authCheckResponse = authCheckService.getAuthCheck(principal);
        int view;
        if (authCheckResponse.isResult()) {
            UserDto user = authCheckResponse.getUser();
            if (user.isModeration() || user.getId() == post.getUser().getId()) {
                view = post.getViewCount();
            } else {
                view = post.getViewCount() + 1;
                post.setViewCount(view);
                postRepository.save(post);
            }
        } else {
            view = post.getViewCount() + 1;
            post.setViewCount(view);
            postRepository.save(post);
        }
        List<CommentDto> comments = post.getPostComments().stream()
                .map(mapperService::convertCommentToDto)
                .collect(Collectors.toList());
        List<String> tags = post.getTags().stream().map(Tag::getName)
                .collect(Collectors.toList());
        PostDto postDto = mapperService.convertPostToDto(post);

        return new PostByIdResponse(postDto.getId(), postDto.getTimestamp(),
                postDto.isActive(), postDto.getUser(), postDto.getTitle(), postDto.getText(),
                postDto.getLikeCount(), postDto.getDislikeCount(), view,
                comments, tags);
    }

    public ApiPostListResponse getPostsByStatus(
            int offset, int limit, String status, Principal principal) {
        String email = principal.getName();
        ApiPostListResponse apiPostListResponse = new ApiPostListResponse();
        List<Post> posts = new ArrayList<>();
        Pageable pageable = PageRequest.of(offset / limit, limit);
        Page<Post> page;
        switch (status) {
            case "inactive":
                page = postRepository.findInactivePosts(pageable, email);
                break;
            case "pending":
                page = postRepository.findPendingPosts(pageable, email);
                break;
            case "declined":
                page = postRepository.findDeclinedPosts(pageable, email);
                break;
            default:
                page = postRepository.findPublishedPosts(pageable, email);
        }
        posts.addAll(page.getContent());
        apiPostListResponse.setCount(page.getTotalElements());
        List<PostDto> postDtoList = posts.stream().map(mapperService::convertPostToDto)
                .collect(Collectors.toList());
        apiPostListResponse.setPosts(postDtoList);
        return apiPostListResponse;
    }

    public User getAuthorizedUser(Principal principal) {
        String email = principal.getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new NoSuchElementException("user " + email + " not found"));
    }

    public PostForModerationListResponse getPostsForModeration(
            int offset, int limit, String status, Principal principal) {
        PostForModerationListResponse moderationListResponse = new PostForModerationListResponse();
        int moderatorId = getAuthorizedUser(principal).getId();
        List<Post> posts = new ArrayList<>();
        Pageable pageable = PageRequest.of(offset / limit, limit);
        Page<Post> page;
        switch (status) {
            case "accepted":
                page = postRepository.findAcceptedPostsByModerator(pageable, moderatorId);
                break;
            case "declined":
                page = postRepository.findDeclinedPostsByModerator(pageable, moderatorId);
                break;
            default:
                page = postRepository.findNewPosts(pageable);
        }
        posts.addAll(page.getContent());
        moderationListResponse.setCount(page.getTotalElements());
        List<PostForModerationResponse> moderatorPosts = posts.stream()
                .map(mapperService::convertPostToDto)
                .map(p -> new PostForModerationResponse(p.getId(), p.getTimestamp(),
                        p.getTitle(), p.getAnnounce(), p.getLikeCount(),
                        p.getDislikeCount(), p.getCommentCount(), p.getViewCount(), p.getUser()))
                .collect(Collectors.toList());
        moderationListResponse.setPosts(moderatorPosts);
        return moderationListResponse;
    }

    public HashMap<String, String> getErrors(PostRequest postRequest) {
        HashMap<String, String> errors = new HashMap<>();
        if (postRequest.getTitle().length() < MIN_TITLE_LENGTH) {
            errors.put("title", "Заголовок не установлен");
        }
        if (postRequest.getText().length() < MIN_TEXT_LENGTH) {
            errors.put("text", "Текст публикации слишком короткий");
        }
        return errors;
    }

    public RegResponse savePost(PostRequest postRequest, Principal principal) {
        RegResponse regResponse = new RegResponse();
        Map<String, String> errors = getErrors(postRequest);
        User user = getAuthorizedUser(principal);
        Date postDate = new Date(postRequest.getTimestamp() * 1000);
        if (errors.isEmpty()) {
            regResponse.setResult(true);
            Post post = new Post();
            post.setIsActive(postRequest.isActive() ? (byte) 1 : 0);
            boolean isModeration = settingsRepository
                    .findSettingValue("POST_PREMODERATION").equals("YES");
            if (!isModeration && post.getIsActive() == 1) {
                post.setStatus(ModerationStatus.ACCEPTED);
            } else {
                post.setStatus(ModerationStatus.NEW);
            }
            post.setTime(postDate.compareTo(new Date()) <= 0 ? new Date() : postDate);
            post.setTitle(postRequest.getTitle());
            post.setText(postRequest.getText());
            post.setViewCount(0);
            post.setUser(user);
            List<Tag> tags = postRequest.getTags().stream()
                    .map(t -> tagRepository.findTagByName(t)
                            .orElseThrow(NoSuchElementException::new))
                    .collect(Collectors.toList());
            post.setTags(tags);
            postRepository.save(post);
        } else {
            regResponse.setResult(false);
            regResponse.setErrors(errors);
        }
        return regResponse;
    }

    public RegResponse editPost(Post post, PostRequest postRequest, Principal principal) {
        RegResponse regResponse = new RegResponse();
        Map<String, String> errors = getErrors(postRequest);
        User user = getAuthorizedUser(principal);
        Date postDate = new Date(postRequest.getTimestamp() * 1000);
        if (errors.isEmpty()) {
            regResponse.setResult(true);
            post.setIsActive(postRequest.isActive() ? (byte) 1 : 0);
            boolean isModeration = settingsRepository
                    .findSettingValue("POST_PREMODERATION").equals("YES");
            if (user.getIsModerator() == 0 && isModeration) {
                post.setStatus(ModerationStatus.NEW);
            }
            post.setTime(postDate.compareTo(new Date()) <= 0 ? new Date() : postDate);
            post.setTitle(postRequest.getTitle());
            post.setText(postRequest.getText());
            List<Tag> tags = postRequest.getTags().stream()
                    .map(t -> tagRepository.findTagByName(t)
                            .orElseThrow(NoSuchElementException::new))
                    .collect(Collectors.toList());
            post.setTags(tags);
            postRepository.save(post);
        } else {
            regResponse.setResult(false);
            regResponse.setErrors(errors);
        }
        return regResponse;
    }

    public ResultResponse moderate(ModerationRequest moderationRequest,
                                   Principal principal) {
        ResultResponse resultResponse = new ResultResponse();
        User user = getAuthorizedUser(principal);
        int id = moderationRequest.getPostId();
        if (user.getIsModerator() == 1 && postRepository.findById(id).isPresent()) {
            Post post = postRepository.findById(id).get();
            String status = moderationRequest.getDecision();
            switch (status) {
                case "accept":
                    post.setStatus(ModerationStatus.ACCEPTED);
                    post.setModeratorId(user.getId());
                    postRepository.save(post);
                    resultResponse.setResult(true);
                    break;
                case "decline":
                    post.setStatus(ModerationStatus.DECLINED);
                    post.setModeratorId(user.getId());
                    postRepository.save(post);
                    resultResponse.setResult(true);
                    break;
                default:
                    resultResponse.setResult(false);
            }
        } else {
            resultResponse.setResult(false);
        }
        return resultResponse;
    }

    public ResultResponse getVoteResponse(
            PostIdRequest postIdRequest, Principal principal, byte voteValue) {
        ResultResponse result = new ResultResponse();
        int postId = postIdRequest.getPostId();
        Post post = postRepository.findActivePostById(postId)
                .orElseThrow(() -> new NoSuchElementException("Post " + postId + " not found"));
        User user = getAuthorizedUser(principal);
        Optional<PostVote> optionalVote = postVoteRepository.findAll().stream()
                .filter(v -> v.getPost().equals(post) && v.getUser().equals(user))
                .findFirst();
        if (optionalVote.isPresent()) {
            if (optionalVote.get().getValue() != voteValue) {
                PostVote vote = optionalVote.get();
                vote.setValue(voteValue);
                postVoteRepository.save(vote);
                result.setResult(true);
            } else {
                result.setResult(false);
            }
        } else {
            PostVote postVote = new PostVote();
            postVote.setPost(post);
            postVote.setUser(user);
            postVote.setTime(new Date());
            postVote.setValue(voteValue);
            postVoteRepository.save(postVote);
            result.setResult(true);
        }
        return result;
    }

    public ResultResponse likePost(PostIdRequest postIdRequest, Principal principal) {
        return getVoteResponse(postIdRequest, principal, (byte) 1);
    }

    public ResultResponse dislikePost(PostIdRequest postIdRequest, Principal principal) {
        return getVoteResponse(postIdRequest, principal, (byte) -1);
    }
}
