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
package org.knime.dl.keras.base.nodes.learner.view.jfreechart;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.SpinnerNumberModel;
import javax.swing.Timer;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.text.DefaultCaret;

import org.jfree.chart.event.AxisChangeEvent;
import org.jfree.chart.event.AxisChangeListener;
import org.jfree.data.Range;
import org.knime.dl.keras.base.nodes.learner.view.DLFloatData;
import org.knime.dl.keras.base.nodes.learner.view.DLView;
import org.knime.dl.keras.base.nodes.learner.view.rangeslider.RangeSlider;

/**
 * DLView containing of a {@link JFreeChartLinePlotPanel} and a textual history
 * view.
 *
 * @author David Kolb, KNIME GmbH, Konstanz, Germany
 */
public class DLJFreeChartLinePlotWithHistoryView implements DLView<DLJFreeChartLinePlotViewSpec> {

	private final JFreeChartLinePlotPanel m_linePlot;
	private final List<JTextArea> m_historyAreas = new ArrayList<>();
	private final List<JLabel> m_currentValueLabels = new ArrayList<>();
	private final float[] m_currentValues;
	private boolean m_isRunning = false;

	private final JPanel m_component;

	private final Timer m_currentValueUpdateTimer = new Timer(1000, (e) -> updateCurrentValueLabels());

	public DLJFreeChartLinePlotWithHistoryView(final DLJFreeChartLinePlotViewSpec plotViewSpec) {
		m_component = new JPanel(new GridBagLayout());

		m_currentValues = new float[plotViewSpec.numPlots()];

		final JTabbedPane historyTabsPane = new JTabbedPane();
		GridBagConstraints gbc;

		for (int i = 0; i < plotViewSpec.numPlots(); i++) {
			final JTextArea historyArea = new JTextArea();
			final DefaultCaret caret = (DefaultCaret) historyArea.getCaret();
			// Enable automatic to bottom scrolling
			caret.setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE);
			historyArea.setEditable(false);
			m_historyAreas.add(historyArea);

			final JScrollPane historyScroller = new JScrollPane(historyArea);
			final JPanel historyWrapper = new JPanel(new GridBagLayout());
			gbc = new GridBagConstraints();
			gbc.gridx = 0;
			gbc.gridy = 0;
			gbc.weightx = 1;
			gbc.weighty = 1;
			gbc.fill = GridBagConstraints.BOTH;
			historyWrapper.add(historyScroller, gbc);

			final JLabel currentValue = new JLabel("-");
			currentValue.setFont(new Font(currentValue.getFont().getName(), currentValue.getFont().getStyle(), 18));

			final JPanel valueWrapperWithBorder = new JPanel(new GridLayout(0, 1));
			valueWrapperWithBorder.setBorder(BorderFactory.createTitledBorder("Current Value:"));
			valueWrapperWithBorder.add(currentValue);
			m_currentValueLabels.add(currentValue);

			gbc.gridy++;
			gbc.weighty = 0;
			gbc.insets = new Insets(10, 10, 10, 10);
			historyWrapper.add(valueWrapperWithBorder, gbc);

			historyTabsPane.addTab(plotViewSpec.getLineLabel(i), historyWrapper);
		}

		gbc = new GridBagConstraints();
		gbc.gridx = 0;
		gbc.gridy = 0;
		gbc.weightx = 1;
		gbc.weighty = 1;
		gbc.insets = new Insets(0, 0, 0, 10);
		gbc.fill = GridBagConstraints.BOTH;
		m_linePlot = new JFreeChartLinePlotPanel(plotViewSpec);
		m_component.add(createPlotWithControlsPanel(m_linePlot), gbc);

		historyTabsPane.setPreferredSize(new Dimension(180, 500));
		historyTabsPane.setMinimumSize(new Dimension(180, 500));
		gbc.gridx = 1;
		gbc.weightx = 0;
		gbc.weighty = 1;
		gbc.fill = GridBagConstraints.VERTICAL;
		m_component.add(historyTabsPane, gbc);
	}

	private Component createPlotWithControlsPanel(Component chartPanel) {
		final JPanel wrapper = new JPanel(new GridBagLayout());

		GridBagConstraints gbc = new GridBagConstraints();
		gbc.gridx = 0;
		gbc.gridy = 0;
		gbc.weighty = 1;
		gbc.weightx = 1;
		gbc.fill = GridBagConstraints.BOTH;
		wrapper.add(chartPanel, gbc);

		gbc.gridy = 1;
		gbc.weighty = 0;
		wrapper.add(createXRangeControls(), gbc);

		gbc.gridy = 2;
		wrapper.add(createSmoothingControls(), gbc);

		return wrapper;
	}

	private Component createSmoothingControls() {
		final JPanel wrapper = new JPanel(new GridBagLayout());

		GridBagConstraints gbc = new GridBagConstraints();
		gbc.gridx = 0;
		gbc.gridy = 0;
		gbc.insets = new Insets(10, 10, 10, 10);

		final JCheckBox enableSmoothingBox = new JCheckBox("Smoothing");
		wrapper.add(enableSmoothingBox, gbc);

		gbc.gridx = 1;
		gbc.insets = new Insets(10, 0, 10, 10);
		final JSpinner smoothingAlphaSpinner = new JSpinner(
				new SpinnerNumberModel(1 - JFreeChartLinePlotPanel.SMOOTHING_ALPHA_DEFAULT, 0.0, 1.0, 0.005));
		smoothingAlphaSpinner.setPreferredSize(new Dimension(80, 25));
		smoothingAlphaSpinner.setEnabled(false);
		wrapper.add(smoothingAlphaSpinner, gbc);

		smoothingAlphaSpinner.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent arg0) {
				double currentSpinnerValue = ((double) smoothingAlphaSpinner.getValue());
				m_linePlot.setSmoothingAlpha(1 - currentSpinnerValue);
				if (!m_isRunning) {
					m_linePlot.triggerSmoothedLinesUpdate();
				}
			}
		});

		enableSmoothingBox.addItemListener(new ItemListener() {
			@Override
			public void itemStateChanged(ItemEvent e) {
				smoothingAlphaSpinner.setEnabled(enableSmoothingBox.isSelected());
				m_linePlot.setEnableSmoothedLines(enableSmoothingBox.isSelected());
				if (!m_isRunning) {
					m_linePlot.triggerSmoothedLinesUpdate();
				}
			}
		});

		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.gridx = 2;
		gbc.weightx = 1;
		wrapper.add(new Box(0), gbc);

		final JPanel border = new JPanel(new GridLayout());
		border.add(wrapper);
		border.setBorder(BorderFactory.createTitledBorder(""));
		return border;
	}

	private Component createXRangeControls() {
		final JPanel wrapper = new JPanel(new GridBagLayout());

		final int sliderMin = 0;
		final int sliderMax = 100;

		final RangeSlider rangeSlider = new RangeSlider();
		rangeSlider.setValue(sliderMin);
		rangeSlider.setUpperValue(sliderMax);

		GridBagConstraints gbc = new GridBagConstraints();
		gbc.gridx = 0;
		gbc.gridy = 0;
		gbc.weightx = 1;
		gbc.insets = new Insets(0, 10, 0, 0);
		gbc.fill = GridBagConstraints.HORIZONTAL;
		wrapper.add(rangeSlider, gbc);

		// TODO prohibit listener trigger hack
		boolean[] hasAxisChanged = new boolean[] { false };
		boolean[] sliderChanged = new boolean[] { false };

		rangeSlider.addChangeListener(new ChangeListener() {
			public void stateChanged(ChangeEvent e) {
				if (hasAxisChanged[0])
					return;
				sliderChanged[0] = true;

				int maxItemCount = getMaxItemCount();
				double lowerBound = (rangeSlider.getValue() / 100.0) * maxItemCount;
				double upperBound = (rangeSlider.getUpperValue() / 100.0) * maxItemCount;

				if (lowerBound < upperBound) {
					m_linePlot.getHorizontalAxis().setRange(lowerBound, upperBound);
					m_linePlot.autoRangeVerticalAxis();
				}

				sliderChanged[0] = false;
			}
		});

		m_linePlot.getHorizontalAxis().addChangeListener(new AxisChangeListener() {
			@Override
			public void axisChanged(AxisChangeEvent event) {
				if (sliderChanged[0])
					return;
				hasAxisChanged[0] = true;

				int maxItemCount = getMaxItemCount();
				Range axisRange = m_linePlot.getHorizontalAxis().getRange();
				int lowerSliderPos = new Double(Math.rint((axisRange.getLowerBound() / maxItemCount) * 100)).intValue();
				int upperSliderPos = new Double(Math.rint((axisRange.getUpperBound() / maxItemCount) * 100)).intValue();
				rangeSlider.setValue(lowerSliderPos);
				rangeSlider.setUpperValue(upperSliderPos);

				hasAxisChanged[0] = false;
			}
		});

		final JButton resetSliderButton = new JButton("Reset");
		resetSliderButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				rangeSlider.setValue(sliderMin);
				rangeSlider.setUpperValue(sliderMax);

				autoRangeYrestoreBoundsX();
			}
		});

		gbc.gridx = 1;
		gbc.weightx = 0;
		gbc.insets = new Insets(0, 10, 5, 10);
		wrapper.add(resetSliderButton, gbc);

		final JPanel border = new JPanel(new GridLayout());
		border.add(wrapper);
		border.setBorder(BorderFactory.createTitledBorder("Horizontal Zoom:"));
		return border;
	}

	private void autoRangeYrestoreBoundsX() {
		m_linePlot.autoRangeVerticalAxis();
		m_linePlot.restoreHorizontalDomainBounds();
	}

	private void updateCurrentValueLabels() {
		for (int i = 0; i < m_currentValues.length; i++) {
			m_currentValueLabels.get(i).setText(m_currentValues[i] + "");
		}
	}

	public void setCurrentValueTimerUpdateDelay(final int miliseconds) {
		m_currentValueUpdateTimer.setDelay(miliseconds);
	}

	public void startCurrentValueUpdate() {
		if (!m_currentValueUpdateTimer.isRunning()) {
			m_currentValueUpdateTimer.start();
		}
	}

	public void stopCurrentValueUpdate() {
		if (m_currentValueUpdateTimer.isRunning()) {
			m_currentValueUpdateTimer.stop();
		}
	}

	@Override
	public Component getComponent() {
		return m_component;
	}

	@Override
	public void update(final DLJFreeChartLinePlotViewSpec spec, final Iterator<DLFloatData>[] iterators) {
		for (int i = 0; i < iterators.length; i++) {
			final Iterator<DLFloatData> it = iterators[i];
			while (it.hasNext()) {
				final float value = it.next().get();
				String lineLabel = spec.getLineLabel(i);
				m_linePlot.plotNext(lineLabel, value);
				m_historyAreas.get(i).append(value + "\n");
				m_currentValues[i] = value;
			}
		}
	}

	private int getMaxItemCount() {
		return m_linePlot.getMaxItemCount();
	}

	public boolean isRunning() {
		return m_isRunning;
	}

	public void setIsRunning(boolean isRunning) {
		m_isRunning = isRunning;
	}
}
