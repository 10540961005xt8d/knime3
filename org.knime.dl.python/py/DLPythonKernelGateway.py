# -*- coding: utf-8 -*-

# ------------------------------------------------------------------------
#  Copyright by KNIME AG, Zurich, Switzerland
#  Website: http://www.knime.com; Email: contact@knime.com
#
#  This program is free software; you can redistribute it and/or modify
#  it under the terms of the GNU General Public License, Version 3, as
#  published by the Free Software Foundation.
#
#  This program is distributed in the hope that it will be useful, but
#  WITHOUT ANY WARRANTY; without even the implied warranty of
#  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
#  GNU General Public License for more details.
#
#  You should have received a copy of the GNU General Public License
#  along with this program; if not, see <http://www.gnu.org/licenses>.
#
#  Additional permission under GNU GPL version 3 section 7:
#
#  KNIME interoperates with ECLIPSE solely via ECLIPSE's plug-in APIs.
#  Hence, KNIME and ECLIPSE are both independent programs and are not
#  derived from each other. Should, however, the interpretation of the
#  GNU GPL Version 3 ("License") under any applicable laws result in
#  KNIME and ECLIPSE being a combined program, KNIME AG herewith grants
#  you the additional permission to use and propagate KNIME together with
#  ECLIPSE with only the license terms in place for ECLIPSE applying to
#  ECLIPSE and the GNU GPL Version 3 applying for KNIME, provided the
#  license terms of ECLIPSE themselves allow for the respective use and
#  propagation of ECLIPSE together with KNIME.
#
#  Additional permission relating to nodes for KNIME that extend the Node
#  Extension (and in particular that are based on subclasses of NodeModel,
#  NodeDialog, and NodeView) and that only interoperate with KNIME through
#  standard APIs ("Nodes"):
#  Nodes are deemed to be separate and independent programs and to not be
#  covered works.  Notwithstanding anything to the contrary in the
#  License, the License does not apply to Nodes, you are not required to
#  license Nodes under the License, and you are granted a license to
#  prepare and propagate Nodes, in each case even if such Nodes are
#  propagated with or for interoperation with KNIME.  The owner of a Node
#  may freely choose the license terms applicable to such Node, including
#  when such Node is propagated with or for interoperation with KNIME.
# ------------------------------------------------------------------------

'''
@author Marcel Wiedenmann, KNIME GmbH, Konstanz, Germany
@author Christian Dietz, KNIME GmbH, Konstanz, Germany
'''

import warnings

try:
    import threading
except ImportError:
    warnings.warn("Failed to import threading module. Using dummy_threading instead.")
    import dummy_threading as threading

# pseudo-singleton, will be populated on DL setup
_instance = None


def global_workspace():
    return _instance.global_workspace


def send_to_java(msg):
    return _instance.send_to_java(msg)


class DLPythonKernelGateway(object):
    def __init__(self, workspace, java_send_func):
        assert workspace is not None
        assert java_send_func is not None
        self._workspace = workspace
        self._java_send_func = java_send_func
        self._java_send_lock = threading.Lock()

    @property
    def global_workspace(self):
        return self._workspace

    def send_to_java(self, msg):
        if msg.is_data_request():
            # async requests are not supported by the current messaging implementation of the Python kernel
            with self._java_send_lock:
                return self._java_send_func(msg)
        else:
            # should always return None, but let's stay defensive here
            return self._java_send_func(msg)
