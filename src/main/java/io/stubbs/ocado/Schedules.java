package io.stubbs.ocado;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;

import java.util.List;
import java.util.Optional;

@Slf4j
@RequiredArgsConstructor
public class Schedules {

    public static String URL = "https://www.ocado.com/webshop/getAddressesForDelivery.do";
    final private ChromeDriver driver;
    final private Notifier notify;

    public void checkSchedules() {
        goToPage();
        Optional<List<WebElement>> schedules = getSchedules();
        process(schedules);
    }

    private void process(Optional<List<WebElement>> schedules) {
        schedules.ifPresentOrElse(x -> {
            notify.notifyOfSchedule(x);
        }, () -> {
            notify.notifyOfNoScheduleAvailable();
        });
    }

    private Optional<List<WebElement>> getSchedules() {
        List<WebElement> noSlotsElement = driver.findElementsByClassName("no-slots-msg");
        if (!noSlotsElement.isEmpty())
            return Optional.empty();
        List<WebElement> schedules = driver.findElementsByClassName("schedules");
        if (schedules.isEmpty())
            return Optional.empty();
        else
            return Optional.of(schedules);
    }

    private void goToPage() {
        driver.get(URL);
        String title = driver.getTitle();
        if (!title.equalsIgnoreCase("Ocado: Delivery: Choose an available delivery slot"))
            throw new RuntimeException("couldn't load page. Title=" + title);
    }

}
