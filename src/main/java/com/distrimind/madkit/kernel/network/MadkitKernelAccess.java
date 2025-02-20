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
package com.distrimind.madkit.kernel.network;

import com.distrimind.madkit.kernel.*;
import com.distrimind.madkit.kernel.AbstractAgent.ReturnCode;
import com.distrimind.madkit.message.hook.HookMessage;
import com.distrimind.util.AbstractDecentralizedID;
import com.distrimind.util.AbstractDecentralizedIDGenerator;
import com.distrimind.util.IDGeneratorInt;
import com.distrimind.util.concurrent.LockerCondition;
import com.distrimind.util.io.MessageExternalizationException;
import com.distrimind.util.io.RandomInputStream;
import com.distrimind.util.io.RandomOutputStream;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;

import static com.distrimind.util.ReflectionTools.invoke;
import static com.distrimind.util.ReflectionTools.getMethod;
import static com.distrimind.util.ReflectionTools.getConstructor;

/**
 * Gives access to Madkit Kernel methods
 * 
 * @author Jason Mahdjoub
 * @version 1.0
 */
class MadkitKernelAccess {

	static Agent getMadkitKernel(AbstractAgent _requester) {
		try {
			return (Agent) invoke(m_get_madkit_kernel, _requester);
		} catch (InvocationTargetException e) {
			System.err.println("Unexpected error :");
			e.printStackTrace();
			System.exit(-1);
			return null;
		}
	}

	static int numberOfValidGeneratedID(AbstractAgent _requester) {
		try {
			return (Integer) invoke(m_nb_valid_generated_id, getMadkitKernel(_requester));
		} catch (InvocationTargetException e) {
			System.err.println("Unexpected error :");
			e.printStackTrace();
			System.exit(-1);
			return -1;
		}
	}

	static IDGeneratorInt getIDTransferGenerator(AbstractAgent _requester) {
		try {
			return ((IDGeneratorInt) invoke(m_get_id_transfer_generator, getMadkitKernel(_requester)));
		} catch (InvocationTargetException e) {
			System.err.println("Unexpected error :");
			e.printStackTrace();
			System.exit(-1);
			return null;
		}
	}

	static void informHooks(AbstractAgent _requester, HookMessage hook_message) {
		try {
			invoke(m_inform_hooks, getMadkitKernel(_requester), hook_message);
		} catch (InvocationTargetException e) {
			System.err.println("Unexpected error :");
			e.printStackTrace();
			System.exit(-1);
		}
	}

	static void setReturnsCode(ReturnCode rc, TransfersReturnsCodes returns_Code) {
		try {
			invoke(m_set_returns_code, rc, returns_Code);
		} catch (InvocationTargetException e) {
			System.err.println("Unexpected error :");
			e.printStackTrace();
			System.exit(-1);
		}
	}

	static void setReceiver(Message m, AgentAddress aa) {
		try {
			invoke(m_set_message_receiver, m, aa);
		} catch (InvocationTargetException e) {
			System.err.println("Unexpected error :");
			e.printStackTrace();
			System.exit(-1);
		}
	}

	static void setSender(Message m, AgentAddress aa) {
		try {
			invoke(m_set_message_sender, m, aa);
		} catch (InvocationTargetException e) {
			System.err.println("Unexpected error :");
			e.printStackTrace();
			System.exit(-1);
		}
	}

	static ConversationID getInterfacedConversationIDToDistantPeer(ConversationID conversationID,
			AbstractAgent requester, KernelAddress currentKernelAddress, KernelAddress distantKernelAddress) {
		try {
			return (ConversationID) invoke(m_get_interfaced_conversation_id_to_distant, conversationID,
					getGlobalInterfacedIDs(requester), currentKernelAddress, distantKernelAddress);
		} catch (InvocationTargetException e) {
			System.err.println("Unexpected error :");
			e.printStackTrace();
			System.exit(-1);
		}
		return null;
	}

	static ConversationID getInterfacedConversationIDFromDistantPeer(ConversationID conversationID,
			AbstractAgent requester, KernelAddress currentKernelAddress, KernelAddress distantKernelAddress) {
		try {
			return (ConversationID) invoke(m_get_interfaced_conversation_id_from_distant, conversationID,
					getGlobalInterfacedIDs(requester), currentKernelAddress, distantKernelAddress);
		} catch (InvocationTargetException e) {
			System.err.println("Unexpected error :");
			e.printStackTrace();
			System.exit(-1);
		}
		return null;
	}

	static RandomInputStream getInputStream(BigDataPropositionMessage m) {
		try {
			return (RandomInputStream) invoke(m_get_big_data_stream, m);
		} catch (InvocationTargetException e) {
			System.err.println("Unexpected error :");
			e.printStackTrace();
			System.exit(-1);
		}
		return null;
	}

	static int getIDPacket(BigDataPropositionMessage m) {
		try {
			return (Integer) invoke(m_get_big_data_id_packet, m);
		} catch (InvocationTargetException e) {
			System.err.println("Unexpected error :");
			e.printStackTrace();
			System.exit(-1);
		}
		return -1;
	}

	static RandomOutputStream getOutputStream(BigDataPropositionMessage m) {
		try {
			return (RandomOutputStream) invoke(m_get_big_data_output_stream, m);
		} catch (InvocationTargetException e) {
			System.err.println("Unexpected error :");
			e.printStackTrace();
			System.exit(-1);
		}
		return null;
	}

	static void setIDPacket(BigDataPropositionMessage m, int idPacket) {
		try {
			invoke(m_set_big_data_id_packet, m, idPacket);
		} catch (InvocationTargetException e) {
			System.err.println("Unexpected error :");
			e.printStackTrace();
			System.exit(-1);
		}
	}


	@SuppressWarnings("SameParameterValue")
	static void dataCorrupted(BigDataPropositionMessage m, long dataTransferred, MessageExternalizationException e) {
		try {
			invoke(m_big_data_data_corrupted, m, dataTransferred, e);
		} catch (InvocationTargetException e2) {
			System.err.println("Unexpected error :");
			e2.printStackTrace();
			System.exit(-1);
		}
	}

	static void transferCompleted(BigDataPropositionMessage m, long dataTransferred) {
		try {
			invoke(m_big_data_complete, m, dataTransferred);
		} catch (InvocationTargetException e) {
			System.err.println("Unexpected error :");
			e.printStackTrace();
			System.exit(-1);
		}
	}

	static int getIDPacket(BigDataResultMessage m) {
		try {
			return (Integer) invoke(m_get_big_data_result_id_packet, m);
		} catch (InvocationTargetException e) {
			System.err.println("Unexpected error :");
			e.printStackTrace();
			System.exit(-1);
		}
		return -1;
	}


	static TaskID getTaskIDInstance(ConversationID id) {
		try {
			return c_task_id.newInstance(id);
		} catch (InvocationTargetException | InstantiationException | IllegalAccessException
				| IllegalArgumentException e) {
			System.err.println("Unexpected error :");
			e.printStackTrace();
			System.exit(-1);
		}
		return null;
	}

	static void transferLostForBigDataTransfer(AbstractAgent requester, ConversationID conversationID, int idPacket,
											   AgentAddress sender, AgentAddress receiver, long readDataLength, long durationInMs, AbstractDecentralizedID differedBigDataInternalIdentifier,
											   ExternalAsynchronousBigDataIdentifier externalAsynchronousBigDataIdentifier, BigDataResultMessage.Type cancelingType, boolean updateDatabase) {
		try {
			invoke(m_transferLostForBigDataTransfer, getMadkitKernel(requester), requester, conversationID,
					idPacket, sender, receiver, readDataLength, durationInMs, differedBigDataInternalIdentifier, externalAsynchronousBigDataIdentifier, cancelingType, updateDatabase);
		} catch (InvocationTargetException e) {
			System.err.println("Unexpected error :");
			e.printStackTrace();
			System.exit(-1);
		}
	}

	static void setAsynchronousTransferAsStarted(AbstractAgent requester, AbstractDecentralizedIDGenerator asynchronousBigDataInternalIdentifier) {
		try {
			invoke(m_setAsynchronousTransferAsStarted, getMadkitKernel(requester), requester, asynchronousBigDataInternalIdentifier);
		} catch (InvocationTargetException e) {
			System.err.println("Unexpected error :");
			e.printStackTrace();
			System.exit(-1);
		}
	}

	static void waitMessageSent(AbstractAgent requester, LockerCondition locker) throws InterruptedException {
		try {
			invoke(m_wait_message_sent, requester, locker);
		} catch (InvocationTargetException e) {
			if (e.getTargetException() instanceof InterruptedException)
				throw (InterruptedException) e.getTargetException();
			else
				e.getTargetException().printStackTrace();
		}
	}

	static Message markAsRead(Message m) {
		try {
			return (Message) invoke(m_message_mark_as_read, m);
		} catch (InvocationTargetException e) {
			System.err.println("Unexpected error :");
			e.printStackTrace();
			System.exit(-1);
		}
		return null;
	}

	static Map<?, ?> getGlobalInterfacedIDs(AbstractAgent requester) {
		try {
			return (Map<?, ?>) invoke(m_get_global_interfaced_ids, getMadkitKernel(requester));
		} catch (InvocationTargetException e) {
			System.err.println("Unexpected error :");
			e.printStackTrace();
			System.exit(-1);
		}
		return null;
	}

	static void setLocalMadkitKernel(BigDataPropositionMessage bdpm, AbstractAgent requester) {
		try {
			invoke(m_setLocalMadkitKernel, bdpm, getMadkitKernel(requester));
		} catch (InvocationTargetException e) {
			System.err.println("Unexpected error :");
			e.printStackTrace();
			System.exit(-1);
		}
	}

	static void setConnectionInfoSystemMessage(AbstractAgent requester, ConnectionIdentifier connectionIdentifier, ConnectionInfoSystemMessage connectionInfoSystemMessage) {
		try {
			invoke(m_setConnectionInfoSystemMessage, getMadkitKernel(requester), requester, connectionIdentifier, connectionInfoSystemMessage);
		} catch (InvocationTargetException e) {
			System.err.println("Unexpected error :");
			e.printStackTrace();
			System.exit(-1);
		}
	}

	private static final String package_name;
	private static final Class<?> c_madkit_kernel;
	private static final Method m_get_madkit_kernel;
	private static final Method m_inform_hooks;
	private static final Method m_nb_valid_generated_id;
	private static final Method m_get_id_transfer_generator;
	private static final Method m_set_returns_code;
	private static final Method m_set_message_receiver;
	private static final Method m_set_message_sender;
	private static final Method m_get_interfaced_conversation_id_from_distant;
	private static final Method m_get_interfaced_conversation_id_to_distant;
	private static final Method m_get_big_data_stream;
	private static final Method m_get_big_data_id_packet;
	private static final Method m_get_big_data_output_stream;
	private static final Method m_set_big_data_id_packet;
	private static final Method m_big_data_data_corrupted;
	private static final Method m_big_data_complete;
	private static final Method m_get_big_data_result_id_packet;
	private static final Method m_transferLostForBigDataTransfer;
	private static final Method m_message_mark_as_read;
	private static final Method m_wait_message_sent;
	private static final Method m_get_global_interfaced_ids;
	private static final Method m_setAsynchronousTransferAsStarted;
	private static final Method m_setLocalMadkitKernel;
	private static final Method m_setConnectionInfoSystemMessage;

	//private static final Constructor<BigDataTransferID> c_big_data_transfer_id;
	private static final Constructor<TaskID> c_task_id;

	static {
		package_name = AbstractAgent.class.getPackage().getName();
		c_madkit_kernel = loadClass(package_name + ".MadkitKernel");
		m_get_madkit_kernel = getMethod(AbstractAgent.class, "getMadkitKernel");
		m_inform_hooks = getMethod(c_madkit_kernel, "informHooks", HookMessage.class);
		m_nb_valid_generated_id = getMethod(c_madkit_kernel, "numberOfValidGeneratedID");
		m_get_id_transfer_generator = getMethod(c_madkit_kernel, "getIDTransferGenerator");
		m_set_returns_code = getMethod(AbstractAgent.ReturnCode.class, "setReturnsCode", TransfersReturnsCodes.class);
		m_set_message_receiver = getMethod(Message.class, "setReceiver", AgentAddress.class);
		m_set_message_sender = getMethod(Message.class, "setSender", AgentAddress.class);
		m_get_interfaced_conversation_id_to_distant = getMethod(ConversationID.class,
				"getInterfacedConversationIDToDistantPeer", Map.class, KernelAddress.class, KernelAddress.class);
		m_get_interfaced_conversation_id_from_distant = getMethod(ConversationID.class,
				"getInterfacedConversationIDFromDistantPeer", Map.class, KernelAddress.class, KernelAddress.class);
		m_get_big_data_stream = getMethod(BigDataPropositionMessage.class, "getInputStream");
		m_get_big_data_id_packet = getMethod(BigDataPropositionMessage.class, "getIDPacket");
		m_get_big_data_output_stream = getMethod(BigDataPropositionMessage.class, "getOutputStream");
		m_set_big_data_id_packet = getMethod(BigDataPropositionMessage.class, "setIDPacket", int.class);
		m_get_big_data_result_id_packet = getMethod(BigDataResultMessage.class, "getIDPacket");
		m_big_data_data_corrupted = getMethod(BigDataPropositionMessage.class, "dataCorrupted", long.class, MessageExternalizationException.class);
		m_big_data_complete = getMethod(BigDataPropositionMessage.class, "transferCompleted", long.class);
		m_transferLostForBigDataTransfer = getMethod(c_madkit_kernel, "transferLostForBigDataTransfer",
				AbstractAgent.class, ConversationID.class, int.class, AgentAddress.class, AgentAddress.class,
				long.class, long.class, AbstractDecentralizedIDGenerator.class, ExternalAsynchronousBigDataIdentifier.class, BigDataResultMessage.Type.class, boolean.class);
		m_message_mark_as_read = getMethod(Message.class, "markMessageAsRead");
		m_wait_message_sent = getMethod(AbstractAgent.class, "waitMessageSent", LockerCondition.class);
		m_get_global_interfaced_ids = getMethod(c_madkit_kernel, "getGlobalInterfacedIDs");
		c_task_id = getConstructor(TaskID.class, ConversationID.class);
		m_setAsynchronousTransferAsStarted = getMethod(c_madkit_kernel, "setAsynchronousTransferAsStarted", AbstractAgent.class, AbstractDecentralizedIDGenerator.class);
		m_setLocalMadkitKernel=getMethod(BigDataPropositionMessage.class, "setLocalMadkitKernel", c_madkit_kernel);
		m_setConnectionInfoSystemMessage=getMethod(c_madkit_kernel, "setConnectionInfoSystemMessage", AbstractAgent.class, ConnectionIdentifier.class, ConnectionInfoSystemMessage.class);
	}

	static Class<?> loadClass(String class_name) {

		try {
			return MadkitClassLoader.getLoader().loadClass(class_name);
		} catch (SecurityException | ClassNotFoundException e) {
			System.err.println("Impossible to access to the class " + class_name
					+ ". This is an inner bug. Please contact the developers. Impossible to continue. See the next error :");
			e.printStackTrace();
			System.exit(-1);
			return null;
		}
	}

}
