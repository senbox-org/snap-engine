/*
 * $Id: L2CloudAuxData.java,v 1.1 2007/03/27 12:52:22 marcoz Exp $
 *
 * Copyright (C) 2007 by Brockmann Consult (info@brockmann-consult.de)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the
 * Free Software Foundation. This program is distributed in the hope it will
 * be useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package org.esa.s3tbx.meris.cloud;

import org.esa.s3tbx.meris.l2auxdata.AuxFile;
import org.esa.s3tbx.meris.l2auxdata.DpmConfig;
import org.esa.s3tbx.meris.l2auxdata.L2AuxDataException;
import org.esa.s3tbx.util.math.LUT;
import org.esa.snap.core.datamodel.ProductData;

import java.io.IOException;

/**
 * Created by marcoz.
 *
 * @author marcoz
 * @version $Revision: 1.1 $ $Date: 2007/03/27 12:52:22 $
 */
public class L2CloudAuxData {

    public LUT surfAlb;

    // Number of different longitudes:
    private static final int SA_NUM_LON = 3600;
    // Number of different latitudes:
    private static final int SA_NUM_LAT = 1800;

    private int month;

    public L2CloudAuxData(DpmConfig config, int month) throws IOException, L2AuxDataException {
        this.month = month;
        loadAuxdata(config);
    }

    private void loadAuxdata(DpmConfig config) throws IOException, L2AuxDataException {
        final AuxFile auxFileO = AuxFile.open('V', config.getAuxDatabaseFile("cloud", null));
        try {
            loadCloudFile(auxFileO);
        } finally {
            auxFileO.close();
        }

    }

    private void loadCloudFile(AuxFile auxFileO) throws IOException {
        double[] latTab = auxFileO.readDoubleArray("V200", -1);
        double[] lonTab = auxFileO.readDoubleArray("V201", -1);
        float surfalbScale = auxFileO.readFloat("V206");

        final ProductData data = auxFileO.readRecord("V300", month, -1, ProductData.TYPE_FLOAT32, null);
        float[] tmpAlb = (float[]) data.getElems();

        float[][] surfAlb_LUT = new float[SA_NUM_LAT][SA_NUM_LON];
        for (int i = 0; i < SA_NUM_LAT; i++) {
            for (int j = 0; j < SA_NUM_LON; j++) {
                surfAlb_LUT[i][j] = surfalbScale * tmpAlb[i * SA_NUM_LON + j];
            }
        }
        surfAlb = new LUT(surfAlb_LUT);
        surfAlb.setTab(0, latTab);
        surfAlb.setTab(1, lonTab);
    }
}
