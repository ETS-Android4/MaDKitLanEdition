/*
 * MadKitLanEdition (created by Jason MAHDJOUB (jason.mahdjoub@distri-mind.fr)) Copyright (c)
 * 2015 is a fork of MadKit and MadKitGroupExtension. 
 * 
 * Copyright or © or Copr. Jason Mahdjoub, Fabien Michel, Olivier Gutknecht, Jacques Ferber (1997)
 * 
 * jason.mahdjoub@distri-mind.fr
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
package com.distrimind.madkit.gui;

import java.awt.BorderLayout;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.Box;
import javax.swing.JFrame;
import javax.swing.JInternalFrame;
import javax.swing.JMenuBar;
import javax.swing.JToolBar;
import javax.swing.WindowConstants;

import com.distrimind.madkit.action.KernelAction;
import com.distrimind.madkit.agr.LocalCommunity;
import com.distrimind.madkit.agr.Organization;
import com.distrimind.madkit.gui.menu.AgentLogLevelMenu;
import com.distrimind.madkit.gui.menu.AgentMenu;
import com.distrimind.madkit.gui.menu.HelpMenu;
import com.distrimind.madkit.gui.menu.MadkitMenu;
import com.distrimind.madkit.kernel.AbstractAgent;
import com.distrimind.madkit.message.KernelMessage;

/**
 * The default frame which is used for the agents in the GUI engine of MaDKit.
 * Subclasses could be defined to obtain customized frames.
 * 
 * @author Fabien Michel
 * @author Jason Mahdjoub
 * @since MaDKitLanEdition 1.0
 * @version 0.92
 * 
 */
public class AgentFrame extends JFrame {

	/**
	 * 
	 */
	private static final long serialVersionUID = 6337250099157352055L;
	private JInternalFrame internalFrame;
	private final AbstractAgent agent;

	/**
	 * TThis constructor is protected because this class should not be directly
	 * instantiated as it is used by the MaDKit GUI manager.
	 * 
	 * @param agent
	 *            the considered agent
	 */
	protected AgentFrame(final AbstractAgent agent) {
		super(agent.getName());
		this.agent = agent;
		setIconImage(SwingUtil.MADKIT_LOGO.getImage());
		setJMenuBar(createMenuBar());
		JToolBar tb = createJToolBar();
		if (tb != null) {
			add(tb, BorderLayout.PAGE_START);
		}
		setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
		addWindowListener(new WindowAdapter() {

			@Override
			public void windowClosed(WindowEvent e) {
				closeProcess();
			}

			private void closeProcess() {
				if (agent.isAlive()) {
					setTitle("Closing " + agent.getName());
					killAgent(agent, 4);
				}
			}

			public void windowClosing(java.awt.event.WindowEvent e) {
				closeProcess();
			}
		});
		setSize(400, 300);
		setLocationRelativeTo(null);
	}

	@Override
	public void dispose() {
		if (internalFrame != null) {
			internalFrame.setVisible(false);
			internalFrame.dispose();
		}
		super.dispose();
	}

	/**
	 * Builds the menu bar that will be used for this frame. By default it creates a
	 * {@link JMenuBar} featuring:
	 * <ul>
	 * <li>{@link MadkitMenu}
	 * <li>{@link AgentMenu}
	 * <li>{@link AgentLogLevelMenu}
	 * <li>{@link HelpMenu}
	 * <li>{@link AgentStatusPanel}
	 * </ul>
	 * 
	 * @return a menu bar
	 */
	public JMenuBar createMenuBar() {
		JMenuBar menuBar = new JMenuBar();
		menuBar.add(new MadkitMenu(agent));
		menuBar.add(new AgentMenu(agent));
		menuBar.add(new AgentLogLevelMenu(agent));
		menuBar.add(new HelpMenu());
		menuBar.add(Box.createHorizontalGlue());
		menuBar.add(new AgentStatusPanel(agent));
		return menuBar;
	}

	/**
	 * Builds the tool bar that will be used. By default, it returns
	 * <code>null</code> so that there is no toll bar in the default agent frames.
	 * 
	 * @return a tool bar
	 */
	public JToolBar createJToolBar() {
		return null;
	}

	/**
	 * @param internalFrame
	 *            the internalFrame to set
	 */
	void setInternalFrame(JInternalFrame internalFrame) {
		this.internalFrame = internalFrame;
	}

	@Override
	public void setLocation(int x, int y) {
		super.setLocation(x, y);
		if (internalFrame != null) {
			internalFrame.setLocation(x, y);
		}
	}

	@Override
	public void pack() {
		super.pack();
		if (internalFrame != null) {
			internalFrame.pack();
		}
	}


	static void killAgent(final AbstractAgent agent, int timeOutSeconds) {// TODO move that
		if (agent.isAlive()) {
			agent.sendMessage(LocalCommunity.Groups.SYSTEM, Organization.GROUP_MANAGER_ROLE,
					new KernelMessage(KernelAction.KILL_AGENT, agent, timeOutSeconds));
		}
	}

	/**
	 * @return the agent for which this frame has been created.
	 */
	public AbstractAgent getAgent() {
		return agent;
	}

	/**
	 * Override to customize the agent frame that should be created by the GUI
	 * engine.
	 * 
	 * @param agent
	 *            the related agent
	 * @return the created frame
	 */
	public static AgentFrame createAgentFrame(final AbstractAgent agent) {
		return new AgentFrame(agent);
	}
}
