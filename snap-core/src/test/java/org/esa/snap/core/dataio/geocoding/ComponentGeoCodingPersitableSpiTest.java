package org.esa.snap.core.dataio.geocoding;

import org.esa.snap.core.dataio.dimap.spi.DimapPersistable;
import org.jdom.Element;
import org.junit.Before;
import org.junit.Test;

import static org.esa.snap.core.dataio.geocoding.ComponentGeoCodingPersistable.TAG_COMPONENT_GEO_CODING;
import static org.junit.Assert.*;
import static org.hamcrest.CoreMatchers.*;

public class ComponentGeoCodingPersitableSpiTest {

    private ComponentGeoCodingPersistableSpi spi;

    @Before
    public void setUp() throws Exception {
        spi = new ComponentGeoCodingPersistableSpi();
    }

    @Test
    public void canPersist() {
        assertThat(spi.canPersist(null), is(false));
        assertThat(spi.canPersist("any Object"), is(false));

        final ComponentGeoCoding object = new ComponentGeoCoding(null, null, null);
        assertThat(spi.canPersist(object), is(true));
    }

    @Test
    public void canDecode() {
        Element parent;

        parent = null;
        assertThat(spi.canDecode(parent), is(false));

        parent = new Element("any_name");
        assertThat(spi.canDecode(parent), is(false));

        parent.addContent(new Element("A_child_element_but_not_the_right_one"));
        assertThat(spi.canDecode(parent), is(false));

        // the right child element
        parent.addContent(new Element(TAG_COMPONENT_GEO_CODING));
        assertThat(spi.canDecode(parent), is(true));
    }

    @Test
    public void createPersistable() {
        final DimapPersistable persistable = spi.createPersistable();

        assertThat(persistable, is(instanceOf(ComponentGeoCodingPersistable.class)));
    }
}