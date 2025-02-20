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


import com.distrimind.madkit.kernel.Message;
import com.distrimind.madkit.kernel.network.DistantKernelAgent.ReceivedSerializableObject;
import com.distrimind.madkit.util.NetworkMessage;
import com.distrimind.util.Cleanable;

/**
 * 
 * @author Jason Mahdjoub
 * @version 1.0
 * @since MadkitLanEdition 1.0
 */
public class LocalLanMessage extends Message implements LockableMessage, Cleanable {

	protected static final class Finalizer extends Cleanable.Cleaner
	{
		ReceivedSerializableObject originalMessage;

		private Finalizer(Cleanable cleanable) {
			super(cleanable);
		}

		@Override
		protected void performCleanup() {
			if (originalMessage != null) {
				originalMessage.markDataAsRead();
				originalMessage = null;
			}
		}
	}
	protected final Finalizer finalizer;
	private final Message message;

	private final MessageLocker locker;

	protected boolean readyForInjection = false;
	private boolean innerMessageNotInitialized =true;
	// int id_packet=-1;

	
	
	
	protected LocalLanMessage(Message _message, ReceivedSerializableObject originalMessage) {
		if (!(_message instanceof NetworkMessage))
			throw new IllegalArgumentException("The message to send me implements NetworkMessage interface");
		finalizer=new Finalizer(this);
		message = _message;
		locker = new MessageLocker(this);
		this.finalizer.originalMessage = originalMessage;
	}

	protected LocalLanMessage(LocalLanMessage This, Message _message, ReceivedSerializableObject originalMessage,
			MessageLocker locker) {
		super(This);
		if (!(_message instanceof NetworkMessage))
			throw new IllegalArgumentException("The message to send me implements NetworkMessage interface");
		finalizer=new Finalizer(this);
		message = _message;
		this.locker = locker;
		this.finalizer.originalMessage = originalMessage;
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + "[conversationID=" + message.getConversationID() + ", readyForInjection="
				+ readyForInjection + "]";
	}
	protected Message getWrappedInnerMessage()
	{
		if (readyForInjection)
		{
			if (innerMessageNotInitialized) {
				MadkitKernelAccess.setReceiver(message, getReceiver());
				MadkitKernelAccess.setSender(message, getSender());
				innerMessageNotInitialized = false;
			}
			return message;
		}
		else
			return this;

	}
	@Override
	protected Message markMessageAsRead() {
		if (readyForInjection) {
			if (finalizer.originalMessage != null) {
				finalizer.originalMessage.markDataAsRead();
				finalizer.originalMessage = null;
			}
			return MadkitKernelAccess.markAsRead(message);
		} else {
			return this;
		}
	}

	public void setReadyForInjection() {
		readyForInjection = true;
	}


	/*
	 * protected LocalLanMessage(MessageLocker _message_locker, Message _message) {
	 * message=_message; locker=_message_locker; locker.lock(); }
	 */

	@SuppressWarnings("MethodDoesntCallSuperMethod")
	@Override
	public LocalLanMessage clone() {
		return this;
	}

	@Override
	public MessageLocker getMessageLocker() {
		return locker;
	}

	/*
	 * void setIDPacket(int _id) { id_packet=_id; }
	 */
	public Message getOriginalMessage() {
		return message;
	}


}
