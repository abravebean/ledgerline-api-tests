package com.ledgerline.ui.support;

import com.ledgerline.ui.driver.DriverFactory;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.testng.ITestListener;
import org.testng.ITestResult;
import org.testng.Reporter;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * TestNG listener: when a test fails, save a screenshot and the page URL to
 * target/screenshots/ so the CI artifact shows what the browser saw.
 */
public class ScreenshotOnFailure implements ITestListener {

    @Override
    public void onTestFailure(ITestResult result) {
        try {
            var driver = DriverFactory.get();
            Path dir = Path.of("target", "screenshots");
            Files.createDirectories(dir);
            String name = result.getTestClass().getRealClass().getSimpleName() + "." + result.getMethod().getMethodName()
                    + "-" + System.currentTimeMillis();
            Path file = dir.resolve(name.replaceAll("[^\\w.-]", "_") + ".png");
            Files.write(file, ((TakesScreenshot) driver).getScreenshotAs(OutputType.BYTES));
            Reporter.log("Screenshot: " + file.toAbsolutePath() + " (URL " + driver.getCurrentUrl() + ")", true);
        } catch (IllegalStateException | IOException e) {
            Reporter.log("Could not save a screenshot: " + e.getMessage(), true);
        }
    }
}
