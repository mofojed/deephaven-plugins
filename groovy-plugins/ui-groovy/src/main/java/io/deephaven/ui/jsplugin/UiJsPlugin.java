package io.deephaven.ui.jsplugin;

import io.deephaven.plugin.js.JsPlugin;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

/**
 * Serves the bundled {@code @deephaven/js-plugin-ui} JS bundle to the Deephaven web client.
 *
 * <p>The JS bundle is shipped inside this JAR under {@code /io/deephaven/ui/js/dist/}, copied at
 * build time from the Python plugin's existing build artifact. The {@link JsPlugin} SPI wants a
 * filesystem {@link Path}, so on first access we extract the bundle to a temp directory.
 */
public final class UiJsPlugin extends JsPlugin {

    private static final String RESOURCE_ROOT = "/io/deephaven/ui/js/dist";
    private static final String[] BUNDLE_ASSETS = {"index.js", "manifest.json", "package.json"};
    private static final String NAME = "@deephaven/js-plugin-ui";
    private static final String VERSION = "0.1.0-SNAPSHOT";
    private static final String MAIN_FILE = "index.js";

    private Path extractedPath;

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public String version() {
        return VERSION;
    }

    @Override
    public Path main() {
        // Must be relative to path() so the manifest URL stays relative — otherwise the web client
        // requests an absolute filesystem path under /js-plugins/<name>/.
        return Paths.get(MAIN_FILE);
    }

    @Override
    public Path path() {
        if (extractedPath != null) {
            return extractedPath;
        }
        try {
            extractedPath = extractBundle();
            return extractedPath;
        } catch (IOException e) {
            throw new RuntimeException("Failed to extract JS bundle from JAR", e);
        }
    }

    private Path extractBundle() throws IOException {
        Path target = Files.createTempDirectory("deephaven-ui-groovy-js-");
        boolean anyCopied = false;
        for (String asset : BUNDLE_ASSETS) {
            String resourcePath = RESOURCE_ROOT + "/" + asset;
            try (InputStream in = getClass().getResourceAsStream(resourcePath)) {
                if (in == null) {
                    continue;
                }
                Path out = target.resolve(asset);
                Files.copy(in, out, StandardCopyOption.REPLACE_EXISTING);
                anyCopied = true;
            }
        }
        if (!anyCopied) {
            throw new IOException("JS bundle resources not found at " + RESOURCE_ROOT
                    + " (build the Python plugin first or pass -PjsBundleSource=<dir>).");
        }
        return target;
    }
}
