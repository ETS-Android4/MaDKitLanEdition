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
import com.distrimind.madkit.util.SecuredObjectInputStream;
import com.distrimind.madkit.util.SecuredObjectOutputStream;
import com.distrimind.util.AbstractDecentralizedID;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * This class represents a secured unique identifier for a distant MaDKit
 * kernel. If the original given {@link KernelAddress} is not secured, this last
 * is interfaced thanks to an encapsulated address. Then no hacker could pretend
 * usurp an identity. Uniqueness is guaranteed even when different kernels run
 * on the same JVM or over the network.
 * 
 * @author Jason Mahdjoub
 * @version 1.0
 * @since MadKitLanEdition 1.0
 *
 */
public class KernelAddressInterfaced extends KernelAddress {

	private KernelAddress original_external_kernel_address;
	private AtomicBoolean interfaced;

	
	@SuppressWarnings("unused")
	KernelAddressInterfaced()
	{
		
	}
	
	/**
	 * @param _original_kernel_address
	 *            the original kernel address to interface
	 */
	public KernelAddressInterfaced(KernelAddress _original_kernel_address) {
		this(_original_kernel_address, true);
	}

	/**
	 * @param _original_kernel_address
	 *            the original kernel address to eventually interface
	 * @param identical_from_original_kernel_interface
	 *            true if the original kernel address do not need to be interfaced.
	 * 				
	 */
	public KernelAddressInterfaced(KernelAddress _original_kernel_address,
			boolean identical_from_original_kernel_interface) {
		super(false, false);
		if (_original_kernel_address == null)
			throw new NullPointerException("_original_kernel_address");
		original_external_kernel_address = _original_kernel_address;
		interfaced = new AtomicBoolean(!identical_from_original_kernel_interface);
		initName();
		internalSize+=original_external_kernel_address.getInternalSerializedSize()+1;
	}

	private KernelAddressInterfaced(KernelAddressInterfaced toClone) {
		super(toClone.id, false);
		original_external_kernel_address = toClone.original_external_kernel_address.clone();
		interfaced = new AtomicBoolean(toClone.interfaced.get());
		initName();
		internalSize+=original_external_kernel_address.getInternalSerializedSize()+1;
	}

	
	@Override
	public void readExternal(SecuredObjectInputStream in) throws IOException, ClassNotFoundException {
		try {
			super.readExternal(in, false);
			original_external_kernel_address=in.readObject(false, KernelAddress.class);
			internalSize+=original_external_kernel_address.getInternalSerializedSize();
			interfaced=new AtomicBoolean(in.readBoolean());
			
			initName();
			
				
		} catch (IOException e) {
			throw e;
		} catch (Exception e) {
			throw new IOException(e);
		}
	}
	@Override
	public void writeExternal(SecuredObjectOutputStream oos) throws IOException {
		super.writeExternal(oos);
		oos.writeObject(original_external_kernel_address, false);
		oos.writeBoolean(interfaced.get());
	}
	/**
	 * 
	 * @param identical_from_original_kernel_interface
	 *            true if the original kernel address do not need to be interfaced.
	 */
	void setInterface(boolean identical_from_original_kernel_interface) {
		if (interfaced.getAndSet(!identical_from_original_kernel_interface) == identical_from_original_kernel_interface)
			initName();
	}

	/**
	 * 
	 * @return true if the original kernel address is interfaced through a
	 *         artificial kernel address
	 */
	public boolean isInterfaced() {
		return interfaced.get();
	}

	/**
	 * 
	 * @return the original kernel address
	 */
	public KernelAddress getOriginalKernelAddress() {
		return original_external_kernel_address;
	}

	@Override
	public int hashCode() {
		return original_external_kernel_address.hashCode();
	}

	@Override
	public AbstractDecentralizedID getAbstractDecentralizedID() {
		if (interfaced.get())
			return super.getAbstractDecentralizedID();
		else
			return original_external_kernel_address.getAbstractDecentralizedID();
	}

	/*
	 * @Override public boolean equals(Object o) { if (o==null) return false; if
	 * (o==this) return true;
	 * 
	 * if (o instanceof KernelAddress) { KernelAddress
	 * ka_to_compare=(KernelAddress)o; return
	 * ka_to_compare.getAbstractDecentralizedID().equals(this.
	 * getAbstractDecentralizedID()); } else return false;
	 * 
	 * }
	 */

	@Override
	public KernelAddressInterfaced clone() {
		return new KernelAddressInterfaced(this);
	}

}
