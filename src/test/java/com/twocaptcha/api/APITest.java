package com.twocaptcha.api;


import org.junit.Test;
import twocaptcha.api.TwoCaptchaService;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class APITest {

    /**
     * OK|coordinates:x=357,y=162;x=247,y=272;x=156,y=354
     * {"status":1,"request":[{"x":"49","y":"235"},{"x":"52","y":"31"},{"x":"126","y":"43"},{"x":"239","y":"226"}]}
     */
    @Test
    public void test() {
        String input = """
                {
                "status":1,
                "request":
                    [{
                        "x":"374",
                        "y":"119"
                    },
                    {
                        "x":"10",
                        "y":"20"
                    }]
                }""";

        TwoCaptchaService s = new TwoCaptchaService(null, null, null);

        List<TwoCaptchaService.ResponseData.Point> points = s.parseResponse(input).get();

        assertThat(points).hasSize(2);
        assertThat(points.get(1).getX()).isEqualTo(10);
    }


}
