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
 *   Jun 2, 2017 (marcel): created
 */
package org.knime.dl.keras.base.nodes.learner;

import org.knime.core.data.DataTableSpec;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.dl.base.nodes.DLDefaultNodeDialogTab;
import org.knime.dl.base.nodes.DLInputPanel;
import org.knime.dl.base.nodes.DLInputsPanel;
import org.knime.dl.base.nodes.DLTensorRole;
import org.knime.dl.base.nodes.DefaultDLNodeDialogPane;
import org.knime.dl.base.portobjects.DLNetworkPortObject;
import org.knime.dl.base.portobjects.DLNetworkPortObjectSpec;
import org.knime.dl.core.DLNetworkSpec;
import org.knime.dl.core.DLTensorSpec;

/**
 * @author Marcel Wiedenmann, KNIME GmbH, Konstanz, Germany
 * @author Christian Dietz, KNIME GmbH, Konstanz, Germany
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 */
final class DLKerasLearnerNodeDialog extends DefaultDLNodeDialogPane {

	private static final NodeLogger LOGGER = NodeLogger.getLogger(DLKerasLearnerNodeDialog.class);

	private final DLKerasLearnerGeneralConfig m_generalCfg;

	private final DLKerasLearnerGeneralPanel m_generalPanel;

	private final DLKerasLearnerOptimizationPanel m_optiPanel;

	private final DLKerasLearningBehaviorPanel m_learningBehaviorPanel;

	private final DLInputsPanel<DLInputPanel<DLKerasLearnerInputConfig>> m_inputsPanel;

	private final DLInputsPanel<DLKerasLearnerTargetPanel> m_targetsPanel;

	public DLKerasLearnerNodeDialog() {
	    DLDefaultNodeDialogTab generalTab = new DLDefaultNodeDialogTab("Options");
	    DLDefaultNodeDialogTab advancedTab = new DLDefaultNodeDialogTab("Advanced Options");
	    DLDefaultNodeDialogTab inputTab = new DLDefaultNodeDialogTab("Input Data");
	    DLDefaultNodeDialogTab targetTab = new DLDefaultNodeDialogTab("Target Data");
		addTab(inputTab.getTitle(), inputTab.getTab(), false);
		addTab(targetTab.getTitle(), targetTab.getTab(), false);
		addTab(generalTab.getTitle(), generalTab.getTab(), false);
		addTab(advancedTab.getTitle(), advancedTab.getTab(), false);

		m_generalCfg = DLKerasLearnerNodeModel.createGeneralModelConfig();
		m_inputsPanel = new DLInputsPanel<>(this::createInputPanel,
		        DLKerasLearnerNodeModel.CFG_KEY_INPUT, "Training input");
		m_targetsPanel = new DLInputsPanel<>(this::createTargetPanel,
		        DLKerasLearnerNodeModel.CFG_KEY_TARGET, "Training target");
		m_generalPanel = new DLKerasLearnerGeneralPanel(m_generalCfg);
		m_learningBehaviorPanel = new DLKerasLearningBehaviorPanel(m_generalCfg);
		m_optiPanel = new DLKerasLearnerOptimizationPanel(m_generalCfg);
		
		// inputs
		setWrapperPanel(inputTab.getTabRoot());
        addSeparator("Input Data");
        addDialogComponentGroup(m_inputsPanel);
        
        // targets
        setWrapperPanel(targetTab.getTabRoot());
        addSeparator("Training Targets");
        addDialogComponentGroup(m_targetsPanel);
		
		// general settings
		setWrapperPanel(generalTab.getTabRoot());
        addDialogComponentGroupWithBorder(m_generalPanel, "General Settings");
        addDialogComponentGroupWithBorder(m_optiPanel, "Optimizer Settings");

        // advanced settings:
        setWrapperPanel(advancedTab.getTabRoot());
        addDialogComponentGroupWithBorder(m_learningBehaviorPanel, "Learning Behavior");
	}


	private DLKerasLearnerTargetPanel createTargetPanel(final DLTensorSpec tensorSpec) {
        final DLKerasLearnerTargetConfig cfg = new DLKerasLearnerTargetConfig(tensorSpec.getName(), m_generalCfg);
        return new DLKerasLearnerTargetPanel(cfg, tensorSpec);
    }


    private DLInputPanel<DLKerasLearnerInputConfig> createInputPanel(final DLTensorSpec tensorSpec) {
        final DLKerasLearnerInputConfig inputCfg = new DLKerasLearnerInputConfig(tensorSpec.getName(), m_generalCfg);
        return new DLInputPanel<>(inputCfg, tensorSpec, "Input columns:", DLTensorRole.INPUT);
    }


    @Override
	protected void saveSettingsTo(final NodeSettingsWO settings) throws InvalidSettingsException {
		super.saveSettingsTo(settings);

		m_generalCfg.saveToSettings(settings);

		m_inputsPanel.saveSettingsTo(settings);

		m_targetsPanel.saveSettingsTo(settings);
	}

	private static void checkPortObjectSpecs(final PortObjectSpec[] specs) throws NotConfigurableException {
	    if (specs[DLKerasLearnerNodeModel.IN_NETWORK_PORT_IDX] == null) {
            throw new NotConfigurableException("Input deep learning network port object is missing.");
        }
        if (specs[DLKerasLearnerNodeModel.IN_DATA_PORT_IDX] == null) {
            throw new NotConfigurableException("Input data table is missing.");
        }

        if (!DLNetworkPortObject.TYPE.acceptsPortObjectSpec(specs[DLKerasLearnerNodeModel.IN_NETWORK_PORT_IDX])) {
            throw new NotConfigurableException("Input port object is not a valid deep learning network port object.");
        }
        if (((DataTableSpec) specs[DLKerasLearnerNodeModel.IN_DATA_PORT_IDX]).getNumColumns() == 0) {
            throw new NotConfigurableException("Input table is missing or has no columns.");
        }
        final DLNetworkPortObjectSpec portObjectSpec =
                (DLNetworkPortObjectSpec) specs[DLKerasLearnerNodeModel.IN_NETWORK_PORT_IDX];

        if (portObjectSpec.getNetworkSpec() == null) {
            throw new NotConfigurableException("Input port object's deep learning network spec is missing.");
        }
        if (portObjectSpec.getNetworkType() == null) {
            throw new NotConfigurableException("Input port object's deep learning network type is missing.");
        }
	}

	@Override
	protected void loadSettingsFrom(final NodeSettingsRO settings, final PortObjectSpec[] specs)
			throws NotConfigurableException {
		checkPortObjectSpecs(specs);

		final DLNetworkPortObjectSpec portObjectSpec =
		        (DLNetworkPortObjectSpec) specs[DLKerasLearnerNodeModel.IN_NETWORK_PORT_IDX];
        final DataTableSpec tableSpec = (DataTableSpec) specs[DLKerasLearnerNodeModel.IN_DATA_PORT_IDX];

		final DLNetworkSpec networkSpec = portObjectSpec.getNetworkSpec();

		if (networkSpec.getInputSpecs().length == 0) {
			LOGGER.warn("Input deep learning network has no input specs.");
		}
		if (networkSpec.getOutputSpecs().length == 0 && networkSpec.getHiddenOutputSpecs().length == 0) {
			LOGGER.warn("Input deep learning network has no output specs.");
		}

		try {
			// we can always try to load the general settings, even if the network has changed
			m_generalCfg.loadFromSettings(settings);
		} catch (final InvalidSettingsException e1) {
			throw new NotConfigurableException(e1.getMessage(), e1);
		}

		super.loadSettingsFrom(settings, specs);

		m_inputsPanel.loadSettingsFrom(settings, networkSpec.getInputSpecs(), tableSpec);
		m_targetsPanel.loadSettingsFrom(settings, networkSpec.getOutputSpecs(), tableSpec);
	}

}
