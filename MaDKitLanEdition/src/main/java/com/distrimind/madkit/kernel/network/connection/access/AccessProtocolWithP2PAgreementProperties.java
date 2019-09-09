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

import java.net.InetSocketAddress;

import com.distrimind.madkit.kernel.MadkitProperties;
import com.distrimind.util.crypto.*;

/**
 * Represents properties of a specific connection protocol
 * 
 * @author Jason Mahdjoub
 * @version 2.0
 * @since MadkitLanEdition 1.2
 *
 */
public class AccessProtocolWithP2PAgreementProperties extends AbstractAccessProtocolProperties{

	/**
	 * 
	 */
	private static final long serialVersionUID = 2781524045639535331L;
	
	/**
	 * Message digest type used for anonymization
	 */
	public MessageDigestType identifierDigestionTypeUsedForAnonymization=MessageDigestType.BC_FIPS_SHA3_512;
	
	
	/**
	 * P2P Login agreement type
	 */
	public P2PLoginAgreementType p2pLoginAgreementType=P2PLoginAgreementType.JPAKE_AND_AGREEMENT_WITH_SYMMETRIC_SIGNATURE;

	/**
	 * Asymmetric login agreement type
	 */
	public ASymmetricLoginAgreementType asymmetricLoginAgreementType= ASymmetricLoginAgreementType.AGREEMENT_WITH_ASYMMETRIC_SIGNATURE;


	/**
	 * The asymetric cipher key size used for
	 * {@link P2PASymmetricSecretMessageExchanger}
	 */
	public short aSymetricKeySize = 4096;

	/**
	 * The minimum asymetric cipher RSA Key size used for
	 * {@link P2PASymmetricSecretMessageExchanger}
	 */
	public final int minASymetricKeySize = 1024;

	/**
	 * Asymmetric encryption algorithm used for
	 * {@link P2PASymmetricSecretMessageExchanger}
	 */
	public ASymmetricEncryptionType aSymetricEncryptionType = ASymmetricEncryptionType.DEFAULT;

	/**
	 * PasswordDigestType used for {@link P2PASymmetricSecretMessageExchanger}
	 */
	public PasswordHashType passwordHashType = PasswordHashType.DEFAULT;

	/**
	 * Password hash cost (iterations=2^passwordHashCost)
	 */
	public byte passwordHashCost = 16;

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





	@Override
	void checkProperties() throws AccessException {
		if (this.anonymizeIdentifiersBeforeSendingToDistantPeer)
		{
			if (identifierDigestionTypeUsedForAnonymization==null)
				throw new AccessException(new NullPointerException("identifierDigestionTypeUsedForAnonymization can't be null !"));
		}
		if (p2pLoginAgreementType==P2PLoginAgreementType.ASYMMETRIC_SECRET_MESSAGE_EXCHANGER
				|| p2pLoginAgreementType==P2PLoginAgreementType.ASYMMETRIC_SECRET_MESSAGE_EXCHANGER_AND_AGREEMENT_WITH_SYMMETRIC_SIGNATURE) {
			if (aSymetricKeySize < minASymetricKeySize)
				throw new AccessException("aSymetricKeySize value must be greter than " + minASymetricKeySize);
			int tmp = aSymetricKeySize;
			while (tmp != 1) {
				if (tmp % 2 == 0)
					tmp = tmp / 2;
				else
					throw new AccessException("The RSA key size have a size of " + aSymetricKeySize
							+ ". This number must correspond to this schema : _rsa_key_size=2^x.");
			}
		}
	}


	@Override
	public AbstractAccessProtocol getAccessProtocolInstance(InetSocketAddress _distant_inet_address,
			InetSocketAddress _local_interface_address, LoginEventsTrigger loginTrigger, MadkitProperties _properties)
			throws AccessException {
		return new AccessProtocolWithP2PAgreement(_distant_inet_address, _local_interface_address, loginTrigger, _properties);
	}

	
	
}
