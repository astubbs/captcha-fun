package io.stubbs.selenium;

import com.google.common.base.Stopwatch;
import io.github.bonigarcia.wdm.WebDriverManager;
import io.stubbs.captcha.FunctionUtils;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.imgscalr.Scalr;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.RasterFormatException;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
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
        //File screenshot = driver.getScreenshotAs(FILE);
        // String screenshotAs = driver.getScreenshotAs(BASE64);
        Stopwatch w = Stopwatch.createStarted();
        byte[] screenshot = time("Screenshot whole page...", () -> driver.getScreenshotAs(BYTES));

        BufferedImage fullImg = time("Read PNG screenshot from byte array", () -> {
            try {
                return ImageIO.read(new ByteArrayInputStream(screenshot));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });

        // log.info("Read screenshot off disk...");
        // BufferedImage fullImg = ImageIO.read(screenshot);

        int scale = 2;

        // Get the location of element on the page
        org.openqa.selenium.Point loc = ele.getLocation();
        org.openqa.selenium.Point point = new org.openqa.selenium.Point(loc.x * scale, loc.y * scale);

        // Get width and height of the element
        int eleWidth = ele.getSize().getWidth() * scale;
        int eleHeight = ele.getSize().getHeight() * scale;

        // Crop the entire page screenshot to get only element screenshot
        BufferedImage eleScreenshot;
        try {
            eleScreenshot = time("Resize", () -> fullImg.getSubimage(point.getX(), point.getY(),
                    eleWidth, eleHeight));
        } catch (RasterFormatException e) {
            log.error("Some bizarre image manipulation mistake...? Saving original to disk for manual inspection.");
            saveImageToDisk("original-source-capture", fullImg, "png");
            throw e;
        }

        int scaledWidth = eleScreenshot.getWidth() / scale;
        int scaledHeight = eleScreenshot.getHeight() / scale;
        BufferedImage scaledImage = new BufferedImage(
                scaledWidth,
                scaledHeight,
                BufferedImage.TYPE_INT_RGB);

//        BufferedImage scalrImagetest = time("Sclar test", () -> Scalr.resize(eleScreenshot, scaledWidth));

        Graphics2D graphics = time("Scaling image", () -> scaledImage.createGraphics());

        time("Drawing", () -> graphics.drawImage(eleScreenshot, 0, 0, scaledWidth, scaledHeight, Color.WHITE, null));


        // Copy the element screenshot to disk
        // File tmpFile = File.createTempFile("screenshot", ".png");
        // tmpFile.deleteOnExit();
        // File screenshotLocation = new File("C:\\images\\GoogleLogo_screenshot.png");
        File screenshotLocationJpg = File.createTempFile("screenshot-part-jpg", ".jpg");
        if (deleteOnExit)
            screenshotLocationJpg.deleteOnExit();

        // File screenshotLocation = File.createTempFile("screenshot-part", ".png");

        log.debug("Saving scaled image to disk...");
        boolean jpg1 = ImageIO.write(scaledImage, "jpg", screenshotLocationJpg);
        if (!jpg1) throw new RuntimeException("Failed to find writer...");

        // boolean jpg = ImageIO.write(eleScreenshot, "png", screenshotLocation);
        // assertThat(jpg).isTrue();

        ByteArrayOutputStream scaledOutputBuffer = new ByteArrayOutputStream();
        // BufferedOutputStream output = new BufferedOutputStream(scaledOutputBuffer);
        log.debug("Save ...");
        boolean jpg = ImageIO.write(scaledImage, "jpg", scaledOutputBuffer);
        if (!jpg) throw new RuntimeException("Failed to find writer...");

        scaledOutputBuffer.close();

        log.debug("Base64 encoding...");
        String base64EncodedImage = Base64.getEncoder().encodeToString(scaledOutputBuffer.toByteArray());
        // FileUtils.copyFile(screenshot, screenshotLocation);
        return base64EncodedImage;
    }

    @SneakyThrows
    private void saveImageToDisk(String name, BufferedImage fullImg, String format) {
        File fileLoc = File.createTempFile(name, "." + format);
        if (deleteOnExit)
            fileLoc.deleteOnExit();
        log.info("Saving {} to disk...", fullImg);
        boolean success = ImageIO.write(fullImg, format, fileLoc);
        if (!success)
            throw new RuntimeException("Failed to find writer...?");

    }

}
