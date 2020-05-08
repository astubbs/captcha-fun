package io.stubbs.ocado;

import org.junit.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

public class LoginAndCheckSchedulesTest extends DriverTest {

    @Test
    public void go() {
        Login login = new Login(driver);
        Schedules schedules = new Schedules(driver, new Notifier());
        LoginAndCheckSchedules t = new LoginAndCheckSchedules(login, schedules);
        Optional<String> go = t.go();
        assertThat(go).isEmpty();
    }
}
