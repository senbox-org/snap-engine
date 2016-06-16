/*
 * Copyright (C) 2012 Brockmann Consult GmbH (info@brockmann-consult.de)
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

package org.esa.s3tbx.fu;

/**
 * @author muhammad.bc
 */
interface FuAlgo {
     double[] ANGLE_OF_TRANSITIONS = new double[]{
             232.0, 227.168, 220.977, 209.994, 190.779, 163.084, 132.999,
             109.054, 94.037, 83.346, 74.572, 67.957, 62.186, 56.435,
             50.665, 45.129, 39.769, 34.906, 30.439, 26.337, 22.741, 19.0, 19.0};
     byte MAX_FU_VALUE = 21;

     FuResult compute(double[] spectrum);
}
