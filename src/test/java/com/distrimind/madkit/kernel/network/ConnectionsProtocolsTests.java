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
package com.distrimind.madkit.kernel.network;

import com.distrimind.madkit.kernel.TestNGMadkit;
import org.testng.annotations.*;
import org.testng.AssertJUnit;
import com.distrimind.madkit.database.KeysPairs;
import com.distrimind.madkit.exceptions.BlockParserException;
import com.distrimind.madkit.exceptions.ConnectionException;
import com.distrimind.madkit.exceptions.NIOException;
import com.distrimind.madkit.exceptions.PacketException;
import com.distrimind.madkit.kernel.MadkitProperties;
import com.distrimind.madkit.kernel.network.connection.*;
import com.distrimind.madkit.kernel.network.connection.ConnectionProtocol.ConnectionState;
import com.distrimind.madkit.kernel.network.connection.secured.*;
import com.distrimind.madkit.kernel.network.connection.unsecured.CheckSumConnectionProtocolProperties;
import com.distrimind.madkit.kernel.network.connection.unsecured.UnsecuredConnectionProtocolProperties;
import com.distrimind.ood.database.DatabaseConfiguration;
import com.distrimind.ood.database.DatabaseSchema;
import com.distrimind.ood.database.EmbeddedH2DatabaseWrapper;
import com.distrimind.ood.database.InMemoryEmbeddedH2DatabaseFactory;
import com.distrimind.util.crypto.*;
import com.distrimind.util.io.*;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

/**
 * 
 * @author Jason Mahdjoub
 * @version 1.3
 * @since MadkitLanEdition 1.0
 */
public class ConnectionsProtocolsTests extends TestNGMadkit {
	private static final EmbeddedH2DatabaseWrapper sql_connection_asker;
	private static final EmbeddedH2DatabaseWrapper sql_connection_recveiver;
	private static final File dbasker = new File("dbasker.database");
	private static final File dbreceiver = new File("dbreceiver.database");

	static {
		EmbeddedH2DatabaseWrapper asker = null;
		EmbeddedH2DatabaseWrapper receiver = null;
		try {

			if (dbasker.exists())
				EmbeddedH2DatabaseWrapper.deleteDatabasesFiles(dbasker);
			if (dbreceiver.exists())
				EmbeddedH2DatabaseWrapper.deleteDatabasesFiles(dbreceiver);
			asker = new InMemoryEmbeddedH2DatabaseFactory(dbasker.getName()).getDatabaseWrapperSingleton();
			receiver = new InMemoryEmbeddedH2DatabaseFactory(dbreceiver.getName()).getDatabaseWrapperSingleton();
			asker.getDatabaseConfigurationsBuilder()
					.addConfiguration(new DatabaseConfiguration(new DatabaseSchema(KeysPairs.class.getPackage())), true, true )
					.commit();
			receiver.getDatabaseConfigurationsBuilder()
					.addConfiguration(new DatabaseConfiguration(new DatabaseSchema(KeysPairs.class.getPackage())), true, true )
					.commit();
		} catch (Exception e) {
			e.printStackTrace();
		}
		sql_connection_asker = asker;
		sql_connection_recveiver = receiver;
	}

	@AfterSuite
	public void removeDatabase() {
		sql_connection_asker.close();
		sql_connection_recveiver.close();
		EmbeddedH2DatabaseWrapper.deleteDatabasesFiles(dbasker);
		EmbeddedH2DatabaseWrapper.deleteDatabasesFiles(dbreceiver);
	}

	private static final int numberMaxExchange = 100;
	private ConnectionProtocol<?> cpasker;
	private ConnectionProtocol<?> cpreceiver;
	private final NetworkProperties npasker;
	private final NetworkProperties npreceiver;
    private final MadkitProperties mkPropertiesAsker;
    private final MadkitProperties mkPropertiesReceiver;
	private final static AbstractSecureRandom rand ;
	static
	{
		AbstractSecureRandom r=null;
		try {
			r=SecureRandomType.DEFAULT.getInstance(null);
		} catch (NoSuchAlgorithmException | NoSuchProviderException e) {
			e.printStackTrace();
			System.exit(-1);
		}
		rand=r;
	}

	private static ArrayList<ConnectionProtocolProperties<?>[]> dataWithSubLevel()
			throws SecurityException, IllegalArgumentException, NoSuchAlgorithmException, NoSuchProviderException, ConnectionException, IOException {
		ArrayList<ConnectionProtocolProperties<?>[]> l=dataOneLevel();
		ArrayList<ConnectionProtocolProperties<?>[]> res = new ArrayList<>(l);
		@SuppressWarnings("unchecked")
		ArrayList<ConnectionProtocolProperties<?>[]>[] firstLevel = new ArrayList[l.size()];
		firstLevel[0]=l;
		for (int i = 1;i<firstLevel.length;i++)
			firstLevel[i]=dataOneLevel();
		for (int i = 0; i < l.size(); i++) {
			ArrayList<ConnectionProtocolProperties<?>[]> subLevel = dataOneLevel();

			for (int j = 0; j < subLevel.size(); j++) {
				ConnectionProtocolProperties<?>[] base = firstLevel[j].get(i);
				ConnectionProtocolProperties<?>[] sub = subLevel.get(j);
				base[0].subProtocolProperties = sub[0];
				base[1].subProtocolProperties = sub[1];
				res.add(base);
			}

		}
		return res;
	}

	private static ArrayList<ConnectionProtocolProperties<?>[]> dataOneLevel()
			throws SecurityException, IllegalArgumentException, NoSuchAlgorithmException, NoSuchProviderException, ConnectionException, IOException {
		ArrayList<ConnectionProtocolProperties<?>[]> res = new ArrayList<>();

		ConnectionProtocolProperties<?>[] o = new ConnectionProtocolProperties<?>[2];
		P2PSecuredConnectionProtocolWithASymmetricKeyExchangerProperties p2pp = new P2PSecuredConnectionProtocolWithASymmetricKeyExchangerProperties();
		p2pp.aSymmetricKeySize = 2048;
		p2pp.symmetricEncryptionType = SymmetricEncryptionType.AES_GCM;
		p2pp.enableEncryption=true;
		o[0] = p2pp;
		p2pp = new P2PSecuredConnectionProtocolWithASymmetricKeyExchangerProperties();
		p2pp.aSymmetricKeySize = 2048;
		p2pp.enableEncryption=true;
		p2pp.symmetricEncryptionType = SymmetricEncryptionType.AES_GCM;
		o[1] = p2pp;
		res.add(o);
		
		o = new ConnectionProtocolProperties<?>[2];
		p2pp = new P2PSecuredConnectionProtocolWithASymmetricKeyExchangerProperties();
		p2pp.aSymmetricKeySize = 2048;
		p2pp.symmetricEncryptionType = SymmetricEncryptionType.AES_CBC_PKCS5Padding;
		p2pp.enableEncryption=true;
		o[0] = p2pp;
		p2pp = new P2PSecuredConnectionProtocolWithASymmetricKeyExchangerProperties();
		p2pp.aSymmetricKeySize = 2048;
		p2pp.symmetricEncryptionType = SymmetricEncryptionType.AES_CBC_PKCS5Padding;
		p2pp.enableEncryption=true;
		o[1] = p2pp;
		res.add(o);
		
		

		o = new ConnectionProtocolProperties<?>[2];
		p2pp = new P2PSecuredConnectionProtocolWithASymmetricKeyExchangerProperties();
		p2pp.aSymmetricKeySize = 2048;
		p2pp.enableEncryption = false;
		p2pp.symmetricEncryptionType = SymmetricEncryptionType.DEFAULT;
		o[0] = p2pp;
		p2pp = new P2PSecuredConnectionProtocolWithASymmetricKeyExchangerProperties();
		p2pp.aSymmetricKeySize = 2048;
		p2pp.enableEncryption = false;
		p2pp.symmetricEncryptionType = SymmetricEncryptionType.DEFAULT;
		o[1] = p2pp;
		res.add(o);

		o = new ConnectionProtocolProperties<?>[2];
		P2PSecuredConnectionProtocolPropertiesWithKeyAgreement p2pp_ecdh = new P2PSecuredConnectionProtocolPropertiesWithKeyAgreement();
		p2pp_ecdh.symmetricEncryptionType = SymmetricEncryptionType.AES_CBC_PKCS5Padding;
		p2pp_ecdh.keyAgreementType=KeyAgreementType.BC_FIPS_XDH_X448_WITH_SHA512CKDF;
		p2pp_ecdh.enableEncryption=true;
		o[0] = p2pp_ecdh;
		p2pp_ecdh = new P2PSecuredConnectionProtocolPropertiesWithKeyAgreement();
		p2pp_ecdh.symmetricEncryptionType = SymmetricEncryptionType.AES_CBC_PKCS5Padding;
		p2pp_ecdh.keyAgreementType=KeyAgreementType.BC_FIPS_XDH_X448_WITH_SHA512CKDF;
		p2pp_ecdh.enableEncryption=true;
		o[1] = p2pp_ecdh;
		res.add(o);

		o = new ConnectionProtocolProperties<?>[2];
		AbstractKeyPair<?, ?> keyPairForSignature=new HybridASymmetricAuthenticatedSignatureType(ASymmetricAuthenticatedSignatureType.BC_FIPS_Ed25519, ASymmetricAuthenticatedSignatureType.BCPQC_SPHINCS256_SHA3_512).generateKeyPair(rand);
		p2pp_ecdh = new P2PSecuredConnectionProtocolPropertiesWithKeyAgreement();
		p2pp_ecdh.symmetricEncryptionType = SymmetricEncryptionType.AES_GCM;
		p2pp_ecdh.keyAgreementType=KeyAgreementType.BC_FIPS_XDH_X448_WITH_SHA512CKDF;
		p2pp_ecdh.enableEncryption=true;
		int profile=p2pp_ecdh.addServerSideProfile(keyPairForSignature );
		o[0] = p2pp_ecdh;
		p2pp_ecdh = new P2PSecuredConnectionProtocolPropertiesWithKeyAgreement();
		p2pp_ecdh.symmetricEncryptionType = SymmetricEncryptionType.AES_GCM;
		p2pp_ecdh.keyAgreementType=KeyAgreementType.BC_FIPS_XDH_X448_WITH_SHA512CKDF;
		p2pp_ecdh.enableEncryption=true;
		p2pp_ecdh.setClientSideProfile(profile, keyPairForSignature.getASymmetricPublicKey());
		o[1] = p2pp_ecdh;
		res.add(o);


		o = new ConnectionProtocolProperties<?>[2];
		p2pp_ecdh = new P2PSecuredConnectionProtocolPropertiesWithKeyAgreement();
		p2pp_ecdh.symmetricEncryptionType = SymmetricEncryptionType.DEFAULT;
		p2pp_ecdh.keyAgreementType=KeyAgreementType.BCPQC_NEW_HOPE;
		p2pp_ecdh.postQuantumKeyAgreement=null;
		p2pp_ecdh.enableEncryption=true;
		p2pp_ecdh.symmetricKeySizeBits=256;
		o[0] = p2pp_ecdh;
		p2pp_ecdh = new P2PSecuredConnectionProtocolPropertiesWithKeyAgreement();
		p2pp_ecdh.symmetricEncryptionType = SymmetricEncryptionType.DEFAULT;
		p2pp_ecdh.keyAgreementType=KeyAgreementType.BCPQC_NEW_HOPE;
		p2pp_ecdh.postQuantumKeyAgreement=null;
		p2pp_ecdh.enableEncryption=true;
		p2pp_ecdh.symmetricKeySizeBits=256;
		o[1] = p2pp_ecdh;
		res.add(o);

		o = new ConnectionProtocolProperties<?>[2];
		p2pp_ecdh = new P2PSecuredConnectionProtocolPropertiesWithKeyAgreement();
		p2pp_ecdh.symmetricEncryptionType = SymmetricEncryptionType.DEFAULT;
		p2pp_ecdh.keyAgreementType=KeyAgreementType.BCPQC_NEW_HOPE;
		p2pp_ecdh.postQuantumKeyAgreement=null;
		p2pp_ecdh.enableEncryption=false;
		profile=p2pp_ecdh.addServerSideProfile(keyPairForSignature );
		o[0] = p2pp_ecdh;
		p2pp_ecdh = new P2PSecuredConnectionProtocolPropertiesWithKeyAgreement();
		p2pp_ecdh.symmetricEncryptionType = SymmetricEncryptionType.DEFAULT;
		p2pp_ecdh.keyAgreementType=KeyAgreementType.BCPQC_NEW_HOPE;
		p2pp_ecdh.postQuantumKeyAgreement=null;
		p2pp_ecdh.enableEncryption=false;
		p2pp_ecdh.setClientSideProfile(profile, keyPairForSignature.getASymmetricPublicKey());
		o[1] = p2pp_ecdh;
		res.add(o);

		SymmetricSecretKey secretKeyForEncryption=SymmetricEncryptionType.AES_CTR.getKeyGenerator(rand).generateKey();
		SymmetricSecretKey secretKeyForSignature=SymmetricAuthenticatedSignatureType.BC_FIPS_HMAC_SHA2_256.getKeyGenerator(rand).generateKey();
		SymmetricSecretKey secretKeyForEncryption2=SymmetricEncryptionType.AES_CTR.getKeyGenerator(rand).generateKey();
		SymmetricSecretKey secretKeyForSignature2=SymmetricAuthenticatedSignatureType.BC_FIPS_HMAC_SHA2_256.getKeyGenerator(rand).generateKey();


		o = new ConnectionProtocolProperties<?>[2];
		P2PSecuredConnectionProtocolWithKnownSymmetricKeysProperties p2psym=new P2PSecuredConnectionProtocolWithKnownSymmetricKeysProperties();
		p2psym.enableEncryption=true;
		p2psym.addProfile(1, secretKeyForEncryption, secretKeyForSignature, MessageDigestType.SHA2_384);
		p2psym.addProfile(0, secretKeyForEncryption2, secretKeyForSignature2, MessageDigestType.SHA2_384);
		p2psym.setDefaultProfileIdentifier(1);
		o[0] = p2psym;
		p2psym = new P2PSecuredConnectionProtocolWithKnownSymmetricKeysProperties();
		p2psym.enableEncryption=true;
		p2psym.addProfile(1, secretKeyForEncryption, secretKeyForSignature, MessageDigestType.SHA2_384);
		p2psym.addProfile(0, secretKeyForEncryption2, secretKeyForSignature2, MessageDigestType.SHA2_384);
		p2psym.setDefaultProfileIdentifier(1);
		o[1] = p2psym;
		res.add(o);

		o = new ConnectionProtocolProperties<?>[2];
		p2psym=new P2PSecuredConnectionProtocolWithKnownSymmetricKeysProperties();
		p2psym.enableEncryption=false;
		p2psym.addProfile(1, secretKeyForEncryption, secretKeyForSignature, MessageDigestType.SHA2_384);
		p2psym.addProfile(0, secretKeyForEncryption2, secretKeyForSignature2, MessageDigestType.SHA2_384);
		p2psym.setDefaultProfileIdentifier(1);
		o[0] = p2psym;
		p2psym = new P2PSecuredConnectionProtocolWithKnownSymmetricKeysProperties();
		p2psym.enableEncryption=false;
		p2psym.addProfile(1, secretKeyForEncryption, secretKeyForSignature, MessageDigestType.SHA2_384);
		p2psym.addProfile(0, secretKeyForEncryption2, secretKeyForSignature2, MessageDigestType.SHA2_384);
		p2psym.setDefaultProfileIdentifier(1);
		o[1] = p2psym;
		res.add(o);

		o = new ConnectionProtocolProperties<?>[2];
		ConnectionProtocolNegotiatorProperties cpnp=new ConnectionProtocolNegotiatorProperties();
		p2pp_ecdh=new P2PSecuredConnectionProtocolPropertiesWithKeyAgreement();
		p2pp_ecdh.symmetricEncryptionType = SymmetricEncryptionType.DEFAULT;
		p2pp_ecdh.keyAgreementType=KeyAgreementType.BCPQC_NEW_HOPE;
		p2pp_ecdh.postQuantumKeyAgreement=null;
		p2pp_ecdh.enableEncryption=true;
		p2pp_ecdh.symmetricKeySizeBits=256;

		cpnp.addConnectionProtocol(p2pp_ecdh, 1);
		p2pp_ecdh = new P2PSecuredConnectionProtocolPropertiesWithKeyAgreement();
		p2pp_ecdh.symmetricEncryptionType = SymmetricEncryptionType.DEFAULT;
		p2pp_ecdh.keyAgreementType=KeyAgreementType.BC_FIPS_XDH_X25519_WITH_SHA384CKDF;
		p2pp_ecdh.enableEncryption=true;
		cpnp.addConnectionProtocol(p2pp_ecdh, 0);
		o[0] = cpnp;
		cpnp=new ConnectionProtocolNegotiatorProperties();
		p2pp_ecdh=new P2PSecuredConnectionProtocolPropertiesWithKeyAgreement();
		p2pp_ecdh.symmetricEncryptionType = SymmetricEncryptionType.DEFAULT;
		p2pp_ecdh.keyAgreementType=KeyAgreementType.BCPQC_NEW_HOPE;
		p2pp_ecdh.postQuantumKeyAgreement=null;
		p2pp_ecdh.enableEncryption=true;
		p2pp_ecdh.symmetricKeySizeBits=256;

		cpnp.addConnectionProtocol(p2pp_ecdh, 1);
		p2pp_ecdh = new P2PSecuredConnectionProtocolPropertiesWithKeyAgreement();
		p2pp_ecdh.symmetricEncryptionType = SymmetricEncryptionType.DEFAULT;
		p2pp_ecdh.keyAgreementType=KeyAgreementType.BC_FIPS_XDH_X25519_WITH_SHA384CKDF;
		p2pp_ecdh.enableEncryption=true;
		cpnp.addConnectionProtocol(p2pp_ecdh, 0);
		o[1] = cpnp;
		res.add(o);

		o = new ConnectionProtocolProperties<?>[2];
		cpnp=new ConnectionProtocolNegotiatorProperties();
		p2pp_ecdh=new P2PSecuredConnectionProtocolPropertiesWithKeyAgreement();
		p2pp_ecdh.symmetricEncryptionType = SymmetricEncryptionType.DEFAULT;
		p2pp_ecdh.keyAgreementType=KeyAgreementType.BCPQC_NEW_HOPE;
		p2pp_ecdh.postQuantumKeyAgreement=null;
		p2pp_ecdh.enableEncryption=true;
		p2pp_ecdh.symmetricKeySizeBits=256;

		cpnp.addConnectionProtocol(p2pp_ecdh, 0);
		p2pp_ecdh = new P2PSecuredConnectionProtocolPropertiesWithKeyAgreement();
		p2pp_ecdh.symmetricEncryptionType = SymmetricEncryptionType.DEFAULT;
		p2pp_ecdh.keyAgreementType=KeyAgreementType.BC_FIPS_XDH_X25519_WITH_SHA512CKDF;
		p2pp_ecdh.enableEncryption=true;
		cpnp.addConnectionProtocol(p2pp_ecdh, 1);
		o[0] = cpnp;
		cpnp=new ConnectionProtocolNegotiatorProperties();
		p2pp_ecdh=new P2PSecuredConnectionProtocolPropertiesWithKeyAgreement();
		p2pp_ecdh.symmetricEncryptionType = SymmetricEncryptionType.DEFAULT;
		p2pp_ecdh.keyAgreementType=KeyAgreementType.BCPQC_NEW_HOPE;
		p2pp_ecdh.postQuantumKeyAgreement=null;
		p2pp_ecdh.enableEncryption=true;
		p2pp_ecdh.symmetricKeySizeBits=256;

		cpnp.addConnectionProtocol(p2pp_ecdh, 0);
		p2pp_ecdh = new P2PSecuredConnectionProtocolPropertiesWithKeyAgreement();
		p2pp_ecdh.symmetricEncryptionType = SymmetricEncryptionType.DEFAULT;
		p2pp_ecdh.keyAgreementType=KeyAgreementType.BC_FIPS_XDH_X25519_WITH_SHA512CKDF;
		p2pp_ecdh.enableEncryption=true;
		cpnp.addConnectionProtocol(p2pp_ecdh, 1);
		o[1] = cpnp;
		res.add(o);

		o = new ConnectionProtocolProperties<?>[2];
		ServerSecuredProtocolPropertiesWithKnownPublicKey sp = new ServerSecuredProtocolPropertiesWithKnownPublicKey();
		ClientSecuredProtocolPropertiesWithKnownPublicKey cp = new ClientSecuredProtocolPropertiesWithKnownPublicKey();
		ASymmetricKeyPair kpe = ASymmetricEncryptionType.DEFAULT
				.getKeyPairGenerator(SecureRandomType.DEFAULT.getSingleton(null), (short) 2048).generateKeyPair();
		sp.addEncryptionProfile(kpe, SymmetricEncryptionType.AES_CBC_PKCS5Padding, ASymmetricKeyWrapperType.DEFAULT, MessageDigestType.DEFAULT);
		cp.setEncryptionProfile(sp);
		sp.enableEncryption = true;
		cp.enableEncryption = true;
		o[0] = cp;
		o[1] = sp;
		res.add(o);

		
		o = new ConnectionProtocolProperties<?>[2];
		sp = new ServerSecuredProtocolPropertiesWithKnownPublicKey();
		cp = new ClientSecuredProtocolPropertiesWithKnownPublicKey();
		HybridASymmetricKeyPair kpepqc=new HybridASymmetricEncryptionType(ASymmetricEncryptionType.RSA_OAEPWithSHA384AndMGF1Padding, ASymmetricEncryptionType.BCPQC_MCELIECE_FUJISAKI_CCA2_SHA256).generateKeyPair(SecureRandomType.DEFAULT.getSingleton(null), 2048);
		sp.addEncryptionProfile(kpepqc, SymmetricEncryptionType.AES_GCM, ASymmetricKeyWrapperType.HYBRID_BCPQC_MCELIECE_FUJISAKI_CCA2_SHA256_AND_RSA_OAEP_WITH_SHA2_384, MessageDigestType.DEFAULT);
		cp.setEncryptionProfile(sp);
		sp.enableEncryption = true;
		cp.enableEncryption = true;
		o[0] = cp;
		o[1] = sp;
		res.add(o);

		o = new ConnectionProtocolProperties<?>[2];
		sp = new ServerSecuredProtocolPropertiesWithKnownPublicKey();
		cp = new ClientSecuredProtocolPropertiesWithKnownPublicKey();
		sp.addEncryptionProfile(kpe, SymmetricEncryptionType.DEFAULT, ASymmetricKeyWrapperType.DEFAULT, MessageDigestType.DEFAULT);
		cp.setEncryptionProfile(sp);
		sp.enableEncryption = false;
		cp.enableEncryption = false;
		o[0] = cp;
		o[1] = sp;
		res.add(o);

		o = new ConnectionProtocolProperties<?>[2];
		sp = new ServerSecuredProtocolPropertiesWithKnownPublicKey();
		cp = new ClientSecuredProtocolPropertiesWithKnownPublicKey();
		sp.addEncryptionProfile(kpepqc, SymmetricEncryptionType.DEFAULT, ASymmetricKeyWrapperType.HYBRID_BCPQC_MCELIECE_FUJISAKI_CCA2_SHA256_AND_RSA_OAEP_WITH_SHA2_384, MessageDigestType.DEFAULT);
		cp.setEncryptionProfile(sp);
		sp.enableEncryption = false;
		cp.enableEncryption = false;
		o[0] = cp;
		o[1] = sp;
		res.add(o);


		o = new ConnectionProtocolProperties<?>[2];
		UnsecuredConnectionProtocolProperties ucp = new UnsecuredConnectionProtocolProperties();
		o[0] = ucp;
		ucp = new UnsecuredConnectionProtocolProperties();
		o[1] = ucp;
		res.add(o);

		o = new ConnectionProtocolProperties<?>[2];
		CheckSumConnectionProtocolProperties cs = new CheckSumConnectionProtocolProperties();
		o[0] = cs;
		cs = new CheckSumConnectionProtocolProperties();
		o[1] = cs;
		res.add(o);


		return res;
	}

	public static Collection<Object[]> data(boolean enableDatabase) throws SecurityException, IllegalArgumentException,
			IOException, NoSuchAlgorithmException, NIOException, NoSuchProviderException, ConnectionException {
		ArrayList<ConnectionProtocolProperties<?>[]> data = dataOneLevel();
		data.addAll(dataWithSubLevel());
		Collection<Object[]> res = new ArrayList<>();

		for (ConnectionProtocolProperties<?>[] base : data) {
			Object[] o = new Object[4];

            MadkitProperties madkitProperties=new MadkitProperties();
			madkitProperties.networkProperties= new NetworkProperties();
            madkitProperties.networkProperties.addConnectionProtocol(base[0]);
			o[0] = getConnectionProtocolInstance(madkitProperties, enableDatabase ? 1 : 0);
			o[1] = madkitProperties;
            madkitProperties=new MadkitProperties();
            madkitProperties.networkProperties= new NetworkProperties();

            madkitProperties.networkProperties.addConnectionProtocol(base[1]);
			o[2] = getConnectionProtocolInstance(madkitProperties, enableDatabase ? 2 : 0);
			o[3] = madkitProperties;
			res.add(o);
		}
		return res;
	}

	@DataProvider(parallel = true)
	public static Object[][] data() throws SecurityException, IllegalArgumentException, IOException,
			NoSuchAlgorithmException, NIOException, NoSuchProviderException, ConnectionException {
		Collection<Object[]> res = data(false);
		res.addAll(data(true));
		return res.toArray(new Object[res.size()][res.iterator().next().length]);
	}

	public static ConnectionProtocol<?> getConnectionProtocolInstance(MadkitProperties mkProperties, int database)
			throws NIOException, UnknownHostException, IllegalArgumentException {
		return mkProperties.networkProperties.getConnectionProtocolInstance(new InetSocketAddress(InetAddress.getByName("56.41.158.221"), 5000),
				new InetSocketAddress(InetAddress.getByName("192.168.0.55"), 5000),
				database == 1 ? sql_connection_asker : database == 2 ? sql_connection_recveiver : null, mkProperties,false, false);
		// ConnectionProtocolProperties<?> cpp=np.getConnectionProtocolProperties(new
		// InetSocketAddress(InetAddress.getByName("56.41.158.221"), 5000), new
		// InetSocketAddress(InetAddress.getByName("192.168.0.55"), 5000));
		// return cpp.getConnectionProtocolInstance(, , 5000), null, np);
	}

	@Factory(dataProvider = "data")
	public ConnectionsProtocolsTests(ConnectionProtocol<?> cpasker, MadkitProperties mkPropertiesAsker,
			ConnectionProtocol<?> cpreceiver, MadkitProperties mkPropertiesReceiver) {
		this.cpasker = cpasker;
		this.cpreceiver = cpreceiver;
		this.mkPropertiesAsker=mkPropertiesAsker;
        this.mkPropertiesReceiver=mkPropertiesReceiver;
        this.npasker=this.mkPropertiesAsker.networkProperties;
        this.npreceiver=this.mkPropertiesReceiver.networkProperties;

	}

	@Test
	public void testRegularConnection()
			throws ConnectionException, PacketException, NIOException, IOException, ClassNotFoundException,
			BlockParserException, IllegalArgumentException, NoSuchAlgorithmException, NoSuchProviderException {
		System.out.println("Testing connection protocol with " + cpasker.getClass() + " for asker and "
				+ cpreceiver.getClass() + " for receiver (crypted=" + cpasker.isCrypted() + ")");
		if (cpasker.getSubProtocol() != null)
			System.out.println("\tSub connection protocol with " + cpasker.getSubProtocol().getClass()
					+ " for asker and " + cpreceiver.getSubProtocol().getClass() + " for receiver (crypted="
					+ cpasker.getSubProtocol().isCrypted() + ")");
		/*this.cpasker.setCounterSelector(new CounterSelector(this.cpasker));
		this.cpreceiver.setCounterSelector(new CounterSelector(this.cpreceiver));*/
		TransferedBlockChecker 	tbcasker = this.cpasker.getTransferredBlockChecker();
		if (tbcasker.isCompletelyInoperant())
		{
			tbcasker=new PointToPointTransferedBlockChecker();
			this.cpasker.setPointToPointTransferedBlockChecker((PointToPointTransferedBlockChecker)tbcasker);
		}
		else
		{
			this.cpasker.setPointToPointTransferedBlockChecker(null);
			tbcasker = (TransferedBlockChecker) unserialize(serialize(tbcasker));
		}

		TransferedBlockChecker tbreceiver = this.cpreceiver.getTransferredBlockChecker();
		if (tbreceiver.isCompletelyInoperant())
		{
			tbreceiver=new PointToPointTransferedBlockChecker();
			this.cpreceiver.setPointToPointTransferedBlockChecker((PointToPointTransferedBlockChecker)tbreceiver);
		}
		else
		{
			this.cpreceiver.setPointToPointTransferedBlockChecker(null);
			tbreceiver = (TransferedBlockChecker) unserialize(serialize(tbreceiver));
		}
		
	
		Iterator<ConnectionProtocol<?>> itasker = this.cpasker.reverseIterator();
		Iterator<ConnectionProtocol<?>> itreceiver = this.cpreceiver.reverseIterator();
		int totalCycles = 0;

		while (itasker.hasNext()) {
			ConnectionProtocol<?> cpasker = itasker.next();
			AssertJUnit.assertTrue(itreceiver.hasNext());
			ConnectionProtocol<?> cpreceiver = itreceiver.next();
			AssertJUnit.assertFalse(cpasker.isConnectionEstablished());
			AssertJUnit.assertFalse(cpreceiver.isConnectionEstablished());
			AssertJUnit.assertEquals(ConnectionState.NOT_CONNECTED, cpasker.getConnectionState());
			AssertJUnit.assertEquals(ConnectionState.NOT_CONNECTED, cpreceiver.getConnectionState());

			ConnectionMessage masker = cpasker.setAndGetNextMessage(new AskConnection(true));

			if (this.cpasker.isTransferBlockCheckerChanged())
			{
				tbcasker = this.cpasker.getTransferredBlockChecker();
				if (tbcasker.isCompletelyInoperant())
				{
					tbcasker=new PointToPointTransferedBlockChecker();
					this.cpasker.setPointToPointTransferedBlockChecker((PointToPointTransferedBlockChecker)tbcasker);
				}
				else
				{
					this.cpasker.setPointToPointTransferedBlockChecker(null);
					tbcasker = (TransferedBlockChecker) unserialize(serialize(tbcasker));
				}
				
			}
			// testRandomPingPongMessage();
			ConnectionMessage mreceiver;
			int cycles = 0;
			try {
				do {
					byte[] message = serialize(masker);
					assert masker != null;
					boolean excludeFromEncryption=masker.excludedFromEncryption();
					masker = (ConnectionMessage) unserialize(getMessage(message,
							getBytesToSend(getBlocks(message,excludeFromEncryption, this.cpasker, npasker, 2, -1, null)), this.cpreceiver,
							npreceiver, 2, -1, null));
					
					//Assert.assertFalse(cpreceiver.isConnectionEstablished());
					mreceiver = cpreceiver.setAndGetNextMessage(masker);
					if (this.cpreceiver.isTransferBlockCheckerChanged())
					{
						tbreceiver = this.cpreceiver.getTransferredBlockChecker();
						if (tbreceiver.isCompletelyInoperant())
						{
							tbreceiver=new PointToPointTransferedBlockChecker();
							this.cpreceiver.setPointToPointTransferedBlockChecker((PointToPointTransferedBlockChecker)tbreceiver);
						}
						else
						{
							this.cpreceiver.setPointToPointTransferedBlockChecker(null);
							tbreceiver = (TransferedBlockChecker) unserialize(serialize(tbreceiver));
							
						}
						
					}

					if (mreceiver == null) {
						masker = null;
						break;
					}
					message = serialize(mreceiver);
					excludeFromEncryption=mreceiver.excludedFromEncryption();
					try
					{
						mreceiver = (ConnectionMessage) unserialize(getMessage(message,
							getBytesToSend(getBlocks(message,excludeFromEncryption, this.cpreceiver, npreceiver, 2, -1, null)),
							this.cpasker, npasker, 2, -1, null));
					}
					catch(IOException e)
					{
						System.err.println(mreceiver.getClass().getName());
						throw e;
					}
					
					AssertJUnit.assertFalse(cpasker.isConnectionEstablished());
					masker = cpasker.setAndGetNextMessage(mreceiver);
					if (this.cpasker.isTransferBlockCheckerChanged())
					{
						tbcasker = this.cpasker.getTransferredBlockChecker();
						if (tbcasker.isCompletelyInoperant())
						{
							tbcasker=new PointToPointTransferedBlockChecker();
							this.cpasker.setPointToPointTransferedBlockChecker((PointToPointTransferedBlockChecker)tbcasker);
						}
						else
						{
							this.cpasker.setPointToPointTransferedBlockChecker(null);
							tbcasker = (TransferedBlockChecker) unserialize(serialize(tbcasker));
							
						}
						
					}

					cycles++;
				} while ((masker != null && mreceiver != null) && cycles < numberMaxExchange);
			} catch (Exception e) {
				System.out.println("asker has next " + itasker.hasNext());
				System.out.println("receiver has next " + itreceiver.hasNext());
				e.printStackTrace();

				throw e;
			}
			AssertJUnit.assertTrue(cycles < numberMaxExchange);
			AssertJUnit.assertNull(masker);
			AssertJUnit.assertTrue(cpreceiver.isConnectionEstablished());
			AssertJUnit.assertTrue(cpasker.isConnectionEstablished());

			AssertJUnit.assertEquals(ConnectionState.CONNECTION_ESTABLISHED, cpasker.getConnectionState());
			AssertJUnit.assertEquals(ConnectionState.CONNECTION_ESTABLISHED, cpreceiver.getConnectionState());
			/*if (!itasker.hasNext())
			{
				this.cpasker.getCounterSelector().setActivated();
				this.cpreceiver.getCounterSelector().setActivated();
			}*/

			testRandomPingPongMessage();
			testRandomPingPongMessage();
			testRandomPingPongMessage();
			testRandomPingPongMessage();
			testRandomPingPongMessage();
			testRandomPingPongMessage();
			testRandomPingPongMessage();
			testRandomPingPongMessage();
			testRandomPingPongMessage();
			totalCycles += cycles;
		}
		
		testRandomPingPongMessage(tbcasker, tbreceiver);
		testRandomPingPongMessage(tbcasker, tbreceiver);
		testRandomPingPongMessage(tbcasker, tbreceiver);
		testRandomPingPongMessage(tbcasker, tbreceiver);

		testIrregularConnectionWithUnkowMessages(totalCycles, cpasker.getDatabaseWrapper() != null);
		testIrregularConnectionWithCurruptedMessages(totalCycles, cpasker.getDatabaseWrapper() != null);
	}

	public void testIrregularConnectionWithUnkowMessages(int cyclesNumber, boolean enableDatabase)
			throws ConnectionException, PacketException, NIOException, IOException, ClassNotFoundException,
			BlockParserException, IllegalArgumentException, NoSuchAlgorithmException, NoSuchProviderException {
		for (int i = 0; i < cyclesNumber; i++) {
			testIrregularConnectionWithUnkowMessage(i, true, enableDatabase);
			testIrregularConnectionWithUnkowMessage(i, false, enableDatabase);
		}
	}

	public void testIrregularConnectionWithUnkowMessage(int index, boolean asker, boolean enableDatabase)
			throws ConnectionException, PacketException, NIOException, IOException, ClassNotFoundException,
			BlockParserException, IllegalArgumentException, NoSuchAlgorithmException, NoSuchProviderException {
		cpasker = getConnectionProtocolInstance(mkPropertiesAsker, enableDatabase ? 1 : 0);
		cpreceiver = getConnectionProtocolInstance(mkPropertiesReceiver, enableDatabase ? 2 : 0);
		/*this.cpasker.setCounterSelector(new CounterSelector(this.cpasker));
		this.cpreceiver.setCounterSelector(new CounterSelector(this.cpreceiver));*/

		AssertJUnit.assertFalse(cpasker.isConnectionEstablished());
		AssertJUnit.assertFalse(cpreceiver.isConnectionEstablished());
		AssertJUnit.assertEquals(ConnectionState.NOT_CONNECTED, cpasker.getConnectionState());
		AssertJUnit.assertEquals(ConnectionState.NOT_CONNECTED, cpreceiver.getConnectionState());

		Iterator<ConnectionProtocol<?>> itasker = this.cpasker.reverseIterator();
		Iterator<ConnectionProtocol<?>> itreceiver = this.cpreceiver.reverseIterator();
		int cycles = 0;

		while (itasker.hasNext()) {
			ConnectionProtocol<?> cpasker = itasker.next();
			AssertJUnit.assertTrue(itreceiver.hasNext());
			ConnectionProtocol<?> cpreceiver = itreceiver.next();

			ConnectionMessage masker = cpasker.setAndGetNextMessage(new AskConnection(true));

			// testRandomPingPongMessage();
			ConnectionMessage mreceiver;

			do {
				if (cycles == index && asker) {
					masker = new UnknowConnectionMessage();
				}
				byte[] message = serialize(masker);
				assert masker != null;
				boolean excludeFromEncryption=masker.excludedFromEncryption();
				masker = (ConnectionMessage) unserialize(
						getMessage(message, getBytesToSend(getBlocks(message,excludeFromEncryption, this.cpasker, npasker, 2, -1, null)),
								this.cpreceiver, npreceiver, 2, -1, null));
				
				mreceiver = cpreceiver.setAndGetNextMessage(masker);
				if (mreceiver == null) {
					masker = null;
					break;
				} else {
					if (cycles == index) {
						if (asker) {
							AssertJUnit.assertEquals(mreceiver.getClass(), UnexpectedMessage.class);
							return;
						} else
							mreceiver = new UnknowConnectionMessage();
					}
				}

				message = serialize(mreceiver);
				excludeFromEncryption=mreceiver.excludedFromEncryption();
				mreceiver = (ConnectionMessage) unserialize(getMessage(message,
						getBytesToSend(getBlocks(message,excludeFromEncryption, this.cpreceiver, npreceiver, 2, -1, null)), this.cpasker,
						npasker, 2, -1, null));
				
				masker = cpasker.setAndGetNextMessage(mreceiver);
				if (masker != null && cycles == index) {
					AssertJUnit.assertEquals(masker.getClass(), UnexpectedMessage.class);
					return;
				}
				cycles++;
			} while ((masker != null && mreceiver != null) && cycles < numberMaxExchange);
			AssertJUnit.assertTrue(cycles < numberMaxExchange);
			AssertJUnit.assertNull(masker);
			AssertJUnit.assertTrue(cpreceiver.isConnectionEstablished());
			AssertJUnit.assertTrue(cpasker.isConnectionEstablished());

			AssertJUnit.assertEquals(ConnectionState.CONNECTION_ESTABLISHED, cpasker.getConnectionState());
			AssertJUnit.assertEquals(ConnectionState.CONNECTION_ESTABLISHED, cpreceiver.getConnectionState());

			testRandomPingPongMessage();
			testRandomPingPongMessage();
			testRandomPingPongMessage();
			testRandomPingPongMessage();
			testRandomPingPongMessage();
			testRandomPingPongMessage();
			testRandomPingPongMessage();
			testRandomPingPongMessage();
			testRandomPingPongMessage();
		}
	}

	public void testIrregularConnectionWithCurruptedMessages(int cyclesNumber, boolean enableDatabase)
			throws ConnectionException, PacketException, NIOException, IOException, ClassNotFoundException,
			BlockParserException, IllegalArgumentException, NoSuchAlgorithmException, NoSuchProviderException {
		for (int i = 0; i < cyclesNumber; i++) {
			testIrregularConnectionWithUnkowMessage(i, true, enableDatabase);
			testIrregularConnectionWithUnkowMessage(i, false, enableDatabase);
		}
	}

	public void testIrregularConnectionWithCurrptedMessage(int index, boolean asker, boolean enableDatabase)
			throws ConnectionException, PacketException, NIOException, IOException, ClassNotFoundException,
			BlockParserException, NoSuchAlgorithmException, NoSuchProviderException {
		cpasker = getConnectionProtocolInstance(mkPropertiesAsker, enableDatabase ? 1 : 0);
		cpreceiver = getConnectionProtocolInstance(mkPropertiesReceiver, enableDatabase ? 2 : 0);
		AssertJUnit.assertFalse(cpasker.isConnectionEstablished());
		AssertJUnit.assertFalse(cpreceiver.isConnectionEstablished());
		AssertJUnit.assertEquals(ConnectionState.NOT_CONNECTED, cpasker.getConnectionState());
		AssertJUnit.assertEquals(ConnectionState.NOT_CONNECTED, cpreceiver.getConnectionState());

		Iterator<ConnectionProtocol<?>> itasker = this.cpasker.reverseIterator();
		Iterator<ConnectionProtocol<?>> itreceiver = this.cpreceiver.reverseIterator();

		int cycles = 0;
		while (itasker.hasNext()) {
			ConnectionProtocol<?> cpasker = itasker.next();
			AssertJUnit.assertTrue(itreceiver.hasNext());
			ConnectionProtocol<?> cpreceiver = itreceiver.next();

			ConnectionMessage masker = cpasker.setAndGetNextMessage(new AskConnection(true));

			testRandomPingPongMessage();
			ConnectionMessage mreceiver;

			do {
				if (cycles == index && asker) {
					assert masker != null;
					masker.corrupt();
				}
				byte[] message = serialize(masker);
				assert masker != null;
				boolean excludeFromEncryption=masker.excludedFromEncryption();

				masker = (ConnectionMessage) unserialize(
						getMessage(message, getBytesToSend(getBlocks(message, excludeFromEncryption, this.cpasker, npasker, 2, -1, null)),
								this.cpreceiver, npreceiver, 2, -1, null));
				
				mreceiver = cpreceiver.setAndGetNextMessage(masker);
				if (mreceiver == null) {
					masker = null;
					break;
				} else {
					if (cycles == index && !asker) {
						mreceiver.corrupt();
					}
				}

				message = serialize(mreceiver);
				excludeFromEncryption=mreceiver.excludedFromEncryption();
				mreceiver = (ConnectionMessage) unserialize(getMessage(message,
						getBytesToSend(getBlocks(message,excludeFromEncryption, this.cpreceiver, npreceiver, 2, -1, null)), this.cpasker,
						npasker, 2, -1, null));
				
				masker = cpasker.setAndGetNextMessage(mreceiver);
				cycles++;
			} while ((masker != null && mreceiver != null) && cycles < numberMaxExchange);
			AssertJUnit.assertTrue(cycles < numberMaxExchange);
			AssertJUnit.assertNull(masker);
			AssertJUnit.assertTrue(cpreceiver.isConnectionEstablished());
			AssertJUnit.assertTrue(cpasker.isConnectionEstablished());

			AssertJUnit.assertEquals(ConnectionState.CONNECTION_ESTABLISHED, cpasker.getConnectionState());
			AssertJUnit.assertEquals(ConnectionState.CONNECTION_ESTABLISHED, cpreceiver.getConnectionState());
			testRandomPingPongMessage();
			testRandomPingPongMessage();
			testRandomPingPongMessage();
			testRandomPingPongMessage();
			testRandomPingPongMessage();
			testRandomPingPongMessage();
			testRandomPingPongMessage();
			testRandomPingPongMessage();
			testRandomPingPongMessage();
		}
	}

	static class UnknowConnectionMessage extends ConnectionMessage {


		public UnknowConnectionMessage() {
		}


		@Override
		public boolean excludedFromEncryption() {
			return false;
		}



		@Override
		public void writeExternal(SecuredObjectOutputStream out) {
			
		}



		@Override
		public void readExternal(SecuredObjectInputStream in) {
			
		}



		

	}

	private byte[] getRandomMessage() {
		byte[] message = new byte[20000 + rand.nextInt(262144/8-20000)];
		rand.nextBytes(message);
		return message;
	}

	public static ArrayList<Block> getBlocks(byte[] message, boolean excludeFromEncryption, ConnectionProtocol<?> cp, NetworkProperties np,
											 int idPacket, int transferType, TransferedBlockChecker tbc) throws PacketException, IOException,
			NIOException, NoSuchAlgorithmException, NoSuchProviderException {
		ArrayList<Block> res = new ArrayList<>();
		WritePacket wp = new WritePacket(PacketPartHead.TYPE_PACKET, idPacket, np.maxBufferSize,
				excludeFromEncryption?0:np.maxRandomPacketValues, rand, new RandomByteArrayInputStream(message), MessageDigestType.BC_FIPS_SHA3_512);
		AssertJUnit.assertEquals(idPacket, wp.getID());
		while (!wp.isFinished()) {
			if (tbc!=null)
			{
				if (tbc instanceof PointToPointTransferedBlockChecker)
				{
					cp.setPointToPointTransferedBlockChecker((PointToPointTransferedBlockChecker)tbc);
					cp.getPointToPointTransferedBlockChecker().setConnectionProtocolInput(null);
					cp.getPointToPointTransferedBlockChecker().setConnectionProtocolOutput(cp);
				}
				else
				{
					cp.setPointToPointTransferedBlockChecker(null);
				}
			}
			else
			{
				cp.setPointToPointTransferedBlockChecker(null);
			}
			Block b = cp.getBlock(wp, transferType,
					np.maxRandomPacketValues > 0 ? SecureRandomType.DEFAULT.getSingleton(null) : null, excludeFromEncryption);
			assert b != null;
			AssertJUnit.assertEquals(transferType, b.getTransferID());
			
			
			AssertJUnit.assertTrue(b.isValid());
			res.add(b);
		}
		/*for (Block b : res)
		{
			b.setCounterSelector(cp.getCounterSelector());
			cp.getCounterSelector().releaseCounterID(b.getCounterID());
		}*/
		
		return res;
	}

	public static ArrayList<byte[]> getBytesToSend(ArrayList<Block> blocks) {
		ArrayList<byte[]> res = new ArrayList<>(blocks.size());
		for (Block b : blocks)
		{
			if (b.getBlockSize()!=b.getBytes().length)
			{
				byte[] tab=new byte[b.getBlockSize()];
				System.arraycopy(b.getBytes(), 0, tab, 0, tab.length);
				res.add(tab);
			}
			else
				res.add(b.getBytes());
		}
		return res;
	}

	public static byte[] getMessage(byte[] originalMessage, ArrayList<byte[]> receivedBytes, ConnectionProtocol<?> cp,
			NetworkProperties np, int idPacket, int transferType, TransferedBlockChecker tbc) throws PacketException, NIOException, BlockParserException, IOException {
		Block b = new Block(receivedBytes.get(0));
		AssertJUnit.assertEquals(transferType, b.getTransferID());
		AssertJUnit.assertTrue(b.isValid());
		if (tbc != null) {
			if (tbc instanceof PointToPointTransferedBlockChecker)
			{
				((PointToPointTransferedBlockChecker)tbc).setConnectionProtocolInput(cp);
				((PointToPointTransferedBlockChecker)tbc).setConnectionProtocolOutput(null);
			}
			SubBlockInfo sbi = tbc.recursiveCheckSubBlock(new SubBlock(b));
			AssertJUnit.assertTrue(tbc.getClass().toString(), sbi.isValid());
			AssertJUnit.assertFalse(sbi.isCandidateToBan());
			b=new Block(sbi.getSubBlock().getBytes());
			AssertJUnit.assertEquals(transferType, b.getTransferID());
			AssertJUnit.assertTrue(b.isValid());
		}

		PacketPart pp;
		try
		{
			 pp = cp.getPacketPart(b, np);
		}
		catch(NIOException e)
		{
			if (tbc==null)
				System.out.println("tbc = null");
			else
				System.out.println("tbc = "+tbc.getClass());
			
			throw e;
		}
		AssertJUnit.assertEquals("tbc = "+tbc, idPacket, pp.getHead().getID());
		// Assert.assertEquals(originalMessage.length, pp.getHead().getTotalLength());
		AssertJUnit.assertEquals(0, pp.getHead().getStartPosition());
		RandomByteArrayOutputStream output = new RandomByteArrayOutputStream();
		ReadPacket rp = new ReadPacket(pp, output,
				MessageDigestType.BC_FIPS_SHA3_512);
		AssertJUnit.assertEquals(idPacket, rp.getID());
		for (int i = 1; i < receivedBytes.size(); i++) {
			b = new Block(receivedBytes.get(i));
			AssertJUnit.assertEquals(transferType, b.getTransferID());
			AssertJUnit.assertTrue(b.isValid());
			if (tbc != null) {
				if (tbc instanceof PointToPointTransferedBlockChecker)
				{
					((PointToPointTransferedBlockChecker)tbc).setConnectionProtocolInput(cp);
					((PointToPointTransferedBlockChecker)tbc).setConnectionProtocolOutput(null);
				}
				SubBlockInfo sbi = tbc.recursiveCheckSubBlock(new SubBlock(b));
				AssertJUnit.assertTrue(sbi.isValid());
				AssertJUnit.assertFalse(sbi.isCandidateToBan());
				b=new Block(sbi.getSubBlock().getBytes());
				AssertJUnit.assertEquals(transferType, b.getTransferID());
				AssertJUnit.assertTrue(b.isValid());
				
			}
			pp = cp.getPacketPart(b, np);
			rp.readNewPart(pp);
		}
output.flush();
		return output.getBytes();
	}

	private void testRandomPingPongMessage() throws PacketException, NIOException, IOException, BlockParserException,
			NoSuchAlgorithmException, NoSuchProviderException {
		testRandomPingPongMessage(null, null);
	}

	private void testRandomPingPongMessage(TransferedBlockChecker tbcasker, TransferedBlockChecker tbcreceiver)
			throws PacketException, NIOException, IOException, BlockParserException, NoSuchAlgorithmException,
			NoSuchProviderException {
		final int idPacket = rand.nextInt(1000000);
		int transferType = -1;
		byte[] message = getRandomMessage();
		boolean excludeFromEncryption=rand.nextBoolean();
		byte[] receivedMessage = getMessage(message,
				getBytesToSend(getBlocks(message, excludeFromEncryption, cpasker, npasker, idPacket, transferType, tbcasker)), cpreceiver,
				npreceiver, idPacket, transferType, tbcasker);
		AssertJUnit.assertArrayEquals(message, receivedMessage);
		receivedMessage = getMessage(message,
				getBytesToSend(getBlocks(message, excludeFromEncryption, cpreceiver, npreceiver, idPacket, transferType, tbcreceiver)),
				cpasker, npasker, idPacket, transferType, tbcreceiver);
		AssertJUnit.assertArrayEquals(message, receivedMessage);
	}

	public static byte[] serialize(SecureExternalizableWithoutInnerSizeControl message) throws IOException {
		
		try (RandomByteArrayOutputStream baos = new RandomByteArrayOutputStream()) {
			baos.writeObject(message, false);
			return baos.getBytes();
		}
	}

	public static SecureExternalizableWithoutInnerSizeControl unserialize(byte[] message) throws IOException, ClassNotFoundException {
		try (RandomByteArrayInputStream bais = new RandomByteArrayInputStream(message)) {

			return bais.readObject(false, SecureExternalizableWithoutInnerSizeControl.class);

		}
	}

}
