package org.esa.s3tbx;

import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductNode;
import org.esa.beam.util.SystemUtils;
import org.esa.beam.util.logging.BeamLogManager;
import org.esa.beam.visat.VisatApp;

import javax.swing.KeyStroke;
import java.awt.Desktop;
import java.awt.KeyboardFocusManager;
import java.awt.event.KeyEvent;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;
import java.util.logging.Level;

/**
 * Adds contextual search support to Snap.
 * <p>
 * A contextual search is performed if a product node is selected and CTRL+F1 is pressed.
 *
 * @author Norman Fomferra
 */
public class ContextSearch {

    private static final String CONFIG_FILENAME = "context-search.properties";
    private static final String DEFAULT_KEY_STROKE = "control F1";

    private final VisatApp visatApp;
    private final KeyStroke keyStroke;
    private final Properties config;

    public static void install(VisatApp visatApp) {
        new ContextSearch(visatApp);
    }

    private ContextSearch(VisatApp visatApp) {
        this.visatApp = visatApp;
        this.config = new Properties();

        try {
            loadConfig();
        } catch (IOException e) {
            BeamLogManager.getSystemLogger().log(Level.SEVERE, "Failed to load context search configuration", e);
        }

        KeyStroke keyStroke = KeyStroke.getKeyStroke(config.getProperty("key", DEFAULT_KEY_STROKE));
        if (keyStroke != null) {
            this.keyStroke = keyStroke;
        } else {
            this.keyStroke = KeyStroke.getKeyStroke(DEFAULT_KEY_STROKE);
        }

        KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(this::processKeyEvent);
    }

    private boolean processKeyEvent(KeyEvent e) {
        int modifiers = keyStroke.getModifiers();
        int keyCode = keyStroke.getKeyCode();
        boolean activated = (modifiers == e.getModifiers() || (modifiers & e.getModifiers()) != 0)
                            && e.getKeyCode() == keyCode;
        if (activated) {
            ProductNode node = visatApp.getSelectedProductNode();
            return node != null && searchFor(node);
        }
        return false;
    }

    private boolean searchFor(ProductNode node) {

        String searchString = config.getProperty("search", "http://www.google.com/search?q=");
        String queryString = getQueryString(node);

        try {
            String search = searchString + URLEncoder.encode(queryString, "UTF-8");
            URI uri = URI.create(search);
            Desktop.getDesktop().browse(uri);
        } catch (IOException e) {
            BeamLogManager.getSystemLogger().log(Level.WARNING, "Failed to perform context search");
        }

        return true;
    }

    private String getQueryString(ProductNode node) {
        String contextTerms = config.getProperty("query", "");

        Product product = node.getProduct();
        if (product != null) {
            String productType = product.getProductType();
            if (productType != null) {
                String productTypeId = productType.replace(" ", "_");
                contextTerms = config.getProperty(String.format("productTypes.%s.query", productTypeId), contextTerms);
            }
        }

        String nodeName = node.getName();
        String nodeNameTerms = nodeName.replace(".", " OR ").replace("_", " OR ").replace("-", " OR ");

        return nodeNameTerms + " " + contextTerms;
    }

    private void loadConfig() throws IOException {
        Path file = getConfigPath();
        config.load(new FileReader(file.toFile()));
    }

    private Path getConfigPath() throws IOException {
        FileSystem fs = FileSystems.getDefault();
        Path dir = fs.getPath(SystemUtils.getApplicationDataDir().getPath(), "snap-ui", "auxdata");
        if (Files.notExists(dir)) {
            Files.createDirectories(dir);
        }

        Path file = fs.getPath(dir.toString(), CONFIG_FILENAME);
        if (Files.notExists(file)) {
            InputStream resourceAsStream = getClass().getResourceAsStream(CONFIG_FILENAME);
            Files.copy(resourceAsStream, file);
        }
        return file;
    }

}
