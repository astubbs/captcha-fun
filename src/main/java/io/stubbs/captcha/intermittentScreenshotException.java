package io.stubbs.captcha;

import org.openqa.selenium.StaleElementReferenceException;

public class intermittentScreenshotException extends Exception {
    public intermittentScreenshotException(Exception e) {
        super(e);
    }

    public intermittentScreenshotException(String s) {
        super(s);
    }
}
