package co.com.simon.tasks.withinCard;

import net.serenitybdd.screenplay.Actor;
import net.serenitybdd.screenplay.Task;
import net.serenitybdd.screenplay.actions.Click;
import net.serenitybdd.screenplay.ensure.Ensure;

import static net.serenitybdd.screenplay.Tasks.instrumented;

import co.com.simon.interactions.Arrastrar;
import co.com.simon.interactions.Scroll;
import co.com.simon.models.ScrollDirection;
import co.com.simon.userinterface.carrito.CarritoPage;

public class WithinCardTask implements Task {

    public static WithinCardTask ValidateProductsAddCard() {
        return instrumented(WithinCardTask.class);
    }

    @Override
    public <T extends Actor> void performAs(T actor) {
        actor.attemptsTo(
                Ensure.that(CarritoPage.BTN_CARRITO).isDisplayed(),
                Click.on(CarritoPage.BTN_CARRITO),
                Arrastrar.desde(CarritoPage.BTN_ITEM)
                        .hasta(CarritoPage.LABEL_CANTIDAD),
                Ensure.that(CarritoPage.BTN_DELETE_LATERAL).isDisplayed(),
                Click.on(CarritoPage.BTN_DELETE_LATERAL),
                Ensure.that(CarritoPage.BTN_REMOVE).isDisplayed(),
                Click.on(CarritoPage.BTN_REMOVE),
                Scroll.untilVisibleTarget(CarritoPage.BTN_CHECKOUT)
                        .direction(ScrollDirection.TO_BOTTOM)
                        .untilMaxAttempts(10),
                Ensure.that(CarritoPage.BTN_CHECKOUT).isDisplayed(),
                Ensure.that(CarritoPage.BTN_CONTINUE_SHOPPING).isDisplayed(),
                Click.on(CarritoPage.BTN_CONTINUE_SHOPPING),
                Ensure.that(CarritoPage.BTN_CARRITO).isDisplayed(),
                Click.on(CarritoPage.BTN_CARRITO),
                Scroll.untilVisibleTarget(CarritoPage.BTN_CHECKOUT)
                        .direction(ScrollDirection.TO_BOTTOM)
                        .untilMaxAttempts(10),
                Ensure.that(CarritoPage.BTN_CHECKOUT).isDisplayed(),
                Click.on(CarritoPage.BTN_CHECKOUT)
        );

    }
}
