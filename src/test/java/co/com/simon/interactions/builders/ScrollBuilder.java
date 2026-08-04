package co.com.simon.interactions.builders;

import net.serenitybdd.screenplay.Performable;
import net.serenitybdd.screenplay.targets.Target;

import static net.serenitybdd.screenplay.Tasks.instrumented;

import co.com.simon.interactions.Scroll;
import co.com.simon.models.ScrollDirection;

public class ScrollBuilder {

    private Target target;
    private ScrollDirection direction;

    public ScrollBuilder untilVisibleTarget(Target target) {
        this.target=target;
        return this;
    }

    public ScrollBuilder direction(ScrollDirection direction){
        this.direction = direction;
        return this;
    }

    public Performable untilMaxAttempts(int attempts){
        return  instrumented (Scroll.class, target, direction, attempts);
    }

}
