package org.knime.dl.python.core;

import java.io.IOException;

import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.port.PortObjectZipInputStream;
import org.knime.core.node.port.PortObjectZipOutputStream;
import org.knime.core.node.port.PortType;
import org.knime.core.node.port.PortTypeRegistry;
import org.knime.dl.base.portobjects.DLNetworkPortObject;
import org.knime.dl.core.DLInvalidSourceException;
import org.knime.dl.core.DLNetwork;

/**
 * Base interface for all Python deep learning port objects.
 *
 * @author Marcel Wiedenmann, KNIME GmbH, Konstanz, Germany
 * @author Christian Dietz, KNIME GmbH, Konstanz, Germany
 * @param <N> the type of the contained Python network
 */
public interface DLPythonNetworkPortObject<N extends DLNetwork & DLPythonNetwork> extends DLNetworkPortObject {

	/**
	 * The Python deep learning network port type.
	 */
	public static final PortType TYPE = PortTypeRegistry.getInstance().getPortType(DLPythonNetworkPortObject.class);

    /**
     * Only purpose is to make this interface class available to the {@link PortTypeRegistry} via the PorType extension
     * point.
     */
    public static final class DummySerializer extends PortObjectSerializer<DLPythonNetworkPortObject> {
        @Override
        public void savePortObject(final DLPythonNetworkPortObject portObject, final PortObjectZipOutputStream out,
            final ExecutionMonitor exec) throws IOException, CanceledExecutionException {
            throw new UnsupportedOperationException("Don't use this serializer");
        }

        @Override
        public DLPythonNetworkPortObject loadPortObject(final PortObjectZipInputStream in, final PortObjectSpec spec,
            final ExecutionMonitor exec) throws IOException, CanceledExecutionException {
            throw new UnsupportedOperationException("Don't use this serializer");
        }
    }

	@Override
	default String getModelName() {
		return "Python-compatible Deep Learning Network";
	}

	@Override
	N getNetwork() throws DLInvalidSourceException, IOException;
}
