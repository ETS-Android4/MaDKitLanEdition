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
import com.distrimind.madkit.kernel.network.TransferAgent.IDTransfer;
import com.distrimind.madkit.util.SecuredObjectInputStream;
import com.distrimind.madkit.util.SecuredObjectOutputStream;

import java.io.IOException;

/**
 * 
 * @author Jason Mahdjoub
 * @version 1.1
 * @since MadkitLanEdition 1.0
 */
final class DataToBroadcast implements SystemMessage {


	private BroadcastableSystemMessage messageToBroadcast;
	private KernelAddress sender;
	private boolean prioritary;
	private IDTransfer transferID;

	@SuppressWarnings("unused")
	DataToBroadcast()
	{
		
	}
	@Override
	public void readExternal(SecuredObjectInputStream in) throws IOException, ClassNotFoundException {
		messageToBroadcast=in.readObject(false, BroadcastableSystemMessage.class);
		sender=in.readObject(false, KernelAddress.class);
		prioritary=in.readBoolean();
		transferID=in.readObject(false, IDTransfer.class);
	}

	@Override
	public void writeExternal(SecuredObjectOutputStream oos) throws IOException {
		oos.writeObject(messageToBroadcast, false);
		oos.writeObject(sender, false);
		oos.writeBoolean(prioritary);
		oos.writeObject(transferID, false);
	}
	
	DataToBroadcast(BroadcastableSystemMessage messageToBroadcast, KernelAddress sender, boolean prioritary,
			IDTransfer transferID) {
		if (messageToBroadcast == null)
			throw new NullPointerException("messageToBroadcast");
		if (sender == null)
			throw new NullPointerException("sender");
		if (transferID == null)
			throw new NullPointerException("transferID");
		this.messageToBroadcast = messageToBroadcast;
		this.sender = sender;
		this.prioritary = prioritary;
		this.transferID = transferID;
	}

	@Override
	public String toString() {
		return "DataToBroadcast[messageToBroadcast=" + messageToBroadcast + ", senderKernelAddress=" + sender
				+ ", prioritary=" + prioritary + ", transferID=" + transferID + "]";
	}

	IDTransfer getTransferID() {
		return transferID;
	}

	BroadcastableSystemMessage getMessageToBroadcast() {
		return messageToBroadcast;
	}

	KernelAddress getSender() {
		return sender;
	}

	boolean isPrioritary() {
		return prioritary;
	}


	@Override
	public boolean excludedFromEncryption() {
		return false;
	}


	
}
