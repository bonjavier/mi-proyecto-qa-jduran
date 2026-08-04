package co.com.simon.tasks.acceptOrder;

import net.serenitybdd.screenplay.Actor;
import net.serenitybdd.screenplay.Task;
import net.serenitybdd.screenplay.actions.Click;
import net.serenitybdd.screenplay.ensure.Ensure;

import static net.serenitybdd.screenplay.Tasks.instrumented;

import co.com.simon.interactions.Arrastrar;
import co.com.simon.interactions.Scroll;
import co.com.simon.models.ScrollDirection;
import co.com.simon.userinterface.confirmarcompra.ConfirmarCompraPage;

public class AcceptOrderTask implements Task {

    public static AcceptOrderTask acceptOrder() {
        return instrumented(AcceptOrderTask.class);
    }

    @Override
    public <T extends Actor> void performAs(T actor) {
        actor.attemptsTo(
                Ensure.that(ConfirmarCompraPage.BTN_CARRITO).isDisplayed(),
                Arrastrar.desde(ConfirmarCompraPage.BTN_ITEM)
                        .hasta(ConfirmarCompraPage.LABEL_CANTIDAD),
                Ensure.that(ConfirmarCompraPage.BTN_DELETE_LATERAL).isDisplayed(),
                Click.on(ConfirmarCompraPage.BTN_DELETE_LATERAL),
                Scroll.untilVisibleTarget(ConfirmarCompraPage.BTN_FINISH)
                        .direction(ScrollDirection.TO_BOTTOM)
                        .untilMaxAttempts(10),
                Ensure.that(ConfirmarCompraPage.LABEL_PAYMENT_INFORMATION).isDisplayed(),
                Ensure.that(ConfirmarCompraPage.LABEL_SHIPPING_INFORMATION).isDisplayed(),
                Ensure.that(ConfirmarCompraPage.BTN_CANCEL).isDisplayed(),
                Ensure.that(ConfirmarCompraPage.BTN_FINISH).isDisplayed(),
                Click.on(ConfirmarCompraPage.BTN_FINISH)
                );

    }
}