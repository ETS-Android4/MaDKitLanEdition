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

import com.distrimind.madkit.agr.LocalCommunity;
import com.distrimind.madkit.exceptions.MadkitException;
import com.distrimind.madkit.exceptions.NIOException;
import com.distrimind.madkit.exceptions.PacketException;
import com.distrimind.madkit.exceptions.SelfKillException;
import com.distrimind.madkit.kernel.AbstractAgent;
import com.distrimind.madkit.kernel.AbstractGroup;
import com.distrimind.madkit.kernel.AgentAddress;
import com.distrimind.madkit.kernel.AgentFakeThread;
import com.distrimind.madkit.kernel.BigDataPropositionMessage;
import com.distrimind.madkit.kernel.BigDataResultMessage;
import com.distrimind.madkit.kernel.CGRSynchro;
import com.distrimind.madkit.kernel.CGRSynchro.Code;
import com.distrimind.madkit.kernel.CancelBigDataTransferMessage;
import com.distrimind.madkit.kernel.ConversationID;
import com.distrimind.madkit.kernel.ExternalAsynchronousBigDataIdentifier;
import com.distrimind.madkit.kernel.Group;
import com.distrimind.madkit.kernel.KernelAddress;
import com.distrimind.madkit.kernel.MadkitClassLoader;
import com.distrimind.madkit.kernel.Message;
import com.distrimind.madkit.kernel.Task;
import com.distrimind.madkit.kernel.TaskID;
import com.distrimind.madkit.kernel.network.AbstractAgentSocket.AgentSocketKilled;
import com.distrimind.madkit.kernel.network.AbstractAgentSocket.Groups;
import com.distrimind.madkit.kernel.network.AbstractAgentSocket.ReceivedBlockData;
import com.distrimind.madkit.kernel.network.TransferAgent.IDTransfer;
import com.distrimind.madkit.kernel.network.connection.ConnectionProtocol.ConnectionClosedReason;
import com.distrimind.madkit.kernel.network.connection.access.CloudIdentifier;
import com.distrimind.madkit.kernel.network.connection.access.Identifier;
import com.distrimind.madkit.kernel.network.connection.access.ListGroupsRoles;
import com.distrimind.madkit.kernel.network.connection.access.PairOfIdentifiers;
import com.distrimind.madkit.message.ObjectMessage;
import com.distrimind.madkit.message.hook.DistantKernelAgentEventMessage;
import com.distrimind.madkit.message.hook.HookMessage.AgentActionEvent;
import com.distrimind.madkit.message.hook.NetworkGroupsAccessEvent;
import com.distrimind.madkit.message.hook.NetworkLoginAccessEvent;
import com.distrimind.util.AbstractDecentralizedIDGenerator;
import com.distrimind.util.IDGeneratorInt;
import com.distrimind.util.concurrent.LockerCondition;
import com.distrimind.util.crypto.AbstractSecureRandom;
import com.distrimind.util.crypto.MessageDigestType;
import com.distrimind.util.io.Integrity;
import com.distrimind.util.io.LimitedRandomInputStream;
import com.distrimind.util.io.MessageExternalizationException;
import com.distrimind.util.io.RandomByteArrayOutputStream;
import com.distrimind.util.io.RandomInputStream;
import com.distrimind.util.io.RandomOutputStream;
import com.distrimind.util.io.SecureExternalizableWithoutInnerSizeControl;
import com.distrimind.util.io.SerializationTools;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Represent a distant Madkit kernel
 * 
 * @author Jason Mahdjoub
 * @version 1.2
 * @since MadkitLanEdition 1.0
 */
@SuppressWarnings("UnusedReturnValue")
class DistantKernelAgent extends AgentFakeThread {

	private final ArrayList<AgentSocketData> agents_socket = new ArrayList<>();
	private final ArrayList<AgentSocketData> indirect_agents_socket = new ArrayList<>();

	protected KernelAddressInterfaced distant_kernel_address = null;
	private final IDGeneratorInt packet_id_generator = new IDGeneratorInt();
	private ListGroupsRoles distant_accepted_groups = new ListGroupsRoles();
	private AbstractSecureRandom random;
	private boolean kernelAddressActivated = false;
	private long lastAgentsUpdateNano = -1;

	private List<PairOfIdentifiers> accepted_identifiers = new ArrayList<>();
	private List<PairOfIdentifiers> last_accepted_identifiers = new ArrayList<>();
	private List<CloudIdentifier> lastDeniedCloudIdentifiersToOther = new ArrayList<>();
	private List<Identifier> lastDeniedIdentifiersFromOther = new ArrayList<>();
	private List<Identifier> lastDeniedIdentifiersToOther = new ArrayList<>();
	private List<PairOfIdentifiers> last_un_logged_identifiers = new ArrayList<>();
	private ListGroupsRoles localAcceptedAndRequestedGroups = new ListGroupsRoles();
	private ListGroupsRoles generalLocalMultiGroup =new ListGroupsRoles();
	private ArrayList<Group> sharedAcceptedAndRequestedGroups = new ArrayList<>();
	// private MultiGroup localGeneralAcceptedGroups=new MultiGroup();

	private StatsBandwidth stats = null;
	protected AtomicLong totalDataInQueue = new AtomicLong(0);

	private final AtomicBoolean transferPaused = new AtomicBoolean(false);
	final AtomicReference<ExceededDataQueueSize> globalExceededDataQueueSize = new AtomicReference<>(null);
	private final HashMap<Integer, BigPacketData> packetsDataInQueue = new HashMap<>();
	private final HashMap<Integer, SerializedReading> current_short_readings = new HashMap<>();
	private final HashMap<Integer, BigDataReading> current_big_data_readings = new HashMap<>();
	NetworkBoard networkBoard = null;
	protected TaskID taskForPurgeCheck = null;
	private ArrayList<ObjectMessage<SecretMessage>> differedSecretMessages = null;
	private boolean lockSocketUntilCGRSynchroIsSent=false;
	private FilteredObjectResolver filteredObjectResolver=null;

	DistantKernelAgent() {
		super();

	}

	@Override
	public void end() {
		for (BigDataReading bdr : this.current_big_data_readings.values())
		{
			MadkitKernelAccess.transferLostForBigDataTransfer(this, bdr.getOriginalMessage().getConversationID(),
					bdr.getIDPacket(), bdr.getOriginalMessage().getSender(), bdr.getOriginalMessage().getReceiver(),
					bdr.getNumberOfReceivedBytes(), bdr.getStatistics().getDurationMilli(),
					bdr.getOriginalMessage().getAsynchronousBigDataInternalIdentifier(), bdr.getOriginalMessage().getExternalAsynchronousBigDataIdentifier(), BigDataResultMessage.Type.CONNECTION_LOST, true);
		}
		current_big_data_readings.clear();
		for (BigPacketData bpd : this.packetsDataInQueue.values()) {
			try {
				bpd.cancel();
			} catch (IOException | MadkitException e) {
				if (logger!=null)
					logger.severeLog("Cannot close stream of BigPacketData", e);
			}
		}
		this.packetsDataInQueue.clear();
		for (BigDataReading bdr : this.current_big_data_readings.values()) {
			try {
				bdr.cancel();
			} catch (IOException e) {
				if (logger!=null)
					logger.severeLog("Cannot close stream of BigDataReading", e);
			}
		}
		this.current_big_data_readings.clear();
		for (SerializedReading sr : this.current_short_readings.values()) {
			try {
				sr.cancel();
			} catch (IOException e) {
				if (logger!=null)
					logger.severeLog("Cannot close stream of SerializedReading", e);
			}
		}
		this.current_short_readings.clear();
		Message m;
		while ((m=nextMessage())!=null)
		{
			unlockMessageIfNecessary(m);


		}
		if (stats != null && kernelAddressActivated)
			getMadkitConfig().networkProperties.removeStatsBandwidth(distant_kernel_address);

		purgeTotalDataQueue();



		if (distant_kernel_address != null && kernelAddressActivated) {
			MadkitKernelAccess.informHooks(this, new DistantKernelAgentEventMessage(
					AgentActionEvent.DISTANT_KERNEL_DISCONNECTED, distant_kernel_address));
			if (logger != null && logger.isLoggable(Level.INFO))
				logger.info("Distant kernel agent disconnected : " + distant_kernel_address);
		}

		if (logger != null && logger.isLoggable(Level.FINE))
			logger.fine("DistantKernelAgent (" + this.distant_kernel_address + ") KILLED !");

	}

	private boolean broadcastLocalLanMessage(ArrayList<AgentSocketData> agents_socket, BroadcastLocalLanMessage message,
			AbstractGroup groups_lacking) throws NIOException {


		for (AgentSocketData asd : agents_socket) {
			if (asd.isUsable()) {
				AbstractGroup concerned_groups = asd.distant_accessible_and_requested_groups.getGroups().intersect(groups_lacking);
				if (!concerned_groups.isEmpty()) {
					message.getMessageLocker().lock();
					sendData(asd.getAgentAddress(), message.getBroadcastMessage(concerned_groups), false,
							message.getMessageLocker(), false);
					groups_lacking=groups_lacking.minus(concerned_groups);
					if (groups_lacking.isEmpty()) {
						return false;
					}
				}
			}
		}
		return true;

	}

	private boolean removeAgentSocketData(ArrayList<AgentSocketData> agents_socket, AgentSocketKilled _message) {
		for (Iterator<AgentSocketData> it = agents_socket.iterator(); it.hasNext();) {
			AgentSocketData asd = it.next();
			if (asd.isConcernedBy(_message.getSender())) {
				it.remove();
				return true;
			}
		}
		return false;
	}

	/*
	 * static class ResendMessageToDistantKernel extends Message { private static
	 * final long serialVersionUID = -5906998698550621029L;
	 * 
	 * final Message messageToResend; ResendMessageToDistantKernel(Message
	 * messageToResend) { this.messageToResend=messageToResend; } }
	 */
	private volatile boolean concurrent = false;
	private boolean isSharedGroupRole(CGRSynchro cgrSynchro)
	{
		return isSharedGroupRole(cgrSynchro.getContent().getGroup(), cgrSynchro.getContent().getRole());
	}
	private boolean isSharedGroupRole(Group group, String role)
	{
		return isDistantlyAcceptedGroupRole(group, role) && isLocallyAcceptedGroupRole(group);
	}
	private boolean isLocallyAcceptedGroupRole(Group group)
	{
		return generalLocalMultiGroup.includes(group);
	}
	private boolean isDistantlyAcceptedGroupRole(Group group, String role)
	{
		return distant_accepted_groups.includes(group, role);
	}

	private void unlockMessageIfNecessary(Message _message)
	{
		if (_message instanceof LockableMessage) {
			((LockableMessage) _message).getMessageLocker().forgive(false);
			if (_message instanceof LocalLanMessage)
				sendReplyEmpty(_message);
		}

	}

	@SuppressWarnings({"SynchronizeOnNonFinalField"})
    @Override
	protected void liveByStep(Message _message) {
		if (concurrent)
			throw new ConcurrentModificationException();
		concurrent = true;
		try {
			if (getState().compareTo(AbstractAgent.State.WAIT_FOR_KILL) >= 0) {
				unlockMessageIfNecessary(_message);
				Logger logger=getLogger();
				if (logger!=null)
					logger.severe("Unexpected access");
				return;
			}

			try {
				if (_message.getClass()==ReceivedBlockData.class) {
					receiveData(_message.getSender(), ((ReceivedBlockData) _message).getContent());
				}
				else if (_message instanceof LocalLanMessage) {
					if (distant_kernel_address == null || !kernelAddressActivated || !hasUsableDistantSocketAgent()) {
						sendReplyEmpty(_message);

					}
					else {
						try {
							if (_message.getClass() == BroadcastLocalLanMessage.class) {
								BroadcastLocalLanMessage message = (BroadcastLocalLanMessage) _message;
								if (logger != null && logger.isLoggable(Level.FINEST))
									logger.finest("BroadcastLocalLanMessage to route (distantInterfacedKernelAddress="
											+ distant_kernel_address + ") : " + _message);

								message.getMessageLocker().lock();
								try {
									if (!message.abstract_group.isEmpty()) {
										AbstractGroup groups_lacking = message.abstract_group.clone();
										updateBestAgent();

										if (broadcastLocalLanMessage(agents_socket, message, groups_lacking))
											broadcastLocalLanMessage(indirect_agents_socket, message, groups_lacking);

									}
								} finally {
									message.getMessageLocker().unlock();
								}


							} else if (_message.getClass() == DirectLocalLanMessage.class) {
								DirectLocalLanMessage message = (DirectLocalLanMessage) _message;
								if (logger != null && logger.isLoggable(Level.FINEST))
									logger.finest("DirectLocalLanMessage to route (distantInterfacedKernelAddress="
											+ distant_kernel_address + ") : " + _message);

								message.getMessageLocker().lock();
								// message.setIDPacket(packet_id_generator.getNewID());

								AgentSocketData asd = getBestAgentSocket(message.getOriginalMessage().getSender(), message.getOriginalMessage().getReceiver(), true);
								if (asd != null) {
									Message m = message.getOriginalMessage();
									if (m.getClass() == BigDataPropositionMessage.class) {
										BigDataPropositionMessage bgpm = (BigDataPropositionMessage) m;
										try {
											addBigDataInQueue(asd, bgpm);

											sendData(asd.getAgentAddress(), new DirectLanMessage(bgpm), false,
													message.getMessageLocker(), false);
										} catch (PacketException e) {
											if (logger != null)
												logger.severeLog("Anomaly detected", e);
											processInvalidMessage(_message, false);
											message.getMessageLocker().forgive(false);
										}
									} else {
										sendData(asd.getAgentAddress(), new DirectLanMessage(m), false,
												message.getMessageLocker(), false);
									}

								} else {
									message.getMessageLocker().unlock();
								}
							}
							else
								throw new IllegalAccessError();
						} finally {
							sendReplyEmpty(_message);
						}
					}
				}  else if (_message.getClass() == SendDataFromAgentSocket.class) {
					if (logger != null && logger.isLoggable(Level.FINEST))
						logger.finest("Rooting message to send from agent socket (distantInterfacedKernelAddress="
								+ distant_kernel_address + ") : " + _message);
					SendDataFromAgentSocket m = (SendDataFromAgentSocket) _message;
					MessageLocker ml = (m.getContent().getClass()==DataToBroadcast.class)
							? ((DataToBroadcast) m.getContent()).getMessageToBroadcast().getMessageLocker()
							: null;
					if (ml != null) {
						ml.lock();
					}


					sendData(m.getSender(), m.getContent(), m.isItAPriority, ml, m.last_message);
				} else if (_message.getClass() == AnomalyDetectedMessage.class) {
					AnomalyDetectedMessage m = (AnomalyDetectedMessage) _message;


					if (m.getKernelAddress() != null && m.getKernelAddress().equals(distant_kernel_address)) {
						processInvalidProcess(null, m.getMessage(), m.isCandidateToBan());
					}
				} else if (_message.getClass() == CGRSynchro.class) {
					CGRSynchro m = (CGRSynchro) _message;

					if (this.kernelAddressActivated) {

						if (isSharedGroupRole(m)) {
							AgentSocketData asd = getBestAgentSocketForLocalAgentAddress(distant_kernel_address, m.getContent(),
									false);

							if (asd != null) {
								if (logger != null && logger.isLoggable(Level.FINER))
									logger.finer("CGRSynchro (distantInterfacedKernelAddress=" + distant_kernel_address
											+ ") : " + _message);

								boolean lock = this.lockSocketUntilCGRSynchroIsSent && m.getCode() != Code.LEAVE_GROUP && m.getCode() != Code.LEAVE_ROLE;

								if (lock) {
									m.initMessageLocker(true);

								}

								CGRSynchroSystemMessage message = new CGRSynchroSystemMessage(m);

								sendData(asd.getAgentAddress(), message, m.getCode() != Code.LEAVE_GROUP && m.getCode() != Code.LEAVE_ROLE, m.getMessageLocker(), false);
								if (m.getMessageLocker() != null)
									m.getMessageLocker().waitUnlock(this, true);
								potentialChangesInGroups();
								newCGRSynchroDetected(m);

								//sendData(asd.getAgentAddress(), message, true, m.getMessageLocker(), false);
							} else {
								if (logger != null && logger.isLoggable(Level.FINER))
									logger.finer("No agent socket found for CGRSynchro (distantInterfacedKernelAddress=" + distant_kernel_address
											+ ") : " + _message);
							}
						}
					}


				} else if (_message.getClass() == NetworkGroupsAccessEvent.class) {
					if (distant_kernel_address == null)
						return;

					NetworkGroupsAccessEvent message = (NetworkGroupsAccessEvent) _message;

					if (message.getConcernedKernelAddress().equals(distant_kernel_address.getOriginalKernelAddress())
							&& message.concernsAuthorizedGroupsFromDistantPeer()) {
						if (logger != null && logger.isLoggable(Level.FINEST))
							logger.finest("Updating accepted groups from distant peer (distantInterfacedKernelAddress="
									+ distant_kernel_address + ") : " + message);
						AgentSocketData asd = getAgentSocketDataFromItsAgentAddress(message.getSender());
						if (asd != null) {
							asd.setDistantAccessibleGroups(message);
							updateSharedAcceptedGroups(true, false);
							//updateDistantAcceptedGroups();
						} else {
							Logger logger=getLogger();
							if (logger!=null)
								logger.severe("Impossible to found agent socket data from agent address "
									+ _message.getSender() + ". So impossible to set given NetworkGroupsAccessEvent.");
						}

					}

				} else if (_message.getClass() == NetworkLoginAccessEvent.class) {
					if (distant_kernel_address == null)
						return;

					NetworkLoginAccessEvent message = (NetworkLoginAccessEvent) _message;

					if (message.getConcernedKernelAddress().equals(distant_kernel_address.getOriginalKernelAddress())) {
						if (logger != null && logger.isLoggable(Level.FINEST))
							logger.finest("Updating accepted identifiers (distantInterfacedKernelAddress="
									+ distant_kernel_address + ") : " + message);
						AgentSocketData asd = getAgentSocketDataFromItsAgentAddress(message.getSender());
						if (asd != null) {
							asd.setAcceptedIdentifiers(message);
							updateLoginData();
						} else {
							Logger logger=getLogger();
							if (logger!=null)
								logger.severe("Impossible to found agent socket data from agent address "
									+ _message.getSender() + ". So impossible to set given NetworkLoginAccessEvent.");
						}
					}

				} else if (_message.getClass() == KernelAddressValidation.class) {
					activateDistantKernelAgent(_message.getSender(),
							((KernelAddressValidation) _message).isKernelAddressInterfaceEnabled());
				}  else if (_message.getClass() == KillYou.class) {
					if (logger != null && logger.isLoggable(Level.FINER))
						logger.finer("DistantKernelAgent disabled (distantInterfacedKernelAddress="
								+ distant_kernel_address + ")");

					this.killAgent(this, KillingType.WAIT_AGENT_PURGE_ITS_MESSAGES_BOX_BEFORE_KILLING_IT);
				} else if (_message.getClass() == AbstractAgentSocket.AgentSocketKilled.class) {
					AbstractAgentSocket.AgentSocketKilled m = (AbstractAgentSocket.AgentSocketKilled) _message;

					boolean removed = removeAgentSocketData(agents_socket, m);
					if (!removed)
						removed = removeAgentSocketData(indirect_agents_socket, m);
					if (removed) {
						if (logger != null && logger.isLoggable(Level.FINER))
							logger.finer(
									"Agent socket killed and removed from distant kernel agent list (distantInterfacedKernelAddress="
											+ distant_kernel_address + ")");
					}
					else {
						if (logger != null)
							logger.warning(
									"Agent socket killed and but not found on distant kernel agent list (distantInterfacedKernelAddress="
											+ distant_kernel_address + ")");
					}
					//updateLocalAcceptedGroups();
					if (agents_socket.size() == 0 && indirect_agents_socket.size() == 0) {
						for (BigPacketData bpd : m.bigDataNotSent) {
							cancelBigPacketDataToSendInQueue(bpd.getIDPacket(), false, BigDataResultMessage.Type.CONNECTION_LOST, true);

						}

						/*
						 * if (this.kernelAddressActivated) MadkitKernelAccess.informHooks(this, new
						 * DistantKernelAgentEventMessage(AgentActionEvent.DISTANT_KERNEL_DISCONNECTED,
						 * this.distant_kernel_address));
						 */
						this.killAgent(this);
					} else if (removed) {
						updateSharedAcceptedGroups(false, true);
						for (AbstractData ad : m.shortDataNotSent) {
							ad.reset();
						}
						for (BigPacketData ad : m.bigDataNotSent) {
							ad.reset();
						}

						// TODO manage transfer connections and associate them with the new agent
						// socket, instead of actually closing them.

					}
				} else if (_message.getClass()==CancelBigDataTransferMessage.class)
				{
					CancelBigDataTransferMessage m=(CancelBigDataTransferMessage)_message;

					BigPacketData bpd=packetsDataInQueue.values().stream().filter(e -> e.getConversationID().equals(m.getBigDataTransferID())).findAny().orElse(null);

					if (bpd==null)
					{
						BigDataReading bdr=current_big_data_readings.values().stream().filter(e -> e.getOriginalMessage().getConversationID().equals(m.getBigDataTransferID())).findAny().orElse(null);
						if (bdr!=null)
						{
							AgentSocketData asd = getBestAgentSocket(false);
							if (asd!=null)
								sendData(asd.getAgentAddress(), new CancelBigDataSystemMessage(bdr.getIDPacket(), false, m.getReason()), true, null, false);
						}
					}
					else
					{
						cancelBigPacketDataToSendInQueue(bpd.getIDPacket(), true, m.getReason(), m.isUpdateDatabase());
					}
				}
				else if (_message.getClass() == ExceededDataQueueSize.class) {
					final ExceededDataQueueSize exceededDataSize = (ExceededDataQueueSize) _message;
					if (logger != null && logger.isLoggable(Level.FINEST))
						logger.finest("Exceeded data queue size (distantInterfacedKernelAddress="
								+ distant_kernel_address + ") : " + exceededDataSize);
					synchronized (networkBoard) {
						if (exceededDataSize.isPaused()
								&& networkBoard.transferPausedForAllDistantKernelAgent.get()) {
							if (exceededDataSize.mustPurge()) {
								exceededDataSize.setReadingsToPurge(this.current_short_readings);
								globalExceededDataQueueSize.set(exceededDataSize);
								if (this.totalDataInQueue.get() > 0) {
									if (this.current_short_readings.size() > 0) {
										if (exceededDataSize.tryPurgeNow(this)) {
											Set<AgentAddress> agents = new HashSet<>();
											for (SerializedReading sr : current_short_readings.values()) {
												agents.add(sr.getInitialAgentAddress());
											}
											if (agents.size() > 0) {
												setTransferPaused(false, agents, true);
												final long durationBeforeTestingDDOS = 30000;
												if (taskForPurgeCheck != null)
													cancelTask(taskForPurgeCheck, true);
												taskForPurgeCheck = scheduleTask(new Task<Void>(durationBeforeTestingDDOS + System.currentTimeMillis()) {
													@Override
													public Void call() {

														ExceededDataQueueSize e = globalExceededDataQueueSize.get();
														if (isAlive() && e != null && e.isPaused() && e.mustPurge()) {
															if (getMadkitConfig().networkProperties
																	.getStatsBandwidth(getKernelAddress())
																	.getBytesDownloadedInRealTime(
																			NetworkProperties.DEFAULT_TRANSFER_STAT_IN_REAL_TIME_PER_30_SECONDS_SEGMENTS)
																	.getNumberOfIdentifiedBytesDuringTheLastCycle() == 0) {
																exceededDataSize.purgeCanceled(DistantKernelAgent.this);
																processPotentialDDOS();
															} else {
																Task<Void> t=this;
																scheduleTask(new Task<Void>(System.currentTimeMillis()
																		+ durationBeforeTestingDDOS) {
																	@Override
																	public Void call() throws Exception {
																		return t.call();
																	}
																});
															}
														}
														return null;
													}
												});
											} else {

												setTransferPaused(true, null, true);
												exceededDataSize.purgeFinished(this);
											}
										}
									} else {
										globalExceededDataQueueSize
												.set(new ExceededDataQueueSize(networkBoard, false, true));
										setTransferPaused(true, null, true);
										exceededDataSize.purgeFinished(this);
									}
								} else {
									setTransferPaused(true, null, true);
									exceededDataSize.purgeFinished(this);
								}
							} else {
								setTransferPaused(true, null, true);
								globalExceededDataQueueSize.set(exceededDataSize);
								if (exceededDataSize.hasOtherCandidates)
									exceededDataSize.candidateForPurge(this);

							}
						} else {
							if (!networkBoard.transferPausedForAllDistantKernelAgent.get()) {
								if (taskForPurgeCheck != null) {
									cancelTask(taskForPurgeCheck, true);
									taskForPurgeCheck = null;
								}
								globalExceededDataQueueSize.set(null);
								setTransferPaused(false, null, true);
							}
						}
					}
				} else if (_message.getClass() == AskForTransferMessage.class) {
					AskForTransferMessage m = (AskForTransferMessage) _message;
					if (logger != null && logger.isLoggable(Level.FINER))
						logger.finer("Ask for transfer message (distantInterfacedKernelAddress="
								+ distant_kernel_address + ") : " + m);

					if (m.needsInetSocketAddress()) {
						if (m.getKernelAddress1().equals(this.distant_kernel_address)
								|| m.getKernelAddress2().equals(this.distant_kernel_address)) {
							AgentSocketData asd = getBestAgentSocketCompatibleForTransfer();
							if (asd != null)
								sendReplyWithRole(m,
										new ObjectMessage<>(
												m.getCandidate(asd.getGlobalAgentAddress(), this.distant_kernel_address,
														asd.distant_inet_socket_address, asd.numberOfIntermediatePeers,
														asd.getConnectionInfo())),
										LocalCommunity.Roles.DISTANT_KERNEL_AGENT_ROLE);
							else
								sendReplyEmpty(m);
						} else
							sendReplyEmpty(m);
					} else if (m.needsKernelAddress()) {
						updateBestAgent();
						AgentSocketData candidate = getAgentSocketDataForTransfer(this.agents_socket, m);
						if (candidate == null)
							candidate = getAgentSocketDataForTransfer(this.indirect_agents_socket, m);
						if (candidate != null) {
							sendReplyWithRole(m,
									new ObjectMessage<>(m.getCandidate(
											candidate.getGlobalAgentAddress(), this.distant_kernel_address,
											candidate.distant_inet_socket_address, candidate.numberOfIntermediatePeers,
											candidate.getConnectionInfo())),
									LocalCommunity.Roles.DISTANT_KERNEL_AGENT_ROLE);
						} else
							sendReplyEmpty(m);
					}
				}
				else if (_message.getClass()==PauseBigDataTransferMessage.class)
				{
					PauseBigDataTransferMessage p=(PauseBigDataTransferMessage)_message;
					if (p.getKernelAddress()==null || p.getKernelAddress().equals(distant_kernel_address))
					{
						AgentSocketData asd=getBestAgentSocket(false);
						if (asd!=null) {
							sendData(asd.getAgentAddress(), new PauseBigDataTransferSystemMessage(p), true, null, false);
						}
					}
				}
				else if (_message.getClass() == ObjectMessage.class) {
					Object o = ((ObjectMessage<?>) _message).getContent();
					if (o.getClass() == AgentSocketData.class) {
						AgentSocketData asd = (AgentSocketData) o;
						if (logger != null && logger.isLoggable(Level.FINER))
							logger.finer("Receiving agent socket data (distantInterfacedKernelAddress="
									+ distant_kernel_address + ") : " + asd);

						if (asd.direct)
							this.agents_socket.add(asd);
						else
							this.indirect_agents_socket.add(asd);
						if (this.kernelAddressActivated)
							sendReplyWithRole(_message, new ObjectMessage<>(stats),
									LocalCommunity.Roles.DISTANT_KERNEL_AGENT_ROLE);
					} else if (o instanceof KernelAddress) {
						// distant kernel address received
						setDistantKernelAddress(_message.getSender(), (KernelAddress) o);
					} else if (o.getClass() == ReceivedSerializableObject.class
							&& (((ReceivedSerializableObject) o).getContent() instanceof LanMessage)) {
						ReceivedSerializableObject originalMessage = ((ReceivedSerializableObject) o);
						LanMessage lm = (LanMessage) originalMessage.getContent();
						if (this.distant_kernel_address == null) {
							originalMessage.markDataAsRead();
							return;
						}
						try {
							MadkitKernelAccess.setSender(lm.message, lm.message.getSender());
						} catch (IllegalArgumentException e) {
							originalMessage.markDataAsRead();
							if (logger != null)
								logger.severeLog("Unexpected exception", e);
							// processInvalidSerializedObject(new AccessException("Invalid kernel address
							// "+lm.message.getSender().getKernelAddress()), lm, true);
							return;
						}

						if (lm.getClass()==DirectLanMessage.class) {
							DirectLanMessage dlm = (DirectLanMessage) lm;
							if (logger != null && logger.isLoggable(Level.FINEST))
								logger.finest("Receiving direct lan message (distantInterfacedKernelAddress="
										+ distant_kernel_address + ") : " + dlm);
							if (dlm.message.getClass()==BigDataResultMessage.class) {
								cancelBigPacketDataToSendInQueue(MadkitKernelAccess.getIDPacket((BigDataResultMessage) dlm.message), false, null, true);
								//cancelBigPacketDataInQueue((BigDataResultMessage) dlm.message);
							}
							else if (dlm.message.getClass()==BigDataPropositionMessage.class)
								MadkitKernelAccess.setLocalMadkitKernel((BigDataPropositionMessage) dlm.message, this);
							this.sendMessageWithRole(LocalCommunity.Groups.NETWORK, LocalCommunity.Roles.NET_AGENT,
									new DirectLocalLanMessage(dlm.message, originalMessage),
									LocalCommunity.Roles.DISTANT_KERNEL_AGENT_ROLE);
						} else if (lm.getClass()==BroadcastLanMessage.class) {
							BroadcastLanMessage blm = (BroadcastLanMessage) lm;
							if (logger != null && logger.isLoggable(Level.FINEST))
								logger.finest("Receiving lan message to broadcast (distantInterfacedKernelAddress="
										+ distant_kernel_address + ") : " + blm);
							this.sendMessageWithRole(LocalCommunity.Groups.NETWORK, LocalCommunity.Roles.NET_AGENT,
									new BroadcastLocalLanMessage(blm.message, originalMessage, blm.abstract_group,
											blm.role, blm.agentAddressesSender),
									LocalCommunity.Roles.DISTANT_KERNEL_AGENT_ROLE);
						}
					} else if (o.getClass() == SecretMessage.class) {
						SecretMessage sm = (SecretMessage) o;
						if (!isConcernedBy(sm.getOriginalDistantKernelAgent())) {
							if (logger != null && logger.isLoggable(Level.FINER))
								logger.finer(
										"Receiving secret message and transmitting to best agent socket (distantInterfacedKernelAddress="
												+ distant_kernel_address + ")");
							// send a secret message in order to try to consider two connections as being
							// part of the same pair of peers.

							AgentSocketData asd = getBestAgentSocket(true);
							if (asd != null && kernelAddressActivated)
								sendData(asd.getAgentAddress(), sm, true, null, false);
							else {
								@SuppressWarnings("unchecked")
								ObjectMessage<SecretMessage> message = (ObjectMessage<SecretMessage>) _message;
								differSecretMessage(message);
							}
						}
					} else if (o.getClass() == AbstractAgentSocket.Groups.class) {
						AgentSocketData asd = getAgentSocketDataFromItsAgentAddress(_message.getSender());
						if (asd == null) {
							if (logger!=null)
								logger.warning(
										"Receiving message (AbstractAgentSocket.Groups) from unexpected agent address : "
												+ _message.getSender());
						} else {
							if (logger != null && logger.isLoggable(Level.FINEST))
								logger.finest("Groups update (distantInterfacedKernelAddress=" + distant_kernel_address
										+ ") from " + _message.getSender());

							asd.setAcceptedLocalGroups((Groups) o);
							updateSharedAcceptedGroups(false, true);
							//updateLocalAcceptedGroups();
							informHooksForAccessibleGroupsData();
						}

					} else if (o.getClass() == ConnectionInfoSystemMessage.class) {
						AgentSocketData asd = getAgentSocketDataFromItsAgentAddress(_message.getSender());
						if (asd != null) {
							if (logger != null && logger.isLoggable(Level.FINEST))
								logger.finest("Receiving connection information from " + _message.getSender()
										+ " (distantInterfacedKernelAddress=" + distant_kernel_address + ") : " + o);

							asd.setConnectionInfo((ConnectionInfoSystemMessage) o);
						} else {
							Logger logger=getLogger();
							if (logger!=null)
								logger.severe(
									"Impossible to found agent socket data from agent address " + _message.getSender()
											+ ". So impossible to set given ConnectionInfoSystemMessage.");
						}
					} else if (o.getClass() == BigDataPropositionMessage.class) {
						BigDataPropositionMessage bdpm = (BigDataPropositionMessage) o;

						if (bdpm.getSender().getKernelAddress().equals(distant_kernel_address)) {
							AgentSocketData asd = getBestAgentSocket(false);
							if (asd != null) {
								if (logger != null && logger.isLoggable(Level.FINEST))
									logger.finest("Receiving big data proposition message " + _message.getSender()
											+ " (distantInterfacedKernelAddress=" + distant_kernel_address + ") : "
											+ o);

								int id = MadkitKernelAccess.getIDPacket(bdpm);
								current_big_data_readings.put(id, new BigDataReading(bdpm));
								AbstractDecentralizedIDGenerator asynchronousBigDataInternalIdentifier = bdpm.getAsynchronousBigDataInternalIdentifier();
								if (asynchronousBigDataInternalIdentifier!=null)
									MadkitKernelAccess.setAsynchronousTransferAsStarted(this, asynchronousBigDataInternalIdentifier);
								sendData(asd.getAgentAddress(), new ValidateBigDataProposition(id), true, null, false);
							}
						}
					} else if (o.getClass() == ValidateBigDataProposition.class) {
						validatePacketDataInQueue(((ValidateBigDataProposition) o).getIDPacket());
					} else if (o.getClass() == DistantKernelAddressValidated.class) {
						activateDistantKernelAgentWithDistantConfirmation(_message.getSender());
					}

				}
			} catch (SelfKillException e) {
				throw e;
			} catch (Exception e) {
				if (logger != null)
					logger.severeLog("Unexpected exception", e);
				this.killAgent(this);
			}
		} finally {
			concurrent = false;
		}
	}

	private void differSecretMessage(ObjectMessage<SecretMessage> _message) {
		if (differedSecretMessages == null)
			differedSecretMessages = new ArrayList<>();
		differedSecretMessages.add(_message);
	}

	private void purgeDifferedSecretMessages() {
		if (differedSecretMessages != null) {
			for (Message m : differedSecretMessages)
				receiveMessage(m);
			differedSecretMessages = null;
		}
	}

	private AgentSocketData getAgentSocketDataForTransfer(ArrayList<AgentSocketData> agents_socket,
			AskForTransferMessage m) {
		for (AgentSocketData asd : agents_socket) {
			if (asd.isUsable() && (asd.distant_inet_socket_address.equals(m.getInetSocketAddress1())
					|| asd.distant_inet_socket_address.equals(m.getInetSocketAddress2()))) {
				if (asd.numberOfIntermediatePeers + 1 <= getMadkitConfig().networkProperties.gatewayDepth) {
					return asd;
				}
			}
		}
		return null;

	}

	private void addBigDataInQueue(AgentSocketData chosenSocket, BigDataPropositionMessage bgpm)
			throws PacketException {
		RandomInputStream inputStream = MadkitKernelAccess.getInputStream(bgpm);
		if (inputStream != null) {
			if (logger != null && logger.isLoggable(Level.FINEST))
				logger.finest("Adding big data in queue (distantInterfacedKernelAddress=" + distant_kernel_address
						+ ") : " + bgpm);

			int id = getNewPacketID();
			MadkitKernelAccess.setIDPacket(bgpm, id);
			WritePacket packet = new WritePacket(PacketPartHead.TYPE_PACKET, id,
					getMadkitConfig().networkProperties.maxBufferSize,
					bgpm.bigDataExcludedFromEncryption()?0:getMadkitConfig().networkProperties.maxRandomPacketValues, random, inputStream,
					bgpm.getStartStreamPosition(), bgpm.getTransferLength(), true, bgpm.getMessageDigestType());
			BigPacketData packetData = new BigPacketData(this, chosenSocket.getAgentAddress(), packet, bgpm.getReceiver(),
					bgpm.getSender(), bgpm.getConversationID(), bgpm.getStatistics(), bgpm.bigDataExcludedFromEncryption(), bgpm.getAsynchronousBigDataInternalIdentifier(), bgpm.getExternalAsynchronousBigDataIdentifier());
			packetsDataInQueue.put(id, packetData);
		}
	}

	private BigPacketData cancelBigPacketDataToSendInQueue(int idTransfer, boolean sendCancelMessageToDistantPeer, BigDataResultMessage.Type resultType, boolean updateDatabase) throws NIOException {
		if (logger != null && logger.isLoggable(Level.FINEST))
			logger.finest("Cancel big data in queue (distantInterfacedKernelAddress=" + distant_kernel_address
					+ ", idTransfer=" + idTransfer + ")");
		BigPacketData bpd=packetsDataInQueue.remove(idTransfer);


		if (bpd!=null) {
			try {
				bpd.cancel();
			} catch (IOException | MadkitException e) {
				if (logger != null)
					logger.severeLog("Cannot close stream of BigPacketData", e);
			}
			if (sendCancelMessageToDistantPeer)
			{
				AgentSocketData asd = getBestAgentSocket(false);
				if (asd!=null) {
					sendData(asd.getAgentAddress(), new CancelBigDataSystemMessage(bpd.getIDPacket(), true, resultType), true, null, false);
				}
			}

			if (resultType!=null ) {
				MadkitKernelAccess.transferLostForBigDataTransfer(this, bpd.getConversationID(),
						bpd.getIDPacket(), bpd.getReceiver(), bpd.getCaller(),
						bpd.getReadDataLength(), bpd.getDurationInNano()/1000000L,
						bpd.getDifferedBigDataInternalIdentifier(), bpd.getDifferedBigDataIdentifier(), resultType, updateDatabase);
			}

		}
		return bpd;
	}
	private BigDataReading cancelBigPacketDataToReceiveInQueue(int idPacket)
	{
		if (logger != null && logger.isLoggable(Level.FINEST))
			logger.finest("Cancel big data in queue (distantInterfacedKernelAddress=" + distant_kernel_address
					+ ", idPacket=" + idPacket + ")");

		BigDataReading bdr=current_big_data_readings.remove(idPacket);
		if (bdr!=null) {
			try {
				bdr.cancel();
			} catch (IOException e) {
				if (logger != null)
					logger.severeLog("Cannot close stream of BigDataReading", e);
			}
		}
		return bdr;
	}

	private boolean validatePacketDataInQueue(int IDPacket) {
		BigPacketData bpd = packetsDataInQueue.get(IDPacket);
		if (bpd == null) {
			if (logger!=null)
				logger.warning("Big data in queue not valid (distantInterfacedKernelAddress=" + distant_kernel_address
						+ ", IDPacket=" + IDPacket + ")");

			return false;
		} else {
			if (logger != null && logger.isLoggable(Level.FINEST))
				logger.finest("Big data in queue valid (distantInterfacedKernelAddress=" + distant_kernel_address
						+ ", IDPacket=" + IDPacket + ")");
			AbstractDecentralizedIDGenerator asynchronousBigDataInternalIdentifier = bpd.getDifferedBigDataInternalIdentifier();
			if (asynchronousBigDataInternalIdentifier!=null)
				MadkitKernelAccess.setAsynchronousTransferAsStarted(this, asynchronousBigDataInternalIdentifier);
			sendMessage(bpd.getAgentSocketSender(), new DistKernADataToUpgradeMessage(bpd));
			return true;
		}
	}

	private AgentSocketData getAgentSocketDataFromItsAgentAddress(AgentAddress aa) {
		for (AgentSocketData asd : agents_socket) {
			if (asd.isConcernedBy(aa))
				return asd;
		}
		for (AgentSocketData asd : indirect_agents_socket) {
			if (asd.isConcernedBy(aa))
				return asd;
		}
		return null;

	}

	static class ExceededDataQueueSize extends Message {

		private final boolean paused;
		private final boolean purge;
		private HashMap<Integer, SerializedReading> readingToPurge = null;
		protected final NetworkBoard networkBoard;
		protected final boolean hasOtherCandidates;

		ExceededDataQueueSize(NetworkBoard networkBoard, boolean hasOtherCandidates, boolean paused) {
			this(networkBoard, hasOtherCandidates, paused, false);
		}

		ExceededDataQueueSize(NetworkBoard networkBoard, boolean hasOtherCandidates, boolean paused,
							  boolean purge) {
			this.networkBoard = networkBoard;
			this.paused = paused;
			this.purge = purge;
			this.hasOtherCandidates = hasOtherCandidates;
		}

		boolean isPaused() {
			return paused;
		}

		boolean mustPurge() {
			return purge;
		}

		@Override
		public String toString() {
			return "ExceededDataQueueSize[paused=" + paused + ", purge=" + purge + "]";
		}

		void candidateForPurge(DistantKernelAgent caller) {
			synchronized (networkBoard.candidatesForPurge) {
				if (!networkBoard.candidatesForPurge.contains(caller)) {
					networkBoard.candidatesForPurge.add(caller);
					networkBoard.candidatesForPurge.notifyAll();
				}

			}
		}

		boolean tryPurgeNow(DistantKernelAgent caller) throws InterruptedException {
			synchronized (networkBoard.candidatesForPurge) {
				if (networkBoard.currentCandidateForPurge == null
						|| networkBoard.currentCandidateForPurge == caller) {
					if (hasOtherCandidates && networkBoard.candidatesForPurge.size() == 0) {
						LockerCondition lc = new LockerCondition() {

							@Override
							public boolean isLocked() {
								return networkBoard.candidatesForPurge.size() > 0;
							}
						};
						lc.setLocker(networkBoard.candidatesForPurge);
						caller.wait(lc);
					}
					networkBoard.currentCandidateForPurge = caller;
					return true;
				} else {
					if (!networkBoard.candidatesForPurge.contains(caller))
						networkBoard.candidatesForPurge.add(caller);
					return false;
				}
			}
		}


		// @SuppressWarnings("synthetic-access")
		void purgeFinished(DistantKernelAgent caller) {
			synchronized (networkBoard.candidatesForPurge) {
				if (caller.taskForPurgeCheck != null) {
					caller.cancelTask(caller.taskForPurgeCheck, true);
					caller.taskForPurgeCheck = null;
				}
				if (networkBoard.transferPausedForAllDistantKernelAgent.get())
					caller.globalExceededDataQueueSize.set(new ExceededDataQueueSize(networkBoard, false, true));
				else
					caller.globalExceededDataQueueSize.set(null);
				if (networkBoard.currentCandidateForPurge == caller) {
					networkBoard.currentCandidateForPurge = null;
					if (!networkBoard.candidatesForPurge.isEmpty()) {
						if (networkBoard.transferPausedForAllDistantKernelAgent.get())
							networkBoard.currentCandidateForPurge = networkBoard.candidatesForPurge
									.remove(networkBoard.candidatesForPurge.size() - 1);
						else {
							networkBoard.candidatesForPurge.clear();
						}
						if (networkBoard.currentCandidateForPurge != null)
							networkBoard.currentCandidateForPurge
									.receiveMessage(new ExceededDataQueueSize(networkBoard, false, true, true));
					} else
						caller.setGlobalTransfersPaused(false);
				} else {
					networkBoard.candidatesForPurge.remove(caller);
					if (networkBoard.currentCandidateForPurge == null
							|| networkBoard.candidatesForPurge.isEmpty()) {
						caller.setGlobalTransfersPaused(false);
					}
				}
			}
		}

		void setOneSocketPurged(DistantKernelAgent caller, Integer id, SerializedReading sr) {
			if (isPaused() && mustPurge()) {
				if (caller.logger != null && caller.logger.isLoggable(Level.FINEST))
					caller.logger.finest("Set one socket purged (distantInterfacedKernelAddress="
							+ caller.distant_kernel_address + ", agentAddress=" + sr.getInitialAgentAddress() + ")");

				removeReading(id);
				if (!hasReadingsToPurge()) {
					purgeFinished(caller);
				}

			}

		}

		void purgeCanceled(DistantKernelAgent caller) {
			purgeFinished(caller);
		}

		@SuppressWarnings("unchecked")
		void setReadingsToPurge(HashMap<Integer, SerializedReading> readings) {
			readingToPurge = (HashMap<Integer, SerializedReading>) readings.clone();
		}

		void removeReading(Integer id) {
			readingToPurge.remove(id);
		}

		boolean hasReadingsToPurge() {
			return !readingToPurge.isEmpty();
		}

	}

	private boolean hasUsableDistantSocketAgent(ArrayList<AgentSocketData> agents_socket) {
		for (AgentSocketData asd : agents_socket) {
			if (asd.isUsable())
				return true;
		}
		return false;
	}

	private boolean hasUsableDistantSocketAgent() {
		return hasUsableDistantSocketAgent(agents_socket);
	}

	private int getUsableDistantSocketAgentNumber(ArrayList<AgentSocketData> agents_socket) {
		int nb = 0;
		for (AgentSocketData asd : agents_socket) {
			if (asd.isUsable())
				++nb;
		}
		return nb;
	}

	private int getUsableDistantSocketAgentNumber() {
		return getUsableDistantSocketAgentNumber(agents_socket)
				+ getUsableDistantSocketAgentNumber(indirect_agents_socket);
	}

	private void activateDistantKernelAgentWithDistantConfirmation(AgentAddress sender) throws MadkitException {
		AgentSocketData asd = getAgentSocketDataFromItsAgentAddress(sender);
		if (asd != null) {
			boolean hasPreviousUsableSocketsAgents = hasUsableDistantSocketAgent();
			asd.setUsable(true);
			
			if (!hasPreviousUsableSocketsAgents && kernelAddressActivated) {
				activateDistantKernelAgent();
				informHooksDistantKernelAgentActivated();
			}
			else
				networkBoard.unlockSimultaneousConnections(distant_kernel_address);
			int nb = getUsableDistantSocketAgentNumber();
			if (nb > 1
					&& nb > getMadkitConfig().networkProperties.numberOfMaximumConnectionsBetweenTwoSameKernelsAndMachines)
				sendMessageWithRole(sender,
						new ObjectMessage<>(new TooMuchConnectionWithTheSamePeers(asd.distant_inet_socket_address)),
						LocalCommunity.Roles.DISTANT_KERNEL_AGENT_ROLE);

		} else {
			Logger logger=getLogger();
			if (logger!=null)
				logger.severe("Unknown agent socket " + sender);
		}
	}

	private void activateDistantKernelAgent() throws MadkitException {
		
		ReturnCode rc = this.requestRole(LocalCommunity.Groups.DISTANT_KERNEL_AGENTS_GROUPS,
				LocalCommunity.Roles.DISTANT_KERNEL_AGENT_ROLE);
		if (rc.equals(ReturnCode.SUCCESS)) {
			updateSharedAcceptedGroups(true, true);
			informHooksForLoginsData();

			potentialChangesInGroups();
			networkBoard.unlockSimultaneousConnections(distant_kernel_address);
		} else {
			Logger logger=getLogger();
			if (logger!=null)
				logger.severe("Unexpected return code during distant kernel agent activation : " + rc);
		}
	}

	private void informHooksDistantKernelAgentActivated() {
		MadkitKernelAccess.informHooks(this,
				new DistantKernelAgentEventMessage(AgentActionEvent.DISTANT_KERNEL_CONNECTED, distant_kernel_address));
		if (logger != null)
			logger.info("New distant madkit kernel connected (distantInterfacedKernelAddress=" + distant_kernel_address
					+ ", localKernelAddress=" + getKernelAddress() + ")");
	}

	private void activateDistantKernelAgent(AgentAddress sender, boolean currentDistantKernelAgentEnabled)
			throws MadkitException {
		if (currentDistantKernelAgentEnabled) {
			boolean previous_activated = kernelAddressActivated;

			boolean inform_hooks = false;
			if (!previous_activated) {
				// activate the current distant kernel agent
				kernelAddressActivated = true;
				stats = getMadkitConfig().networkProperties.addIfNecessaryAndGetStatsBandwidth(distant_kernel_address);

				if (hasUsableDistantSocketAgent()) {
					
					activateDistantKernelAgent();
					inform_hooks = true;
				}
				purgeDifferedSecretMessages();

			}

			sendMessageWithRole(sender, new ObjectMessage<>(this.distant_kernel_address),
					LocalCommunity.Roles.DISTANT_KERNEL_AGENT_ROLE);

			sendMessageWithRole(sender, new ObjectMessage<>(stats),
					LocalCommunity.Roles.DISTANT_KERNEL_AGENT_ROLE);

			if (inform_hooks) {
				informHooksDistantKernelAgentActivated();
			}

		} else {
			receiveMessage(new KillYou());
			this.leaveRole(LocalCommunity.Groups.getOriginalDistantKernelAgentGroup(this.distant_kernel_address),
					LocalCommunity.Roles.DISTANT_KERNEL_AGENT_ROLE);
		}
		

	}

	static class KillYou extends Message {


	}

	/*
	 * An agent socket sent its concerned distant kernel address.
	 */
	private void setDistantKernelAddress(AgentAddress sender, KernelAddress distant_ka) throws MadkitException {

		if (this.distant_kernel_address == null) {
			if (logger != null && logger.isLoggable(Level.FINER))
				logger.finer("Setting distant kernel address (distantKernelAddress=" + distant_ka + ")");

			try {

				networkBoard.lockForSimultaneousConnections(this, distant_ka);

			} catch (InterruptedException e) {
				e.printStackTrace();
				this.killAgent(this);
			}

			// save the distant kernel address
			this.distant_kernel_address = new KernelAddressInterfaced(distant_ka, true);

			// test if the distant kernel address has already been binded
			boolean duplicate_group = this.getAgentsWithRole(
					LocalCommunity.Groups.getOriginalDistantKernelAgentGroup(this.distant_kernel_address),
					LocalCommunity.Roles.DISTANT_KERNEL_AGENT_ROLE, false).size() > 0;
			// the current agent join the original distant kernel address
			this.requestRole(LocalCommunity.Groups.getOriginalDistantKernelAgentGroup(this.distant_kernel_address),
					LocalCommunity.Roles.DISTANT_KERNEL_AGENT_ROLE);
			if (duplicate_group) {
				if (logger != null && logger.isLoggable(Level.FINER))
					logger.finer("duplicate kernel address (distantKernelAddress=" + distant_ka + ")");
				// interface the received distant kernel address
				this.distant_kernel_address = new KernelAddressInterfaced(distant_ka, false);
				// the current agent join the interfaced distant kernel address
				this.requestRole(LocalCommunity.Groups.getDistantKernelAgentGroup(getNetworkID()),
						LocalCommunity.Roles.DISTANT_KERNEL_AGENT_ROLE);
			} else {
				// the current agent join the interfaced distant kernel address
				this.requestRole(LocalCommunity.Groups.getDistantKernelAgentGroup(getNetworkID()),
						LocalCommunity.Roles.DISTANT_KERNEL_AGENT_ROLE);
				// activate the current distant kernel agent
				activateDistantKernelAgent(sender, true);
			}
			// send to the concerned agent socket the result of the current agent activation
			sendMessageWithRole(sender, new KernelAddressValidation(duplicate_group),
					LocalCommunity.Roles.DISTANT_KERNEL_AGENT_ROLE);

		}
		else {
			if (logger!=null)
				logger.warning("Setting distant kernel address already done (currentDistantKernelAddress="
					+ distant_kernel_address + ", receivedKernelAddress=" + distant_ka + ")");
		}

	}

	private void updateDistantAcceptedGroups(ArrayList<AgentSocketData> agents_socket, ListGroupsRoles general,
			ListGroupsRoles groups) {
		for (AgentSocketData a : agents_socket) {
			if (a.distant_accessible_and_requested_groups2 != null) {
				groups.addListGroupsRoles(a.distant_accessible_and_requested_groups2);
				general.addListGroupsRoles(a.distant_general_accessible_groups);
			}
		}
	}


	private void updateLoginData(ArrayList<AgentSocketData> agents_socket, Set<PairOfIdentifiers> ids,
			Set<PairOfIdentifiers> idsnewa,
								 Set<CloudIdentifier> lastDeniedCloudIdentifiersToOther,
								 Set<Identifier> lastDeniedIdentifiersFromOther,
								 Set<Identifier> lastDeniedIdentifiersToOther,
								 Set<PairOfIdentifiers> idsnewu) {
		for (AgentSocketData a : agents_socket) {
			ids.addAll(a.getAcceptedPairOfIdentifiers());
			idsnewa.addAll(a.getLastAcceptedPairOfIdentifiers());
			lastDeniedCloudIdentifiersToOther.addAll(a.getLastDeniedCloudIdentifiersToOther());
			lastDeniedIdentifiersFromOther.addAll(a.getLastDeniedIdentifiersFromOther());
			lastDeniedIdentifiersToOther.addAll(a.getLastDeniedIdentifiersToOther());
			idsnewu.addAll(a.getLastUnLoggedPairOfIdentifiers());
		}
	}

	private void updateLoginData() {
		Set<PairOfIdentifiers> ids = new HashSet<>();
		Set<PairOfIdentifiers> idsnewa = new HashSet<>();
		Set<CloudIdentifier> lastDeniedCloudIdentifiersToOther=new HashSet<>();
		Set<Identifier> lastDeniedIdentifiersFromOther=new HashSet<>();
		Set<Identifier> lastDeniedIdentifiersToOther=new HashSet<>();
		Set<PairOfIdentifiers> idsnewu = new HashSet<>();
		updateLoginData(agents_socket, ids, idsnewa, lastDeniedCloudIdentifiersToOther, lastDeniedIdentifiersFromOther, lastDeniedIdentifiersToOther, idsnewu);
		updateLoginData(indirect_agents_socket, ids, idsnewa, lastDeniedCloudIdentifiersToOther, lastDeniedIdentifiersFromOther, lastDeniedIdentifiersToOther, idsnewu);

		idsnewa.removeIf(ids::contains);
		idsnewu.removeIf(ids::contains);

		accepted_identifiers = new ArrayList<>();
		accepted_identifiers.addAll(ids);
		last_accepted_identifiers.removeAll(idsnewu);
		accepted_loop:for (Iterator<PairOfIdentifiers> it=last_accepted_identifiers.iterator();it.hasNext();)
		{
			PairOfIdentifiers poi=it.next();
			for (CloudIdentifier cloudIdentifier : lastDeniedCloudIdentifiersToOther)
			{
				if (poi.getCloudIdentifier().equalsTimeConstant(cloudIdentifier))
				{
					it.remove();
					continue accepted_loop;
				}
			}
			for (Identifier cloudIdentifier : lastDeniedIdentifiersFromOther)
			{
				if (poi.equalsLocalIdentifier(cloudIdentifier))
				{
					it.remove();
					continue accepted_loop;
				}
			}
			for (Identifier cloudIdentifier : lastDeniedIdentifiersToOther)
			{
				if (poi.equalsDistantIdentifier(cloudIdentifier))
				{
					it.remove();
					continue accepted_loop;
				}
			}
		}
		last_accepted_identifiers.addAll(idsnewa);

		this.lastDeniedCloudIdentifiersToOther.addAll(lastDeniedCloudIdentifiersToOther);
		this.lastDeniedIdentifiersFromOther.addAll(lastDeniedIdentifiersFromOther);
		this.lastDeniedIdentifiersToOther.addAll(lastDeniedIdentifiersToOther);
		last_un_logged_identifiers.removeAll(idsnewa);
		last_un_logged_identifiers.addAll(idsnewu);

		informHooksForLoginsData();
		if (logger != null && logger.isLoggable(Level.FINEST))
			logger.finest("Login data updated (distantInterfacedKernelAddress=" + distant_kernel_address + ")");

	}

	private void informHooksForLoginsData() {
		if (this.kernelAddressActivated && (accepted_identifiers.size() > 0 || last_accepted_identifiers.size() > 0
				|| lastDeniedCloudIdentifiersToOther.size() > 0
				|| lastDeniedIdentifiersFromOther.size()>0 || lastDeniedIdentifiersToOther.size()>0
				|| last_un_logged_identifiers.size() > 0)) {
			MadkitKernelAccess.informHooks(this,
					new NetworkLoginAccessEvent(this.distant_kernel_address, accepted_identifiers,
							last_accepted_identifiers, lastDeniedCloudIdentifiersToOther, lastDeniedIdentifiersFromOther, lastDeniedIdentifiersToOther, last_un_logged_identifiers));
			last_accepted_identifiers = new ArrayList<>();
			lastDeniedCloudIdentifiersToOther = new ArrayList<>();
			lastDeniedIdentifiersFromOther = new ArrayList<>();
			lastDeniedIdentifiersToOther = new ArrayList<>();
			last_un_logged_identifiers = new ArrayList<>();
		}
	}
	private void informHooksForAccessibleGroupsData() {

		if (this.kernelAddressActivated ) {
			ListGroupsRoles mg = computeLocalGeneralAcceptedGroups();
			MadkitKernelAccess.informHooks(this, new NetworkGroupsAccessEvent(
					AgentActionEvent.ACCESSIBLE_LAN_GROUPS_GIVEN_TO_DISTANT_PEER, mg, localAcceptedAndRequestedGroups, distant_kernel_address, false));
		}
	}

	@Override
	public void activate() {
		setLogLevel(getMadkitConfig().networkProperties.networkLogLevel);
		this.filteredObjectResolver=new FilteredObjectResolver(getMadkitConfig().networkProperties);
		this.lockSocketUntilCGRSynchroIsSent= getMadkitConfig().networkProperties != null && getMadkitConfig().networkProperties.lockSocketUntilCGRSynchroIsSent;
		if (logger != null && logger.isLoggable(Level.FINE))
			logger.fine("Launching DistantKernelAgent  (" + this.distant_kernel_address + ") ... !");

		this.requestRole(LocalCommunity.Groups.NETWORK, LocalCommunity.Roles.DISTANT_KERNEL_AGENT_ROLE);

		networkBoard = (NetworkBoard) getBoard(LocalCommunity.Groups.NETWORK,
				LocalCommunity.Boards.NETWORK_BOARD);
		try {
			random = getMadkitConfig().getApprovedSecureRandom();
		} catch (Exception e) {
			if (logger != null)
				logger.severeLog("Unexpected exception", e);
		}
		if (logger != null && logger.isLoggable(Level.FINE))
			logger.fine("DistantKernelAgent  (" + this.distant_kernel_address + ") LAUNCHED !");
	}

	private void updateBestAgent() {
		if (lastAgentsUpdateNano == -1 || lastAgentsUpdateNano
				+ getMadkitConfig().networkProperties.delayInMsBetweenEachAgentSocketOptimization*1000000L < System
						.nanoTime()) {
			Collections.sort(agents_socket);
			Collections.sort(indirect_agents_socket);
			lastAgentsUpdateNano = System.nanoTime();
		}
	}

	private void potentialChangesInGroups(ArrayList<AgentSocketData> agents_socket) throws MadkitException {
		for (AgentSocketData asd : agents_socket) {
			if (asd.isUsable())
				asd.potentialChangesInGroups(this);
		}
	}

	protected void potentialChangesInGroups() throws MadkitException {
		potentialChangesInGroups(agents_socket);
		potentialChangesInGroups(indirect_agents_socket);
	}

	protected AgentSocketData getBestAgentSocketForLocalAgentAddress(KernelAddress distantKernelAddress, AgentAddress agentAddress,
																	 @SuppressWarnings("SameParameterValue") boolean testOnlyRequestedGroups) {
		updateBestAgent();

		if (!distantKernelAddress.equals(distant_kernel_address)) {
			return null;
		}

		AgentSocketData asd = getFirstValidAgentSocketDataForLocalAgentAddress(agents_socket, agentAddress,
				testOnlyRequestedGroups);
		if (asd == null)
			asd = getFirstValidAgentSocketDataForLocalAgentAddress(indirect_agents_socket, agentAddress,
					testOnlyRequestedGroups);
		return asd;
	}

	protected AgentSocketData getBestAgentSocket(AgentAddress agentAddressSender, AgentAddress agentAddressReceiver,
												 @SuppressWarnings("SameParameterValue") boolean testOnlyRequestedGroups) {
		updateBestAgent();

		if (!agentAddressReceiver.getKernelAddress().equals(distant_kernel_address)) {
			return null;
		}

		AgentSocketData asd = getFirstValidAgentSocketData(agents_socket, agentAddressSender, agentAddressReceiver,
				testOnlyRequestedGroups);
		if (asd == null)
			asd = getFirstValidAgentSocketData(indirect_agents_socket, agentAddressSender, agentAddressReceiver,
					testOnlyRequestedGroups);

		return asd;
	}


	private AgentSocketData getFirstValidAgentSocketDataForLocalAgentAddress(ArrayList<AgentSocketData> agents_socket,
														 AgentAddress agentAddress, boolean testOnlyRequestedGroups) {
		for (AgentSocketData asd : agents_socket) {
			if (asd.isUsable() && asd.acceptLocalAgentAddressToSend(getKernelAddress(),agentAddress, testOnlyRequestedGroups))
				return asd;
		}
		return null;
	}

	private AgentSocketData getFirstValidAgentSocketData(ArrayList<AgentSocketData> agents_socket,
														 AgentAddress agentAddressSender, AgentAddress agentAddressReceiver, boolean testOnlyRequestedGroups) {
		for (AgentSocketData asd : agents_socket) {
			if (asd.isUsable() && asd.acceptMessageToSend(getKernelAddress(), agentAddressSender, agentAddressReceiver, testOnlyRequestedGroups))
				return asd;
		}
		return null;
	}
	private AgentSocketData getBestAgentSocket(boolean includeNotUsable) {
		updateBestAgent();

		AgentSocketData asd = getFirstValidAgentSocketData(includeNotUsable, agents_socket);
		if (asd == null)
			asd = getFirstValidAgentSocketData(includeNotUsable, indirect_agents_socket);
		return asd;
	}

	private AgentSocketData getFirstValidAgentSocketData(boolean includeNotUsable,
			ArrayList<AgentSocketData> agents_socket) {
		for (AgentSocketData asd : agents_socket) {
			if (includeNotUsable || asd.isUsable())
				return asd;
		}
		return null;
	}

	private AgentSocketData getFirstValidAgentSocketDataCompatibleForTransfer(
			ArrayList<AgentSocketData> agents_socket) {
		for (AgentSocketData asd : agents_socket) {
			if (asd.isUsable() && asd.numberOfIntermediatePeers + 1 <= getMadkitConfig().networkProperties.gatewayDepth)
				return asd;
		}
		return null;
	}

	private AgentSocketData getBestAgentSocketCompatibleForTransfer() {
		updateBestAgent();

		AgentSocketData asd = getFirstValidAgentSocketDataCompatibleForTransfer(agents_socket);
		if (asd == null)
			asd = getFirstValidAgentSocketDataCompatibleForTransfer(indirect_agents_socket);
		return asd;
	}

	static class AgentSocketData implements Comparable<AgentSocketData> {
		// final AbstractAgentSocket agent;
		private final AgentAddress global_address;
		private final AbstractAgentSocket abstractAgentSocket;
		private AgentAddress address;
		final StatsBandwidth stat;
		ListGroupsRoles distant_accessible_and_requested_groups = new ListGroupsRoles();
		ListGroupsRoles distant_general_accessible_groups = new ListGroupsRoles();
		ListGroupsRoles distant_accessible_and_requested_groups2 = null;
		final boolean direct;
		private List<PairOfIdentifiers> accepted_identifiers = new ArrayList<>();
		private ArrayList<PairOfIdentifiers> last_accepted_identifiers = new ArrayList<>();
		private ArrayList<CloudIdentifier> lastDeniedCloudIdentifiersToOther = new ArrayList<>();
		private ArrayList<Identifier> lastDeniedIdentifiersFromOther = new ArrayList<>();
		private ArrayList<Identifier> lastDeniedIdentifiersToOther = new ArrayList<>();
		private ArrayList<PairOfIdentifiers> last_un_logged_identifiers = new ArrayList<>();
		private Groups myAcceptedGroups = null;
		final InetSocketAddress distant_inet_socket_address;
		final int numberOfIntermediatePeers;
		ConnectionInfoSystemMessage distantConnectionInfo = null;
		private boolean distantKernelAddressValidated;
		//private final CounterSelector counterSelector;

		AgentSocketData(AbstractAgentSocket _agent_socket) {
			this.abstractAgentSocket=_agent_socket;
			global_address = address = _agent_socket.getAgentAddressIn(LocalCommunity.Groups.NETWORK,
					LocalCommunity.Roles.SOCKET_AGENT_ROLE);
			stat = _agent_socket.getStatistics();
			direct = _agent_socket instanceof AgentSocket;
			distant_inet_socket_address = _agent_socket.getDistantInetSocketAddress();
			numberOfIntermediatePeers = _agent_socket.getNumberOfIntermediatePeers();
			distantKernelAddressValidated = false;
		}


		@SuppressWarnings("SameParameterValue")
		void setUsable(boolean value) {
			distantKernelAddressValidated = value;
		}

		public boolean isUsable() {
			return distantKernelAddressValidated && abstractAgentSocket.getState()==State.LIVING;
		}

		@Override
		public String toString() {
			return "AgentSocketData[global_address=" + global_address + ", direct=" + direct + "]";
		}

		@Override
		public int compareTo(@SuppressWarnings("NullableProblems") AgentSocketData _o) {
			if (_o==null)
				throw new NullPointerException();
			if (this.isUsable() && !_o.isUsable())
				return 1;
			else if (!this.isUsable() && _o.isUsable())
				return -1;

			TransferSpeedStat t1 = stat.getBytesUploadedInRealBytes(NetworkProperties.DEFAULT_STAT_PER_512KB_SEGMENTS);
			TransferSpeedStat t2 = _o.stat
					.getBytesUploadedInRealBytes(NetworkProperties.DEFAULT_STAT_PER_512KB_SEGMENTS);
			if (t1.isOneCycleDone()) {
				if (!t2.isOneCycleDone())
					return -1;
			} else {
				if (t2.isOneCycleDone())
					return 1;
				else
					return 0;
			}

			double v = t1.getBytesPerSecond() - t2.getBytesPerSecond();
			return Double.compare(v, 0.0);
		}

		void setConnectionInfo(ConnectionInfoSystemMessage distantConnectionInfo) {
			this.distantConnectionInfo = distantConnectionInfo;
		}

		ConnectionInfoSystemMessage getConnectionInfo() {
			return this.distantConnectionInfo;
		}

		boolean isConcernedBy(AgentAddress agentAddress) {
			if (address.representsSameAgentThan(agentAddress)) {
				address = agentAddress;
				return true;
			} else
				return false;
		}

		AgentAddress getAgentAddress() {
			return address;
		}

		AgentAddress getGlobalAgentAddress() {
			return global_address;
		}

		boolean acceptMessageToSend(KernelAddress localKernelAddress, AgentAddress agentAddressSender, AgentAddress agentAddressReceiver, boolean testOnlyRequestedGroups) {

			if (testOnlyRequestedGroups) {
				if (!distant_accessible_and_requested_groups.includesDistant(localKernelAddress, agentAddressSender))
					return false;
			}
			else {
					if (!distant_general_accessible_groups.includesDistant(localKernelAddress, agentAddressSender))
						return false;
			}
			return getAcceptedLocalGroups().getGroups().getGroups().includes(agentAddressReceiver.getGroup())
					&& getAcceptedLocalGroups().acceptLocal(agentAddressSender);
		}

		boolean acceptLocalAgentAddressToSend(KernelAddress localKernelAddress, AgentAddress agentAddressSender, boolean testOnlyRequestedGroups) {

			if (testOnlyRequestedGroups) {
				if (!distant_accessible_and_requested_groups.includesDistant(localKernelAddress, agentAddressSender))
					return false;
			}
			else {
				if (!distant_general_accessible_groups.includesDistant(localKernelAddress, agentAddressSender))
					return false;
			}
			return getAcceptedLocalGroups().acceptLocal(agentAddressSender);
		}


		void setDistantAccessibleGroups(NetworkGroupsAccessEvent event) {
			distant_general_accessible_groups = event.getGeneralAcceptedGroups();
			distant_accessible_and_requested_groups = event.getRequestedAccessibleGroups();
			distant_accessible_and_requested_groups2 = event.getRequestedAccessibleGroups();
		}

		void setAcceptedIdentifiers(NetworkLoginAccessEvent event) {
			accepted_identifiers = event.getCurrentIdentifiers();
			last_accepted_identifiers.addAll(event.getNewAcceptedIdentifiers());
			lastDeniedCloudIdentifiersToOther.addAll(event.getNewDeniedCloudIdentifiersToOther());
			lastDeniedIdentifiersToOther.addAll(event.getNewDeniedIdentifiersToOther());
			lastDeniedIdentifiersFromOther.addAll(event.getNewDeniedIdentifiersFromOther());
			last_un_logged_identifiers.addAll(event.getNewUnLoggedIdentifiers());
		}

		List<PairOfIdentifiers> getAcceptedPairOfIdentifiers() {
			return accepted_identifiers;
		}

		ArrayList<CloudIdentifier> getLastDeniedCloudIdentifiersToOther() {
			ArrayList<CloudIdentifier> res = lastDeniedCloudIdentifiersToOther;
			lastDeniedCloudIdentifiersToOther = new ArrayList<>();
			return res;
		}
		ArrayList<Identifier> getLastDeniedIdentifiersFromOther() {
			ArrayList<Identifier> res = lastDeniedIdentifiersFromOther;
			lastDeniedIdentifiersFromOther = new ArrayList<>();
			return res;
		}
		ArrayList<Identifier> getLastDeniedIdentifiersToOther() {
			ArrayList<Identifier> res = lastDeniedIdentifiersToOther;
			lastDeniedIdentifiersToOther = new ArrayList<>();
			return res;
		}

		ArrayList<PairOfIdentifiers> getLastAcceptedPairOfIdentifiers() {
			ArrayList<PairOfIdentifiers> res = last_accepted_identifiers;
			last_accepted_identifiers = new ArrayList<>();
			return res;
		}

		ArrayList<PairOfIdentifiers> getLastUnLoggedPairOfIdentifiers() {
			ArrayList<PairOfIdentifiers> res = last_un_logged_identifiers;
			last_un_logged_identifiers = new ArrayList<>();
			return res;
		}

		void setAcceptedLocalGroups(Groups groups) {
			myAcceptedGroups = groups;
		}

		Groups getAcceptedLocalGroups() {
			return myAcceptedGroups;
		}

		void potentialChangesInGroups(DistantKernelAgent dka) throws MadkitException {
			if (dka.isAlive()) {
				AcceptedGroups ag = getAcceptedLocalGroups().potentialChangesInGroups();
				if (ag != null) {
					dka.sendData(this.global_address, ag, true, null, false);
				}
			}

		}

	}

	protected void newCGRSynchroDetected(CGRSynchro cgr) {
		if (cgr.getCode() == Code.REQUEST_ROLE || cgr.getCode() == Code.LEAVE_ROLE
				|| cgr.getCode() == Code.LEAVE_GROUP) {
			ListGroupsRoles ag = computeLocalAcceptedAndRequestedGroups();
			ListGroupsRoles mg = computeLocalGeneralAcceptedGroups();
			if (kernelAddressActivated && hasUsableDistantSocketAgent()
					&& ((cgr.getCode() == Code.REQUEST_ROLE
							&& this.localAcceptedAndRequestedGroups.areDetectedChanges(ag, getKernelAddress(), true))
							|| ((cgr.getCode() == Code.LEAVE_ROLE || cgr.getCode() == Code.LEAVE_GROUP)
									&& this.localAcceptedAndRequestedGroups.areDetectedChanges(ag, getKernelAddress(), true))))
				MadkitKernelAccess.informHooks(this, new NetworkGroupsAccessEvent(
						AgentActionEvent.ACCESSIBLE_LAN_GROUPS_GIVEN_TO_DISTANT_PEER, mg, ag, distant_kernel_address, false));
			localAcceptedAndRequestedGroups = ag;
			// localGeneralAcceptedGroups=mg;
		}
	}

	private ListGroupsRoles computeLocalGeneralAcceptedGroups() {
		ListGroupsRoles res = new ListGroupsRoles();
		computeLocalGeneralAcceptedGroups(agents_socket, res);
		computeLocalGeneralAcceptedGroups(indirect_agents_socket, res);
		return res;
	}

	private void computeLocalGeneralAcceptedGroups(ArrayList<AgentSocketData> agents_socket, ListGroupsRoles res) {
		for (AgentSocketData asd : agents_socket) {
			Groups g = asd.getAcceptedLocalGroups();
			if (g != null) {
				res.addListGroupsRoles(g.getGroups());
			}
		}
	}

	private ListGroupsRoles computeLocalAcceptedAndRequestedGroups() {
		ListGroupsRoles ag = new ListGroupsRoles();
		computeLocalAcceptedAndRequestedGroups(agents_socket, ag);
		computeLocalAcceptedAndRequestedGroups(indirect_agents_socket, ag);
		return ag;
	}

	private void computeLocalAcceptedAndRequestedGroups(ArrayList<AgentSocketData> agents_socket, ListGroupsRoles ag) {
		for (AgentSocketData asd : agents_socket) {
			Groups g = asd.getAcceptedLocalGroups();
			if (g != null) {
				ag.addListGroupsRoles(g.getGroups().getListWithRepresentedGroupsRoles(getKernelAddress()));
			}
		}
	}

	private ArrayList<Group> computeMissedGroups(Group[] reference, Group[] listToTest) {
		ArrayList<Group> res = new ArrayList<>();
		for (Group g : listToTest) {
			boolean found = false;
			for (Group g2 : reference) {
				if (g.equals(g2)) {
					found = true;
					break;
				}
			}
			if (!found) {
				res.add(g);
			}
		}
		return res;
	}
	private ArrayList<Group> computeMissedGroups(Collection<Group> reference, Collection<Group> listToTest) {
		ArrayList<Group> res = new ArrayList<>();
		for (Group g : listToTest) {
			boolean found = false;
			for (Group g2 : reference) {
				if (g.equals(g2)) {
					found = true;
					break;
				}
			}
			if (!found) {
				res.add(g);
			}
		}
		return res;
	}

	protected void updateSharedAcceptedGroups(boolean updateDistant, boolean updateLocal) throws NIOException {
		ListGroupsRoles old_distant_accepted_groups=null;
		ListGroupsRoles generalDistantAcceptedGroups=null;
		if (updateDistant) {
			generalDistantAcceptedGroups = new ListGroupsRoles();
			ListGroupsRoles groups = new ListGroupsRoles();

			updateDistantAcceptedGroups(agents_socket, generalDistantAcceptedGroups, groups);
			updateDistantAcceptedGroups(indirect_agents_socket, generalDistantAcceptedGroups, groups);

			old_distant_accepted_groups = distant_accepted_groups;
			distant_accepted_groups = generalDistantAcceptedGroups;


		}

		/*ListGroupsRoles generalLocalMultiGroup =null;
		ListGroupsRoles localAcceptedGroup=null;
		ArrayList<Group> localRemovedAcceptedGroups=null;
		if (kernelAddressActivated && hasUsableDistantSocketAgent()) {

			if (updateLocal)
			{
				localAcceptedGroup = computeLocalAcceptedAndRequestedGroups();
				ArrayList<Group> localNewAcceptedGroups = computeMissedGroups(this.localAcceptedAndRequestedGroups, localAcceptedGroup);
				localRemovedAcceptedGroups = computeMissedGroups(localAcceptedGroup, this.localAcceptedAndRequestedGroups);

				if (localNewAcceptedGroups.size() > 0 || localRemovedAcceptedGroups.size() > 0) {
					generalLocalMultiGroup = computeLocalGeneralAcceptedGroups();

				}
				localAcceptedAndRequestedGroups = localAcceptedGroup;
			}*/

		ListGroupsRoles localAcceptedGroup=null;
		Group[] localRepresentedAcceptedGroup;
		ArrayList<Group> localRemovedAcceptedGroups=null;

		if (kernelAddressActivated && hasUsableDistantSocketAgent()) {

			if (updateLocal)
			{
				localAcceptedGroup = computeLocalAcceptedAndRequestedGroups();
				localRepresentedAcceptedGroup=localAcceptedGroup.getRepresentedGroups(getKernelAddress());
				Group[] localRepresentedAcceptedAndRequestedGroups = localAcceptedAndRequestedGroups.getRepresentedGroups(getKernelAddress());
				ArrayList<Group> localNewAcceptedGroups = computeMissedGroups(localRepresentedAcceptedAndRequestedGroups, localRepresentedAcceptedGroup);
				localRemovedAcceptedGroups = computeMissedGroups(localRepresentedAcceptedGroup, localRepresentedAcceptedAndRequestedGroups);

				if (localNewAcceptedGroups.size() > 0 || localRemovedAcceptedGroups.size() > 0) {
					this.generalLocalMultiGroup = computeLocalGeneralAcceptedGroups();

				}
				this.localAcceptedAndRequestedGroups = localAcceptedGroup;

			}

			/*ListGroupsRoles ag=distant_accepted_groups.intersect(getKernelAddress(), this.localAcceptedAndRequestedGroups);



			ListGroupsRoles newAcceptedGroups = this.sharedAcceptedAndRequestedGroups.computeMissedGroups(getKernelAddress(),ag);
			ListGroupsRoles removedAcceptedGroups = ag.computeMissedGroups(getKernelAddress(), this.sharedAcceptedAndRequestedGroups);
			if (!newAcceptedGroups.isEmpty() || !removedAcceptedGroups.isEmpty()) {*/
			ArrayList<Group> ag = new ArrayList<>();
			for (Group g : localAcceptedAndRequestedGroups.getRepresentedGroups(getKernelAddress()))
			{
				if (distant_accepted_groups.includes(g))
					ag.add(g);
			}

			ArrayList<Group> newAcceptedGroups = computeMissedGroups(this.sharedAcceptedAndRequestedGroups, ag);
			ArrayList<Group> removedAcceptedGroups = computeMissedGroups(ag, this.sharedAcceptedAndRequestedGroups);
			if (newAcceptedGroups.size() > 0 || removedAcceptedGroups.size() > 0) {
				Map<String, Map<Group, Map<String, Collection<AgentAddress>>>> agent_addresses = getOrganizationSnapShot(
						newAcceptedGroups, distant_accepted_groups);

				if (!agent_addresses.isEmpty()) {

					CGRSynchrosSystemMessage message = new CGRSynchrosSystemMessage(agent_addresses, getKernelAddress(),
							removedAcceptedGroups);
					if (isAlive()) {
						AgentSocketData asd = getBestAgentSocket(false);
						if (asd != null) {

							MessageLocker locker = null;
							if (this.lockSocketUntilCGRSynchroIsSent) {
								locker = new MessageLocker();

								locker.lock();
							}
							sendData(asd.getAgentAddress(), message, true, locker, false);
							if (locker != null) {
								try {
									locker.waitUnlock(this, true);
								} catch (InterruptedException e) {
									if (logger!=null)
										logger.warning("Unexpected interrupted exception");
								}
							}
						}
					}
				}
			}
			this.sharedAcceptedAndRequestedGroups=ag;




			if (updateDistant && !((old_distant_accepted_groups == null || old_distant_accepted_groups.isEmpty()) && distant_accepted_groups.isEmpty()))
			{

				MadkitKernelAccess.informHooks(this,
						new NetworkGroupsAccessEvent(AgentActionEvent.ACCESSIBLE_LAN_GROUPS_GIVEN_BY_DISTANT_PEER, generalDistantAcceptedGroups,
								distant_accepted_groups, distant_kernel_address, false));

			}
			if (updateLocal && generalLocalMultiGroup!=null) {

				MadkitKernelAccess.informHooks(this, new NetworkGroupsAccessEvent(
						AgentActionEvent.ACCESSIBLE_LAN_GROUPS_GIVEN_TO_DISTANT_PEER, generalLocalMultiGroup, localAcceptedGroup, distant_kernel_address, localRemovedAcceptedGroups.size() > 0));

			}


		}
		if (updateDistant)
			if (logger != null && logger.isLoggable(Level.FINEST))
				logger.finest(
						"Distant accepted groups updated (distantInterfacedKernelAddress=" + distant_kernel_address + ")");

	}

	protected void sendData(AgentAddress receiver, SystemMessageWithoutInnerSizeControl _data, boolean isItAPriority,
							MessageLocker _messageLocker, boolean last_message) throws NIOException {
		int maxBufferSize=getMadkitConfig().networkProperties.maxBufferSize;
		try  {
			RandomByteArrayOutputStream baos = new RandomByteArrayOutputStream(new byte[maxBufferSize]);
			baos.setObjectResolver(filteredObjectResolver);
			baos.writeObject(_data, false);
			baos.flush();

			WritePacket packet = new WritePacket(PacketPartHead.TYPE_PACKET, getNewPacketID(),
					maxBufferSize,
					_data.excludedFromEncryption()?0:getMadkitConfig().networkProperties.maxRandomPacketValues, random,
					new LimitedRandomInputStream(baos.getRandomInputStream(), 0, baos.currentPosition()));
			if (logger != null && logger.isLoggable(Level.FINEST))
				logger.finest("Sending data (distantInterfacedKernelAddress=" + distant_kernel_address + ", packetID="
						+ packet.getID() + ") : " + _data);
			PacketData pd=new PacketData(this, receiver, _data, packet, _messageLocker, last_message, isItAPriority, _data.excludedFromEncryption());
			if (!ReturnCode.isSuccessOrIsTransferInProgress(sendMessage(receiver,
					new DistKernADataToUpgradeMessage(pd)))) {
				pd.unlockMessage();
				if (_messageLocker!=null)
					_messageLocker.forgive(false);
				if (logger!=null)
					logger.warning("Fail sending data (distantInterfacedKernelAddress=" + distant_kernel_address
						+ ", packetID=" + packet.getID() + ") : " + _data);

			}
		} catch (IOException | MadkitException e) {
			if (_messageLocker!=null) {
				_messageLocker.forgive(false);
			}
			throw new NIOException(e);
		}
	}

	public class FilteredObjectResolver extends SerializationTools.ObjectResolver {
		private final NetworkProperties np;
		public FilteredObjectResolver(NetworkProperties np) {
			this.np=np;
		}

		public Class<?> resolveClass(String clazz) throws MessageExternalizationException
		{
			try
			{
				if (np.isAcceptedClassForSerializationUsingPatterns(clazz))
				{
					Class<?> c=MadkitClassLoader.getLoader().loadClass(clazz, true);

					if (np.isAcceptedClassForSerializationUsingAllowClassList(c))
					{
						return c;
					}
				}
			}
			catch(Exception e)
			{
				throw new MessageExternalizationException(Integrity.FAIL_AND_CANDIDATE_TO_BAN, new ClassNotFoundException(clazz));
			}
			throw new MessageExternalizationException(Integrity.FAIL_AND_CANDIDATE_TO_BAN, new ClassNotFoundException(clazz));

		}

		@Override
		public SecureExternalizableWithoutInnerSizeControl replaceObject(SecureExternalizableWithoutInnerSizeControl obj)
			{
			if (obj instanceof KernelAddressInterfaced) {
				return ((KernelAddressInterfaced) obj).getOriginalKernelAddress();
			} else if (obj instanceof ConversationID) {
				/*if (obj.getClass() == BigDataTransferID.class) {
					return MadkitKernelAccess.getBigDataTransferIDInstance(
							MadkitKernelAccess.getInterfacedConversationIDToDistantPeer((ConversationID) obj,
									DistantKernelAgent.this, getKernelAddress(),
									DistantKernelAgent.this.distant_kernel_address),
							((BigDataTransferID) obj).getBytePerSecondsStat());
				} else */if (obj.getClass() == TaskID.class) {
					return MadkitKernelAccess.getTaskIDInstance(MadkitKernelAccess
							.getInterfacedConversationIDToDistantPeer((ConversationID) obj, DistantKernelAgent.this,
									getKernelAddress(), DistantKernelAgent.this.distant_kernel_address));
				} else
					return MadkitKernelAccess.getInterfacedConversationIDToDistantPeer((ConversationID) obj,
							DistantKernelAgent.this, getKernelAddress(),
							DistantKernelAgent.this.distant_kernel_address);
			} else
				return obj;
		}

		@Override
		public SecureExternalizableWithoutInnerSizeControl resolveObject(SecureExternalizableWithoutInnerSizeControl obj) {
			if (obj==null)
				return null;
			if (obj.getClass() == KernelAddress.class) {
				KernelAddress ka=(KernelAddress) obj;
				if (distant_kernel_address!=null && ka.equals(distant_kernel_address.getOriginalKernelAddress()))
					return distant_kernel_address;
				else
					return ka;

			} else if (obj instanceof ConversationID) {
				if (obj.getClass() == TaskID.class) {
					return MadkitKernelAccess.getTaskIDInstance(MadkitKernelAccess
							.getInterfacedConversationIDFromDistantPeer((ConversationID) obj, DistantKernelAgent.this,
									getKernelAddress(), DistantKernelAgent.this.distant_kernel_address));
				} else
					return MadkitKernelAccess.getInterfacedConversationIDFromDistantPeer((ConversationID) obj,
							DistantKernelAgent.this, getKernelAddress(),
							DistantKernelAgent.this.distant_kernel_address);
			} else
				return obj;
		}
	}



	protected int getNewPacketID() {
		synchronized (packet_id_generator) {
			int res=packet_id_generator.getNewID();
			if (res==-1)
				res=packet_id_generator.getNewID();
			return res;
		}
	}


	protected static abstract class AbstractPacketDataFinalizer extends AbstractData.Finalizer
	{
		protected boolean unlocked = false;
		protected ByteBuffer currentByteBuffer;
		protected RealTimeTransferStat stat;
		protected final WritePacket packet;
		protected final DistantKernelAgent agent;

		protected AbstractPacketDataFinalizer(WritePacket packet, DistantKernelAgent agent) {
			if (packet == null)
				throw new NullPointerException("_packet");
			if (agent == null)
				throw new NullPointerException("distant_kernel_address");
			this.packet=packet;
			this.agent=agent;
		}

		@Override
		void unlockMessage(boolean cancel) throws MadkitException{
			synchronized (this) {
				unlocked = true;
			}
			removePendingBigDataPacket();
		}

		@Override
		boolean isUnlocked() {
			synchronized (this) {
				return unlocked;
			}
		}
		void removePendingBigDataPacket() {

		}
		protected void finishLastStat() {
			synchronized (this) {
				if (currentByteBuffer != null && stat != null) {
					stat.newBytesIdentified(currentByteBuffer.capacity() - currentByteBuffer.remaining());
				}
			}
		}
	}
	@SuppressWarnings("SynchronizeOnNonFinalField")
	static abstract class AbstractPacketData<F extends AbstractPacketDataFinalizer> extends AbstractData<F> {


		protected Block currentBlock;
		private boolean asking_new_buffer_in_process;


		private IDTransfer idTransfer = null;

		protected final AgentAddress firstAgentSocketSender;
		private final AgentAddress agentReceiver;

		protected final boolean excludedFromEncryption;
		AbstractAgentSocket agentSocket;


		protected AbstractPacketData(boolean priority, F finalizer, AgentAddress firstAgentSocketSender,
									 AgentAddress agentReceiver, boolean excludedFromEncryption) {
			super(priority, finalizer);
			if (firstAgentSocketSender == null && !finalizer.packet.concernsBigData())
				throw new NullPointerException("firstAgentSocketSender");
			this.firstAgentSocketSender = firstAgentSocketSender;
			finalizer.currentByteBuffer = null;
			currentBlock = null;
			asking_new_buffer_in_process = false;
			finalizer.stat = null;
			this.agentReceiver = agentReceiver;
			this.excludedFromEncryption=excludedFromEncryption;
			//this.counterSelector=counterSelector;
		}

		@Override
		public String toString() {
			synchronized (finalizer) {
				return getClass().getSimpleName() + "[idPacket=" + finalizer.packet.getID() + ", unlocked=" + finalizer.unlocked
						+ ", canceled=" + isCanceled + ", totalDataToSendLength(Without connection protocol)="
						+ this.finalizer.packet.getDataLengthWithHashIncluded() + ", currentByteBuffer="
						+ (finalizer.currentByteBuffer == null ? null : finalizer.currentByteBuffer.capacity())
						+ ", currentByteBufferRemaining="
						+ (finalizer.currentByteBuffer == null ? null : finalizer.currentByteBuffer.remaining()) + ", dataSent="
						+ this.finalizer.packet.getReadDataLengthIncludingHash() + "]";
			}
		}

		@Override
		void closeStream() throws IOException {
			if (!finalizer.packet.getInputStream().isClosed())
				finalizer.packet.getInputStream().close();
		}

		@Override
		void cancel() throws IOException, MadkitException {
			AbstractAgentSocket as=agentSocket;
			super.cancel();
			unlockMessage(true);
			if (as!=null) {
				//noinspection SynchronizationOnLocalVariableOrMethodParameter
				synchronized (as) {
					as.notifyAll();
				}
			}
			//closeStream();

		}
		
		@Override
		Object getLocker()
		{
			return agentSocket;
		}

		@Override
		public void reset() {
			synchronized (finalizer) {
				if (finalizer.currentByteBuffer != null) {
					finalizer.currentByteBuffer.rewind();
					asking_new_buffer_in_process = false;
				}
			}
		}

		@Override
		boolean isCanceledNow()
		{
			if (isCanceled) {
				synchronized (this) {
					return finalizer.currentByteBuffer == null || finalizer.currentByteBuffer.remaining() == 0 || finalizer.currentByteBuffer.position() == 0;
				}
			}
			else
				return false;
		}
		@Override
		public ByteBuffer getByteBuffer() throws PacketException {
			
			synchronized (finalizer) {
				if (isCanceled)
				{	
					try
					{
						return finalizer.currentByteBuffer;
					}
					finally
					{
						finalizer.currentByteBuffer=null;
					}
				}
				if (finalizer.currentByteBuffer == null || finalizer.currentByteBuffer.remaining()==0) {
					finalizer.currentByteBuffer=null;
					if (!asking_new_buffer_in_process) {
						if (!finalizer.packet.isFinished())
						{
							updateNextByteBufferUnsafe();
						}
					}
					
					return null;
				}
				try
				{
					if (finalizer.stat != null)
						finalizer.stat.newBytesIdentified(finalizer.currentByteBuffer.capacity());

					return finalizer.currentByteBuffer;
				}
				finally
				{
					finalizer.currentByteBuffer=null;
				}
				
			}
		}


		protected void setNewBlock(IDTransfer id, Block _block) throws NIOException {
			NIOException e=null;
			synchronized (finalizer) {
				idTransfer = id;
				if (finalizer.currentByteBuffer == null) {
					finalizer.currentByteBuffer = ByteBuffer.wrap((currentBlock=_block).getBytes(), 0, _block.getBlockSize());
					asking_new_buffer_in_process = false;
				}  else
					e=new NIOException("Unexpected exception !");
			}
			synchronized(agentSocket)
			{
				agentSocket.notifyAll();
			}
			if (e!=null)
				throw e;
			
		}

		@Override
		boolean isDataBuildInProgress()
		{
			synchronized(finalizer)
			{
				return asking_new_buffer_in_process;
			}
		}
		
		@Override
		public boolean isFinished() {
			boolean res;
			boolean canceled=isCanceled;
			if (canceled && isCurrentByteBufferFinished()) {
				res=true;
			}
			else {
				synchronized (finalizer) {
					if (asking_new_buffer_in_process) {
						res = canceled;
					} else if (finalizer.currentByteBuffer == null || finalizer.currentByteBuffer.remaining() == 0) {
						res = canceled || this.finalizer.packet.isFinished();
					} else {
						res = false;
					}
				}
			}

			return res;
		}


		
		

		@Override
		public boolean isCurrentByteBufferFinished() {
			synchronized (finalizer) {
				return finalizer.currentByteBuffer == null || finalizer.currentByteBuffer.remaining() == 0;
			}
		}

		@Override
		public boolean isCurrentByteBufferFinishedOrNotStarted() {
			synchronized (finalizer) {
				return finalizer.currentByteBuffer == null || finalizer.currentByteBuffer.remaining() == 0 || finalizer.currentByteBuffer.position()==0;
			}
		}

		public int getIDPacket() {
			return finalizer.packet.getID();
		}

		private void updateNextByteBufferUnsafe() throws PacketException  {
			try
			{
				asking_new_buffer_in_process = true;
				if ((agentSocket == null || !agentSocket.isAlive())
						&& !isUnlocked())
				{
					/*if (logger!=null)
						logger.warning("Impossible to send message to "+this.getAgentSocketSender());*/
					cancel();
					return;
				}
			}
			catch(MadkitException | IOException e)
			{
				throw new PacketException(e);
			}
			agentSocket.receiveMessage(new DistKernADataToUpgradeMessage(this));
		}

		public IDTransfer getIDTransfer() {
			return idTransfer;
		}

		void setStat(RealTimeTransferStat stat) {
			synchronized (finalizer) {
				this.finalizer.stat = stat;
			}
		}





		AgentAddress getFirstAgentSocketSender() {
			return firstAgentSocketSender;
		}

		AgentAddress getAgentSocketSender() {
			return getFirstAgentSocketSender();

		}

		public long getReadDataLength() {
			return finalizer.packet.getReadDataLength();
		}

		public long getReadDataLengthIncludingHash() {
			return finalizer.packet.getReadDataLengthIncludingHash();
		}

		AgentAddress getReceiver() {
			return this.agentReceiver;
		}
	}
	protected static class PacketDataFinalizer extends AbstractPacketDataFinalizer
	{
		protected final MessageLocker messageLocker;
		protected final LanMessage original_lan_message;


		public PacketDataFinalizer(WritePacket packet, DistantKernelAgent agent, MessageLocker messageLocker, LanMessage original_lan_message) {
			super(packet, agent);
			this.messageLocker = messageLocker;
			this.original_lan_message=original_lan_message;
		}

		@Override
		protected void performCleanup() {
			removePacketID(packet.getID());
			super.performCleanup();
		}
		protected void removePacketID(int id) {
			synchronized (agent.packet_id_generator) {
				agent.packet_id_generator.removeID(id);
			}
		}

		@Override
		public void unlockMessage(boolean cancel) throws MadkitException {
			synchronized (this) {
				try {
					if (messageLocker != null && !isUnlocked()) {
						finishLastStat();
						long sendLength = packet.getReadDataLengthIncludingHash();
						if (currentByteBuffer != null)
							sendLength -= currentByteBuffer.remaining();

						messageLocker.unlock(agent.distant_kernel_address, new DataTransferResult(
								packet.getInputStream().length(), packet.getReadDataLength(), sendLength, this.original_lan_message instanceof BroadcastLanMessage), cancel);

					}

				} catch (IOException e) {
					throw new MadkitException(e);
				}
				finally {
					super.unlockMessage(cancel);
				}
			}
		}
	}
	static class PacketData extends AbstractPacketData<PacketDataFinalizer> {

		private final boolean last_message;





		protected PacketData(DistantKernelAgent agent, AgentAddress first_receiver, SystemMessageWithoutInnerSizeControl lan_message, WritePacket _packet,
							 MessageLocker _messageLocker, boolean _last_message, boolean isItAPriority, boolean excludedFromEncryption) {
			super(isItAPriority, new PacketDataFinalizer(_packet, agent, _messageLocker, (!(lan_message instanceof LanMessage)) ? null : (LanMessage) lan_message),
					first_receiver,
					(lan_message instanceof LanMessage) ? ((LanMessage) lan_message).message.getReceiver() : null, excludedFromEncryption);
			if (_packet.concernsBigData())
				throw new IllegalArgumentException("_packet cannot use big data !");

			last_message = _last_message;
		}

		@Override
		public DataTransferType getDataTransferType() {
			return DataTransferType.SHORT_DATA;
		}

		@Override
		public boolean isLastMessage() {
			return last_message;
		}




	}
	static class BigPacketDataFinalizer extends AbstractPacketDataFinalizer
	{
		private boolean pendingDataRemoved=false;

		protected BigPacketDataFinalizer(WritePacket packet, DistantKernelAgent agent) {
			super(packet, agent);
		}

		@Override
		protected void removePendingBigDataPacket()
		{
			synchronized (this) {
				if (!pendingDataRemoved) {
					pendingDataRemoved = true;
					agent.synchronizeActionWithLiveByCycleFunction(() -> agent.packetsDataInQueue.remove(packet.getID()));
				}
			}
		}
	}
	static class BigPacketData extends AbstractPacketData<BigPacketDataFinalizer> {
		private final AgentAddress caller;
		private final ConversationID conversationID;
		private final long nanoTime;
		private final AbstractDecentralizedIDGenerator differedBigDataInternalIdentifier;
		private final ExternalAsynchronousBigDataIdentifier externalAsynchronousBigDataIdentifier;


		protected BigPacketData(DistantKernelAgent agent, AgentAddress _firstAgentSocketSender, WritePacket _packet, AgentAddress _agentReceiver,
								AgentAddress caller, ConversationID conversationID, RealTimeTransferStat stat, boolean excludedFromEncryption,
								AbstractDecentralizedIDGenerator differedBigDataInternalIdentifier,
								ExternalAsynchronousBigDataIdentifier externalAsynchronousBigDataIdentifier) {
			super(false, new BigPacketDataFinalizer(_packet, agent), _firstAgentSocketSender, _agentReceiver, excludedFromEncryption);
			if (!_packet.concernsBigData())
				throw new IllegalArgumentException("_packet has to use big data !");
			if (caller == null)
				throw new NullPointerException("caller");
			if (conversationID == null)
				throw new NullPointerException("conversationID");
			if ((differedBigDataInternalIdentifier==null)!=(externalAsynchronousBigDataIdentifier ==null))
				throw new NullPointerException();
			this.differedBigDataInternalIdentifier=differedBigDataInternalIdentifier;
			this.externalAsynchronousBigDataIdentifier = externalAsynchronousBigDataIdentifier;
			this.caller = caller;
			this.conversationID = conversationID;
			setStat(stat);
			nanoTime = System.nanoTime();
		}



		@Override
		public DataTransferType getDataTransferType() {
			return DataTransferType.BIG_DATA;
		}

		AgentAddress getCaller() {
			return caller;
		}

		ConversationID getConversationID() {
			return conversationID;
		}

		long getDurationInNano() {
			return System.nanoTime() - nanoTime;
		}

		public AbstractDecentralizedIDGenerator getDifferedBigDataInternalIdentifier() {
			return differedBigDataInternalIdentifier;
		}

		public ExternalAsynchronousBigDataIdentifier getDifferedBigDataIdentifier() {
			return externalAsynchronousBigDataIdentifier;
		}
	}

	static class DistKernADataToUpgradeMessage extends NIOMessage {

		final AbstractPacketData<?> dataToUpgrade;

		DistKernADataToUpgradeMessage(AbstractPacketData<?> _data) {
			dataToUpgrade = _data;
		}
	}

	static class SendDataFromAgentSocket extends ObjectMessage<SystemMessageWithoutInnerSizeControl> {

		final boolean last_message;
		final boolean isItAPriority;

		public SendDataFromAgentSocket(SystemMessageWithoutInnerSizeControl _content, boolean last_message, boolean isItAPriority) {
			super(_content);
			this.last_message = last_message;
			this.isItAPriority = isItAPriority;

		}

		@Override
		public String toString() {
			return "SendDataFromAgentSocket[last_message=" + last_message + ", isItAPriority=" + isItAPriority
					+ ", systemMessage=" + getContent() + "]";
		}

	}



	private static class Reading {
		protected final MessageDigestType messageDigestType;
		protected ReadPacket read_packet;
		protected final RandomOutputStream output_stream;

		protected Reading(MessageDigestType messageDigestType, RandomOutputStream os) {
			if (os == null)
				throw new NullPointerException("os");
			this.messageDigestType = messageDigestType;
			read_packet = null;
			output_stream = os;

		}

		protected Reading(MessageDigestType messageDigestType, PacketPart _first_part, RandomOutputStream os)
				throws PacketException, IOException {
			if (os == null)
				throw new NullPointerException("os");
			this.messageDigestType = messageDigestType;
			output_stream = os;
			read_packet = new ReadPacket(
					_first_part, output_stream,
					messageDigestType);
		}

		public void closeStream() throws IOException {
			if (!output_stream.isClosed())
				output_stream.close();
		}
		public void freeDataSize() {

		}
		public void cancel() throws IOException {
			closeStream();
			freeDataSize();
		}

		public int getIDPacket() {
			return read_packet.getID();
		}

		public boolean isFinished() {
			return read_packet.isFinished();
		}

		public void readNewPart(PacketPart _part) throws PacketException, IOException {
			if (read_packet == null)
				read_packet = new ReadPacket(_part, output_stream,
						messageDigestType);
			read_packet.readNewPart(_part);
		}

		public boolean isValid() {
			return read_packet == null || read_packet.isValid();
		}

		public boolean isInvalid() {
			return read_packet == null || read_packet.isInvalid();
		}

		public boolean isTemporaryInvalid() {
			return read_packet == null || read_packet.isTemporaryInvalid();
		}

		ReadPacket getReadPacket() {
			return read_packet;
		}

	}

	private class SerializedReading extends Reading {
		private long dataSize;
		private final AgentAddress initialSocketAgent;

		public SerializedReading(AgentAddress initialSocketAgent, PacketPart _part) throws PacketException, IOException {
			super(null, _part, new RandomByteArrayOutputStream((int)_part.getHead().getTotalLength()));
			this.initialSocketAgent = initialSocketAgent;
			dataSize = _part.getSubBlock().getSize();
			incrementTotalDataQueue(dataSize);
		}

		public RandomInputStream getRandomInputStream() throws IOException {
			return output_stream.getRandomInputStream();
		}

		@Override
		public void readNewPart(PacketPart _part) throws PacketException, IOException {
			dataSize += _part.getSubBlock().getSize();
			incrementTotalDataQueue(_part.getSubBlock().getSize());
			super.readNewPart(_part);
		}

		public long getDataSize() {
			return dataSize;
		}

		@Override
		public void freeDataSize() {
			decrementTotalDataQueue(dataSize);
			dataSize = 0;
		}

		AgentAddress getInitialAgentAddress() {
			return initialSocketAgent;
		}
	}

	private class BigDataReading extends Reading {
		private final int IDPacket;
		private final BigDataPropositionMessage originalMessage;
		private final RealTimeTransferStat stat;
		private int data = 0;

		public BigDataReading(BigDataPropositionMessage m) {
			super(m.getMessageDigestType(), MadkitKernelAccess.getOutputStream(m));
			IDPacket = MadkitKernelAccess.getIDPacket(m);
			this.originalMessage = m;
			stat = m.getStatistics();
		}

		@Override
		public int getIDPacket() {
			return IDPacket;
		}

		@Override
		public void readNewPart(PacketPart _part) throws PacketException, IOException {
			data += _part.getSubBlock().getSize();
			incrementTotalDataQueue(_part.getSubBlock().getSize());
			if (read_packet == null)
				read_packet = new ReadPacket(_part, output_stream,
						messageDigestType);
			else
				read_packet.readNewPart(_part);

		}

		public long getNumberOfReceivedBytes()
		{
			if (read_packet==null)
				return 0;
			else
				return read_packet.getWrittenDataLength();
		}
		@Override
		public void freeDataSize() {
			decrementTotalDataQueue(data);
			data = 0;
		}

		BigDataPropositionMessage getOriginalMessage() {
			return originalMessage;
		}

		RealTimeTransferStat getStatistics() {
			return stat;
		}

	}

	void removeBigDataReading(int IDPacket)
	{
		BigDataReading bdr=current_big_data_readings.remove(IDPacket);
		if (bdr!=null)
			bdr.freeDataSize();

	}
	void removeShortDataReading(int IDPacket)
	{
		current_short_readings.remove(IDPacket);
	}


	protected void receiveData(AgentAddress agent_socket_sender, PacketPart p) {

		boolean bigData = false;

		Reading reading = current_short_readings.get(p.getHead().getID());
		if (reading == null) {
			reading = current_big_data_readings.get(p.getHead().getID());

			if (reading != null)
				bigData = true;
		}
		if (bigData) {
			if (logger != null && logger.isLoggable(Level.FINEST))
				logger.finest("Receiving block data for big data transfer from " + agent_socket_sender
						+ " (distantInterfacedKernelAddress=" + distant_kernel_address + ") : " + p);

			BigDataReading sr = (BigDataReading) reading;

			if (sr.isValid() || (sr.isTemporaryInvalid() && p.getHead().isRedownloadedPacketPart())) {
				try {
					sr.readNewPart(p);
					sr.getStatistics().newBytesIdentified(p.getSubBlock().getSize());
					if (sr.isInvalid()) {
						MadkitKernelAccess.dataCorrupted(sr.getOriginalMessage(),
								sr.getReadPacket().getWrittenDataLength(), null);
						removeBigDataReading(reading.getIDPacket());
						processInvalidPacketPart(agent_socket_sender,
								new PacketException("The given packet is not valid."), p, false);
						try {
							sr.closeStream();
						} catch (Exception ignored) {

						}
					} else if (sr.isFinished()) {
						sr.closeStream();
						MadkitKernelAccess.transferCompleted(sr.getOriginalMessage(),
								sr.getReadPacket().getWrittenDataLength());
						removeBigDataReading(reading.getIDPacket());
					}
				}
				catch (PacketException | IOException e) {
					boolean candidateToBan=(e instanceof MessageExternalizationException) && ((MessageExternalizationException) e).getIntegrity().equals(Integrity.FAIL_AND_CANDIDATE_TO_BAN);
					MadkitKernelAccess.dataCorrupted(sr.getOriginalMessage(), sr.getReadPacket()==null?0:sr.getReadPacket().getWrittenDataLength(), null);
					removeBigDataReading(reading.getIDPacket());
					processInvalidPacketPart(agent_socket_sender, e, p, candidateToBan);
					try {
						sr.closeStream();
					} catch (Exception ignored) {

					}

				}
				sr.freeDataSize();
			} else if (sr.isInvalid()) {
				MadkitKernelAccess.dataCorrupted(sr.getOriginalMessage(), sr.getReadPacket().getWrittenDataLength(), null);
				removeBigDataReading(reading.getIDPacket());

				processInvalidPacketPart(agent_socket_sender, new PacketException("The given packet is not valid."), p,
						false);
				try {
					sr.closeStream();
				} catch (Exception ignored) {

				}
			}
		} else {
			if (p.getHead().getTotalLength() > getMadkitConfig().networkProperties.maxShortDataSize)
				processInvalidPacketPart(agent_socket_sender,
						new PacketException(
								"The given packet have not be treated as a big data, and has a too big size : "
										+ p.getHead().getTotalLength()),
						p, false);
			else {
				SerializedReading sr = null;

				if (reading == null) {
					// new short data received
					try {
						if (!p.getHead().isFirstPacketPart())
						{
							processInvalidPacketPart(agent_socket_sender,
									new PacketException("Receiving packet "+p.getHead().getID()+" that cannot be rooted"), p, false);
						}
						else {
							if (logger != null && logger.isLoggable(Level.FINEST))
								logger.finest("Receiving block data for new short data transfer from " + agent_socket_sender
										+ " (distantInterfacedKernelAddress=" + distant_kernel_address + ") : " + p);

							reading = sr = new SerializedReading(agent_socket_sender, p);

							if (sr.isInvalid()) {
								processInvalidPacketPart(agent_socket_sender,
										new PacketException("The given packet is not valid."), p, false);
								try {
									sr.closeStream();
								} catch (Exception ignored) {

								}
							}
							// check too simultaneous short data sent
							boolean tooMuch = current_short_readings.size() >= this.agents_socket.size()
									+ this.indirect_agents_socket.size();
							if (!tooMuch) {
								for (SerializedReading sr2 : current_short_readings.values()) {
									if (sr2.getInitialAgentAddress().equals(agent_socket_sender)) {
										tooMuch = true;
										break;
									}
								}
							}
							if (tooMuch) {
								processTooMuchSimultaneousShortDataSent();
							} else {
								current_short_readings.put(p.getHead().getID(), sr);

							}
						}
					} catch (PacketException | IOException e) {
						boolean candidateToBan=(e instanceof MessageExternalizationException) && ((MessageExternalizationException) e).getIntegrity().equals(Integrity.FAIL_AND_CANDIDATE_TO_BAN);
						processInvalidPacketPart(agent_socket_sender, e, p, candidateToBan);
					}
				} else {
					sr = ((SerializedReading) reading);
					if (reading.isValid() || (reading.isTemporaryInvalid() && p.getHead().isRedownloadedPacketPart())) {
						try {
							if (logger != null && logger.isLoggable(Level.FINEST))
								logger.finest("Receiving block data and updating short data transfer from "
										+ agent_socket_sender + " (distantInterfacedKernelAddress="
										+ distant_kernel_address + ") : " + p);

							reading.readNewPart(p);
							if (sr.isInvalid()) {
								processInvalidPacketPart(agent_socket_sender,
										new PacketException("The given packet is not valid."), p, false);
								try {
									sr.closeStream();
								} catch (Exception ignored) {

								}
							}
						} catch (PacketException | IOException e) {
							boolean candidateToBan=(e instanceof MessageExternalizationException) && ((MessageExternalizationException) e).getIntegrity().equals(Integrity.FAIL_AND_CANDIDATE_TO_BAN);
							processInvalidPacketPart(agent_socket_sender, e, p, candidateToBan);
							sr.freeDataSize();
						}
					}
				}
				if (reading != null && sr.isFinished()) {
					if (sr.isInvalid()) {
						sr.freeDataSize();
					} else {
						try {

							try (RandomInputStream bais = sr.getRandomInputStream()) {
								bais.setObjectResolver(filteredObjectResolver);
								Object obj = bais.readObject(false);

								receiveData(agent_socket_sender, obj, sr.getDataSize());
							}
							catch(MessageExternalizationException e)
							{
								sr.freeDataSize();
								processInvalidSerializedObject(agent_socket_sender, e, null, e.getIntegrity().equals(Integrity.FAIL_AND_CANDIDATE_TO_BAN));
							}
							catch (IOException | ClassNotFoundException | NIOException e) {
								sr.freeDataSize();
								processInvalidSerializedData(agent_socket_sender, e, sr.read_packet);
							}
							finally {
								sr.closeStream();
							}

						} catch (IOException e) {

							sr.freeDataSize();
							sendMessageWithRole(agent_socket_sender,
									new AskForConnectionMessage(ConnectionClosedReason.CONNECTION_ANOMALY, null, null,
											null, false, false),
									LocalCommunity.Roles.DISTANT_KERNEL_AGENT_ROLE);
						}
					}
					int id = reading.getIDPacket();
					removeShortDataReading(id);
					setOneSocketPurged(id, sr);

				}

			}
		}
	}

	public void receiveData(AgentAddress agent_socket_sender, Object obj, long dataSize) throws NIOException {
		if (obj instanceof SystemMessageWithoutInnerSizeControl) {
			SystemMessageWithoutInnerSizeControl sm = ((SystemMessageWithoutInnerSizeControl) obj);
			if (logger != null && logger.isLoggable(Level.FINEST))
				logger.finest("Receiving system message from " + agent_socket_sender
						+ " (distantInterfacedKernelAddress=" + distant_kernel_address + ") : " + sm);
			if (obj.getClass()==CancelBigDataSystemMessage.class)
			{
				CancelBigDataSystemMessage m=(CancelBigDataSystemMessage)obj;
				if (m.isFromSender()) {
					BigDataReading bdr = cancelBigPacketDataToReceiveInQueue(m.getIDPacket());
					if (bdr!=null) {
						MadkitKernelAccess.transferLostForBigDataTransfer(this, bdr.getOriginalMessage().getConversationID(),
								bdr.getIDPacket(), bdr.getOriginalMessage().getSender(), bdr.getOriginalMessage().getReceiver(),
								bdr.getNumberOfReceivedBytes(), bdr.getStatistics().getDurationMilli(),
								bdr.getOriginalMessage().getAsynchronousBigDataInternalIdentifier(), bdr.getOriginalMessage().getExternalAsynchronousBigDataIdentifier(), m.getReason(), true);
					}
				}
				else
				{
					cancelBigPacketDataToSendInQueue(m.getIDPacket(), true, m.getReason(), true);
				}
			}
			else if (obj.getClass()==PauseBigDataTransferSystemMessage.class)
			{
				sendMessage(LocalCommunity.Groups.NETWORK, LocalCommunity.Roles.NIO_ROLE, new PauseBigDataTransferMessage(distant_kernel_address, ((PauseBigDataTransferSystemMessage) obj).isTransferPaused()));
			}
			else
				sendMessageWithRole(agent_socket_sender, new ReceivedSerializableObject(sm, dataSize),
					LocalCommunity.Roles.DISTANT_KERNEL_AGENT_ROLE);
		} else
			processInvalidSerializedObject(agent_socket_sender, null, obj, true);
	}


	final class ReceivedSerializableObject extends ObjectMessage<SystemMessageWithoutInnerSizeControl> {


		private final long dataIncrement;
		private final AtomicLong dataSize = new AtomicLong(0);
		private volatile boolean markedAsRead=false;

		public ReceivedSerializableObject(SystemMessageWithoutInnerSizeControl _content, long dataSize) {
			super(_content);
			this.dataSize.set(dataSize);
			this.dataIncrement = dataSize;
		}

		void markDataAsRead() {
			if (!markedAsRead) {
				markedAsRead=true;
				if (dataSize.addAndGet(-dataIncrement) >= 0) {
					decrementTotalDataQueue(dataIncrement);
				} else {
					dataSize.set(0);
					if (logger != null && logger.isLoggable(Level.FINER))
						logger.finer("Illegal computed data size queue !");
				}
			}
		}

		@SuppressWarnings("MethodDoesntCallSuperMethod")
        @Override
		public ReceivedSerializableObject clone() {
			incrementTotalDataQueue(dataIncrement);
			dataSize.addAndGet(dataIncrement);
			return this;
		}

	}

	private void setTransferPaused(boolean value) {
		setTransferPaused(value, null, false);
	}

	private void setTransferPaused(boolean value, Set<AgentAddress> agents, boolean force) {

		if (!force) {
			ExceededDataQueueSize e = this.globalExceededDataQueueSize.get();
			if (e != null) {
				synchronized (networkBoard.candidatesForPurge) {
					if (e.isPaused()) {
                        value = !e.mustPurge();
					}
				}
			}
		}
		/*
		 * if (e!=null && e.isPaused() && !e.mustPurge()) value=true; if (e!=null &&
		 * value && e.isPurging(this)) { value=false; }
		 */

		if (this.transferPaused.compareAndSet(!value, value) || force) {
			if (logger != null && logger.isLoggable(Level.FINEST))
				logger.finest("Set transfer paused (distantInterfacedKernelAddress=" + distant_kernel_address
						+ ", pause=" + value + ", force=" + force + ", agents=" + agents + ")");

			if (getState().compareTo(State.ENDING) >= 0)
				return;

			if (agents == null) {
				broadcastMessageWithRole(LocalCommunity.Groups.getDistantKernelAgentGroup(getNetworkID()),
						LocalCommunity.Roles.SOCKET_AGENT_ROLE,
						new ExceededDataQueueSize(networkBoard, false, value),
						LocalCommunity.Roles.DISTANT_KERNEL_AGENT_ROLE);
			} else {
				for (AgentAddress aa : agents)
					sendMessageWithRole(aa, new ExceededDataQueueSize(networkBoard, false, value),
							LocalCommunity.Roles.DISTANT_KERNEL_AGENT_ROLE);
			}
		}
	}

	private long getMaxSizeForUnreadShortDataFromOneDistantKernel() {
		return Math.max(getMadkitConfig().networkProperties.maxSizeForUnreadShortDataFromOneDistantKernel,
				((long) getMadkitConfig().networkProperties.numberOfMaximumConnectionsBetweenTwoSameKernelsAndMachines)
						* getMadkitConfig().networkProperties.maxShortDataSize);
	}

	private long getMaxSizeForUnreadShortDataFromAllDistantKernel() {
		return getMadkitConfig().networkProperties.maxSizeForUnreadShortDataFromAllConnections;
	}

	void incrementTotalDataQueue(long value) {
		if (value < 0)
			throw new IllegalAccessError();
		if (totalDataInQueue.addAndGet(value) > getMaxSizeForUnreadShortDataFromOneDistantKernel())
			setTransferPaused(true);
		incrementTotalDataQueueForAllDistantKernelAgent(value);

	}

	private void purgeTotalDataQueue() {
		decrementTotalDataQueueAllDistantKernelAgent(totalDataInQueue.getAndSet(0));
		new ExceededDataQueueSize(networkBoard, false, false).purgeCanceled(this);
	}

	void decrementTotalDataQueue(long value) {
		if (value==0)
			return;
		if (value < 0)
			throw new IllegalAccessError();
		long val = totalDataInQueue.addAndGet(-value);
		if (val < 0) {
			totalDataInQueue.addAndGet(-val);
			Logger logger=getLogger();
			if (logger!=null)
				logger.severe("DistantKernelAgent.totalDataInQueue cannot be negative");
		}
		if (val < getMaxSizeForUnreadShortDataFromOneDistantKernel())
			setTransferPaused(false);

		decrementTotalDataQueueAllDistantKernelAgent(value);
	}

	private void setOneSocketPurged(Integer id, SerializedReading sr) {
		ExceededDataQueueSize e = this.globalExceededDataQueueSize.get();
		if (e != null) {
			e.setOneSocketPurged(this, id, sr);
		}
	}

	void setGlobalTransfersPaused(boolean value) {
		synchronized (networkBoard.candidatesForPurge) {
			if (value && !hasToPauseGlobalTransfers())
				return;
			if (!value && hasToPauseGlobalTransfers())
				return;

			if (networkBoard.transferPausedForAllDistantKernelAgent.compareAndSet(!value, value)) {
				if (logger != null && logger.isLoggable(Level.FINEST))
					logger.finest("Set global transfer paused (distantInterfacedKernelAddress=" + distant_kernel_address
							+ ", pause=" + value + ")");

				if (value) {
					networkBoard.currentCandidateForPurge = this;
					// set paused
					ReturnCode rc = broadcastMessageWithRole(LocalCommunity.Groups.DISTANT_KERNEL_AGENTS_GROUPS,
							LocalCommunity.Roles.DISTANT_KERNEL_AGENT_ROLE,
							new ExceededDataQueueSize(networkBoard, true, false),
							LocalCommunity.Roles.DISTANT_KERNEL_AGENT_ROLE);
					Message m = new ExceededDataQueueSize(networkBoard, ReturnCode.isSuccessOrIsTransferInProgress(rc), true, true);
					this.receiveMessage(m);
				} else {
					// unset paused
					Message m = new ExceededDataQueueSize(networkBoard, false, false);
					broadcastMessageWithRole(LocalCommunity.Groups.DISTANT_KERNEL_AGENTS_GROUPS,
							LocalCommunity.Roles.DISTANT_KERNEL_AGENT_ROLE, m,
							LocalCommunity.Roles.DISTANT_KERNEL_AGENT_ROLE);
					this.receiveMessage(m);
					networkBoard.candidatesForPurge.clear();
					networkBoard.currentCandidateForPurge = null;
				}
			}
		}
	}

	private void incrementTotalDataQueueForAllDistantKernelAgent(long value) {
		if (value < 0)
			throw new IllegalAccessError();
		if (networkBoard.totalDataInQueueForAllDistantKernelAgent
				.addAndGet(value) > getMaxSizeForUnreadShortDataFromAllDistantKernel()) {
			setGlobalTransfersPaused(true);
		}
	}

	private boolean hasToPauseGlobalTransfers() {
		return networkBoard.totalDataInQueueForAllDistantKernelAgent
				.get() < getMaxSizeForUnreadShortDataFromAllDistantKernel() / 2;
	}

	private void decrementTotalDataQueueAllDistantKernelAgent(long value) {
		if (value < 0)
			throw new IllegalAccessError();
		long val = networkBoard.totalDataInQueueForAllDistantKernelAgent.addAndGet(-value);
		if (val < 0) {
			networkBoard.totalDataInQueueForAllDistantKernelAgent.set(0);
			Logger logger=getLogger();
			if (logger!=null)
				logger.severe("DistantKernelAgent.totalDataInQueueForAllDistantKernelAgent cannot be negative");
		}
		if (hasToPauseGlobalTransfers()) {
			setGlobalTransfersPaused(false);
		}
	}

	@SuppressWarnings("unused")
    private void processInvalidSerializedData(AgentAddress source, Exception e, ReadPacket _read_packet) {
		processInvalidProcess(source, "Invalid serialized data from Kernel Address " + distant_kernel_address, e,
				false);
	}

	@SuppressWarnings("SameParameterValue")
    private void processInvalidMessage(Message _message, boolean candidate_to_ban) {
		processInvalidProcess(null, "Invalid message " + _message, candidate_to_ban);
	}

	@SuppressWarnings({"SameParameterValue", "unused"})
    private void processInvalidPacketPart(AgentAddress source, Exception e, PacketPart _part,
                                          boolean candidate_to_ban) {
		processInvalidProcess(source, "Invalid packet part from Kernel Address " + distant_kernel_address, e,
				candidate_to_ban);
	}

	@SuppressWarnings("unused")
    private void processInvalidSerializedObject(AgentAddress source, Exception e, Object data,
                                                boolean candidate_to_ban) {
		processInvalidProcess(source, "Invalid serialized object from Kernel Address " + distant_kernel_address, e,
				candidate_to_ban);
	}

	private void processTooMuchSimultaneousShortDataSent() {
		processInvalidProcess(null, "Too much data sent from Kernel Address " + distant_kernel_address, true);
		Logger logger=getLogger();
		if (logger!=null)
			logger.severe(
					"Short data from " + this.distant_kernel_address + " sent too big. Killing related connections.");
	}

	protected void processPotentialDDOS() {
		processInvalidProcess(null, "Potential DDOS from Kernel Address " + distant_kernel_address, true);
		Logger logger=getLogger();
		if (logger!=null)
			logger.severe(
					"Detected potential DDOS from " + this.distant_kernel_address + ". Killing related connections.");
	}

	@SuppressWarnings("SameParameterValue")
    private void processInvalidProcess(AgentAddress source, String message, boolean candidate_to_ban) {
		processInvalidProcess(source, message, null, candidate_to_ban);
	}

	private void processInvalidProcess(AgentAddress source, String message, Exception e, boolean candidate_to_ban) {
		if (logger != null) {
			if (e == null)
				logger.severeLog(message == null ? "Invalid process" : message);
			else
				logger.severeLog(message == null ? "Invalid process" : message, e);
		}
		try {
			if (source != null) {
				sendMessageWithRole(source, new AskForConnectionMessage(ConnectionClosedReason.CONNECTION_ANOMALY, null,
						null, null, false, false), LocalCommunity.Roles.DISTANT_KERNEL_AGENT_ROLE);
			} else if (distant_kernel_address != null) {
				broadcastMessageWithRole(LocalCommunity.Groups.getDistantKernelAgentGroup(getNetworkID()),
						LocalCommunity.Roles.SOCKET_AGENT_ROLE,
						new AnomalyDetectedMessage(candidate_to_ban, distant_kernel_address, message),
						LocalCommunity.Roles.DISTANT_KERNEL_AGENT_ROLE);
			}
		} catch (Exception e2) {
			if (logger != null)
				logger.severeLog("Unexpected exception", e2);
			else
				e2.printStackTrace();
		}
	}

}
