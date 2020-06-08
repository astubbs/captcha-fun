package io.stubbs.captcha;

public class Config {
    static DefaultSecretProvider secretProvider = new DefaultSecretProvider();
    static String ocadoDataSiteKey = "6LcRDbsUAAAAAP8Kg4CtjPzIY40yzlgwzXFV4JzV"; // data-sitekey
    static String apiKey = secretProvider.get("apiKey");
}
