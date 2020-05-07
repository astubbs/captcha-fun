package io.stubbs.ocado;

import io.github.bonigarcia.wdm.WebDriverManager;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;

public class Application {
    public static void main(String[] args) {
        WebDriverManager.chromedriver().setup();
        ChromeOptions chromeOptions = new ChromeOptions();

        ChromeDriver driver = new ChromeDriver(chromeOptions);

        Login login = new Login(driver);
        login.start();
    }
}
