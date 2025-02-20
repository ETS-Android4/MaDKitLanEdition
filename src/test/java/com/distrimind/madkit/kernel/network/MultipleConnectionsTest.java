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

import com.distrimind.madkit.kernel.AbstractAgent;
import com.distrimind.madkit.kernel.TestNGMadkit;
import com.distrimind.madkit.kernel.Madkit;
import com.distrimind.madkit.kernel.network.connection.ConnectionProtocolNegotiator;
import com.distrimind.madkit.kernel.network.connection.ConnectionProtocolNegotiatorProperties;
import com.distrimind.madkit.kernel.network.connection.ConnectionProtocolProperties;
import com.distrimind.madkit.testing.util.agent.AgentBigTransfer;
import com.distrimind.madkit.testing.util.agent.NetworkPingAgent;
import com.distrimind.madkit.testing.util.agent.NetworkPongAgent;
import com.distrimind.ood.database.EmbeddedH2DatabaseWrapper;
import com.distrimind.util.Timer;
import org.testng.AssertJUnit;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Factory;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Level;

/**
 * 
 * @author Jason Mahdjoub
 * @since MadkitLanEdition 1.0
 * @version 1.1
 * 
 */

public class MultipleConnectionsTest extends TestNGMadkit {

	public static final int HOST_NUMBERS = 5;

	@DataProvider
	public static Object[][] data() {
		try {
			ArrayList<Object[]> res = new ArrayList<>();
			res.addAll(Objects.requireNonNull(data(NetworkEventListener.getNetworkEventListenersForLocalClientServerConnection(true, true,
					false, true, true, false, null,null, HOST_NUMBERS - 1, 1, 2, 3, 4, 5), null, null)));
			res.addAll(Objects.requireNonNull(data(NetworkEventListener.getNetworkEventListenersForLocalClientServerConnection(true, true,
					false, true, true, false, null,null, HOST_NUMBERS - 1, 1, 2, 3, 4, 5), 100, null)));
			res.addAll(Objects.requireNonNull(data(NetworkEventListener.getNetworkEventListenersForLocalClientServerConnection(true, true,
					false, true, true, false, null,null, HOST_NUMBERS - 1, 1, 2, 3, 4, 5), null, 200)));
			res.addAll(Objects.requireNonNull(data(NetworkEventListener.getNetworkEventListenersForLocalClientServerConnection(true, true,
					false, true, true, false, null,null, HOST_NUMBERS - 1, 1, 2, 3, 4, 5), 100,
					200)));
			return res.toArray(new Object[res.size()][res.get(0).length]);
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	public static Collection<Object[]> data(Collection<Object[]> c, Integer localDataAmountAcc,
			Integer globalDataAmountAcc) {
		try {
			ArrayList<Object[]> res = new ArrayList<>(c.size());
			for (Object[] o : c) {
				Object[] o2 = new Object[o.length + 2];
				o2[0] = localDataAmountAcc;
				o2[1] = globalDataAmountAcc;
				System.arraycopy(o, 0, o2, 2, o.length);
				res.add(o2);
			}
			return res;
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
	final Integer localDataAmountAcc;
	final Integer globalDataAmountAcc;

	@Factory(dataProvider = "data")
	public MultipleConnectionsTest(Integer localDataAmountAcc, Integer globalDataAmountAcc,
			final NetworkEventListener eventListener1, final NetworkEventListener eventListener2,
			final NetworkEventListener eventListener3, final NetworkEventListener eventListener4,
			final NetworkEventListener eventListener5) {
		this.eventListener1 = eventListener1;
		this.eventListener2 = eventListener2;
		this.eventListener3 = eventListener3;
		this.eventListener4 = eventListener4;
		this.eventListener5 = eventListener5;
		this.localDataAmountAcc = localDataAmountAcc;
		this.globalDataAmountAcc = globalDataAmountAcc;
	}

	private static final long timeOut = 60000;

	@Test
	public void multipleAsynchronousConnectionTest() {
		eventListener1.setLocalDataAmountAcc(localDataAmountAcc);
		eventListener1.setGlobalDataAmountAcc(globalDataAmountAcc);
		eventListener2.setLocalDataAmountAcc(localDataAmountAcc);
		eventListener2.setGlobalDataAmountAcc(globalDataAmountAcc);
		eventListener3.setLocalDataAmountAcc(localDataAmountAcc);
		eventListener3.setGlobalDataAmountAcc(globalDataAmountAcc);
		eventListener4.setLocalDataAmountAcc(localDataAmountAcc);
		eventListener4.setGlobalDataAmountAcc(globalDataAmountAcc);
		eventListener5.setLocalDataAmountAcc(localDataAmountAcc);
		eventListener5.setGlobalDataAmountAcc(globalDataAmountAcc);
		for (ConnectionProtocolProperties<?> cpp : eventListener1.madkitEventListenerForConnectionProtocols.getConnectionProtocolProperties()) {
			System.out.println(cpp);
			if (cpp instanceof ConnectionProtocolNegotiatorProperties) {
				ConnectionProtocolNegotiatorProperties c=(ConnectionProtocolNegotiatorProperties)cpp;
				for (Map.Entry<Integer, ConnectionProtocolProperties<?>> e :  c.getConnectionProtocolProperties().entrySet())
				{
					System.out.println("\tto negotiate ("+e.getKey()+") : " + e.getValue());
				}
			}
			while((cpp=cpp.subProtocolProperties)!=null)
				System.out.println("\tsub protocol : "+cpp);

		}
		for (ConnectionProtocolProperties<?> cpp : eventListener2.madkitEventListenerForConnectionProtocols.getConnectionProtocolProperties()) {
			System.out.println(cpp);
			if (cpp instanceof ConnectionProtocolNegotiatorProperties) {
				ConnectionProtocolNegotiatorProperties c=(ConnectionProtocolNegotiatorProperties)cpp;
				for (Map.Entry<Integer, ConnectionProtocolProperties<?>> e :  c.getConnectionProtocolProperties().entrySet())
				{
					System.out.println("\tto negotiate ("+e.getKey()+") : " + e.getValue());
				}
			}
			while((cpp=cpp.subProtocolProperties)!=null)
				System.out.println("\tsub protocol : "+cpp);
		}
		cleanHelperMDKs();
		// addMadkitArgs(LevelOption.networkLogLevel.toString(),"FINER");
		launchTest(new AbstractAgent() {
			@SuppressWarnings("UnusedAssignment")
			@Override
			protected void activate() throws InterruptedException {
				try {
					sleep(1000);
					System.out.println("------------------------ Thread cound at start : " + Thread.activeCount());
					System.out.println("------------------------ localDataAmountAcc=" + localDataAmountAcc
							+ " --- globalDataAmountAcc=" + globalDataAmountAcc);
					removeDatabase();
					AgentsToLaunch agentsToLaunch1 = new AgentsToLaunch(1, 4, true);
					AgentsToLaunch agentsToLaunch2 = new AgentsToLaunch(2, 1, false);
					AgentsToLaunch agentsToLaunch3 = new AgentsToLaunch(3, 1, false);
					AgentsToLaunch agentsToLaunch4 = new AgentsToLaunch(4, 1, false);
					AgentsToLaunch agentsToLaunch5 = new AgentsToLaunch(5, 1, false);

					launchThreadedMKNetworkInstance(Level.INFO, AbstractAgent.class, agentsToLaunch1, eventListener1);
					sleep(2500);
					launchThreadedMKNetworkInstance(Level.INFO, AbstractAgent.class, agentsToLaunch2, eventListener2);
					launchThreadedMKNetworkInstance(Level.INFO, AbstractAgent.class, agentsToLaunch3, eventListener3);
					launchThreadedMKNetworkInstance(Level.INFO, AbstractAgent.class, agentsToLaunch4, eventListener4);
					launchThreadedMKNetworkInstance(Level.INFO, AbstractAgent.class, agentsToLaunch5, eventListener5);

					sleep(1400);
					int index = 0;
					for (Madkit m : getHelperInstances(this, 5)) {
						if (index++ == 0) {
							checkConnectedIntancesNb(this, m, 8, timeOut);
							checkConnectedKernelsNb(this, m, 4, timeOut);
						} else {
							checkConnectedIntancesNb(this, m, 2, timeOut);
							checkConnectedKernelsNb(this, m, 1, timeOut);
						}
					}
					Timer t = new Timer(true);
					while (t.getMilli() < 10000 && (!agentsToLaunch1.networkPingAgent.isOK()
							|| !agentsToLaunch2.networkPingAgent.isOK() || !agentsToLaunch3.networkPingAgent.isOK()
							|| !agentsToLaunch4.networkPingAgent.isOK() || !agentsToLaunch5.networkPingAgent.isOK())) {
						System.out.println("-------------------");
						agentsToLaunch1.networkPingAgent.printOK();
						agentsToLaunch2.networkPingAgent.printOK();
						agentsToLaunch3.networkPingAgent.printOK();
						agentsToLaunch4.networkPingAgent.printOK();
						agentsToLaunch5.networkPingAgent.printOK();
						sleep(1000);
					}

					AssertJUnit.assertTrue(agentsToLaunch1.networkPingAgent.isOK());
					AssertJUnit.assertTrue(agentsToLaunch2.networkPingAgent.isOK());
					AssertJUnit.assertTrue(agentsToLaunch3.networkPingAgent.isOK());
					AssertJUnit.assertTrue(agentsToLaunch4.networkPingAgent.isOK());
					AssertJUnit.assertTrue(agentsToLaunch5.networkPingAgent.isOK());

					/*agentsToLaunch1.networkPingAgent.killPingPongAgents();
					agentsToLaunch2.networkPingAgent.killPingPongAgents();
					agentsToLaunch3.networkPingAgent.killPingPongAgents();
					agentsToLaunch4.networkPingAgent.killPingPongAgents();
					agentsToLaunch5.networkPingAgent.killPingPongAgents();*/


					AssertJUnit.assertEquals(ReturnCode.SUCCESS, agentsToLaunch1.launchBigDataTransferAgent());
					AssertJUnit.assertEquals(ReturnCode.SUCCESS, agentsToLaunch2.launchBigDataTransferAgent());
					AssertJUnit.assertEquals(ReturnCode.SUCCESS, agentsToLaunch3.launchBigDataTransferAgent());
					AssertJUnit.assertEquals(ReturnCode.SUCCESS, agentsToLaunch4.launchBigDataTransferAgent());
					AssertJUnit.assertEquals(ReturnCode.SUCCESS, agentsToLaunch5.launchBigDataTransferAgent());
					sleep(2000);
					for (Madkit m : getHelperInstances(this, 5))
					{
						AssertJUnit.assertTrue(isAgentsPresentInGroup(m, TestNGMadkit.DEFAULT_NETWORK_GROUP_FOR_ACCESS_DATA, AgentBigTransfer.class, true, AgentBigTransfer.bigTransferRole));
						AssertJUnit.assertTrue(isAgentsPresentInGroup(m, TestNGMadkit.DEFAULT_NETWORK_GROUP_FOR_ACCESS_DATA, AgentBigTransfer.class, false, AgentBigTransfer.bigTransferRole));
					}

					int nb = 0;
					boolean lanToSynchro=false;
					int nb2=0;
					while (nb++ < 200 && (!agentsToLaunch1.agentBigTransfer.isFinished()
							|| !agentsToLaunch2.agentBigTransfer.isFinished()
							|| !agentsToLaunch3.agentBigTransfer.isFinished()
							|| !agentsToLaunch4.agentBigTransfer.isFinished()
							|| !agentsToLaunch5.agentBigTransfer.isFinished()
							|| !agentsToLaunch1.agentBigTransfer.isKilled()
							|| !agentsToLaunch2.agentBigTransfer.isKilled()
							|| !agentsToLaunch3.agentBigTransfer.isKilled()
							|| !agentsToLaunch4.agentBigTransfer.isKilled()
							|| !agentsToLaunch5.agentBigTransfer.isKilled()
							|| lanToSynchro)) {
						lanToSynchro=false;
						sleep(1000);
						System.out.println("------------------------ localDataAmountAcc=" + localDataAmountAcc
								+ " --- globalDataAmountAcc=" + globalDataAmountAcc);
						if (agentsToLaunch1.agentBigTransfer.isFinished()
							&& agentsToLaunch2.agentBigTransfer.isFinished()
							&& agentsToLaunch3.agentBigTransfer.isFinished()
							&& agentsToLaunch4.agentBigTransfer.isFinished()
							&& agentsToLaunch5.agentBigTransfer.isFinished())
						{
							if (agentsToLaunch1.agentBigTransfer.isKilled()
									&& agentsToLaunch2.agentBigTransfer.isKilled()
									&& agentsToLaunch3.agentBigTransfer.isKilled()
									&& agentsToLaunch4.agentBigTransfer.isKilled()
									&& agentsToLaunch5.agentBigTransfer.isKilled())
							{
								System.out.println("All agents in all MK killed");
								int i=0;
								for (Madkit m : getHelperInstances(this, 5))
								{

									if (isAgentsPresentInGroup(m, TestNGMadkit.DEFAULT_NETWORK_GROUP_FOR_ACCESS_DATA, AgentBigTransfer.class, false, AgentBigTransfer.bigTransferRole)) {
										lanToSynchro = true;
										System.out.println("Local agents in MK "+i+" cleaned : "+false);
									}
									else
										System.out.println("Local agents in MK "+i+" cleaned : "+true);
									++i;
								}
								i=0;
								for (Madkit m : getHelperInstances(this, 5))
								{

									if (isAgentsPresentInGroup(m, TestNGMadkit.DEFAULT_NETWORK_GROUP_FOR_ACCESS_DATA, AgentBigTransfer.class, true, AgentBigTransfer.bigTransferRole)) {
										lanToSynchro = true;
										if (nb2>3)
											System.out.print("");
										System.out.println("Network agents in MK "+i+" cleaned : "+false);
									}
									else
										System.out.println("Network agents in MK "+i+" cleaned : "+true);
									++i;
								}
								if (lanToSynchro)
									nb2++;

							}
							else
							{
								System.out.println("Agents in MK 1 killed : "+agentsToLaunch1.agentBigTransfer.isKilled());
								System.out.println("Agents in MK 2 killed : "+agentsToLaunch2.agentBigTransfer.isKilled());
								System.out.println("Agents in MK 3 killed : "+agentsToLaunch3.agentBigTransfer.isKilled());
								System.out.println("Agents in MK 4 killed : "+agentsToLaunch4.agentBigTransfer.isKilled());
								System.out.println("Agents in MK 5 killed : "+agentsToLaunch5.agentBigTransfer.isKilled());
							}
						}
						else {
							System.out.println(agentsToLaunch1.agentBigTransfer);
							System.out.println(agentsToLaunch2.agentBigTransfer);
							System.out.println(agentsToLaunch3.agentBigTransfer);
							System.out.println(agentsToLaunch4.agentBigTransfer);
							System.out.println(agentsToLaunch5.agentBigTransfer);
						}
						AssertJUnit.assertTrue(agentsToLaunch1.agentBigTransfer.isOK());
						AssertJUnit.assertTrue(agentsToLaunch2.agentBigTransfer.isOK());
						AssertJUnit.assertTrue(agentsToLaunch3.agentBigTransfer.isOK());
						AssertJUnit.assertTrue(agentsToLaunch4.agentBigTransfer.isOK());
						AssertJUnit.assertTrue(agentsToLaunch5.agentBigTransfer.isOK());

					}

					AssertJUnit.assertTrue(agentsToLaunch1.agentBigTransfer.isOK());
					AssertJUnit.assertTrue(agentsToLaunch2.agentBigTransfer.isOK());
					AssertJUnit.assertTrue(agentsToLaunch3.agentBigTransfer.isOK());
					AssertJUnit.assertTrue(agentsToLaunch4.agentBigTransfer.isOK());
					AssertJUnit.assertTrue(agentsToLaunch5.agentBigTransfer.isOK());
					sleep(500);
					AssertJUnit.assertTrue(agentsToLaunch1.agentBigTransfer.isKilled());
					AssertJUnit.assertTrue(agentsToLaunch2.agentBigTransfer.isKilled());
					AssertJUnit.assertTrue(agentsToLaunch3.agentBigTransfer.isKilled());
					AssertJUnit.assertTrue(agentsToLaunch4.agentBigTransfer.isKilled());
					AssertJUnit.assertTrue(agentsToLaunch5.agentBigTransfer.isKilled());
					AssertJUnit.assertFalse(lanToSynchro);

					for (Madkit m : getHelperInstances(this, 5))
					{
						AssertJUnit.assertFalse(isAgentsPresentInGroup(m, TestNGMadkit.DEFAULT_NETWORK_GROUP_FOR_ACCESS_DATA, AgentBigTransfer.class, false, AgentBigTransfer.bigTransferRole));
					}
					for (Madkit m : getHelperInstances(this, 5))
					{
						AssertJUnit.assertFalse(isAgentsPresentInGroup(m, TestNGMadkit.DEFAULT_NETWORK_GROUP_FOR_ACCESS_DATA, AgentBigTransfer.class, true, AgentBigTransfer.bigTransferRole));
					}
					for (Madkit m : getHelperInstances(this, 5))
					{
						AssertJUnit.assertTrue(isAgentsPresentInGroup(m, TestNGMadkit.DEFAULT_NETWORK_GROUP_FOR_ACCESS_DATA, NetworkPongAgent.class, true, NetworkPingAgent.pongRole));
						AssertJUnit.assertTrue(isAgentsPresentInGroup(m, TestNGMadkit.DEFAULT_NETWORK_GROUP_FOR_ACCESS_DATA, NetworkPingAgent.class, true, NetworkPingAgent.pingRole));
					}
					if (logger != null)
						logger.info("stoping networks");
					// pause(1000);
					for (Madkit m : getHelperInstances(this, 5))
						stopNetworkWithMadkit(m);
					for (Madkit m : getHelperInstances(this, 5)) {
						checkConnectedKernelsNb(this, m, 0, timeOut);
						checkConnectedIntancesNb(this, m, 0, timeOut);
					}
					for (Madkit m : getHelperInstances(this, 5))
					{
						AssertJUnit.assertFalse(isAgentsPresentInGroup(m, TestNGMadkit.DEFAULT_NETWORK_GROUP_FOR_ACCESS_DATA, null, true, null));
					}

					System.gc();
					System.out.println("second round");
					sleep(800);
					// second round
					index = 0;
					for (Madkit m : getHelperInstances(this, 5)) {
						launchNetwork(m);
						if (index++ == 0)
							sleep(2000);
					}
					index = 0;
					for (Madkit m : getHelperInstances(this, 5)) {

						if (index++ == 0) {
							checkConnectedKernelsNb(this, m, 4, timeOut);
							checkConnectedIntancesNb(this, m, 8, timeOut);
						} else {
							checkConnectedKernelsNb(this, m, 1, timeOut);
							checkConnectedIntancesNb(this, m, 2, timeOut);
						}
					}
					sleep(400);
					for (Madkit m : getHelperInstances(this, 5))
					{
						AssertJUnit.assertTrue(isAgentsPresentInGroup(m, TestNGMadkit.DEFAULT_NETWORK_GROUP_FOR_ACCESS_DATA, NetworkPongAgent.class, true, NetworkPingAgent.pongRole));
						AssertJUnit.assertTrue(isAgentsPresentInGroup(m, TestNGMadkit.DEFAULT_NETWORK_GROUP_FOR_ACCESS_DATA, NetworkPingAgent.class, true, NetworkPingAgent.pingRole));
					}
					for (Madkit m : getHelperInstances(this, 5))
						stopNetworkWithMadkit(m);
					for (Madkit m : getHelperInstances(this, 5)) {
						checkConnectedKernelsNb(this, m, 0, timeOut);
						checkConnectedIntancesNb(this, m, 0, timeOut);
					}

				    for (Madkit m : getHelperInstances(this, 5))
					{
						AssertJUnit.assertFalse(isAgentsPresentInGroup(m, TestNGMadkit.DEFAULT_NETWORK_GROUP_FOR_ACCESS_DATA, null, true, null));
					}

					sleep(400);
					agentsToLaunch1.killAgent(agentsToLaunch1);
					agentsToLaunch2.killAgent(agentsToLaunch2);
					agentsToLaunch3.killAgent(agentsToLaunch3);
					agentsToLaunch4.killAgent(agentsToLaunch4);
					agentsToLaunch5.killAgent(agentsToLaunch5);
					agentsToLaunch1=null;
					agentsToLaunch2=null;
					agentsToLaunch3=null;
					agentsToLaunch4=null;
					agentsToLaunch5=null;

					cleanHelperMDKs(this);
					AssertJUnit.assertEquals(getHelperInstances(this, 0).size(), 0);
					/*
					 * for (Madkit m : getHelperInstances()) checkConnectedKernelsNb(this, m, 0,
					 * timeOut);
					 */
					/*
					 * for (Madkit m : getHelperInstances())
					 * Assert.assertTrue(checkMemoryLeakAfterNetworkStopped(m));
					 */
				} finally {
					removeDatabase();
				}
				sleep(400);
				System.out.println("------------------------ Thread cound at end : " + Thread.activeCount());
			}
		});
		assertMadkitsEmpty();
	}

	protected void removeDatabase() {
		if (eventListener1.databaseFile != null && eventListener1.databaseFile.exists())
			EmbeddedH2DatabaseWrapper.deleteDatabasesFiles(eventListener1.databaseFile);
		if (eventListener2.databaseFile != null && eventListener2.databaseFile.exists())
			EmbeddedH2DatabaseWrapper.deleteDatabasesFiles(eventListener2.databaseFile);
		if (eventListener3.databaseFile != null && eventListener3.databaseFile.exists())
			EmbeddedH2DatabaseWrapper.deleteDatabasesFiles(eventListener3.databaseFile);
		if (eventListener4.databaseFile != null && eventListener4.databaseFile.exists())
			EmbeddedH2DatabaseWrapper.deleteDatabasesFiles(eventListener4.databaseFile);
		if (eventListener5.databaseFile != null && eventListener5.databaseFile.exists())
			EmbeddedH2DatabaseWrapper.deleteDatabasesFiles(eventListener5.databaseFile);
	}
}

class AgentsToLaunch extends AbstractAgent {

	public final NetworkPongAgent networkPingAgent;
	public final AgentBigTransfer agentBigTransfer;

	@SuppressWarnings("unused")
	public AgentsToLaunch(int thisKernelNumber, int distantKernelNumber, boolean includeMoreThanOneBigDataTest) {
		this.networkPingAgent = new NetworkPongAgent(distantKernelNumber);
		agentBigTransfer = new AgentBigTransfer(0, true, true, false, true, true, distantKernelNumber, false,
				new AgentBigTransfer(1, true, false, false, includeMoreThanOneBigDataTest,
						!includeMoreThanOneBigDataTest, distantKernelNumber, false,
						new AgentBigTransfer(2, true, false, true, includeMoreThanOneBigDataTest,
								!includeMoreThanOneBigDataTest, distantKernelNumber, false,
								new AgentBigTransfer(3, false, false, false, includeMoreThanOneBigDataTest,
										!includeMoreThanOneBigDataTest, distantKernelNumber, false,
										new AgentBigTransfer(4, false, false, false, includeMoreThanOneBigDataTest,
												!includeMoreThanOneBigDataTest, distantKernelNumber, false,
												new AgentBigTransfer(5, false, false, true,
														includeMoreThanOneBigDataTest, !includeMoreThanOneBigDataTest,
														distantKernelNumber, false,
														new AgentBigTransfer(6, true, true, false, true, true,
																distantKernelNumber, true,
																new AgentBigTransfer(7, true, false, false,
																		includeMoreThanOneBigDataTest,
																		!includeMoreThanOneBigDataTest,
																		distantKernelNumber, true,
																		new AgentBigTransfer(8, true, false, true,
																				includeMoreThanOneBigDataTest,
																				!includeMoreThanOneBigDataTest,
																				distantKernelNumber, true,
																				new AgentBigTransfer(9, false, false,
																						false,
																						includeMoreThanOneBigDataTest,
																						!includeMoreThanOneBigDataTest,
																						distantKernelNumber, true,
																						new AgentBigTransfer(10, false,
																								false, false,
																								includeMoreThanOneBigDataTest,
																								!includeMoreThanOneBigDataTest,
																								distantKernelNumber,
																								true,
																								new AgentBigTransfer(11,
																										false, false,
																										true,
																										includeMoreThanOneBigDataTest,
																										!includeMoreThanOneBigDataTest,
																										distantKernelNumber,
																										true,
																										null))))))))))));
	}

	@Override
	public void activate() {
		launchAgent(networkPingAgent);
	}

	@Override
	protected void end() {
		//noinspection StatementWithEmptyBody
		while (nextMessage()!=null);
	}

	public ReturnCode launchBigDataTransferAgent() {
		return launchAgent(agentBigTransfer);
	}
}
