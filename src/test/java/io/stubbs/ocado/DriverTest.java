package io.stubbs.ocado;

import io.stubbs.selenium.SeleniumUtils;
import org.junit.After;
import org.junit.Before;
import org.openqa.selenium.chrome.ChromeDriver;

import java.util.logging.Level;
import java.util.logging.Logger;
import org.slf4j.bridge.SLF4JBridgeHandler;

public class DriverTest {

    {
        SLF4JBridgeHandler.removeHandlersForRootLogger();
        SLF4JBridgeHandler.install();
        Logger.getLogger("").setLevel(Level.FINEST); // Root logger, for example.
    }

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
