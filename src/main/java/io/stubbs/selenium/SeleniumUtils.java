package io.stubbs.selenium;

import com.google.common.base.Stopwatch;
import io.github.bonigarcia.wdm.WebDriverManager;
import io.stubbs.captcha.FunctionUtils;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.util.Base64;

import static io.stubbs.captcha.FunctionUtils.logStopwatch;
import static io.stubbs.captcha.FunctionUtils.time;
import static org.openqa.selenium.OutputType.BYTES;

@Slf4j
@RequiredArgsConstructor
public class SeleniumUtils {

    final private ChromeDriver driver;

    private boolean deleteOnExit = false;

    @SneakyThrows
    static public ChromeDriver getChromeDriver() {
        WebDriverManager.chromedriver().setup();
        ChromeOptions chromeOptions = new ChromeOptions();

//        File tempFile = File.createTempFile("touch", "txt", null);
//        String absolutePath = tempFile.getPath() + "/ocado/touch";
//        new File(absolutePath).mkdirs();

//        chromeOptions.addArguments("user-data-dir=" + "/private/var/folders/wl/3xwk5p0x22q76b_yrfkpb1pw0000gp/T/ocado");

        ChromeDriver driver = new ChromeDriver(chromeOptions);

        return driver;
    }

    @SneakyThrows
    public String shotElement(WebElement ele) {
        log.info("Capturing rendered captcha image...");
        // Get entire page screenshot
        // TODO screenshot to memory buffer instead of disk
        log.info("Screenshot whole page...");
        //File screenshot = driver.getScreenshotAs(FILE);
        // String screenshotAs = driver.getScreenshotAs(BASE64);
        Stopwatch w = Stopwatch.createStarted();
        byte[] screenshot = driver.getScreenshotAs(BYTES);
        logStopwatch(log, w);
        BufferedImage fullImg = ImageIO.read(new ByteArrayInputStream(screenshot));
        log.info(w.elapsed().toString());
//        log.info("Read screenshot off disk...");
        // BufferedImage fullImg = ImageIO.read(screenshot);

        int scale = 2;

        // Get the location of element on the page
        org.openqa.selenium.Point loc = ele.getLocation();
        org.openqa.selenium.Point point = new org.openqa.selenium.Point(loc.x * scale, loc.y * scale);

        // Get width and height of the element
        int eleWidth = ele.getSize().getWidth() * scale;
        int eleHeight = ele.getSize().getHeight() * scale;

        // Crop the entire page screenshot to get only element screenshot
        BufferedImage eleScreenshot = fullImg.getSubimage(point.getX(), point.getY(),
                eleWidth, eleHeight);

        int scaledWidth = eleScreenshot.getWidth() / scale;
        int scaledHeight = eleScreenshot.getHeight() / scale;
        BufferedImage scaledImage = new BufferedImage(
                scaledWidth,
                scaledHeight,
                BufferedImage.TYPE_INT_RGB);

        Graphics2D graphics = time("Scaling image", () -> scaledImage.createGraphics());

        time("Drawing", () -> graphics.drawImage(eleScreenshot, 0, 0, scaledWidth, scaledHeight, Color.WHITE, null));


        // Copy the element screenshot to disk
        // File tmpFile = File.createTempFile("screenshot", ".png");
        // tmpFile.deleteOnExit();
        // File screenshotLocation = new File("C:\\images\\GoogleLogo_screenshot.png");
        File screenshotLocationjpg = File.createTempFile("screenshot-part-jpg", ".jpg");
        if (deleteOnExit)
            screenshotLocationjpg.deleteOnExit();

        // File screenshotLocation = File.createTempFile("screenshot-part", ".png");

        log.info("Saving scaled image to disk...");
        boolean jpg1 = ImageIO.write(scaledImage, "jpg", screenshotLocationjpg);
        if (!jpg1) throw new RuntimeException("Failed to find writer...");

        // boolean jpg = ImageIO.write(eleScreenshot, "png", screenshotLocation);
        // assertThat(jpg).isTrue();

        ByteArrayOutputStream scaledOutputBuffer = new ByteArrayOutputStream();
        // BufferedOutputStream output = new BufferedOutputStream(scaledOutputBuffer);
        log.info("Save ...");
        boolean jpg = ImageIO.write(scaledImage, "jpg", scaledOutputBuffer);
        if (!jpg) throw new RuntimeException("Failed to find writer...");

        scaledOutputBuffer.close();

        log.info("Base64 encoding...");
        String base64EncodedImage = Base64.getEncoder().encodeToString(scaledOutputBuffer.toByteArray());
        // FileUtils.copyFile(screenshot, screenshotLocation);
        return base64EncodedImage;
    }

}
