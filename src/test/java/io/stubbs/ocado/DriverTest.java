package io.stubbs.ocado;

import io.github.bonigarcia.wdm.WebDriverManager;
import io.stubbs.selenium.SeleniumUtils;
import org.junit.After;
import org.junit.Before;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;

public class DriverTest {

    protected ChromeDriver driver;

    @Before
    public void setupDriver() {
        driver = SeleniumUtils.getChromeDriver();
    }

    @After
    public void teardown() {
        driver.close();
    }
}
