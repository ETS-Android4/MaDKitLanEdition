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
import com.distrimind.util.crypto.ASymmetricLoginAgreementType;
import com.distrimind.util.crypto.MessageDigestType;
import com.distrimind.util.crypto.P2PLoginAgreementType;

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

	@Override
	void checkProperties() throws AccessException {
		if (this.encryptIdentifiersBeforeSendingToDistantPeer)
		{
			if (identifierDigestionTypeUsedForAnonymization==null)
				throw new AccessException(new NullPointerException("identifierDigestionTypeUsedForAnonymization can't be null !"));
		}
		
	}


	@Override
	public AbstractAccessProtocol getAccessProtocolInstance(InetSocketAddress _distant_inet_address,
			InetSocketAddress _local_interface_address, LoginEventsTrigger loginTrigger, MadkitProperties _properties)
			throws AccessException {
		return new AccessProtocolWithJPake(_distant_inet_address, _local_interface_address, loginTrigger, _properties);
	}

	
	
}
