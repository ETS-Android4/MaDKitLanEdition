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
package com.distrimind.madkit.action;

import com.distrimind.madkit.agr.LocalCommunity;
import com.distrimind.madkit.agr.Organization;
import com.distrimind.madkit.i18n.I18nUtilities;
import com.distrimind.madkit.kernel.AbstractAgent;
import com.distrimind.madkit.kernel.network.AskForConnectionMessage;
import com.distrimind.madkit.kernel.network.AskForTransferMessage;
import com.distrimind.madkit.message.KernelMessage;

import java.util.ResourceBundle;


/**
 * Enum representing kernel actions. This especially could be used to
 * communicate with the kernel in order to trigger kernel's actions.
 * 
 * @author Fabien Michel
 * @author Jason Mahdjoub
 * @since MaDKit 5.0.0.14
 * @version 0.93
 * 
 */

public enum KernelAction {

	/**
	 * Close the kernel
	 */
	EXIT,
	/**
	 * Clone the kernel with its initial options
	 */
	COPY,
	/**
	 * Restart the kernel with its initial options
	 */
	RESTART,
	/**
	 * Start the network
	 */
	LAUNCH_NETWORK,
	/**
	 * Stop the network
	 */
	STOP_NETWORK,

	/**
	 * Makes a redirection of the out and err to a MaDKit agent.
	 */
	CONSOLE,

	// //Actions that need parameters, i.e. not global
	/**
	 * Launch an agent
	 */
	LAUNCH_AGENT,
	/**
	 * Launch a MAS configuration
	 */
	LAUNCH_MAS,
	/**
	 * Launch an XML configuration
	 */
	LAUNCH_XML,
	/**
	 * Launch an XML configuration
	 */
	LAUNCH_YAML,
	/**
	 * Kill an agent
	 */
	KILL_AGENT,
	/**
	 * Connection to the MaDKit web repository
	 */
	CONNECT_WEB_REPO,
	/**
	 * For connecting/disconnecting two kernels directly in a wide area network. It
	 * requires a parameter of type {@link AskForConnectionMessage}.
	 */
	MANAGE_DIRECT_CONNECTION,

	/**
	 * For connecting/disconnecting two kernels indirectly by making data transferred
	 * by the current kernel to constitute a meshed network. It requires a parameter
	 * of type {@link AskForTransferMessage}.
	 */
	MANAGE_TRANSFER_CONNECTION,

	/**
	 * Cancel a programmed execution of a task which would be executed by the
	 * TaskAgent identified by the given name.
	 */
	CANCEL_TASK,

	/**
	 * Add a new task to be executed at a specific time by the task agent which
	 * correspond to the given task agent name.
	 */
	SCHEDULE_TASK,

	/**
	 * Add a new collection of tasks to be executed at a specific time by the task
	 * agent which correspond to the given task agent name.
	 */
	SCHEDULE_TASKS,

	/**
	 * Kill a Task Manager Agent
	 */
	KILL_TASK_MANAGER_AGENT,

	/**
	 * Launch a Task Manager Agent
	 */
	LAUNCH_TASK_MANAGER_AGENT,

	/**
	 * Set threads priority related to a specific Task Manager Agent
	 */
	SET_TASK_MANAGER_AGENT_PRIORITY;


	/**
	 * Builds an action that will make the kernel do the corresponding operation if
	 * possible.
	 *
	 * @param agent
	 *            the agent that will send the message to the kernel
	 * @param parameters
	 *            the info
	 * @return the new corresponding action
	 */
	public Action getActionFor(final AbstractAgent agent, final Object... parameters) {
		return new Action(this, () -> {
				if (agent.isAlive()) {
					agent.sendMessage(LocalCommunity.Groups.SYSTEM, Organization.GROUP_MANAGER_ROLE,
							new KernelMessage(KernelAction.this, parameters));// TODO work with AA but this is probably
					// worthless
				}

			});
	}

	final static private ResourceBundle messages = I18nUtilities.getResourceBundle(KernelAction.class.getSimpleName());

	public static ResourceBundle getMessages()
	{
		return messages;
	}

}
