package co.com.simon.tasks.login;


import net.serenitybdd.screenplay.Actor;
import net.serenitybdd.screenplay.Task;
import net.serenitybdd.screenplay.actions.Click;
import net.serenitybdd.screenplay.actions.Enter;
import static net.serenitybdd.screenplay.Tasks.instrumented;

import co.com.simon.userinterface.login.LoginPage;

public class LoginTask implements Task {

    private final String username;
    private final String password;

    public LoginTask(String username, String password) {
        this.username = username;
        this.password = password;
    }

    public static LoginTask loginConCredenciales(String username, String password) {
        return instrumented(LoginTask.class, username, password);
    }

    @Override
    public <T extends Actor> void performAs(T actor) {
        actor.attemptsTo(
                Click.on(LoginPage.INPUT_USERNAME),
                Enter.theValue(username).into(LoginPage.INPUT_USERNAME),
                Click.on(LoginPage.INPUT_PASSWORD),
                Enter.theValue(password).into(LoginPage.INPUT_PASSWORD),
                Click.on(LoginPage.BTN_LOGIN)
        );
    }
}