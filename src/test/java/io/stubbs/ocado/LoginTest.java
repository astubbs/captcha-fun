package io.stubbs.ocado;

import lombok.extern.slf4j.Slf4j;
import org.junit.Test;

@Slf4j
public class LoginTest extends DriverTest {

    @Test
    public void login() {
        Login login = new Login(driver);
        login.login();
    }

}
