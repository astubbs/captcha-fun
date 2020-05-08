package io.stubbs.ocado;

import io.github.bonigarcia.wdm.WebDriverManager;
import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;

import java.util.Optional;

@Slf4j
public class Application {
    public static void main(String[] args) {
        WebDriverManager.chromedriver().setup();
        ChromeOptions chromeOptions = new ChromeOptions();

        ChromeDriver driver = new ChromeDriver(chromeOptions);

        LoginAndCheckSchedules t = new LoginAndCheckSchedules(new Login(driver), new Schedules(driver, new Notifier()));
        Optional<String> go = t.go();
        log.info(go.toString());
    }
}
