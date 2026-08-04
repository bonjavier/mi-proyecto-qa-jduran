package co.com.simon.interactions;

import io.appium.java_client.android.AndroidDriver;
import net.serenitybdd.screenplay.Actor;
import net.serenitybdd.screenplay.Interaction;

import static net.thucydides.core.webdriver.ThucydidesWebDriverSupport.getProxiedDriver;

public class HideKeyboard implements Interaction {

    public static  HideKeyboard perform(){
        return new HideKeyboard();
    }

    @Override
    public <T extends Actor> void performAs(T actor) {
        AndroidDriver driver = getProxiedDriver();
        driver.hideKeyboard();
    }
}
