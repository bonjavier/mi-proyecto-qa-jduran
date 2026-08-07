package co.com.simon.tasks.home;


import net.serenitybdd.screenplay.Actor;
import net.serenitybdd.screenplay.Task;
import net.serenitybdd.screenplay.actions.Click;
import net.serenitybdd.screenplay.ensure.Ensure;

import static net.serenitybdd.screenplay.Tasks.instrumented;

import co.com.simon.interactions.Scroll;
import co.com.simon.models.ScrollDirection;
import co.com.simon.userinterface.home.HomePage;

public class HomeTask implements Task {

    public static HomeTask validarInterfazProducts() {
        return instrumented(HomeTask.class);
    }


    @Override
    public <T extends Actor> void performAs(T actor) {
        actor.attemptsTo(

                Ensure.that(HomePage.BTN_MENU_HAMBURGUESA).isDisplayed(),
                Ensure.that(HomePage.BTN_CARRITO).isDisplayed(),
                Ensure.that(HomePage.BTN_TOGGLE).isDisplayed(),
                Ensure.that(HomePage.BTN_FILTRO).isDisplayed(),
                Click.on(HomePage.BTN_FILTRO),
                Ensure.that(HomePage.BTN_ORDER_BY_A_TO_Z).isDisplayed(),
                Ensure.that(HomePage.BTN_ORDER_BY_Z_TO_A).isDisplayed(),
                Ensure.that(HomePage.BTN_ORDER_BY_PRICE_LOW_TO_HIGH).isDisplayed(),
                Ensure.that(HomePage.BTN_ORDER_BY_PRICE_HIGH_TO_LOW).isDisplayed(),
                Ensure.that(HomePage.BTN_CANCEL).isDisplayed(),
                Click.on(HomePage.BTN_CANCEL),

                Click.on(HomePage.BTN_FILTRO),
                Click.on(HomePage.BTN_ORDER_BY_A_TO_Z),

                Click.on(HomePage.BTN_FILTRO),
                Click.on(HomePage.BTN_ORDER_BY_Z_TO_A),

                Click.on(HomePage.BTN_FILTRO),
                Click.on(HomePage.BTN_ORDER_BY_PRICE_LOW_TO_HIGH),

                Click.on(HomePage.BTN_FILTRO),
                Click.on(HomePage.BTN_ORDER_BY_PRICE_HIGH_TO_LOW),

                Click.on(HomePage.BTN_TOGGLE),
                Scroll.untilVisibleTarget(HomePage.TEXT_TERMS_OF_SERVICE)
                        .direction(ScrollDirection.TO_BOTTOM)
                        .untilMaxAttempts(10)
        );
    }
}