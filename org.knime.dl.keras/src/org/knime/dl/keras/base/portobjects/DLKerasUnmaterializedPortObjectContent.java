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
package org.knime.dl.keras.base.portobjects;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Collections;

import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.dl.core.DLNetworkLocation;
import org.knime.dl.keras.core.DLKerasNetwork;
import org.knime.dl.keras.core.layers.DLInvalidTensorSpecException;
import org.knime.dl.keras.core.layers.DLKerasLayer;
import org.knime.dl.keras.core.layers.DLKerasNetworkLayerGraphSerializer;
import org.knime.dl.keras.core.layers.DLKerasNetworkMaterializer;

/**
 * @author Marcel Wiedenmann, KNIME GmbH, Konstanz, Germany
 * @author Christian Dietz, KNIME GmbH, Konstanz, Germany
 */
final class DLKerasUnmaterializedPortObjectContent implements DLKerasPortObjectContent {

    private final DLKerasUnmaterializedNetworkPortObjectSpec m_spec;

    // also used for deserialization
    DLKerasUnmaterializedPortObjectContent(final DLKerasLayer outputLayer) throws DLInvalidTensorSpecException {
        m_spec = new DLKerasUnmaterializedNetworkPortObjectSpec(outputLayer);
    }

    @Override
    public DLKerasUnmaterializedNetworkPortObjectSpec getSpec() {
        return m_spec;
    }

    public DLKerasMaterializedPortObjectContent materialize(final DLNetworkLocation saveLocation)
        throws IOException {
        try {
            final DLKerasNetwork materialized =
                new DLKerasNetworkMaterializer(Collections.singletonList(m_spec.getOutputLayer()), saveLocation)
                    .materialize();
            return new DLKerasMaterializedPortObjectContent(materialized);
        } catch (final Exception e) {
            throw new IOException(
                "An error occurred while creating the Keras network from its layer specifications. See log for details.",
                e);
        }
    }

    static final class Serializer {

        public void savePortObjectContent(final DLKerasUnmaterializedPortObjectContent portObjectContent,
            final ObjectOutputStream objOut, final ExecutionMonitor exec)
            throws IOException, CanceledExecutionException {
            new DLKerasNetworkLayerGraphSerializer()
                .writeGraphTo(Collections.singletonList(portObjectContent.m_spec.getOutputLayer()), objOut);
        }

        public DLKerasUnmaterializedPortObjectContent loadPortObjectContent(final ObjectInputStream objIn,
            final PortObjectSpec spec, final ExecutionMonitor exec) throws IOException, CanceledExecutionException {
            try {
                final DLKerasLayer outputLayer = new DLKerasNetworkLayerGraphSerializer().readGraphFrom(objIn).get(0);
                return new DLKerasUnmaterializedPortObjectContent(outputLayer);
            } catch (final ClassNotFoundException e) {
                throw new IOException("Failed to load Keras deep learning network port object."
                    + " Are you missing a KNIME Deep Learning extension.", e);
            } catch (final Exception e) {
                throw new IOException("Failed to load Keras deep learning network port object. See log for details.",
                    e);
            }
        }
    }
}
