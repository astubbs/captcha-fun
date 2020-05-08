package io.stubbs.ocado;

import lombok.extern.slf4j.Slf4j;
import org.junit.Before;
import org.junit.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@Slf4j
public class LoginTest extends DriverTest {

    Login login;

    @Before
    public void setupLogin() {
        login = new Login(driver);
    }

    @Test
    public void login() {
        login.login();
        String title = driver.getTitle();
        String currentUrl = driver.getCurrentUrl();
//        assertThat(title).contains("Schedule");
        assertThat(currentUrl).contains("q.ocado.com");
    }

    @Test
    public void queue() {
        String url = "file:///Users/antony/Downloads/OcadoQueue.html";
        driver.get(url);
        Optional<String> s = login.checkForQueueRedirect();
        assertThat(s.get()).contains("slots", "sorry", "coronavirus");
    }
}
