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
package com.distrimind.madkit.kernel.network.connection.access;

import com.distrimind.madkit.util.SecureExternalizable;
import com.distrimind.madkit.util.SecuredObjectInputStream;
import com.distrimind.madkit.util.SecuredObjectOutputStream;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;

/**
 * This identifier associates a {@link HostIdentifier} and a
 * {@link CloudIdentifier}. The cloud is associated with a user, or an entity,
 * and with a machine or an instance of the same program.
 * 
 * @author Jason Mahdjoub
 * @version 1.0
 * @since MadKitLanEdition 1.0
 * @see CloudIdentifier
 * @see HostIdentifier
 */
public class Identifier implements SecureExternalizable {

	private CloudIdentifier cloud_identifier;
	private HostIdentifier host_identifier;

	Identifier()
	{
		
	}
	
	public Identifier(CloudIdentifier _cloud_identifier, HostIdentifier _host_identifier) {
		if (_cloud_identifier == null)
			throw new NullPointerException("_cloud_identifier");
		if (_host_identifier == null)
			throw new NullPointerException("_host_identifier");
		cloud_identifier = _cloud_identifier;
		host_identifier = _host_identifier;
	}

	@Override
	public boolean equals(Object o) {
		if (o instanceof Identifier) {
			Identifier id = (Identifier) o;
			return cloud_identifier.equals(id.cloud_identifier) && host_identifier.equals(id.host_identifier);
		}
		return false;
	}

	@Override
	public String toString() {
		return "Identifier["+cloud_identifier.toString()+", "+host_identifier.toString()+"]";
	}

	@Override
	public int hashCode()
	{
		return cloud_identifier.hashCode()^host_identifier.hashCode();
	}
	
	/**
	 * Tells if the given identifier has the same cloud identifier than those of the
	 * current instance.
	 * 
	 * @param _identifier
	 *            the identifier
	 * @return true if the given identifier has the same cloud identifier than those
	 *         of the current instance.
	 * @see CloudIdentifier
	 */
	public boolean equalsCloudIdentifier(Identifier _identifier) {
		return cloud_identifier.equals(_identifier.cloud_identifier);
	}

	/**
	 * Tells if the given identifier has the same host identifier than those of the
	 * current instance.
	 * 
	 * @param _identifier
	 *            the identifier
	 * @return true if the given identifier has the same host identifier than those
	 *         of the current instance.
	 * @see HostIdentifier
	 */
	public boolean equalsHostIdentifier(Identifier _identifier) {
		return host_identifier.equals(_identifier.host_identifier);
	}

	/**
	 * 
	 * @return the cloud identifier
	 * @see CloudIdentifier
	 */
	public CloudIdentifier getCloudIdentifier() {
		return cloud_identifier;
	}

	/**
	 * 
	 * @return the host identifier
	 * @see HostIdentifier
	 */
	public HostIdentifier getHostIdentifier() {
		return host_identifier;
	}

	@Override
	public int getInternalSerializedSize() {
		return cloud_identifier.getInternalSerializedSize()+host_identifier.getInternalSerializedSize();
	}

	@Override
	public void readExternal(final SecuredObjectInputStream in) throws IOException, ClassNotFoundException
	{
		cloud_identifier=in.readObject(false, CloudIdentifier.class);
		host_identifier=in.readObject(false, HostIdentifier.class);
	}
	@Override
	public void writeExternal(final SecuredObjectOutputStream oos) throws IOException
	{
		oos.writeObject(  cloud_identifier, false);
		oos.writeObject(  host_identifier, false);
	}

	public byte[] toBytes() throws IOException {
		try(ByteArrayOutputStream baos=new ByteArrayOutputStream();
			DataOutputStream oos=new DataOutputStream(baos);
			SecuredObjectOutputStream soos=new SecuredObjectOutputStream(oos))
		{
			soos.writeObject(this, false);
			soos.flush();
			return baos.toByteArray();
		}
	}
}
