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
 *   Jul 10, 2017 (marcel): created
 */
package org.knime.dl.keras.base.nodes.learner;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.OptionalLong;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataValue;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.util.filter.NameFilterConfiguration.FilterResult;
import org.knime.core.node.util.filter.column.DataColumnSpecFilterConfiguration;
import org.knime.core.node.util.filter.column.DataColumnSpecFilterPanel;
import org.knime.dl.base.nodes.DialogComponentObjectSelection;
import org.knime.dl.core.DLTensorSpec;
import org.knime.dl.core.data.convert.DLCollectionDataValueToTensorConverterFactory;
import org.knime.dl.core.data.convert.DLDataValueToTensorConverterFactory;
import org.knime.dl.core.training.DLLossFunction;
import org.knime.dl.core.training.DLTrainingContext;
import org.knime.dl.keras.core.training.DLKerasLossFunction;
import org.knime.dl.keras.core.training.DLKerasTrainingContext;
import org.knime.dl.util.DLUtils;

/**
 * @author Marcel Wiedenmann, KNIME GmbH, Konstanz, Germany
 * @author Christian Dietz, KNIME GmbH, Konstanz, Germany
 */
final class DLKerasLearnerTargetPanel extends JPanel {

	private static final long serialVersionUID = 1L;

	private final DLKerasLearnerTargetConfig m_cfg;

	private final DLTensorSpec m_outputDataSpec;

	private final DialogComponentObjectSelection<DLDataValueToTensorConverterFactory<?, ?>> m_dcConverter;

	private final DataColumnSpecFilterPanel m_dcInputColumns;

	private final DialogComponentObjectSelection<DLKerasLossFunction> m_dcLossFunction;

	private DataTableSpec m_lastTableSpec;

	DLKerasLearnerTargetPanel(final DLKerasLearnerTargetConfig cfg, final DLTensorSpec outputDataSpec,
			final DataTableSpec tableSpec) throws NotConfigurableException {
		super(new GridBagLayout());
		m_cfg = cfg;
		m_outputDataSpec = outputDataSpec;
		m_lastTableSpec = tableSpec;

		// construct panel:

		setBorder(BorderFactory.createTitledBorder("Training target: " + m_outputDataSpec.getName()));
		final GridBagConstraints constr = new GridBagConstraints();
		constr.gridx = 0;
		constr.gridy = 0;
		constr.weightx = 1;
		constr.anchor = GridBagConstraints.WEST;
		constr.fill = GridBagConstraints.VERTICAL;
		// meta information
		final JPanel numNeurons = new JPanel();
		final GridBagConstraints numNeuronsConstr = new GridBagConstraints();
		numNeuronsConstr.insets = new Insets(5, 0, 5, 0);
		numNeurons.add(
				new JLabel("Number of neurons: "
						+ DLUtils.Shapes.getSize(DLUtils.Shapes.getFixedShape(m_outputDataSpec.getShape()).get())),
				numNeuronsConstr);
		add(numNeurons, constr);
		constr.gridy++;
		final JPanel shape = new JPanel();
		final GridBagConstraints shapeConstr = new GridBagConstraints();
		shapeConstr.insets = new Insets(5, 0, 5, 0);
		shape.add(new JLabel("Shape: " + m_outputDataSpec.getShape().toString()), shapeConstr);
		add(shape, constr);
		constr.gridy++;
		// converter selection
		m_dcConverter = new DialogComponentObjectSelection<>(m_cfg.getConverterEntry(), c -> "From " + c.getName(),
				"Conversion");
		add(m_dcConverter.getComponentPanel(), constr);
		constr.gridy++;
		// column selection
		final JPanel inputColumnsLabel = new JPanel();
		inputColumnsLabel.add(new JLabel("Target columns:"));
		add(inputColumnsLabel, constr);
		constr.gridy++;
		final JPanel inputColumnsFilter = new JPanel();
		m_dcInputColumns = new DataColumnSpecFilterPanel();
		inputColumnsFilter.add(m_dcInputColumns);
		add(inputColumnsFilter, constr);
		constr.gridy++;
		// loss function selection
		m_dcLossFunction = new DialogComponentObjectSelection<>(m_cfg.getLossFunctionEntry(), DLLossFunction::getName,
				"Loss function");
		add(m_dcLossFunction.getComponentPanel(), constr);
		constr.gridy++;

		m_cfg.getGeneralConfig().getTrainingContextEntry().addLoadListener((entry) -> {
			try {
				refreshAvailableConverters();
				refreshAvailableLossFunctions();
			} catch (final NotConfigurableException ex) {
				throw new IllegalStateException(ex.getMessage(), ex);
			}
		});

		m_cfg.getGeneralConfig().getTrainingContextEntry().addValueChangeListener((entry, oldValue) -> {
			try {
				refreshAvailableConverters();
				refreshAvailableLossFunctions();
			} catch (final NotConfigurableException ex) {
				throw new IllegalStateException(ex.getMessage(), ex);
			}
		});

		m_cfg.getConverterEntry().addValueChangeListener((entry, oldValue) -> refreshAllowedInputColumns());
		m_cfg.getConverterEntry().addLoadListener((entry) -> refreshAllowedInputColumns());

		m_dcConverter.getConfigEntry().addLoadPredicate((e) -> {
			return m_lastTableSpec.containsCompatibleType(e.getValue().getSourceType());
		});
	}

	DLKerasLearnerTargetConfig getConfig() {
		return m_cfg;
	}

	DataColumnSpecFilterPanel getInputColumns() {
		return m_dcInputColumns;
	}

	void saveToSettings(final NodeSettingsWO settings) throws InvalidSettingsException {
		final long inputSize = DLUtils.Shapes.getSize(DLUtils.Shapes.getFixedShape(m_outputDataSpec.getShape())
				.orElseThrow(() -> new InvalidSettingsException("Target '" + m_outputDataSpec.getName()
						+ "' has an (at least partially) unknown shape. This is not supported.")));
		m_dcInputColumns.saveConfiguration(m_cfg.getInputColumnsEntry().getValue());
		m_cfg.saveToSettings(settings);
		// validate input: get user-selected columns and converter, ask
		// converter for its output size given the input
		// columns (if possible) and compare to number of available target
		// neurons
		final FilterResult filter = m_cfg.getInputColumnsEntry().getValue().applyTo(m_lastTableSpec);
		final List<DataColumnSpec> includedColSpecs = Arrays.stream(filter.getIncludes())
				.collect(Collectors.mapping(col -> m_lastTableSpec.getColumnSpec(col), Collectors.toList()));
		final DLDataValueToTensorConverterFactory<? extends DataValue, ?> converter =
				m_cfg.getConverterEntry().getValue();
		final OptionalLong destSizeOpt = converter.getDestCount(includedColSpecs);
		if (destSizeOpt.isPresent()) {
			final long converterOutputSize = destSizeOpt.getAsLong();
			if (converterOutputSize > inputSize) {
				throw new InvalidSettingsException("Selected target columns provide more elements ("
						+ converterOutputSize + ") than neurons available (" + inputSize + ") for network target '"
						+ m_outputDataSpec.getName() + "'. Try removing some columns from the selection.");
			}
			if (converterOutputSize < inputSize) {
				throw new InvalidSettingsException("Selected target columns do not provide enough elements ("
						+ converterOutputSize + ") to populate all neurons (" + inputSize + ") of network target '"
						+ m_outputDataSpec.getName() + "'. Try adding some columns to the selection.");
			}
		} else {
			// we still can check if there are more input columns than input
			// neurons since every column provides at
			// least one element
			if (includedColSpecs.size() > inputSize) {
				throw new InvalidSettingsException("More target columns selected (" + includedColSpecs.size()
						+ ") than neurons available (" + inputSize + ") for network target '"
						+ m_outputDataSpec.getName() + "'. Try removing some columns from the selection.");
			}
		}
	}

	void loadFromSettings(final NodeSettingsRO settings, final PortObjectSpec[] specs) throws NotConfigurableException {
		m_lastTableSpec = (DataTableSpec) specs[DLKerasLearnerNodeModel.IN_DATA_PORT_IDX];
		try {
			m_cfg.loadFromSettingsInDialog(settings, m_lastTableSpec);
			refreshAllowedInputColumns();
		} catch (final InvalidSettingsException e) {
			// ignore
		}
	}

	void refreshAvailableConverters() throws NotConfigurableException {
		final DLTrainingContext<?, ?> trainingContext = m_cfg.getGeneralConfig().getTrainingContextEntry().getValue();
		final Collection<DLDataValueToTensorConverterFactory<?, ?>> converterFactories =
				DLKerasLearnerTargetConfig.getAvailableConverters(trainingContext, m_lastTableSpec, m_outputDataSpec);
		if (converterFactories.isEmpty()) {
			throw new NotConfigurableException(
					"No converters available for target '" + m_outputDataSpec.getName() + "'.");
		}
		final Set<DLDataValueToTensorConverterFactory<?, ?>> builtInElement = new HashSet<>(1);
		final Set<DLDataValueToTensorConverterFactory<?, ?>> builtInCollection = new HashSet<>(1);
		final Set<DLDataValueToTensorConverterFactory<?, ?>> extensionElement = new HashSet<>(1);
		final Set<DLDataValueToTensorConverterFactory<?, ?>> extensionCollection = new HashSet<>(1);
		for (final DLDataValueToTensorConverterFactory<?, ?> converter : converterFactories) {
			if (converter.getClass().getCanonicalName().contains("org.knime.dl.core.data.convert")) {
				if (converter instanceof DLCollectionDataValueToTensorConverterFactory) {
					builtInCollection.add(converter);
				} else {
					builtInElement.add(converter);
				}
			} else {
				if (converter instanceof DLCollectionDataValueToTensorConverterFactory) {
					extensionCollection.add(converter);
				} else {
					extensionElement.add(converter);
				}
			}
		}
		final Comparator<DLDataValueToTensorConverterFactory<?, ?>> nameComparator =
				Comparator.comparing(DLDataValueToTensorConverterFactory::getName);
		final List<DLDataValueToTensorConverterFactory<?, ?>> converterFactoriesSorted = Stream.concat(
				Stream.concat(builtInElement.stream().sorted(nameComparator),
						extensionElement.stream().sorted(nameComparator)),
				Stream.concat(builtInCollection.stream().sorted(nameComparator),
						extensionCollection.stream().sorted(nameComparator)))
				.collect(Collectors.toList());
		m_dcConverter.replaceListItems(converterFactoriesSorted, null);
	}

	private void refreshAllowedInputColumns() {
		final Class<? extends DataValue> allowedColType = m_cfg.getConverterEntry().getValue().getSourceType();
		if (m_lastTableSpec.containsCompatibleType(allowedColType)) {
			m_dcInputColumns.loadConfiguration(m_cfg.getInputColumnsEntry().getValue(), m_lastTableSpec);
			m_cfg.getInputColumnsEntry().setValue(new DataColumnSpecFilterConfiguration(
					DLKerasLearnerInputConfig.CFG_KEY_INPUT_COL, new DLDataTypeColumnFilter(allowedColType)));
			final DataColumnSpecFilterConfiguration filterConfig = m_cfg.getInputColumnsEntry().getValue();
			m_dcInputColumns.updateWithNewConfiguration(filterConfig);
		}
		// FIXME (knime-core):
		// Strange behavior within DataColumnSpecFilterPanel (see
		// #toFilteredStringArray where m_filter is always
		// null because it doesn't get set in #updateWithNewConfiguration (only
		// in the super class).
		// Also see NameFilterPanel#loadConfiguration where
		// #getRemovedFromIncludeList and #getRemovedFromExcludeList
		// get added to the panel, which makes sense in general but not really
		// when updating the filter config).
	}

	void refreshAvailableLossFunctions() throws NotConfigurableException {
		final DLKerasTrainingContext<?> trainingContext = m_cfg.getGeneralConfig().getTrainingContextEntry().getValue();
		final List<DLKerasLossFunction> availableLossFunctions = trainingContext.createLossFunctions() //
				.stream() //
				.sorted(Comparator.comparing(DLKerasLossFunction::getName)) //
				.collect(Collectors.toList());
		if (availableLossFunctions.isEmpty()) {
			throw new NotConfigurableException("No loss functions available for output '" + m_outputDataSpec.getName()
					+ "' (with training context '" + trainingContext.getName() + "').");
		}
		final DLKerasLossFunction selectedLossFunction =
				m_cfg.getLossFunctionEntry().getValue() != null ? m_cfg.getLossFunctionEntry().getValue()
						: availableLossFunctions.get(0);
		for (int i = availableLossFunctions.size() - 1; i >= 0; i--) {
			if (availableLossFunctions.get(i).getClass() == selectedLossFunction.getClass()) {
				availableLossFunctions.remove(i);
				availableLossFunctions.add(i, selectedLossFunction);
			}
		}
		m_dcLossFunction.replaceListItems(availableLossFunctions, selectedLossFunction);
	}
}
