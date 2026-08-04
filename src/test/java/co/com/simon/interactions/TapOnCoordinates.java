package co.com.simon.interactions;

import io.appium.java_client.AppiumDriver;
import net.serenitybdd.screenplay.Actor;
import net.serenitybdd.screenplay.Interaction;
import org.openqa.selenium.Point;
import org.openqa.selenium.interactions.PointerInput;
import org.openqa.selenium.interactions.Sequence;

import java.time.Duration;
import java.util.List;

import static net.thucydides.core.webdriver.ThucydidesWebDriverSupport.getProxiedDriver;

public class TapOnCoordinates implements Interaction {

    private final Point point;

    public TapOnCoordinates(Point point) {
        this.point=point;
    }

    public static TapOnCoordinates onPoint(Point point){
        return new TapOnCoordinates(point);
    }

    @Override
    public <T extends Actor> void performAs(T actor) {
        AppiumDriver driver = getProxiedDriver();
        PointerInput finger = new PointerInput(PointerInput.Kind.TOUCH, "finger");
        Sequence tap = new Sequence(finger,1);
        tap.addAction(finger.createPointerMove(Duration.ofMillis(0),
                PointerInput.Origin.viewport(), point.x, point.y));
        tap.addAction(finger.createPointerDown(0));
        tap.addAction(finger.createPointerUp(0));
        driver.perform(List.of(tap));
    }
}
