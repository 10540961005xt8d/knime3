/*
 * ------------------------------------------------------------------------
 *
 *  Copyright by KNIME GmbH, Konstanz, Germany
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
package org.knime.dl.core;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.Platform;
import org.knime.core.node.NodeLogger;

/**
 * @author Marcel Wiedenmann, KNIME, Konstanz, Germany
 * @author Christian Dietz, KNIME, Konstanz, Germany
 */
public abstract class DLAbstractExtensionPointRegistry {

	protected final NodeLogger m_logger;

	private final String m_extPointId;

	private final String[] m_extPointAttrNames;

	protected DLAbstractExtensionPointRegistry(final String extPointId, final String... extPointAttrNames) {
		m_extPointId = extPointId;
		m_extPointAttrNames = extPointAttrNames;
		m_logger = NodeLogger.getLogger(getClass());
	}

	protected abstract void registerInternal(final IConfigurationElement elem, final Map<String, String> attrs)
			throws Throwable;

	protected void register() {
		try {
			final IExtensionRegistry registry = Platform.getExtensionRegistry();
			final IExtensionPoint point = registry.getExtensionPoint(m_extPointId);
			if (point == null) {
				final String msg = "Invalid extension point: '" + m_extPointId + "'.";
				m_logger.error(msg);
				throw new IllegalStateException(msg);
			}
			outer: for (final IConfigurationElement elem : point.getConfigurationElements()) {
				final String extension = elem.getDeclaringExtension().getUniqueIdentifier();
				final HashMap<String, String> attributes = new HashMap<>(m_extPointAttrNames.length);
				for (final String attrName : m_extPointAttrNames) {
					final String attr = elem.getAttribute(attrName);
					if (attr == null || attr.isEmpty()) {
						m_logger.error("The extension '" + extension + "' doesn't provide the required attribute '"
								+ attrName + "'.");
						m_logger.error("Extension '" + extension + "' was ignored.");
						continue outer;
					}
					attributes.put(attrName, attr);
				}
				try {
					registerInternal(elem, attributes);
				} catch (final Throwable t) {
					m_logger.error("An exception occurred while registering an extension at extension point '"
							+ m_extPointId + "'.", t);
					m_logger.error("Extension '" + extension + "' was ignored.", t);
					continue;
				}
			}
		} catch (final Exception e) {
			m_logger.error(
					"An exception occurred while registering extensions at extension point '" + m_extPointId + "'.", e);
		}
	}
}
