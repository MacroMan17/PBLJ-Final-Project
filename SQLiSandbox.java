import com.sqli.sandbox.ui.SQLiSandboxApp;

/**
 * Root Application Entrypoint wrapper.
 * Preserves backwards compatibility for scripts or IDE run configurations pointing to SQLiSandbox.
 * Delegates execution to {@link com.sqli.sandbox.ui.SQLiSandboxApp}.
 */
public class SQLiSandbox {

    public static void main(String[] args) {
        SQLiSandboxApp.main(args);
    }
}
