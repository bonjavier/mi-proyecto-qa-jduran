package co.com.simon.userinterface.home;

import io.appium.java_client.AppiumBy;
import net.serenitybdd.screenplay.targets.Target;

public class HomePage {

    public static final Target BTN_MENU_HAMBURGUESA = Target.the("menu hamburguesa")
            .located(AppiumBy.accessibilityId("test-Menu"));

    public static final Target BTN_CARRITO = Target.the("boton carrito")
            .located(AppiumBy.accessibilityId("test-Cart"));

    public static final Target BTN_TOGGLE = Target.the("boton toggle")
            .located(AppiumBy.accessibilityId("test-Toggle"));

    public static final Target BTN_FILTRO = Target.the("boton filtro")
            .located(AppiumBy.accessibilityId("test-Modal Selector Button"));

    public static final Target BTN_ORDER_BY_A_TO_Z = Target.the("boton ordernar items de A a Z")
            .located(AppiumBy.xpath("//android.widget.TextView[@text=\"Name (A to Z)\"]"));

    public static final Target BTN_ORDER_BY_Z_TO_A = Target.the("boton ordernar items de Z a A")
            .located(AppiumBy.xpath("//android.widget.TextView[@text=\"Name (Z to A)\"]"));

    public static final Target BTN_ORDER_BY_PRICE_LOW_TO_HIGH = Target.the("boton ordernar items precio de menor a mayor")
            .located(AppiumBy.xpath("//android.widget.TextView[@text=\"Price (low to high)\"]"));

    public static final Target BTN_ORDER_BY_PRICE_HIGH_TO_LOW = Target.the("boton ordernar items precio de mayor a menor")
            .located(AppiumBy.xpath("//android.widget.TextView[@text=\"Price (high to low)\"]"));

    public static final Target BTN_CANCEL = Target.the("boton cancelar modal de filtros")
            .located(AppiumBy.xpath("//android.widget.TextView[@text=\"Cancel\"]"));

    public static final Target TEXT_TERMS_OF_SERVICE = Target.the("texto terminos de servicio")
            .located(AppiumBy.xpath("//android.widget.TextView[@text=\"Terms of Service | Privacy Policy\"]"));

}
