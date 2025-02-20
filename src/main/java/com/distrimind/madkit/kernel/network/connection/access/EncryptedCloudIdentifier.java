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

import com.distrimind.util.crypto.ASymmetricKeyPair;
import com.distrimind.util.crypto.ASymmetricPublicKey;
import com.distrimind.util.crypto.AbstractMessageDigest;
import com.distrimind.util.crypto.AbstractSecureRandom;
import com.distrimind.util.data_buffers.WrappedSecretData;
import com.distrimind.util.io.SecuredObjectInputStream;
import com.distrimind.util.io.SecuredObjectOutputStream;
import com.distrimind.util.io.SerializationTools;

import java.io.IOException;
import java.util.Arrays;

/**
 * Represent a cloud identifier encrypted
 * 
 * @author Jason Mahdjoub
 * @version 1.2
 * @since MadKitLanEdition 1.0
 * @see CloudIdentifier
 */
final class EncryptedCloudIdentifier extends CloudIdentifier {
	public final static int MAX_ENCRYPTED_CLOUD_IDENTIFIER_LENGTH=CloudIdentifier.MAX_CLOUD_IDENTIFIER_LENGTH+512;

	private byte[] bytes;

	@SuppressWarnings("unused")
	EncryptedCloudIdentifier()
	{
		
	}

	EncryptedCloudIdentifier getRandomEncryptedCloudIdentifier(AbstractSecureRandom random)
	{
		EncryptedCloudIdentifier res=new EncryptedCloudIdentifier();
		res.bytes=new byte[bytes.length];
		random.nextBytes(res.bytes);
		return res;
	}

	EncryptedCloudIdentifier(CloudIdentifier cloudIdentifier, AbstractSecureRandom random, AbstractMessageDigest messageDigest, byte[] distantGeneratedSalt)
			throws
			IOException {
		if (cloudIdentifier == null)
			throw new NullPointerException("cloudIdentifier");
		if (random == null)
			throw new NullPointerException("random");
		
		bytes = AccessProtocolWithP2PAgreement.anonymizeIdentifier(cloudIdentifier.getBytesTabToEncode(), random, messageDigest, distantGeneratedSalt);
	}
	

	

	@Override
	public boolean equalsTimeConstant(CloudIdentifier _cloud_identifier) {
		if (_cloud_identifier == null)
			return false;
		if (_cloud_identifier == this)
			return true;
		if (_cloud_identifier instanceof EncryptedCloudIdentifier) {
			EncryptedCloudIdentifier c = (EncryptedCloudIdentifier) _cloud_identifier;
			return WrappedSecretData.constantTimeAreEqual(bytes, c.bytes);
		}
		return false;
	}

	@Override
	public int hashCode() {
		return Arrays.hashCode(bytes);
	}

	@Override
	public byte[] getIdentifierBytes() {
		return bytes;
	}

	/**
	 * Tells if the given cloud identifier corresponds to the current encrypted
	 * cloud identifier, considering the given cipher.
	 *
	 * @param originalCloudIdentifier
	 *            the original cloud identifier
	 * @param messageDigest
	 *            the message digest algorithm
	 * @param localGeneratedSalt
	 * 			  the local generated salt
	 * @return true if the given cloud identifier corresponds to the current
	 *         encrypted cloud identifier, considering the given message digest and the given salt.
	 * @throws IOException
	 *             if a problem occurs
	 */
	public boolean verifyWithLocalCloudIdentifier(CloudIdentifier originalCloudIdentifier,
			AbstractMessageDigest messageDigest, byte[] localGeneratedSalt) throws
			IOException {
		if (originalCloudIdentifier == null)
			throw new NullPointerException("originalCloudIdentifier");
		if (messageDigest == null)
			throw new NullPointerException("messageDigest");
		return AccessProtocolWithP2PAgreement.compareAnonymousIdentifier(originalCloudIdentifier.getBytesTabToEncode(), bytes, messageDigest, localGeneratedSalt);
	}

	@Override
	public byte[] getSaltBytes() {
		return null;
	}

	@Override
	public boolean mustBeAnonymous() {
		return false;
	}

	@Override
	public Identifier.AuthenticationMethod getAuthenticationMethod() {
		return Identifier.AuthenticationMethod.NOT_DEFINED;
	}

	@Override
	public ASymmetricPublicKey getAuthenticationPublicKey() {
		return null;
	}

	@Override
	public ASymmetricKeyPair getAuthenticationKeyPair() {
		return null;
	}

	@Override
	public int getInternalSerializedSize() {
		return SerializationTools.getInternalSize(bytes, MAX_ENCRYPTED_CLOUD_IDENTIFIER_LENGTH);
	}

	@Override
	public void readExternal(final SecuredObjectInputStream in) throws IOException {
		bytes=in.readBytesArray(false, MAX_ENCRYPTED_CLOUD_IDENTIFIER_LENGTH);
	}
	@Override
	public void writeExternal(final SecuredObjectOutputStream oos) throws IOException
	{
		oos.writeBytesArray(bytes, false, MAX_ENCRYPTED_CLOUD_IDENTIFIER_LENGTH);
	}

}
