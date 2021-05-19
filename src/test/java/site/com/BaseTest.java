package site.com;


import org.openqa.selenium.chrome.ChromeDriver;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.testng.annotations.Listeners;
import site.com.utils.DataUtils;
import site.com.utils.LoginUtils;

@Listeners({TestLogListener.class, SkipListener.class, SaveParametersListener.class, DecideUserTypeListener.class})
public abstract class BaseTest {

    /**
     * The Log.
     */
    protected static Logger Log = LogManager.getLogger(BaseTest.class.getName());

    protected static final String DOWNLOAD_FOLDER = DataUtils.DOWNLOAD_FOLDER;
    protected static final String VIDEO_FOLDER = DataUtils.VIDEOS_FOLDER;
    protected static final String SCREENSHOT_FOLDER = DataUtils.SCREENSHOT_FOLDER;
    protected final Env env = Env.current();

    public String email() {
        return LoginUtils.getEmailToLogin();
    }

    /**
     * The driver.
     */
    protected ThreadLocal<WebDriver> driver = new ThreadLocal<>();
    private static ThreadLocal<String> currentEmail = new ThreadLocal<>();

    public static void setCurrentEmail(String email) {
        currentEmail.set(email);
    }

    public static String getCurrentEmail() {
        return currentEmail.get();
    }

    /**
     * The pages.
     */
    private ThreadLocal<PageCollection> pages = new ThreadLocal<>();


    protected PageCollection pages() {
        return pages.get();
    }

    /**
     * The web actions.
     */
    private ThreadLocal<AfterTestActions> afterTestActions = new ThreadLocal<>();

    protected AfterTestActions afterTestActions() {
        return afterTestActions.get();
    }

    /**
     * The actions.
     */
    protected AssertionActions actions;

    /**
     * Creates the chrome driver.
     */
    private ChromeDriver createChromeDriver() {
        try {
            Log.info("Found Chrome as Browser");
            Map<String, Object> prefs = new HashMap<>();
            prefs.put("profile.default_content_setting_values.notifications", 2);
            prefs.put("download.default_directory", DOWNLOAD_FOLDER);

            ChromeOptions options = new ChromeOptions();
            options.addArguments("enable-automation");
            options.addArguments("--disable-extensions");
            options.addArguments("--dns-prefetch-disable");
            options.setPageLoadStrategy(PageLoadStrategy.NORMAL);
            options.addArguments("--use-fake-ui-for-media-stream");
            options.addArguments("--lang=eng");
            prefs.put("profile.default_content_setting_values.popups", 1);
            options.setExperimentalOption("prefs", prefs);
            options.addArguments("--no-sandbox");//***linux
            options.addArguments("--disable-dev-shm-usage");//***linux
            options.addArguments("--ignore-certificate-errors");
            options.addArguments("--disable-gpu");

            WebDriverManager chromeDriverManager = WebDriverManager.chromedriver();
            Supplier<ChromeDriver> setupDriver = () -> {
                //setup driver
                chromeDriverManager.setup();
                ChromeDriver chromeDriver = new ChromeDriver(options);
                setDimension(chromeDriver);
                LogUtils.log(this.getClass(), Level.INFO, "Launch chrome driver - {}#{}. (version: {})",
                        Thread.currentThread().getId(), chromeDriver.getSessionId(), chromeDriverManager.getDownloadedDriverVersion());
                return chromeDriver;
            };
            ChromeDriver driver;
            try {
                driver = setupDriver.get();
            } catch (Exception e) {
                LogUtils.logEverywhere(this.getClass(), Level.WARN, "Chromedriver version changes, clear preference");
                chromeDriverManager.clearResolutionCache();
                driver = setupDriver.get();
            }
            return driver;
        } catch (Exception e) {
            LogUtils.logEverywhere(this.getClass(), Level.ERROR, "UNABLE TO CREATE CHROMEDRIVER");
            throw new RuntimeException(e);
        }
    }

    private void setDimension(WebDriver driver) {
        if (TestUtils.isDebug()) {
            driver.manage().window().setSize(new Dimension(1024, 653));
        } else {
            driver.manage().window().maximize();
        }
    }

    /**
     * Initialize pages.
     */
    private void initializeMainReferences() {
        PageCollection pageCollection = new PageCollection(driver);
        pages.set(pageCollection);
        afterTestActions.set(new AfterTestActions());
        actions = new AssertionActions();
        ReferencesProvider.Reference reference =
                new ReferencesProvider.Reference(driver, pageCollection, actions);
        //add drivers to the pool for the referencing withing non-constructed structures
        ReferencesProvider.add(Thread.currentThread().getId(), reference);
    }

    protected WebDriver driverSetup() {
        String browser = TestUtils.getProperty("selenium.browser");

        switch (browser) {
            case "chrome":
                driver.set(createChromeDriver());
                break;
            default:
                Log.info("browser not supported");
                break;
        }
        //initialize driver references for static access
        initializeMainReferences();

        return driver.get();
    }

    protected void driverTeardown() {
        //clean soft assertions after each method
        actions.cleanSoftAssert();
        Log.info("Closing Webdriver threads");
        //clear driver
        if (TestUtils.getProperty("selenium.closeBrowser").equalsIgnoreCase("true")) {
            WebDriver webDriver = driver.get();
            SessionId sessionId = ReferencesProvider.getSessionId(webDriver);
            webDriver.quit();
            Log.info("Driver - '{}' is closed", sessionId);
        }
    }

    @BeforeSuite(alwaysRun = true)
    public void videoConfig() {
        System.setProperty("video.folder", VIDEO_FOLDER);
        System.setProperty("video.mode", "ANNOTATED");
    }

    @BeforeSuite(alwaysRun = true)
    public void setupAll() {
        if (!TestUtils.isDebug()) {
            TestUtils.deleteFiles(VIDEO_FOLDER);
            TestUtils.deleteFiles(DOWNLOAD_FOLDER);
            TestUtils.deleteFiles(SCREENSHOT_FOLDER);
        }
        AfterTestActions.clearLogFile();
    }

    /**
     * Driver setup.
     */
    @BeforeMethod(alwaysRun = true)
    public void setup() {
        driverSetup();
    }

    /**
     * Teardown.
     *
     * @param result the result
     */
    @AfterMethod(alwaysRun = true)
    public void teardown(ITestResult result) {
        // if test case failed then capture screenshot and console logs
        int status = result.getStatus();
        if (status == ITestResult.FAILURE || (status == ITestResult.SKIP && ReferencesProvider.isDriverActive(driver))) {
            afterTestActions().captureScreenshot(result);
            afterTestActions().printConsoleErrors();
        }
        driverTeardown();
    }

    @AfterSuite(alwaysRun = true)
    public void teardownAll() {
        if (!TestUtils.isDebug()) {
            TestUtils.deleteFiles(DOWNLOAD_FOLDER);
        }
    }

