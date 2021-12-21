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

import com.distrimind.madkit.kernel.*;
import com.distrimind.madkit.kernel.network.connection.ConnectionProtocolNegotiatorProperties;
import com.distrimind.madkit.kernel.network.connection.ConnectionProtocolProperties;
import com.distrimind.madkit.kernel.network.connection.secured.*;
import com.distrimind.madkit.kernel.network.connection.unsecured.CheckSumConnectionProtocolProperties;
import com.distrimind.madkit.kernel.network.connection.unsecured.UnsecuredConnectionProtocolProperties;
import com.distrimind.madkit.message.hook.DistantKernelAgentEventMessage;
import com.distrimind.madkit.message.hook.HookMessage.AgentActionEvent;
import com.distrimind.madkit.message.hook.NetworkEventMessage;
import com.distrimind.madkit.message.hook.TransferEventMessage;
import com.distrimind.madkit.message.hook.TransferEventMessage.TransferEventType;
import com.distrimind.util.OS;
import com.distrimind.util.io.SecureExternalizable;
import com.distrimind.util.io.SecuredObjectInputStream;
import com.distrimind.util.io.SecuredObjectOutputStream;
import org.testng.AssertJUnit;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Factory;
import org.testng.annotations.Test;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Map;
import java.util.logging.Level;

/**
 * @author Jason Mahdjoub
 * 
 * @since MadkitLanEdition 1.0
 * @version 1.1
 * 
 */

public class TransferConnectionTest extends TestNGMadkit {

	public static final int HOST_NUMBERS = 4;

	@DataProvider
	public static Object[][] data() {
		try {
			ArrayList<Object[]> res = NetworkEventListener.getNetworkEventListenersForLocalClientServerConnection(false,
					true, false, false, true, true, null, null, HOST_NUMBERS - 1, 1, 2, 3, 4);
			ArrayList<Object[]> tmp = NetworkEventListener.getNetworkEventListenersForLocalClientServerConnection(false,
					true, false, false, true, true, null,null, 0, 1, 2, 3, 4);
			ArrayList<Object[]> l = new ArrayList<>();

			for (int i = 0; i < res.size(); i++) {
				Object[] a = res.get(i);
				Object[] b = tmp.get(i);
				Object[] c = new Object[a.length + 1];
				System.arraycopy(a, 0, c, 0, a.length);
				c[a.length] = b[0];
				l.add(c);
			}
			return l.toArray(new Object[l.size()][l.get(0).length]);
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	final NetworkEventListener eventListener1;
	final NetworkEventListener eventListener2;
	final NetworkEventListener eventListener3;
	final NetworkEventListener eventListener4;
	final NetworkEventListener eventListener5;

	@Factory(dataProvider = "data")
	public TransferConnectionTest(final NetworkEventListener eventListener1, final NetworkEventListener eventListener2,
			final NetworkEventListener eventListener3, final NetworkEventListener eventListener4,
			final NetworkEventListener eventListener5) {
		this.eventListener1 = eventListener1;
		this.eventListener2 = eventListener2;
		this.eventListener3 = eventListener3;
		this.eventListener4 = eventListener4;
		this.eventListener5 = eventListener5;
		this.eventListener1.setGatewayDepth(2);
		this.eventListener2.setGatewayDepth(2);
		this.eventListener3.setGatewayDepth(2);
		this.eventListener4.setGatewayDepth(2);
		this.eventListener5.setGatewayDepth(2);

		ArrayList<ConnectionProtocolProperties<?>> cpps = ((ConnectionsProtocolsMKEventListener) (this.eventListener1
				.getConnectionProtocolsMKEventListener())).getConnectionProtocolProperties();
		boolean added = false;
		for (ConnectionProtocolProperties<?> cpp : cpps) {
			ConnectionProtocolProperties<?> c = cloneAndChangeServerConnectionProtocolProperty(cpp);
			if (c != null) {
				((ConnectionsProtocolsMKEventListener) (this.eventListener4.getConnectionProtocolsMKEventListener()))
						.add(cloneAndChangeConnectionProtocolProperty(c));
				added = true;
				break;
			}
		}
		AssertJUnit.assertTrue(added);
		this.eventListener4.setLocalPortToBind(5100);
		try {
			this.eventListener4.addInetAddressesToBind(InetAddress.getByName("0.0.0.0"));
			this.eventListener5.setLocalPortToBind(5101);
			this.eventListener2
					.addConnectionsToAttempt(new DoubleIP(5101, (Inet4Address) InetAddress.getByName("127.0.0.1")));
		} catch (UnknownHostException e) {
			e.printStackTrace();
			AssertJUnit.fail();
		}

	}

	private ConnectionProtocolProperties<?> cloneAndChangeServerConnectionProtocolProperty(
			ConnectionProtocolProperties<?> cpp) {
		ConnectionProtocolProperties<?> res = cloneAndChangeConnectionProtocolProperty(cpp);
		ConnectionProtocolProperties<?> c = res;

		while (c != null) {
			if (c.getClass() == ClientSecuredProtocolPropertiesWithKnownPublicKey.class)
				return null;
			else if (cpp.getClass() == P2PSecuredConnectionProtocolWithASymmetricKeyExchangerProperties.class) {
				if (!((P2PSecuredConnectionProtocolWithASymmetricKeyExchangerProperties) cpp).isServer)
					return null;
			} else if (cpp.getClass() == CheckSumConnectionProtocolProperties.class) {
				if (!((CheckSumConnectionProtocolProperties) cpp).isServer)
					return null;
			}
			else if (cpp.getClass() == P2PSecuredConnectionProtocolWithKnownSymmetricKeysProperties.class) {
				if (!((P2PSecuredConnectionProtocolWithKnownSymmetricKeysProperties) cpp).isServer)
					return null;
			}
			else if (cpp.getClass() == UnsecuredConnectionProtocolProperties.class) {
				if (!((UnsecuredConnectionProtocolProperties) cpp).isServer)
					return null;
			}
			c = c.subProtocolProperties;
		}
		return res;

	}

	private ConnectionProtocolProperties<?> cloneAndChangeConnectionProtocolProperty(
			ConnectionProtocolProperties<?> cpp) {
		ConnectionProtocolProperties<?> res = null;
		if (cpp.getClass() == ServerSecuredProtocolPropertiesWithKnownPublicKey.class) {
			ServerSecuredProtocolPropertiesWithKnownPublicKey sold = (ServerSecuredProtocolPropertiesWithKnownPublicKey) cpp;
			ServerSecuredProtocolPropertiesWithKnownPublicKey s = new ServerSecuredProtocolPropertiesWithKnownPublicKey();
			s.addEncryptionProfile(sold.getDefaultKeyPairForEncryption(), 
					sold.getDefaultSymmetricEncryptionType(), sold.getDefaultSymmetricEncryptionKeySizeBits(), sold.getDefaultKeyWrapper(), sold.getDefaultSignatureType(), sold.getDefaultMessageDigestType());
			res = s;
		} else if (cpp.getClass() == P2PSecuredConnectionProtocolWithASymmetricKeyExchangerProperties.class) {
			P2PSecuredConnectionProtocolWithASymmetricKeyExchangerProperties sold = (P2PSecuredConnectionProtocolWithASymmetricKeyExchangerProperties) cpp;
			P2PSecuredConnectionProtocolWithASymmetricKeyExchangerProperties s = new P2PSecuredConnectionProtocolWithASymmetricKeyExchangerProperties();
			s.aSymmetricEncryptionType = sold.aSymmetricEncryptionType;
			s.aSymmetricKeySize = sold.aSymmetricKeySize;
			s.aSymmetricKeyExpirationMs = sold.aSymmetricKeyExpirationMs;
			s.enableEncryption = sold.enableEncryption;
			s.isServer = sold.isServer;
			s.symmetricEncryptionType = sold.symmetricEncryptionType;
			s.signatureType = sold.signatureType;
			res = s;
		} else if (cpp.getClass() == P2PSecuredConnectionProtocolPropertiesWithKeyAgreement.class) {
			P2PSecuredConnectionProtocolPropertiesWithKeyAgreement sold = (P2PSecuredConnectionProtocolPropertiesWithKeyAgreement) cpp;
			P2PSecuredConnectionProtocolPropertiesWithKeyAgreement s = new P2PSecuredConnectionProtocolPropertiesWithKeyAgreement();
			s.enableEncryption = sold.enableEncryption;
			s.isServer = sold.isServer;
			s.symmetricEncryptionType = sold.symmetricEncryptionType;
			s.symmetricSignatureType=sold.symmetricSignatureType;
			res = s;
		} else if (cpp.getClass() == P2PSecuredConnectionProtocolWithKnownSymmetricKeysProperties.class) {
			P2PSecuredConnectionProtocolWithKnownSymmetricKeysProperties sold = (P2PSecuredConnectionProtocolWithKnownSymmetricKeysProperties) cpp;
			P2PSecuredConnectionProtocolWithKnownSymmetricKeysProperties s = new P2PSecuredConnectionProtocolWithKnownSymmetricKeysProperties();
			s.enableEncryption = sold.enableEncryption;
			s.isServer = sold.isServer;
			for (Integer i : sold.getProfileIdentifiers())
			{
				s.addProfile(i, sold.getSymmetricSecretKeyForEncryption(i), sold.getSymmetricSecretKeyForSignature(i), sold.getMessageDigestType(i));
				if (!sold.isValidProfile(i, EncryptionRestriction.NO_RESTRICTION))
					s.invalidateProfile(i);
			}

			s.setDefaultProfileIdentifier(sold.getDefaultProfileIdentifier(EncryptionRestriction.NO_RESTRICTION));
			res = s;
		}
		else if (cpp.getClass() == CheckSumConnectionProtocolProperties.class) {
			CheckSumConnectionProtocolProperties sold = (CheckSumConnectionProtocolProperties) cpp;
			CheckSumConnectionProtocolProperties s = new CheckSumConnectionProtocolProperties();
			s.isServer = sold.isServer;
			s.messageDigestType = sold.messageDigestType;
			s.renewRandomInterval = sold.renewRandomInterval;

			res = s;
		} else if (cpp.getClass() == UnsecuredConnectionProtocolProperties.class) {
			UnsecuredConnectionProtocolProperties sold = (UnsecuredConnectionProtocolProperties) cpp;
			UnsecuredConnectionProtocolProperties s = new UnsecuredConnectionProtocolProperties();
			s.isServer = sold.isServer;
			res = s;
		}
		else if (cpp.getClass() == ConnectionProtocolNegotiatorProperties.class) {
			ConnectionProtocolNegotiatorProperties sold = (ConnectionProtocolNegotiatorProperties) cpp;
			ConnectionProtocolNegotiatorProperties s = new ConnectionProtocolNegotiatorProperties(sold);
			for (Map.Entry<Integer, ConnectionProtocolProperties<?>> e : sold.getConnectionProtocolProperties().entrySet())
				s.getConnectionProtocolProperties().put(e.getKey(), cloneAndChangeConnectionProtocolProperty(e.getValue()));
			res = s;
		}
		if (cpp.subProtocolProperties != null) {
			assert res != null;
			res.subProtocolProperties = cloneAndChangeConnectionProtocolProperty(cpp.subProtocolProperties);
		}
		return res;

	}

	private static final long timeOut = 180000;
	final static SecureExternalizable attachedData = new SecureExternalizable() {
		

		@Override
		public int getInternalSerializedSize() {
			
			return 0;
		}

		@Override
		public void writeExternal(SecuredObjectOutputStream out) {
			
		}

		@Override
		public void readExternal(SecuredObjectInputStream in) {
			
		}
	};

	@Test
	public void tryDirectConnectionTestWithKernelAddressReference() {
		indirectConnectionTest(AskForTransferMessage.Type.TRY_DIRECT_CONNECTION_FIRST_OR_TRANSFER, true);

	}

	@Test
	public void tryDirectConnectionTestWithInetAddressReference() {
		indirectConnectionTest(AskForTransferMessage.Type.TRY_DIRECT_CONNECTION_FIRST_OR_TRANSFER, false);
	}

	@Test
	public void transferConnectionTestWithKernelAddressReference() {
		indirectConnectionTest(AskForTransferMessage.Type.TRANSFER, true);
	}

	@Test
	public void transferConnectionTestWithInetAddressReference() {
		indirectConnectionTest(AskForTransferMessage.Type.TRANSFER, false);
	}

	protected static KernelAddress ka1;
	protected static KernelAddress ka2;

	public void indirectConnectionTest(final AskForTransferMessage.Type type,
			final boolean connectionPerKernelAddress) {
		System.out.println("JRE Version : "+OS.getCurrentJREVersionDouble());
		cleanHelperMDKs();
		// addMadkitArgs(LevelOption.networkLogLevel.toString(),"FINER");
		launchTest(new AbstractAgent() {

			@Override
			protected void activate() throws InterruptedException {
				System.err.println("------------------------ Thread cound at start : " + Thread.activeCount());
				
				AgentToLaunch a1 = new AgentToLaunch(connectionPerKernelAddress, false);
				AgentToLaunch a2 = new AgentToLaunch(connectionPerKernelAddress, true);

				System.out.println("**************Launch kernels****************");
				System.out.println(TransferConnectionTest.this.eventListener1.madkitEventListenerForConnectionProtocols.getConnectionProtocolProperties().toString());
				
				launchThreadedMKNetworkInstance(Level.INFO, AbstractAgent.class, a1, eventListener1);

				ka1 = TransferConnectionTest.this.getKernelAddress(getHelperInstances(this, 1).get(0));

				launchThreadedMKNetworkInstance(Level.INFO, AbstractAgent.class, null, eventListener5);
				ka2 = TransferConnectionTest.this.getKernelAddress(getHelperInstances(this, 2).get(1));
				sleep(2000);
				launchThreadedMKNetworkInstance(Level.INFO, AbstractAgent.class, a2, eventListener2);
				System.out.println("**************Test connections 1****************");
				int index = -1;
				for (Madkit m : getHelperInstances(this, 3)) {
					++index;
					if (index == 2)
					{
						checkConnectedIntancesNb(this, m, 2, timeOut);
						checkConnectedKernelsNb(this, m, 2, timeOut);

					}
					else
					{
						checkConnectedIntancesNb(this, m, 1, timeOut);
						checkConnectedKernelsNb(this, m, 1, timeOut);
					}
				}
				sleep(1000);
				launchThreadedMKNetworkInstance(Level.INFO, AbstractAgent.class, null, eventListener3);

				System.out.println("**************Test connections 2****************");
				// test madkit kernels connection
				index = -1;
				for (Madkit m : getHelperInstances(this, 4)) {
					++index;
					if (index == 0 || index == 2)
					{
						checkConnectedKernelsNb(this, m, 2, timeOut);
						checkConnectedIntancesNb(this, m, 2, timeOut);
					}
					else
					{
						checkConnectedKernelsNb(this, m, 1, timeOut);
						checkConnectedIntancesNb(this, m, 1, timeOut);
					}
				}
				System.out
						.println("**************Try transafert connection between 2 and 3 (through 1)****************");
				// test 1 transfer connection
				sleep(400);
				a1.askForTransferConnection(type, attachedData);

				index = -1;
				for (Madkit m : getHelperInstances(this, 4)) {
					++index;
					if (index == 2)
						checkConnectedKernelsNb(this, m, 3, timeOut);
					else if (index != 1)
						checkConnectedKernelsNb(this, m, 2, timeOut);
					else
						checkConnectedKernelsNb(this, m, 1, timeOut);
				}
				sleep(600);
				a1.assertConnection(1, 0, 0);
				System.out.println(
						"**************Try transafert connection between 5 and 3 (through 2 and 1)****************");
				a2.askForTransferConnection(type, attachedData);
				index = -1;
				for (Madkit m : getHelperInstances(this, 4)) {
					++index;
					if (index == 2 || index == 3)
						checkConnectedKernelsNb(this, m, 3, timeOut);
					else
						checkConnectedKernelsNb(this, m, 2, timeOut);
				}
				sleep(600);
				a1.assertConnection(1, 0, 0);
				a2.assertConnection(
						((type == AskForTransferMessage.Type.TRY_DIRECT_CONNECTION_FIRST_OR_TRANSFER) ? 0 : 1),
						((type == AskForTransferMessage.Type.TRY_DIRECT_CONNECTION_FIRST_OR_TRANSFER) ? 1 : 0), 0);

				System.out.println(
						"**************Disconnect transfert connection between 2 and 3 (through 1)****************");
				// test transfer deconnection
				a1.askForTransferDeconnection();
				index = -1;
				for (Madkit m : getHelperInstances(this, 4)) {
					++index;
					if (index == 0 || index == 2)
						checkConnectedKernelsNb(this, m, 2, timeOut);
					else if (index == 1 || index == 3) {
						checkConnectedKernelsNb(this, m,
								((type == AskForTransferMessage.Type.TRY_DIRECT_CONNECTION_FIRST_OR_TRANSFER) ? 2 : 1),
								timeOut);
					} else {
						checkConnectedKernelsNb(this, m, 1, timeOut);
					}
				}
				sleep(400);
				a1.assertConnection(1, 0, 1);
				a2.assertConnection(
						((type == AskForTransferMessage.Type.TRY_DIRECT_CONNECTION_FIRST_OR_TRANSFER) ? 0 : 1),
						((type == AskForTransferMessage.Type.TRY_DIRECT_CONNECTION_FIRST_OR_TRANSFER) ? 1 : 0),
						((type == AskForTransferMessage.Type.TRY_DIRECT_CONNECTION_FIRST_OR_TRANSFER) ? 0 : 1));
				System.out.println(
						"**************try(re) transfert connection between 2 and 3 (through 1)****************");

				// reconnection transfer
				sleep(200);
				a1.askForTransferConnection(type, attachedData);

				index = -1;
				for (Madkit m : getHelperInstances(this, 4)) {
					++index;
					if (index == 2)
						checkConnectedKernelsNb(this, m, 3, timeOut);
					else if (index != 1 && index != 3)
						checkConnectedKernelsNb(this, m, 2, timeOut);
					else if (index == 1)
						checkConnectedKernelsNb(this, m,
								1 + ((type == AskForTransferMessage.Type.TRY_DIRECT_CONNECTION_FIRST_OR_TRANSFER) ? 1
										: 0),
								timeOut);
					else if (index == 3)
						checkConnectedKernelsNb(this, m,
								2 + ((type == AskForTransferMessage.Type.TRY_DIRECT_CONNECTION_FIRST_OR_TRANSFER) ? 1
										: 0),
								timeOut);
				}
				sleep(400);
				a1.assertConnection(2, 0, 1);
				a2.assertConnection(
						((type == AskForTransferMessage.Type.TRY_DIRECT_CONNECTION_FIRST_OR_TRANSFER) ? 0 : 1),
						((type == AskForTransferMessage.Type.TRY_DIRECT_CONNECTION_FIRST_OR_TRANSFER) ? 1 : 0),
						((type == AskForTransferMessage.Type.TRY_DIRECT_CONNECTION_FIRST_OR_TRANSFER) ? 0 : 1));

				System.out.println("**************Adding kernel 4****************");

				// adding new kernel
				launchThreadedMKNetworkInstance(Level.INFO, AbstractAgent.class, null, eventListener4);
				index = -1;
				for (Madkit m : getHelperInstances(this, 5)) {
					if (++index == 0 || index == 2)
						checkConnectedKernelsNb(this, m, 3, timeOut);
					else if (index == 3) {
						checkConnectedKernelsNb(this, m,
								2 + ((type == AskForTransferMessage.Type.TRY_DIRECT_CONNECTION_FIRST_OR_TRANSFER) ? 1
										: 0),
								timeOut);
					} else if (index == 1)
						checkConnectedKernelsNb(this, m,
								1 + ((type == AskForTransferMessage.Type.TRY_DIRECT_CONNECTION_FIRST_OR_TRANSFER) ? 1
										: 0),
								timeOut);
					else
						checkConnectedKernelsNb(this, m, 1, timeOut);
				}
				sleep(600);
				System.out.println(
						"**************Disconnect transfert connection between 4 and 3 (through 1) and between 4 and 2 (trough 1)****************");
				a1.askForTransferConnection(type, attachedData);
				index = -1;
				for (Madkit m : getHelperInstances(this, 5)) {
					if (++index == 2)
						checkConnectedKernelsNb(this, m, 4, timeOut);
					else if (index != 1 && index != 3) {
						checkConnectedKernelsNb(this, m, 3, timeOut);
					} else if (index == 3)
						checkConnectedKernelsNb(this, m,
								3 + ((type == AskForTransferMessage.Type.TRY_DIRECT_CONNECTION_FIRST_OR_TRANSFER) ? 1
										: 0),
								timeOut);
					else if (index == 1)
						checkConnectedKernelsNb(this, m,
								1 + ((type == AskForTransferMessage.Type.TRY_DIRECT_CONNECTION_FIRST_OR_TRANSFER) ? 1
										: 0),
								timeOut);
					else
						checkConnectedKernelsNb(this, m, 1, timeOut);
				}
				sleep(200);
				a1.assertConnection(
						2 + (type == AskForTransferMessage.Type.TRY_DIRECT_CONNECTION_FIRST_OR_TRANSFER ? 0 : 2),
						(type == AskForTransferMessage.Type.TRY_DIRECT_CONNECTION_FIRST_OR_TRANSFER ? 2 : 0), 1);
				a2.assertConnection(
						((type == AskForTransferMessage.Type.TRY_DIRECT_CONNECTION_FIRST_OR_TRANSFER) ? 0 : 1),
						((type == AskForTransferMessage.Type.TRY_DIRECT_CONNECTION_FIRST_OR_TRANSFER) ? 1 : 0),
						((type == AskForTransferMessage.Type.TRY_DIRECT_CONNECTION_FIRST_OR_TRANSFER) ? 0 : 1));

				if (logger != null)
					logger.info("stoping networks");
				// pause(1000);
				for (Madkit m : getHelperInstances(this, 5))
					stopNetworkWithMadkit(m);
				for (Madkit m : getHelperInstances(this, 5))
					checkConnectedKernelsNb(this, m, 0, timeOut);

				cleanHelperMDKs(this);
				AssertJUnit.assertEquals(getHelperInstances(this, 0).size(), 0);

				sleep(400);
				System.err.println("------------------------ Thread cound at end : " + Thread.activeCount());
			}
		});
	}

}

class AgentToLaunch extends AgentFakeThread {

	volatile KernelAddress kernelAddress1 = null;
	volatile KernelAddress kernelAddress2 = null;
	volatile KernelAddress kernelAddress3 = null;
	volatile Connection connection1 = null;
	volatile Connection connection2 = null;
	volatile Connection connection3 = null;
	private AskForTransferMessage.Type type;
	private SecureExternalizable attachedData;
	private volatile int transferConnectionFinished = 0;
	private volatile int transferDeconnected = 0;
	private volatile int directConnectionFinished = 0;
	private volatile boolean connectionOK = true;
	private final boolean connectionPerKernelAddress;
	private final boolean tryMultiplePeerConnection;

	AgentToLaunch(boolean connectionPerKernelAddress, boolean tryMultiplePeerConnection) {
		this.connectionPerKernelAddress = connectionPerKernelAddress;
		this.tryMultiplePeerConnection = tryMultiplePeerConnection;
	}

	@Override
	public void activate() {
		AssertJUnit.assertEquals(ReturnCode.SUCCESS, requestHookEvents(AgentActionEvent.TRANSFER_CONNEXION_EVENT));
		AssertJUnit.assertEquals(ReturnCode.SUCCESS, requestHookEvents(AgentActionEvent.DISTANT_KERNEL_CONNECTED));
		if (!connectionPerKernelAddress)
			AssertJUnit.assertEquals(ReturnCode.SUCCESS, requestHookEvents(AgentActionEvent.CONNEXION_ESTABLISHED));
		System.out.println("Agent to launch OK");
	}

	@Override
	protected void liveByStep(Message _message) {
		if (_message instanceof DistantKernelAgentEventMessage) {
			DistantKernelAgentEventMessage m = (DistantKernelAgentEventMessage) _message;
			if (!tryMultiplePeerConnection || !m.getDistantKernelAddress().equals(TransferConnectionTest.ka1)) {
				if (kernelAddress1 == null)
					kernelAddress1 = m.getDistantKernelAddress();
				else {
					if (kernelAddress2 == null)
						kernelAddress2 = m.getDistantKernelAddress();
					else
						kernelAddress3 = m.getDistantKernelAddress();

					synchronized (this) {
						if (connectionPerKernelAddress) {
							ensureTransferConnectionWithKernelAddress();
						} else {
							ensureTransferConnectionWithInetAddress();
						}
					}
				}
			}
		} else if (_message instanceof NetworkEventMessage) {
			NetworkEventMessage m = (NetworkEventMessage) _message;
			if (!tryMultiplePeerConnection || m.getConnection().getConnectionIdentifier().getDistantInetSocketAddress().getPort() != 5001) {
				if (connection1 == null)
					connection1 = m.getConnection();
				else if (connection2 == null) {
					connection2 = m.getConnection();
				} else
					connection3 = m.getConnection();
			}
		} else if (_message instanceof TransferEventMessage) {
			TransferEventMessage t = (TransferEventMessage) _message;
			if (t.getEventType() == TransferEventType.DIRECT_CONNECTION_EFFECTIVE) {
				directConnectionFinished++;
				connectionOK &= attachedData.equals(t.getOriginalMessage().getAttachedData());
				AssertJUnit.assertTrue(connectionOK);
			} else if (t.getEventType() == TransferEventType.TRANSFER_EFFECTIVE) {
				transferConnectionFinished++;
				connectionOK &= attachedData.equals(t.getOriginalMessage().getAttachedData());
				AssertJUnit.assertTrue(connectionOK);
			} else if (t.getEventType() == TransferEventType.TRANSFER_DISCONNECTED) {
				transferDeconnected++;
			} else {
				System.out.println(_message);
				connectionOK = false;
				AssertJUnit.fail();
			}

		} else
			System.out.println(_message);
	}

	public void assertConnection(int transferConnectionNb, int directConnectionNb, int transferDeconnectionNb) {
		AssertJUnit.assertEquals(directConnectionNb, directConnectionFinished);
		AssertJUnit.assertEquals(transferConnectionNb, transferConnectionFinished);
		AssertJUnit.assertEquals(transferDeconnectionNb, transferDeconnected);
		AssertJUnit.assertTrue(connectionOK);
	}

	private void ensureTransferConnectionWithInetAddress() {
		if (this.type != null && connection1 != null && ((connection3 != null && kernelAddress3 != null)
				|| (connection3 == null && connection2 != null && kernelAddress2 != null))) {
			try {
				if (connection3 == null) {
					manageTransferConnection(new AskForTransferMessage(type,
							connection1.getConnectionIdentifier().getDistantInetSocketAddress(),
							connection2.getConnectionIdentifier().getDistantInetSocketAddress(), attachedData));
					System.out.println("Connection per inet address asked : " + connection1 + " --- " + connection2
							+ " --- (" + connection3 + ")");
				} else {
					manageTransferConnection(new AskForTransferMessage(type,
							connection1.getConnectionIdentifier().getDistantInetSocketAddress(),
							connection3.getConnectionIdentifier().getDistantInetSocketAddress(), attachedData));
					System.out.println("Connection per inet address asked : " + connection1 + " --- " + connection3);
					sleep(200);
					manageTransferConnection(new AskForTransferMessage(type,
							connection2.getConnectionIdentifier().getDistantInetSocketAddress(),
							connection3.getConnectionIdentifier().getDistantInetSocketAddress(), attachedData));
					System.out.println("Connection per inet address asked : " + connection2 + " --- " + connection3);
				}

			} catch (Exception e) {
				e.printStackTrace();
				AssertJUnit.fail();
			}
			type = null;
		}
	}

	private void ensureTransferConnectionWithKernelAddress() {
		if (this.type != null && kernelAddress1 != null && kernelAddress2 != null) {
			try {
				if (kernelAddress3 == null) {
					manageTransferConnection(
							new AskForTransferMessage(type, kernelAddress1, kernelAddress2, attachedData));
					System.out.println("Connection per kernel address asked (one)");
				} else {
					manageTransferConnection(
							new AskForTransferMessage(type, kernelAddress1, kernelAddress3, attachedData));
					sleep(200);
					manageTransferConnection(
							new AskForTransferMessage(type, kernelAddress2, kernelAddress3, attachedData));
					System.out.println("Connection per kernel address asked (two)");
				}

			} catch (Exception e) {
				e.printStackTrace();
				AssertJUnit.fail();
			}
			type = null;
		}
	}

	public void askForTransferConnection(AskForTransferMessage.Type type, SecureExternalizable attachedData) {
		synchronized (this) {
			this.type = type;
			this.attachedData = attachedData;
			if (connectionPerKernelAddress) {
				ensureTransferConnectionWithKernelAddress();
			} else {
				ensureTransferConnectionWithInetAddress();
			}
		}
	}

	public void askForTransferDeconnection() {
		synchronized (this) {
			try {
				if (connectionPerKernelAddress)
					manageTransferConnection(new AskForTransferMessage(AskForTransferMessage.Type.DISCONNECT,
							kernelAddress1, kernelAddress2, attachedData));
				else
					manageTransferConnection(new AskForTransferMessage(AskForTransferMessage.Type.DISCONNECT,
							connection1.getConnectionIdentifier().getDistantInetSocketAddress(),
							connection2.getConnectionIdentifier().getDistantInetSocketAddress(), attachedData));
				System.out.println("Deconnection asked");
			} catch (Exception e) {
				e.printStackTrace();
				AssertJUnit.fail();
			}
		}
	}
}
