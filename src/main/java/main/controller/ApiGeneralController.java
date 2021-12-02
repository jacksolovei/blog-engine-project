package main.controller;

import lombok.AllArgsConstructor;
import main.api.request.CommentRequest;
import main.api.request.ModerationRequest;
import main.api.request.ProfileRequest;
import main.api.request.SettingsRequest;
import main.api.response.*;
import main.service.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.security.Principal;
import java.util.NoSuchElementException;

@RestController
@RequestMapping("/api")
@AllArgsConstructor
public class ApiGeneralController {
    private final InitResponse initResponse;
    private final SettingsService settingsService;
    private final TagService tagService;
    private final CalendarService calendarService;
    private final ImageService imageService;
    private final CommentService commentService;
    private final ApiPostService apiPostService;
    private final UserService userService;

    @GetMapping("/init")
    public InitResponse init() {
        return initResponse;
    }

    @GetMapping("/settings")
    public ResponseEntity<SettingsResponse> settings() {
        return ResponseEntity.ok(settingsService.getGlobalSettings());
    }

    @GetMapping("/tag")
    public ResponseEntity<TagListResponse> tags(@RequestParam(required = false) String query) {
        return ResponseEntity.ok(tagService.getTags(query));
    }

    @GetMapping("/calendar")
    public ResponseEntity<CalendarResponse> getCalendar(
            @RequestParam(required = false) String year) {
        return ResponseEntity.ok(calendarService.getPostsInCalendar(year));
    }

    @PreAuthorize("hasAuthority('user:write')")
    @PostMapping(value = "/image", consumes = {MediaType.MULTIPART_FORM_DATA_VALUE})
    public ResponseEntity<?> image(@RequestParam MultipartFile image) throws IOException {
        if (!imageService.checkImage(image)) {
            return ResponseEntity.badRequest().body(imageService.getErrorResponse(image));
        }
        return ResponseEntity.ok(imageService.uploadImage(image));
    }

    @PreAuthorize("hasAuthority('user:write')")
    @PostMapping("/comment")
    public ResponseEntity<?> postComment(@RequestBody CommentRequest commentRequest,
                                         Principal principal) {
        if (!commentService.checkComment(commentRequest)) {
            return ResponseEntity.badRequest()
                    .body(commentService.getErrorResponse(commentRequest));
        }
        return ResponseEntity.ok(commentService.saveComment(commentRequest, principal));
    }

    @PreAuthorize("hasAuthority('user:moderate')")
    @PostMapping("/moderation")
    public ResponseEntity<ResultResponse> moderatePost(
            @RequestBody ModerationRequest moderationRequest,
            Principal principal) {
        return ResponseEntity.ok(apiPostService.moderate(moderationRequest, principal));
    }

    @PreAuthorize("hasAuthority('user:write')")
    @PostMapping(value = "/profile/my", consumes = {MediaType.MULTIPART_FORM_DATA_VALUE})
    public ResponseEntity<RegResponse> editProfile(
            @RequestParam(value = "photo") MultipartFile photo,
            @RequestParam(value = "name") String name,
            @RequestParam(value = "email") String email,
            @RequestParam(value = "password", required = false) String password,
            @RequestParam(value = "removePhoto") int removePhoto,
            Principal principal) throws IOException {
        return ResponseEntity
                .ok(userService.editImage(principal, photo, name, email, password));
    }

    @PreAuthorize("hasAuthority('user:write')")
    @PostMapping("/profile/my")
    public ResponseEntity<RegResponse> editProfile(@RequestBody ProfileRequest profileRequest,
                                                   Principal principal) {
        return ResponseEntity.ok(userService.editUser(principal, profileRequest));
    }

    @PreAuthorize("hasAuthority('user:write')")
    @GetMapping("/statistics/my")
    public ResponseEntity<StatResponse> getUserStatistics(Principal principal) {
        return ResponseEntity.ok(userService.getUserStatistics(principal));
    }

    @GetMapping("/statistics/all")
    public ResponseEntity<StatResponse> getAllStatistics(Principal principal) {
        if (!userService.isStatisticsShown(principal)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(null);
        }
        return ResponseEntity.ok(userService.getStatistics());
    }

    @PreAuthorize("hasAuthority('user:moderate')")
    @PutMapping("/settings")
    public ResponseEntity setSettings(@RequestBody SettingsRequest settingsRequest) {
        settingsService.setSettings(settingsRequest);
        return ResponseEntity.ok().body(null);
    }

    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<String> handleNoSuchElementException(NoSuchElementException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ex.getMessage());
    }
}
