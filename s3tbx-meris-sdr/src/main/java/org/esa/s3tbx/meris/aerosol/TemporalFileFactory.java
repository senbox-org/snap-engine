/*
 * $Id: TemporalFileFactory.java,v 1.1 2007/03/27 12:52:21 marcoz Exp $
 *
 * Copyright (c) 2003 Brockmann Consult GmbH. All right reserved.
 * http://www.brockmann-consult.de
 */
package org.esa.s3tbx.meris.aerosol;

import java.io.File;

public interface TemporalFileFactory {
    TemporalFile createTemporalFile(final File file);
}
