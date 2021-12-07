package main.service;

import main.api.response.RegResponse;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;
import org.imgscalr.Scalr;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

@Service
public class ImageService {
    private static Logger logger;

    public static final long IMAGE_MAX_SIZE = 1024 * 1024;
    public static final int NEW_WIDTH = 500;

    @Value("${upload.path}")
    private String uploadPath;

    public Map<String, String> getErrors(MultipartFile image) {
        Map<String, String> errors = new HashMap<>();
        if (image.getSize() > IMAGE_MAX_SIZE) {
            errors.put("image",
                    "Размер файла превышает допустимый размер");
        }
        String extension = FilenameUtils.getExtension(image.getOriginalFilename());
        if (!extension.equals("jpg") && !extension.equals("png")) {
            errors.put("format", "Неверный формат файла");
        }
        return errors;
    }

    public boolean checkImage(MultipartFile image) {
        return getErrors(image).isEmpty();
    }

    public RegResponse getErrorResponse(MultipartFile image) {
        RegResponse regResponse = new RegResponse();
        Map<String, String> errors = getErrors(image);
        regResponse.setResult(false);
        regResponse.setErrors(errors);
        return regResponse;
    }

    public String uploadImage(MultipartFile image) throws IOException {
        logger = LoggerFactory.getLogger(ImageService.class);
        String random = RandomStringUtils.randomAlphabetic(6);
        StringBuilder pathToImage = new StringBuilder();
        pathToImage
                .append(uploadPath)
                .append("/")
                .append(random, 0, 2)
                .append("/")
                .append(random, 2, 4)
                .append("/")
                .append(random.substring(4))
                .append("/")
                .append(image.getOriginalFilename());
        Path path = Paths.get(pathToImage.toString());
        try {
            Files.createDirectories(path.getParent());
            Files.createFile(path);
            logger.info("Path to file: " + path);
            BufferedImage bufferedImage = ImageIO.read(image.getInputStream());
            int height = bufferedImage.getHeight();
            int width = bufferedImage.getWidth();
            logger.info("Image " + image.getOriginalFilename() + " (size " + height + " * " + width + ") is read");
            String extension = FilenameUtils.getExtension(image.getOriginalFilename());
            if (width <= NEW_WIDTH) {
                ImageIO.write(bufferedImage, extension, path.toFile());
                logger.info("Image with size " + width + " * " + height + " is written with name " + image.getOriginalFilename());
            } else {
                int newHeight = (int) Math.round(height / (double) (width / NEW_WIDTH));
                BufferedImage resultImage = Scalr.resize(
                        bufferedImage, Scalr.Method.QUALITY, NEW_WIDTH, newHeight);
                ImageIO.write(resultImage, extension, path.toFile());
                logger.info("Image with size " + NEW_WIDTH  + " * " + newHeight + " is written to file  " + path);
            }
        } catch (Exception e) {
            e.printStackTrace();
            logger.error(e.getMessage());
        }
        String load = pathToImage.toString();
        return load.substring(load.lastIndexOf("/upload"));
    }
}
