package twocaptcha.api;

import com.google.common.base.Stopwatch;
import com.google.gson.Gson;
import io.stubbs.captcha.ClickCaptcha.CapchaData;
import kong.unirest.HttpResponse;
import kong.unirest.JsonNode;
import kong.unirest.MultipartBody;
import kong.unirest.Unirest;
import kong.unirest.json.JSONObject;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import twocaptcha.http.HttpWrapper;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
public class TwoCaptchaService {
    public static final String TWOCAPTCHA_IN_URL = "http://2captcha.com/in.php?";
    public static final double API_ERROR_CODE = 0.0;

    /**
     * This class is used to establish a connection to 2captcha.com
     * and receive the token for solving google recaptcha v2
     *
     * @author Chillivanilli
     * @version 1.0
     *
     * If you have a custom software requests, please contact me
     * via forum: http://thebot.net/members/chillivanilli.174861/
     * via eMail: chillivanilli@chillibots.com
     * via skype: ktlotzek
     */


    /**
     * Your 2captcha.com captcha KEY
     */
    private String apiKey;


    /**
     * The google site key from the page you want to solve the recaptcha at
     */
    private String googleKey;


    /**
     * The URL where the recaptcha is placed.
     * For example: https://www.google.com/recaptcha/api2/demo
     */
    private String pageUrl;

    /**
     * The proxy ip if you want a worker to solve the recaptcha through your proxy
     */
    private String proxyIp;

    /**
     * The proxy port
     */
    private String proxyPort;

    /**
     * Your proxy username, if your proxy uses user authentication
     */
    private String proxyUser;

    /**
     * Your proxy password, if your proxy uses user authentication
     */
    private String proxyPw;

    /**
     * Your proxy type, for example ProxyType.HTTP
     */
    private ProxyType proxyType;

    /**
     * The HttpWrapper which the requests are made with
     */
    private HttpWrapper hw;


    /**
     * Constructor if you don't use any proxy
     *
     * @param apiKey
     * @param googleKey
     * @param pageUrl
     */
    public TwoCaptchaService(String apiKey, String googleKey, String pageUrl) {
        this.apiKey = apiKey;
        this.googleKey = googleKey;
        this.pageUrl = pageUrl;
        hw = new HttpWrapper();
    }

    /**
     * Constructor if you are using a proxy without user authentication
     *
     * @param apiKey
     * @param googleKey
     * @param pageUrl
     * @param proxyIp
     * @param proxyPw
     * @param proxyType
     */
    public TwoCaptchaService(String apiKey, String googleKey, String pageUrl, String proxyIp, String proxyPort, ProxyType proxyType) {
        this(apiKey, googleKey, pageUrl);
        this.proxyIp = proxyIp;
        this.proxyPort = proxyPort;
        this.proxyType = proxyType;
    }

    /**
     * Constructor if you are using a proxy with user authentication
     *
     * @param apiKey
     * @param googleKey
     * @param pageUrl
     * @param proxyIp
     * @param proxyPort
     * @param proxyUser
     * @param proxyPw
     * @param proxyType
     */
    public TwoCaptchaService(String apiKey, String googleKey, String pageUrl, String proxyIp, String proxyPort,
                             String proxyUser, String proxyPw, ProxyType proxyType) {
        this(apiKey, googleKey, pageUrl);
        this.proxyIp = proxyIp;
        this.proxyPort = proxyPort;
        this.proxyUser = proxyUser;
        this.proxyPw = proxyPw;
        this.proxyType = proxyType;
    }

    /**
     * Sends the recaptcha challenge to 2captcha.com and
     * checks every second if a worker has solved it
     *
     * @param data
     * @return The response-token which is needed to solve and submit the recaptcha
     * @throws InterruptedException, when thread.sleep is interrupted
     * @throws IOException,          when there is any server issue and the request cannot be completed
     */
    public Optional<List<ResponseData.Point>> solveCaptcha(CapchaData data) throws InterruptedException, IOException {
        log.info("Sending recaptcha challenge to 2captcha.com...");

        Stopwatch w = Stopwatch.createStarted();
        String response = data == null ? getMethod() : postMethod(data);

        if (response.contains("ERROR_ZERO_BALANCE")) {
            throw new RuntimeException("No account balance: " + response.stripTrailing());
        }

        String captchaId = response.replaceAll("\\D", "");
        int timeCounter = 0;

        int INITIAL_DELAY = 8000;
        Thread.sleep(INITIAL_DELAY);

        do {
            hw.get("http://2captcha.com/res.php?key=" + apiKey
                    + "&action=get"
                    + "&json=1"
                    + "&id=" + captchaId);

            Thread.sleep(1000);

            timeCounter++;
            log.info("Waiting for captcha to be solved: {} try, {}", timeCounter, w);
        } while (hw.getHtml().contains("NOT_READY"));

        log.info("It took " + timeCounter + " seconds to solve the captcha");
        String responseBody = hw.getHtml();
        Optional<List<ResponseData.Point>> coordinates = parseResponse(responseBody);
        return coordinates;
    }

    public Optional<List<ResponseData.Point>> parseResponse(String responseBody) {
        Map mapResponse = new Gson().fromJson(responseBody, Map.class); // inconsistent return types, so just check status first
        if (mapResponse.get("status").equals(API_ERROR_CODE))
            return Optional.empty();
        ResponseData responseData = new Gson().fromJson(responseBody, ResponseData.class);
        List<ResponseData.Point> request = responseData.getRequest();
        if (request.isEmpty()) {
            log.info("Service returned a `success` result of zero coordinates");
        }
        return Optional.of(request);
    }

    @Data
    @AllArgsConstructor
    public class ResponseData {
        final Double status;
        List<Point> request;

        @Data
        public class Point {
            final Integer x;
            final Integer y;
        }
    }

    private String postMethod(CapchaData data) {
        log.info("Instructions; {}", data.getInstructions());
        MultipartBody request = Unirest.post(TWOCAPTCHA_IN_URL)
                .field("googlekey", googleKey)
                .field("key", apiKey)
                .field("coordinatescaptcha", "1")
                .field("textinstructions", data.getInstructions())
                .field("body", data.getBase64ImageData())
                .field("method", "base64")
                .field("json", "1");

        HttpResponse<JsonNode> response = request.asJson();

        response.ifFailure(x -> {
            throw new RuntimeException(x.getBody().toPrettyString());
        });
        int status = response.getStatus();
        String string = response.getBody().toPrettyString();
        JSONObject body = response.getBody().getObject();
        String captchaId = body.getString("request");
        String apiStatus = body.getString("status");
        if (!apiStatus.equals("1"))
            throw new RuntimeException("Api error status: " + status + " request: " + request);
        return captchaId;
    }

    private String getMethod() {
        String parameters = "key=" + apiKey
                + "&method=userrecaptcha"
                + "&googlekey=" + googleKey
                + "&pageurl=" + pageUrl;

        if (proxyIp != null) {
            if (proxyUser != null) {
                parameters += "&proxy="
                        + proxyUser + ":" + proxyPw
                        + "@"
                        + proxyIp + ":" + proxyPort;
            } else {
                parameters += "&proxy="
                        + proxyIp + ":" + proxyPort;
            }

            parameters += "&proxytype=" + proxyType;
        }
        hw.get(TWOCAPTCHA_IN_URL + parameters);

        return hw.getHtml();
    }

    /**
     * @return The 2captcha.com captcha key
     */
    public String getApiKey() {
        return apiKey;
    }

    /**
     * Sets the 2captcha.com captcha key
     *
     * @param apiKey
     */
    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    /**
     * @return The google site key
     */
    public String getGoogleKey() {
        return googleKey;
    }

    /**
     * Sets the google site key
     *
     * @param googleKey
     */
    public void setGoogleKey(String googleKey) {
        this.googleKey = googleKey;
    }

    /**
     * @return The page url
     */
    public String getPageUrl() {
        return pageUrl;
    }

    /**
     * Sets the page url
     *
     * @param pageUrl
     */
    public void setPageUrl(String pageUrl) {
        this.pageUrl = pageUrl;
    }

    /**
     * @return The proxy ip
     */
    public String getProxyIp() {
        return proxyIp;
    }

    /**
     * Sets the proxy ip
     *
     * @param proxyIp
     */
    public void setProxyIp(String proxyIp) {
        this.proxyIp = proxyIp;
    }

    /**
     * @return The proxy port
     */
    public String getProxyPort() {
        return proxyPort;
    }

    /**
     * Sets the proxy port
     *
     * @param proxyPort
     */
    public void setProxyPort(String proxyPort) {
        this.proxyPort = proxyPort;
    }

    /**
     * @return The proxy authentication user
     */
    public String getProxyUser() {
        return proxyUser;
    }

    /**
     * Sets the proxy authentication user
     *
     * @param proxyUser
     */
    public void setProxyUser(String proxyUser) {
        this.proxyUser = proxyUser;
    }

    /**
     * @return The proxy authentication password
     */
    public String getProxyPw() {
        return proxyPw;
    }

    /**
     * Sets the proxy authentication password
     *
     * @param proxyPw
     */
    public void setProxyPw(String proxyPw) {
        this.proxyPw = proxyPw;
    }

    /**
     * @return The proxy type
     */
    public ProxyType getProxyType() {
        return proxyType;
    }

    /**
     * Sets the proxy type
     *
     * @param proxyType
     */
    public void setProxyType(ProxyType proxyType) {
        this.proxyType = proxyType;
    }
}
