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
 */
package org.knime.dl.keras.core.layers.impl;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.knime.core.node.InvalidSettingsException;
import org.knime.dl.keras.core.layers.DLInvalidTensorSpecException;
import org.knime.dl.keras.core.layers.DLKerasAbstractUnaryLayer;
import org.knime.dl.keras.core.layers.DLKerasConstraint;
import org.knime.dl.keras.core.layers.DLKerasInitializer;
import org.knime.dl.keras.core.layers.DLKerasRegularizer;
import org.knime.dl.keras.core.layers.DLKerasUtiltiyObjectUtils;
import org.knime.dl.keras.core.struct.param.Parameter;
import org.knime.dl.python.util.DLPythonUtils;

/**
 * @author Marcel Wiedenmann, KNIME GmbH, Konstanz, Germany
 * @author Christian Dietz, KNIME GmbH, Konstanz, Germany
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 */
public final class DLKerasEmbeddingLayer extends DLKerasAbstractUnaryLayer {

    @Parameter(label = "Input dimension", min = "1", max = "1000000", stepSize = "1")
    private int m_inputDim;

    @Parameter(label = "Output dimension", min = "0", max = "1000000", stepSize = "1")
    private int m_outputDim;

    @Parameter(label = "Initializer", choices = DLKerasInitializer.DLKerasInitializerChoices.class)
    private DLKerasInitializer m_initializer = new DLKerasInitializer.DLKerasRandomUniformInitializer();

    @Parameter(label = "Embedding regularizer", required = false, choices = DLKerasRegularizer.DLKerasRegularizerChoices.class)
    private DLKerasRegularizer m_embeddingRegularizer = null;
    
    @Parameter(label = "Constraint", required = false, choices = DLKerasConstraint.DLKerasConstraintChoices.class)
    private DLKerasConstraint m_constraint = null;

    @Parameter(label = "Mask zero")
    private boolean m_maskZero = false;

    @Parameter(label = "Input length", required = false)
    private String m_inputLength = null;

    /**
     * Constructor for embedding layers.
     */
    public DLKerasEmbeddingLayer() {
        super("keras.layers.Embedding");
    }

    @Override
    public void validateParameters() throws InvalidSettingsException {
        if (m_inputDim < 1 || m_inputDim > 1000000) {
            throw new InvalidSettingsException("Invalid input dimension: " + m_inputDim);
        }
        if (m_outputDim < 1 || m_outputDim > 1000000) {
            throw new InvalidSettingsException("Invalid input dimension: " + m_inputDim);
        }
        
        m_initializer.validateParameters();
        
        if (m_embeddingRegularizer != null) {
            m_embeddingRegularizer.validateParameters();
        }
        
        if (m_constraint != null) {
            m_constraint.validateParameters();
        }
        
        if (hasInputLength()) {
            Long[] inputLength;
            try {
                inputLength = parseInputLength();
            } catch (NumberFormatException e) {
                throw new InvalidSettingsException("The provided input length is invalid.", e);
            }
            for (Long dim : inputLength) {
                if (dim == null) {
                    throw new InvalidSettingsException("If input length is given it must not contain null dimensions.");
                }
                if (dim < 1) {
                    throw new InvalidSettingsException("Invalid input length dimension: " + dim);
                }
            }
        }
    }

    private boolean hasInputLength() {
        return m_inputLength != null;
    }

    private Long[] parseInputLength() {
        if (hasInputLength()) {
            return DLPythonUtils.parseShape(m_inputLength);
        } else {
            throw new IllegalStateException("The input length parameter is not provided.");
        }
    }

    @Override
    protected void validateInputSpec(final Class<?> inputElementType, final Long[] inputShape)
        throws DLInvalidTensorSpecException {
        if (!int.class.equals(inputElementType)) {
            throw new DLInvalidTensorSpecException("The input to an embedding layer must be of type integer.");
        }
        if (hasInputLength()) {
            checkInputLength(inputShape);
        }
    }

    private void checkInputLength(Long[] inputShape) throws DLInvalidTensorSpecException {
        Long[] inputLength = parseInputLength();
        if (inputLength.length != inputShape.length - 1) {
            throw createInvalidInputShapeException(inputLength, inputShape);
        }
        for (int i = 0; i < inputLength.length; i++) {
            Long l = inputLength[i];
            Long incoming = inputShape[i + 1];
            if (l != null && incoming != null && !l.equals(incoming)) {
                throw createInvalidInputShapeException(inputLength, inputShape);
            }
        }
    }

    private static DLInvalidTensorSpecException createInvalidInputShapeException(Long[] inputLength,
        Long[] inputShape) {
        return new DLInvalidTensorSpecException("'Input length' is " + Arrays.deepToString(inputLength)
            + ", but received input has shape " + Arrays.deepToString(inputShape));
    }

    @Override
    protected Long[] inferOutputShape(final Long[] inputShape) {
        if (!hasInputLength()) {
            return Stream.concat(Arrays.stream(inputShape), Stream.of(m_outputDim)).toArray(Long[]::new);
        }
        Long[] inLength = parseInputLength();
        for (int i = 0; i < inLength.length; i++) {
            if (inLength[i] == 0) {
                inLength[i] = inputShape[i + 1];
            }
        }
        return Stream.concat(Stream.concat(Stream.of(inputShape[0]), Arrays.stream(inLength)), Stream.of(m_outputDim))
            .toArray(Long[]::new);
    }

    @Override
    protected void populateParameters(final List<String> positionalParams, final Map<String, String> namedParams) {
        positionalParams.add(DLPythonUtils.toPython(m_inputDim));
        positionalParams.add(DLPythonUtils.toPython(m_outputDim));
        namedParams.put("embeddings_initializer", DLKerasUtiltiyObjectUtils.toPython(m_initializer));
        namedParams.put("embeddings_regularizer", DLKerasUtiltiyObjectUtils.toPython(m_embeddingRegularizer));
        namedParams.put("embeddings_constraint", DLKerasUtiltiyObjectUtils.toPython(m_constraint));
        namedParams.put("mask_zero", DLPythonUtils.toPython(m_maskZero));
        namedParams.put("input_length", hasInputLength() ? DLPythonUtils.NONE : DLPythonUtils.toPython(parseInputLength()));
    }
}
