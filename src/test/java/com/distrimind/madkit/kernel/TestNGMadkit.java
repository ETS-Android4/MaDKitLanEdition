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
package com.distrimind.madkit.kernel;

import com.distrimind.madkit.action.KernelAction;
import com.distrimind.madkit.agr.LocalCommunity;
import com.distrimind.madkit.agr.Organization;
import com.distrimind.madkit.kernel.AbstractAgent.ReturnCode;
import com.distrimind.madkit.kernel.AbstractAgent.State;
import com.distrimind.madkit.kernel.network.Connection;
import com.distrimind.madkit.kernel.network.NetworkEventListener;
import com.distrimind.madkit.message.KernelMessage;
import com.distrimind.madkit.testing.util.agent.ForEverAgent;
import com.distrimind.ood.database.DatabaseFactory;
import com.distrimind.ood.database.exceptions.DatabaseException;
import com.distrimind.util.Timer;
import com.distrimind.util.concurrent.LockerCondition;
import com.distrimind.util.concurrent.ScheduledPoolExecutor;
import org.testng.Assert;
import org.testng.AssertJUnit;
import org.testng.annotations.BeforeMethod;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;

import static com.distrimind.madkit.kernel.AbstractAgent.ReturnCode.SUCCESS;
import static com.distrimind.madkit.kernel.AbstractAgent.State.TERMINATED;

/**
 * @author Fabien Michel
 * @author Jason Mahdjoub
 * @since MaDKit 5.0.0.7
 * @version 0.9
 * @version 1.0
 * 
 */
public class TestNGMadkit {

	public String testName = "";

	@BeforeMethod
	public void handleTestMethodName(Method method)
	{
		testName = method.getName();
	}

	/**
	 * 	 */
	public static String aString = "a";
	public static final String C = "Tcommunity";
	public static final String C2 = "Tcommunity2";
	public static final String G = "Tgroup";
	public static final String G2 = "Tgroup2";
	public static final String GB = "TgroupB";
	public static final String NGDAccessData = "TgroupNGDAccessData";
	public static final String NGDLoginData = "TgroupNGDLoginData";
	public static final String NGLD = "TgroupNGLD";
	public static final Group GROUP = new Group(true, null, false, C, G);
	public static final Group GROUPB = new Group(true, null, false, C, GB);
	public static final Group GROUP2 = new Group(C2, G2);
	public static final Group DEFAULT_NETWORK_GROUP_FOR_ACCESS_DATA = new Group(true, null, false, C, NGDAccessData);

	public static final Group NETWORK_GROUP_FOR_LOGIN_DATA = new Group(true, null, false, C, NGLD);
	public static final String ROLE = "Trole";
	public static final String ROLE2 = "Trole2";

	public static String testTitle;
	//protected Madkit madkit;
	private final List<Madkit> madkits=Collections.synchronizedList(new ArrayList<>());
	private static final ArrayList<Madkit> helperInstances = new ArrayList<>();



	public void addHelperInstance(Madkit m) {
		synchronized (helperInstances) {
			helperInstances.add(m);
			helperInstances.notifyAll();
		}
	}

	ArrayList<Madkit> getHelperInstances() {
		synchronized (helperInstances) {
			return new ArrayList<>(helperInstances);
		}
	}

	public ArrayList<Madkit> getHelperInstances(AbstractAgent requester, int nb) throws InterruptedException {
		return getHelperInstances(requester, nb, 10000);
	}

	public ArrayList<Madkit> getHelperInstances(AbstractAgent requester, final int nb, long delay) throws InterruptedException {

		try {
			requester.wait(new LockerCondition(helperInstances) {
				@Override
				public boolean isLocked() {
					return helperInstances.size() != nb;
				}
			}, delay);
		} catch (TimeoutException ignored) {

		}
		AssertJUnit.assertEquals(nb, helperInstances.size());
		return new ArrayList<>(helperInstances);
	}

	public void closePeers(AbstractAgent launcher) throws InterruptedException {
		for (Madkit mk : getHelperInstances(launcher, 2))
			stopNetworkWithMadkit(mk);
		for (Madkit mk : getHelperInstances(launcher, 2)) {
			checkConnectedKernelsNb(launcher, mk, 0, 30000);
			checkConnectedIntancesNb(launcher, mk, 0, 30000);
		}
		launcher.sleep(400);
		cleanHelperMDKs(launcher);
		AssertJUnit.assertEquals(getHelperInstances(launcher, 0).size(), 0);
	}

	protected List<String> mkArgs;

	@BeforeMethod
	public void setMkArgs()
	{
		mkArgs = new ArrayList<>(Arrays.asList(
				// "--"+Madkit.warningLogLevel,"INFO",
				"--desktop", "false", "--forceDesktop", "true", "--launchAgents",
				"{com.distrimind.madkit.kernel.AbstractAgent}", // to not have the desktop mode by
				// default
				"--logDirectory", getBinTestDir(), "--agentLogLevel", "ALL", "--madkitLogLevel", "INFO"));
	}

	private static final List<Process> externalProcesses = new ArrayList<>();

	public Madkit launchTest(AbstractAgent a, ReturnCode expected, boolean gui) {
		return launchTest(a, expected, gui, _properties -> {

		});
	}

	public Madkit launchTest(AbstractAgent a, ReturnCode expected, boolean gui, Runnable postTest) {
		return launchTest(a, expected, gui, _properties -> {

		}, postTest);
	}

	public static void setDatabaseFactory(MadkitProperties p, DatabaseFactory<?> df) throws DatabaseException {
		p.setDatabaseFactory(df);
	}

	private static volatile boolean oneFailed = false;

	public static boolean isOneFailed() {
		return oneFailed;
	}

	public Madkit launchTest(AbstractAgent a, ReturnCode expected, boolean gui, MadkitEventListener eventListener) {
		return launchTest(a, expected, gui, eventListener, null);
	}

	public Madkit launchTest(AbstractAgent a, ReturnCode expected, boolean gui, MadkitEventListener eventListener,
			Runnable postTest) {
		System.out
				.println("\n\n------------------------ " + testName + " TEST START ---------------------");
		Madkit madkit = null;
		try {
			String[] args = null;
			if (mkArgs != null) {
				args = mkArgs.toArray(new String[0]);
			}

			madkits.add(madkit = new Madkit(eventListener, args));
			AbstractAgent kernelAgent = madkit.getKernel()
					.getAgentWithRole(null, LocalCommunity.Groups.SYSTEM, Organization.GROUP_MANAGER_ROLE).getAgent();
			a.setName(testName);
			AssertJUnit.assertEquals(expected, kernelAgent.launchAgent(a, gui));
			if (postTest != null)
				postTest.run();
			if (testFailed) {
				if (testException != null) {
					testException.printStackTrace();
				}
				oneFailed = true;
				Assert.fail();
			}
		} catch (Throwable e) {
			Throwable throwable = e;
			System.out.println("\n\n\n------------------------------------");
			while (throwable.getCause() != null)
				throwable = throwable.getCause();
			throwable.printStackTrace();
			System.out.println("------------------------------------\n\n\n");
			oneFailed = true;

			AssertJUnit.fail(TestNGMadkit.class.getSimpleName()+" ; "+ throwable.getMessage());

		} finally {
			System.out.println("\n\n------------------------ " + testName
					+ " TEST FINISHED ---------------------\n\n");

			cleanHelperMDKs(a);
			closeMadkit(madkit);
			madkits.remove(madkit);

		}
		/*
		 * if (madkit.getKernel().isAlive()) madkit.doAction(KernelAction.EXIT);
		 */

		return madkit;
	}

	public void assertMadkitsEmpty()
	{
		AssertJUnit.assertTrue(madkits.isEmpty());
	}

	public void lineBreak() {
		System.out.println("---------------------------------");
	}

	public void assertKernelIsAlive(MadkitKernel m) {
		AssertJUnit.assertTrue(m.isAlive());
	}

	public void assertKernelIsAlive(KernelAddress ka) {
		for (Madkit m : madkits) {
			if (m.getKernelAddress().equals(ka))
				assertKernelIsAlive(m.getKernel());
		}
	}

	public static void noExceptionFailure() {
		Assert.fail("Exception not thrown");
	}

	public Madkit launchTest(AbstractAgent a) {
		return launchTest(a, _properties -> {if (_properties.configFiles==null) _properties.configFiles=new ArrayList<>();
		});
	}

	public ScheduledPoolExecutor getScheduledPoolExecutor(AbstractAgent agent)
	{
		return agent.getMadkitKernel().getMaDKitServiceExecutor();
	}

	public static KernelAddress getKernelAddressInstance() {
		return new KernelAddress(false);
	}

	public Madkit launchTest(AbstractAgent a, MadkitEventListener eventListener) {
		return launchTest(a, SUCCESS, eventListener);
	}

	public Madkit launchTest(AbstractAgent a, ReturnCode expected) {
		return launchTest(a, expected, false, _properties -> {
		});
	}

	public Madkit launchTest(AbstractAgent a, ReturnCode expected, MadkitEventListener eventListener) {
		return launchTest(a, expected, false, eventListener);
	}

	public void launchDefaultAgent(AbstractAgent a) {
		a.launchAgent(new AbstractAgent() {
			@Override
			protected void activate() {
				createGroup(GROUP);
				requestRole(GROUP, ROLE);
			}
		});
	}

	public void everythingOK() {
		if (testFailed) {
			if (testException != null) {
				testException.printStackTrace();
			}
			Assert.fail();
		}
	}

	@SuppressWarnings("UnusedReturnValue")
	public Madkit launchTest(AbstractAgent a, boolean all) {
		if (all) {
			addMadkitArgs("--agentLogLevel", "ALL");
			addMadkitArgs("--kernelLogLevel", "FINEST");
		} else {
			addMadkitArgs("--agentLogLevel", "INFO");
			addMadkitArgs("--kernelLogLevel", "OFF");
		}
		return launchTest(a, SUCCESS);
	}

	public AbstractAgent getKernel(Madkit m) {
		return m.getKernel();
	}

	public AbstractAgent getKernel(KernelAddress ka)
	{
		for (Madkit m : madkits)
			if (m.getKernelAddress().equals(ka))
				return m.getKernel();
		return null;
	}

	public void addMadkitArgs(String... string) {
		mkArgs.addAll(Arrays.asList(string));
	}

	@SuppressWarnings("SameReturnValue")
	public static String getBinTestDir() {
		return "bin";
	}

	public void test() {
		launchTest(new AbstractAgent());
	}

	public static String aa() {
		return aString += "a";
	}

	static long time;

	public static boolean testFailed = false;

	private static Throwable testException = null;

	public static void startTimer() {
		time = System.nanoTime();
	}


	public static long stopTimer(String message) {
		final long t = System.nanoTime() - time;
		System.out.println(message + (t / 1000000) + " ms");
		return (t / 1000000);
	}

	public void assertAgentIsTerminated(AbstractAgent a) {
		System.out.println(a);
		AssertJUnit.assertEquals(TERMINATED, a.getState());
		AssertJUnit.assertFalse(a.isAlive());
	}

	public void assertAgentIsZombie(AbstractAgent a) {
		System.out.println(a);
		AssertJUnit.assertEquals(State.ZOMBIE, a.getState());
		// assertFalse(a.isAlive());
	}

	static public void printMemoryUsage() {
		// System.gc();
		long mem = (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory());
		System.out.println("\n----used memory: " + Long.toString(mem).substring(0, 3) + " Mo\n");
	}


	public static void pause(AbstractAgent agent, int millis) {
		try {
			if (agent != null)
				agent.sleep(millis);
			else
				Thread.sleep(millis);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

	}

	public static void printAllStacks() {
		for (Map.Entry<Thread, StackTraceElement[]> t : Thread.getAllStackTraces().entrySet()) {
			System.out.println("------------- " + t.getKey());
			for (StackTraceElement ste : t.getValue()) {
				System.out.println(ste);
			}
		}
	}

	public static void createDefaultCGR(AbstractAgent a) {
		a.createGroup(GROUP, null);
		try {
			AssertJUnit.assertEquals(SUCCESS, a.requestRole(GROUP, ROLE, null));
		} catch (AssertionError e) {
			TestNGMadkit.testFails(e);
		}
	}

	public static void testFails(Throwable a) {
		testFailed = true;
		testException = a;
	}

	public void launchThreadedMKNetworkInstance(final Level l, final NetworkEventListener networkEventListener) {
		launchThreadedMKNetworkInstance(l, ForEverAgent.class, null, networkEventListener);
	}

	public void launchThreadedMKNetworkInstance(final Level l, final Class<? extends AbstractAgent> agentClass,
			final AbstractAgent agentToLaunch, final NetworkEventListener networkEventListener) {
		this.launchThreadedMKNetworkInstance(l, agentClass, agentToLaunch, networkEventListener, null);
	}

	public void launchThreadedMKNetworkInstance(final Level l, final Class<? extends AbstractAgent> agentClass,
			final AbstractAgent agentToLaunch, final NetworkEventListener networkEventListener,
			final KernelAddress kernelAddress) {
		AtomicReference<Madkit> mkReference=new AtomicReference<>();
		Thread t=new Thread(() -> {
			try {
				launchCustomNetworkInstance(l, agentClass, agentToLaunch, networkEventListener, kernelAddress, mkReference);
			} catch (IOException e) {
				e.printStackTrace();
				Assert.fail();
			}
		});
		t.setName("Madkit thread launcher");
		t.start();
		synchronized (helperInstances) {

			while(!helperInstances.contains(mkReference.get()))
			{
				try {
					helperInstances.wait();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
	}

	public KernelAddress getKernelAddress(Madkit m) {
		return m.getKernel().getKernelAddress();
	}

	public void closeMadkit(Madkit m) {
		if (m == null)
			return;
		if (m.getKernel().isAlive())
			m.doAction(KernelAction.STOP_NETWORK);

		checkConnectedKernelsNb(null, m, 0, 20000);
		AssertJUnit.assertTrue(checkMemoryLeakAfterNetworkStopped(m));

		checkConnectedIntancesNb(null, m, 0, 20000);

		checkNumberOfNetworkAgents(null, m, 0, 20000);

		if (m.getKernel().isAlive())
			m.doAction(KernelAction.EXIT);

		checkKilledKernelsNb(null, m, 30000);

		checkEmptyConversationIDTraces(null, m, 10000);

		checkReleasedGroups(null, m);

		checkDisabledAsynchronousTransfers(null, m, 10000);
	}

	public void cleanHelperMDKs(AbstractAgent agent) {
		synchronized (helperInstances) {
			if (!helperInstances.isEmpty() || !externalProcesses.isEmpty()) {
				try {
					for (Madkit m : helperInstances) {
						if (m.getKernel().isAlive())
							m.doAction(KernelAction.STOP_NETWORK);
					}
					for (Madkit m : helperInstances) {

						checkConnectedKernelsNb(agent, m, 0, 20000);
						AssertJUnit.assertTrue(checkMemoryLeakAfterNetworkStopped(m));
					}
					for (Madkit m : helperInstances) {
						checkConnectedIntancesNb(agent, m, 0, 20000);
					}

					for (Madkit m : helperInstances) {
						checkNumberOfNetworkAgents(agent, m, 0, 20000);
					}

					for (Madkit m : helperInstances) {
						if (m.getKernel().isAlive())
							m.doAction(KernelAction.EXIT);
					}
					for (Madkit m : helperInstances) {
						checkKilledKernelsNb(agent, m, 10000);
					}
					for (Madkit m : helperInstances) {
						checkEmptyConversationIDTraces(agent, m, 30000);
					}
					for (Madkit m : helperInstances) {
						checkReleasedGroups(agent, m);
					}
					for (Madkit m : helperInstances) {
						checkDisabledAsynchronousTransfers(agent, m, 10000);
					}

					for (Process p : externalProcesses) {
						p.destroy();
						try {
							p.waitFor();
							Assert.assertFalse(p.isAlive());
						} catch (InterruptedException e) {
							e.printStackTrace();
							Assert.fail();
						}
					}



				} finally {
					helperInstances.clear();
					externalProcesses.clear();
					testException=null;
					helperInstances.notifyAll();
					// pause(agent, pauseTime);
					System.out.println("------------Cleaning help instances done ---------------------\n\n");
				}
			}
		}
	}

	/*
	 * public static void cleanHelperMDKs(AbstractAgent agent){
	 * cleanHelperMDKs(agent, 100); }
	 */
	public void cleanHelperMDKs(AbstractAgent agent, int pause) {
		cleanHelperMDKs(agent);
		pause(agent, pause);
	}

	public boolean isAgentsPresentInGroupRole(Madkit m, Group group, String role)
	{
		InternalRole r;
		try {
			r = m.getKernel().getRole(group, role);
		} catch (CGRNotAvailable cgrNotAvailable) {
			return false;
		}
		return !r.getAgentsList().isEmpty();
	}
	public boolean isAgentsPresentInGroup(Madkit m, Group group)
	{
		return isAgentsPresentInGroup(m, group, null, null, null);
	}
	public boolean isAgentsPresentInGroup(Madkit m, Group group, Class<?> agentClass, Boolean isNetwork, String roleStartWith)
	{

		try {
			synchronized (m.getKernel().getOrganizations()) {
				InternalGroup g = m.getKernel().getGroup(group);
				for (InternalRole ir : g.values()) {

					if ((roleStartWith == null || ir.getRoleName().startsWith(roleStartWith)) && !ir.getAgentAddressesCopy().isEmpty()) {
						if (agentClass != null || isNetwork != null) {
							for (AgentAddress aa : ir.getAgentAddressesCopy()) {
								if (agentClass != null && (isNetwork == null || !isNetwork) && aa.getAgent() != null && agentClass.isAssignableFrom(aa.getAgent().getClass())) {
									System.out.println(aa);
									return true;
								}
								if (isNetwork != null && aa.getKernelAddress().equals(m.getKernelAddress()) != isNetwork) {
									System.out.println(aa);
									return true;
								}
							}

							continue;
						}
						return true;
					}
				}
			}
		} catch (CGRNotAvailable cgrNotAvailable) {
			return false;
		}
		return false;
	}



	public void cleanHelperMDKs() {
		cleanHelperMDKs(null);
	}

	public void launchThreadedMKNetworkInstance(final NetworkEventListener networkEventListener) {
		launchThreadedMKNetworkInstance(Level.OFF, networkEventListener);
	}

	public Madkit launchMKNetworkInstance(final NetworkEventListener networkEventListener) throws IOException {
		return launchMKNetworkInstance(Level.INFO, networkEventListener);
	}

	public Madkit launchMKNetworkInstance(Level l, final NetworkEventListener networkEventListener) throws IOException {
		return launchCustomNetworkInstance(l, ForEverAgent.class, null, networkEventListener, null, null);
	}

	public Madkit launchCustomNetworkInstance(final Level l, final Class<? extends AbstractAgent> agentTolaunch,
			final AbstractAgent agentToLaunch, final NetworkEventListener networkEventListener,
			KernelAddress kernelAddress, AtomicReference<Madkit> mkReference) throws IOException {
		Madkit m = new Madkit(Madkit.generateDefaultMadkitConfig(), kernelAddress, _properties -> {
			_properties.networkProperties.network = true;
			_properties.networkProperties.networkLogLevel = l;
			_properties.launchAgents = new ArrayList<>();
			_properties.launchAgents.add(new AgentToLaunch(agentTolaunch, false, 1));
			_properties.kernelLogLevel = l;
			_properties.networkProperties.upnpIGDEnabled = false;
			networkEventListener.onMaDKitPropertiesLoaded(_properties);
		});
		if (mkReference!=null)
			mkReference.set(m);
		if (agentToLaunch != null)
			m.getKernel().launchAgent(agentToLaunch);
		addHelperInstance(m);
		return m;
	}

	public void launchExternalNetworkInstance() {
		launchExternalNetworkInstance(ForEverAgent.class);
	}

	public void launchExternalNetworkInstance(Class<? extends AbstractAgent> agentTolaunch) {
		launchExternalMDKInstance("--createLogFiles", "--kernelLogLevel.network", "--launchAgents",
				"{" + agentTolaunch.getCanonicalName() + "}");
	}

	public void launchExternalMDKInstance(String... args) {
		StringBuilder cmdLince = new StringBuilder("java -Xms1024m -cp bin:build/test/classes:lib/junit-4.12.jar:lib/hamcrest-core-1.3.jar madkit.kernel.Madkit");
		for (String string : args) {
			cmdLince.append(" ").append(string);
		}
		try {
			Process p = Runtime.getRuntime().exec(cmdLince.toString());
			externalProcesses.add(p);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void stopNetworkWithMadkit(Madkit m) {
		m.getKernel().receiveMessage(new KernelMessage(KernelAction.STOP_NETWORK));
	}

	public void launchNetwork(Madkit m) {
		m.getKernel().receiveMessage(new KernelMessage(KernelAction.LAUNCH_NETWORK));
	}

	public int getEffectiveConnections(Madkit m) {
		Set<Connection> l = m.getKernel().getEffectiveConnections((AbstractAgent) null);
		if (l == null)
			return 0;
		else
			return l.size();
	}

	public void checkConnectedIntancesNb(AbstractAgent agent, Madkit m, int nb, long timeout) {
		Set<Connection> l;
		Timer t = new Timer(true);

		do {
			l = m.getKernel().getEffectiveConnections((AbstractAgent) null);
			if (l != null) {
				System.out.println(m.getKernel() + " : connected instances =" + l.size() + " (expected=" + nb + ")");
				for (Madkit m2 : getHelperInstances()) {
					System.out.println("\t" + m2 + ". Effective connections : " + getEffectiveConnections(m2));
				}

			} else
				System.out.println("others =0");
			if (t.getMilli() < timeout && (l == null || l.size() != nb))
				pause(agent, 1000);
		} while (t.getMilli() < timeout && (l == null || l.size() != nb));
		assert l != null;
		AssertJUnit.assertEquals(nb, l.size());
	}

	public void checkConnectedKernelsNb(AbstractAgent agent, Madkit m, int nb, long timeout) {
		Set<KernelAddress> l;
		Timer t = new Timer(true);

		do {
			l = m.getKernel().getAvailableDistantKernels();
			if (l != null) {
				System.out.println(m.getKernel() + " : connected kernels=" + l.size() + " (expected=" + nb + ")");
			} else
				System.out.println("others =0");
			if (t.getMilli() < timeout && (l == null || l.size() != nb))
				pause(agent, 1000);
		} while (t.getMilli() < timeout && (l == null || l.size() != nb));
		assert l != null;
		AssertJUnit.assertEquals(nb, l.size());
	}

	public void checkNumberOfNetworkAgents(AbstractAgent agent, Madkit m, int nbExpected, long timeout) {
		int nb;
		Timer t = new Timer(true);

		do {
			nb = getNumberOfNetworkAgents(m);
			if (nb != nbExpected) {
				System.out.println(
						m.getKernel() + " : connected network agents=" + nb + " (expected=" + nbExpected + ")");
			} else
				System.out.println("others =0");
			if (t.getMilli() < timeout && nb != nbExpected)
				pause(agent, 1000);
		} while (t.getMilli() < timeout && nb != nbExpected);
		AssertJUnit.assertEquals(nb, nbExpected);
	}

	private int getNumberOfNetworkAgents(Madkit m) {
		ArrayList<AbstractAgent> c = m.getKernel().getConnectedNetworkAgents();
		for (AbstractAgent aa : c) {
			System.out.println(m.getKernel() + "\t\t connected network agent : " + aa);
		}
		return c.size();
	}

	public void checkKilledKernelsNb(AbstractAgent agent, Madkit m, long timeout) {

		Timer t = new Timer(true);

		do {
			if (t.getMilli() < timeout && m.getKernel().getState()!= TERMINATED) {
				System.out.println(m+" : state="+m.getKernel().getState());
				pause(agent, 1000);
			}
		} while (t.getMilli() < timeout && m.getKernel().getState()!= TERMINATED);
		AssertJUnit.assertSame(TERMINATED, m.getKernel().getState());
	}
	public void checkDisabledAsynchronousTransfers(AbstractAgent agent, Madkit m, long timeout) {

		Timer t = new Timer(true);

		do {
			if (t.getMilli() < timeout && m.getKernel().getTransferIdsPerInternalAsynchronousId().size()!=0) {
				System.out.println(m+" : NumberOfTransferIdsPerInternalAsynchronousId="+m.getKernel().getTransferIdsPerInternalAsynchronousId().size());
				pause(agent, 1000);
			}
		} while (t.getMilli() < timeout && m.getKernel().getTransferIdsPerInternalAsynchronousId().size()!=0);
		AssertJUnit.assertSame(0, m.getKernel().getTransferIdsPerInternalAsynchronousId().size());
	}

	public void checkEmptyConversationIDTraces(AbstractAgent agent, Madkit m, long timeout) {

		Timer t = new Timer(true);
		System.gc();
		do {
			if (t.getMilli() < timeout && !m.getKernel().isGlobalInterfacedIDsEmpty()) {
				System.out.println(m.getKernel() + " : global conversation ID interfaces empty = false");
				System.gc();
				System.gc();
				pause(agent, 1000);
			}
		} while (t.getMilli() < timeout && !m.getKernel().isGlobalInterfacedIDsEmpty());
		AssertJUnit.assertTrue(m.getKernel().getGlobalInterfacedIDs().isEmpty());
	}

	public void checkReleasedGroups(AbstractAgent agent, Madkit m) {
		AssertJUnit.assertFalse(LocalCommunity.Groups.AGENTS_SOCKET_GROUPS.hasMadKitTraces(m.kernelAddress));
		AssertJUnit.assertFalse(LocalCommunity.Groups.DISTANT_KERNEL_AGENTS_GROUPS.hasMadKitTraces(m.kernelAddress));
		AssertJUnit.assertFalse(LocalCommunity.Groups.GUI.hasMadKitTraces(m.kernelAddress));
		AssertJUnit.assertFalse(LocalCommunity.Groups.KERNELS.hasMadKitTraces(m.kernelAddress));
		AssertJUnit.assertFalse(LocalCommunity.Groups.LOCAL_NETWORKS.hasMadKitTraces(m.kernelAddress));
		AssertJUnit.assertFalse(LocalCommunity.Groups.NETWORK.hasMadKitTraces(m.kernelAddress));
		AssertJUnit.assertFalse(LocalCommunity.Groups.NETWORK_INTERFACES.hasMadKitTraces(m.kernelAddress));
		AssertJUnit.assertFalse(LocalCommunity.Groups.SYSTEM.hasMadKitTraces(m.kernelAddress));
		AssertJUnit.assertFalse(LocalCommunity.Groups.SYSTEM_ROOT.hasMadKitTraces(m.kernelAddress));
		AssertJUnit.assertFalse(LocalCommunity.Groups.TASK_AGENTS.hasMadKitTraces(m.kernelAddress));

		AssertJUnit.assertFalse(TestNGMadkit.GROUP.hasMadKitTraces(m.kernelAddress));
		AssertJUnit.assertFalse(TestNGMadkit.GROUP2.hasMadKitTraces(m.kernelAddress));
		AssertJUnit.assertFalse(TestNGMadkit.DEFAULT_NETWORK_GROUP_FOR_ACCESS_DATA.hasMadKitTraces(m.kernelAddress));
		AssertJUnit.assertFalse(TestNGMadkit.NETWORK_GROUP_FOR_LOGIN_DATA.hasMadKitTraces(m.kernelAddress));

	}

	public boolean checkMemoryLeakAfterNetworkStopped(Madkit m) {
		return m.getKernel().checkMemoryLeakAfterNetworkStopped();
	}

}
