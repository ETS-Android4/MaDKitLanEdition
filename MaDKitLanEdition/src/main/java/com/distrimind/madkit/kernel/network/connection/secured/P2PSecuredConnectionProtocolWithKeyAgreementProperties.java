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

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.spec.InvalidKeySpecException;

/**
 * 
 * 
 * @author Jason Mahdjoub
 * @version 2.0
 * @since MadkitLanEdition 1.7
 */
public class P2PSecuredConnectionProtocolWithKeyAgreementProperties extends ConnectionProtocolProperties<P2PSecuredConnectionProtocolWithKeyAgreementAlgorithm> {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = -616754777676015639L;

	public P2PSecuredConnectionProtocolWithKeyAgreementProperties() {
		super(P2PSecuredConnectionProtocolWithKeyAgreementAlgorithm.class);
	}

	/**
	 * Tells if the connection must be encrypted or not. If not, only signature
	 * packet will be enabled.
	 */
	public boolean enableEncryption = true;

	/**
	 * Key agreement type
	 */
	public KeyAgreementType keyAgreementType=KeyAgreementType.DEFAULT;
	
	/**
	 * Symmetric encryption algorithm
	 */
	public SymmetricEncryptionType symmetricEncryptionType = SymmetricEncryptionType.DEFAULT;

	/**
	 * Symmetric signature algorithm
	 */
	public SymmetricAuthentifiedSignatureType symmetricSignatureType=SymmetricAuthentifiedSignatureType.DEFAULT;
	
	/**
	 * symmetric key size in bits
	 */
	public short symmetricKeySizeBits=keyAgreementType.getDefaultKeySizeBits();
	
	/**
	 * Tells if the current peer can receive an ask for connection.
	 */
	public boolean isServer = true;


	@Override
	public void checkProperties() throws ConnectionException {

		if (keyAgreementType==null)
			throw new ConnectionException(new NullPointerException("keyAgreementType"));
		if (symmetricEncryptionType==null && enableEncryption)
			throw new ConnectionException(new NullPointerException("symmetricEncryptionType"));
		if (symmetricSignatureType==null)
			throw new ConnectionException(new NullPointerException("symmetricSignatureType"));
		if (keyAgreementType.isPostQuantumAlgorithm() && enableEncryption && !symmetricEncryptionType.isPostQuantumAlgorithm(symmetricKeySizeBits))
			throw new ConnectionException("The key agreement is a post quantum cryptography. However, the given symmetric encryption algorithm associated with the given symmetric key size are not post quantum compatible algorithms.");
		if (keyAgreementType.isPostQuantumAlgorithm() && !symmetricSignatureType.isPostQuantumAlgorithm(symmetricKeySizeBits))
			throw new ConnectionException("The key agreement is a post quantum cryptography. However, the given symmetric signature algorithm associated with the given symmetric signature size are not post quantum compatible algorithms.");
	}

	@Override
	public boolean needsMadkitLanEditionDatabase() {
		return false;
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
    @Override
    public int getMaximumSizeHead() throws BlockParserException {
        if (maxHeadSize==null)
		{
            try {

				SymmetricAuthenticatedSignerAlgorithm signerTmp = new SymmetricAuthenticatedSignerAlgorithm(symmetricSignatureType.getKeyGenerator(SecureRandomType.DEFAULT.getSingleton(null), symmetricEncryptionType.getDefaultKeySizeBits()).generateKey());
                signerTmp.init();
                maxHeadSize = signerTmp.getMacLengthBytes();

            } catch (NoSuchAlgorithmException | NoSuchProviderException | InvalidKeyException | InvalidKeySpecException e) {
                throw new BlockParserException(e);
            }
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
