package io.stubbs.ocado;

import lombok.RequiredArgsConstructor;

import java.util.Optional;

@RequiredArgsConstructor
public class LoginAndCheckSchedules {

    final Login login;
    final Schedules schedules;

    public Optional<String> go() {
        Optional<String> start = login.start();
        if (start.isEmpty()) {
            schedules.checkSchedules();
        }
        return start;
    }
}
