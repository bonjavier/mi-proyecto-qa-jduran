package co.com.simon.interactions;

import net.serenitybdd.screenplay.Actor;
import net.serenitybdd.screenplay.Interaction;
import net.serenitybdd.screenplay.abilities.BrowseTheWeb;
import net.serenitybdd.screenplay.targets.Target;
import org.openqa.selenium.Point;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Pause;
import org.openqa.selenium.interactions.PointerInput;
import org.openqa.selenium.interactions.Sequence;
import org.openqa.selenium.remote.RemoteWebDriver;

import java.time.Duration;
import java.util.Collections;

import static net.serenitybdd.screenplay.Tasks.instrumented;

public class DragAndDrop implements Interaction {

    private final Target origin;
    private final Target destination;

    public DragAndDrop(Target origin, Target destination) {
        this.origin = origin;
        this.destination = destination;
    }

    public static DragAndDropBuilder from(Target origin) {
        return new DragAndDropBuilder(origin);
    }

    @Override
    public <T extends Actor> void performAs(T actor) {
        WebElement originElement = origin.resolveFor(actor);
        WebElement destinationElement = destination.resolveFor(actor);

        RemoteWebDriver driver = (RemoteWebDriver) BrowseTheWeb.as(actor).getDriver();

        Point start = getCenter(originElement);
        Point end = getCenter(destinationElement);

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

    public static class DragAndDropBuilder {
        private final Target origin;

        public DragAndDropBuilder(Target origin) {
            this.origin = origin;
        }

        public DragAndDrop to(Target destination) {
            return instrumented(DragAndDrop.class, origin, destination);
        }
    }
}