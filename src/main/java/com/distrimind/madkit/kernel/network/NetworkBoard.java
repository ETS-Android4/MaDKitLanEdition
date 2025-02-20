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

import com.distrimind.madkit.kernel.KernelAddress;
import com.distrimind.madkit.kernel.Task;
import com.distrimind.madkit.kernel.TaskID;
import com.distrimind.util.concurrent.LockerCondition;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 
 * @author Jason Mahdjoub
 * @version 1.1
 * @since MadkitLanEdition 1.0
 */
class NetworkBoard {
	final AtomicLong totalDataInQueueForAllDistantKernelAgent = new AtomicLong(0);
	final AtomicBoolean transferPausedForAllDistantKernelAgent = new AtomicBoolean(false);
	DistantKernelAgent currentCandidateForPurge = null;
	final ArrayList<DistantKernelAgent> candidatesForPurge = new ArrayList<>();

	//private final HashMap<KernelAddress, KernelAddressInterfaced> distant_kernel_addresses = new HashMap<>();
	protected final HashMap<KernelAddress, LockerCondition> lockerForSimultaneousConnections = new HashMap<>();

	void lockForSimultaneousConnections(DistantKernelAgent agent, KernelAddress kernelAddress)
			throws InterruptedException {
		//do {
			final KernelAddress ka = (kernelAddress instanceof KernelAddressInterfaced)
					? ((KernelAddressInterfaced) kernelAddress).getOriginalKernelAddress()
					: kernelAddress;
			LockerCondition lc = new LockerCondition() {
				private boolean alreadyLockedOneTime=false;
				@Override
				public boolean isLocked() {
					synchronized(lockerForSimultaneousConnections)
					{
						LockerCondition lctmp=lockerForSimultaneousConnections.get(ka);
						boolean locked=  lctmp!=null;
						if (locked && lctmp==this)
						{
							alreadyLockedOneTime=true;
							return false;
						}
						
						if (!locked && !alreadyLockedOneTime)
						{
							lockerForSimultaneousConnections.put(ka, this);
							alreadyLockedOneTime=true;
							return false;
						}
						return locked && !alreadyLockedOneTime;

					}
				}
			};
			lc.setLocker(lockerForSimultaneousConnections);

			synchronized (lockerForSimultaneousConnections) {
				//lc = null;
				lockerForSimultaneousConnections.putIfAbsent(ka, lc);
			}
			agent.wait(lc);
			/*if (lc != null) {
				agent.wait(lc);
			} else {
				return;
			}
		} while (true);*/
	}

	void unlockSimultaneousConnections(KernelAddress kernelAddress) {

		final KernelAddress ka = (kernelAddress instanceof KernelAddressInterfaced)
				? ((KernelAddressInterfaced) kernelAddress).getOriginalKernelAddress()
				: kernelAddress;

		synchronized (lockerForSimultaneousConnections) {
			lockerForSimultaneousConnections.remove(ka);
			lockerForSimultaneousConnections.notifyAll();
		}

	}

	boolean checkDistantKernelAgentCandidateForPurgeEmpty() {
		if (totalDataInQueueForAllDistantKernelAgent.get() != 0) {
			new Exception("" + totalDataInQueueForAllDistantKernelAgent.get()).printStackTrace();
			return false;
		}
		if (transferPausedForAllDistantKernelAgent.get()) {
			new Exception("" + transferPausedForAllDistantKernelAgent.get()).printStackTrace();
			return false;
		}
		if (currentCandidateForPurge != null) {
			new Exception(currentCandidateForPurge.toString()).printStackTrace();
			return false;
		}
		if (!candidatesForPurge.isEmpty()) {
			new Exception("" + candidatesForPurge.size()).printStackTrace();
			return false;
		} else
			return true;

	}


	@SuppressWarnings("unused")
	boolean checkBoardEmpty() {
		return checkDistantKernelAgentCandidateForPurgeEmpty();
	}

	protected TaskID taskIDToRemoveDatagramMessages = null;
	protected final HashSet<DatagramLocalNetworkPresenceMessage> datagramMessages = new HashSet<>();

	protected boolean addMessage(MulticastListenerAgent agent, DatagramLocalNetworkPresenceMessage m) {
		synchronized (datagramMessages) {
			if (datagramMessages.contains(m))
				return false;
			else {
				datagramMessages.add(m);
				if (taskIDToRemoveDatagramMessages == null) {
					taskIDToRemoveDatagramMessages = agent.scheduleTask(new Task<Void>(MulticastListenerAgent.durationBeforeRemovingMulticastMessages + System.currentTimeMillis(),
							MulticastListenerAgent.durationBeforeRemovingMulticastMessages) {
						@Override
						public Void call() {
							synchronized (datagramMessages) {
								datagramMessages.removeIf(m1 -> m1.getOnlineTime()
										+ MulticastListenerAgent.durationBeforeRemovingMulticastMessages < System
										.currentTimeMillis());
								if (datagramMessages.isEmpty()) {
									taskIDToRemoveDatagramMessages.cancelTask(false);
									taskIDToRemoveDatagramMessages = null;
								}

							}
							return null;
						}
					});
				}
				return true;
			}
		}
	}

}
