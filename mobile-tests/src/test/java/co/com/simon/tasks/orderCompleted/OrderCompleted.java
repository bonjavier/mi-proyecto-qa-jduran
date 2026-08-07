package co.com.simon.tasks.orderCompleted;

import net.serenitybdd.screenplay.Actor;
import net.serenitybdd.screenplay.Task;
import net.serenitybdd.screenplay.actions.Click;
import net.serenitybdd.screenplay.ensure.Ensure;

import static net.serenitybdd.screenplay.Tasks.instrumented;

import co.com.simon.userinterface.comprarealizada.AcceptOrderTask;

public class OrderCompleted implements Task {

    public static OrderCompleted ordenRealizada() {
        return instrumented(OrderCompleted.class);
    }

    @Override
    public <T extends Actor> void performAs(T actor) {
        actor.attemptsTo(
                Ensure.that(AcceptOrderTask.LBL_CHECKOUT_COMPLETE).isDisplayed(),
                Ensure.that(AcceptOrderTask.TITLE_LABEL_THANK_ORDER).isDisplayed(),
                Ensure.that(AcceptOrderTask.SUBTITLE_LABEL_THANK_ORDER).isDisplayed(),
                Ensure.that(AcceptOrderTask.BTN_BACK_HOME).isDisplayed(),
                Click.on(AcceptOrderTask.BTN_BACK_HOME),
                Ensure.that(AcceptOrderTask.BTN_MENU_HOME).isDisplayed(),
                Click.on(AcceptOrderTask.BTN_MENU_HOME),
                Ensure.that(AcceptOrderTask.BTN_ALL_ITEMS).isDisplayed(),
                Ensure.that(AcceptOrderTask.BTN_WEBVIEW).isDisplayed(),
                Ensure.that(AcceptOrderTask.BTN_QR_CODE_SCANNER).isDisplayed(),
                Ensure.that(AcceptOrderTask.BTN_GEO_LOCATION).isDisplayed(),
                Ensure.that(AcceptOrderTask.BTN_DRAWING).isDisplayed(),
                Ensure.that(AcceptOrderTask.BTN_ABOUT).isDisplayed(),
                Ensure.that(AcceptOrderTask.BTN_LOGOUT).isDisplayed(),
                Click.on(AcceptOrderTask.BTN_LOGOUT)
        );
    }
}