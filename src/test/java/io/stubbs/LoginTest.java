package io.stubbs;

import com.twocaptcha.api.ProxyType;
import com.twocaptcha.api.TwoCaptchaService;
import com.twocaptcha.api.TwoCaptchaService.ResponseData.Point;
import io.github.bonigarcia.wdm.WebDriverManager;
import lombok.Data;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.NotImplementedException;
import org.assertj.core.util.Lists;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openqa.selenium.*;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.interactions.Actions;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.time.Duration;
import java.util.Base64;
import java.util.List;
import java.util.Optional;

import static io.stubbs.LoginTest.CaptchaType.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.openqa.selenium.OutputType.FILE;

@Slf4j
public class LoginTest {
    Duration MAX_SPEED = Duration.ofSeconds(1);
    public static final String G_RECAPTCHA_RESPONSE_ID = "g-recaptcha-response";
    private ChromeDriver driver;
//    private Map<String, Object> vars;
//    JavascriptExecutor js;

    @Before
    public void setUp() {
//        driver = new FirefoxDriver();
        WebDriverManager.chromedriver().setup();
        ChromeOptions chromeOptions = new ChromeOptions();
//        chromeOptions.setHeadless(true);

        driver = new ChromeDriver(chromeOptions);

//        js = (JavascriptExecutor) driver;
//        vars = new HashMap<String, Object>();
    }

    @After
    public void tearDown() {
        driver.quit();
    }

    @Test
    public void login() {
        driver.get("https://accounts.ocado.com/auth-service/sso/login");

        submitCreds();

        // TODO save session / cookie data to avoid having to log in again before needed
        captchaMaybe();

        log.info("Dumping cookies:");
        for (Cookie cookie : driver.manage().getCookies()) {
            log.info("Cookie: {}", cookie);
        }

        log.info("Log in complete...");

        checkForQueueRedirect();

        checkSchedules();
    }

    private void checkForQueueRedirect() {
//        driver.findElement(By.linkText("log in.")).click();
        driver.switchTo().defaultContent();
        driver.findElement(By.cssSelector("html")).click();
    }

    private void checkSchedules() {
        //https://www.ocado.com/webshop/getAddressesForDelivery.do
    }

    private void submitCreds() {
        String currentUrl = driver.getCurrentUrl();
        if (currentUrl.contains("https://q.ocado.com/")) {
            // redirected into queue
            waitForQueue();
        }
        String title = driver.getTitle();
        String pageSource = driver.getPageSource();
//        driver.findElement(By.cssSelector(".login")).click();
//        driver.findElement(By.id("login-input")).click();
        WebElement username = driver.findElement(By.id("login-input"));
        username.sendKeys("antony.stubbs@gmail.com");
        driver.findElement(By.name("password")).sendKeys("ADZZ/rct2CK6DFYrjCyh");
        driver.findElement(By.id("login-submit-button")).click();
//        username.submit();
    }

    private void waitForQueue() {
        throw new NotImplementedException("");
    }

    @SneakyThrows
    private void captchaMaybe() {
        // loop
        while (true) {
            // find
            CaptchaType captchaType = findIfCaptchaIsPresentAndType();
            // break?
            if (captchaType == NONE)
                break;

            // solve
            // get data
            while (true) {
                // have more tiles appeared? or has the captcha changed?
                CapchaData data = getData();
                boolean mock = false;
                Optional<List<Point>> coordinates = mock ? Optional.of(Lists.emptyList()) : solveCapcha(data);
                // String coordinates = mock ? "" : solveCapcha(data);

                if (coordinates.isEmpty()) {
                    log.warn("Captcha solution failed, skipping...");
                    break;
                } else {
                    // submit
                    submit(coordinates.get());

                    // stop analysing captcha if it's not a repeating one
                    if (captchaType == NORMAL)
                        break;

                    // wait for tiles to fade in
                    waitForTilesToFadeIn();
                }
            }
            clickVerify();

            // slow down
            Thread.sleep(500);
        }

        log.info("Captcha no longer detected...");
    }

    @SneakyThrows
    private void waitForTilesToFadeIn() {
        // TODO make this more dynamic by checking for CSS properties - perhaps check all cells in table are fully visible via opaqueness
        Thread.sleep(5000);
    }

    enum CaptchaType {
        NONE, NORMAL, UNTIL_NONE_LEFT;
    }

    private CaptchaType findIfCaptchaIsPresentAndType() {
        // Optional<WebElement> recaptchaResponseOpt = driver.findElements(By.id("recaptcha-token")).stream().findFirst();
        Optional<WebElement> first = driver.switchTo().defaultContent().findElements(By.xpath("/html/body/div[2]/div[2]/iframe")).stream().findFirst();
        boolean noCapcha = first.isEmpty();
        if (noCapcha) {
            log.info("Captcha not present");
            return NONE;
        } else {
            String instructions = findCaptchaInstructions();
            boolean specialInstructions = instructions.contains("once there are none left");
            if (specialInstructions) {
                log.info("Captcha has been detected - {}", UNTIL_NONE_LEFT);
                return UNTIL_NONE_LEFT;
            } else {
                log.info("Captcha has been detected - {}", NORMAL);
                return NORMAL;
            }
        }
    }

    private void clickVerify() {
        WebDriver webDriver = switchToCaptchaIFrame();
        Optional<WebElement> first = webDriver.findElements(By.className("rc-button-default")).stream().findFirst();
        first.get().click();
    }

    @SneakyThrows
    private void submit(List<Point> responseToken) {
//        translateCoords(responseToken);
//        Optional<WebElement> recaptchaResponseOpt = driver.findElements(By.className("rc-imageselect-challenge")).stream().findFirst();
//        WebElement webElement = recaptchaResponseOpt.get();

        WebElement webElement = driver.switchTo().defaultContent().findElement(By.xpath("/html/body/div[2]/div[2]/iframe"));


//        File screenshotAs = webElement.getScreenshotAs(FILE);

        Dimension size = webElement.getSize();

        int x = -1 * size.width / 2;
        int y = -1 * size.height / 2;

        responseToken.forEach(p -> {
            Actions actions = new Actions(driver);

            Integer px = p.getX();
            Integer py = p.getY();

            int adjustedX = px + x;
            int adjustedY = py + y;

            log.info("Slowly clicking at {},{}", adjustedX, adjustedY);
            actions.moveToElement(webElement, adjustedX, adjustedY).click().build().perform();

            try {
                Thread.sleep(MAX_SPEED.toMillis());
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        });
//        actions.build().perform();

//        WebElement webElement = recaptchaResponseOpt.get();
//        webElement.
//        webElement.sendKeys("");
//        webElement.submit();
    }

//    private void translateCoords(List<Point> responseToken, int x, int y) {
//        responseToken.stream().map(p -> {
//            return new Point(p.getX() + x, p.getY() + y);
//        });
//    }

    @SneakyThrows
    private CapchaData getData() {
        // find url
        WebElement iframe = getCaptchaIFrame();

//        log.info("Screenshot to file...");
//        driver.getScreenshotAs(FILE);
        String encodedImageData = shotElement(iframe);


//        List<WebElement> elements1 = iframe.findElements(By.className("rc-image-tile-33"));
//        List<WebElement> elements1 = frame.findElements(By.className("rc-image-tile-33"));
//        List<WebElement> elements = driver.findElements(By.className("rc-image-tile-33"));
//        String stringStream = driver.findElements(By.tagName("img")).stream()
//                .filter(x -> x.getAttribute("src").contains("https://www.google.com:443/recaptcha/api2/payload?"))
//                .findFirst()
//                .stream()
//                .map(x -> x.getAttribute("src"))
//                .findFirst()
//                .get();


//        File screenshotAs = driver.getScreenshotAs(OutputType.FILE);
//        String screenshotAs1 = driver.getScreenshotAs(OutputType.BASE64);

//        Optional<WebElement> recaptchaResponseOpt = driver.findElements(By.className("rc-imageselect-challenge")).stream().findFirst();
////        shotElement(recaptchaResponseOpt.get());
//        WebElement webElement = recaptchaResponseOpt.get();
//        File screenshotAs = webElement.getScreenshotAs(FILE);


        // download
//        log.info("Downloading captcha image: {}", stringStream);
//        URL url = new URL(stringStream);
//        Image image = ImageIO.read(url);
//
//
//        InputStream in = new BufferedInputStream(url.openStream());
//        ByteArrayOutputStream out = new ByteArrayOutputStream();
//
//        in.transferTo(out);
//
//        out.close();
//        in.close();
//
//        byte[] bytes = out.toByteArray();
//
//        // encode
//        String encoded = Base64.getEncoder().encodeToString(bytes);

//        List<WebElement> collect = webElementStream.collect(Collectors.toList());
//        Optional<WebElement> first = webElementStream.findFirst();

//        byte[] encodedBytes = Base64.getEncoder().encode("Test".getBytes());
//        System.out.println("encodedBytes " + new String(encodedBytes));
//        byte[] decodedBytes = Base64.getDecoder().decode(encodedBytes);
//        System.out.println("decodedBytes " + new String(decodedBytes));

        String strippedInstructions = findCaptchaInstructions();

        return new CapchaData(encodedImageData, strippedInstructions);
    }

    private String findCaptchaInstructions() {
        WebDriver frame = switchToCaptchaIFrame();

        List<WebElement> element = frame.findElements(By.className("rc-imageselect-desc-no-canonical"));
        if (element.isEmpty()) {
            // try other class
            element = frame.findElements(By.className("rc-imageselect-desc"));
        }
        String text = element.stream().findFirst().get().getText();
        //String strippedInstructions = text.strippedInstructions().replaceFirst("\n", " ").replaceFirst("\n.*", "");
        return text.strip().replaceFirst("\n", " ").replaceFirst("\n", ". ");
    }

    private WebDriver switchToCaptchaIFrame() {
        WebElement iframe = getCaptchaIFrame();

        String name = iframe.getAttribute("name");
        return driver.switchTo().frame(name);
    }

    private WebElement getCaptchaIFrame() {
        return driver.switchTo().defaultContent().findElement(By.xpath("/html/body/div[2]/div[2]/iframe"));
    }

    @SneakyThrows
    private String shotElement(WebElement ele) {
        log.info("Capturing rendered captcha image...");
        // Get entire page screenshot
        // TODO screenshot to memory buffer instead of disk
        log.info("Screenshot whole page...");
        File screenshot = driver.getScreenshotAs(FILE);
        // String screenshotAs = driver.getScreenshotAs(BASE64);
        log.info("Read screenshot off disk...");
        BufferedImage fullImg = ImageIO.read(screenshot);

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
        log.info("Scaling image...");
        scaledImage.createGraphics().drawImage(eleScreenshot, 0, 0, scaledWidth, scaledHeight, Color.WHITE, null);


        // Copy the element screenshot to disk
        // File tmpFile = File.createTempFile("screenshot", ".png");
        // tmpFile.deleteOnExit();
        // File screenshotLocation = new File("C:\\images\\GoogleLogo_screenshot.png");
        File screenshotLocationjpg = File.createTempFile("screenshot-part-jpg", ".jpg");
        screenshotLocationjpg.deleteOnExit();

        // File screenshotLocation = File.createTempFile("screenshot-part", ".png");

        log.info("Saving scaled image to disk...");
        boolean jpg1 = ImageIO.write(scaledImage, "jpg", screenshotLocationjpg);
        assertThat(jpg1).isTrue();


        // boolean jpg = ImageIO.write(eleScreenshot, "png", screenshotLocation);
        // assertThat(jpg).isTrue();

        ByteArrayOutputStream scaledOutputBuffer = new ByteArrayOutputStream();
        // BufferedOutputStream output = new BufferedOutputStream(scaledOutputBuffer);
        log.info("Save ...");
        boolean jpg = ImageIO.write(scaledImage, "jpg", scaledOutputBuffer);
        assertThat(jpg).isTrue();

        scaledOutputBuffer.close();

        log.info("Base64 encoding...");
        String base64EncodedImage = Base64.getEncoder().encodeToString(scaledOutputBuffer.toByteArray());
        // FileUtils.copyFile(screenshot, screenshotLocation);
        return base64EncodedImage;
    }

    @Data
    public
    class CapchaData {
        final String base64ImageData;
        final String instructions;
    }

    @SneakyThrows
    private Optional<List<Point>> solveCapcha(CapchaData data) {
        String ocadoDataSiteKey = "6LcRDbsUAAAAAP8Kg4CtjPzIY40yzlgwzXFV4JzV"; // data-sitekey
        String apiKey = "2c555debdc6d33fa0db1aa73aeaa45bd";
        String googleKey = ocadoDataSiteKey; // "6Le-wvkSAAAAAPBMRTvw0Q4Muexq9bi0DJwx_mJ-";
        String pageUrl = "https://accounts.ocado.com/auth-service/sso/login";
//        String proxyIp = "183.38.231.131";
//        String proxyPort = "8888";
//        String proxyUser = "username";
//        String proxyPw = "password";

        String proxyIp = "";
        String proxyPort = "";
        String proxyUser = "";
        String proxyPw = "";

        /**
         * With proxy and user authentication
         */
        TwoCaptchaService service = new TwoCaptchaService(apiKey, googleKey, pageUrl, proxyIp, proxyPort, proxyUser, proxyPw, ProxyType.HTTP);

        /**
         * Without proxy and user authentication
         * TwoCaptchaService service = new TwoCaptchaService(apiKey, googleKey, pageUrl);
         */

        try {
            Optional<List<Point>> responseToken = service.solveCaptcha(data);
            log.info("The response token is: " + responseToken);
            return responseToken;
        } catch (InterruptedException e) {
            System.out.println("ERROR case 1");
            throw e;
        } catch (IOException e) {
            System.out.println("ERROR case 2");
            throw e;
        }
    }
}
