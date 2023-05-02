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
 * History
 *   May 3, 2017 (marcel): created
 */
package org.knime.dl.python.core.execution;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.ArrayUtils;
import org.knime.dl.core.DLInvalidEnvironmentException;
import org.knime.dl.core.DLMissingExtensionException;
import org.knime.dl.core.DLTensor;
import org.knime.dl.core.DLTensorFactory;
import org.knime.dl.core.DLTensorId;
import org.knime.dl.core.DLTensorSpec;
import org.knime.dl.core.data.DLReadableBuffer;
import org.knime.dl.core.data.DLWritableBuffer;
import org.knime.dl.core.execution.DLAbstractExecutableNetwork;
import org.knime.dl.core.execution.DLExecutionMonitor;
import org.knime.dl.core.execution.DLNetworkInputPreparer;
import org.knime.dl.core.execution.DLNetworkOutputConsumer;
import org.knime.dl.python.core.DLPythonCommands;
import org.knime.dl.python.core.DLPythonNetwork;
import org.knime.dl.python.core.DLPythonNetworkHandle;
import org.knime.dl.python.core.DLPythonNetworkLoaderRegistry;

/**
 * @author Marcel Wiedenmann, KNIME GmbH, Konstanz, Germany
 * @author Christian Dietz, KNIME GmbH, Konstanz, Germany
 */
public abstract class DLPythonAbstractExecutableNetwork<N extends DLPythonNetwork, C extends DLPythonCommands>
	extends DLAbstractExecutableNetwork<N> {

	private C m_commands;

	private DLPythonNetworkHandle m_handle;

	protected DLPythonAbstractExecutableNetwork(final N network, final Set<DLTensorSpec> executionInputSpecs,
			final Set<DLTensorId> requestedOutputs,
			final DLNetworkInputPreparer<DLTensor<? extends DLWritableBuffer>> inputPreparer,
			final DLNetworkOutputConsumer<DLTensor<? extends DLReadableBuffer>> outputConsumer,
			final DLTensorFactory tensorFactory) {
		super(network, executionInputSpecs, requestedOutputs, inputPreparer, outputConsumer, tensorFactory);
	}

	protected abstract C createCommands() throws DLInvalidEnvironmentException;

	@Override
	public void close() throws Exception {
		super.close();
		if (m_commands != null) {
			m_commands.close();
		}
	}

	@Override
	protected void executeInternal(final DLExecutionMonitor monitor) throws Exception {
		if (m_commands == null) {
			m_commands = createCommands();
			m_handle = DLPythonNetworkLoaderRegistry.getInstance().getNetworkLoader(m_network.getClass()).orElseThrow(
					() -> new DLMissingExtensionException("Python back end '" + m_network.getClass().getCanonicalName()
							+ "' could not be found. Are you missing a KNIME Deep Learning extension?"))
					.load(m_network.getSource(), m_commands.getContext(), false);
		}
		for (long i = 0; i < m_inputPreparer.getNumBatches(); i++) {
			m_inputPreparer.prepare(m_input, i);
			m_commands.setNetworkInputs(m_handle, m_input);
			m_commands.executeNetwork(m_handle, m_requestedOutputs, m_batchSize);
			for (final DLTensor<?> input : m_input.values()) {
				input.getBuffer().reset();
			}
			if (m_output == null) {
				m_output = new HashMap<>(m_requestedOutputs.size());
				final DLTensorSpec[] outputSpecs = ArrayUtils.addAll(m_network.getSpec().getOutputSpecs(),
						m_network.getSpec().getHiddenOutputSpecs());
				final Map<DLTensorId, long[]> outputShapes = m_commands.getNetworkOutputShapes(m_handle,
						m_requestedOutputs);
				for (final DLTensorSpec spec : outputSpecs) {
					if (m_requestedOutputs.contains(spec.getIdentifier())) {
						final long[] shape = outputShapes.get(spec.getIdentifier());
						final long batchSize = shape[0];
						final long[] shapeWithoutBatchSize = new long[shape.length - 1];
						System.arraycopy(shape, 1, shapeWithoutBatchSize, 0, shapeWithoutBatchSize.length);
						final DLTensorSpec executionSpec = m_tensorFactory.createExecutionTensorSpec(spec, batchSize,
								shapeWithoutBatchSize);
						m_output.put(spec.getIdentifier(), m_tensorFactory.createReadableTensor(executionSpec));
					}
				}
			}
			m_commands.getNetworkOutputs(m_handle, m_output);
			for (final DLTensor<?> output : m_output.values()) {
				output.getBuffer().reset();
			}
		}
	}
}
