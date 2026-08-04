package co.com.simon.tasks.checkout;

import net.serenitybdd.screenplay.Actor;
import net.serenitybdd.screenplay.Task;
import net.serenitybdd.screenplay.actions.Click;
import net.serenitybdd.screenplay.actions.Enter;
import net.serenitybdd.screenplay.ensure.Ensure;

import static net.serenitybdd.screenplay.Tasks.instrumented;

import co.com.simon.userinterface.carrito.CarritoPage;
import co.com.simon.userinterface.checkout.CheckoutPage;

public class CheckoutTask implements Task {

    private final String firstname;
    private final String lastname;
    private final String postalcode;

    public CheckoutTask(String firstname, String lastname, String postalcode) {
        this.firstname = firstname;
        this.lastname = lastname;
        this.postalcode = postalcode;
    }

    public static CheckoutTask validarDatosPersonales(String firstname, String lastname, String postalcode) {
        return instrumented(CheckoutTask.class, firstname, lastname, postalcode);
    }
    @Override
    public <T extends Actor> void performAs(T actor) {
        actor.attemptsTo(
                Ensure.that(CarritoPage.BTN_CARRITO).isDisplayed(),
                Click.on(CheckoutPage.INPUT_FIRST_NAME),
                Enter.theValue(firstname).into(CheckoutPage.INPUT_FIRST_NAME),

                Click.on(CheckoutPage.INPUT_LAST_NAME),
                Enter.theValue(lastname).into(CheckoutPage.INPUT_LAST_NAME),

                Click.on(CheckoutPage.INPUT_POSTAL_CODE),
                Enter.theValue(postalcode).into(CheckoutPage.INPUT_POSTAL_CODE),
                Ensure.that(CheckoutPage.BTN_CANCEL).isDisplayed(),
                Ensure.that(CheckoutPage.BTN_CONTINUE).isDisplayed(),
                Click.on(CheckoutPage.BTN_CONTINUE)
        );
    }
}
