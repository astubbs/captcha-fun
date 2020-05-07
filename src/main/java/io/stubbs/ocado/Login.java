package io.stubbs.ocado;

import io.github.bonigarcia.wdm.WebDriverManager;
import io.stubbs.captcha.ClickCaptcha;
import io.stubbs.selenium.SeleniumUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.NotImplementedException;
import org.openqa.selenium.By;
import org.openqa.selenium.Cookie;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;

@Slf4j
public class Login {

    private ChromeDriver driver;

    private ClickCaptcha cc;

    Login() {
        WebDriverManager.chromedriver().setup();
        ChromeOptions chromeOptions = new ChromeOptions();

        driver = new ChromeDriver(chromeOptions);
        SeleniumUtils sutils = new SeleniumUtils(driver);
        cc = new ClickCaptcha(driver, sutils);
    }

    public void close() {
        driver.quit();
    }

    public void start() {
        login();
    }

    public void login() {
        driver.get("https://accounts.ocado.com/auth-service/sso/login");

        submitCreds();

        // TODO save session / cookie data to avoid having to log in again before needed
        cc.captchaMaybe();

        log.info("Dumping cookies:");
        for (Cookie cookie : driver.manage().getCookies()) {
            log.info("Cookie: {}", cookie);
        }

        checkForQueueRedirect();

        log.info("Log in complete...");

    }

    private void checkForQueueRedirect() {
        //        driver.findElement(By.linkText("log in.")).click();
        driver.switchTo().defaultContent();
        driver.findElement(By.cssSelector("html")).click();
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
