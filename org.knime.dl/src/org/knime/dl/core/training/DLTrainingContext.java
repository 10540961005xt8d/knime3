package org.knime.dl.core.training;

import org.knime.dl.core.DLNetwork;
import org.knime.dl.core.DLOptimizer;
import org.knime.dl.core.modelling.DLLossFunction;

public interface DLTrainingContext<N extends DLNetwork> extends AutoCloseable {

	// TODO:
	// DLNetworkType<N> getNetworkType();

	// String getIdentifier();

	// String getName();

	// DLLayerDataFactory<N> getLayerDataFactory();

	// TODO think!
	DLTrainableNetwork trainable(DLNetwork network, DLLossFunction loss, DLOptimizer opt, int epochs, int params,
			int blah);
}
