package co.com.simon.userinterface.confirmarcompra;

import io.appium.java_client.AppiumBy;
import net.serenitybdd.screenplay.targets.Target;

public class ConfirmarCompraPage {

    public static final Target BTN_CARRITO = Target.the("boton carrito")
            .located(AppiumBy.accessibilityId("test-Cart"));

    public static final Target BTN_DELETE_LATERAL = Target.the("boton eliminar item del carrito lado lateral izquierdo")
            .located(AppiumBy.accessibilityId("test-Delete"));

    public static final Target BTN_ITEM = Target.the("Seleccionar el item del carrito")
            .located(AppiumBy.accessibilityId("test-Item"));

    public static final Target LABEL_PAYMENT_INFORMATION = Target.the("COPY Payment Information")
            .located(AppiumBy.xpath("//android.widget.TextView[@text='Payment Information:' ]"));

    public static final Target LABEL_SHIPPING_INFORMATION = Target.the("COPY SHIPPING Information")
            .located(AppiumBy.xpath("//android.widget.TextView[@text='Shipping Information:' ]"));

    public static final Target LABEL_CANTIDAD = Target.the("copy cantidad de productos agregados al carrito")
            .located(AppiumBy.accessibilityId("test-Amount"));

    public static final Target BTN_CANCEL = Target.the("boton cancelar compra")
            .located(AppiumBy.accessibilityId("test-CANCEL"));

    public static final Target BTN_FINISH = Target.the("boton finalizar compra")
            .located(AppiumBy.accessibilityId("test-FINISH"));

}
