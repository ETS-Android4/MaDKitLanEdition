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


import com.distrimind.util.Cleanable;
import com.distrimind.util.io.SecureExternalizable;
import com.distrimind.util.io.SecuredObjectInputStream;
import com.distrimind.util.io.SecuredObjectOutputStream;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 
 * This class represents the conversation ID to which a message belongs.
 * 
 * When a message is created, it is given an ID that will be used to tag all the
 * messages that will be created for answering this message using
 * {@link AbstractAgent#sendReply(Message, Message)} like methods. Especially,
 * if the answer is again used for replying, the ID will be used again to tag
 * this new answer, and so on.
 * 
 * 
 * @author Fabien Michel
 * @author Jason Mahdjoub
 * @version 2.2
 * @since MadKitLanEdition 1.0
 */
public class ConversationID implements SecureExternalizable, Cloneable, Cleanable {

	final static private AtomicInteger ID_COUNTER = new AtomicInteger(
			(int) (Math.random() * ((double) Integer.MAX_VALUE)));// TODO if many many ??
	protected static final class Finalizer extends Cleanable.Cleaner
	{
		private transient volatile Map<KernelAddress, InterfacedIDs> global_interfaced_ids = null;
		private transient Map<KernelAddress, OriginalID> myInterfacedIDs = null;
		private final boolean finalize;
		private int id;

		public Finalizer(ConversationID cleaner, int id) {
			super(cleaner);
			this.finalize = cleaner!=null;
			this.id=id;
		}

		@Override
		protected void performCleanup() {
			if (finalize && global_interfaced_ids != null && myInterfacedIDs != null) {

				//noinspection SynchronizeOnNonFinalField
				synchronized (global_interfaced_ids) {
					try {
						for (Map.Entry<KernelAddress, OriginalID> kpi : myInterfacedIDs.entrySet()) {
							InterfacedIDs i2 = global_interfaced_ids.get(kpi.getKey());
							i2.removeID(id);
							if (i2.isEmpty()) {
								global_interfaced_ids.remove(kpi.getKey());
							}
						}
					} catch (Throwable e) {
						e.printStackTrace();
					}
				}
				global_interfaced_ids = null;
				myInterfacedIDs = null;

			}
		}
	}

	private KernelAddress origin;
	private final Finalizer finalizer;
	@SuppressWarnings("unused")
	ConversationID() {
		this(true);
	}
	ConversationID(boolean finalize) {
		finalizer=new Finalizer(finalize?this:null, -1);
		//id = ID_COUNTER.getAndIncrement();
		origin = null;
	}
	static ConversationID getConversationIDInstance()
	{
		return new ConversationID(ID_COUNTER.getAndIncrement(), null);
	}

	protected int getID() {
		return finalizer.id;
	}

	ConversationID(int id, KernelAddress origin) {
		this(true, id, origin);
	}
	ConversationID(boolean finalize, int id, KernelAddress origin) {
		finalizer=new Finalizer(finalize?this:null, id);
		this.origin = origin;
	}

	ConversationID(ConversationID conversationID) {
		if (conversationID==null)
			throw new NullPointerException();
		this.finalizer=new Finalizer(conversationID.finalizer.finalize?this:null, conversationID.finalizer.id);
		this.origin = conversationID.origin;
		if (conversationID.finalizer.global_interfaced_ids!=null)
		{
			this.finalizer.global_interfaced_ids=conversationID.finalizer.global_interfaced_ids;
			if (conversationID.finalizer.myInterfacedIDs!=null)
			{
				this.finalizer.myInterfacedIDs=Collections.synchronizedMap(new HashMap<>());
				try {
					for (Map.Entry<KernelAddress, OriginalID> kpi : conversationID.finalizer.myInterfacedIDs.entrySet()) {
						finalizer.myInterfacedIDs.put(kpi.getKey(), kpi.getValue());
						kpi.getValue().incrementPointerToThisOriginalID();
					}
				} catch (Throwable e) {
					e.printStackTrace();
				}
				//}

			}
			else
				this.finalizer.myInterfacedIDs=null;
		}
		else
			this.finalizer.global_interfaced_ids=null;
	}


	@SuppressWarnings("MethodDoesntCallSuperMethod")
	@Override
	public ConversationID clone() {
		return this;
	}

	@Override
	public String toString() {
		return finalizer.id + (origin == null ? "" : origin.toString());
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if ((obj instanceof ConversationID)) {// obj necessarily comes from the network or is different,
																// so origin should have been set priorly if there is a
																// chance of equality
			final ConversationID ci = (ConversationID) obj;// no check is intentional

			return this.getID() == ci.getID()
					&& ((getOrigin() == ci.getOrigin()) || (getOrigin() != null && getOrigin().equals(ci.getOrigin())));
		}

		return false;
	}

	void setOrigin(KernelAddress origin) {
		if (this.origin == null) {
			this.origin = origin;
		}
	}

	@Override
	public int hashCode() {
		return finalizer.id;
	}

	public KernelAddress getOrigin() {
		return origin;
	}

	

	private static class OriginalID {
		final int originalID;
		private final AtomicInteger nbPointers;

		OriginalID(int originalID) {
			this(originalID, new AtomicInteger(0));
		}

		OriginalID(int originalID, AtomicInteger nbPointers) {
			this.originalID = originalID;
			this.nbPointers = nbPointers;
		}

		public void incrementPointerToThisOriginalID() {
			nbPointers.incrementAndGet();
		}

		public int getOriginalID() {
			// nbPointers.incrementAndGet();
			return originalID;
		}

		public AtomicInteger getNbPointers() {
			return nbPointers;
		}

		public boolean remove() {
			int val = nbPointers.decrementAndGet();
			if (val < 0)
				new IllegalAccessError().printStackTrace();
			return val <= 0;
		}
	}

	static class InterfacedIDs {
		private int id_counter;
		private final HashMap<Integer, OriginalID> original_ids = new HashMap<>();
		private final HashMap<Integer, OriginalID> distant_ids = new HashMap<>();

		InterfacedIDs() {
			id_counter = (int) (Math.random() * (double) Integer.MAX_VALUE);
		}

		private int getAndIncrementIDCounter() {
			if (++id_counter == -1)
				return ++id_counter;
			else
				return id_counter;
		}

		OriginalID getNewID(Integer original) {
			OriginalID res = distant_ids.get(original);
			if (res == null) {
				res = new OriginalID(getAndIncrementIDCounter());
				original_ids.put(res.originalID, new OriginalID(original, res.getNbPointers()));
				distant_ids.put(original, res);
			}
			res.incrementPointerToThisOriginalID();
			return res;
		}

		void removeID(Integer original) {
			OriginalID di = distant_ids.get(original);
			if (di.remove())
				original_ids.remove(distant_ids.remove(original).originalID);
		}

		OriginalID getNewIDFromDistantID(Integer distantID) {

			OriginalID res = original_ids.get(distantID);
			if (res == null) {
				res = new OriginalID(getAndIncrementIDCounter());
				distant_ids.put(res.originalID, new OriginalID(distantID, res.getNbPointers()));
				original_ids.put(distantID, res);
			}
			res.incrementPointerToThisOriginalID();
			return res;
		}

		OriginalID getOriginalID(int distant_id) {
			return original_ids.get(distant_id);
		}
		OriginalID getDistantOriginalID(int local_id) {
			return distant_ids.get(local_id);
		}



		boolean isEmpty() {
			return original_ids.isEmpty();
		}

	}



	Map<KernelAddress, InterfacedIDs> getGlobalInterfacedIDs() {
		return this.finalizer.global_interfaced_ids;
	}



	ConversationID getInterfacedConversationIDToDistantPeer(Map<KernelAddress, InterfacedIDs> global_interfaced_ids,
			KernelAddress currentKernelAddress, KernelAddress distantKernelAddress) {

		if (origin.equals(distantKernelAddress))
			return this;
		else if (origin.equals(currentKernelAddress)) {
			OriginalID distantId = null;
			if (finalizer.myInterfacedIDs != null) {
				distantId = finalizer.myInterfacedIDs.get(distantKernelAddress);
			}
			if (distantId == null) {
				//noinspection SynchronizationOnLocalVariableOrMethodParameter
				synchronized (global_interfaced_ids) {
					if (finalizer.myInterfacedIDs == null)
						finalizer.myInterfacedIDs = Collections.synchronizedMap(new HashMap<>());
					else
						distantId = finalizer.myInterfacedIDs.get(distantKernelAddress);
					if (distantId==null) {
						this.finalizer.global_interfaced_ids = global_interfaced_ids;
						InterfacedIDs i = global_interfaced_ids.get(distantKernelAddress);
						if (i == null) {
							i = new InterfacedIDs();
							global_interfaced_ids.put(distantKernelAddress, i);
						}
						distantId = i.getNewID(this.finalizer.id);
					}
					else
						distantId.incrementPointerToThisOriginalID();
				}

				finalizer.myInterfacedIDs.put(distantKernelAddress, distantId);
			}
			else
			{
				distantId.incrementPointerToThisOriginalID();
			}
			/*
			 * else { myInterfacedIDs.put(distantKernelAddress, distantId); }
			 */
			if (this instanceof BigDataTransferID)
				return new BigDataTransferID(distantId.getOriginalID(), origin, ((BigDataTransferID) this).getBytePerSecondsStat());
			else
				return new ConversationID(distantId.getOriginalID(), origin);
			/*
			 * ConversationID cid=new ConversationID(distantId.getOriginalID(), origin);
			 * cid.myInterfacedIDs=new HashMap<KernelAddress, ConversationID.OriginalID>();
			 * cid.myInterfacedIDs.put(distantKernelAddress, distantId);
			 * distantId.incrementPointerToThisOriginalID(); return cid;
			 */
		} else {
			if (this instanceof BigDataTransferID)
				return new BigDataTransferID(0, null, null);
			else
				return new ConversationID(0, null);

		}
	}

	@SuppressWarnings("SynchronizationOnLocalVariableOrMethodParameter")
	ConversationID getInterfacedConversationIDFromDistantPeer(Map<KernelAddress, InterfacedIDs> global_interfaced_ids,
															  KernelAddress currentKernelAddress, KernelAddress distantKernelAddress) {
		if (origin == null) {
			return BigDataTransferID.getConversationIDInstance();
		} else if (origin.equals(distantKernelAddress)) {
			return this;
		} else if (origin.equals(currentKernelAddress)) {

			synchronized (global_interfaced_ids) {
				InterfacedIDs i = global_interfaced_ids.get(distantKernelAddress);
				if (i == null) {

					i = new InterfacedIDs();
					global_interfaced_ids.put(distantKernelAddress, i);

				}

				OriginalID o = i.getOriginalID(finalizer.id);
				if (o == null) {
					o = i.getNewIDFromDistantID(this.finalizer.id);
				}
				else {
					o.incrementPointerToThisOriginalID();
				}
				OriginalID distantOriginalID=i.getDistantOriginalID(o.getOriginalID());
				assert distantOriginalID!=null;
				assert distantOriginalID.originalID==this.finalizer.id;
				ConversationID cid;
				if (this instanceof BigDataTransferID)
					cid=new BigDataTransferID(o.getOriginalID(), origin, ((BigDataTransferID) this).getBytePerSecondsStat());
				else
					cid = new ConversationID(o.getOriginalID(), origin);

				cid.finalizer.global_interfaced_ids = global_interfaced_ids;
				cid.finalizer.myInterfacedIDs = Collections.synchronizedMap(new HashMap<>());
				cid.finalizer.myInterfacedIDs.put(distantKernelAddress, distantOriginalID);
				return cid;
			}
		} else 	if (this instanceof BigDataTransferID)
			return BigDataTransferID.getConversationIDInstance();
		else {
			return ConversationID.getConversationIDInstance();
		}
	}

	@Override
	public int getInternalSerializedSize() {
		
		return 4+this.origin.getInternalSerializedSize();
	}

	@Override
	public void writeExternal(SecuredObjectOutputStream out) throws IOException {
		out.writeInt(this.finalizer.id);
		out.writeObject(this.origin, true);
	}

	@Override
	public void readExternal(SecuredObjectInputStream in) throws IOException, ClassNotFoundException {
		finalizer.performCleanup();
		this.finalizer.id=in.readInt();
		this.origin=in.readObject(true, KernelAddress.class);
	}

	@Override
	public void close() {
		try {
			Cleanable.super.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
