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

import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;

import com.distrimind.madkit.kernel.Message;

/**
 * Order to bind specific lan port, with a specific {@link InetAddress}.
 * 
 * @author Jason Mahdjoub
 *
 * @see InetSocketAddress
 * @version 1.0
 * @since MadkitLanEdition 1.0
 */
public class BindInetSocketAddressMessage extends Message {

	
	private final InetSocketAddress address;
	private final Type type;
	final long networkInterfaceSpeed;

	@Override
	public String toString() {
		return "BindInetSocketAddressMessage[address=" + address + ", type=" + type + ", networkInterfaceSpeed="
				+ networkInterfaceSpeed + "]";
	}

	BindInetSocketAddressMessage(Type _type, InetSocketAddress _address) {
		if (_address == null)
			throw new NullPointerException("_address");
		address = _address;
		type = _type;
		this.networkInterfaceSpeed = -1;
	}

	BindInetSocketAddressMessage(Type _type, InetSocketAddress _address, long networkInterfaceSpeed) {
		if (_address == null)
			throw new NullPointerException("_address");
		address = _address;
		type = _type;
		this.networkInterfaceSpeed = networkInterfaceSpeed;
	}

	BindInetSocketAddressMessage(Type _type, InetAddress _address, int _port) {
		this(_type, new InetSocketAddress(_address, _port));
	}

	BindInetSocketAddressMessage(Type _type, int _port) {
		this(_type, new InetSocketAddress(_port));
	}

	InetSocketAddress getInetSocketAddress() {
		return address;
	}

	boolean include(BindInetSocketAddressMessage m) {
		if (address.getAddress() instanceof Inet4Address) {
			if (!(m.address.getAddress() instanceof Inet4Address)) {
				return false;
			}
			return address.getPort() == m.address.getPort() && (address.getAddress().equals(m.address.getAddress())
					|| (address.getAddress().isAnyLocalAddress() && m.address.getAddress().isSiteLocalAddress()));
		} else if (address.getAddress() instanceof Inet6Address) {
			if (!(m.address.getAddress() instanceof Inet6Address)) {
				return false;
			}
			return address.getPort() == m.address.getPort() && (address.getAddress().equals(m.address.getAddress())
					|| (address.getAddress().isAnyLocalAddress() && (m.address.getAddress().isSiteLocalAddress()
							|| m.address.getAddress().isLinkLocalAddress())));
		} else
			return false;

	}

	Type getType() {
		return type;
	}

	enum Type {
		BIND, DISCONNECT
	}

	@Override
	public boolean equals(Object o) {
		if (o == null)
			return false;
		if (o == this)
			return true;
		if (o instanceof BindInetSocketAddressMessage) {
			BindInetSocketAddressMessage bind = (BindInetSocketAddressMessage) o;
			return bind.address.equals(address) && bind.type.equals(type);
		}
		return false;
	}

}
