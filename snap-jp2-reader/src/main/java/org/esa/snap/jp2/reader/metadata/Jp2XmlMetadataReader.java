/*
 * Copyright (C) 2014-2015 CS-SI (foss-contact@thor.si.c-s.fr)
 * Copyright (C) 2014-2015 CS-Romania (office@c-s.ro)
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

package org.esa.snap.jp2.reader.metadata;

import org.esa.snap.core.metadata.GenericXmlMetadata;

import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;

/**
 * Reader for decoding JP2 XML blocks into Metadata elements.
 *
 * @author Cosmin Cara
 */
public class Jp2XmlMetadataReader {

    /* Start of contiguous codestream block (reversed) */
    private static final int[] JP2_JP2C = { 0x63, 0x32, 0x70, 0x6A };
    /* Start of any XML block (reversed) */
    private static final int[] JP2_XML = { 0x20, 0x6C, 0x6D, 0x78 };

    private static final Set<Integer> blockTerminators = new HashSet<Integer>() {{ add(0); add(7); }};

    private ByteSequenceMatcher jp2cMatcher;
    private ByteSequenceMatcher xmlTagMatcher;

    private Path jp2File;

    public Jp2XmlMetadataReader(Path jp2File) {
        this.jp2File = jp2File;
        jp2cMatcher = new ByteSequenceMatcher(JP2_JP2C);
        xmlTagMatcher = new ByteSequenceMatcher(JP2_XML);
    }

    public Jp2XmlMetadata read() {
        Jp2XmlMetadata metadata = null;
        if (jp2File != null && Files.isReadable(jp2File)) {
            try (FileChannel channel = FileChannel.open(jp2File)) {
                MappedByteBuffer mappedByteBuffer = channel.map(FileChannel.MapMode.READ_ONLY, 0L, channel.size());
                int currentByte;
                while (mappedByteBuffer.hasRemaining() && !jp2cMatcher.matches((currentByte = mappedByteBuffer.get()))) {
                    if (xmlTagMatcher.matches(currentByte)) {
                        String xmlString = extractBlock(mappedByteBuffer);
                        if (metadata == null) {
                            metadata = GenericXmlMetadata.create(Jp2XmlMetadata.class, xmlString);
                            metadata.setName("XML Metadata");
                        } else {
                            metadata.getRootElement().addElement(GenericXmlMetadata.create(Jp2XmlMetadata.class, xmlString).getRootElement());
                        }
                    }
                }
                channel.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return metadata;
    }

    private String extractBlock(MappedByteBuffer buffer) throws IOException {
        StringBuilder builder = new StringBuilder();
        int current;
        while (!blockTerminators.contains(current = buffer.get())) {
            builder.append(Character.toString((char) current));
        }
        return builder.toString();
    }

    private class ByteSequenceMatcher {
        private int[] queue;
        private int[] sequence;

        ByteSequenceMatcher(int[] sequenceToMatch) {
            sequence = sequenceToMatch;
            queue = new int[sequenceToMatch.length];
        }

        public boolean matches(int unsignedByte) {
            insert(unsignedByte);
            return isMatch();
        }

        private void insert(int unsignedByte) {
            System.arraycopy(queue, 0, queue, 1, sequence.length - 1);
            queue[0] = unsignedByte;
        }

        private boolean isMatch() {
            boolean result = true;
            for (int i = 0; i < sequence.length; i++) {
                result = (queue[i] == sequence[i]);
                if (!result)
                    break;
            }
            return result;
        }
    }
}
