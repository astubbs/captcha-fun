package io.stubbs.ocado;

import io.stubbs.captcha.ClickCaptcha;
import io.stubbs.selenium.SeleniumUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.NotImplementedException;
import org.jsoup.Jsoup;
import org.openqa.selenium.By;
import org.openqa.selenium.Cookie;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;

import java.util.List;
import java.util.Optional;

@Slf4j
public class Login {

    private ChromeDriver driver;

    private ClickCaptcha cc;

    static public String url = "https://accounts.ocado.com/auth-service/sso/login";

    Login(ChromeDriver driver) {
        SeleniumUtils sutils = new SeleniumUtils(driver);
        cc = new ClickCaptcha(driver, sutils);
        this.driver = driver;
    }

    public void close() {
        driver.quit();
    }

    public Optional<String> start() {
        return login();
    }

    public Optional<String> login() {
        driver.get(Login.url);

        Optional<String> immediateQueue = checkForQueueRedirect();
        if (immediateQueue.isPresent())
            return immediateQueue;

        submitCreds();

        // TODO save session / cookie data to avoid having to log in again before needed
        cc.captchaMaybe();

        log.info("Dumping cookies:");
        for (Cookie cookie : driver.manage().getCookies()) {
            log.info("Cookie: {}", cookie);
        }

        Optional<String> s = checkForQueueRedirect();

        log.info("Log in complete...");

        return s;
    }

    Optional<String> checkForQueueRedirect() {
        driver.switchTo().defaultContent();
        String currentUrl = driver.getCurrentUrl();
        List<WebElement> elementsById = driver.findElementsById("queue-it_log");
        if (!elementsById.isEmpty()) {
            List<WebElement> message = driver.findElementsById("message");
            if (message.isEmpty())
                throw new NotImplementedException("Unhandled state");
            driver.switchTo().frame("message");
            WebElement elementByXPath = driver.findElementByXPath("/html/body");
            String text = Jsoup.parse(elementByXPath.getText()).text();
            return Optional.of(text);
        } else {
            return Optional.empty();
        }
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

}
