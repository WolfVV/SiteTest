package site.com.utils;

import java.io.File;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class DataUtils {

    /**
     * The Log.
     */
    private static Logger log = LogManager.getLogger(DataUtils.class.getName());

    /**
     * The datafile.
     */
    private static final String DATAFILE = System.getProperty("datafile") == null ? "TestData.xlsx"
            : System.getProperty("datafile");

    private static final String TEST_DATA = System.getProperty("user.dir") + File.separator + "resources" +
            File.separator + "TestData.xlsx";

    public static final String DOWNLOAD_FOLDER = System.getProperty("user.dir") + File.separator +
            TestUtils.getProperty("selenium.downloadFileFolder");
    public static final String VIDEOS_FOLDER = System.getProperty("user.dir") + File.separator +
            TestUtils.getProperty("selenium.videoFolder");
    public static final String SCREENSHOT_FOLDER = System.getProperty("user.dir") + File.separator +
            TestUtils.getProperty("selenium.screenshotFolder");
}
