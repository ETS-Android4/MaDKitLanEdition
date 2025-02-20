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

import com.distrimind.madkit.exceptions.MadkitException;
import com.distrimind.madkit.exceptions.PacketException;
import com.distrimind.madkit.kernel.network.TransferAgent.IDTransfer;
import com.distrimind.util.Cleanable;

import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * 
 * @author Jason Mahdjoub
 * @version 1.0
 * @since MadkitLanEdition 1.0
 */
abstract class AbstractData<F extends AbstractData.Finalizer> implements Cleanable {
	protected static abstract class Finalizer extends Cleaner
	{
		protected Finalizer() {
			super(null);
		}

		abstract boolean isUnlocked();
		abstract void unlockMessage(boolean cancel) throws MadkitException;
		@Override
		protected void performCleanup() {
			synchronized (this) {
				try {
					if (!isUnlocked())
						unlockMessage(false);
				} catch (Exception ignored) {

				}
			}
		}
	}
	protected final F finalizer;
	private final boolean priority;
	protected volatile boolean isCanceled=false;


	AbstractData(boolean priority, F finalizer) {
		if (finalizer==null)
			throw new NullPointerException();
		this.finalizer=finalizer;
		registerCleanerIfNotDone(finalizer);
		this.priority = priority;
	}

	boolean isPriority() {
		return priority;
	}
	/*
	 * void setPriority(boolean priority) { this.priority=priority; }
	 */

	public void unlockMessage() throws MadkitException {
		unlockMessage(false);
	}
	final void unlockMessage(boolean cancel) throws MadkitException
	{
		finalizer.unlockMessage(cancel);
	}

	final boolean isUnlocked()
	{
		return finalizer.isUnlocked();
	}

	abstract ByteBuffer getByteBuffer() throws PacketException;

	//abstract boolean isReady();

	abstract boolean isFinished() throws PacketException;

	abstract boolean isCurrentByteBufferFinished() throws PacketException;
	abstract boolean isCurrentByteBufferFinishedOrNotStarted() throws PacketException;
	//abstract boolean isCurrentByteBufferStarted();

	abstract DataTransferType getDataTransferType();

	abstract IDTransfer getIDTransfer();
	
	abstract void reset();

	@Override
	public String toString() {
		return getClass().getSimpleName() + "[priority=" + isPriority() + ", dataTransferType=" + getDataTransferType()
				+ ", idTransfer=" + getIDTransfer() + ", lastMessage=" + isLastMessage() + "]";
	}


	/*long timeToSend() {
		return Long.MIN_VALUE;
	}*/
	
	abstract Object getLocker();

	boolean isLastMessage() {
		return false;
	}

	enum DataTransferType {
		SHORT_DATA, BIG_DATA, DATA_TO_TRANSFER
	}

	abstract boolean isDataBuildInProgress();
	void cancel() throws IOException, MadkitException {
		isCanceled=true;
	}
	void closeStream() throws IOException {
	}

	final boolean isCanceled()
	{
		return isCanceled;
	}
	abstract boolean isCanceledNow();
}
