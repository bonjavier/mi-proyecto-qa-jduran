package co.com.simon.userinterface.checkout;

import io.appium.java_client.AppiumBy;
import net.serenitybdd.screenplay.targets.Target;

public class CheckoutPage {

    public static final Target INPUT_FIRST_NAME = Target.the("Input primer nombre")
            .located(AppiumBy.accessibilityId("test-First Name"));

    public static final Target INPUT_LAST_NAME = Target.the("Input primer apellido")
            .located(AppiumBy.accessibilityId("test-Last Name"));

    public static final Target INPUT_POSTAL_CODE = Target.the("Input codigo postal")
            .located(AppiumBy.accessibilityId("test-Zip/Postal Code"));

    public static final Target BTN_CANCEL = Target.the("boton cancelar checkout")
            .located(AppiumBy.accessibilityId("test-CANCEL"));

    public static final Target BTN_CONTINUE = Target.the("boton continuar checkout")
            .located(AppiumBy.accessibilityId("test-CONTINUE"));

}
