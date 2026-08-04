package co.com.simon.interactions;

import net.serenitybdd.screenplay.Actor;
import net.serenitybdd.screenplay.Interaction;
import net.serenitybdd.screenplay.abilities.BrowseTheWeb;
import net.serenitybdd.screenplay.targets.Target;
import net.thucydides.core.webdriver.WebDriverFacade;
import org.openqa.selenium.Point;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Pause;
import org.openqa.selenium.interactions.PointerInput;
import org.openqa.selenium.interactions.Sequence;
import org.openqa.selenium.remote.RemoteWebDriver;

import java.time.Duration;
import java.util.Collections;

import static net.serenitybdd.screenplay.Tasks.instrumented;

public class Arrastrar implements Interaction {

    private final Target origen;
    private final Target destino;

    public Arrastrar(Target origen, Target destino) {
        this.origen = origen;
        this.destino = destino;
    }

    public static Builder desde(Target origen) {
        return new Builder(origen);
    }

    @Override
    public <T extends Actor> void performAs(T actor) {
        WebElement elementOrigen = origen.resolveFor(actor);
        WebElement elementDestino = destino.resolveFor(actor);
        WebDriver driverFacade = BrowseTheWeb.as(actor).getDriver();
        RemoteWebDriver driver = (RemoteWebDriver) ((WebDriverFacade) driverFacade).getProxiedDriver();
        Point start = getCenter(elementOrigen);
        Point end = getCenter(elementDestino);

        PointerInput finger = new PointerInput(PointerInput.Kind.TOUCH, "finger");
        Sequence sequence = new Sequence(finger, 1);

        sequence.addAction(finger.createPointerMove(Duration.ZERO, PointerInput.Origin.viewport(), start.x, start.y));
        sequence.addAction(finger.createPointerDown(PointerInput.MouseButton.LEFT.asArg()));
        sequence.addAction(new Pause(finger, Duration.ofMillis(600)));
        sequence.addAction(finger.createPointerMove(Duration.ofMillis(1000), PointerInput.Origin.viewport(), end.x, end.y));
        sequence.addAction(finger.createPointerUp(PointerInput.MouseButton.LEFT.asArg()));

        driver.perform(Collections.singletonList(sequence));
    }

    private Point getCenter(WebElement element) {
        int x = element.getLocation().getX() + (element.getSize().getWidth() / 2);
        int y = element.getLocation().getY() + (element.getSize().getHeight() / 2);
        return new Point(x, y);
    }

    public static class Builder {
        private final Target origen;

        public Builder(Target origen) {
            this.origen = origen;
        }

        public Arrastrar hasta(Target destino) {
            return instrumented(Arrastrar.class, origen, destino);
        }
    }
}