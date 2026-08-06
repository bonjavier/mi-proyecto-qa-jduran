package co.com.simon.tasks.addToCard;

import net.serenitybdd.screenplay.Actor;
import net.serenitybdd.screenplay.Task;
import net.serenitybdd.screenplay.actions.Click;

import static net.serenitybdd.screenplay.Tasks.instrumented;

import co.com.simon.interactions.Arrastrar;
import co.com.simon.interactions.Scroll;
import co.com.simon.models.ScrollDirection;
import co.com.simon.userinterface.addToCart.AddToCardItemsPage;

public class AddToCardItemTask implements Task {

    public static AddToCardItemTask addToCardItemTaskCarrito() {
        return instrumented(AddToCardItemTask.class);
    }

    @Override
    public <T extends Actor> void performAs(T actor) {
        actor.attemptsTo(
                Scroll.untilVisibleTarget(AddToCardItemsPage.BTN_CARRITO)
                        .direction(ScrollDirection.TO_TOP)
                        .untilMaxAttempts(10),
                Scroll.untilVisibleTarget(AddToCardItemsPage.BTN_CARRITO)
                        .direction(ScrollDirection.TO_TOP)
                        .untilMaxAttempts(10),
                Click.on(AddToCardItemsPage.BTN_ADDTOCARDITEM.of(String.valueOf(1))),
                Click.on(AddToCardItemsPage.BTN_ADDTOCARDITEM.of(String.valueOf(1))),
                Click.on(AddToCardItemsPage.BTN_ADDTOCARDITEM.of(String.valueOf(1)))
        );
        actor.attemptsTo(
                Arrastrar.desde(AddToCardItemsPage.BTN_ARRASTRAR.of(String.valueOf(1)))
                        .hasta(AddToCardItemsPage.ZONA_DE_ARRASTRE)
        );

    }
}
