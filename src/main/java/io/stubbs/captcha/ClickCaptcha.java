package io.stubbs.captcha;

import com.google.common.base.Stopwatch;
import io.stubbs.selenium.SeleniumUtils;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
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
import static io.stubbs.captcha.FunctionUtils.randomPause;
import static java.time.Duration.ofSeconds;

@Slf4j
@RequiredArgsConstructor
public class ClickCaptcha {

    Duration MAX_SPEED = ofSeconds(1);
    public static final String G_RECAPTCHA_RESPONSE_ID = "g-recaptcha-response";
    final private ChromeDriver driver;
    final private SeleniumUtils sutils;

    @SneakyThrows
    public void captchaMaybe() {
        Stopwatch started = Stopwatch.createStarted();
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
                CapchaData data = getCaptchaData(captchaType);
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
        }

        log.info("Captcha no longer detected... Took: {}", started.stop().elapsed());
    }

    @SneakyThrows
    private void waitForTilesToFadeIn() {
        // TODO make this more dynamic by checking for CSS properties - perhaps check all cells in table are fully visible via opaqueness
        Duration pauseTime = ofSeconds(8);
        log.info("Pausing for {} for new tiles to fade in...", pauseTime);
        Thread.sleep(pauseTime.toMillis());
    }

    enum CaptchaType {
        NONE, NORMAL, UNTIL_NONE_LEFT;
    }

    private CaptchaType findIfCaptchaIsPresentAndType() {
        String currentUrl = driver.getCurrentUrl();
        if (!currentUrl.contains("accounts.ocado.com/auth-service/sso/login"))
            return NONE;

        Optional<WebElement> first = driver.switchTo().defaultContent().findElements(By.xpath("/html/body/div[2]/div[2]/iframe")).stream().findFirst();
        boolean noCaptcha = first.isEmpty();
        if (noCaptcha) {
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
        randomPause("Clicking verify after pause...");
        WebDriver webDriver = switchToCaptchaIFrame();
        Optional<WebElement> first = webDriver.findElements(By.className("rc-button-default")).stream().findFirst();
        first.get().click();

        waitForCaptchaImageToSlideIn();
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

            randomPause("Pausing...");
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
    private CapchaData getCaptchaData(CaptchaType captchaType) {
        WebElement iframe = getCaptchaIFrame();

        String encodedImageData = sutils.shotElement(iframe);

        String strippedInstructions = findCaptchaInstructions();

        if (captchaType == UNTIL_NONE_LEFT)
            strippedInstructions = strippedInstructions + ". There may be none, in which case solve it with nothing clicked, don't click unsolvable.";

        return new CapchaData(encodedImageData, strippedInstructions);
    }

    @SneakyThrows
    private void waitForCaptchaImageToSlideIn() {
        CaptchaType captchaType = findIfCaptchaIsPresentAndType();
        if (captchaType != NONE) {
            log.info("Capture still present, image might be animating in from the side, after a previous round");
            // TODO make this smarter by testing for animation?
            Thread.sleep(2000);
        }
    }

    private String findCaptchaInstructions() {
        WebDriver frame = switchToCaptchaIFrame();

        List<WebElement> element = frame.findElements(By.className("rc-imageselect-desc-no-canonical"));
        if (element.isEmpty()) {
            // try other class
            element = frame.findElements(By.className("rc-imageselect-desc"));
        }
        String text = element.stream().findFirst().get().getText();
        text = Jsoup.parse(text).text();
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
