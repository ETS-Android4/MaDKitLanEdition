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

import com.distrimind.madkit.kernel.network.connection.AskConnection;
import com.distrimind.util.crypto.*;
import com.distrimind.util.io.Integrity;
import com.distrimind.util.io.MessageExternalizationException;
import com.distrimind.util.io.SecuredObjectInputStream;
import com.distrimind.util.io.SecuredObjectOutputStream;

import java.io.IOException;

/**
 * 
 * @author Jason Mahdjoub
 * @version 1.1
 * @since MadkitLanEdition 1.0
 */
class AskClientServerConnection extends AskConnection {

	//private final transient byte[] distantPublicKeyForEncryptionEncoded;
	private WrappedEncryptedSymmetricSecretKey secretKeyForEncryption, secretKeyForSignature;
	//private byte[] signatureOfSecretKeyForEncryption;
	private byte[] randomBytes;

	@SuppressWarnings("unused")
	AskClientServerConnection()
	{
		
	}
	
	@Override
	public void readExternal(SecuredObjectInputStream in) throws IOException, ClassNotFoundException {
		super.readExternal(in);
		byte[] tab=in.readBytesArray(true, MAX_SECRET_KEY_LENGTH);
		secretKeyForEncryption=tab==null?null:new WrappedEncryptedSymmetricSecretKey(tab);
		secretKeyForSignature=new WrappedEncryptedSymmetricSecretKey(in.readBytesArray(false, MAX_SECRET_KEY_LENGTH));
		//signatureOfSecretKeyForEncryption=in.readBytesArray(true, MAX_SIGNATURE_LENGTH);
		if (secretKeyForEncryption!=null && secretKeyForEncryption.getBytes().length == 0)
			throw new MessageExternalizationException(Integrity.FAIL_AND_CANDIDATE_TO_BAN);
		if (secretKeyForSignature.getBytes().length == 0)
			throw new MessageExternalizationException(Integrity.FAIL_AND_CANDIDATE_TO_BAN);
		if (this.isYouAreAsking())
			throw new MessageExternalizationException(Integrity.FAIL_AND_CANDIDATE_TO_BAN);
		randomBytes=in.readBytesArray(true, 256);
		if (secretKeyForEncryption==null && (/*signatureOfSecretKeyForEncryption!=null || */randomBytes!=null))
			throw new MessageExternalizationException(Integrity.FAIL_AND_CANDIDATE_TO_BAN);
		if (secretKeyForEncryption!=null && (/*signatureOfSecretKeyForEncryption==null || */randomBytes==null))
			throw new MessageExternalizationException(Integrity.FAIL_AND_CANDIDATE_TO_BAN);
		if (randomBytes!=null  && randomBytes.length!=256)
			throw new MessageExternalizationException(Integrity.FAIL_AND_CANDIDATE_TO_BAN);
	}


	@Override
	public void writeExternal(SecuredObjectOutputStream oos) throws IOException {
		super.writeExternal(oos);
		oos.writeBytesArray(secretKeyForEncryption==null?null:secretKeyForEncryption.getBytes(), true, MAX_SECRET_KEY_LENGTH);
		oos.writeBytesArray(secretKeyForSignature.getBytes(), false, MAX_SECRET_KEY_LENGTH);
		//oos.writeBytesArray(signatureOfSecretKeyForEncryption, true, MAX_SIGNATURE_LENGTH);
		oos.writeBytesArray(randomBytes, true, 256);
	}
	
	
	AskClientServerConnection(AbstractSecureRandom random, ASymmetricKeyWrapperType keyWrapper, SymmetricSecretKey encryptionSecretKey,SymmetricSecretKey signatureSecretKey,			
			IASymmetricPublicKey distantPublicKeyForEncryption) throws IOException, IllegalStateException
			{
		super(false);
		if (keyWrapper == null)
			throw new NullPointerException("symmetricAlgo");
		if (encryptionSecretKey == null)
			throw new NullPointerException("encryptionSecretKey");
		if (signatureSecretKey == null)
			throw new NullPointerException("signatureSecretKey");
		if (distantPublicKeyForEncryption == null)
			throw new NullPointerException("distantPublicKeyForEncryption");
		KeyWrapperAlgorithm kwe=new KeyWrapperAlgorithm(keyWrapper, distantPublicKeyForEncryption);
		KeyWrapperAlgorithm kws=new KeyWrapperAlgorithm(keyWrapper, distantPublicKeyForEncryption, signatureSecretKey);

		this.secretKeyForEncryption=kws.wrap(random, encryptionSecretKey);
		this.secretKeyForSignature=kwe.wrap(random, signatureSecretKey);
		this.randomBytes=new byte[256];
		random.nextBytes(randomBytes);
	}
	AskClientServerConnection(AbstractSecureRandom random, ASymmetricKeyWrapperType keyWrapper, SymmetricSecretKey signatureSecretKey,			
			IASymmetricPublicKey distantPublicKeyForEncryption) throws IOException, IllegalStateException {
		super(false);
		if (keyWrapper == null)
			throw new NullPointerException("symmetricAlgo");
		if (signatureSecretKey == null)
			throw new NullPointerException("signatureSecretKey");
		if (distantPublicKeyForEncryption == null)
			throw new NullPointerException("distantPublicKeyForEncryption");
		KeyWrapperAlgorithm kws=new KeyWrapperAlgorithm(keyWrapper, distantPublicKeyForEncryption);
		this.secretKeyForEncryption=null;
		this.secretKeyForSignature=kws.wrap(random, signatureSecretKey);
		this.randomBytes=null;
	}

	WrappedEncryptedSymmetricSecretKey getSecretKeyForEncryption() {
		return secretKeyForEncryption;
	}
	WrappedEncryptedSymmetricSecretKey getSecretKeyForSignature() {
		return secretKeyForSignature;
	}
	

	

	@Override
	public void corrupt() {
		WrappedEncryptedSymmetricSecretKey tmp=secretKeyForEncryption;
		secretKeyForEncryption=secretKeyForSignature;
		secretKeyForSignature=tmp;
	}
	
	

}