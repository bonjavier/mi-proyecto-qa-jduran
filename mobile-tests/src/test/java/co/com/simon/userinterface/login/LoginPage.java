package co.com.simon.userinterface.login;


import io.appium.java_client.AppiumBy;
import net.serenitybdd.screenplay.targets.Target;


public class LoginPage {

    public static final Target INPUT_USERNAME = Target.the("input username")
            .located(AppiumBy.accessibilityId("test-Username"));

    public static final Target INPUT_PASSWORD = Target.the("input password")
            .located(AppiumBy.accessibilityId("test-Password"));

    public static final Target BTN_LOGIN = Target.the("Login button")
            .located(AppiumBy.accessibilityId("test-LOGIN"));

    public static final Target ERROR_MESSAGE = Target.the("login error message")
            .located(AppiumBy.accessibilityId("test-Error message"));

}


