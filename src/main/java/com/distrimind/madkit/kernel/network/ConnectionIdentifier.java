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

import java.io.Serializable;
import java.net.InetSocketAddress;

import com.distrimind.madkit.kernel.network.TransferAgent.IDTransfer;

/**
 * Identify a connection between two peers. It can concerns a direct connection,
 * or an indirect connection.
 * 
 * @author Jason Mahdjoub
 * @version 1.1
 * @since MadkitLanEdition 1.0
 * 
 *
 */
public final class ConnectionIdentifier implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = -1955268639719634401L;

	private final IDTransfer idTransfer;
	private final InetSocketAddress distantInetSocketAddress;
	private final InetSocketAddress localInetSocketAddress;
	private final int hashCode;

	ConnectionIdentifier(IDTransfer idTransfer, InetSocketAddress distantInetSocketAddress,
						 InetSocketAddress localInetSocketAddress) {
		if (idTransfer == null)
			throw new NullPointerException("idTransfer");
		if (distantInetSocketAddress == null)
			throw new NullPointerException("distantInetSocketAddress");
		if (localInetSocketAddress == null)
			throw new NullPointerException("localInetSocketAddress");
		this.idTransfer = idTransfer;
		this.distantInetSocketAddress = distantInetSocketAddress;
		this.localInetSocketAddress = localInetSocketAddress;
		hashCode = idTransfer.hashCode() + distantInetSocketAddress.hashCode() + localInetSocketAddress.hashCode();
	}

	@Override
	public boolean equals(Object o) {
		if (o == null)
			return false;
		if (o.getClass() == ConnectionIdentifier.class) {
			ConnectionIdentifier ci = (ConnectionIdentifier) o;
			return idTransfer.equals(ci.idTransfer) && distantInetSocketAddress.equals(ci.distantInetSocketAddress)
					&& localInetSocketAddress.equals(ci.localInetSocketAddress);
		}
		return false;
	}

	@Override
	public int hashCode() {
		return hashCode;
	}

	@Override
	public String toString() {
		return "Connection[" + idTransfer + "," + distantInetSocketAddress + "," + localInetSocketAddress + "]";
	}

	public int getIdTransfer() {
		return idTransfer.getID();
	}

	public InetSocketAddress getDistantInetSocketAddress() {
		return distantInetSocketAddress;
	}

	public InetSocketAddress getLocalInetSocketAddress() {
		return localInetSocketAddress;
	}

	public boolean isDirectConnection() {
		return idTransfer.equals(TransferAgent.NullIDTransfer);
	}

}
