package io.stubbs.captcha;

import com.google.common.base.Stopwatch;
import io.stubbs.selenium.SeleniumUtils;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.interactions.Action;
import org.openqa.selenium.interactions.Actions;
import twocaptcha.api.ProxyType;
import twocaptcha.api.TwoCaptchaService;

import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.Optional;

import static io.stubbs.captcha.ClickCaptcha.CaptchaType.*;
import static io.stubbs.captcha.FunctionUtils.randomPause;
import static io.stubbs.captcha.FunctionUtils.time;
import static java.time.Duration.ofSeconds;

@Slf4j
@RequiredArgsConstructor
public class ClickCaptcha {

    final private ChromeDriver driver;
    final private SeleniumUtils sutils;

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
            try {
                solveLoop(captchaType);
            } catch (CaptchaException e) {
                log.info(e.getMessage());
                break;
            }
        }
        log.info("Captcha no longer detected... Took: {}", started.stop());
    }

    private void solveLoop(CaptchaType captchaType) throws CaptchaException {
        while (true) {
            CaptchaType newCaptchaTypeChange = findIfCaptchaIsPresentAndType(); // type has changed?
            if (captchaType != newCaptchaTypeChange) {
                log.info("the ol' switcheroo?");
                captchaType = newCaptchaTypeChange;
            }
            // have more tiles appeared? or has the captcha changed?
            CapchaData data;
            try{
                data = getCaptchaData(captchaType);
            } catch (intermittentScreenshotException e){
                e.printStackTrace();
                log.error("Error capturing data, abort and retry..?", e.toString());
                continue;
            }
            boolean mock = false;
            Optional<List<TwoCaptchaService.ResponseData.Point>> coordinates = mock ?
                    Optional.of(List.of()) :
                    solveCapcha(data);

            if (coordinates.isEmpty()) {
                log.warn("Captcha solution failed or finished, skipping round. Instructions where: {}...", data.getInstructions());
                break;
            } else {
                List<TwoCaptchaService.ResponseData.Point> responseToken = coordinates.get();
                log.info("{} tiles found apparently for type {}...", responseToken.size(), captchaType);


                try {
                    clickOnTiles(responseToken);
                } catch (CaptchaException e) {
                    // switicheroo?
                    e.printStackTrace();
                    break;
                }

                // TODO check if iframe captcha has now changed due to clicking on the wrong tile?

                // stop analysing captcha if it's not a repeating one
                if (captchaType != UNTIL_NONE_LEFT) {
                    break;
                } else {
                    // wait for tiles to fade in
                    waitForTilesToFadeIn();
                }
            }
        }
        clickVerify();
    }

    @SneakyThrows
    private void waitForTilesToFadeIn() {
        // TODO make this more dynamic by checking for CSS properties - perhaps check all cells in table are fully visible via opaqueness
        Duration pauseTime = ofSeconds(7);
        log.info("Pausing for {} for new tiles to fade in...", pauseTime);
        Thread.sleep(pauseTime.toMillis());
    }

    enum CaptchaType {
        NONE, NORMAL, UNTIL_NONE_LEFT;
    }

    @SneakyThrows
    private CaptchaType findIfCaptchaIsPresentAndType() {
        Thread.sleep(1000); //pause
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

    private void clickVerify() throws CaptchaException {
        randomPause("Clicking verify after pause...");
        WebDriver webDriver = switchToCaptchaIFrame();
        Optional<WebElement> first = webDriver.findElements(By.className("rc-button-default")).stream().findFirst();

        String beforeUrl = driver.getCurrentUrl();
        first.get().click();
        String afterUrl = driver.getCurrentUrl();

        if (!beforeUrl.equals(afterUrl)) {
            return; // page has changed
        } else {
            waitForCaptchaImageToSlideIn();
        }
    }

    private void clickOnTiles(List<TwoCaptchaService.ResponseData.Point> responseToken) throws CaptchaException {
        WebElement webElement = driver.switchTo().defaultContent().findElement(By.xpath("/html/body/div[2]/div[2]/iframe"));

        Dimension size = webElement.getSize();

        int x = -1 * size.width / 2;
        int y = -1 * size.height / 2;

        for (TwoCaptchaService.ResponseData.Point p : responseToken) {
            Actions actions = new Actions(driver);

            Integer px = p.getX();
            Integer py = p.getY();

            int adjustedX = px + x;
            int adjustedY = py + y;

            log.info("Slowly clicking at {},{}", adjustedX, adjustedY);
            try {
                Action build = actions.moveToElement(webElement, adjustedX, adjustedY).click().build();
                build.perform();
            } catch (StaleElementReferenceException e) {
                throw new CaptchaException(e);
            }

            randomPause("Pausing...");
        }
    }


    private CapchaData getCaptchaData(CaptchaType captchaType) throws intermittentScreenshotException {
        WebElement iframe = getCaptchaIFrame();

        //String encodedImageData = time("Screenshot element", () -> sutils.shotElement(iframe));
        String encodedImageData = sutils.shotElement(iframe);

        String strippedInstructions = findCaptchaInstructions();

        if (captchaType == UNTIL_NONE_LEFT)
            strippedInstructions = strippedInstructions + ". There may be none, in which case solve it with nothing clicked, don't click unsolvable.";

        return new CapchaData(encodedImageData, strippedInstructions);
    }

    private void waitForCaptchaImageToSlideIn() throws CaptchaException {
        CaptchaType captchaType = findIfCaptchaIsPresentAndType();
        if (captchaType != NONE) {
            log.info("Capture still present, image might be animating in from the side, after a previous round");
            // TODO make this smarter by testing for animation?
            int SLIDE_ANIMATION_DELAY = 1000;
            try {
                Thread.sleep(SLIDE_ANIMATION_DELAY);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        } else {
            throw new CaptchaException("Captcha no longer present");
        }
    }

    private String findCaptchaInstructions() {
        WebDriver frame = switchToCaptchaIFrame();

        List<WebElement> element = frame.findElements(By.className("rc-imageselect-desc-no-canonical"));
        if (element.isEmpty()) {
            // try other class
            element = frame.findElements(By.className("rc-imageselect-desc"));
        }
        WebElement webElement = element.stream().findFirst().get();
        try {
            String text = webElement.getText();
            text = Jsoup.parse(text).text();
            return text.strip().replaceFirst("\n", " ").replaceFirst("\n", ". ");
        } catch (StaleElementReferenceException e) {
            e.printStackTrace();
            throw e;
        }
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
