/*
 * ------------------------------------------------------------------------
 *
 *  Copyright by KNIME AG, Zurich, Switzerland
 *  Website: http://www.knime.com; Email: contact@knime.com
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License, Version 3, as
 *  published by the Free Software Foundation.
 *
 *  This program is distributed in the hope that it will be useful, but
 *  WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, see <http://www.gnu.org/licenses>.
 *
 *  Additional permission under GNU GPL version 3 section 7:
 *
 *  KNIME interoperates with ECLIPSE solely via ECLIPSE's plug-in APIs.
 *  Hence, KNIME and ECLIPSE are both independent programs and are not
 *  derived from each other. Should, however, the interpretation of the
 *  GNU GPL Version 3 ("License") under any applicable laws result in
 *  KNIME and ECLIPSE being a combined program, KNIME AG herewith grants
 *  you the additional permission to use and propagate KNIME together with
 *  ECLIPSE with only the license terms in place for ECLIPSE applying to
 *  ECLIPSE and the GNU GPL Version 3 applying for KNIME, provided the
 *  license terms of ECLIPSE themselves allow for the respective use and
 *  propagation of ECLIPSE together with KNIME.
 *
 *  Additional permission relating to nodes for KNIME that extend the Node
 *  Extension (and in particular that are based on subclasses of NodeModel,
 *  NodeDialog, and NodeView) and that only interoperate with KNIME through
 *  standard APIs ("Nodes"):
 *  Nodes are deemed to be separate and independent programs and to not be
 *  covered works.  Notwithstanding anything to the contrary in the
 *  License, the License does not apply to Nodes, you are not required to
 *  license Nodes under the License, and you are granted a license to
 *  prepare and propagate Nodes, in each case even if such Nodes are
 *  propagated with or for interoperation with KNIME.  The owner of a Node
 *  may freely choose the license terms applicable to such Node, including
 *  when such Node is propagated with or for interoperation with KNIME.
 * ---------------------------------------------------------------------
 *
 */
package org.knime.dl.python.core.data.serde;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.charset.Charset;

import org.knime.core.data.DataCell;
import org.knime.core.data.filestore.FileStoreFactory;
import org.knime.dl.core.DLTensor;
import org.knime.dl.python.core.data.DLPythonDataBuffer;
import org.knime.dl.python.core.data.DLPythonFloatBuffer;
import org.knime.dl.python.core.data.DLPythonStringBuffer;
import org.knime.python.typeextension.Deserializer;
import org.knime.python.typeextension.DeserializerFactory;

import com.google.common.base.Charsets;

/**
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 */
public class DLPythonStringBufferDeserializerFactory extends DeserializerFactory
    implements DLPythonDeserializerFactory {

    /**
     * The unique identifier of this deserializer factory.
     */
    public static final String IDENTIFIER =
        "org.knime.dl.python.core.data.serde.DLPythonStringBufferDeserializerFactory";

    /**
     */
    public DLPythonStringBufferDeserializerFactory() {
        super(DLPythonStringBuffer.TYPE);
    }

    @Override
    public Class<? extends DLPythonDataBuffer<?>> getBufferType() {
        return DLPythonFloatBuffer.class;
    }

    @Override
    public Deserializer createDeserializer() {
        return new DLPythonDeserializer<DLPythonStringBuffer>() {

            @Override
            public DataCell deserialize(byte[] bytes, FileStoreFactory fileStoreFactory) throws IOException {
                int[] lengths = getLengths(bytes);
                int nValues = lengths.length;
                final DLPythonStringBuffer value = new DLPythonStringBuffer(nValues);
                String[] storage = value.getStorageForWriting(value.size(), nValues);
                readStrings(bytes, storage, lengths);
                return value;
            }

            private void readStrings(byte[] bytes, String[] dst, int[] lengths) {
                int nValues = lengths.length;
                int offset = Integer.BYTES * (nValues + 1);
                for (int i = 0; i < nValues; i++) {
                    int length = lengths[i];
                    dst[i] = new String(bytes, offset, length, Charsets.UTF_8);
                    offset += length;
                }
            }

            private int[] getLengths(byte[] bytes) {
                final ByteBuffer buffer = ByteBuffer.wrap(bytes);
                buffer.order(ByteOrder.LITTLE_ENDIAN);
                int nValues = buffer.getInt();
                int[] lengths = new int[nValues];
                for (int i = 0; i < nValues; i++) {
                    lengths[i] = buffer.getInt();
                }
                return lengths;
            }

            @Override
            public void deserialize(byte[] bytes, DLTensor<DLPythonStringBuffer> data) {
                final int[] lengths = getLengths(bytes);
                final int nValues = lengths.length;
                DLPythonStringBuffer tensorBuffer = data.getBuffer();
                final int writeStart = (int)tensorBuffer.size();
                final String[] tensorStorage = tensorBuffer.getStorageForWriting(writeStart, nValues);
                readStrings(bytes, tensorStorage, lengths);
            }

        };
    }

}