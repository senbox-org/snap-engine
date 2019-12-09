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

package org.esa.snap.binning.operator;


import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

import java.util.Random;

import org.esa.snap.core.util.RandomUtils;

import static org.junit.Assert.assertEquals;

/**
 * Tests runtime behaviour and performance of {@link FileChannel#map(FileChannel.MapMode, long, long)}.
 * May be used to store intermediate spatial bins.
 *
 * @author Norman Fomferra
 */
public class MappedByteBufferRunner {

    interface FileIO {
        void write(File file, int n, Producer producer) throws IOException;

        int read(File file, Consumer consumer) throws IOException;
    }

    interface Producer {
        long createKey();

        float[] createSamples();
    }

    interface Consumer {
        void process(long key, float[] samples);
    }


    private static final int MiB = 1024 * 1024;
    private static final int N = 25000;


    public static void main(String[] args) throws Exception {
        testMemoryMappedFileIOPerformance();
        testStreamedFileIOPerformance();
    }

    private static void testStreamedFileIOPerformance() throws Exception {
        File file = genTestFile();
        testFileIOPerformance(file, new StreamedFileIO());
        deleteFile(file);
    }

    private static void testMemoryMappedFileIOPerformance() throws Exception {
        File file = genTestFile();
        testFileIOPerformance(file, new MemoryMappedFileIO());
        deleteFile(file);
    }

    private static void testFileIOPerformance(File file, FileIO fileIO) throws IOException {

        System.out.println("Testing " + fileIO.getClass().getSimpleName() + " for " + N + " samples");

        MyProducer producer = new MyProducer();
        MyConsumer consumer = new MyConsumer();

        long t1 = System.currentTimeMillis();
        fileIO.write(file, N, producer);
        long t2 = System.currentTimeMillis();
        fileIO.read(file, consumer);
        long t3 = System.currentTimeMillis();

        assertEquals(N, producer.n);
        assertEquals(N, consumer.n);

        System.out.println("buf write time: " + (t2 - t1) + " ms");
        System.out.println("buf read time:  " + (t3 - t2) + " ms");
        System.out.println("buf total time: " + (t3 - t1) + " ms");
        System.out.println("file size:      " + file.length() + " bytes");
    }

    static class MemoryMappedFileIO implements FileIO {
        @Override
        public void write(File file, int n, Producer producer) throws IOException {
            RandomAccessFile raf = new RandomAccessFile(file, "rw");
            FileChannel channel = raf.getChannel();
            ByteBuffer buffer = channel.map(FileChannel.MapMode.READ_WRITE, 0, 100L * MiB);
            try {
                for (int i = 0; i < n; i++) {
                    long key = producer.createKey();
                    float[] samples = producer.createSamples();
                    writeKey(buffer, key);
                    writeSamples(buffer, samples);
                }
            } finally {
                writeKey(buffer, -1L);
                raf.close();
            }
        }

        @Override
        public int read(File file, Consumer consumer) throws IOException {
            RandomAccessFile raf = new RandomAccessFile(file, "r");
            FileChannel channel = raf.getChannel();
            long length = file.length();
            ByteBuffer buffer = channel.map(FileChannel.MapMode.READ_ONLY, 0, length);
            int n = 0;
            try {
                while (true) {
                    long key = readKey(buffer);
                    if (key == -1L) {
                        break;
                    }
                    float[] samples = readSamples(buffer);
                    consumer.process(key, samples);
                    n++;
                }
            } finally {
                raf.close();
            }
            return n;
        }

        private long readKey(ByteBuffer is) {
            return is.getLong();
        }

        private float[] readSamples(ByteBuffer is) {
            int n = is.getInt();
            float[] samples = new float[n];
            for (int i = 0; i < samples.length; i++) {
                samples[i] = is.getFloat();
            }
            return samples;
        }

        private void writeKey(ByteBuffer stream, long key) {
            stream.putLong(key);
        }

        private void writeSamples(ByteBuffer stream, float[] samples) {
            stream.putInt(samples.length);
            for (float sample : samples) {
                stream.putFloat(sample);
            }
        }


    }

    static class StreamedFileIO implements FileIO {
        @Override
        public void write(File file, int n, Producer producer) throws IOException {
            try (DataOutputStream stream = new DataOutputStream(new FileOutputStream(file))) {
                for (int i = 0; i < n; i++) {
                    long key = producer.createKey();
                    float[] samples = producer.createSamples();
                    writeKey(stream, key);
                    writeSamples(stream, samples);
                }
            }
        }

        @Override
        public int read(File file, Consumer consumer) throws IOException {
            int n = 0;
            try (DataInputStream stream = new DataInputStream(new FileInputStream(file))) {
                while (true) {
                    long key;
                    try {
                        key = readKey(stream);
                    } catch (EOFException eof) {
                        break;
                    }
                    float[] samples = readSamples(stream);
                    consumer.process(key, samples);
                    n++;
                }
            }
            return n;
        }

        private long readKey(DataInputStream is) throws IOException {
            return is.readLong();
        }

        private float[] readSamples(DataInputStream is) throws IOException {
            int n = is.readInt();
            float[] samples = new float[n];
            for (int i = 0; i < samples.length; i++) {
                samples[i] = is.readFloat();
            }
            return samples;
        }

        private void writeKey(DataOutputStream stream, long key) throws IOException {
            stream.writeLong(key);
        }

        private void writeSamples(DataOutputStream stream, float[] samples) throws IOException {
            stream.writeInt(samples.length);
            for (float sample : samples) {
                stream.writeFloat(sample);
            }
        }
    }

    /**
     * Sample producer which creates randomly-sized sample arrays (length: 0 ... 10).
     */
    private static class MyProducer implements Producer {
        long n;

        @Override
        public long createKey() {
            return n++;
        }

        @Override
        public float[] createSamples() {
            final Random random = new Random(RandomUtils.seed())
            final float[] samples = new float[(int) (random.nextDouble() * 11)];
            for (int i = 0; i < samples.length; i++) {
                samples[i] = 0.1f * i;
            }
            return samples;
        }
    }

    /**
     * Sample consumer which simply counts received sample arrays.
     */
    private static class MyConsumer implements Consumer {
        long n;

        @Override
        public void process(long key, float[] samples) {
            n++;
        }
    }

    private static long getFreeMiB() {
        return Runtime.getRuntime().freeMemory() / MiB;
    }

    static File genTestFile() throws IOException {
        return File.createTempFile(MappedByteBufferRunner.class.getSimpleName() + "-", ".dat");
    }

    static void deleteFile(File file) {
        if (file.exists()) {
            if (!file.delete()) {
                file.deleteOnExit();
            }
        }
    }
}
