package co.com.simon.runners;

import io.cucumber.junit.CucumberOptions;
import net.serenitybdd.cucumber.CucumberWithSerenity;
import org.junit.runner.RunWith;

@RunWith(CucumberWithSerenity.class)
@CucumberOptions(
        plugin = {"pretty"},
        features = "src/test/resources/features",
        glue = "co.com.simon.stepdefinitions",
        snippets = CucumberOptions.SnippetType.CAMELCASE
)
public class SwaglabsRunner {
}
