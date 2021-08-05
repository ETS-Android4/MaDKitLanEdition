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

import com.distrimind.madkit.kernel.network.RealTimeTransferStat;
import com.distrimind.util.io.SecuredObjectInputStream;
import com.distrimind.util.io.SecuredObjectOutputStream;

import java.io.IOException;
import java.util.Map;

/**
 * Represent an identifier for a big data transfer.
 * 
 * @author Jason Mahdjoub
 * @version 2.1
 * @since MadkitLanEdition 1.0
 * @see AbstractAgent#sendBigDataWithRole(AgentAddress, com.distrimind.util.io.RandomInputStream, long, long, com.distrimind.util.io.SecureExternalizable, com.distrimind.util.crypto.MessageDigestType, String, boolean)
 */
public class BigDataTransferID extends ConversationID {

	private transient RealTimeTransferStat stat;

	@SuppressWarnings("unused")
	BigDataTransferID() {
	}

	private ConversationID cid;

	@Override
	public void writeExternal(SecuredObjectOutputStream out) throws IOException {
		super.writeExternal(out);
		out.writeObject(cid,false );

	}

	@Override
	public void readExternal(SecuredObjectInputStream in) throws IOException, ClassNotFoundException {
		stat=null;
		super.readExternal(in);
		cid=in.readObject(false, ConversationID.class);
	}
	
	
	BigDataTransferID(ConversationID cid, RealTimeTransferStat stat) {
		super(-1, null);
		//super(cid);
		if (cid == null)
			throw new NullPointerException("cid");
		this.stat = stat;
		this.cid = cid;
	}
	BigDataTransferID(int id, KernelAddress kernelAddress, RealTimeTransferStat stat) {
		super(id, kernelAddress);
		this.stat = stat;
	}

	@Override
	protected int getID() {
		return cid.getID();
	}

	@Override
	public KernelAddress getOrigin() {
		return cid.getOrigin();
	}

	@Override
	public String toString() {
		return cid.toString();
	}

	@Override
	void setOrigin(KernelAddress origin) {
		cid.setOrigin(origin);
	}

	@Override
	public int hashCode() {
		return cid.hashCode();
	}


	@Override
	ConversationID getInterfacedConversationIDToDistantPeer(Map<KernelAddress, InterfacedIDs> global_interfaced_ids,
			KernelAddress currentKernelAddress, KernelAddress distantKernelAddress) {
		return cid.getInterfacedConversationIDToDistantPeer(global_interfaced_ids, currentKernelAddress,
				distantKernelAddress);
	}

	@Override
	ConversationID getInterfacedConversationIDFromDistantPeer(Map<KernelAddress, InterfacedIDs> global_interfaced_ids,
			KernelAddress currentKernelAddress, KernelAddress distantKernelAddress) {
		return cid.getInterfacedConversationIDFromDistantPeer(global_interfaced_ids, currentKernelAddress,
				distantKernelAddress);
	}

	@SuppressWarnings("MethodDoesntCallSuperMethod")
    @Override
	protected void finalize() {
	}

	@Override
	Map<KernelAddress, InterfacedIDs> getGlobalInterfacedIDs() {
		return cid.getGlobalInterfacedIDs();
	}

	/**
	 * Gets statistics in bytes per seconds related to the concerned big data
	 * transfer
	 * 
	 * @return statistics in bytes per seconds related to the concerned big data
	 *         transfer
	 */
	public RealTimeTransferStat getBytePerSecondsStat() {
		return stat;
	}
}
