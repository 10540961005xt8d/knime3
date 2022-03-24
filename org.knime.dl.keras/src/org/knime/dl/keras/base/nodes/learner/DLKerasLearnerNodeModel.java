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
 *  KNIME and ECLIPSE being a combined program, KNIME GMBH herewith grants
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
 *   May 2, 2017 (dietzc): created
 */
package org.knime.dl.keras.base.nodes.learner;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.knime.core.data.DataTableSpec;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.port.PortType;
import org.knime.core.node.util.filter.column.DataColumnSpecFilterConfiguration;
import org.knime.dl.base.portobjects.DLNetworkPortObject;
import org.knime.dl.base.portobjects.DLNetworkPortObjectSpec;
import org.knime.dl.core.DLDefaultRowIterator;
import org.knime.dl.core.DLException;
import org.knime.dl.core.DLMissingDependencyException;
import org.knime.dl.core.DLNetwork;
import org.knime.dl.core.DLNetworkSpec;
import org.knime.dl.core.DLRowIterator;
import org.knime.dl.core.DLTensorSpec;
import org.knime.dl.core.data.convert.DLDataValueToTensorConverterFactory;
import org.knime.dl.core.training.DLKnimeNetworkLearner;
import org.knime.dl.core.training.DLLossFunction;
import org.knime.dl.core.training.DLTrainingContextRegistry;
import org.knime.dl.keras.base.portobjects.DLKerasNetworkPortObject;
import org.knime.dl.keras.base.portobjects.DLKerasNetworkPortObjectSpec;
import org.knime.dl.keras.core.DLKerasNetwork;
import org.knime.dl.keras.core.DLKerasNetworkSpec;
import org.knime.dl.keras.core.training.DLKerasCallback;
import org.knime.dl.keras.core.training.DLKerasDefaultTrainingConfig;
import org.knime.dl.keras.core.training.DLKerasLossFunction;
import org.knime.dl.keras.core.training.DLKerasOptimizer;
import org.knime.dl.keras.core.training.DLKerasTrainableNetworkAdapter;
import org.knime.dl.keras.core.training.DLKerasTrainingConfig;
import org.knime.dl.keras.core.training.DLKerasTrainingContext;
import org.knime.dl.util.DLUtils;

import com.google.common.base.Strings;

/**
 * @author Marcel Wiedenmann, KNIME GmbH, Konstanz, Germany
 * @author Christian Dietz, KNIME GmbH, Konstanz, Germany
 */
final class DLKerasLearnerNodeModel extends NodeModel {

	static final int IN_NETWORK_PORT_IDX = 0;

	static final int IN_DATA_PORT_IDX = 1;

	static final int OUT_NETWORK_PORT_IDX = 0;

	static final String CFG_KEY_TRAINING = "training";

	static final String CFG_KEY_TARGET = "target";

	private static final NodeLogger LOGGER = NodeLogger.getLogger(DLKerasLearnerNodeModel.class);

	static DLKerasLearnerGeneralConfig createGeneralModelConfig() {
		return new DLKerasLearnerGeneralConfig();
	}

	static DLKerasLearnerInputConfig createInputTensorModelConfig(final String inputTensorName,
			final DLKerasLearnerGeneralConfig generalCfg) {
		return new DLKerasLearnerInputConfig(inputTensorName, generalCfg);
	}

	static DLKerasLearnerTargetConfig createOutputTensorModelConfig(final String outputTensorName,
			final DLKerasLearnerGeneralConfig generalCfg) {
		return new DLKerasLearnerTargetConfig(outputTensorName, generalCfg);
	}

	private final DLKerasLearnerGeneralConfig m_generalCfg;

	private final HashMap<String, DLKerasLearnerInputConfig> m_inputCfgs;

	private final HashMap<String, DLKerasLearnerTargetConfig> m_targetCfgs;

	private LinkedHashMap<DLTensorSpec, DLDataValueToTensorConverterFactory<?, ?>> m_converters;

	private DLNetworkSpec m_lastIncomingNetworkSpec;

	private DLNetworkSpec m_lastConfiguredNetworkSpec;

	private DataTableSpec m_lastIncomingTableSpec;

	private DataTableSpec m_lastConfiguredTableSpec;

	private boolean m_initialLoaded;

	DLKerasLearnerNodeModel() {
		super(new PortType[] { DLKerasNetworkPortObject.TYPE, BufferedDataTable.TYPE },
				new PortType[] { DLKerasNetworkPortObject.TYPE });
		m_generalCfg = createGeneralModelConfig();
		m_inputCfgs = new HashMap<>();
		m_targetCfgs = new HashMap<>();
	}

	@Override
	protected PortObjectSpec[] configure(final PortObjectSpec[] inSpecs) throws InvalidSettingsException {
		if (inSpecs[DLKerasLearnerNodeModel.IN_NETWORK_PORT_IDX] == null) {
			throw new InvalidSettingsException("Input deep learning network is missing.");
		}
		if (inSpecs[DLKerasLearnerNodeModel.IN_DATA_PORT_IDX] == null) {
			throw new InvalidSettingsException("Input data table is missing.");
		}
		if (!DLKerasNetworkPortObject.TYPE
				.acceptsPortObjectSpec(inSpecs[DLKerasLearnerNodeModel.IN_NETWORK_PORT_IDX])) {
			throw new InvalidSettingsException(
					"Input port object is not a valid Keras deep learning network port object.");
		}

		final DLKerasNetworkPortObjectSpec inPortObjectSpec = ((DLKerasNetworkPortObjectSpec) inSpecs[IN_NETWORK_PORT_IDX]);
		final DLKerasNetworkSpec inNetworkSpec = inPortObjectSpec.getNetworkSpec();
		final Class<? extends DLNetwork> inNetworkType = inPortObjectSpec.getNetworkType();
		final DataTableSpec inTableSpec = ((DataTableSpec) inSpecs[IN_DATA_PORT_IDX]);

		if (inNetworkSpec == null) {
			throw new InvalidSettingsException("Input port object's deep learning network specs are missing.");
		}

		m_lastIncomingNetworkSpec = inNetworkSpec;
		m_lastIncomingTableSpec = inTableSpec;

		if (m_lastConfiguredNetworkSpec != null && m_lastConfiguredTableSpec != null) {
			if (!m_lastConfiguredNetworkSpec.equals(m_lastIncomingNetworkSpec)) {
				throw new InvalidSettingsException("Input deep learning network changed. Please reconfigure the node.");
			}
		} else if (m_initialLoaded) {
			// loaded from saved workflow
			m_lastConfiguredNetworkSpec = m_lastIncomingNetworkSpec;
			m_lastConfiguredTableSpec = m_lastIncomingTableSpec;
		}

		try {
			configureGeneral(inNetworkType);
			configureInputs(inNetworkSpec, inTableSpec);
		} catch (final Exception e) {
			throw new InvalidSettingsException(e.getMessage(), e);
		}

		final DLNetworkPortObjectSpec outDataSpec = createOutputSpec(inPortObjectSpec);
		return new PortObjectSpec[] { outDataSpec };
	}

	@Override
	protected PortObject[] execute(final PortObject[] inObjects, final ExecutionContext exec) throws Exception {
		final PortObject inPortObject = inObjects[IN_NETWORK_PORT_IDX];
		final BufferedDataTable inTable = (BufferedDataTable) inObjects[IN_DATA_PORT_IDX];

		final PortObject outPortObject = executeInternal(inPortObject, inTable, exec);

		return new PortObject[] { outPortObject };
	}

	@Override
	protected void loadInternals(final File nodeInternDir, final ExecutionMonitor exec)
			throws IOException, CanceledExecutionException {
		// no op
	}

	@Override
	protected void saveInternals(final File nodeInternDir, final ExecutionMonitor exec)
			throws IOException, CanceledExecutionException {
		// no op
	}

	@Override
	protected void saveSettingsTo(final NodeSettingsWO settings) {
		try {
			m_generalCfg.saveToSettings(settings);

			final NodeSettingsWO inputSettings = settings.addNodeSettings(CFG_KEY_TRAINING);
			for (final DLKerasLearnerInputConfig inputCfg : m_inputCfgs.values()) {
				inputCfg.saveToSettings(inputSettings);
			}

			final NodeSettingsWO outputSettings = settings.addNodeSettings(CFG_KEY_TARGET);
			for (final DLKerasLearnerTargetConfig outputCfg : m_targetCfgs.values()) {
				outputCfg.saveToSettings(outputSettings);
			}
		} catch (final InvalidSettingsException e) {
			throw new RuntimeException(e.getMessage(), e);
		}
	}

	@Override
	protected void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
		m_inputCfgs.clear();
		final NodeSettingsRO inputSettings = settings.getNodeSettings(CFG_KEY_TRAINING);
		for (final String layerName : inputSettings) {
			final DLKerasLearnerInputConfig inputCfg = createInputTensorModelConfig(layerName, m_generalCfg);
			m_inputCfgs.put(layerName, inputCfg);
		}

		m_targetCfgs.clear();
		final NodeSettingsRO outputSettings = settings.getNodeSettings(CFG_KEY_TARGET);
		for (final String layerName : outputSettings) {
			final DLKerasLearnerTargetConfig outputCfg = createOutputTensorModelConfig(layerName, m_generalCfg);
			m_targetCfgs.put(layerName, outputCfg);
		}
	}

	@Override
	protected void loadValidatedSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {
		m_generalCfg.loadFromSettings(settings);

		final NodeSettingsRO inputSettings = settings.getNodeSettings(CFG_KEY_TRAINING);
		for (final DLKerasLearnerInputConfig inputCfg : m_inputCfgs.values()) {
			inputCfg.loadFromSettingsInModel(inputSettings);
		}

		final NodeSettingsRO outputSettings = settings.getNodeSettings(CFG_KEY_TARGET);
		for (final DLKerasLearnerTargetConfig outputCfg : m_targetCfgs.values()) {
			outputCfg.loadFromSettingsInModel(outputSettings);
		}

		m_lastConfiguredNetworkSpec = m_lastIncomingNetworkSpec;
		m_lastConfiguredTableSpec = m_lastIncomingTableSpec;
		m_initialLoaded = true;
	}

	@Override
	protected void reset() {
		// no op
	}

	private void configureGeneral(final Class<? extends DLNetwork> inNetworkType) throws Exception {
		final DLKerasTrainingContext<?> backend = m_generalCfg.getTrainingContextEntry().getValue();
		if (backend == null) {
			if (DLTrainingContextRegistry.getInstance().getTrainingContextsForNetworkType(inNetworkType).isEmpty()) {
				throw new DLMissingDependencyException("No compatible training back end available. "
						+ "Are you missing a KNIME Deep Learning extension?");
			}
			throw new InvalidSettingsException("No training back end selected. Please configure the node.");
		}
		if (!backend.getNetworkType().isAssignableFrom(inNetworkType)) {
			throw new InvalidSettingsException(
					"Selected training back end is not compatible to the input deep learning network. "
							+ "Please reconfigure the node.");
		}
		final DLKerasOptimizer optimizer = m_generalCfg.getOptimizerEntry().getValue();
		if (optimizer == null) {
			throw new InvalidSettingsException("No optimizer selected. Please configure the node.");
		}
	}

	private void configureInputs(final DLNetworkSpec inNetworkSpec, final DataTableSpec inTableSpec)
			throws InvalidSettingsException {
		if (inTableSpec.getNumColumns() == 0) {
			setWarningMessage("Input table has no columns. Output network will equal input network.");
		}
		m_converters = new LinkedHashMap<>(m_inputCfgs.size() + m_targetCfgs.size());
		final DLTensorSpec[] inputSpecs = inNetworkSpec.getInputSpecs();
		if (inputSpecs.length == 0) {
			setWarningMessage("Input deep learning network has no input specs.");
		}
		for (final DLTensorSpec tensorSpec : inNetworkSpec.getInputSpecs()) {
			// validate layer spec
			if (!DLUtils.Shapes.isFixed(tensorSpec.getShape())) {
				throw new InvalidSettingsException("Input '" + tensorSpec.getName()
						+ "' has an (at least partially) unknown shape. This is not supported, yet.");
			}
			final DLKerasLearnerInputConfig inputCfg = m_inputCfgs.get(tensorSpec.getName());
			if (inputCfg == null) {
				throw new InvalidSettingsException(
						"Network input '" + tensorSpec.getName() + "' is not yet configured.");
			}
			// get selected converter
			final DLDataValueToTensorConverterFactory<?, ?> converter = inputCfg.getConverterEntry().getValue();
			if (converter == null) {
				throw new InvalidSettingsException(
						"No converter selected for input '" + tensorSpec.getName() + "'. Please configure the node.");
			}
			m_converters.put(tensorSpec, converter);
			if (m_lastConfiguredTableSpec != null) {
				// check if selected columns are still in input table
				final DataColumnSpecFilterConfiguration filterConfig = inputCfg.getInputColumnsEntry().getValue();
				((DLDataTypeColumnFilter) filterConfig.getFilter()).setFilterClasses(converter.getSourceType());
				final String[] missingColumns = filterConfig.applyTo(inTableSpec).getRemovedFromIncludes();
				if (missingColumns.length != 0) {
					throw new InvalidSettingsException("Selected column '" + missingColumns[0] + "' of input '"
							+ tensorSpec.getName() + "' is missing in the input table. Please reconfigure the node.");
				}
			}
		}
		final DLTensorSpec[] outputSpecs = inNetworkSpec.getOutputSpecs();
		if (outputSpecs.length == 0) {
			setWarningMessage("Input deep learning network has no target specs.");
		}
		for (final DLTensorSpec tensorSpec : outputSpecs) {
			// validate layer spec
			if (!DLUtils.Shapes.isFixed(tensorSpec.getShape())) {
				throw new InvalidSettingsException("Target '" + tensorSpec.getName()
						+ "' has an (at least partially) unknown shape. This is not supported, yet.");
			}
			final DLKerasLearnerTargetConfig targetCfg = m_targetCfgs.get(tensorSpec.getName());
			if (targetCfg == null) {
				throw new InvalidSettingsException(
						"Network target '" + tensorSpec.getName() + "' is not yet configured.");
			}
			// get selected converter
			final DLDataValueToTensorConverterFactory<?, ?> converter = targetCfg.getConverterEntry().getValue();
			if (converter == null) {
				throw new InvalidSettingsException(
						"No converter selected for target '" + tensorSpec.getName() + "'. Please configure the node.");
			}
			m_converters.put(tensorSpec, converter);
			if (m_lastConfiguredTableSpec != null) {
				// check if selected columns are still in input table
				final DataColumnSpecFilterConfiguration filterConfig = targetCfg.getInputColumnsEntry().getValue();
				((DLDataTypeColumnFilter) filterConfig.getFilter()).setFilterClasses(converter.getSourceType());
				final String[] missingColumns = filterConfig.applyTo(inTableSpec).getRemovedFromIncludes();
				if (missingColumns.length != 0) {
					throw new InvalidSettingsException("Selected column '" + missingColumns[0] + "' of target '"
							+ tensorSpec.getName() + "' is missing in the input table. Please reconfigure the node.");
				}
			}
			final DLLossFunction lossFunction = targetCfg.getLossFunctionEntry().getValue();
			if (lossFunction == null) {
				throw new InvalidSettingsException("No loss function selected for target '" + tensorSpec.getName()
						+ "'. Please configure the node.");
			}
		}
	}

	private DLNetworkPortObjectSpec createOutputSpec(final DLNetworkPortObjectSpec inPortObjectSpec)
			throws InvalidSettingsException {
		// TODO: create new network spec with updated training config
		return inPortObjectSpec;
	}

	@SuppressWarnings("unchecked")
	private <N extends DLKerasNetwork> PortObject executeInternal(final PortObject inPortObject,
			final BufferedDataTable inTable, final ExecutionContext exec) throws Exception {
		final N inNetwork = (N) ((DLNetworkPortObject) inPortObject).getNetwork();
		final DLKerasNetworkSpec inNetworkSpec = inNetwork.getSpec();
		final DataTableSpec inTableSpec = inTable.getDataTableSpec();

		if (inTableSpec.getNumColumns() == 0 || inTable.size() == 0) {
			setWarningMessage("Input table is empty. Output network equals input network.");
			return inPortObject;
		}

		final DLKerasTrainingContext<N> ctx = (DLKerasTrainingContext<N>) m_generalCfg.getTrainingContextEntry()
				.getValue();

		// training configuration
		final int batchSize = m_generalCfg.getBatchSizeEntry().getValue();
		final int epochs = m_generalCfg.getEpochsEntry().getValue();
		final DLKerasOptimizer optimizer = m_generalCfg.getOptimizerEntry().getValue();
		final Map<DLTensorSpec, DLKerasLossFunction> lossFunctions = new HashMap<>();
		for (final DLTensorSpec targetSpec : inNetworkSpec.getOutputSpecs()) {
			final DLKerasLossFunction lossFunction = m_targetCfgs.get(targetSpec.getName()).getLossFunctionEntry()
					.getValue();
			lossFunctions.put(targetSpec, lossFunction);
		}
		final ArrayList<DLKerasCallback> callbacks = new ArrayList<>(3);
		if (m_generalCfg.getTerminateOnNaNEntry().getEnabled()) {
			callbacks.add(m_generalCfg.getTerminateOnNaNEntry().getValue());
		}
		if (m_generalCfg.getEarlyStoppingEntry().getEnabled()) {
			callbacks.add(m_generalCfg.getEarlyStoppingEntry().getValue());
		}
		if (m_generalCfg.getReduceLROnPlateauEntry().getEnabled()) {
			callbacks.add(m_generalCfg.getReduceLROnPlateauEntry().getValue());
		}
		final DLKerasTrainingConfig trainingConfig = new DLKerasDefaultTrainingConfig(batchSize, epochs, optimizer,
				lossFunctions, callbacks);

		final DLKerasTrainableNetworkAdapter trainableNetwork = ctx.trainable(inNetwork, trainingConfig);

		final Map<DLTensorSpec, int[]> columns = new HashMap<>(
				inNetworkSpec.getInputSpecs().length + inNetworkSpec.getOutputSpecs().length);
		for (final Entry<DLTensorSpec, DLDataValueToTensorConverterFactory<?, ?>> input : m_converters.entrySet()) {
			final DLTensorSpec tensorSpec = input.getKey();
			final DataColumnSpecFilterConfiguration filterConfig;
			final DLKerasLearnerInputConfig inputCfg = m_inputCfgs.get(tensorSpec.getName());
			if (inputCfg != null) {
				filterConfig = inputCfg.getInputColumnsEntry().getValue();
			} else {
				filterConfig = m_targetCfgs.get(tensorSpec.getName()).getInputColumnsEntry().getValue();
			}
			((DLDataTypeColumnFilter) filterConfig.getFilter()).setFilterClasses(input.getValue().getSourceType());
			// the input columns that will be used to fill the current spec's tensor
			final int[] indices = Arrays.stream(filterConfig.applyTo(inTableSpec).getIncludes()).mapToInt(column -> {
				final int idx = inTableSpec.findColumnIndex(column);
				if (idx == -1) {
					throw new IllegalStateException(
							"Selected input column '" + column + "' could not be found in the input table.");
				}
				return idx;
			}).toArray();
			columns.put(tensorSpec, indices);
		}

		try (final DLKnimeNetworkLearner learner = new DLKnimeNetworkLearner(trainableNetwork, m_converters);
				final DLRowIterator iterator = new DLDefaultRowIterator(inTable, columns)) {
			learner.train(iterator, exec);
		} catch (final Exception e) {
			String message;
			if (e instanceof DLException) {
				message = e.getMessage();
			} else {
				if (!Strings.isNullOrEmpty(e.getMessage())) {
					LOGGER.error(e.getMessage());
				}
				message = "An error occured during training of the Keras deep learning network. See log for details.";
			}
			throw new RuntimeException(message, e);
		}
		exec.setMessage("Saving trained Keras deep learning network...");
		return trainableNetwork.getNetwork().getTrainedNetwork(exec);
	}
}
