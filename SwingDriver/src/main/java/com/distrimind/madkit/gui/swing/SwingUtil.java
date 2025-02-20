/*
 * Copyright 1997-2013 Fabien Michel, Olivier Gutknecht, Jacques Ferber
 * 
 * This file is part of MaDKit.
 * 
 * MaDKit is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * MaDKit is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with MaDKit. If not, see <http://www.gnu.org/licenses/>.
 */
package com.distrimind.madkit.gui.swing;


import com.distrimind.madkit.gui.MASModel;
import com.distrimind.madkit.gui.swing.action.GUIManagerAction;
import com.distrimind.madkit.gui.swing.action.GlobalAction;
import com.distrimind.madkit.gui.swing.action.KernelAction;
import com.distrimind.madkit.kernel.AbstractAgent;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.MouseWheelListener;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Objects;

/**
 * This class provides some utilities for building swing components.
 * 
 * @author Fabien Michel
 * @since MaDKit 5.0.0.16
 * @version 0.9
 * 
 */
final public class SwingUtil {

	/**
	 * The MaDKit's logo
	 */
	final public static ImageIcon MADKIT_LOGO = new ImageIcon(Objects.requireNonNull(MASModel.class.getResource("images/madkit_logo.png")));
	/**
	 * The MaDKit's logo with a size of 14x14 pixels
	 */
	final public static ImageIcon MADKIT_LOGO_SMALL = new ImageIcon(
			MADKIT_LOGO.getImage().getScaledInstance(14, 14, Image.SCALE_SMOOTH));

	/**
	 * Creates a labeled panel containing a slider with default size.
	 * 
	 * @param slider the slider
	 * @param label the label
	 * @return a panel for the slider
	 */
	public static JPanel createSliderPanel(final JSlider slider, String label) {
		return createSliderPanel(slider, label, 170);
	}

	/**
	 * Creates a labeled panel containing a slider and considering a particular
	 * width
	 * 
	 * @param slider the slider
	 * @param label the label
	 * @param width the width
	 * @return a panel for the slider
	 */
	public static JPanel createSliderPanel(final JSlider slider, String label, int width) {
		final JPanel p = new JPanel();
		p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
		p.setBorder(new TitledBorder(label));
		p.setPreferredSize(new Dimension(width, 60));
		p.add(slider);
		return p;
	}

	/**
	 * Creates a labeled panel containing a slider built using an existing
	 * {@link DefaultBoundedRangeModel}
	 * 
	 * @param model the model
	 * @param label the label
	 * @return a panel for this model
	 */
	public static JPanel createSliderPanel(final BoundedRangeModel model, String label) {
		return createSliderPanel(createJSlider(model), label);
	}

	/**
	 * Creates a JSlider built using a {@link DefaultBoundedRangeModel} and
	 * containing a {@link MouseWheelListener} and some usual default settings
	 * 
	 * @param model the model
	 * @return the corresponding {@link JSlider}
	 */
	public static JSlider createJSlider(final BoundedRangeModel model) {
		final JSlider slider = new JSlider(model);
		slider.addMouseWheelListener(e -> slider.setValue(-e.getWheelRotation() * model.getMaximum() / 100 + model.getValue()));
		slider.setPaintTicks(true);
		slider.setPaintLabels(true);
		return slider;
	}

	/**
	 * Adds to a menu or toolbar the following actions:
	 * <ul>
	 * <li>{@link KernelAction#EXIT}
	 * <li>{@link KernelAction#COPY}
	 * <li>{@link KernelAction#RESTART}
	 * <li>{@link KernelAction#LAUNCH_NETWORK}
	 * <li>{@link KernelAction#STOP_NETWORK}
	 * <li>{@link GUIManagerAction#CONNECT_TO_IP}
	 * <li>{@link GlobalAction#JConsole}
	 * <li>{@link KernelAction#CONSOLE}
	 * <li>{@link GlobalAction#DEBUG}
	 * <li>{@link GlobalAction#LOAD_LOCAL_DEMOS}
	 * <li>{@link GlobalAction#LOAD_JAR_FILE}
	 * <li>{@link GUIManagerAction#ICONIFY_ALL}
	 * <li>{@link GUIManagerAction#DE_ICONIFY_ALL}
	 * <li>{@link GUIManagerAction#KILL_AGENTS}
	 * </ul>
	 * @param menuOrToolBar the menu tool bar
	 * @param agent
	 *            the agent for which this menu will be built.
	 */
	public static void addMaDKitActionsTo(Container menuOrToolBar, AbstractAgent agent) {
		try {// this bypasses class incompatibility
			final Class<? extends Container> componentClass = menuOrToolBar.getClass();
			final Method add = componentClass.getMethod("add", Action.class);
			final Method addSeparator = componentClass.getMethod("addSeparator");

			add.invoke(menuOrToolBar, KernelAction.EXIT.getActionFor(agent));
			addSeparator.invoke(menuOrToolBar);
			add.invoke(menuOrToolBar, KernelAction.COPY.getActionFor(agent));
			add.invoke(menuOrToolBar, KernelAction.RESTART.getActionFor(agent));
			addSeparator.invoke(menuOrToolBar);
			add.invoke(menuOrToolBar, KernelAction.LAUNCH_NETWORK.getActionFor(agent));
			add.invoke(menuOrToolBar, KernelAction.STOP_NETWORK.getActionFor(agent));
			add.invoke(menuOrToolBar, GUIManagerAction.CONNECT_TO_IP.getActionFor(agent));
			addSeparator.invoke(menuOrToolBar);
			if (!(GlobalAction.JConsole == null )) {
				add.invoke(menuOrToolBar, GlobalAction.JConsole);
			}
			add.invoke(menuOrToolBar, KernelAction.CONSOLE.getActionFor(agent));
			addBooleanActionTo(menuOrToolBar, GlobalAction.DEBUG);
			add.invoke(menuOrToolBar, GlobalAction.LOG_FILES);
			addSeparator.invoke(menuOrToolBar);
			add.invoke(menuOrToolBar, GlobalAction.LOAD_LOCAL_DEMOS);
			add.invoke(menuOrToolBar, GlobalAction.LOAD_JAR_FILE);
			addSeparator.invoke(menuOrToolBar);
			add.invoke(menuOrToolBar, GUIManagerAction.ICONIFY_ALL.getActionFor(agent));
			add.invoke(menuOrToolBar, GUIManagerAction.DE_ICONIFY_ALL.getActionFor(agent));
			addSeparator.invoke(menuOrToolBar);
			add.invoke(menuOrToolBar, GUIManagerAction.KILL_AGENTS.getActionFor(agent));
		} catch (IllegalArgumentException | NoSuchMethodException | SecurityException | IllegalAccessException
				| InvocationTargetException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Creates a {@link JCheckBoxMenuItem} for a menu or {@link JToggleButton} for a
	 * tool bar
	 * 
	 * @param menuOrToolBar the menu or tool bar
	 * @param action the action
	 * 
	 */
	public static void addBooleanActionTo(Container menuOrToolBar, Action action) {
		Method addButton;
		try {
			addButton = Container.class.getMethod("add", Component.class);
			if (menuOrToolBar instanceof JMenu) {
				addButton.invoke(menuOrToolBar, new JCheckBoxMenuItem(action));
			} else {
				final JToggleButton jToggleButton = new JToggleButton(action);
				jToggleButton.setText(null);
				addButton.invoke(menuOrToolBar, jToggleButton);
			}
		} catch (NoSuchMethodException | SecurityException | IllegalAccessException | IllegalArgumentException
				| InvocationTargetException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Resizes the icons of all the abstract buttons which are contained in a
	 * container.
	 * 
	 * @param container
	 *            a container containing abstract buttons
	 * @param size
	 *            the size which should be used for the icons
	 */
	public static void scaleAllAbstractButtonIconsOf(Container container, int size) {
		for (final Component c : container.getComponents()) {
			if (c instanceof AbstractButton) {
				final ImageIcon i = (ImageIcon) ((AbstractButton) c).getIcon();
				if (i != null) {
					i.setImage(i.getImage().getScaledInstance(size, size, Image.SCALE_SMOOTH));
				}
			}
		}
	}

}
