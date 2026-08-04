package co.com.simon.userinterface.addToCart;

import io.appium.java_client.AppiumBy;
import net.serenitybdd.screenplay.targets.Target;

public class AddToCardItemsPage {

    public static final Target BTN_ADDTOCARDITEM = Target.the("Boton agregar item al carrito")
            .locatedBy("(//android.view.ViewGroup[@content-desc='test-ADD TO CART'])[{0}]");

    public static final Target BTN_ARRASTRAR = Target.the("Boton arrastrar item al carrito")
            .locatedBy("(//android.view.ViewGroup[@content-desc=\"test-Drag Handle\"])[{0}]");

    public static final Target ZONA_DE_ARRASTRE = Target.the("Zona de arrastre para agregar el item al carrito")
            .locatedBy("//android.view.ViewGroup[@content-desc=\"test-Cart drop zone\"]");

    public static final Target BTN_CARRITO = Target.the("boton carrito")
            .located(AppiumBy.accessibilityId("test-Cart"));
}
