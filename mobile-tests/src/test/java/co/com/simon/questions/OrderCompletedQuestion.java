package co.com.simon.questions;

import net.serenitybdd.screenplay.Actor;
import net.serenitybdd.screenplay.Question;
import net.serenitybdd.screenplay.waits.WaitUntil;
import static net.serenitybdd.screenplay.matchers.WebElementStateMatchers.containsText;

import co.com.simon.userinterface.comprarealizada.AcceptOrderTask;

public class OrderCompletedQuestion implements Question<Boolean> {
    public String titleOrderFinish;
    public OrderCompletedQuestion(String titleOrderFinish) {
        this.titleOrderFinish = titleOrderFinish;
    }

    public static Question <Boolean> titleOrderIsVisible  (String titleOrderFinish){
        return new OrderCompletedQuestion(titleOrderFinish);
    }

    @Override
    public Boolean answeredBy(Actor actor) {
        actor.attemptsTo(
                WaitUntil.the(AcceptOrderTask.TITLE_LABEL_THANK_ORDER, containsText(titleOrderFinish)).forNoMoreThan(20).seconds()
        );
        return true;
    }
}
