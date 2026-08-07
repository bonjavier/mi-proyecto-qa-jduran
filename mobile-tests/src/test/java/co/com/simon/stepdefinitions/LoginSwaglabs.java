package co.com.simon.stepdefinitions;

import io.cucumber.datatable.DataTable;
import io.cucumber.java.Before;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import net.serenitybdd.screenplay.actors.OnStage;
import net.serenitybdd.screenplay.actors.OnlineCast;
import net.serenitybdd.screenplay.ensure.Ensure;

import static net.serenitybdd.screenplay.GivenWhenThen.seeThat;

import co.com.simon.questions.OrderCompletedQuestion;
import co.com.simon.tasks.acceptOrder.AcceptOrderTask;
import co.com.simon.tasks.addToCard.AddToCardItemTask;
import co.com.simon.tasks.checkout.CheckoutTask;
import co.com.simon.tasks.home.HomeTask;
import co.com.simon.tasks.login.LoginTask;
import co.com.simon.tasks.orderCompleted.OrderCompleted;
import co.com.simon.tasks.withinCard.WithinCardTask;
import co.com.simon.userinterface.login.LoginPage;

public class LoginSwaglabs {
    @Before
    public void prepareStage() {
        OnStage.setTheStage(new OnlineCast());
    }

    @Given("el {string} abre la app de swagLabs")
    public void elEstaEnLaPaginaDeLoginEnSwagLabs(String actorName) {
        OnStage.theActorCalled(actorName).attemptsTo(
        );
    }

    @When("el usuario ingresa los datos de {string} y {string}")
    public void elUsuarioIngresaLosDatosDeStandardUserYSecretSauce(String username, String password) {
        OnStage.theActorInTheSpotlight().attemptsTo(
                LoginTask.loginConCredenciales(username, password)
        );
    }

    @And("visualiza y agrega los productos al carrito")
    public void visualizaYAgregaLosProductosAlCarrito() {
        OnStage.theActorInTheSpotlight().attemptsTo(
                HomeTask.validarInterfazProducts(),
                AddToCardItemTask.addToCardItemTaskCarrito()
        );

    }

    @And("procede al checkout")
    public void procedeAlCheckout() {
        OnStage.theActorInTheSpotlight().attemptsTo(
                WithinCardTask.ValidateProductsAddCard()
        );
    }

    @And("ingresa sus datos de envío y finaliza la compra:")
    public void ingresaSusDatosDeEnvioYFinalizaLaCompra(DataTable dataTable) {
        String nombre = dataTable.asMaps().get(0).get("nombre");
        String apellido = dataTable.asMaps().get(0).get("apellido");
        String codigoPostal = dataTable.asMaps().get(0).get("codigoPostal");
        OnStage.theActorInTheSpotlight().attemptsTo(
                CheckoutTask.validarDatosPersonales(nombre, apellido, codigoPostal)
        );
    }

    @And("confirma la compra")
    public void confirmaLaCompra() {
        OnStage.theActorInTheSpotlight().attemptsTo(
                AcceptOrderTask.acceptOrder()
        );
    }

    @Then("visualiza el mensaje de confirmación {string}")
    public void visualizaElMensajeDeConfirmacion(String mensajeEsperado) {
        OnStage.theActorInTheSpotlight().should(
                seeThat(OrderCompletedQuestion.titleOrderIsVisible(mensajeEsperado))
        );

        OnStage.theActorInTheSpotlight().attemptsTo(
                OrderCompleted.ordenRealizada()
        );
    }

    @Then("visualiza el mensaje de error de login")
    public void visualizaElMensajeDeErrorDeLogin() {
        OnStage.theActorInTheSpotlight().attemptsTo(
                Ensure.that(LoginPage.ERROR_MESSAGE).isDisplayed()
        );
    }
}