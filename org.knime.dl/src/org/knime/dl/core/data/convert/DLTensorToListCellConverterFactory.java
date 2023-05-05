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
package org.knime.dl.core.data.convert;

import static com.google.common.base.Preconditions.checkArgument;

import java.util.ArrayList;
import java.util.OptionalLong;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataType;
import org.knime.core.data.collection.CollectionCellFactory;
import org.knime.core.data.collection.ListCell;
import org.knime.dl.core.DLTensorSpec;
import org.knime.dl.core.data.DLReadableBuffer;
import org.knime.dl.util.DLUtils;

/**
 * @param <I> the input {@link DLReadableBuffer buffer type}
 * @param <OE> the {@link DataCell element type} of the output collection
 * @author Marcel Wiedenmann, KNIME GmbH, Konstanz, Germany
 * @author Christian Dietz, KNIME GmbH, Konstanz, Germany
 */
public final class DLTensorToListCellConverterFactory<I extends DLReadableBuffer, OE extends DataCell>
		implements DLTensorToDataCellConverterFactory<I, ListCell> {
	
	private static final OptionalLong DEST_COUNT = OptionalLong.of(1);

	private final DLTensorToDataCellConverterFactory<I, OE> m_elementConverterFactory;

	/**
	 * @param elementConverterFactory the converter factory that is responsible for converting the elements of the
	 *            output collection
	 */
	public DLTensorToListCellConverterFactory(final DLTensorToDataCellConverterFactory<I, OE> elementConverterFactory) {
		m_elementConverterFactory = elementConverterFactory;
	}

	@Override
	public String getIdentifier() {
		return getClass().getName() + "(" + m_elementConverterFactory.getIdentifier() + ")";
	}

	@Override
	public String getName() {
		return "List of " + m_elementConverterFactory.getName();
	}

	@Override
	public Class<I> getBufferType() {
		return m_elementConverterFactory.getBufferType();
	}

	@Override
	public DataType getDestType() {
		return ListCell.getCollectionType(m_elementConverterFactory.getDestType());
	}

	@Override
	public OptionalLong getDestCount(final DLTensorSpec spec) {
		return DEST_COUNT;
	}

	@Override
	public DLTensorToDataCellConverter<I, ListCell> createConverter() {
		final DLTensorToDataCellConverter<I, OE> elementConverter = m_elementConverterFactory.createConverter();
		return (exec, input, out) -> {
			final ArrayList<OE> temp = new ArrayList<>();
			elementConverter.convert(exec, input, temp::add);
			final long exampleSizeLong = DLUtils.Shapes.getFixedSize(input.getSpec().getShape()).orElseThrow(
					() -> new IllegalArgumentException("Tensor spec does not provide a fully defined shape."));
			checkArgument(exampleSizeLong <= Integer.MAX_VALUE,
					"Invalid example size. Converter only supports sizes up to " + Integer.MAX_VALUE + ".");
			final int exampleSize = (int) exampleSizeLong;
			final int numLists = temp.size() / exampleSize;
			for (int i = 0; i < numLists; i++) {
				out.accept(CollectionCellFactory.createListCell(temp.subList(i * exampleSize, (i + 1) * exampleSize)));
			}
		};
	}

	@Override
	public int hashCode() {
		return m_elementConverterFactory.hashCode() * 37 + getDestType().hashCode();
	}

	@Override
	public boolean equals(final Object obj) {
		if (obj == this) {
			return true;
		}
		if (obj == null || obj.getClass() != getClass()) {
			return false;
		}
		@SuppressWarnings("rawtypes")
		final DLTensorToListCellConverterFactory other = (DLTensorToListCellConverterFactory) obj;
		return other.m_elementConverterFactory.equals(m_elementConverterFactory);
	}
}
