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
 */
package org.knime.dl.keras.base.nodes.learner;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.dl.base.nodes.AbstractGridBagDialogComponentGroup;
import org.knime.dl.base.settings.ConfigEntry;
import org.knime.dl.keras.core.training.DLKerasCallback.DLKerasEarlyStopping;
import org.knime.dl.keras.core.training.DLKerasCallback.DLKerasReduceLROnPlateau;
import org.knime.dl.keras.core.training.DLKerasCallback.DLKerasTerminateOnNaN;

/**
 * @author Marcel Wiedenmann, KNIME, Konstanz, Germany
 * @author Christian Dietz, KNIME, Konstanz, Germany
 */
public class DLKerasLearningBehaviorPanel extends AbstractGridBagDialogComponentGroup {

	private final DLKerasLearnerGeneralConfig m_cfg;

	DLKerasLearningBehaviorPanel(final DLKerasLearnerGeneralConfig cfg) throws NotConfigurableException {
		m_cfg = cfg;

		final ConfigEntry<DLKerasTerminateOnNaN> terminateOnNaN = m_cfg.getTerminateOnNaNEntry();
		terminateOnNaN.addEnabledChangeOrLoadListener((e, oldEnabled) -> e.getValue().setAllEnabled(e.getEnabled()));
		addToggleComponentGroup(terminateOnNaN, terminateOnNaN.getValue().getName(),
				terminateOnNaN.getValue().getParameterDialogGroup());

		addHorizontalSeparator();

		final ConfigEntry<DLKerasEarlyStopping> earlyStopping = m_cfg.getEarlyStoppingEntry();
		earlyStopping.addEnabledChangeOrLoadListener((e, oldEnabled) -> e.getValue().setAllEnabled(e.getEnabled()));
		addToggleComponentGroup(earlyStopping, earlyStopping.getValue().getName(),
				earlyStopping.getValue().getParameterDialogGroup());

		addHorizontalSeparator();

		final ConfigEntry<DLKerasReduceLROnPlateau> reduceLROnPlateau = m_cfg.getReduceLROnPlateauEntry();
		reduceLROnPlateau.addEnabledChangeOrLoadListener((e, oldEnabled) -> e.getValue().setAllEnabled(e.getEnabled()));
		addToggleComponentGroup(reduceLROnPlateau, reduceLROnPlateau.getValue().getName(),
				reduceLROnPlateau.getValue().getParameterDialogGroup());
	}

	@Override
	public void saveSettingsTo(final NodeSettingsWO settings) throws InvalidSettingsException {
		// no op
	}

	@Override
	public void loadSettingsFrom(final NodeSettingsRO settings, final PortObjectSpec[] specs)
			throws NotConfigurableException {
		// on first load, the individual entries of the toggled "learning behavior" groups have to be set according to
		// the toggle state (as the toggle is only associated with the groups config, not with the config's entries,
		// also see change listeners in above constructor)
		final ConfigEntry<DLKerasTerminateOnNaN> terminateOnNan = m_cfg.getTerminateOnNaNEntry();
		terminateOnNan.getValue().setAllEnabled(terminateOnNan.getEnabled());
		final ConfigEntry<DLKerasEarlyStopping> earlyStopping = m_cfg.getEarlyStoppingEntry();
		earlyStopping.getValue().setAllEnabled(earlyStopping.getEnabled());
		final ConfigEntry<DLKerasReduceLROnPlateau> reduceLROnPlateu = m_cfg.getReduceLROnPlateauEntry();
		reduceLROnPlateu.getValue().setAllEnabled(reduceLROnPlateu.getEnabled());
	}
}
