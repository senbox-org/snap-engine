/*
 *
 * Copyright (c) 2021.  Brockmann Consult GmbH (info@brockmann-consult.de)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 3 of the License, or (at your option)
 * any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, see http://www.gnu.org/licenses/
 *
 */

package org.esa.snap.core.dataio.persistable.xml;

import org.esa.snap.core.dataio.persistable.Container;
import org.esa.snap.core.dataio.persistable.Item;
import org.esa.snap.core.dataio.persistable.Property;
import org.hamcrest.Matcher;
import org.jdom.Element;
import org.junit.Test;

import java.util.List;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.hamcrest.MatcherAssert.assertThat;

public class XmlSupportTest {

    @Test
    public void createRootContainer_invalid_name() {
        //preparation
        final String illegalRootName = "   first root   ";
        final String validXmlRootName = "first_root";
        final String illegalPropName = "   prop name   ";
        final String validXmlPropName = "prop_name";

        //execution
        final XmlSupport xmlSupport = new XmlSupport();
        final Container<Element> rootContainer = xmlSupport.createRootContainer(illegalRootName);
        rootContainer.set(xmlSupport.createProperty(illegalPropName, "    prop   value   "));

        //verification
        assertThat(rootContainer, is(notNull()));
        assertThat(rootContainer.getName(), is(illegalRootName));
        assertThat(rootContainer.getProperties().size(), is(1));
        assertThat(rootContainer.getProperties().get(0).getName(), is(illegalPropName));
        assertThat(rootContainer.getProperties().get(0), is(sameInstance(rootContainer.getProperty(illegalPropName))));

        final List<Element> created = xmlSupport.getCreated();
        assertThat(created, is(notNull()));
        assertThat(created.size(), is(1));
        final Element xmlRoot = created.get(0);
        assertThat(xmlRoot, is(notNull()));
        assertThat(xmlRoot.getName(), is(validXmlRootName));
        assertThat(xmlRoot.getChildren().size(), is(1));
        assertThat(xmlRoot.getAttribute("___unmodified_name___").getValue(), is(illegalRootName));
        assertThat(xmlRoot.getChild(validXmlPropName).getValue(), is("    prop   value   "));
    }

    @Test
    public void ContainerInsideContainer() {
        final XmlSupport xmlSupport = new XmlSupport();
        final Container<Element> root = xmlSupport.createRootContainer("roo");
        root.set(xmlSupport.createContainer("inside"));

        assertThat(root.getContainer().size(), is(1));
        assertThat(root.getProperties().size(), is(0));
        assertThat(root.getContainer("inside"), is(sameInstance(root.getContainer().get(0))));

        final List<Element> created = xmlSupport.getCreated();
        assertThat(created.size(), is(1));
        final Element rootElem = created.get(0);
        assertThat(rootElem.getName(), is("roo"));
        assertThat(rootElem.getAttributes().size(), is(0));
        assertThat(rootElem.getChildren().size(), is(1));
        assertThat(rootElem.getChild("inside"), is(sameInstance(rootElem.getChildren().get(0))));
        assertThat(rootElem.getChild("inside").getAttributes().size(), is(0));
    }

    @Test
    public void createTwoRootContainersWithProperties() {
        final XmlSupport xmlSupport = new XmlSupport();

        Container<Element> container;

        container = xmlSupport.createRootContainer("root");
        container.set(xmlSupport.createProperty("some", "aaa"));
        container.set(xmlSupport.createProperty("different", 3.58571236548123691235416549715));
        container.set(xmlSupport.createProperty("property", 21987430L));

        assertThat(container.getProperties().size(), is(3));

        container = xmlSupport.createRootContainer("  second root  ");
        container.set(xmlSupport.createProperty("And", "bbbbbb"));
        container.set(xmlSupport.createProperty("Other", 3.58571236548123691235416549715f));
        container.set(xmlSupport.createProperty("Characteristics", (short) 2198));

        assertThat(container.getProperties().size(), is(3));


        final List<Element> created = xmlSupport.getCreated();
        assertThat(created, is(notNull()));
        assertThat(created.size(), is(2));

        final Element firstRoot = created.get(0);
        assertThat(firstRoot, is(notNull()));
        assertThat(firstRoot.getName(), is("root"));
        assertThat(firstRoot.getChildren().size(), is(3));
        assertThat(firstRoot.getChild("some").getValue(), is("aaa"));
        assertThat(firstRoot.getChild("different").getValue(), is("3.585712365481237"));
        assertThat(firstRoot.getChild("property").getValue(), is("21987430"));

        final Element secondRoot = created.get(1);
        assertThat(secondRoot, is(notNull()));
        assertThat(secondRoot.getName(), is("second_root"));
        assertThat(secondRoot.getChildren().size(), is(3));
        assertThat(secondRoot.getChild("And").getValue(), is("bbbbbb"));
        assertThat(secondRoot.getChild("Other").getValue(), is("3.5857124"));
        assertThat(secondRoot.getChild("Characteristics").getValue(), is("2198"));
    }

    @Test
    public void propertyGetter() {
        final XmlSupport xmlSupport = new XmlSupport();
        final Property<Element> numba = xmlSupport.createProperty("Numba", 123456.78910111213141516);

        assertThat(numba.getValueString(), is("123456.78910111212"));
        assertThat(numba.getValueDouble(), is(123456.78910111212));
        assertThat(numba.getValueFloat(), is(123456.79F));

        final Property<Element> numbaLong = xmlSupport.createProperty("NumbaLong", Long.MAX_VALUE);

        assertThat(numbaLong.getValueLong(), is(Long.MAX_VALUE));
        assertThat(numbaLong.getValueInt(), is(-1));
        assertThat(numbaLong.getValueShort(), is((short) -1));
        assertThat(numbaLong.getValueByte(), is((byte) -1));

        final Property<Element> numbaInt = xmlSupport.createProperty("NumbaInt", Integer.MAX_VALUE);

        assertThat(numbaInt.getValueLong(), is((long) Integer.MAX_VALUE));
        assertThat(numbaInt.getValueInt(), is(Integer.MAX_VALUE));
        assertThat(numbaInt.getValueShort(), is((short) -1));
        assertThat(numbaInt.getValueByte(), is((byte) -1));

        final Property<Element> numbaShort = xmlSupport.createProperty("NumbaShort", Short.MAX_VALUE);

        assertThat(numbaShort.getValueLong(), is((long) Short.MAX_VALUE));
        assertThat(numbaShort.getValueInt(), is((int) Short.MAX_VALUE));
        assertThat(numbaShort.getValueShort(), is(Short.MAX_VALUE));
        assertThat(numbaShort.getValueByte(), is((byte) -1));

        final Property<Element> numbaByte = xmlSupport.createProperty("NumbaInt", Byte.MAX_VALUE);

        assertThat(numbaByte.getValueLong(), is((long) Byte.MAX_VALUE));
        assertThat(numbaByte.getValueInt(), is((int) Byte.MAX_VALUE));
        assertThat(numbaByte.getValueShort(), is((short) Byte.MAX_VALUE));
        assertThat(numbaByte.getValueByte(), is(Byte.MAX_VALUE));
    }

    @Test
    public void convert() {
        //preparation
        final Element parent = new Element("ParentContainer");
        parent.addContent(new Element("property_1").setText("some text value").setAttribute(XmlSupport.___UNMODIFIED_NAME___, "  property 1  "));
        parent.addContent(new Element("property_2").setText("some other text value"));
        final Element contaier1 = new Element("childContainer1");
        final Element contaier2 = new Element("childContainer2");
        parent.addContent(contaier1);
        parent.addContent(contaier2);
        contaier1.addContent(new Element("container1Leaf1").setText("leaf text value 1"));
        contaier1.addContent(new Element("container1Leaf2").setText("leaf text value 2"));
        contaier2.addContent(new Element("container2Leaf1").setText("leaf text value 3"));

        final Element parent2 = new Element("SecondParentContainer");
        parent2.addContent(new Element("prop").setText("prop value"));

        final Element singlePropElem = new Element("singleProperty").setText("single prop value");

        //execution
        final List<Item> converted = new XmlSupport().convert(parent, parent2, singlePropElem);

        //verification
        assertThat(converted.size(), is(3));
        assertThat(converted.get(0), is(instanceOf(Container.class)));
        assertThat(converted.get(1), is(instanceOf(Container.class)));
        assertThat(converted.get(2), is(instanceOf(Property.class)));

        final Container<Element> cont1 = (Container) converted.get(0);
        assertThat(cont1.getName(), is("ParentContainer"));
        assertThat(cont1.getProperties().size(), is(2));
        assertThat(cont1.getProperty("  property 1  "), is(sameInstance(cont1.getProperties().get(0))));
        assertThat(cont1.getProperty("  property 1  ").getValueString(), is("some text value"));
        assertThat(cont1.getProperty("property_2"), is(sameInstance(cont1.getProperties().get(1))));
        assertThat(cont1.getProperty("property_2").getValueString(), is("some other text value"));
        assertThat(cont1.getContainer().size(), is(2));

        final Container<Element> chCont1 = cont1.getContainer("childContainer1");
        assertThat(chCont1.getName(), is("childContainer1"));
        assertThat(chCont1, is(sameInstance(cont1.getContainer().get(0))));
        assertThat(chCont1.getProperties().size(), is(2));
        assertThat(chCont1.getProperty("container1Leaf1"), is(sameInstance(chCont1.getProperties().get(0))));
        assertThat(chCont1.getProperty("container1Leaf1").getValueString(), is("leaf text value 1"));
        assertThat(chCont1.getProperty("container1Leaf2"), is(sameInstance(chCont1.getProperties().get(1))));
        assertThat(chCont1.getProperty("container1Leaf2").getValueString(), is("leaf text value 2"));

        final Container<Element> chCont2 = cont1.getContainer("childContainer2");
        assertThat(chCont2, is(sameInstance(cont1.getContainer().get(1))));
        assertThat(chCont2.getProperties().size(), is(1));
        assertThat(chCont2.getProperty("container2Leaf1"), is(sameInstance(chCont2.getProperties().get(0))));
        assertThat(chCont2.getProperty("container2Leaf1").getValueString(), is("leaf text value 3"));

        final Container<Element> cont2 = (Container) converted.get(1);
        assertThat(cont2.getName(), is("SecondParentContainer"));
        assertThat(cont2.getProperties().size(), is(1));
        assertThat(cont2.getProperties().get(0).getName(), is("prop"));
        assertThat(cont2.getProperties().get(0).getValueString(), is("prop value"));
        assertThat(cont2.getContainer().size(), is(0));

        final Property singleProp = (Property) converted.get(2);
        assertThat(singleProp.getName(), is("singleProperty"));
        assertThat(singleProp.getValueString(), is("single prop value"));
    }

    private Matcher<Object> notNull() {
        return notNullValue();
    }
}