/*
 * Copyright or © or Copr. Fabien Michel, Olivier Gutknecht, Jacques Ferber (1997)
 * 
 * fmichel@lirmm.fr
 * olg@no-distance.net
 * ferber@lirmm.fr
 * 
 * This software is a computer program whose purpose is to
 * provide a lightweight Java library for designing and simulating Multi-Agent Systems (MAS).
 * This software is governed by the CeCILL-C license under French law and
 * abiding by the rules of distribution of free software.  You can  use,
 * modify and/ or redistribute the software under the terms of the CeCILL-C
 * license as circulated by CEA, CNRS and INRIA at the following URL
 * "http://www.cecill.info".
 * As a counterpart to the access to the source code and  rights to copy,
 * modify and redistribute granted by the license, users are provided only
 * with a limited warranty  and the software's author,  the holder of the
 * economic rights,  and the successive licensors  have only  limited
 * liability.
 * 
 * In this respect, the user's attention is drawn to the risks associated
 * with loading,  using,  modifying and/or developing or reproducing the
 * software by the user in light of its specific status of free software,
 * that may mean  that it is complicated to manipulate,  and  that  also
 * therefore means  that it is reserved for developers  and  experienced
 * professionals having in-depth computer knowledge. Users are therefore
 * encouraged to load and test the software's suitability as regards their
 * requirements in conditions enabling the security of their systems and/or
 * data to be ensured and,  more generally, to use and operate it in the
 * same conditions as regards security.
 * The fact that you are presently reading this means that you have had
 * knowledge of the CeCILL-C license and that you accept its terms.
 */
package com.distrimind.madkit.gui.swing.toolbar;

import com.distrimind.madkit.action.KernelAction;
import com.distrimind.madkit.gui.swing.SwingUtil;
import com.distrimind.madkit.kernel.AbstractAgent;
import com.distrimind.madkit.gui.swing.action.GUIManagerAction;
import com.distrimind.madkit.gui.swing.action.GlobalAction;

import javax.swing.*;

/**
 * An out of the box toolbar for MaDKit based applications.
 * 
 * @author Fabien Michel
 * @since MaDKit 5.0.0.9
 * @version 0.92
 * 
 */
public class MadkitToolBar extends JToolBar {

	/**
	 * Creates a {@link JToolBar} featuring:
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
	 * 
	 * @param agent
	 *            the agent for which this menu is created
	 */
	public MadkitToolBar(final AbstractAgent agent) {
		super("MaDKit");
		SwingUtil.addMaDKitActionsTo(this, agent);
		SwingUtil.scaleAllAbstractButtonIconsOf(this, 20);
	}
}
