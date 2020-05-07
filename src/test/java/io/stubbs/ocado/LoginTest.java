package io.stubbs.ocado;

import lombok.extern.slf4j.Slf4j;
import org.junit.Test;

@Slf4j
public class LoginTest {

    @Test
    public void login() {
        Login login = new Login();
        login.login();
    }

}
