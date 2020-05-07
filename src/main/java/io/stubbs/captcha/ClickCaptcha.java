package io.stubbs.captcha;

import io.stubbs.selenium.SeleniumUtils;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.By;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.interactions.Actions;
import twocaptcha.api.ProxyType;
import twocaptcha.api.TwoCaptchaService;

import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.Optional;

import static io.stubbs.captcha.ClickCaptcha.CaptchaType.*;

@Slf4j
@RequiredArgsConstructor
public class ClickCaptcha {

    Duration MAX_SPEED = Duration.ofSeconds(1);
    public static final String G_RECAPTCHA_RESPONSE_ID = "g-recaptcha-response";
    final private ChromeDriver driver;
    final private SeleniumUtils sutils;

    @SneakyThrows
    public void captchaMaybe() {
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
                Optional<List<TwoCaptchaService.ResponseData.Point>> coordinates = mock ? Optional.of(List.of()) : solveCapcha(data);
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
        Duration pauseTime = Duration.ofSeconds(5);
        log.info("Pausing for {} for new tiles to fade in...", pauseTime);
        Thread.sleep(pauseTime.toMillis());
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
    private void submit(List<TwoCaptchaService.ResponseData.Point> responseToken) {
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
        String encodedImageData = sutils.shotElement(iframe);


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


    @Data
    public
    class CapchaData {
        final String base64ImageData;
        final String instructions;
    }

    @SneakyThrows
    private Optional<List<TwoCaptchaService.ResponseData.Point>> solveCapcha(CapchaData data) {

        String googleKey = Config.ocadoDataSiteKey; // "6Le-wvkSAAAAAPBMRTvw0Q4Muexq9bi0DJwx_mJ-";
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
        TwoCaptchaService service = new TwoCaptchaService(Config.apiKey, googleKey, pageUrl, proxyIp, proxyPort, proxyUser, proxyPw, ProxyType.HTTP);

        /**
         * Without proxy and user authentication
         * TwoCaptchaService service = new TwoCaptchaService(apiKey, googleKey, pageUrl);
         */

        try {
            Optional<List<TwoCaptchaService.ResponseData.Point>> responseToken = service.solveCaptcha(data);
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
