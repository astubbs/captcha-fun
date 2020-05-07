package io.stubbs.ocado;

import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.WebElement;

import java.util.List;

@Slf4j
public class Notifier {

    void notifyOfSchedule(List<WebElement> x) {
        log.info("Found the following schedules...");
        log.info(x.toString());
    }

    public void notifyOfNoScheduleAvailable() {
        log.info("No schedules sorry :(");
    }
}
