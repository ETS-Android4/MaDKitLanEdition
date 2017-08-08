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
package com.distrimind.madkit.kernel.network.connection.secured;

import com.distrimind.madkit.kernel.network.connection.ConnectionMessage;
import com.distrimind.util.crypto.ASymmetricPublicKey;

/**
 * 
 * @author Jason Mahdjoub
 * @version 1.0
 * @since MadkitLanEdition 1.0
 */
class PublicKeyMessage extends ConnectionMessage {
	/**
	 * 
	 */
	private static final long serialVersionUID = 3516261115139207424L;

	private transient ASymmetricPublicKey public_key = null;
	private byte[] public_key_bytes;
	private transient final byte[] public_key_bytes_distant;

	public PublicKeyMessage(ASymmetricPublicKey _public_key, ASymmetricPublicKey _public_key_distant) {
		public_key = _public_key;
		public_key_bytes = _public_key.encode();
		public_key_bytes_distant = _public_key_distant == null ? null : _public_key_distant.encode();
	}

	public ASymmetricPublicKey getPublicKey() {
		return public_key;
	}

	@Override
	public Integrity checkDataIntegrity() {
		if (public_key_bytes == null)
			return Integrity.FAIL_AND_CANDIDATE_TO_BAN;
		try {
			public_key = ASymmetricPublicKey.decode(public_key_bytes);
			if (public_key == null)
				return Integrity.FAIL_AND_CANDIDATE_TO_BAN;
		} catch (Exception e) {
			return Integrity.FAIL_AND_CANDIDATE_TO_BAN;
		}
		return Integrity.OK;
	}

	@Override
	public void corrupt() {
		if (public_key_bytes_distant != null)
			public_key_bytes = public_key_bytes_distant;
	}

}