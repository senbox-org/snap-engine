/*
 * Copyright (C) 2010 Brockmann Consult GmbH (info@brockmann-consult.de)
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
 */
package org.esa.snap.core.dataio;

/**
 * The <code>ProductReaderPlugIn</code> interface is implemented by data product reader plug-ins.
 * <p>ProductReaderPlugIn plug-ins are used to provide meta-information about a particular data format and to create instances of
 * the actual reader objects.
 * <p> A plug-in can register itself in the <code>ProductIO</code> plug-in registry or it is automatically found during
 * a classpath scan.
 * <p>
 * Consider reading the developer guide when implementing a new product reader in the wiki:<br>
 * <a href="https://senbox.atlassian.net/wiki/spaces/SNAP/pages/10584125/How+to+create+a+new+product+reader">How to create a new product reader</a>
 *
 * @author Norman Fomferra
 * @see ProductWriterPlugIn
 */
public interface ProductReaderPlugIn extends ProductIOPlugIn {

    /**
     * Gets the qualification of the product reader to decode a given input object.
     * Things to consider when implementing this method:
     * <ul>
     * <li>make sure method always succeeds; ensure that no exception is thrown</li>
     * <li>make sure that it is fast (terminates within milliseconds)</li>
     * <li>make sure method is thread safe</li>
     * <li>use the INTENDED qualification with care; generic readers shall return SUITABLE at most</li>
     * </ul>
     *
     * @param input the input object
     * @return the decode qualification
     */
    DecodeQualification getDecodeQualification(Object input);

    /**
     * Returns an array containing the classes that represent valid input types for this reader.
     * <p> Instances of the classes returned in this array are valid objects for the <code>setInput</code> method of the
     * <code>ProductReader</code> interface (the method will not throw an <code>InvalidArgumentException</code> in this
     * case).
     *
     * @return an array containing valid input types, never <code>null</code>
     */
    Class[] getInputTypes();

    /**
     * Creates an instance of the actual product reader class. This method should never return <code>null</code>.
     *
     * @return a new reader instance, never <code>null</code>
     */
    ProductReader createReaderInstance();

}
