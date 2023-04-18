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
package org.esa.snap.core.util.math;

/**
 * The class {@code IntervalPartition} is a representation of an interval partition,
 * i.e. a strictly monotonous sequence of real numbers.
 *
 * @author Ralf Quast
 */
public class IntervalPartition {

    private final double[] sequence;
    private final int monotonicity;

    /**
     * Constructs an interval partition from a sequence of real numbers.
     *
     * @param sequence the sequence. The sequence must increase or decrease strictly and
     *                 consist of at least two real numbers.
     * @throws IllegalArgumentException if the sequence is not strictly monotonous
     *                                  or consists of less than two real numbers.
     * @throws NullPointerException     if the sequence is {@code null}.
     */
    public IntervalPartition(final double... sequence) {
        this(new Array.Double(sequence));
    }

    /**
     * Constructs an interval partition from a sequence of real numbers.
     *
     * @param sequence the sequence. The sequence must increase or decrease strictly and
     *                 consist of at least two real numbers.
     * @throws IllegalArgumentException if the sequence is not strictly monotonous
     *                                  or consists of less than two real numbers.
     * @throws NullPointerException     if the sequence is {@code null}.
     */
    public IntervalPartition(final float... sequence) {
        this(new Array.Float(sequence));
    }

    private IntervalPartition(final Array sequence) {
        if (sequence.getLength() < 2) {
            throw new IllegalArgumentException("sequence.length < 2");
        }
        monotonicity = ensureStrictMonotonicity(sequence);

        this.sequence = new double[sequence.getLength()];
        sequence.copyTo(0, this.sequence, 0, this.sequence.length);
    }

    /**
     * Creates an array of interval partitions from an array of sequences.
     *
     * @param sequences the array of sequences. Each sequence must increase or decrease
     *                  strictly and consist of at least two real numbers.
     * @return the created array of interval partitions.
     * @throws IllegalArgumentException if the length of the sequence array is zero
     *                                  or any sequence is not strictly monotonous
     *                                  or consists of less than two real numbers.
     * @throws NullPointerException     if the array of sequences or any sequence
     *                                  is {@code null}.
     */
    public static IntervalPartition[] createArray(final double[]... sequences) {
        if (sequences.length == 0) {
            throw new IllegalArgumentException("sequences.length == 0");
        }

        final IntervalPartition[] partitions = new IntervalPartition[sequences.length];

        for (int i = 0; i < partitions.length; ++i) {
            partitions[i] = new IntervalPartition(sequences[i]);
        }

        return partitions;
    }

    /**
     * Creates an array of interval partitions from an array of sequences.
     *
     * @param sequences the array of sequences. Each sequence must increase or decrease
     *                  strictly and consist of at least two real numbers.
     * @return the created array of interval partitions.
     * @throws IllegalArgumentException if the length of the sequence array is zero
     *                                  or any sequence is not strictly monotonous
     *                                  or consists of less than two real numbers.
     * @throws NullPointerException     if the array of sequences or any sequence
     *                                  is {@code null}.
     */
    public static IntervalPartition[] createArray(final float[]... sequences) {
        if (sequences.length == 0) {
            throw new IllegalArgumentException("sequences.length == 0");
        }

        final IntervalPartition[] partitions = new IntervalPartition[sequences.length];

        for (int i = 0; i < partitions.length; ++i) {
            partitions[i] = new IntervalPartition(sequences[i]);
        }

        return partitions;
    }

    /**
     * Returns the cardinal number of the interval partition.
     *
     * @return the cardinal number.
     */
    public final int getCardinal() {
        return sequence.length;
    }

    /**
     * Returns the ith number in the interval partition.
     *
     * @param i the index number of the real number of interest.
     * @return the ith real number.
     */
    public final double get(int i) {
        return sequence[i];
    }

    /**
     * Returns the sequence of all numbers in the interval partition.
     *
     * @return the sequence of all numbers.
     */
    public final double[] getSequence() {
        return sequence.clone();
    }

    /**
     * Returns the maximum (i.e. final) number in the partition.
     *
     * @return the maximum number in the partition.
     */
    public final double getMax() {
        return monotonicity > 0 ? sequence[sequence.length - 1] : sequence[0];
    }

    /**
     * Returns the minimum (i.e. first) number in the partition.
     *
     * @return the minimum number in the partition.
     */
    public final double getMin() {
        return monotonicity > 0 ? sequence[0] : sequence[sequence.length - 1];
    }

    /**
     * Returns the mesh of the interval partition, i.e. the maximum
     * distance between two adjacent real numbers in the partition.
     *
     * @return the mesh.
     */
    public final double getMesh() {
        double mesh = 0.0;

        for (int i = 1; i < sequence.length; ++i) {
            final double length = Math.abs(sequence[i] - sequence[i - 1]);
            if (length > mesh) {
                mesh = length;
            }
        }

        return mesh;
    }

    static int ensureStrictMonotonicity(final Array sequence) throws IllegalArgumentException {
        int monotonicity = 0;
        for (int i = 1; i < sequence.getLength(); ++i) {
            if (sequence.getValue(i - 1) < sequence.getValue(i)) {
                monotonicity += 1;
            } else if (sequence.getValue(i - 1) > sequence.getValue(i)) {
                monotonicity -= 1;
            }
            if (Math.abs(monotonicity) != i) {
                throw new IllegalArgumentException("sequence is not strictly monotonous");
            }
        }
        return monotonicity;
    }

    /**
     * Returns the monotonicity of the interval partition. A positive (negative) value indicates
     * a strictly increasing (decreasing) partition.
     *
     * @return the monotonicity.
     */
    public final int getMonotonicity() {
        return monotonicity;
    }
}
