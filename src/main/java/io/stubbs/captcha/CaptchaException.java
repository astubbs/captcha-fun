package io.stubbs.captcha;

import org.openqa.selenium.StaleElementReferenceException;

public class CaptchaException extends Exception {
    public CaptchaException(StaleElementReferenceException e) {
        super(e);
    }

    public CaptchaException(String s) {
        super(s);
    }
}
