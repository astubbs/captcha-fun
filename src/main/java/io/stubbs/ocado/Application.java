package io.stubbs.ocado;

import io.github.bonigarcia.wdm.WebDriverManager;
import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.slf4j.bridge.SLF4JBridgeHandler;

import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

@Slf4j
public class Application {

    {
        SLF4JBridgeHandler.removeHandlersForRootLogger();
        SLF4JBridgeHandler.install();
        Logger.getLogger("").setLevel(Level.FINEST); // Root logger, for example.
    }

    public static void main(String[] args) {
        WebDriverManager.chromedriver().setup();
        ChromeOptions chromeOptions = new ChromeOptions();

        ChromeDriver driver = new ChromeDriver(chromeOptions);

        LoginAndCheckSchedules t = new LoginAndCheckSchedules(new Login(driver), new Schedules(driver, new Notifier()));
        Optional<String> go = t.go();
        log.info(go.toString());
    }
}
