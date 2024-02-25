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
 *   Jun 26, 2017 (marcel): created
 */
package org.knime.dl.python.core;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.knime.core.node.NodeLogger;
import org.knime.dl.core.DLInvalidEnvironmentException;
import org.knime.dl.core.DLNetworkInputProvider;
import org.knime.dl.core.DLTensor;
import org.knime.dl.core.DLTensorId;
import org.knime.dl.core.data.DLReadableBuffer;
import org.knime.dl.core.data.DLWritableBuffer;
import org.knime.dl.core.training.DLMetrics;
import org.knime.dl.core.training.DLTrainingMonitor;
import org.knime.dl.core.training.DLTrainingStatus.Status;
import org.knime.dl.python.core.data.DLPythonDataBuffer;
import org.knime.dl.python.core.data.serde.DLPythonDeserializer;
import org.knime.dl.python.core.data.serde.DLPythonDeserializerFactory;
import org.knime.dl.python.core.data.serde.DLSerializerFactory;
import org.knime.dl.python.util.DLPythonSourceCodeBuilder;
import org.knime.dl.python.util.DLPythonUtils;
import org.knime.dl.util.DLUtils;
import org.knime.python.typeextension.Deserializer;
import org.knime.python.typeextension.DeserializerFactory;
import org.knime.python.typeextension.KnimeToPythonExtension;
import org.knime.python.typeextension.KnimeToPythonExtensions;
import org.knime.python.typeextension.PythonToKnimeExtensions;
import org.knime.python.typeextension.Serializer;
import org.knime.python2.extensions.serializationlibrary.interfaces.Cell;
import org.knime.python2.extensions.serializationlibrary.interfaces.Row;
import org.knime.python2.extensions.serializationlibrary.interfaces.TableChunker;
import org.knime.python2.extensions.serializationlibrary.interfaces.TableCreator;
import org.knime.python2.extensions.serializationlibrary.interfaces.TableSpec;
import org.knime.python2.extensions.serializationlibrary.interfaces.Type;
import org.knime.python2.extensions.serializationlibrary.interfaces.impl.CellImpl;
import org.knime.python2.extensions.serializationlibrary.interfaces.impl.KeyValueTableIterator;
import org.knime.python2.extensions.serializationlibrary.interfaces.impl.RowImpl;
import org.knime.python2.extensions.serializationlibrary.interfaces.impl.TableSpecImpl;
import org.knime.python2.kernel.AbstractPythonToJavaMessageHandler;
import org.knime.python2.kernel.DefaultJavaToPythonResponse;
import org.knime.python2.kernel.Messages;
import org.knime.python2.kernel.PythonToJavaMessage;

import com.google.common.collect.Sets;
import com.google.common.primitives.UnsignedBytes;

/**
 * @author Marcel Wiedenmann, KNIME GmbH, Konstanz, Germany
 * @author Christian Dietz, KNIME GmbH, Konstanz, Germany
 */
public abstract class DLPythonAbstractCommands implements DLPythonCommands {

	// String constants that are used on Python side:

	public static final String DEFAULT_MODEL_NAME = "model";

	public static final String INPUT_SPECS_NAME = "input_specs";

	public static final String HIDDEN_OUTPUT_SPECS_NAME = "intermediate_output_specs";

	public static final String OUTPUT_SPECS_NAME = "output_specs";

	public static final String OPTIMIZER_SPECS = "optimizer_specs";

	public static final String LOSS_SPECS = "loss_specs";

	public static final String METRICS_SPECS = "metrics_specs";

	public static final String INPUT_TABLE_NAME = "input_table";

	public static final String OUTPUT_TABLE_NAME = "output_table";

	public static final String OUTPUT_SHAPES_NAME = "output_shapes";

	private static final String INSTALLATION_TEST_OK_MSG = "[DL Python installation test: OK]";

	private static final String INSTALLATION_TEST_FAIL_MSG = "[DL Python installation test: FAIL]";
	
	// --

	private static final NodeLogger LOGGER = NodeLogger.getLogger(DLPythonAbstractCommands.class);

	/**
	 * Methods that require a properly setup Python environment should not access this field directly. Instead, they
	 * should use {@link #getContext()}.
	 */
	protected final DLPythonContext m_context;

	/**
	 * Set to <code>true</code> if the setup steps in {@link #getContext()} were successful.
	 */
	private boolean m_contextSetup = false;

	/**
	 * Creates a new instance of this commands class.
	 */
	protected DLPythonAbstractCommands() {
		this(new DLPythonDefaultContext());
	}

	/**
	 * Creates a new instance of this commands class that uses the given context to communicate with Python.
	 *
	 * @param context the Python context
	 */
	protected DLPythonAbstractCommands(final DLPythonContext context) {
		m_context = context;
	}

	protected abstract String getSetupEnvironmentCode();

	protected abstract File getInstallationTestFile() throws IOException;

	protected abstract String getSetupBackendCode();

	protected abstract String getLoadNetworkCode(String path, boolean loadTrainingConfig);

	@Override
	public final synchronized DLPythonContext getContext() throws DLInvalidEnvironmentException {
		if (!m_contextSetup) {
			// setup Python process environment
			try {
				final String error = m_context.getKernel().execute(getSetupEnvironmentCode())[1];
				if (!error.isEmpty()) {
					throw new DLInvalidEnvironmentException(
							"Deep learning Python back end environment could not be set up.\nCause: " + error);
				}
			} catch (final IOException e) {
				throw new DLInvalidEnvironmentException("An error occurred while communicating with Python "
						+ "(while setting up the Python back end environment)."
						+ (e.getMessage() != null ? "\nCause: " + e.getMessage() : ""), e);
			}
			// register all back ends
			try {
				final String error = m_context.getKernel()
						.execute(DLPythonNetworkLoaderRegistry.getInstance().getAllNetworkLoaders() //
								.stream() //
								.map(nl -> "import " + nl.getPythonModuleName() + "\n") //
								.collect(Collectors.joining()))[1];
				if (!error.isEmpty()) {
					throw new DLInvalidEnvironmentException(
							"Deep learning Python back ends could not be registered.\nCause: " + error);
				}
			} catch (final IOException e) {
				throw new DLInvalidEnvironmentException(
						"An error occurred while communicating with Python (while registering the Python back ends)."
								+ (e.getMessage() != null ? "\nCause: " + e.getMessage() : ""),
						e);
			}
			// setup the actual back end
			try {
				final String error = m_context.getKernel().execute(getSetupBackendCode())[1];
				if (!error.isEmpty()) {
					throw new DLInvalidEnvironmentException(
							"Deep learning Python back end could not be set up.\nCause: " + error);
				}
			} catch (final IOException e) {
				throw new DLInvalidEnvironmentException(
						"An error occurred while communicating with Python (while setting up the Python back end)."
								+ (e.getMessage() != null ? "\nCause: " + e.getMessage() : ""),
						e);
			}

			m_contextSetup = true;
		}
		return m_context;
	}

	@Override
	public synchronized void testInstallation() throws DLInvalidEnvironmentException {
		try {
			final File script = getInstallationTestFile();
			final String[] output = m_context.isKernelOpen()
					? m_context.getKernel().execute(DLUtils.Files.readAllUTF8(script))
					: m_context.execute(script);
			if (!output[0].contains(INSTALLATION_TEST_OK_MSG)) {
				final int idx = output[0].indexOf(INSTALLATION_TEST_FAIL_MSG);
				final String cause = idx != -1 //
						? "\nCause: " + output[0].substring(idx + INSTALLATION_TEST_FAIL_MSG.length())
						: "";
				final String further = !output[1].isEmpty() ? "\nFurther output: " + output[1] : "";
				if (!cause.isEmpty()) {
					throw new DLInvalidEnvironmentException(
							"Deep learning Python back end installation tests failed." + cause + further);
				} else {
					throw new DLInvalidEnvironmentException(
							"Deep learning Python back end installation tests failed for unknown reasons." + further);
				}
			}
		} catch (final IOException e) {
			throw new DLInvalidEnvironmentException("An error occurred while communicating with Python "
					+ "(while testing the installation of the Python back end)."
					+ (e.getMessage() != null ? "\nCause: " + e.getMessage() : ""), e);
		}
	}

	@Override
	public DLPythonNetworkHandle loadNetwork(final String path, final boolean loadTrainingConfig)
			throws DLInvalidEnvironmentException, IOException {
		getContext().executeInKernel(getLoadNetworkCode(path, loadTrainingConfig));
		// TODO: we should get the model name (= network identifier) from Python
		return new DLPythonNetworkHandle(DEFAULT_MODEL_NAME);
	}

	@Override
	public void saveNetwork(final DLPythonNetworkHandle network, final String path)
			throws DLInvalidEnvironmentException, IOException {
		final DLPythonSourceCodeBuilder b = DLPythonUtils.createSourceCodeBuilder() //
				.a("import DLPythonNetwork") //
				.n("network = DLPythonNetwork.get_network(").as(network.getIdentifier()).a(")") //
				.n("network.save(").asr(path).a(")");
		getContext().executeInKernel(b.toString());
	}

	// TODO: implement network handle
	@Override
	public void setNetworkInputs(final DLPythonNetworkHandle network,
			final Map<? extends DLTensorId, ? extends DLTensor<? extends DLWritableBuffer>> inputs)
			throws DLInvalidEnvironmentException, IOException {
		for (final Entry<? extends DLTensorId, ? extends DLTensor<? extends DLWritableBuffer>> input : inputs
				.entrySet()) {
			final DLTensorId tensorIdentifier = input.getKey();
			final DLTensor<? extends DLWritableBuffer> tensor = input.getValue();
			final TableChunker tableChunker = createSingleTensorTableChunker(tensor);
			try {
				getContext().getKernel().putData(tensorIdentifier.getIdentifierString(), tableChunker, 1);
			} catch (final IOException ex) {
				throw new RuntimeException("Transmitting input data to Python failed.", ex);
			}
		}
	}

	@Override
	public void executeNetwork(final DLPythonNetworkHandle network, final Set<? extends DLTensorId> requestedOutputs,
			final long batchSize) throws DLInvalidEnvironmentException, IOException {
		final DLPythonSourceCodeBuilder b = DLPythonUtils.createSourceCodeBuilder() //
				.a("import DLPythonNetwork") //
				.n("network = DLPythonNetwork.get_network(").as(network.getIdentifier()).a(")") //
				.n("in_data = {}") //
				.n("for input_spec in network.spec.input_specs:") //
				.n().t().a("in_data[input_spec.identifier] = globals()[input_spec.identifier]") //
				.n("out_data = network.execute(in_data, ").a(batchSize).a(")") //
				.n("import pandas as pd") //
				.n("output_shapes = {}") //
				.n("for name, data in out_data.items():") //
				.n().t().a("shape = [list(data.iloc[0][0].array.shape)]").n().t()
				.a("output_shapes[name] = [-1 if d is None else d for d in shape]") // replace None with -1
				.n().t().a("globals()[name] = data").n("globals()[").as(OUTPUT_SHAPES_NAME)
				.a("] = pd.DataFrame(output_shapes)");
		getContext().executeInKernel(b.toString());
	}

	@Override
	public <T extends DLTensorId> Map<T, long[]> getNetworkOutputShapes(final DLPythonNetworkHandle network,
			final Set<T> outputs) throws DLInvalidEnvironmentException, IOException {
		final Map<T, long[]> shapes = new HashMap<>(outputs.size());
		final Map<String, T> idMap = outputs.stream()
				.collect(Collectors.toMap(DLTensorId::getIdentifierString, Function.identity()));
		getContext().getKernel().getData(OUTPUT_SHAPES_NAME, (tableSpec, tableSize) -> new TableCreator<Object>() {

			@Override
			public void addRow(final Row row) {
				final String[] tensorNames = tableSpec.getColumnNames();
				for (int i = 0; i < tensorNames.length; i++) {
					final Cell shapeCell = row.getCell(i);
					try {
						final int[] intShape = shapeCell.getIntegerArrayValue();
						shapes.put(idMap.get(tensorNames[i]), Arrays.stream(intShape).mapToLong(d -> d).toArray());
					} catch (final IllegalStateException e) {
						LOGGER.error(
								"An exception occurred while collecting output shapes from Python: " + e.getMessage(),
								e);
					}
				}
			}

			@Override
			public TableSpec getTableSpec() {
				return tableSpec;
			}

			@Override
			public Object getTable() {
				return null;
			}

		});
		// ensure that we have a shape for each output tensor
		if (shapes.size() != outputs.size()) {
			throw new IllegalStateException(
					"Python didn't return a shape for each output. The shape is missing for outputs "
							+ Sets.difference(outputs, shapes.keySet()) + ".");
		}
		return shapes;
	}

	// TODO: implement network handle
	@Override
	public void getNetworkOutputs(final DLPythonNetworkHandle network,
			final Map<? extends DLTensorId, ? extends DLTensor<? extends DLReadableBuffer>> outputs)
			throws DLInvalidEnvironmentException, IOException {
		for (final Entry<? extends DLTensorId, ? extends DLTensor<? extends DLReadableBuffer>> output : outputs
				.entrySet()) {
			final DLTensorId tensorIdentifier = output.getKey();
			final DLTensor<? extends DLReadableBuffer> tensor = output.getValue();

			getContext().getKernel().getData(tensorIdentifier.getIdentifierString(),
					(tableSpec, tableSize) -> new TableCreator<DLTensor<? extends DLReadableBuffer>>() {

						@Override
						public void addRow(final Row row) {
							final String deserializerId = tableSpec.getColumnSerializers()
									.get(tensorIdentifier.getIdentifierString());
							final DeserializerFactory deserializerFactory = PythonToKnimeExtensions
									.getExtension(deserializerId).getJavaDeserializerFactory();
							if (!(deserializerFactory instanceof DLPythonDeserializerFactory)) {
								LOGGER.coding(
										"Deep learning Python to KNIME serialization factory must implement DLSerializerFactory.");
							}
							final Deserializer deserializer = deserializerFactory.createDeserializer();
							if (!(deserializer instanceof DLPythonDeserializer)) {
								final String msg = "An exception occurred while collecting network output from Python. Unsupported deserializer.";
								LOGGER.error(msg);
								// TODO
								throw new RuntimeException(msg);
							}
							final Cell cell = row.getCell(0);
							try {
								((DLPythonDeserializer) deserializer).deserialize(cell.getBytesValue(), tensor);
							} catch (final IllegalStateException e) {
								LOGGER.error("An exception occurred while collecting network output from Python: "
										+ e.getMessage(), e);
							}
						}

						@Override
						public TableSpec getTableSpec() {
							return tableSpec;
						}

						@Override
						public DLTensor<? extends DLReadableBuffer> getTable() {
							return tensor;
						}
					});
		}
	}

	@Override
	public void trainNetwork(final DLPythonNetworkHandle network, final DLNetworkInputProvider inputSupplier,
			final DLTrainingMonitor<?> monitor) throws DLInvalidEnvironmentException, IOException {
		final Messages messages = getContext().getKernel().getMessages();
		final AbstractPythonToJavaMessageHandler dataRequestHandler = new AbstractPythonToJavaMessageHandler(
				"request_training_data") {

			@Override
			protected void handle(final PythonToJavaMessage msg) throws Exception {
				final long batchIndex = Long.parseLong(msg.getValue());
				final Map<DLTensorId, DLTensor<? extends DLWritableBuffer>> input = inputSupplier.get(batchIndex);
				for (final Entry<DLTensorId, DLTensor<? extends DLWritableBuffer>> entry : input.entrySet()) {
					final DLTensor<? extends DLWritableBuffer> tensor = entry.getValue();
					final TableChunker tableChunker = createSingleTensorTableChunker(tensor);
					try {
						getContext().getKernel().putData(entry.getKey().getIdentifierString(), tableChunker, 1);
					} catch (final IOException ex) {
						throw new IOException("Transmitting data to Python failed.", ex);
					} finally {
						tensor.getBuffer().reset();
					}
				}
				messages.answer(new DefaultJavaToPythonResponse(msg, ""));
			}
		};
		messages.registerMessageHandler(dataRequestHandler);

		final LinkedHashMap<String, DLMetrics> metrics = new LinkedHashMap<>(4);
		metrics.put("accuracy", new DLMetrics("accuracy", 0f));
		metrics.put("loss", new DLMetrics("loss", 0f));
		// metrics.put("val_accuracy", new DLMetrics("val_accuracy", 0f));
		// metrics.put("val_loss", new DLMetrics("val_loss", 0f));
		monitor.getTrainingStatus().setMetrics(metrics);

		final AbstractPythonToJavaMessageHandler onBatchEndHandler = new AbstractPythonToJavaMessageHandler(
				"batch_end") {

			@Override
			protected void handle(final PythonToJavaMessage msg) throws Exception {
				final String[] metricsStr = msg.getValue().split(";");
				int i = 0;
				for (final DLMetrics m : metrics.values()) {
					try {
						m.setValue(Float.parseFloat(metricsStr[i]));
					} catch (final NumberFormatException e) {
						m.setValue(-1f);
						LOGGER.debug("Received invalid value for metric '" + m.getName() + "': " + m.getValue() + ".");
					}
					i++;
				}
				messages.answer(new DefaultJavaToPythonResponse(msg,
						monitor.getTrainingStatus().getStatus() == Status.RUNNING ? "c" : "s"));
				monitor.getTrainingStatus().batchEnded().raise(null);
			}
		};
		messages.registerMessageHandler(onBatchEndHandler);

		final DLPythonSourceCodeBuilder b = DLPythonUtils.createSourceCodeBuilder() //
				.a("import DLPythonNetwork") //
				.n("network = DLPythonNetwork.get_network(").as(network.getIdentifier()).a(")") //
				.n("from DLPythonKernelService import DLPythonKernelService") //
				.n("kernel_service = DLPythonKernelService(globals(), request_from_java)") //
				.n("from DLKerasNetwork import DLKerasNetworkInputBatchGenerator") //
				.n("data_supplier = DLKerasNetworkInputBatchGenerator(network, ").a(inputSupplier.getNumBatches()) //
				.a(", network.spec.training_config.batch_size, kernel_service)") //
				.n("network.train(data_supplier)");
		monitor.getTrainingStatus().trainingStarted().raise(null);
		getContext().executeInKernel(b.toString());

		messages.unregisterMessageHandler(dataRequestHandler);
		messages.unregisterMessageHandler(onBatchEndHandler);

		monitor.getTrainingStatus().trainingEnded().raise(null);
	}

	@Override
	public void getTrainingResults(final DLPythonNetworkHandle network) {
		// TODO
	}

	/**
	 * Closes the underlying {@link DLPythonContext Python context}.
	 */
	@Override
	public synchronized void close() throws Exception {
		m_context.close();
	}

	protected String getExtractNetworkSpecsCode(final DLPythonNetworkHandle network) {
		return "import DLPythonNetworkSpecExtractor\n" + //
				"global " + INPUT_SPECS_NAME + "\n" + //
				"global " + HIDDEN_OUTPUT_SPECS_NAME + "\n" + //
				"global " + OUTPUT_SPECS_NAME + "\n" + //
				INPUT_SPECS_NAME + ", " + HIDDEN_OUTPUT_SPECS_NAME + ", " + OUTPUT_SPECS_NAME + ", " + //
				OPTIMIZER_SPECS + ", " + LOSS_SPECS + ", " + METRICS_SPECS + " = " + //
				"DLPythonNetworkSpecExtractor.get_layer_data_specs_as_data_frame('" + network.getIdentifier() + "')";
	}

	private TableChunker createSingleTensorTableChunker(final DLTensor<? extends DLWritableBuffer> tensor)
			throws IOException {
		final KnimeToPythonExtension extension = KnimeToPythonExtensions.getExtensions().stream()
				.filter(ext -> (ext.getJavaSerializerFactory() instanceof DLSerializerFactory)
						&& ((DLSerializerFactory) ext.getJavaSerializerFactory()).getBufferType()
								.isAssignableFrom(tensor.getBuffer().getClass()))
				.findFirst() //
				.orElseThrow(() -> new RuntimeException(
						"Transmitting data to Python failed. No matching serializer available."));
		// TODO: if nothing found, we should also try to match primitive types with their wrapper types (guava
		// Primitives.wrap etc.)
		final Serializer<DLPythonDataBuffer> serializer = (Serializer<DLPythonDataBuffer>) extension
				.getJavaSerializerFactory().createSerializer();
		final Cell cell = new CellImpl(serializer.serialize((DLPythonDataBuffer) tensor.getBuffer()));
		final long[] shape = DLUtils.Shapes.getFixedShape(tensor.getSpec().getShape()).orElseThrow(
				() -> new IllegalStateException("Execution spec does not contain fixed shape."));
		final Cell shapeCell = new CellImpl(shape, getNotMissingForLength(shape.length));
		final String identifier = tensor.getSpec().getIdentifier().getIdentifierString();
		final TableSpec tableSpec = new TableSpecImpl(new Type[] { Type.BYTES, Type.LONG_LIST }, new String[] { identifier, "shape" },
				Collections.singletonMap(identifier, extension.getId()));
		final Row row = new RowImpl(identifier, 2);
		row.setCell(cell, 0);
		row.setCell(shapeCell, 1);
		final KeyValueTableIterator iterator = new KeyValueTableIterator(tableSpec, row);
		return new DLSingletonTableChunker(iterator);
	}
	
	private byte[] getNotMissingForLength(int length) {
		int entries = length / 8 + 1;
		byte[] missings = new byte[entries];
		Arrays.fill(missings, UnsignedBytes.MAX_VALUE);
		return missings;
	}
}
