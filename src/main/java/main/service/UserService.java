package main.service;

import main.api.request.ProfileRequest;
import main.api.response.RegResponse;
import main.api.response.SettingsResponse;
import main.api.response.StatResponse;
import main.dto.PostDto;
import main.model.User;
import main.repository.PostRepository;
import main.repository.UserRepository;
import org.apache.commons.io.FilenameUtils;
import org.apache.logging.log4j.LogManager;
import org.imgscalr.Scalr;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.Principal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

@Service
public class UserService {
    public static final long PHOTO_MAX_SIZE = 5 * 1024 * 1024;
    public static final int PASSWORD_LENGTH = 6;
    public static final int MAX_LENGTH = 255;
    public static final int PROFILE_IMG_SIZE = 36;
    public static final PasswordEncoder BCRYPT = new BCryptPasswordEncoder(12);
    private static Logger logger;

    @Value("${upload.path}")
    private String uploadPath;

    private final UserRepository userRepository;
    private final PostRepository postRepository;
    private final ApiPostService apiPostService;
    private final MapperService mapperService;
    private final SettingsService settingsService;

    public UserService(UserRepository userRepository, PostRepository postRepository,
                       ApiPostService apiPostService, MapperService mapperService,
                       SettingsService settingsService) {
        this.userRepository = userRepository;
        this.postRepository = postRepository;
        this.apiPostService = apiPostService;
        this.mapperService = mapperService;
        this.settingsService = settingsService;
    }

    public RegResponse editImage(
            Principal principal,
            MultipartFile photo, String name, String email,
            String password) throws IOException {
        logger = LoggerFactory.getLogger(UserService.class);
        RegResponse regResponse = new RegResponse();
        User user = apiPostService.getAuthorizedUser(principal);
        Map<String, String> errors = new HashMap<>();
        if (!email.equals(user.getEmail()) && userRepository.findByEmail(email).isPresent()) {
            errors.put("email", "Этот e-mail уже зарегистрирован");
        }
        if (photo.getSize() > PHOTO_MAX_SIZE) {
            errors.put("photo",
                    "Фото слишком большое, нужно не более 5 Мб");
        }
        if (name.length() > MAX_LENGTH || !name.matches("[А-Яа-яA-Za-z]+([А-Яа-яA-Za-z\\s]+)?")) {
            errors.put("name", "Имя указано неверно");
        }
        if (password != null && password.length() < PASSWORD_LENGTH) {
            errors.put("password", "Пароль короче 6-ти символов");
        }
        if (errors.isEmpty()) {
            try {
                BufferedImage bufferedImage = ImageIO.read(photo.getInputStream());
                logger.info("Image " + photo.getOriginalFilename() + " is read");
                BufferedImage resultImage = Scalr.resize(bufferedImage,
                        Scalr.Method.QUALITY,
                        PROFILE_IMG_SIZE,
                        PROFILE_IMG_SIZE);
                String toFile = uploadPath + "/" + user.getId() + "/" + photo.getOriginalFilename();
                logger.info("Path to upload: " + toFile);
                Path path = Paths.get(toFile);
                if (!path.toFile().exists()) {
                    Files.createDirectories(path.getParent());
                    Files.createFile(path);
                    logger.info("New file is created: " + path);
                    String extension = FilenameUtils.getExtension(photo.getOriginalFilename());
                    ImageIO.write(resultImage, extension, path.toFile());
                    logger.info("Image is written to file " + path);
                }
                user.setPhoto(toFile.substring(toFile.lastIndexOf("/upload")));
                logger.info("Image " + toFile.substring(toFile.lastIndexOf("/upload")) + " is set to user " + user.getId());
            } catch (Exception e) {
                e.printStackTrace();
                logger.error(e.getMessage());
            }
            if (!user.getName().equals(name)) {
                user.setName(name);
            }
            if (!user.getEmail().equals(email)) {
                user.setEmail(email);
            }
            if (password != null) {
                user.setPassword(BCRYPT.encode(password));
            }
            userRepository.save(user);
            regResponse.setResult(true);
        } else {
            regResponse.setResult(false);
            regResponse.setErrors(errors);
        }
        return regResponse;
    }

    public RegResponse editUser(Principal principal, ProfileRequest profileRequest) {
        RegResponse regResponse = new RegResponse();
        Map<String, String> errors = new HashMap<>();
        User user = apiPostService.getAuthorizedUser(principal);
        String email = profileRequest.getEmail();
        if (!email.equals(user.getEmail()) && userRepository.findByEmail(email).isPresent()) {
            errors.put("email", "Этот e-mail уже зарегистрирован");
        }
        String name = profileRequest.getName();
        if (name.length() > MAX_LENGTH || !name.matches("[А-Яа-яA-Za-z]+([А-Яа-яA-Za-z\\s]+)?")) {
            errors.put("name", "Имя указано неверно");
        }
        String password = profileRequest.getPassword();
        if (password != null && password.length() < PASSWORD_LENGTH) {
            errors.put("password", "Пароль короче 6-ти символов");
        }
        if (errors.isEmpty()) {
            if (!user.getName().equals(name)) {
                user.setName(name);
            }
            if (!user.getEmail().equals(email)) {
                user.setEmail(email);
            }
            if (password != null) {
                user.setPassword(BCRYPT.encode(password));
            }
            if (profileRequest.getRemovePhoto() == 1) {
                user.setPhoto(null);
            }
            userRepository.save(user);
            regResponse.setResult(true);
        } else {
            regResponse.setResult(false);
            regResponse.setErrors(errors);
        }
        return regResponse;
    }

    public StatResponse getUserStatistics(Principal principal) {
        List<PostDto> posts = postRepository.findUserActivePosts(principal.getName())
                .stream()
                .map(mapperService::convertPostToDto)
                .collect(Collectors.toList());
        return getStatResponse(posts);
    }

    public StatResponse getStatistics() {
        List<PostDto> posts = postRepository.findActivePosts()
                .stream()
                .map(mapperService::convertPostToDto)
                .collect(Collectors.toList());
        return getStatResponse(posts);
    }

    public StatResponse getStatResponse(List<PostDto> posts) {
        StatResponse statResponse = new StatResponse();
        int likesCount = posts.stream().map(PostDto::getLikeCount).reduce(0, Integer::sum);
        int disLikesCount = posts.stream().map(PostDto::getDislikeCount).reduce(0, Integer::sum);
        int viewsCount = posts.stream().map(PostDto::getViewCount).reduce(0, Integer::sum);
        statResponse.setPostsCount(posts.size());
        statResponse.setLikesCount(likesCount);
        statResponse.setDislikesCount(disLikesCount);
        statResponse.setViewsCount(viewsCount);
        if (posts.isEmpty()) {
            statResponse.setFirstPublication(0);
        } else {
            long firstPublication = posts.stream()
                    .map(PostDto::getTimestamp)
                    .min(Long::compare)
                    .orElseThrow(NoSuchElementException::new);
            statResponse.setFirstPublication(firstPublication);
        }
        return statResponse;
    }

    public boolean isStatisticsShown(Principal principal) {
        SettingsResponse settings = settingsService.getGlobalSettings();
        if (!settings.isStatisticsIsPublic()) {
            if (principal != null) {
                return apiPostService.getAuthorizedUser(principal).getIsModerator() == 1;
            }
            return false;
        }
        return true;
    }
}
