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
package com.distrimind.madkit.kernel;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;

import com.distrimind.madkit.kernel.network.AskForConnectionMessage;
import com.distrimind.madkit.kernel.network.BroadcastLocalLanMessage;
import com.distrimind.madkit.kernel.network.DirectLocalLanMessage;
import com.distrimind.madkit.util.NetworkMessage;

import static com.distrimind.madkit.util.ReflectionTools.*;

/**
 * Gives access to Madkit Network methods
 * 
 * @author Jason Mahdjoub
 * @version 1.0
 */
class MadkitNetworkAccess {

	@SuppressWarnings("unused")
	static AbstractAgent getNIOAgent(AbstractAgent _requester) {
		try {
			return (AbstractAgent) nio_constructor.newInstance();
		} catch (InstantiationException | IllegalAccessException | IllegalArgumentException
				| InvocationTargetException e) {
			System.err.println("Unexpected error :");
			e.printStackTrace();
			System.exit(-1);
			return null;
		}
	}

	static Object getNetworkBoard() {
		try {
			return network_board_constructor.newInstance();
		} catch (InstantiationException | IllegalAccessException | IllegalArgumentException
				| InvocationTargetException e) {
			System.err.println("Unexpected error :");
			e.printStackTrace();
			System.exit(-1);
			return null;
		}
	}

	static boolean checkNetworkBoardEmpty(Object networkBoard) {
		try {
			return (Boolean) invoke(check_network_board_memory_leak, networkBoard);
		} catch (InvocationTargetException e) {
			System.err.println("Unexpected error :");
			e.printStackTrace();
			System.exit(-1);
			return false;
		}

	}

	@SuppressWarnings("unused")
	static AbstractAgent getLocalNetworkAffectationAgent(AbstractAgent _requester) {
		try {
			return (AbstractAgent) local_network_affectation_agent_constructor.newInstance();
		} catch (InstantiationException | IllegalAccessException | IllegalArgumentException
				| InvocationTargetException e) {
			System.err.println("Unexpected error :");
			e.printStackTrace();
			System.exit(-1);
			return null;
		}
	}

	@SuppressWarnings("unused")
	static AbstractAgent getUpnpIDGAgent(AbstractAgent _requester) {
		try {
			return (AbstractAgent) invoke(get_upnp_igd_agent_instance, null);
		} catch (InvocationTargetException e) {
			System.err.println("Unexpected error :");
			e.printStackTrace();
			System.exit(-1);
			return null;
		}
	}

	static DirectLocalLanMessage getDirectLocalLanMessageInstance(Message m) {
		try {
			if (!(m instanceof NetworkMessage))
				throw new IllegalArgumentException("The message to send "+m.getClass()+" me implements NetworkMessage interface");

			return direct_local_lan_message.newInstance(m);
		} catch (InstantiationException | IllegalAccessException | IllegalArgumentException
				| InvocationTargetException e) {
			System.err.println("Unexpected error :");
			e.printStackTrace();
			System.exit(-1);
			return null;
		}

	}

	static BroadcastLocalLanMessage getBroadcastLocalLanMessage(Message m, AbstractGroup _destination_groups,
			String _destination_role, ArrayList<AgentAddress> _agentAddressesSender) {
		try {
			if (!(m instanceof NetworkMessage))
				throw new IllegalArgumentException("The message to send me implements NetworkMessage interface");

			return broadcast_local_lan_message.newInstance(m, _destination_groups,
					_destination_role, _agentAddressesSender);
		} catch (InstantiationException | IllegalAccessException | IllegalArgumentException
				| InvocationTargetException e) {
			System.err.println("Unexpected error :");
			e.printStackTrace();
			System.exit(-1);
			return null;
		}
	}

	private static final String package_name;
	private static final Class<?> c_nio_agent;
	private static final Class<?> c_network_board;
	private static final Constructor<?> nio_constructor;
	private static final Constructor<?> network_board_constructor;
	private static final Class<?> c_local_network_affectation_agent;
	private static final Constructor<?> local_network_affectation_agent_constructor;
	private static final Class<?> c_upnp_igd_agent;
	private static final Method get_upnp_igd_agent_instance;
	private static final Method check_network_board_memory_leak;
	private static final Constructor<DirectLocalLanMessage> direct_local_lan_message;
	private static final Constructor<BroadcastLocalLanMessage> broadcast_local_lan_message;

	static {
		package_name = AskForConnectionMessage.class.getPackage().getName();
		c_nio_agent = loadClass(package_name + ".NIOAgent");
		c_network_board = loadClass(package_name + ".NetworkBoard");
		nio_constructor = getConstructor(c_nio_agent);
		network_board_constructor = getConstructor(c_network_board);
		c_upnp_igd_agent = loadClass(package_name + ".UpnpIGDAgent");
		c_local_network_affectation_agent = loadClass(package_name + ".LocalNetworkAffectationAgent");
		local_network_affectation_agent_constructor = getConstructor(c_local_network_affectation_agent);
		get_upnp_igd_agent_instance = getMethod(c_upnp_igd_agent, "getInstance");
		check_network_board_memory_leak = getMethod(c_network_board, "checkBoardEmpty");
		direct_local_lan_message = getConstructor(DirectLocalLanMessage.class, Message.class);
		broadcast_local_lan_message = getConstructor(BroadcastLocalLanMessage.class, Message.class, AbstractGroup.class,
				String.class, ArrayList.class);
	}

	

}
