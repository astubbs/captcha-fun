package io.stubbs.captcha;

public class DefaultSecretProvider {

    String get(String key) {
        String getenv = System.getenv(key);
        return getenv;
    }

}
