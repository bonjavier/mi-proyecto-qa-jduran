package co.com.simon.interactions.builders;

import co.com.devco.automation.mobile.actions.DragDirection;
import co.com.simon.interactions.DragAndDrop;
import net.serenitybdd.screenplay.Performable;
import net.serenitybdd.screenplay.targets.Target;

import static net.serenitybdd.screenplay.Tasks.instrumented;

public class DragBuilder {
    private Target target;

    public DragBuilder theElement(Target target){
        this.target=target;
        return this;
    }

    public Performable to(DragDirection direction) {
        return instrumented(DragAndDrop.class, target, direction);
    }

}
