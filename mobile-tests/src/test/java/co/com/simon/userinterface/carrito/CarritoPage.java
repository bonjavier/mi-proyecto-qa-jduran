package co.com.simon.userinterface.carrito;

import io.appium.java_client.AppiumBy;
import net.serenitybdd.screenplay.targets.Target;

public class CarritoPage {

    public static final Target BTN_ITEM = Target.the("Seleccionar el item del carrito")
            .located(AppiumBy.accessibilityId("test-Item"));

    public static final Target BTN_REMOVE = Target.the("boton eliminar item del carrito")
            .located(AppiumBy.accessibilityId("test-REMOVE"));

    public static final Target BTN_DELETE_LATERAL = Target.the("boton eliminar item del carrito lado lateral izquierdo")
            .located(AppiumBy.accessibilityId("test-Delete"));

    public static final Target BTN_CHECKOUT = Target.the("boton para pasar al checkout")
            .located(AppiumBy.accessibilityId("test-CHECKOUT"));

    public static final Target LABEL_CANTIDAD = Target.the("Copy con la cantidad de productos agregados al carrito")
            .located(AppiumBy.accessibilityId("test-Amount"));

    public static final Target BTN_CARRITO = Target.the("boton carrito")
            .located(AppiumBy.accessibilityId("test-Cart"));

    public static final Target BTN_CONTINUE_SHOPPING = Target.the("boton carrito")
            .located(AppiumBy.accessibilityId("test-CONTINUE SHOPPING"));
}
