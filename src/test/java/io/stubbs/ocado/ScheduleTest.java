package io.stubbs.ocado;

import org.junit.Test;

import java.util.List;

import static org.mockito.Mockito.*;

public class ScheduleTest extends DriverTest {

    @Test
    public void none() {
        Notifier mock = mock(Notifier.class);

        Schedules schedules = new Schedules(driver, mock);
        Schedules.URL = "file:///Users/antony/Downloads/Ocado_ Delivery_ Choose an available delivery slot.html";
        schedules.checkSchedules();

        verify(mock).notifyOfNoScheduleAvailable();
//        verify(mock).notifyOfSchedule(any());
    }

}
