package co.com.simon.interactions;

import io.appium.java_client.android.AndroidDriver;
import net.serenitybdd.screenplay.Actor;
import net.serenitybdd.screenplay.Interaction;
import net.serenitybdd.screenplay.abilities.BrowseTheWeb;
import net.serenitybdd.screenplay.targets.Target;
import org.openqa.selenium.interactions.PointerInput;
import org.openqa.selenium.interactions.Sequence;

import co.com.simon.interactions.builders.ScrollBuilder;
import co.com.simon.models.ScrollDirection;
import net.thucydides.core.webdriver.WebDriverFacade;
import java.time.Duration;
import java.util.List;

public class Scroll implements Interaction {
    private final Target target;
    private final int attempts;
    private final ScrollDirection direction;

    public Scroll(Target target, ScrollDirection direction, int attempts) {
        this.target = target;
        this.direction = direction;
        this.attempts = attempts;
    }

    public static ScrollBuilder untilVisibleTarget(Target target){
        return new ScrollBuilder().untilVisibleTarget(target);
    }

    public static ScrollBuilder direction(ScrollDirection direction){
        return new ScrollBuilder().direction(direction);
    }

    @Override
    public <T extends Actor> void performAs(T actor) {

        AndroidDriver driver = (AndroidDriver) ((WebDriverFacade) BrowseTheWeb.as(actor).getDriver()).getProxiedDriver();
        var dimension = driver.manage().window().getSize();
        int centerX = dimension.width / 2;
        int startY = dimension.height / 2;
        int endY = (direction.equals(ScrollDirection.TO_BOTTOM)) ? (dimension.height / 4) : (dimension.height - (dimension.height / 4));

        var currentAttempt = 0;
         do {
            PointerInput finger = new PointerInput(PointerInput.Kind.TOUCH,"finger");
            Sequence scroll = new Sequence(finger,1);
            scroll.addAction(finger.createPointerMove(Duration.ofSeconds(0),PointerInput.Origin.viewport(),centerX,startY));
            scroll.addAction(finger.createPointerDown(0));
            scroll.addAction(finger.createPointerMove(Duration.ofMillis(700),
                    PointerInput.Origin.viewport(),centerX, endY));
            scroll.addAction(finger.createPointerUp(0));

            driver.perform(List.of(scroll));

            currentAttempt++;
        }while (!target.resolveFor(actor).isVisible() && !target.resolveFor(actor).isPresent() && currentAttempt <= attempts);

    }
}
