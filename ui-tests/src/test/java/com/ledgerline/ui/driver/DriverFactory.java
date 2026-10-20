package com.ledgerline.ui.driver;

import com.ledgerline.ui.config.Config;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.edge.EdgeDriver;
import org.openqa.selenium.edge.EdgeOptions;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxOptions;

/**
 * One WebDriver per thread. TestNG runs test methods in parallel (testng.xml), and a
 * WebDriver must never be shared between threads, so each thread keeps its own here.
 * Selenium Manager (built into Selenium 4) finds or downloads the matching driver binary.
 */
public final class DriverFactory {

    private static final ThreadLocal<WebDriver> DRIVER = new ThreadLocal<>();

    private DriverFactory() {
    }

    public static WebDriver start() {
        WebDriver driver = switch (Config.browser()) {
            case "firefox" -> {
                FirefoxOptions options = new FirefoxOptions();
                if (Config.headless()) {
                    options.addArguments("-headless");
                }
                yield new FirefoxDriver(options);
            }
            case "edge" -> {
                EdgeOptions options = new EdgeOptions();
                if (Config.headless()) {
                    options.addArguments("--headless=new");
                }
                yield new EdgeDriver(options);
            }
            default -> {
                ChromeOptions options = new ChromeOptions();
                if (Config.headless()) {
                    options.addArguments("--headless=new");
                }
                options.addArguments("--disable-dev-shm-usage", "--no-sandbox");
                yield new ChromeDriver(options);
            }
        };
        driver.manage().window().setSize(new Dimension(1280, 900));
        DRIVER.set(driver);
        return driver;
    }

    public static WebDriver get() {
        WebDriver driver = DRIVER.get();
        if (driver == null) {
            throw new IllegalStateException("No WebDriver for this thread. Was start() called in @BeforeMethod?");
        }
        return driver;
    }

    public static void quit() {
        WebDriver driver = DRIVER.get();
        if (driver != null) {
            driver.quit();
            DRIVER.remove();
        }
    }
}
