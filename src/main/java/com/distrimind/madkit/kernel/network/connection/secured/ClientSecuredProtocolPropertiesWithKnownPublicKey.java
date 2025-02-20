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

import com.distrimind.madkit.exceptions.BlockParserException;
import com.distrimind.madkit.exceptions.ConnectionException;
import com.distrimind.madkit.kernel.network.EncryptionRestriction;
import com.distrimind.madkit.kernel.network.connection.ConnectionProtocolProperties;
import com.distrimind.util.crypto.*;
import com.distrimind.util.sizeof.ObjectSizer;

/**
 * 
 * 
 * @author Jason Mahdjoub
 * @version 2.0
 * @since MadkitLanEdition 1.0
 */
public class ClientSecuredProtocolPropertiesWithKnownPublicKey
		extends ConnectionProtocolProperties<ClientSecuredConnectionProtocolWithKnownPublicKey> {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1775803204716519459L;

	public ClientSecuredProtocolPropertiesWithKnownPublicKey() {
		super(ClientSecuredConnectionProtocolWithKnownPublicKey.class);
	}

	/**
	 * Tells if the connection must be encrypted or not. If not, only signature
	 * packet will be enabled.
	 */
	public volatile boolean enableEncryption = true;

	/**
	 * Set the encryption profile according properties given by the server side
	 * properties
	 * 
	 * @param identifier
	 *            the encryption identifier
	 * @param publicKeyForEncryption
	 *            the public key for encryption
	 * @param symmetricEncryptionType
	 *            the symmetric encryption type (if null, use default symmetric
	 *            encryption type and default key size)
	 * @param keyWrapper
	 *            the key wrapper type
	 */
	public void setEncryptionProfile(int identifier, IASymmetricPublicKey publicKeyForEncryption, SymmetricEncryptionType symmetricEncryptionType, ASymmetricKeyWrapperType keyWrapper, MessageDigestType messageDigestType) {
		assert symmetricEncryptionType != null;
		this.setEncryptionProfile(identifier, publicKeyForEncryption, symmetricEncryptionType,
				symmetricEncryptionType.getDefaultKeySizeBits(), keyWrapper, symmetricEncryptionType.getDefaultSignatureAlgorithm(),
				messageDigestType);
	}

	/**
	 * Set the encryption profile according properties given by the server side
	 * properties
	 * 
	 * @param identifier
	 *            the encryption identifier
	 * @param publicKeyForEncryption
	 *            the public key for encryption
	 * @param symmetricEncryptionType
	 *            the symmetric encryption type (if null, use default symmetric
	 *            encryption type and default key size)
	 * @param symmetricKeySizeBits
	 *            the symmetric encryption key size in bits
	 * @param keyWrapper
	 *            the key wrapper type
	 * @param signatureType
	 *            the signature type (if null, use default signature type)
	 */
	public void setEncryptionProfile(int identifier, IASymmetricPublicKey publicKeyForEncryption, SymmetricEncryptionType symmetricEncryptionType,
			short symmetricKeySizeBits, ASymmetricKeyWrapperType keyWrapper, SymmetricAuthenticatedSignatureType signatureType, MessageDigestType messageDigestType) {
		synchronized (this) {
			if (publicKeyForEncryption == null)
				throw new NullPointerException("publicKey");
			int s;
			if (publicKeyForEncryption instanceof HybridASymmetricPublicKey) {
				if (publicKeyForEncryption.getNonPQCPublicKey().getEncryptionAlgorithmType()==null)
					throw new IllegalArgumentException();
				s = publicKeyForEncryption.getNonPQCPublicKey().getKeySizeBits();

			}
			else {
				if (((ASymmetricPublicKey)publicKeyForEncryption).getEncryptionAlgorithmType()==null)
					throw new IllegalArgumentException();
				s = ((ASymmetricPublicKey) publicKeyForEncryption).getKeySizeBits();
			}
			if (s < minASymmetricKeySizeBits)
				throw new IllegalArgumentException("The public key size must be greater than " + minASymmetricKeySizeBits);

			if (signatureType == null)
				signatureType=SymmetricAuthenticatedSignatureType.DEFAULT;
			this.publicKeyForEncryption = publicKeyForEncryption;
			this.signatureType = signatureType;
			keyIdentifier = identifier;
			this.maximumBodyOutputSizeComputer=null;
			this.maxSizeHead=null;
			if (symmetricEncryptionType != null) {
				this.symmetricEncryptionType = symmetricEncryptionType;
				this.SymmetricKeySizeBits = symmetricKeySizeBits;
			} else {
				this.symmetricEncryptionType = SymmetricEncryptionType.DEFAULT;
				this.SymmetricKeySizeBits = this.symmetricEncryptionType.getDefaultKeySizeBits();
			}
			if (keyWrapper == null)
				this.keyWrapper = ASymmetricKeyWrapperType.DEFAULT;
			else
				this.keyWrapper = keyWrapper;
			this.messageDigestType=messageDigestType;
		}
	}

	/**
	 * Set the encryption profile according properties given by the server side
	 * properties
	 * 
	 * @param serverProperties
	 *            the server side properties
	 */
	public void setEncryptionProfile(ServerSecuredProtocolPropertiesWithKnownPublicKey serverProperties) {
		synchronized (this) {
			enableEncryption = serverProperties.enableEncryption;
			setEncryptionProfile(serverProperties.getLastEncryptionProfileIdentifier(),
					serverProperties.getDefaultKeyPairForEncryption().getASymmetricPublicKey(),
					serverProperties.getDefaultSymmetricEncryptionType(),
					serverProperties.getDefaultSymmetricEncryptionKeySizeBits(),
					serverProperties.getDefaultKeyWrapper(),
					serverProperties.getDefaultSignatureType(), serverProperties.getDefaultMessageDigestType());
		}
	}

	/**
	 * Gets the publicKey attached to this connection protocol
	 * 
	 * @return the publicKey attached to this connection protocol
	 */
	public IASymmetricPublicKey getPublicKeyForEncryption() {
		synchronized (this) {
			return publicKeyForEncryption;
		}
	}

	/**
	 * Gest the signature attached to this connection protocol
	 * 
	 * @return the signature attached to this connection protocol
	 */
	public SymmetricAuthenticatedSignatureType getSignatureType() {

		return signatureType;
	}

	/**
	 * Gets the encryption profile attached to this connection protocol
	 * 
	 * @return the encryption profile attached to this connection protocol
	 */
	public int getEncryptionProfileIdentifier() {
		return keyIdentifier;
	}

	/**
	 * Gets the symmetric encryption type attached to this connection protocol
	 * 
	 * @return the symmetric encryption type attached to this connection protocol
	 */
	public SymmetricEncryptionType getSymmetricEncryptionType() {
		return symmetricEncryptionType;
	}

	/**
	 * Gets the symmetric encryption key size in bits type attached to this
	 * connection protocol
	 * 
	 * @return the symmetric encryption key size in bits type attached to this
	 *         connection protocol
	 */
	public short getSymmetricKeySizeBits() {
		return SymmetricKeySizeBits;
	}

	/**
	 * The used public key
	 */
	private IASymmetricPublicKey publicKeyForEncryption;

	/**
	 * Message digest type used to check message validity
	 */
	public MessageDigestType messageDigestType=MessageDigestType.SHA2_384;

	/**
	 * Do not hash message when encryption is an authenticated algorithm, in order to maximise performances
	 */
	public boolean doNotUseMessageDigestWhenEncryptionIsAuthenticated=true;

	/**
	 * key identifier
	 */
	private int keyIdentifier = 0;

	/**
	 * The minimum asymmetric cipher RSA Key size
	 */
	public final int minASymmetricKeySizeBits = 2048;

	/**
	 * Symmetric encryption algorithm
	 */
	private SymmetricEncryptionType symmetricEncryptionType = SymmetricEncryptionType.DEFAULT;

	/**
	 * The symmetric key size in bits
	 */
	private short SymmetricKeySizeBits = symmetricEncryptionType.getDefaultKeySizeBits();

	/**
	 * Signature type
	 */
	public SymmetricAuthenticatedSignatureType signatureType = null;

	/**
	 * Key wrapper
	 */
	public ASymmetricKeyWrapperType keyWrapper=null;



	private void checkPublicKey(IASymmetricPublicKey publicKey) throws ConnectionException
	{
		if (publicKey == null)
			throw new ConnectionException("The public key must defined");
		int s;
		if (publicKeyForEncryption instanceof HybridASymmetricPublicKey) {
			if (publicKeyForEncryption.getNonPQCPublicKey().getEncryptionAlgorithmType()==null)
				throw new IllegalArgumentException();

			s = publicKey.getNonPQCPublicKey().getKeySizeBits();
		}
		else
		{
			if (((ASymmetricPublicKey)publicKeyForEncryption).getEncryptionAlgorithmType()==null)
				throw new IllegalArgumentException();
			s=((ASymmetricPublicKey)publicKey).getKeySizeBits();
		}

		if (s < minASymmetricKeySizeBits)
			throw new ConnectionException("_rsa_key_size must be greater or equal than " + minASymmetricKeySizeBits
					+ " . Moreover, this number must correspond to this schema : _rsa_key_size=2^x.");
		if (publicKey.getTimeExpirationUTC() < System.currentTimeMillis()) {
			throw new ConnectionException("The distant public key has expired !");
		}

		int tmp = s;
		while (tmp != 1) {
			if (tmp % 2 == 0)
				tmp = tmp / 2;
			else
				throw new ConnectionException("The RSA key size have a size of " + s
						+ ". This number must correspond to this schema : _rsa_key_size=2^x.");
		}
		
	}

	@Override
	public void checkProperties() throws ConnectionException {

		checkPublicKey(publicKeyForEncryption);
		if (symmetricEncryptionType==null)
			throw new ConnectionException(new NullPointerException());
		if (signatureType == null)
			signatureType = symmetricEncryptionType.getDefaultSignatureAlgorithm();

	}

	@Override
	public boolean needsMadkitLanEditionDatabase() {
		return false;
	}

	@Override
	public boolean isEncrypted() {
		return enableEncryption;
	}
	private transient MaximumBodyOutputSizeComputer maximumBodyOutputSizeComputer=null;

	@Override
	public int getMaximumBodyOutputSizeForEncryption(int size) throws BlockParserException {
		if (maximumBodyOutputSizeComputer==null)
			maximumBodyOutputSizeComputer=new MaximumBodyOutputSizeComputer(isEncrypted(), symmetricEncryptionType, getSymmetricKeySizeBits(), getSignatureType(), messageDigestType);
		return maximumBodyOutputSizeComputer.getMaximumBodyOutputSizeForEncryption(size);
	}

	private transient Integer maxSizeHead=null;

    @Override
    public int getMaximumHeadSize() {
        if (maxSizeHead==null)
		{
            maxSizeHead=Math.max(ObjectSizer.sizeOf(getEncryptionProfileIdentifier()), EncryptionSignatureHashEncoder.headSize);
		}
        return maxSizeHead;
    }


	@Override
	public boolean isConcernedBy(EncryptionRestriction encryptionRestriction) {
		if (subProtocolProperties!=null && subProtocolProperties.isConcernedBy(encryptionRestriction))
			return true;

		if (encryptionRestriction==EncryptionRestriction.NO_RESTRICTION)
    		return true;
		if (!signatureType.isPostQuantumAlgorithm(this.SymmetricKeySizeBits))
			return false;

		if (enableEncryption && !symmetricEncryptionType.isPostQuantumAlgorithm(this.SymmetricKeySizeBits))
    		return false;

    	if (encryptionRestriction==EncryptionRestriction.HYBRID_ALGORITHMS)
    		return this.publicKeyForEncryption instanceof HybridASymmetricPublicKey && !keyWrapper.isPostQuantumKeyAlgorithm();
		return this.publicKeyForEncryption.isPostQuantumKey() && keyWrapper.isPostQuantumKeyAlgorithm();
	}

	@Override
	public boolean needsServerSocketImpl() {
		return false;
	}

	@Override
	public boolean canTakeConnectionInitiativeImpl() {
		return true;
	}

	@Override
	public boolean supportBidirectionalConnectionInitiativeImpl() {
		return false;
	}

	@Override
	public boolean canBeServer() {
		return false;
	}

}
