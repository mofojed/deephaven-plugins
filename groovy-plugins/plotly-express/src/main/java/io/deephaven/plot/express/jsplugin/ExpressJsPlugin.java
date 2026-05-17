package io.deephaven.plot.express.jsplugin;

import io.deephaven.plugin.js.JsPlugin;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * Serves the bundled {@code @deephaven/js-plugin-plotly-express} JS bundle to the Deephaven web
 * client.
 *
 * <p>Unlike the much simpler ui plugin (which ships a single {@code index.js}), the plotly
 * bundle is a directory tree: top-level entry files, type-declaration files, and a {@code
 * bundle/} subdirectory carrying the ~8MB plotly.js payload + stylesheet. So at extraction time
 * we copy <em>every</em> resource under {@code /io/deephaven/plot/express/js/dist/}, not just a
 * hardcoded short list.
 *
 * <p>The {@link JsPlugin} SPI wants a filesystem {@link Path}, so on first access we extract
 * the bundle to a temp dir. The bundle is shipped inside this JAR.
 */
public final class ExpressJsPlugin extends JsPlugin {

    private static final String RESOURCE_ROOT = "/io/deephaven/plot/express/js/dist";
    private static final String ENTRY_PREFIX = "io/deephaven/plot/express/js/dist/";
    private static final String NAME = "@deephaven/js-plugin-plotly-express";
    private static final String VERSION = "0.1.0-SNAPSHOT";
    // The plotly-express bundle ships a top-level ES-module entry (`index.js`) and a bundled
    // CommonJS entry (`bundle/index.js`). The deephaven web client loads plugins via
    // CommonJS-style require semantics, so we must point main() at the bundled file — the
    // top-level index.js fails with "Cannot use import statement outside a module". This
    // matches the Python plugin's package.json `"main": "dist/bundle/index.js"`.
    private static final String MAIN_FILE = "bundle/index.js";
    private static final String MARKER_FILE = "index.js";

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
        Path target = Files.createTempDirectory("deephaven-plotly-express-js-");
        boolean anyCopied;

        // Locate the JAR (or filesystem directory) holding our resources, then walk every entry
        // under the dist prefix and write it to the temp dir.
        URL marker = getClass().getResource(RESOURCE_ROOT + "/" + MARKER_FILE);
        if (marker == null) {
            throw new IOException("JS bundle resources not found at " + RESOURCE_ROOT
                    + " (build the Python plugin first or pass -PjsBundleSource=<dir>).");
        }
        String proto = marker.getProtocol();
        if ("jar".equals(proto)) {
            anyCopied = extractFromJar(marker, target);
        } else if ("file".equals(proto)) {
            anyCopied = extractFromFilesystem(target);
        } else {
            throw new IOException("Unsupported resource protocol: " + proto);
        }

        if (!anyCopied) {
            throw new IOException("JS bundle extracted but no files were copied (resource enumeration failed).");
        }
        return target;
    }

    private boolean extractFromJar(URL marker, Path target) throws IOException {
        // JAR URL format: jar:file:/path/to/jar.jar!/io/deephaven/plot/express/js/dist/index.js
        String spec = marker.getFile();
        int bang = spec.indexOf('!');
        if (bang < 0) {
            throw new IOException("Malformed jar URL: " + marker);
        }
        String jarPath = spec.substring(0, bang).replaceFirst("^file:", "");

        boolean anyCopied = false;
        try (JarFile jar = new JarFile(jarPath)) {
            Enumeration<JarEntry> entries = jar.entries();
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                String entryName = entry.getName();
                if (!entryName.startsWith(ENTRY_PREFIX) || entry.isDirectory()) {
                    continue;
                }
                String rel = entryName.substring(ENTRY_PREFIX.length());
                Path out = target.resolve(rel);
                Files.createDirectories(out.getParent());
                try (InputStream in = jar.getInputStream(entry)) {
                    Files.copy(in, out, StandardCopyOption.REPLACE_EXISTING);
                }
                anyCopied = true;
            }
        }
        return anyCopied;
    }

    private boolean extractFromFilesystem(Path target) throws IOException {
        // Tests/dev mode: resources live on the filesystem (build/resources/main).
        URL rootUrl = getClass().getResource(RESOURCE_ROOT);
        if (rootUrl == null) {
            return false;
        }
        Path source;
        try {
            source = Paths.get(rootUrl.toURI());
        } catch (Exception e) {
            return false;
        }
        if (!Files.isDirectory(source)) {
            return false;
        }
        boolean[] anyCopied = {false};
        try (var walk = Files.walk(source)) {
            walk.filter(Files::isRegularFile).forEach(p -> {
                try {
                    Path rel = source.relativize(p);
                    Path out = target.resolve(rel.toString());
                    Files.createDirectories(out.getParent());
                    Files.copy(p, out, StandardCopyOption.REPLACE_EXISTING);
                    anyCopied[0] = true;
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });
        }
        return anyCopied[0];
    }
}
