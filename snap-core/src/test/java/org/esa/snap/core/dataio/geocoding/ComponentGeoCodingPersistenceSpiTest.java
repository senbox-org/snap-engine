package org.esa.snap.core.dataio.geocoding;

import org.esa.snap.core.dataio.persistence.JdomLanguageSupport;
import org.esa.snap.core.dataio.persistence.JsonLanguageSupport;
import org.esa.snap.core.dataio.persistence.PersistenceConverter;
import org.jdom2.Element;
import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.esa.snap.core.dataio.geocoding.ComponentGeoCodingPersistenceConverter.NAME_COMPONENT_GEO_CODING;
import static org.esa.snap.core.dataio.persistence.PersistenceDecoder.KEY_PERSISTENCE_ID;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class ComponentGeoCodingPersistenceSpiTest {

    private ComponentGeoCodingPersistenceSpi spi;

    @Before
    public void setUp() throws Exception {
        spi = new ComponentGeoCodingPersistenceSpi();
    }

    @Test
    public void canPersist() {
        assertThat(spi.canEncode(null), is(false));
        assertThat(spi.canEncode("any Object"), is(false));

        final ComponentGeoCoding object = new ComponentGeoCoding(null, null, null);
        assertThat(spi.canEncode(object), is(true));
    }

    @Test
    public void canDecode_PrehistoricalXML() {
        assertThat(spi.canDecode(null), is(false));

        final JdomLanguageSupport ls = new JdomLanguageSupport();
        Element element = new Element("any_name");

        assertThat(spi.canDecode(ls.translateToItem(element)), is(false));

        // the right element name
        element = new Element(NAME_COMPONENT_GEO_CODING);
        element.addContent(new Element("somePropertyButNotPersistenceIdProperty"));
        assertThat(spi.canDecode(ls.translateToItem(element)), is(true));
    }

    @Test
    public void canDecode_currentVersionXML() {
        assertThat(spi.canDecode(null), is(false));

        final JdomLanguageSupport ls = new JdomLanguageSupport();
        Element element = new Element("any_name");
        final Element persistenceIdProperty = new Element(KEY_PERSISTENCE_ID);
        persistenceIdProperty.setText("notTheCurrentId");
        element.addContent(persistenceIdProperty);

        assertThat(spi.canDecode(ls.translateToItem(element)), is(false));

        // the right child element
        persistenceIdProperty.setText(spi.createConverter().getID());
        assertThat(spi.canDecode(ls.translateToItem(element)), is(true));
    }

    @Test
    public void canDecode_currentVersionJSON() {
        assertThat(spi.canDecode(null), is(false));

        final JsonLanguageSupport ls = new JsonLanguageSupport();
        final HashMap<Object, Object> persistenceIdProperty = new HashMap<>();
        persistenceIdProperty.put(KEY_PERSISTENCE_ID, "notTheCurrentId");
        Map<String, Object> container = new HashMap<>();
        container.put("any_name", persistenceIdProperty);
        assertThat(spi.canDecode(ls.translateToItem(container)), is(false));

        // the right child element
        persistenceIdProperty.put(KEY_PERSISTENCE_ID, spi.createConverter().getID());
        assertThat(spi.canDecode(ls.translateToItem(container)), is(true));
    }

    @Test
    public void createPersistable() {
        final PersistenceConverter<?> converter = spi.createConverter();

        assertThat(converter, is(instanceOf(ComponentGeoCodingPersistenceConverter.class)));
    }
}