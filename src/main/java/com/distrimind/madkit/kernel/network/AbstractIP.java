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

import com.distrimind.madkit.util.MultiFormatPropertiesObjectParser;
import com.distrimind.util.io.*;
import com.distrimind.util.properties.MultiFormatProperties;

import java.io.IOException;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.util.Collection;
import java.util.Objects;

/**
 * 
 * @author Jason Mahdjoub
 * @version 1.2
 * @since MadkitLanEdition 1.0
 */
public abstract class AbstractIP extends MultiFormatProperties implements SystemMessageWithoutInnerSizeControl, SecureExternalizable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 5670994991069850019L;

	private int port;
	private transient volatile int hashCode=-1;

	public final Inet6Address getInet6Address()
	{
		return getInet6Address(null);
	}

	public final Inet4Address getInet4Address()
	{
		return getInet4Address(null);
	}

	public abstract Inet6Address getInet6Address(Collection<InetAddress> rejectedIps);

	public abstract Inet4Address getInet4Address(Collection<InetAddress> rejectedIps);

	public abstract InetAddress[] getInetAddresses();

	@Override
	public void readExternal(SecuredObjectInputStream in) throws IOException, ClassNotFoundException {
		hashCode=-1;
		port=in.readInt();
		if (port<0)
			throw new MessageExternalizationException(Integrity.FAIL);
	}

	@Override
	public void writeExternal(SecuredObjectOutputStream oos) throws IOException {
		oos.writeInt(port);
	}
	@Override
	public int getInternalSerializedSize() {
		return 4;
	}
	
	public InetAddressFilter[] getInetAddressFilters() {
		InetAddress[] ias = getInetAddresses();
		InetAddressFilter[] res = new InetAddressFilter[ias.length];
		for (int i = 0; i < ias.length; i++) {
			res[i] = new InetAddressFilter(ias[i], (short) 0, port);
		}
		return res;
	}

	public abstract Inet6Address[] getInet6Addresses();

	public InetAddressFilter[] getInet6AddressFilters() {
		InetAddress[] ias = getInet6Addresses();
		InetAddressFilter[] res = new InetAddressFilter[ias.length];
		for (int i = 0; i < ias.length; i++) {
			res[i] = new InetAddressFilter(ias[i], (short) 0, port);
		}
		return res;
	}

	public abstract Inet4Address[] getInet4Addresses();

	public InetAddressFilter[] getInet4AddressFilters() {
		InetAddress[] ias = getInet4Addresses();
		InetAddressFilter[] res = new InetAddressFilter[ias.length];
		for (int i = 0; i < ias.length; i++) {
			res[i] = new InetAddressFilter(ias[i], (short) 0, port);
		}
		return res;
	}

	protected AbstractIP(int port) {
		super(new MultiFormatPropertiesObjectParser());
		this.port = port;
	}

	public int getPort() {
		return port;
	}
	public InetAddress getInetAddress() {
		return getInetAddress(null);
	}
	public InetAddress getInetAddress(Collection<InetAddress> rejectedIps) {
		InetAddress res = getInet6Address(rejectedIps);
		if (res == null)
			return getInet4Address(rejectedIps);
		else if (rejectedIps!=null && rejectedIps.contains(res))
		{
			InetAddress res2=getInet4Address(rejectedIps);
			if (res2!=null && !rejectedIps.contains(res2))
				return res2;
			else
				return res;
		}
		else
			return res;

	}

	@Override
	public String toString() {
		InetAddress ia = getInetAddress();
		if (ia == null)
			return "null:" + port;
		else
			return ia + ":" + port;
	}

	@SuppressWarnings("MethodDoesntCallSuperMethod")
	@Override
	public AbstractIP clone() {
		return this;
	}

	@Override
	public boolean equals(Object o) {
		if (o == null)
			return false;
		if (o == this)
			return true;
		if (o instanceof AbstractIP) {
			if (port != ((AbstractIP) o).port)
				return false;
			InetAddress[] ias = getInetAddresses();
			InetAddress[] oias = ((AbstractIP) o).getInetAddresses();
			if (ias == oias)
				return true;
			if (ias == null || oias == null)
				return false;
			if (ias.length != oias.length)
				return false;
			for (InetAddress ia1 : ias) {
				boolean found = false;
				for (InetAddress ia2 : oias) {
					if (ia1.equals(ia2)) {
						found = true;
						break;
					}
				}
				if (!found)
					return false;
			}
			return true;
		} else
			return false;
	}

	@Override
	public int hashCode() {
		if (hashCode==-1)
		{
			synchronized (this) {
				if (hashCode==-1) {
					hashCode = port;
					for (InetAddress ia : getInetAddresses()) {
						hashCode = Objects.hash(hashCode, ia);
					}
					if (hashCode == -1)
						hashCode = 0;
				}
			}
		}
		return hashCode;
	}

	public boolean areInternetAddresses()
	{
		for (InetAddress ia : getInetAddresses())
		{
			if (!isInternetAddress(ia))
				return false;
		}
		return true;
	}

	public static boolean isInternetAddress(InetAddress perceivedDistantInetAddress) {
		return !perceivedDistantInetAddress.isAnyLocalAddress() && !perceivedDistantInetAddress.isLinkLocalAddress()
				&& !perceivedDistantInetAddress.isLoopbackAddress() && !perceivedDistantInetAddress.isMulticastAddress()
				&& !perceivedDistantInetAddress.isSiteLocalAddress();
	}
	public boolean areLocalAddresses()
	{
		for (InetAddress ia : getInetAddresses())
		{
			if (!isLocalAddress(ia))
				return false;
		}
		return true;
	}
	public static boolean isLocalAddress(InetAddress perceivedDistantInetAddress) {
		return (perceivedDistantInetAddress.isAnyLocalAddress() && !perceivedDistantInetAddress.isLinkLocalAddress()
				&& !perceivedDistantInetAddress.isMulticastAddress()
				&& !perceivedDistantInetAddress.isSiteLocalAddress())
				|| perceivedDistantInetAddress.isLoopbackAddress();
	}
}
