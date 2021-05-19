package site.com.utils;

public class LoginUtils {
    public static String getEmailToLogin() {
        return getCloudOpsCredentials().getEmail();
    }
}
