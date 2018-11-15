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
import com.distrimind.madkit.kernel.network.connection.ConnectionProtocolProperties;
import com.distrimind.util.crypto.*;

/**
 * 
 * 
 * @author Jason Mahdjoub
 * @version 1.1
 * @since MadkitLanEdition 1.0
 */
public class P2PSecuredConnectionProtocolWithASymmetricKeyExchangerProperties extends ConnectionProtocolProperties<P2PSecuredConnectionProtocolWithASymmetricKeyExchanger> {

	/**
	 * 
	 */
	private static final long serialVersionUID = -3844860519386846020L;

	public P2PSecuredConnectionProtocolWithASymmetricKeyExchangerProperties() {
		super(P2PSecuredConnectionProtocolWithASymmetricKeyExchanger.class);
	}

	/**
	 * Tells if the connection must be encrypted or not. If not, only signature
	 * packet will be enabled.
	 */
	public boolean enableEncryption = true;

	/**
	 * The asymetric cipher key size
	 */
	public short aSymetricKeySize = 4096;

	/**
	 * The minimum asymetric cipher RSA Key size
	 */
	public final int minASymetricKeySize = 2048;

	/**
	 * Symmetric encryption algorithm
	 */
	public SymmetricEncryptionType symmetricEncryptionType = SymmetricEncryptionType.DEFAULT;

	/**
	 * The symmetric key size in bits
	 */
	public short symmetricKeySizeBits = symmetricEncryptionType.getDefaultKeySizeBits();

	/**
	 * Asymmetric encryption algorithm
	 */
	public ASymmetricEncryptionType aSymetricEncryptionType = ASymmetricEncryptionType.DEFAULT;

	/**
	 * Signature type
	 */
	public ASymmetricAuthentifiedSignatureType signatureType = aSymetricEncryptionType.getDefaultSignatureAlgorithm();

	/**
	 * ASymmetric key wrapper
	 */
	public ASymmetricKeyWrapperType keyWrapper=ASymmetricKeyWrapperType.DEFAULT;
	
	/**
	 * Default duration of a public key before being regenerated. Must be greater or
	 * equal than 0.
	 */
	public final long defaultASymmetricKeyExpirationMs = 15552000000L;

	/**
	 * The duration of a public key before being regenerated. Must be greater or
	 * equal than 0.
	 */
	public long aSymmetricKeyExpirationMs = defaultASymmetricKeyExpirationMs;

	/**
	 * Tells if the current peer can receive an ask for connection.
	 */
	public boolean isServer = true;

	@Override
	public void checkProperties() throws ConnectionException {
		if (aSymetricKeySize < minASymetricKeySize)
			throw new ConnectionException("_rsa_key_size must be greater or equal than " + minASymetricKeySize
					+ " . Moreover, this number must correspond to this schema : _rsa_key_size=2^x.");
		if (keyWrapper==null)
			throw new ConnectionException(new NullPointerException());
		int tmp = aSymetricKeySize;
		while (tmp != 1) {
			if (tmp % 2 == 0)
				tmp = tmp / 2;
			else
				throw new ConnectionException("The RSA key size have a size of " + aSymetricKeySize
						+ ". This number must correspond to this schema : _rsa_key_size=2^x.");
		}

	}

	@Override
	public boolean needsMadkitLanEditionDatabase() {
		return true;
	}

	@Override
	public boolean isEncrypted() {
		return enableEncryption;
	}

	private transient SymmetricEncryptionAlgorithm maxAlgo=null;

	@Override
	public int getMaximumBodyOutputSizeForEncryption(int size) throws BlockParserException {
		if (!isEncrypted())
			return size;
		else
		{

			try {
				if (maxAlgo==null)
					maxAlgo=new SymmetricEncryptionAlgorithm(SecureRandomType.DEFAULT.getSingleton(null), symmetricEncryptionType.getKeyGenerator(SecureRandomType.DEFAULT.getSingleton(null), symmetricKeySizeBits).generateKey());
				return maxAlgo.getOutputSizeForEncryption(size)+4;
			} catch (Exception e) {
				throw new BlockParserException(e);
			}

		}
	}


    private transient volatile Integer maxHeadSize=null;
    @SuppressWarnings("deprecation")
    @Override
    public int getMaximumSizeHead() {
        if (maxHeadSize==null) {

            maxHeadSize = signatureType.getSignatureSizeBytes(aSymetricKeySize);
        }
        return maxHeadSize;
    }

    @Override
	public boolean needsServerSocketImpl() {
		return isServer;
	}

	@Override
	public boolean canTakeConnectionInitiativeImpl() {
		return true;
	}

	@Override
	public boolean supportBidirectionalConnectionInitiativeImpl() {
		return true;
	}

	@Override
	public boolean canBeServer() {
		return true;
	}

}
