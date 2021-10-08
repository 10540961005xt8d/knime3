/*
 * ------------------------------------------------------------------------
 *
 *  Copyright by KNIME GmbH, Konstanz, Germany
 *  Website: http://www.knime.org; Email: contact@knime.org
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
package org.knime.dl.keras.core.training;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

import org.knime.dl.core.DLLayerDataFactory;
import org.knime.dl.core.DLLayerDataRegistry;
import org.knime.dl.core.training.DLTrainingContext;
import org.knime.dl.keras.core.DLKerasNetwork;
import org.knime.dl.keras.core.DLKerasNetworkType;
import org.knime.dl.keras.core.training.DLKerasLossFunction.DLKerasCosineProximity;
import org.knime.dl.keras.core.training.DLKerasLossFunction.DLKerasKullbackLeiblerDivergence;
import org.knime.dl.keras.core.training.DLKerasLossFunction.DLKerasMeanAbsoluteError;
import org.knime.dl.keras.core.training.DLKerasLossFunction.DLKerasMeanAbsolutePercentageError;
import org.knime.dl.keras.core.training.DLKerasLossFunction.DLKerasMeanSquaredError;
import org.knime.dl.keras.core.training.DLKerasLossFunction.DLKerasMeanSquaredLogarithmicError;
import org.knime.dl.keras.core.training.DLKerasOptimizer.DLKerasRMSProp;
import org.knime.dl.keras.core.training.DLKerasOptimizer.DLKerasStochasticGradientDescent;

/**
 * @author Marcel Wiedenmann, KNIME, Konstanz, Germany
 * @author Christian Dietz, KNIME, Konstanz, Germany
 */
public abstract class DLKerasAbstractTrainingContext<NT extends DLKerasNetworkType<N, ?>, N extends DLKerasNetwork<?>, CFG extends DLKerasTrainingConfig>
		implements DLTrainingContext<N, CFG> {

	private final NT m_networkType;

	private final String m_name;

	private final DLLayerDataFactory m_layerDataFactory;

	private final Collection<DLKerasLossFunction> m_losses;

	private final Collection<DLKerasOptimizer> m_optimizers;

	protected DLKerasAbstractTrainingContext(final NT networkType, final String name) {
		m_networkType = networkType;
		m_name = name;
		m_layerDataFactory = DLLayerDataRegistry.getInstance().getLayerDataFactory(m_networkType)
				.orElseThrow(() -> new IllegalStateException("Deep learning network type '" + m_networkType
						+ "' is not supported. No layer data factory found."));

		// TODO: these will be auto-discovered by SciJava

		m_losses = Arrays.asList( //
				new DLKerasMeanSquaredError(), //
				new DLKerasMeanAbsoluteError(), //
				new DLKerasMeanAbsolutePercentageError(), //
				new DLKerasMeanSquaredLogarithmicError(), //
				new DLKerasKullbackLeiblerDivergence(), //
				new DLKerasCosineProximity());

		m_optimizers = Arrays.asList( //
				new DLKerasRMSProp(), //
				new DLKerasStochasticGradientDescent());
	}

	@Override
	public abstract DLKerasTrainableNetworkAdapter trainable(N network, CFG trainingConfig) throws RuntimeException;

	@Override
	public NT getNetworkType() {
		return m_networkType;
	}

	@Override
	public String getName() {
		return m_name;
	}

	@Override
	public DLLayerDataFactory getLayerDataFactory() {
		return m_layerDataFactory;
	}

	@Override
	public Collection<? extends DLKerasLossFunction> getLossFunctions() {
		return Collections.unmodifiableCollection(m_losses);
	}

	@Override
	public Collection<? extends DLKerasOptimizer> getOptimizers() {
		return Collections.unmodifiableCollection(m_optimizers);
	}
}
