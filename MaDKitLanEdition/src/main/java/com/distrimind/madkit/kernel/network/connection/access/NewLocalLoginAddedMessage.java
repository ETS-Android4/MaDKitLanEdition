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

import com.distrimind.madkit.exceptions.MessageSerializationException;
import com.distrimind.madkit.kernel.network.NetworkProperties;
import com.distrimind.madkit.util.SecuredObjectInputStream;
import com.distrimind.madkit.util.SecuredObjectOutputStream;

import java.io.IOException;
import java.util.ArrayList;

/**
 * 
 * @author Jason Mahdjoub
 * @version 1.0
 * @since MadkitLanEdition 1.0
 */
public class NewLocalLoginAddedMessage extends LocalLogingAccessMessage {

	public ArrayList<Identifier> identifiers;
	private transient boolean forceLoginInitiative;

	@SuppressWarnings("unused")
	NewLocalLoginAddedMessage()
	{
		
	}

	public NewLocalLoginAddedMessage(ArrayList<Identifier> _identifiers, boolean forceLoginInitiative) {
		identifiers = _identifiers;
		this.forceLoginInitiative=forceLoginInitiative;
	}

	public NewLocalLoginAddedMessage(ArrayList<Identifier> _identifiers) {
		this(_identifiers, false);
	}

	boolean isForceLoginInitiative() {
		return forceLoginInitiative;
	}

	@Override
	public void readExternal(SecuredObjectInputStream in) throws IOException, ClassNotFoundException {
		super.readExternal(in);
		int size=in.readInt();
		int totalSize=4;
		int globalSize=NetworkProperties.GLOBAL_MAX_SHORT_DATA_SIZE;
		if (size<0 || totalSize+size*4>globalSize)
			throw new MessageSerializationException(Integrity.FAIL_AND_CANDIDATE_TO_BAN);
		identifiers=new ArrayList<>(size);
		for (int i=0;i<size;i++)
		{
			Identifier id=in.readObject(false, Identifier.class);
			totalSize+=id.getInternalSerializedSize();
			if (totalSize>globalSize)
				throw new MessageSerializationException(Integrity.FAIL_AND_CANDIDATE_TO_BAN);
			identifiers.add(id);
		}
	}


	@Override
	public void writeExternal(SecuredObjectOutputStream oos) throws IOException {
		super.writeExternal(oos);
		oos.writeInt(identifiers.size()); 
		for (Identifier id : identifiers)
			oos.writeObject(id, false);

		
		
	}
	

}
