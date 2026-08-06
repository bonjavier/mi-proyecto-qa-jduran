package co.com.simon.userinterface.comprarealizada;

import io.appium.java_client.AppiumBy;
import net.serenitybdd.screenplay.targets.Target;

public class AcceptOrderTask {

    public static final Target LBL_CHECKOUT_COMPLETE = Target.the("label Checkout: Complete")
            .located(AppiumBy.xpath("//android.widget.TextView[@text=\"CHECKOUT: COMPLETE!\"]"));

    public static final Target TITLE_LABEL_THANK_ORDER = Target.the("titulo gracias por tu compra")
            .located(AppiumBy.xpath("//android.widget.TextView[@text=\"THANK YOU FOR YOU ORDER\"]"));

    public static final Target SUBTITLE_LABEL_THANK_ORDER = Target.the("subtitulo gracias por tu compra")
            .located(AppiumBy.xpath("//android.widget.TextView[@text=\"Your order has been dispatched, and will arrive just as fast as the pony can get there!\"]"));

    public static final Target BTN_BACK_HOME = Target.the("boton ir al home")
            .located(AppiumBy.accessibilityId("test-BACK HOME"));

    public static final Target BTN_MENU_HOME = Target.the("boton MENU_HOME en el menu")
            .located(AppiumBy.accessibilityId("test-Menu"));

    public static final Target BTN_ALL_ITEMS = Target.the("boton ALL_ITEMS en el menu")
            .located(AppiumBy.accessibilityId("test-ALL ITEMS"));

    public static final Target BTN_WEBVIEW = Target.the("boton WEBVIEW en el menu")
            .located(AppiumBy.accessibilityId("test-WEBVIEW"));

    public static final Target BTN_QR_CODE_SCANNER = Target.the("boton QR_CODE_SCANNER en el menu")
            .located(AppiumBy.accessibilityId("test-QR CODE SCANNER"));

    public static final Target BTN_GEO_LOCATION = Target.the("boton GEO_LOCATION en el menu")
            .located(AppiumBy.accessibilityId("test-GEO LOCATION"));

    public static final Target BTN_DRAWING = Target.the("boton DRAWING en el menu")
            .located(AppiumBy.accessibilityId("test-DRAWING"));

    public static final Target BTN_ABOUT = Target.the("boton ABOUT en el menu")
            .located(AppiumBy.accessibilityId("test-ABOUT"));

    public static final Target BTN_LOGOUT = Target.the("boton LOGOUT en el menu")
            .located(AppiumBy.accessibilityId("test-LOGOUT"));

    public static final Target BTN_RESET_APP_STATET = Target.the("boton RESET_APP_STATET en el menu")
            .located(AppiumBy.accessibilityId("test-RESET APP STATET"));


}
