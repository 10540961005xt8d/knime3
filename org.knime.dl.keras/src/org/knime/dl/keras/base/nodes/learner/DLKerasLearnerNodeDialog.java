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

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.ArrayList;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSeparator;

import org.knime.core.data.DataTableSpec;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.dl.base.nodes.DLDefaultNodeDialogTab;
import org.knime.dl.base.nodes.DLInputsPanel;
import org.knime.dl.base.nodes.DefaultDLNodeDialogPane;
import org.knime.dl.base.portobjects.DLNetworkPortObject;
import org.knime.dl.base.portobjects.DLNetworkPortObjectSpec;
import org.knime.dl.core.DLNetwork;
import org.knime.dl.core.DLNetworkSpec;
import org.knime.dl.core.DLTensorSpec;
import org.knime.dl.util.DLUtils;

/**
 * @author Marcel Wiedenmann, KNIME GmbH, Konstanz, Germany
 * @author Christian Dietz, KNIME GmbH, Konstanz, Germany
 */
final class DLKerasLearnerNodeDialog extends DefaultDLNodeDialogPane {

	private static final NodeLogger LOGGER = NodeLogger.getLogger(DLKerasLearnerNodeDialog.class);

	private final DLDefaultNodeDialogTab m_generalTab = new DLDefaultNodeDialogTab("Options");

	private final DLDefaultNodeDialogTab m_advancedTab = new DLDefaultNodeDialogTab("Advanced Options");

	private final DLDefaultNodeDialogTab m_inputTab = new DLDefaultNodeDialogTab("Input Data");

	private final DLDefaultNodeDialogTab m_targetTab = new DLDefaultNodeDialogTab("Target Data");

	private final DLKerasLearnerGeneralConfig m_generalCfg;

	private DLKerasLearnerGeneralPanel m_generalPanel;

	private DLKerasLearnerOptimizationPanel m_optiPanel;

	private DLKerasLearningBehaviorPanel m_learningBehaviorPanel;

	private DLInputsPanel<DLKerasLearnerGeneralConfig, DLKerasLearnerInputConfig> m_inputsPanel;

	private final ArrayList<DLKerasLearnerTargetPanel> m_targetPanels = new ArrayList<>();

	private DLNetworkSpec m_lastConfiguredNetworkSpec;

	private DataTableSpec m_lastConfiguredTableSpec;

	public DLKerasLearnerNodeDialog() {
		addTab(m_inputTab.getTitle(), m_inputTab.getTab(), false);
		addTab(m_targetTab.getTitle(), m_targetTab.getTab(), false);
		addTab(m_generalTab.getTitle(), m_generalTab.getTab(), false);
		addTab(m_advancedTab.getTitle(), m_advancedTab.getTab(), false);

		m_generalCfg = DLKerasLearnerNodeModel.createGeneralModelConfig();
	}

	@Override
	public void reset() {
		// null check is necessary because this method is called from super constructor
		if (m_inputsPanel != null) {
		    m_inputsPanel.reset();
			m_targetPanels.clear();
		}
		super.reset();
	}

	@Override
	protected void saveSettingsTo(final NodeSettingsWO settings) throws InvalidSettingsException {
		super.saveSettingsTo(settings);

		m_generalCfg.saveToSettings(settings);

		m_inputsPanel.saveSettingsTo(settings);

		final NodeSettingsWO outputSettings = settings.addNodeSettings(DLKerasLearnerNodeModel.CFG_KEY_TARGET);
		for (final DLKerasLearnerTargetPanel outputPanel : m_targetPanels) {
			outputPanel.saveToSettings(outputSettings);
		}
	}
	
	private void checkPortObjectSpecs(final PortObjectSpec[] specs) throws NotConfigurableException {
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
        final DLNetworkPortObjectSpec portObjectSpec = (DLNetworkPortObjectSpec) specs[DLKerasLearnerNodeModel.IN_NETWORK_PORT_IDX];

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

		final DLNetworkPortObjectSpec portObjectSpec = (DLNetworkPortObjectSpec) specs[DLKerasLearnerNodeModel.IN_NETWORK_PORT_IDX];
        final DataTableSpec tableSpec = (DataTableSpec) specs[DLKerasLearnerNodeModel.IN_DATA_PORT_IDX];

		final DLNetworkSpec networkSpec = portObjectSpec.getNetworkSpec();

		if (networkSpec.getInputSpecs().length == 0) {
			LOGGER.warn("Input deep learning network has no input specs.");
		}
		if (networkSpec.getOutputSpecs().length == 0 && networkSpec.getHiddenOutputSpecs().length == 0) {
			LOGGER.warn("Input deep learning network has no output specs.");
		}

		final boolean networkChanged = !networkSpec.equals(m_lastConfiguredNetworkSpec);
		final boolean tableSpecChanged = !tableSpec.equals(m_lastConfiguredTableSpec);

		// first time we open dialog
		if (m_lastConfiguredNetworkSpec == null) {
			reset();
			createDialogContent(portObjectSpec);
			createInputPanels(networkSpec, tableSpec);
            createTargetPanels(networkSpec, tableSpec);
		} else if (networkChanged || tableSpecChanged) {
			reset();
			createInputPanels(networkSpec, tableSpec);
            createTargetPanels(networkSpec, tableSpec);
		}

		try {
			// we can always try to load the general settings, even if the network has changed
			m_generalCfg.loadFromSettings(settings);
		} catch (final InvalidSettingsException e1) {
			throw new NotConfigurableException(e1.getMessage());
		}

		super.loadSettingsFrom(settings, specs);

		// we have to be more careful here. Now, we want the following
        // behaviour:
        // * if a layer with the same name was loaded, it should still have the
        // same settings
        // * if a layer with a different name was loaded, the settings should
        // change
            m_inputsPanel.loadSettingsFrom(settings, specs);

		loadTargetSettings(settings, specs);

		m_lastConfiguredTableSpec = tableSpec;
		m_lastConfiguredNetworkSpec = networkSpec;
	}
	
	private void loadTargetSettings(final NodeSettingsRO settings, final PortObjectSpec[] specs) throws NotConfigurableException {
	    if (settings.containsKey(DLKerasLearnerNodeModel.CFG_KEY_TARGET)) {
            final NodeSettingsRO targetSettings;
            try {
                targetSettings = settings.getNodeSettings(DLKerasLearnerNodeModel.CFG_KEY_TARGET);
            } catch (final InvalidSettingsException e) {
                throw new NotConfigurableException(e.getMessage(), e);
            }
            for (final DLKerasLearnerTargetPanel targetPanel : m_targetPanels) {
                targetPanel.loadFromSettings(targetSettings, specs);
            }
        }
	}

	private void createInputPanels(final DLNetworkSpec networkSpec, final DataTableSpec tableSpec)
            throws NotConfigurableException {
	    // input settings:
        m_inputTab.reset();
        setWrapperPanel(m_inputTab.getTabRoot());
        addSeparator("Input Data");
        m_inputsPanel = new DLInputsPanel<>(networkSpec, tableSpec, m_generalCfg,
                DLKerasLearnerInputConfig::new, DLKerasLearnerNodeModel.IN_DATA_PORT_IDX,
                DLKerasLearnerNodeModel.CFG_KEY_INPUT, "Training input");
        addDialogComponentGroup(m_inputsPanel);
	}
	
	private void createTargetPanels(final DLNetworkSpec networkSpec, final DataTableSpec tableSpec)
            throws NotConfigurableException {
	 // output settings:
        m_targetTab.reset();
        setWrapperPanel(m_targetTab.getTabRoot());
        final JPanel targetsSeparator = new JPanel(new GridBagLayout());
        final GridBagConstraints targetsSeparatorLabelConstr = new GridBagConstraints();
        targetsSeparatorLabelConstr.gridwidth = 1;
        targetsSeparatorLabelConstr.weightx = 0;
        targetsSeparatorLabelConstr.anchor = GridBagConstraints.WEST;
        targetsSeparatorLabelConstr.fill = GridBagConstraints.NONE;
        targetsSeparatorLabelConstr.insets = new Insets(7, 7, 7, 7);
        final GridBagConstraints targetsSeparatorSeparatorConstr = new GridBagConstraints();
        targetsSeparatorSeparatorConstr.gridwidth = GridBagConstraints.REMAINDER;
        targetsSeparatorSeparatorConstr.weightx = 1;
        targetsSeparatorSeparatorConstr.fill = GridBagConstraints.HORIZONTAL;
        targetsSeparator.add(new JLabel("Training Targets"), targetsSeparatorLabelConstr);
        targetsSeparator.add(new JSeparator(), targetsSeparatorSeparatorConstr);
        addPanelToWrapper(targetsSeparator);
        // outputs
        for (final DLTensorSpec targetDataSpec : networkSpec.getOutputSpecs()) {
            if (!DLUtils.Shapes.isKnown(targetDataSpec.getShape())) {
                throw new NotConfigurableException(
                        "Target '" + targetDataSpec.getName() + "' has an unknown shape. This is not supported.");
            }
            addTargetPanel(targetDataSpec, tableSpec, m_generalCfg);
        }
	}

	private void createDialogContent(final DLNetworkPortObjectSpec portObjectSpec) throws NotConfigurableException {
		final DLNetworkSpec networkSpec = portObjectSpec.getNetworkSpec();
		final Class<? extends DLNetwork> networkType = portObjectSpec.getNetworkType();

		// general settings:
		m_generalTab.reset();
		setWrapperPanel(m_generalTab.getTabRoot());

		m_generalPanel = new DLKerasLearnerGeneralPanel(m_generalCfg, networkSpec, networkType);
		addDialogComponentGroupWithBorder(m_generalPanel, "General Settings");

		m_optiPanel = new DLKerasLearnerOptimizationPanel(m_generalCfg);
		addDialogComponentGroupWithBorder(m_optiPanel, "Optimizer Settings");

		// advanced settings:
		m_advancedTab.reset();
		setWrapperPanel(m_advancedTab.getTabRoot());
		m_learningBehaviorPanel = new DLKerasLearningBehaviorPanel(m_generalCfg);
		addDialogComponentGroupWithBorder(m_learningBehaviorPanel, "Learning Behavior");
	}

	private void addTargetPanel(final DLTensorSpec targetDataSpec, final DataTableSpec tableSpec,
			final DLKerasLearnerGeneralConfig generalCfg) {
		final DLKerasLearnerTargetConfig targetCfg = DLKerasLearnerNodeModel
				.createOutputTensorModelConfig(targetDataSpec.getName(), generalCfg);
		final DLKerasLearnerTargetPanel targetPanel = new DLKerasLearnerTargetPanel(targetCfg, targetDataSpec,
				tableSpec);
		// add target panel to dialog
		m_targetPanels.add(targetPanel);
		addDialogComponentGroupWithBorder(targetPanel, "Training target: " + targetDataSpec.getName());
	}
}
