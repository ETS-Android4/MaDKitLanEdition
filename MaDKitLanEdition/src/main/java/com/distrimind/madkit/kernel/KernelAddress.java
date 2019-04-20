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

import com.distrimind.madkit.exceptions.MessageSerializationException;
import com.distrimind.madkit.kernel.network.SystemMessage.Integrity;
import com.distrimind.madkit.util.SecureExternalizable;
import com.distrimind.madkit.util.SecuredObjectInputStream;
import com.distrimind.madkit.util.SecuredObjectOutputStream;
import com.distrimind.util.AbstractDecentralizedID;
import com.distrimind.util.RenforcedDecentralizedIDGenerator;
import org.apache.commons.codec.binary.Base64;

import java.io.IOException;

/**
 * This class represents a unique identifier for MaDKit kernel. Uniqueness is
 * guaranteed even when different kernels run on the same JVM or over the
 * network.
 * 
 * @author Oliver Gutknecht
 * @author Fabien Michel
 * @author Jason Mahdjoub
 * @version 6.1
 * @since MaDKit 1.0
 * @since MaDKitLanEdition 1.0
 *
 */
public class KernelAddress implements SecureExternalizable, Cloneable {

	protected AbstractDecentralizedID id;

	private transient String name;
	protected transient short internalSize;


	/**
	 * Avoid the default public visibility for denying usage.
	 * 
	 * @param isSecured if the kernel address must be secured
	 */
	protected KernelAddress(boolean isSecured) {
		this(isSecured, true);
	}
	protected KernelAddress()
	{
		
	}
	

	protected KernelAddress(boolean isSecured, boolean initName) {

		//RenforcedDecentralizedIDGenerator generatedid = new RenforcedDecentralizedIDGenerator();
		if (isSecured) {
			id = new RenforcedDecentralizedIDGenerator(false, true);//new SecuredDecentralizedID(generatedid, SecureRandomType.FORTUNA_WITH_BC_FIPS_APPROVED.getInstance(null));
		} else
			id = new RenforcedDecentralizedIDGenerator(false, false);//generatedid;
		internalSize=(short)(id.getBytes().length+1);
		if (initName)
			initName();
		else
			name = null;
	}

	protected void initName() {
		name = getKernelName();
	}

	protected KernelAddress(AbstractDecentralizedID id, boolean initName) {
		if (id == null)
			throw new NullPointerException("id");
		this.id = id;
		internalSize=(short)(id.getBytes().length+1);
		if (initName)
			initName();
		else
			name = null;
	}

	protected static final byte[] tab = new byte[65];
	
	protected void readExternal(SecuredObjectInputStream in, boolean initName) throws IOException
	{
		try {
			internalSize=in.readShort();
			if (internalSize<16 || internalSize>tab.length)
				throw new MessageSerializationException(Integrity.FAIL, "internalSize="+internalSize);
			synchronized(tab)
			{
				int pos=0;
				do
				{
					int v=in.read(tab, pos, internalSize-pos);
					if (v<0)
						throw new MessageSerializationException(Integrity.FAIL_AND_CANDIDATE_TO_BAN);
					pos+=v;
				} while(pos<internalSize);
				try
				{
					id=AbstractDecentralizedID.instanceOf(tab, 0, internalSize);
				}
				catch(Throwable t)
				{
					throw new IOException(t);
				}
			}
			++internalSize;
			
			try {
				if (id.getBytes() == null)
					throw new MessageSerializationException(Integrity.FAIL);
				//noinspection EqualsWithItself
				if (!id.equals(id))
					throw new MessageSerializationException(Integrity.FAIL);
				
			} catch (Exception e) {
				throw new MessageSerializationException(Integrity.FAIL);
			}
			
			if (initName)
				initName();
		} catch (IOException e) {
			throw e;
		} catch (Exception e) {
			throw new IOException(e);
		}
	}
	
	@Override
	public void readExternal(SecuredObjectInputStream in) throws IOException, ClassNotFoundException {
		readExternal(in, true);
	}
	@Override
	public void writeExternal(SecuredObjectOutputStream oos) throws IOException {
		byte[] tab=id.getBytes();
		oos.writeShort(tab.length);
		oos.write(tab);
	}
	private String getKernelName() {
		return "@" + Madkit.getVersion().getShortProgramName() + "-" + getNetworkID();
	}

	@Override
	public boolean equals(Object o) {
		if (o == null)
			return false;
		if (o == this)
			return true;
		if (o instanceof KernelAddress) {
			KernelAddress ka_to_compare = (KernelAddress) o;
			return ka_to_compare.getAbstractDecentralizedID().equals(this.getAbstractDecentralizedID());
		} else
			return false;
	}

	@Override
	public int hashCode() {
		return id.hashCode();
	}

	public String getNetworkID() {
		return getHexString(getAbstractDecentralizedID().getBytes());
	}

	private static String getHexString(byte[] bytes) {
		return Base64.encodeBase64URLSafeString(bytes);
	}

	public AbstractDecentralizedID getAbstractDecentralizedID() {
		return id;
	}

	/**
	 * Returns a simplified string representation for this platform address
	 * 
	 * @return a string representation for this platform address
	 */
	@Override
	public String toString() {
		return name;
	}



	@SuppressWarnings("MethodDoesntCallSuperMethod")
	@Override
	public KernelAddress clone() {
		return this;
	}

	@Override
	public int getInternalSerializedSize() {
		return internalSize;
	}

}
