package org.esa.s3tbx;

import com.bc.ceres.core.Assert;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductNode;
import org.esa.beam.util.SystemUtils;
import org.esa.beam.util.logging.BeamLogManager;
import org.esa.beam.visat.VisatApp;

import javax.swing.KeyStroke;
import java.awt.Desktop;
import java.awt.KeyboardFocusManager;
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

    private static final String DEFAULT_KEY = "control F1";
    private static final String DEFAULT_SEARCH = "http://www.google.com/search?q=";
    private static final String DEFAULT_QUERY = "Sentinel Toolbox";
    private static final String CONFIG_FILENAME = "context-search.properties";

    private final Properties config;

    public static void install(VisatApp visatApp) {
        new ContextSearch(visatApp);
    }

    public void searchForNode(ProductNode node) {

        String searchString = getSearch();
        String queryString = getQueryString(node);

        try {
            String search = searchString + URLEncoder.encode(queryString, "UTF-8");
            URI uri = URI.create(search);
            Desktop.getDesktop().browse(uri);
        } catch (IOException e) {
            BeamLogManager.getSystemLogger().log(Level.WARNING, "Failed to perform context search");
        }
    }

    private ContextSearch(final VisatApp visatApp) {
        Assert.notNull(visatApp, "visatApp");

        this.config = new Properties();

        try {
            loadConfig();
        } catch (IOException e) {
            BeamLogManager.getSystemLogger().log(Level.SEVERE, "Failed to load context search configuration", e);
        }

        final KeyStroke keyStroke;
        KeyStroke ks = KeyStroke.getKeyStroke(getKey());
        if (ks == null) {
            keyStroke = KeyStroke.getKeyStroke(DEFAULT_KEY);
        } else {
            keyStroke = ks;
        }
        Assert.notNull(keyStroke, "keyStroke");

        ContextSearchAction command = (ContextSearchAction) visatApp.getCommandManager().getCommand(ContextSearchAction.ID);
        command.setContextSearch(this);
        command.setAccelerator(keyStroke);

        KeyboardFocusManager kfm = KeyboardFocusManager.getCurrentKeyboardFocusManager();
        kfm.addKeyEventDispatcher(e -> {
            if (keyStroke.getModifiers() == e.getModifiers() || (keyStroke.getModifiers() & e.getModifiers()) != 0
                                                                && keyStroke.getKeyCode() == e.getKeyCode()) {
                ProductNode productNode = visatApp.getSelectedProductNode();
                if (productNode != null) {
                    searchForNode(productNode);
                    return true;
                }
            }
            return false;
        });
    }

    private String getKey() {
        return config.getProperty("key", DEFAULT_KEY);
    }

    private String getSearch() {
        return config.getProperty("search", DEFAULT_SEARCH);
    }

    private String getQuery() {
        return config.getProperty("query", DEFAULT_QUERY);
    }

    private String getQuery(String productType, String def) {
        return config.getProperty(String.format("products.%s.query", productType.replace(" ", "_")), def);
    }

    private String getQueryString(ProductNode node) {
        String contextTerms = getQuery();

        Product product = node.getProduct();
        if (product != null) {
            String productType = product.getProductType();
            if (productType != null) {
                contextTerms = getQuery(productType, contextTerms);
            }
        }

        String nodeName = node.getName();
        String[] nodeNameSplits = nodeName.split("[\\.\\_\\ \\-]");

        StringBuilder nodeNameTerms = new StringBuilder();
        for (String nodeNameSplit : nodeNameSplits) {
            if (!nodeNameSplit.isEmpty() && Character.isAlphabetic(nodeNameSplit.charAt(0))) {
                if (nodeNameTerms.length() > 0) {
                    nodeNameTerms.append(" OR ");
                }
                nodeNameTerms.append(nodeNameSplit);
            }
        }

        return contextTerms + " " + nodeNameTerms;
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
