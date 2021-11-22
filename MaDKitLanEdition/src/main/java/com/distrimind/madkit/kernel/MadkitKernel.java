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


import com.distrimind.madkit.action.GlobalAction;
import com.distrimind.madkit.action.KernelAction;
import com.distrimind.madkit.agr.LocalCommunity;
import com.distrimind.madkit.agr.LocalCommunity.Groups;
import com.distrimind.madkit.agr.LocalCommunity.Roles;
import com.distrimind.madkit.database.AsynchronousBigDataTable;
import com.distrimind.madkit.database.AsynchronousMessageTable;
import com.distrimind.madkit.exceptions.MadkitException;
import com.distrimind.madkit.gui.AgentStatusPanel;
import com.distrimind.madkit.gui.ConsoleAgent;
import com.distrimind.madkit.gui.menu.AgentLogLevelMenu;
import com.distrimind.madkit.i18n.ErrorMessages;
import com.distrimind.madkit.kernel.AbstractAgent.ReturnCode;
import com.distrimind.madkit.kernel.ConversationID.InterfacedIDs;
import com.distrimind.madkit.kernel.network.*;
import com.distrimind.madkit.kernel.network.connection.access.CloudIdentifier;
import com.distrimind.madkit.kernel.network.connection.access.Identifier;
import com.distrimind.madkit.kernel.network.connection.access.ListGroupsRoles;
import com.distrimind.madkit.kernel.network.connection.access.PairOfIdentifiers;
import com.distrimind.madkit.message.BooleanMessage;
import com.distrimind.madkit.message.KernelMessage;
import com.distrimind.madkit.message.ObjectMessage;
import com.distrimind.madkit.message.hook.*;
import com.distrimind.madkit.message.hook.HookMessage.AgentActionEvent;
import com.distrimind.madkit.message.task.TasksExecutionConfirmationMessage;
import com.distrimind.madkit.util.XMLUtilities;
import com.distrimind.ood.database.exceptions.DatabaseException;
import com.distrimind.ood.database.exceptions.DatabaseLoadingException;
import com.distrimind.util.AbstractDecentralizedIDGenerator;
import com.distrimind.util.IDGeneratorInt;
import com.distrimind.util.Reference;
import com.distrimind.util.Utils;
import com.distrimind.util.concurrent.LockerCondition;
import com.distrimind.util.concurrent.ScheduledPoolExecutor;
import com.distrimind.util.crypto.MessageDigestType;
import com.distrimind.util.crypto.fortuna.Fortuna;
import com.distrimind.util.io.RandomInputStream;
import com.distrimind.util.io.SecureExternalizable;
import com.distrimind.util.properties.PropertiesParseException;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.net.InetAddress;
import java.net.URL;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.logging.Level;

import static com.distrimind.madkit.kernel.AbstractAgent.ReturnCode.*;
import static com.distrimind.madkit.kernel.AbstractAgent.State.*;
import static com.distrimind.madkit.kernel.CGRSynchro.Code.*;

/**
 * The brand new MaDKit kernel and it is now a real Agent :)
 * 
 * @author Fabien Michel
 * @author Jason Mahdjoub
 * @version 2.5
 * @since MaDKit 5.0
 * @since MaDKitLanEdition 1.0
 * 
 */
@SuppressWarnings({"SameParameterValue", "SynchronizationOnLocalVariableOrMethodParameter", "UnusedReturnValue"})
class MadkitKernel extends Agent {

	final ThreadGroup SYSTEM = new ThreadGroup("MK_SYSTEM") {
		public void uncaughtException(Thread t, Throwable e) {
			System.err.println("\n------------uncaught exception on " + t);
			e.printStackTrace();
		}
	};

	public static final int DEFAULT_THREAD_PRIORITY = Thread.NORM_PRIORITY;

	// final static ThreadGroup A_LIFE = new ThreadGroup("A_LIFE"){//TODO
	// duplicate
	// public void uncaughtException(Thread t, Throwable e) {
	// System.err.println(t);
	// if(e instanceof KilledException){
	// e.printStackTrace();
	// }
	// else{
	// System.err.println("--------------internal BUG--------------------");
	// System.err.println(t);
	// e.printStackTrace();
	// };
	// }
	// };

	/*
	 * final static private ThreadPoolExecutor serviceExecutor = new
	 * ThreadPoolExecutor( // Runtime.getRuntime().availableProcessors() + 1, 2,
	 * Integer.MAX_VALUE, 4L, TimeUnit.SECONDS, new SynchronousQueue<Runnable>(),
	 * new ThreadFactory() { public Thread newThread(Runnable r) { final Thread t =
	 * new Thread(SYSTEM, r); t.setPriority(DEFAULT_THREAD_PRIORITY);
	 * t.setName(SYSTEM.getName()); t.setDaemon(true); //
	 * t.setUncaughtExceptionHandler(new UncaughtExceptionHandler() { // @Override
	 * // public void uncaughtException(Thread t, Throwable e) { //
	 * e.printStackTrace(); // e.getCause().printStackTrace(); // } // }); return t;
	 * } });
	 */

	protected volatile int threadPriorityForServiceExecutor = DEFAULT_THREAD_PRIORITY;
	private ScheduledPoolExecutor serviceExecutor;

	//private PoolExecutor lifeExecutor/* , lifeExecutorWithBlockQueue */;
    private volatile int maximumGlobalUploadSpeedInBytesPerSecond;
    private volatile int maximumGlobalDownloadSpeedInBytesPerSecond;
    private volatile boolean hasSpeedLimitation;


	void setThreadPriorityForServiceExecutor(int _priority) {
		threadPriorityForServiceExecutor = _priority;
		serviceExecutor.setThreadsPriority(_priority);
	}

	ScheduledPoolExecutor getMaDKitServiceExecutor() {
		return serviceExecutor;
	}

	private static ScheduledPoolExecutor createSchedulerServiceExecutor(int corePoolSize,
			ThreadFactory threadFactory, long timeOutSeconds) {

		return new ScheduledPoolExecutor(corePoolSize,
				Runtime.getRuntime().availableProcessors(),
				timeOutSeconds,
				TimeUnit.SECONDS,
				threadFactory);
	}

	private static ScheduledPoolExecutor createSchedulerServiceExecutor(final ThreadGroup SYSTEM,
																			  final int threadPriorityForServiceExecutor, final boolean daemon, final String threadName,
																			  int corePoolSize, long timeOutSeconds,
																			  final AgentThreadFactory agentThreadFactory) {
		return createSchedulerServiceExecutor(corePoolSize, r -> {
			final Thread t = new Thread(agentThreadFactory == null ? SYSTEM : agentThreadFactory.getThreadGroup(),
					r);
			t.setPriority(threadPriorityForServiceExecutor);
			t.setName(threadName);
			t.setDaemon(daemon);
			return t;
		}, timeOutSeconds);
	}


	private final ConcurrentHashMap<String, Organization> organizations;
	final private Set<Overlooker<? extends AbstractAgent>> operatingOverlookers;
	
	protected Madkit platform;
	final private KernelAddress kernelAddress;

	protected MadkitKernel loggedKernel;
	protected volatile boolean shutDown = false;
	protected final AgentThreadFactory normalAgentThreadFactory;
	private final AgentThreadFactory daemonAgentThreadFactory;

	private AgentAddress netAgent;
	// my private addresses for optimizing the message building
	private AgentAddress netUpdater, netEmitter, kernelRole, netSecurity, netCentralDatabaseBackupReceiverChecker;
	final private Set<Agent> threadedAgents;

	private final Map<AgentActionEvent, Set<AbstractAgent>> hooks = Collections
			.synchronizedMap(new EnumMap<>(AgentActionEvent.class));

	private final AtomicBoolean auto_create_group = new AtomicBoolean(true);
	final HashMap<AbstractAgent, AutoRequestedGroups> auto_requested_groups = new HashMap<>();

	private final Set<Connection> availableConnections = Collections.synchronizedSet(new HashSet<>());
	private final Set<KernelAddress> distantKernelAddresses = Collections.synchronizedSet(new HashSet<>());
	private final Map<KernelAddress, Set<Group>> distantAccessibleGroupsGivenByDistantPeer = Collections
			.synchronizedMap(new HashMap<>());
	private final Map<Group, Set<KernelAddress>> distantAccessibleKernelsPerGroupsGivenByDistantPeer= Collections
			.synchronizedMap(new HashMap<>());
	private final Map<KernelAddress, Set<Group>> distantAccessibleGroupsGivenToDistantPeer = Collections
			.synchronizedMap(new HashMap<>());
	private final Map<Group, Set<KernelAddress>> distantAccessibleKernelsPerGroupsGivenToDistantPeer= Collections
			.synchronizedMap(new HashMap<>());
	private final Map<KernelAddress, Set<PairOfIdentifiers>> acceptedDistantLogins = Collections
			.synchronizedMap(new HashMap<>());

	private final IDGeneratorInt generatorIdTransfer;
	private final Map<KernelAddress, InterfacedIDs> globalInterfacedIds;
	//private final boolean lockSocketUntilCGRSynchroIsSent;

	private volatile AsynchronousMessageTable asynchronousMessageTable =null;
	private volatile AsynchronousBigDataTable asynchronousBigDataTable =null;
	private volatile TaskID databaseCleanerTaskID=null;
	private volatile Runnable additionalDataCleaner=null;

	/**
	 * Constructing the real one.
	 * 
	 * @param m madkit
	 */
	@SuppressWarnings("ResultOfMethodCallIgnored")
	MadkitKernel(Madkit m) {
		super(true);

		platform = m;
		kernel = this;
		threadedAgents = new HashSet<>(20);
		generatorIdTransfer = new IDGeneratorInt((int) (Math.random() * (double) Integer.MAX_VALUE));
		globalInterfacedIds = new HashMap<>();
		// set the log dir name and checking uniqueness
		final MadkitProperties madkitConfig = getMadkitConfig();
		// final String logBaseDir= + File.separator;

		KernelAddress ka;
		if (m.kernelAddress != null)
			ka = m.kernelAddress;
		else
			ka = new KernelAddress(madkitConfig.isKernelAddressSecured());

		kernelAddress = ka;

		File logDir = new File(madkitConfig.logDirectory, platform.dateFormat.format(new Date()) + kernelAddress);
		while (logDir.exists()) {
			logDir = new File(madkitConfig.logDirectory, platform.dateFormat.format(new Date()) + kernelAddress);
		}

		madkitConfig.logDirectory = logDir;

		organizations = new ConcurrentHashMap<>();
		operatingOverlookers = new LinkedHashSet<>();
		loggedKernel = new LoggedKernel(this);

		getLogger(); // Bootstrapping the agentLoggers with default logger variable for global
						// actions

		setLogLevel(madkitConfig.kernelLogLevel);

		if (logger != null && madkitConfig.createLogFiles) {
			logger.createLogFile();
		}

		normalAgentThreadFactory = new AgentThreadFactory(kernelAddress, false);
		daemonAgentThreadFactory = new AgentThreadFactory(kernelAddress, true);
		this.serviceExecutor = createSchedulerServiceExecutor(SYSTEM, threadPriorityForServiceExecutor, false,
				SYSTEM.getName(), Math.min(Runtime.getRuntime().availableProcessors(), 2), 4L,
				null);
		this.serviceExecutor.start();
		if (madkitConfig.isUseMadkitSchedulerWithFortunaSecureRandom())
			Fortuna.setPersonalDefaultScheduledExecutorService(this.serviceExecutor);

		madkitConfig.getApprovedRandomType();
		madkitConfig.getApprovedRandomTypeForKeys();
		asynchronousMessageTable =null;
		asynchronousBigDataTable =null;
		if (madkitConfig.isDatabaseEnabled()) {
			try {
				asynchronousMessageTable = madkitConfig.getDatabaseWrapper().getTableInstance(AsynchronousMessageTable.class);
				asynchronousBigDataTable = madkitConfig.getDatabaseWrapper().getTableInstance(AsynchronousBigDataTable.class);
				setAutomaticObsoleteDataCleaningDelay(madkitConfig.delayInMsBeforeCleaningObsoleteMadkitData);
			} catch (DatabaseException e) {
				bugReport(e);
				try {
					madkitConfig.setDatabaseFactory(null);
				} catch (DatabaseException e2) {
					bugReport(e2);
				}
			}
		}
		madkitConfig.madkitLaunched=true;

	}

	AsynchronousBigDataTable getAsynchronousBigDataTable() {
		return asynchronousBigDataTable;
	}

	AgentThreadFactory getNormalAgentThreadFactory() {
		return normalAgentThreadFactory;
	}

	AgentThreadFactory getDaemonAgentThreadFactory() {
		return daemonAgentThreadFactory;
	}

	MadkitKernel() {
		// for fake kernels
		super(null);
		kernel = this;
		threadedAgents = null;
		loggedKernel = this;
		platform = null;
		kernelAddress = null;
		organizations = null;
		operatingOverlookers = null;
		normalAgentThreadFactory = null;
		daemonAgentThreadFactory = null;
		this.serviceExecutor = null;
		generatorIdTransfer = null;
		globalInterfacedIds = null;
	}

	/**
	 * for the logged kernel
	 */
	MadkitKernel(MadkitKernel k) {
		super(null);
		threadedAgents = null;
		platform = k.platform;
		kernelAddress = k.kernelAddress;
		organizations = k.organizations;
		operatingOverlookers = k.operatingOverlookers;
		normalAgentThreadFactory = null;
		daemonAgentThreadFactory = null;
		//lifeExecutor = null;
		this.serviceExecutor = null;
		generatorIdTransfer = null;
		globalInterfacedIds = null;
		// lifeExecutorWithBlockQueue=null;
		kernel = k;
		//this.lockSocketUntilCGRSynchroIsSent= false;
	}

	Map<KernelAddress, InterfacedIDs> getGlobalInterfacedIDs() {
		return globalInterfacedIds;
	}

	boolean isGlobalInterfacedIDsEmpty() {
		synchronized (globalInterfacedIds) {
			return globalInterfacedIds.isEmpty();
		}
	}

	ArrayList<AbstractAgent> getConnectedNetworkAgents() {

		try {
			synchronized (organizations) {
				ArrayList<AbstractAgent> res = new ArrayList<>();

				for (InternalRole r : getGroup(LocalCommunity.Groups.NETWORK).values()) {
					for (AbstractAgent aa : r.getAgentsList()) {
						if (!(aa instanceof MadkitKernel))
							res.add(aa);
					}
				}
				return res;
			}
		} catch (CGRNotAvailable e) {
			return null;
		}
	}

	private void setHasSpeedLimitation()
	{
		hasSpeedLimitation=maximumGlobalUploadSpeedInBytesPerSecond!=Integer.MAX_VALUE || maximumGlobalDownloadSpeedInBytesPerSecond!=Integer.MAX_VALUE;
	}

	boolean hasSpeedLimitation(@SuppressWarnings("unused") AbstractAgent requester) {
		return hasSpeedLimitation;
	}

	@Override
	protected void activate() {
		// addWebRepository();
		if (logger != null)
			logger.setWarningLogLevel(Level.INFO);

        maximumGlobalDownloadSpeedInBytesPerSecond=Math.max(0, getMadkitConfig().networkProperties.maximumGlobalDownloadSpeedInBytesPerSecond);
        maximumGlobalUploadSpeedInBytesPerSecond=Math.max(0, getMadkitConfig().networkProperties.maximumGlobalUploadSpeedInBytesPerSecond);
        setHasSpeedLimitation();
        createGroup(LocalCommunity.Groups.SYSTEM);
		createGroup(LocalCommunity.Groups.KERNELS);



		// building the network group
		createGroup(Groups.NETWORK);
		if (!requestRole(Groups.NETWORK, Roles.KERNEL, null).equals(ReturnCode.SUCCESS)) {
			System.err.println("Kernel is unable get role Roles.KERNEL");
		}
		if (!requestRole(Groups.NETWORK, Roles.UPDATER, null).equals(ReturnCode.SUCCESS)) {
			System.err.println("Kernel is unable get role Roles.UPDATER");
		}
		if (!requestRole(Groups.NETWORK, Roles.CENTRAL_DATABASE_BACKUP_CHECKER, null).equals(ReturnCode.SUCCESS)) {
			System.err.println("Kernel is unable get role Roles.CENTRAL_DATABASE_BACKUP_CHECKER");
		}
		if (!requestRole(Groups.NETWORK, Roles.EMITTER, null).equals(ReturnCode.SUCCESS)) {
			System.err.println("Kernel is unable get role Roles.EMITTER");
		}
		if (!requestRole(Groups.NETWORK, Roles.SECURITY, null).equals(ReturnCode.SUCCESS)) {
			System.err.println("Kernel is unable get role Roles.SECURITY");
		}

		// my AAs cache
		netCentralDatabaseBackupReceiverChecker = getAgentAddressIn(Groups.NETWORK, Roles.CENTRAL_DATABASE_BACKUP_CHECKER);
		netUpdater = getAgentAddressIn(Groups.NETWORK, Roles.UPDATER);
		netSecurity = getAgentAddressIn(Groups.NETWORK, Roles.SECURITY);
		netEmitter = getAgentAddressIn(Groups.NETWORK, Roles.EMITTER);
		kernelRole = getAgentAddressIn(Groups.NETWORK, Roles.KERNEL);

		myThread.setPriority(Thread.NORM_PRIORITY + 1);

		if (getMadkitConfig().loadLocalDemos) {
			GlobalAction.LOAD_LOCAL_DEMOS.actionPerformed(null);
		}
		launchGuiManagerAgent();
		if (getMadkitConfig().console) {
			launchAgent(new ConsoleAgent());
		}
		launchNetworkAgent();

		if (getMadkitConfig().autoConnectMadkitWebsite) {
			addWebRepository();
		}
		startSession();
		// logCurrentOrganization(logger,Level.FINEST);
	}

	/**
	 * Starts a session considering the current MaDKit configuration
	 */
	private void startSession() {
		launchXMLConfigurations();
		launchConfigAgents();
	}

	@Override
	protected void liveCycle() throws InterruptedException {
		handleMessage(waitNextMessage());// As a daemon, a timeout is not required

	}

	@Override
	public boolean isAlive() {
		return super.isAlive() && !shutDown;
	}

	private void launchGuiManagerAgent() {
		if (logger != null && logger.isLoggable(Level.FINE))
			logger.fine("\n\t****** Launching GUI Manager ******\n");
		// if (noGUIManager.isActivated(getMadkitConfig())) {
		// if (logger != null)
		// logger.fine("** No GUI Manager: " + noGUIManager +
		// " option is true**\n");
		// } else {
		try {
			// no need to externalize : it is the only use of that string
			final Constructor<?> c = MadkitClassLoader.getLoader()
					.loadClass("com.distrimind.madkit.gui.GUIManagerAgent").getDeclaredConstructor(boolean.class);
			c.setAccessible(true);
			final Agent a = (Agent) c.newInstance(!getMadkitConfig().desktop);
			// c.setAccessible(false); //useless
			a.setLogLevel(getMadkitConfig().guiLogLevel);
			launchAgent(a);
			//threadedAgents.remove(a);//TODO uncomment ok ?
			if (logger != null && logger.isLoggable(Level.FINE))
				logger.fine("\n\t****** GUI Manager launched ******\n");
		} catch (ClassNotFoundException | SecurityException | NoSuchMethodException | IllegalArgumentException
				| InstantiationException | IllegalAccessException | InvocationTargetException e) {
			bugReport(e);
		}
		// }
	}

	/**
	 * Tells if a group must be manually created through
	 * {@link AbstractAgent#createGroup(Group)} or if groups can be automatically
	 * created.
	 * 
	 * @param value
	 *            true if groups can be automatically created
	 */
	@SuppressWarnings("unused")
	void setAutoCreateGroup(AbstractAgent aa, boolean value) {
		auto_create_group.set(value);
	}

	/**
	 * Tells if a group must be manually created through
	 * {@link AbstractAgent#createGroup(Group)} or if groups can be automatically
	 * created.
	 * 
	 * @return true if groups can be automatically created
	 */
    @SuppressWarnings("unused")
    boolean isAutoCreateGroup(AbstractAgent aa) {

		return auto_create_group.get();
	}

	void manageDirectConnection(AbstractAgent requester, AskForConnectionMessage m, boolean addToNetworkProperties, boolean actOnlyIfModifyNetworkProperties) throws IllegalAccessException {
		if (m == null)
			throw new NullPointerException("message");
		if (getMadkitConfig().networkProperties.network) {
			updateNetworkAgent();
			boolean act=true;
			if (addToNetworkProperties) {
				if (m.getType()== ConnectionStatusMessage.Type.CONNECT)
					act=getMadkitConfig().networkProperties.addAddressesForDirectConnectionToAttemptFromThisPeerToOtherPeer(m.getIP());
				else if (m.getType()== ConnectionStatusMessage.Type.DISCONNECT)
					act=getMadkitConfig().networkProperties.removeAddressesForDirectConnectionToAttemptFromThisPeerToOtherPeer(m.getIP());
			}
			act|=!actOnlyIfModifyNetworkProperties;
			if (act)
				sendNetworkKernelMessageWithRole(new KernelMessage(KernelAction.MANAGE_DIRECT_CONNECTION, m));
		} else
			throw new IllegalAccessException(
					"The network is disabled into the madkit properties. Impossible to connect to an IP.");
	}

	void manageDirectConnections(AbstractAgent requester, List<AskForConnectionMessage> lm, boolean addToNetworkProperties, boolean actOnlyIfModifyNetworkProperties) throws IllegalAccessException {
		if (lm == null)
			throw new NullPointerException("message");
		if (lm.size()==0)
			return;
		if (getMadkitConfig().networkProperties.network) {
			updateNetworkAgent();

			if (addToNetworkProperties) {
				for (AskForConnectionMessage m : lm) {
					boolean act=true;
					if (m.getType() == ConnectionStatusMessage.Type.CONNECT)
						act=getMadkitConfig().networkProperties.addAddressesForDirectConnectionToAttemptFromThisPeerToOtherPeer(m.getIP());
					else if (m.getType() == ConnectionStatusMessage.Type.DISCONNECT)
						act=getMadkitConfig().networkProperties.removeAddressesForDirectConnectionToAttemptFromThisPeerToOtherPeer(m.getIP());
					act|=!actOnlyIfModifyNetworkProperties;
					if (act)
						sendNetworkKernelMessageWithRole(new KernelMessage(KernelAction.MANAGE_DIRECT_CONNECTION, m));
				}
			}
			else {
				for (AskForConnectionMessage m : lm) {
					sendNetworkKernelMessageWithRole(new KernelMessage(KernelAction.MANAGE_DIRECT_CONNECTION, m));
				}
			}


		} else
			throw new IllegalAccessException(
					"The network is disabled into the madkit properties. Impossible to connect to an IP.");
	}


	void manageTransferConnection(AbstractAgent requester, AskForTransferMessage m) throws IllegalAccessException {
		if (m == null)
			throw new NullPointerException("message");
		if (getMadkitConfig().networkProperties.network) {
			updateNetworkAgent();
			sendNetworkKernelMessageWithRole(new KernelMessage(KernelAction.MANAGE_TRANSFER_CONNECTION, m));
		} else
			throw new IllegalAccessException(
					"The network is disabled into the madkit properties. Impossible to connect to an IP.");
	}

	protected void copy() {
		startSession(false);
	}

	@SuppressWarnings("unused")
	private void restart() throws InterruptedException {
		new java.util.Timer().schedule(new TimerTask() {
			@Override
			public void run() {
				try {
					myThread.join();
				} catch (InterruptedException ignored) {
				}
				copy();
			}
		}, 100);
		exit();
		// copy();
	}

	/**
	 * 
	 */
	private void addWebRepository() {
		final URL repoLocation = getMadkitConfig().madkitRepositoryURL;
		if (logger != null && logger.isLoggable(Level.FINE))
			logger.fine("** CONNECTING WEB REPO **" + repoLocation);
		try {
			BufferedReader in = new BufferedReader(new InputStreamReader(repoLocation.openStream()));
			for (String s : in.readLine().split("<br/>")) {
				MadkitClassLoader.loadUrl(new URL(s));
			}
			in.close();
		} catch (IOException e) {
			if (logger != null)
				logger.log(Level.WARNING, ErrorMessages.CANT_CONNECT + ": " + repoLocation + "\n" + e.getMessage());
		}
	}

	@SuppressWarnings("unused")
	/*
	 * private void launchMas(MASModel dm) { if (logger != null)
	 * logger.finer("** LAUNCHING SESSION " + dm.getName()); Properties mkCfg =
	 * platform.getConfigOption(); Properties currentConfig = new Properties();
	 * currentConfig.putAll(mkCfg);
	 * mkCfg.putAll(platform.buildConfigFromArgs(dm.getSessionArgs())); //TODO parse
	 * config File launchConfigAgents(); mkCfg.putAll(currentConfig); }
	 */

	private void launchXml(File xmlFile, boolean inNewMadkit) {
		if (logger != null && logger.isLoggable(Level.FINER))
			logger.finer("** LAUNCHING XML CONFIG " + xmlFile);
		if (inNewMadkit) {
			new Madkit("configFiles", xmlFile.toString());
		} else {
			MadkitProperties mkCfg = platform.getConfigOption().clone();
			/*
			 * MadkitProperties currentConfig = mkCfg.clone(); currentConfig.putAll(mkCfg);
			 */
			try {
				Document document = XMLUtilities.getDOM(xmlFile);
				mkCfg.loadXML(document);
				launchXmlAgents(document);
			} catch (IOException | SAXException | ParserConfigurationException e) {
				getLogger().severeLog("", e);
			}
			// mkCfg.putAll(currentConfig);
		}
	}
	@SuppressWarnings("unused")
	private void launchYAML(File yamlFile, boolean inNewMadkit) {
		if (logger != null && logger.isLoggable(Level.FINER))
			logger.finer("** LAUNCHING YAML CONFIG " + yamlFile);
		if (inNewMadkit) {
			new Madkit("configFiles", yamlFile.toString());
		} else {
			MadkitProperties mkCfg = platform.getConfigOption().clone();
			/*
			 * MadkitProperties currentConfig = mkCfg.clone(); currentConfig.putAll(mkCfg);
			 */
			try {
				
				mkCfg.loadYAML(yamlFile);
				
			} catch (IOException e) {
				getLogger().severeLog("", e);
			}
			// mkCfg.putAll(currentConfig);
		}
	}

	@SuppressWarnings("unused")
	private void console() {
		launchAgent(ConsoleAgent.class.getName());
	}

	private void launchConfigAgents() {
		// TODO do that with life
		if (logger != null)
			logger.fine("** LAUNCHING CONFIG AGENTS **");
		ArrayList<AgentToLaunch> agentsToLaunch = platform.getConfigOption().launchAgents;
		if (agentsToLaunch != null) {
			for (final AgentToLaunch atl : agentsToLaunch) {
				if (logger != null && logger.isLoggable(Level.FINER))
					logger.finer("Launching " + atl.getNumber() + " instance(s) of " + atl.getClassAgent()
							+ " with GUI = " + atl.isWithGUI());
				for (int i = 0; i < atl.getNumber(); i++) {
					serviceExecutor.execute(() -> {
						if (!shutDown) {
							try {
								launchAgent(atl.getClassAgent().getDeclaredConstructor().newInstance(), 0, atl.isWithGUI());
							} catch (Exception e) {
								cannotLaunchAgent(atl.getClassAgent().getCanonicalName(), e, null);
							}
						}
					});
				}
			}
			// startExecutor.shutdown();
		}
	}

	private void launchXMLConfigurations() {
		if (logger != null)
			logger.fine("** LAUNCHING XML CONFIGS **");
		if (getMadkitConfig().configFiles != null) {
			for (final File file : getMadkitConfig().configFiles) {

				if (file.toString().endsWith(".xml")) {
					serviceExecutor.execute(() -> {
						try {
							if (logger != null && logger.isLoggable(Level.FINER))
								logger.finer("Launching xml " + file);
							launchXmlAgents(file);
						} catch (SAXException | IOException | ParserConfigurationException e) {
							getLogger().severeLog("xml config", e);
							e.printStackTrace();
						}
					});
				}
			}
		}
	}

	private void startSession(final boolean externalVM) {
		if (logger != null) {
			logger.config("starting new MaDKit session with " + Arrays.deepToString(platform.args));
		}
		if (externalVM) {
			try {
				StringBuilder args = new StringBuilder();
				for (String s : platform.args) {
					args.append(s).append(" ");
				}


				Process p=Runtime.getRuntime().exec(new String[]{
						System.getProperty("java.home") + File.separatorChar + "bin" + File.separatorChar + "java"
						," -cp "+System.getProperty("java.class.path") + " "+platform.getConfigOption().madkitMainClass.getCanonicalName(),
						args.toString()});
				Utils.flushAndDestroyProcess(p);
			} catch (IOException e) {
				bugReport(e);
			}
		} else {
			final Thread t = new Thread(() -> new Madkit(platform.args));
			t.setDaemon(false);
			t.start();
		}
	}

	@SuppressWarnings("unused")
	private void stopNetwork() {
		// ReturnCode r = sendNetworkMessageWithRole(new Message(), kernelRole);
		if (sendNetworkKernelMessageWithRole(new KernelMessage(KernelAction.STOP_NETWORK)) != SUCCESS) {
			if (logger != null)
				logger.fine("\n\t****** Network already down ******\n");
		}
	}

	private void handleMessage(Message m) {

		if (m instanceof KernelMessage) {
			proceedEnumMessage((KernelMessage) m);
		} else if (m instanceof HookMessage) {
			handleHookRequest((HookMessage) m);
		} else if (m instanceof RequestRoleSecure) {
			handleRequestRoleSecure((RequestRoleSecure) m);
		} else {
			if (logger != null)
				logger.warning("I received a message that I do not understand. Discarding " + m);
		}
	}

	private void handleRequestRoleSecure(RequestRoleSecure m) {
		AgentAddress requesterAddress = m.getRequester();
		InternalGroup g = null;
		try {
			g = getGroup(requesterAddress.getGroup());
		} catch (CGRNotAvailable ignored) {
		}
		sendReply(m,
				new BooleanMessage(g != null
                        && g.getGatekeeper().allowAgentToTakeRole(requesterAddress.getGroup(), m.getRoleName(),
                        m.getRequesterClass(), requesterAddress.getAgentNetworkID(), m.getContent())));
	}

	private void handleHookRequest(HookMessage m) {
		requestHookEvents(m.getSender().getAgent(), m.getContent(), true);
	}

	@SuppressWarnings("SynchronizationOnLocalVariableOrMethodParameter")
    ReturnCode requestHookEvents(AbstractAgent requester, AgentActionEvent hookType, boolean autoRemove) {
		if (requester.getState().compareTo(State.WAIT_FOR_KILL) >= 0)
			return ReturnCode.IGNORED;

		Set<AbstractAgent> l = hooks.get(hookType);
		if (l == null) {
			synchronized (hooks) {
				l = hooks.computeIfAbsent(hookType, k -> new HashSet<>());
			}
		}
		// for speeding up if there is no hook, i.e. logger == null is default
		// getLogger().setLevel(Level.INFO);
		synchronized (l) {
			if (!l.add(requester)) {
				if (autoRemove) {
					l.remove(requester);
					return ReturnCode.SUCCESS;
				} else
					return ReturnCode.IGNORED;
			}
		}
		return ReturnCode.SUCCESS;
	}

	@SuppressWarnings("SynchronizationOnLocalVariableOrMethodParameter")
    ReturnCode releaseHookEvents(AbstractAgent requester, AgentActionEvent hookType) {
		Set<AbstractAgent> l = hooks.get(hookType);
		if (l == null) {
			return ReturnCode.IGNORED;
		}
		// for speeding up if there is no hook, i.e. logger == null is default
		// getLogger().setLevel(Level.INFO);
		synchronized (l) {
			if (l.remove(requester)) {
				return ReturnCode.SUCCESS;
			} else
				return ReturnCode.IGNORED;
		}
	}

	void releaseHookEvents(AbstractAgent requester) {
		for (AgentActionEvent aae : AgentActionEvent.values())
			releaseHookEvents(requester, aae);
	}

	private void launchNetworkAgent() {
		if (getMadkitConfig().networkProperties.network) {
			launchNetwork();
		} else {
			if (logger != null)
				logger.fine("** Networking is off: No Net Agent **\n");
		}
	}

	// /////////////////////////////////////: Kernel Part

	/**
	 * @return the loggedKernel
	 */
	final MadkitKernel getLoggedKernel() {
		return loggedKernel;
	}

	// /////////////////////////////////////////////////////////////////////////
	// //////////////////////// Agent interface
	// /////////////////////////////////////////////////////////////////////////

	// ////////////////////////////////////////////////////////////
	// //////////////////////// Organization interface
	// ////////////////////////////////////////////////////////////

	@SuppressWarnings("SynchronizationOnLocalVariableOrMethodParameter")
    ReturnCode createGroup(final AbstractAgent creator, Group group, final Object memberCard, boolean manual_creation) {
		if (group == null)
			throw new NullPointerException(ErrorMessages.G_NULL.toString());
		if (group.isUsedSubGroups())
			return ReturnCode.MULTI_GROUP_NOT_ACCEPTED;
		final String community = group.getCommunity();
		Group[] parents = group.getParentGroups();
		for (Group g : parents) {
			Gatekeeper gk = g.getGateKeeper();
			if (gk != null) {
				try {
					if (gk.allowAgentToCreateSubGroup(g, group, creator.getClass(), creator.getNetworkID(), memberCard))
						break;
					else
						return ReturnCode.ACCESS_DENIED;
				} catch (Exception e) {
					e.printStackTrace();
					if (logger != null)
						logger.severeLog(Influence.CREATE_GROUP.failedString(), e);
					return ReturnCode.AGENT_CRASH;
				}
			}
		}

		// final boolean isDistributed=isDistributed;
		Organization organization = new Organization(community, this);
		// no need to remove org: never failed
		// will throw null pointer if community is null
		final Organization tmpOrg = organizations.putIfAbsent(community, organization);
		if (tmpOrg != null) {
			organization = tmpOrg;
		}
		synchronized (organization) {
			if (!organization.addGroup(creator, group, manual_creation)) {
				return ALREADY_GROUP;
			}
			try {// TODO bof...
				if (group.isDistributed()) {

					sendNetworkCGRSynchroMessageWithRole(creator, new CGRSynchro(CREATE_GROUP,
							getRole(group, com.distrimind.madkit.agr.Organization.GROUP_MANAGER_ROLE)
									.getAgentAddressOf(creator),
							true));
				}
                informHooks(AgentActionEvent.CREATE_GROUP,
                        getRole(group, com.distrimind.madkit.agr.Organization.GROUP_MANAGER_ROLE)
                                .getAgentAddressOf(creator));
            } catch (CGRNotAvailable e) {
				getLogger().severeLog("Please bug report", e);
			}
		}
		return SUCCESS;
	}

	void informHooks(AgentActionEvent action, Object parameter) {
		Object[] o = new Object[1];
		o[0] = parameter;
		informHooks(action, o);

	}

	private void checkAsynchronousMessagesToSend(Object parameter)
	{
		if (asynchronousBigDataTable!=null) {

			AgentAddress aa=(AgentAddress) parameter;
			try {
				asynchronousBigDataTable.groupRoleAvailable(this, platform.getConfigOption().rootOfPathGroupUsedToFilterAsynchronousMessages, aa.getGroup(), aa.getRole());
			} catch (DatabaseException e) {
				getLogger().severeLog("Unexpected exception", e);
			}
		}
	}

	@SuppressWarnings("unchecked")
	void informHooks(AgentActionEvent action, Object... parameters) {
		if (hooks.size() > 0) {
			HookMessage hm = null;
			switch (action) {

			case REQUEST_ROLE:
				checkAsynchronousMessagesToSend(parameters[0]);

			case CREATE_GROUP:
			case LEAVE_GROUP:
			case LEAVE_ROLE:
				hm = new OrganizationEvent(action, (AgentAddress) parameters[0]);
				break;
			case BROADCAST_MESSAGE:
			case SEND_MESSAGE:
				Message m = ((Message) parameters[0]);
				if (m instanceof LocalLanMessage)
					m = ((LocalLanMessage) m).getOriginalMessage();
				hm = new MessageEvent(action, m);
				break;
			case AGENT_STARTED:
			case AGENT_TERMINATED:
			case AGENT_ZOMBIE:
				hm = new AgentLifeEvent(action, (AbstractAgent) parameters[0]);
				break;
			case CONNEXION_CLOSED_BECAUSE_OF_NETWORK_ANOMALY:
			case CONNEXION_LOST:
			case CONNEXION_ESTABLISHED:
			case CONNEXION_PROPERLY_CLOSED:
				hm = new NetworkEventMessage(action, (Connection) parameters[0]);
				break;
			case ACCESSIBLE_LAN_GROUPS_GIVEN_BY_DISTANT_PEER:
			case ACCESSIBLE_LAN_GROUPS_GIVEN_TO_DISTANT_PEER:
				hm = new NetworkGroupsAccessEvent(action, (ListGroupsRoles) parameters[0], (ListGroupsRoles) parameters[1],
						(KernelAddressInterfaced) parameters[2], (Boolean) parameters[3]);
				break;
			case LOGGED_IDENTIFIERS_UPDATE:
				hm = new NetworkLoginAccessEvent((KernelAddress) parameters[0],
						(List<PairOfIdentifiers>) parameters[1], (List<PairOfIdentifiers>) parameters[2],
						(List<CloudIdentifier>) parameters[3], (List<Identifier>) parameters[4], (List<Identifier>) parameters[5],
						(List<PairOfIdentifiers>) parameters[6]);
				break;
			case TRANSFER_CONNEXION_EVENT:
				hm = new TransferEventMessage((Integer) parameters[0],
						(AskForTransferMessage) parameters[1], (TransferEventMessage.TransferEventType) parameters[2]);
				break;
			case DISTANT_KERNEL_CONNECTED:
			case DISTANT_KERNEL_DISCONNECTED:
				hm = new DistantKernelAgentEventMessage(action, (KernelAddress) parameters[0]);
				break;
			case NETWORK_ANOMALY_EVENT:
				if (parameters.length == 4)
					hm = new NetworkAnomalyEvent((KernelAddress) parameters[0], (InetAddress) parameters[1],
                            (Boolean) parameters[2], (String) parameters[3]);
				else
					hm = new NetworkAnomalyEvent((KernelAddress) parameters[0], (InetAddress) parameters[1],
                            (Boolean) parameters[2]);
				break;
			case IP_BANNED_EVENT:
                //noinspection ConstantConditions
                hm = new IPBannedEvent((InetAddress) parameters[0], (Long) parameters[0]);
				break;

			}
			informHooks(hm);
		}
		else {

			HookMessage hm = null;
			switch (action) {
				case REQUEST_ROLE:
					checkAsynchronousMessagesToSend(parameters[0]);
				case LEAVE_ROLE:
					hm = new OrganizationEvent(action, (AgentAddress) parameters[0]);
					break;
				case CONNEXION_CLOSED_BECAUSE_OF_NETWORK_ANOMALY:
				case CONNEXION_LOST:
				case CONNEXION_ESTABLISHED:
				case CONNEXION_PROPERLY_CLOSED:
					hm = new NetworkEventMessage(action, (Connection) parameters[0]);
					break;
				case ACCESSIBLE_LAN_GROUPS_GIVEN_BY_DISTANT_PEER:
				case ACCESSIBLE_LAN_GROUPS_GIVEN_TO_DISTANT_PEER:
					hm = new NetworkGroupsAccessEvent(action, (ListGroupsRoles) parameters[0], (ListGroupsRoles) parameters[1],
							(KernelAddressInterfaced) parameters[2], (Boolean) parameters[3]);
					break;
				case LOGGED_IDENTIFIERS_UPDATE:
					hm = new NetworkLoginAccessEvent((KernelAddress) parameters[0],
							(ArrayList<PairOfIdentifiers>) parameters[1], (ArrayList<PairOfIdentifiers>) parameters[2],
							(List<CloudIdentifier>) parameters[3], (List<Identifier>) parameters[4], (List<Identifier>) parameters[5],
							(List<PairOfIdentifiers>) parameters[6]);
					break;
				case DISTANT_KERNEL_CONNECTED:
				case DISTANT_KERNEL_DISCONNECTED:
					hm = new DistantKernelAgentEventMessage(action, (KernelAddress) parameters[0]);
					break;
			}
			if (hm != null)
				informHooks(hm);
		}
	}
	private void updateKernelMapPerGroups(Map<KernelAddress, Set<Group>> groupsPerKernelAddress, Map<Group, Set<KernelAddress>> kernelAddressesPerGroups, KernelAddress ka, ListGroupsRoles l)
	{
		updateKernelMapPerGroups(groupsPerKernelAddress, kernelAddressesPerGroups, ka, Arrays.asList(l.getRepresentedGroups(getKernelAddress())));
	}
	private void updateKernelMapPerGroups(Map<KernelAddress, Set<Group>> groupsPerKernelAddress, Map<Group, Set<KernelAddress>> kernelAddressesPerGroups, KernelAddress ka, List<Group> l)
    {

        Set<Group> ol;
        groupsPerKernelAddress.put(ka, ol=Collections.synchronizedSet(new HashSet<>(l)));
        List<Group> added=new ArrayList<>(l.size());
        List<Group> removed=new ArrayList<>(ol.size());
        for (Group g : l) {

            if (!ol.contains(g))
                added.add(g);
        }
        for (Group g : ol) {

            if (!l.contains(g))
                removed.add(g);
        }
        for (Group g : removed)
        {
            Set<KernelAddress> lka=kernelAddressesPerGroups.get(g);
            if (lka!=null)
            {
                lka.remove(ka);
                if (lka.isEmpty())
                    kernelAddressesPerGroups.remove(g);
            }
        }
        for (Group g : added)
        {
            Set<KernelAddress> lka;
            if ((lka=kernelAddressesPerGroups.get(g))==null) {
                lka = Collections.synchronizedSet(new HashSet<>());
                kernelAddressesPerGroups.put(g, lka);
            }
            lka.add(ka);
        }



    }

    private static void removeDistantKernelAddress(Map<KernelAddress, Set<Group>> groupsPerKernelAddress, Map<Group, Set<KernelAddress>> kernelAddressesPerGroups, KernelAddress ka)
    {
        Set<Group> l=groupsPerKernelAddress.remove(ka);
        if (l!=null) {
			for (Group g : l) {
				Set<KernelAddress> lka = kernelAddressesPerGroups.get(g);
				if (lka != null) {
					lka.remove(ka);
					if (lka.isEmpty())
						kernelAddressesPerGroups.remove(g);
				}
			}
		}
    }

	void informHooks(HookMessage hook_message) {
		if (hook_message != null) {

			if (asynchronousMessageTable != null && hook_message.getClass() == OrganizationEvent.class)
			{
				OrganizationEvent oe=(OrganizationEvent)hook_message;

				if (oe.getContent()==AgentActionEvent.REQUEST_ROLE) {
					try {
						asynchronousMessageTable.groupRoleAvailable(platform.getConfigOption().rootOfPathGroupUsedToFilterAsynchronousMessages, oe.getSourceAgent().getAgent(), oe.getSourceAgent());
					} catch (DatabaseException e) {
						logLifeException(e);
					}
				}
				else if (oe.getContent()==AgentActionEvent.LEAVE_ROLE) {
					asynchronousMessageTable.newAgentDisconnected(platform.getConfigOption().rootOfPathGroupUsedToFilterAsynchronousMessages, oe.getSourceAgent());
				}

			} else if (hook_message.getClass() == NetworkEventMessage.class) {
				if (hook_message.getContent() == AgentActionEvent.CONNEXION_ESTABLISHED) {
					Connection c = ((NetworkEventMessage) hook_message).getConnection();
					availableConnections.add(c);

				} else if (hook_message.getContent() == AgentActionEvent.CONNEXION_CLOSED_BECAUSE_OF_NETWORK_ANOMALY
						|| hook_message.getContent() == AgentActionEvent.CONNEXION_LOST
						|| hook_message.getContent() == AgentActionEvent.CONNEXION_PROPERLY_CLOSED) {
					Connection c = ((NetworkEventMessage) hook_message).getConnection();
					availableConnections.remove(c);
				}
			} else if (hook_message.getClass() == DistantKernelAgentEventMessage.class) {
				if (hook_message.getContent() == AgentActionEvent.DISTANT_KERNEL_CONNECTED) {
					distantKernelAddresses
							.add(((DistantKernelAgentEventMessage) hook_message).getDistantKernelAddress());
				} else if (hook_message.getContent() == AgentActionEvent.DISTANT_KERNEL_DISCONNECTED) {
					DistantKernelAgentEventMessage m = (DistantKernelAgentEventMessage) hook_message;
					if (!distantKernelAddresses.remove(m.getDistantKernelAddress()) && logger != null)
						logger.warning(
								"Removing but impossible to find distant kernel : " + m.getDistantKernelAddress());

                    removeDistantKernelAddress(distantAccessibleGroupsGivenByDistantPeer, distantAccessibleKernelsPerGroupsGivenByDistantPeer, m.getDistantKernelAddress());
                    removeDistantKernelAddress(distantAccessibleGroupsGivenToDistantPeer, distantAccessibleKernelsPerGroupsGivenToDistantPeer, m.getDistantKernelAddress());
					acceptedDistantLogins.remove(m.getDistantKernelAddress());
					removeDistantKernelAddressFromCGR(m.getDistantKernelAddress());
				}
			} else if (hook_message.getClass() == NetworkGroupsAccessEvent.class) {
				NetworkGroupsAccessEvent n = (NetworkGroupsAccessEvent) hook_message;
				if (hook_message.getContent() == AgentActionEvent.ACCESSIBLE_LAN_GROUPS_GIVEN_BY_DISTANT_PEER) {
				    updateKernelMapPerGroups(distantAccessibleGroupsGivenByDistantPeer, distantAccessibleKernelsPerGroupsGivenByDistantPeer, n.getConcernedKernelAddress(), n.getRequestedAccessibleGroups());

				} else if (hook_message.getContent() == AgentActionEvent.ACCESSIBLE_LAN_GROUPS_GIVEN_TO_DISTANT_PEER) {
                    updateKernelMapPerGroups(distantAccessibleGroupsGivenToDistantPeer, distantAccessibleKernelsPerGroupsGivenToDistantPeer, n.getConcernedKernelAddress(), n.getRequestedAccessibleGroups());
                    if (n.isLocalGroupsRemoved()) {
						synchronized (organizations) {
							organizations.values().removeIf(org -> {
								org.updateAcceptedDistantGroupsGivenToDistantPeer(n.getConcernedKernelAddress(), n.getGeneralAcceptedGroups());
								return org.isEmpty();
							});
						}
					}

				}
			} else if (hook_message.getClass() == NetworkLoginAccessEvent.class) {
				NetworkLoginAccessEvent n = (NetworkLoginAccessEvent) hook_message;
				acceptedDistantLogins.put(n.getConcernedKernelAddress(), new HashSet<>(n.getCurrentIdentifiers()));
			}

			if (hooks.size() > 0) {
				final Set<AbstractAgent> l = hooks.get(hook_message.getContent());
				if (l != null) {
					synchronized (l) {
						for (final AbstractAgent a : l) {
							a.receiveMessage(hook_message);
						}
					}
				}
			}
		}
	}

	Set<Connection> getEffectiveConnections(AbstractAgent requester) {
		return Collections.unmodifiableSet(availableConnections);
	}

	Set<KernelAddress> getAvailableDistantKernels(AbstractAgent requester) {
		return Collections.unmodifiableSet(distantKernelAddresses);
	}

	private Set<Group> filterGroups(Set<Group> in, AbstractAgent requester)
	{
		HashSet<Group> res=new HashSet<>();
		for (Group g : in)
			if (requester.hasGroup(g))
				res.add(g);
		return res;
	}

	private Set<KernelAddress> filterKernelAddresses(Map<Group, Set<KernelAddress>> in, Group group, AbstractAgent requester)
	{
		if (requester.hasGroup(group))
			return new HashSet<>(in.get(group));
		else
			return new HashSet<>();
	}

	Set<Group> getAccessibleGroupsGivenByDistantPeer(AbstractAgent requester, KernelAddress kernelAddress) {
		if (kernelAddress == null)
			throw new NullPointerException("kernelAddress");
		return filterGroups(distantAccessibleGroupsGivenByDistantPeer.get(kernelAddress), requester);
	}

	Set<Group> getAccessibleGroupsGivenToDistantPeer(AbstractAgent requester, KernelAddress kernelAddress) {
		if (kernelAddress == null)
			throw new NullPointerException("kernelAddress");
		return filterGroups(distantAccessibleGroupsGivenToDistantPeer.get(kernelAddress), requester);
	}

	Set<KernelAddress> getAccessibleKernelsPerGroupGivenByDistantPeer(AbstractAgent requester, Group group) {
		if (kernelAddress == null)
			throw new NullPointerException("kernelAddress");
		return filterKernelAddresses(distantAccessibleKernelsPerGroupsGivenByDistantPeer, group, requester);
	}


	Set<KernelAddress> getAccessibleKernelsPerGroupGivenToDistantPeer(AbstractAgent requester, Group group) {
		if (kernelAddress == null)
			throw new NullPointerException("kernelAddress");
		return filterKernelAddresses(distantAccessibleKernelsPerGroupsGivenToDistantPeer, group, requester);
	}

	Set<PairOfIdentifiers> getEffectiveDistantLogins(AbstractAgent requester, KernelAddress kernelAddress) {
		if (kernelAddress == null)
			throw new NullPointerException("kernelAddress");
		return acceptedDistantLogins.get(kernelAddress);
	}



    @SuppressWarnings("unused")
	void setMaximumGlobalUploadSpeedInBytesPerSecond(AbstractAgent requester, int max)
    {
        if (max<=0)
            throw new IllegalArgumentException("max must be greater than zero !");
        maximumGlobalUploadSpeedInBytesPerSecond=max;
		setHasSpeedLimitation();
    }
    @SuppressWarnings("unused")
    void setMaximumGlobalDownloadSpeedInBytesPerSecond(AbstractAgent requester, int max)
    {
        if (max<=0)
            throw new IllegalArgumentException("max must be greater than zero !");
        maximumGlobalDownloadSpeedInBytesPerSecond=max;
		setHasSpeedLimitation();
    }

    @SuppressWarnings("unused")
    int getMaximumGlobalUploadSpeedInBytesPerSecond(AbstractAgent requester) {
        return maximumGlobalUploadSpeedInBytesPerSecond;
    }

    @SuppressWarnings("unused")
    int getMaximumGlobalDownloadSpeedInBytesPerSecond(AbstractAgent requester) {
        return maximumGlobalDownloadSpeedInBytesPerSecond;
    }

    ReturnCode requestRole(AbstractAgent requester, Group group, String role, SecureExternalizable memberCard,
                           boolean manually_requested) {
		if (group.isUsedSubGroups())
			return ReturnCode.MULTI_GROUP_NOT_ACCEPTED;

		ReturnCode result;
		InternalGroup g;
		synchronized (organizations) {
			if (!group.isMadKitCreated(kernelAddress) && isAutoCreateGroup(null)) {
				createGroup(requester, group, memberCard, false);
			}

			try {
				g = getGroup(group);
			} catch (CGRNotAvailable e) {
				return e.getCode();
			}
			result = g.requestRole(requester, role, memberCard, manually_requested);
		}
		if (result == SUCCESS) {
			if (g.isDistributed()) {
				sendNetworkCGRSynchroMessageWithRole(requester, new CGRSynchro(REQUEST_ROLE,
						new AgentAddress(requester, g.get(role), kernelAddress, manually_requested),
						manually_requested));
			}
            informHooks(AgentActionEvent.REQUEST_ROLE,
                    new AgentAddress(requester, g.get(role), kernelAddress, manually_requested));
		}
		return result;
	}


	ReturnCode leaveGroup(final AbstractAgent requester, Group group, boolean manually_requested) {
		final InternalGroup g;
		final List<InternalRole> affectedRoles;
		if (group == null)
			throw new NullPointerException("_group");
		if (manually_requested)
			leaveAutoRequestedGroup(requester, group);
		synchronized (organizations) {
			if (group.isUsedSubGroups()) {
				for (Group gr : group.getSubGroups(kernelAddress))
					leaveGroup(requester, gr, manually_requested);
			}
			try {
				g = getGroup(group);
			} catch (CGRNotAvailable e) {
				return e.getCode();
			}
			affectedRoles = g.leaveGroup(requester, manually_requested);
		}
		if (affectedRoles != null) {// success
			if (g.isDistributed()) {
				sendNetworkCGRSynchroMessageWithRole(requester, new CGRSynchro(LEAVE_GROUP, new AgentAddress(requester,
						new InternalRole(group), kernelAddress, isAutoCreateGroup(requester)), manually_requested));
			}
            informHooks(AgentActionEvent.LEAVE_GROUP, new AgentAddress(requester, new InternalRole(group),
                    kernelAddress, isAutoCreateGroup(requester)));
			return SUCCESS;
		}
		return NOT_IN_GROUP;
	}


	ReturnCode leaveRole(AbstractAgent requester, Group _group, String role, boolean manualRequested) {
		final InternalRole r;
		if (manualRequested)
			leaveAutoRequestedRole(requester, role);
		synchronized (organizations) {
			if (_group.isUsedSubGroups()) {
				for (Group gr : _group.getSubGroups(kernelAddress))
					leaveRole(requester, gr, role, manualRequested);
			}
			try {
				r = getRole(_group, role);
			} catch (CGRNotAvailable e) {

				return e.getCode();
			}
			ReturnCode rc;
			// this is apart because I need the address before the leave
			if (r.getMyGroup().isDistributed()) {
				AgentAddress leaver = r.getAgentAddressOf(requester);
				if (leaver == null)
					return ReturnCode.ROLE_NOT_HANDLED;
				rc = r.removeMember(requester, manualRequested);
				if (rc != SUCCESS)// TODO remove that
					throw new AssertionError("cannot remove " + requester + " from " + r.buildAndGetAddresses());
				sendNetworkCGRSynchroMessageWithRole(requester, new CGRSynchro(LEAVE_ROLE,
						new AgentAddress(requester, r, kernelAddress, manualRequested), manualRequested));
			} else {
				rc = r.removeMember(requester, manualRequested);
			}
			if (rc == SUCCESS) {
				r.removeFromOverlookers(requester);
                informHooks(AgentActionEvent.LEAVE_ROLE,
                        new AgentAddress(requester, r, kernelAddress, manualRequested));
            }
			return rc;
		}
	}

	// Warning never touch this without looking at the logged kernel
	Set<AgentAddress> getAgentsWithRole(AbstractAgent requester, AbstractGroup group, String role,
			boolean callerIncluded) {
		if (group == null)
			throw new NullPointerException("group");
		if (role == null)
			throw new NullPointerException("role");

		HashSet<AgentAddress> set = new HashSet<>();
		try {
			for (Group g : getRepresentedGroups(group, role)) {
				try {
					if (callerIncluded) {
						set.addAll(getRole(g, role).buildAndGetAddresses());
					} else {
						List<AgentAddress> l = getOtherRolePlayers(requester, g, role);
						if (l != null)
							set.addAll(l);
					}
				} catch (CGRNotAvailable ignored) {

				}
			}
		} catch (CGRNotAvailable ignored) {

		}

		return set;

	}


	AgentAddress getAgentWithRole(AbstractAgent requester, AbstractGroup group, String role) {
		try {
			Group[] groups = getRepresentedGroups(group, role);
			ArrayList<AgentAddress> laa = new ArrayList<>(groups.length);
			for (Group g : groups) {
				AgentAddress aa = getAnotherRolePlayer(requester, g, role);
				if (aa != null)
					laa.add(aa);
			}
			if (laa.size() == 0)
				return null;
			else
				return laa.get((int) (Math.random() * laa.size()));

		} catch (CGRNotAvailable e) {
			return null;
		}
	}

	// ////////////////////////////////////////////////////////////
	// //////////////////////// Messaging interface
	// ////////////////////////////////////////////////////////////

	AgentAddress getDistantAgentWithRole(AbstractAgent abstractAgent, Group group, String role, KernelAddress from) {
		try {
			List<AgentAddress> l = getOtherRolePlayers(abstractAgent, group, role);
			if (l != null) {
				for (AgentAddress agentAddress : l) {
					if (agentAddress.getKernelAddress().equals(from))
						return agentAddress;
				}
			}
		} catch (CGRNotAvailable ignored) {
		}
		return null;
	}

	Group[] getRepresentedGroups(AbstractGroup group, String role) throws CGRNotAvailable {
		if (group == null)
			throw new NullPointerException("group");
		if (role == null)
			throw new NullPointerException("role");
		// Group groups[]=group.getRepresentedGroups(kernelAddress);
		Group[] groups = group.getRepresentedGroups(kernelAddress);
		if (groups.length == 0) {
			if (group instanceof Group) {
				if (role.equals(com.distrimind.madkit.agr.Organization.GROUP_MANAGER_ROLE)) {
					groups = new Group[1];
					groups[0] = ((Group) group).getThisGroupWithoutItsSubGroups();
				} else if (!isCommunity(((Group) group).getCommunity()))
					throw new CGRNotAvailable(ReturnCode.NOT_COMMUNITY);
				else if (!isCreatedGroup(((Group) group).getThisGroupWithoutItsSubGroups()))
					throw new CGRNotAvailable(ReturnCode.NOT_GROUP);
			} else
				throw new CGRNotAvailable(ReturnCode.NOT_GROUP);
		}

		return groups;

	}
	void receivedBigDataToRestartMessage(AbstractAgent requester, BigDataToRestartMessage message)
	{
		try {
			asynchronousBigDataTable.receiveAskingForTransferRestart(requester, message.getReceiver(), message.getSender(),
				message.getAsynchronousBigDataInternalIdentifier(), message.getPosition());
		} catch (DatabaseException e) {
			requester.getLogger().severeLog("Unexpected exception", e);
		}
	}

	boolean receivedPotentialAsynchronousBigDataResultMessage(AbstractAgent requester, BigDataResultMessage m)
	{
		if (m.isAsynchronousMessage())
		{
			try {
				return asynchronousBigDataTable.receivedPotentialAsynchronousBigDataResultMessage(requester, m.getType(),
						m.getAsynchronousBigDataInternalIdentifier(),
						m.getTransferredDataLength());
			} catch (DatabaseException e) {
				requester.getLogger().severeLog("Unexpected exception", e);
			}


		}
		return false;
	}


	ReturnCode sendMessageAndDifferItIfNecessary(final AbstractAgent requester, Group group, final String role,
												 final Message message, final String senderRole,
												 final long timeOutInMs)  {
		if (requester==null)
			throw new NullPointerException();
		if (message==null)
			throw new NullPointerException();
		if (group==null)
			throw new NullPointerException();
		if (role==null)
			throw new NullPointerException();
		if (senderRole==null)
			throw new NullPointerException();
		if (!(message instanceof com.distrimind.madkit.util.NetworkMessage))
			throw new IllegalArgumentException(message+" must implement NetworkMessage interface");

		if (asynchronousMessageTable ==null) {
			bugReport(new MadkitException("Cannot differ message when no database was loaded !"));
			return IGNORED;
		}
		try {
			message.setSender(new AgentAddress());
			return asynchronousMessageTable.differMessage(platform.getConfigOption().rootOfPathGroupUsedToFilterAsynchronousMessages,
					requester,group, senderRole, role, message, timeOutInMs);
		} catch (DatabaseException e) {
			bugReport(e);
			return IGNORED;
		}
	}
	AsynchronousBigDataTransferID sendBigDataAndDifferItIfNecessary(AbstractAgent requester, Group group, final String role, String senderRole,
																	ExternalAsynchronousBigDataIdentifier externalAsynchronousBigDataIdentifier,
																	SecureExternalizable attachedData,
																	MessageDigestType messageDigestType, boolean excludedFromEncryption, long timeOutInMs,
																	AsynchronousBigDataToSendWrapper asynchronousBigDataToSendWrapper) {
		if (requester==null)
			throw new NullPointerException();
		if (group==null)
			throw new NullPointerException();
		if (role==null)
			throw new NullPointerException();
		if (senderRole==null)
			throw new NullPointerException();

		if (asynchronousBigDataTable ==null) {
			bugReport(new MadkitException("Cannot differ message when no database was loaded !"));
			return null;
		}

		AsynchronousBigDataTable.Record r= asynchronousBigDataTable.startAsynchronousBigDataTransfer(
				platform.getConfigOption().rootOfPathGroupUsedToFilterAsynchronousMessages, requester,
				group, senderRole, role,
				externalAsynchronousBigDataIdentifier, attachedData, messageDigestType, excludedFromEncryption, timeOutInMs,
				asynchronousBigDataToSendWrapper);
		if (r==null)
			return null;
		else
		{
			return new AsynchronousBigDataTransferID(r.getAsynchronousBigDataInternalIdentifier(), r.getExternalAsynchronousBigDataIdentifier(), this);
		}
	}

	long cancelAsynchronousMessagesBySenderRole(AbstractAgent requester, Group group, String senderRole)  {
		if (group==null)
			throw new NullPointerException();
		if (senderRole==null)
			throw new NullPointerException();

		if (asynchronousMessageTable ==null)
		{
			bugReport(new MadkitException("Cannot cancel asynchronous messages when no database was loaded !"));
			return 0;
		}
		try {
			return asynchronousMessageTable.cancelAsynchronousMessagesBySenderRole(group, senderRole);
		} catch (DatabaseException e) {
			bugReport(e);
			return 0;
		}
	}
	long cancelAsynchronousMessagesByReceiverRole(AbstractAgent requester, Group group, String receiverRole)  {
		if (group==null)
			throw new NullPointerException();
		if (receiverRole==null)
			throw new NullPointerException();

		if (asynchronousMessageTable ==null) {
			bugReport(new MadkitException("Cannot cancel asynchronous messages when no database was loaded !"));
			return 0;
		}
		try {
			return asynchronousMessageTable.cancelAsynchronousMessagesByReceiverRole(group, receiverRole);
		} catch (DatabaseException e) {
			bugReport(e);
			return 0;
		}
	}
	long cancelAsynchronousMessagesByGroup(AbstractAgent requester, Group group)  {
		if (group==null)
			throw new NullPointerException();

		if (asynchronousMessageTable ==null) {
			bugReport(new MadkitException("Cannot cancel asynchronous messages when no database was loaded !"));
			return 0;
		}
		try {
			return asynchronousMessageTable.cancelAsynchronousMessagesByGroup(group);
		} catch (DatabaseException e) {
			bugReport(e);
			return 0;
		}

	}

	void cleanObsoleteMaDKitData(AbstractAgent requester)
	{
		if (asynchronousMessageTable!=null)
		{
			asynchronousBigDataTable.cleanObsoleteData(this);
			asynchronousMessageTable.cleanObsoleteData();
			Runnable additionalDataCleaner=this.additionalDataCleaner;
			if (additionalDataCleaner!=null)
				additionalDataCleaner.run();
		}
	}

	void setAutomaticObsoleteDataCleaningDelay(AbstractAgent requester, MadkitKernel parentMadkitKernel, long delayInMsBeforeCleaningObsoleteMadkitData)
	{
		MadkitKernel madkitKernel=parentMadkitKernel==null?this:parentMadkitKernel;
		if (databaseCleanerTaskID!=null) {
			cancelTask(requester, databaseCleanerTaskID, false);
			databaseCleanerTaskID=null;
		}
		if (delayInMsBeforeCleaningObsoleteMadkitData>0)
		{
			getMadkitConfig().delayInMsBeforeCleaningObsoleteMadkitData=delayInMsBeforeCleaningObsoleteMadkitData=Math.max(20L*60L*1000L, delayInMsBeforeCleaningObsoleteMadkitData);
			databaseCleanerTaskID=scheduleTask(requester, new Task<>((Callable<Void>) () -> {
				madkitKernel.cleanObsoleteMaDKitData(madkitKernel);
				return null;
			}, System.currentTimeMillis()+delayInMsBeforeCleaningObsoleteMadkitData, delayInMsBeforeCleaningObsoleteMadkitData),false);
		}
		else {

			getMadkitConfig().delayInMsBeforeCleaningObsoleteMadkitData = delayInMsBeforeCleaningObsoleteMadkitData;
		}
	}

	void setAdditionalObsoleteDataCleaner(AbstractAgent requester, Runnable additionalDataCleaner)
	{
		this.additionalDataCleaner=additionalDataCleaner;
	}

	List<AsynchronousMessageTable.Record> getAsynchronousMessagesBySenderRole(AbstractAgent requester, Group group, String senderRole)  {
		if (group==null)
			throw new NullPointerException();
		if (senderRole==null)
			throw new NullPointerException();

		if (asynchronousMessageTable ==null)
		{
			bugReport(new MadkitException("Cannot get asynchronous messages when no database was loaded !"));
			return new ArrayList<>(0);
		}
		try {
			return asynchronousMessageTable.getAsynchronousMessagesBySenderRole(group, senderRole);
		} catch (DatabaseException e) {
			bugReport(e);
			return new ArrayList<>(0);
		}
	}
	List<AsynchronousMessageTable.Record> getAsynchronousMessagesByReceiverRole(AbstractAgent requester, Group group, String receiverRole)  {
		if (group==null)
			throw new NullPointerException();
		if (receiverRole==null)
			throw new NullPointerException();

		if (asynchronousMessageTable ==null) {
			bugReport(new MadkitException("Cannot get asynchronous messages when no database was loaded !"));
			return new ArrayList<>(0);
		}
		try {
			return asynchronousMessageTable.getAsynchronousMessagesByReceiverRole(group, receiverRole);
		} catch (DatabaseException e) {
			bugReport(e);
			return new ArrayList<>(0);
		}
	}
	List<AsynchronousMessageTable.Record> getAsynchronousMessagesByGroup(AbstractAgent requester, Group group)  {
		if (group==null)
			throw new NullPointerException();

		if (asynchronousMessageTable ==null) {
			bugReport(new MadkitException("Cannot get asynchronous messages when no database was loaded !"));
			return new ArrayList<>(0);
		}
		try {
			return asynchronousMessageTable.getAsynchronousMessagesByGroup(group);
		} catch (DatabaseException e) {
			bugReport(e);
			return new ArrayList<>(0);
		}
	}

	long getAsynchronousMessagesNumberBySenderRole(AbstractAgent requester, Group group, String senderRole)  {
		if (group==null)
			throw new NullPointerException();
		if (senderRole==null)
			throw new NullPointerException();

		if (asynchronousMessageTable ==null)
		{
			bugReport(new MadkitException("Cannot get asynchronous messages when no database was loaded !"));
			return 0;
		}
		try {
			return asynchronousMessageTable.getAsynchronousMessagesNumberBySenderRole(group, senderRole);
		} catch (DatabaseException e) {
			bugReport(e);
			return 0;
		}
	}
	long getAsynchronousMessagesNumberByReceiverRole(AbstractAgent requester, Group group, String receiverRole)  {
		if (group==null)
			throw new NullPointerException();
		if (receiverRole==null)
			throw new NullPointerException();

		if (asynchronousMessageTable ==null) {
			bugReport(new MadkitException("Cannot get asynchronous messages when no database was loaded !"));
			return 0;
		}
		try {
			return asynchronousMessageTable.getAsynchronousMessagesNumberByReceiverRole(group, receiverRole);
		} catch (DatabaseException e) {
			bugReport(e);
			return 0;
		}
	}
	long getAsynchronousMessagesNumberByGroup(AbstractAgent requester, Group group)  {
		if (group==null)
			throw new NullPointerException();

		if (asynchronousMessageTable ==null) {
			bugReport(new MadkitException("Cannot get asynchronous messages when no database was loaded !"));
			return 0;
		}
		try {
			return asynchronousMessageTable.getAsynchronousMessagesNumberByGroup(group);
		} catch (DatabaseException e) {
			bugReport(e);
			return 0;
		}
	}
	private List<AsynchronousBigDataTransferID> getAsynchronousBigDataTransferIDs(AbstractAgent requester, List<AsynchronousBigDataTable.Record> l)
	{
		List<AsynchronousBigDataTransferID> res=new ArrayList<>(l.size());
		for (AsynchronousBigDataTable.Record r : l)
			res.add(getAsynchronousBigDataTransferIDInstance(requester, r.getExternalAsynchronousBigDataIdentifier()));
		return res;
	}
	List<AsynchronousBigDataTransferID> getAsynchronousBigDataTransferIDsByGroup(AbstractAgent requester, Group group)
	{
		return getAsynchronousBigDataTransferIDs(requester, getAsynchronousBigDataMessagesByGroup(requester, group));
	}
	List<AsynchronousBigDataTransferID> getAsynchronousBigDataTransferIDsBySenderRole(AbstractAgent requester, Group group, String senderRole)
	{
		return getAsynchronousBigDataTransferIDs(requester, getAsynchronousBigDataMessagesBySenderRole(requester, group, senderRole));
	}
	List<AsynchronousBigDataTransferID> getAsynchronousBigDataTransferIDsByReceiverRole(AbstractAgent requester, Group group, String receiverRole)
	{
		return getAsynchronousBigDataTransferIDs(requester, getAsynchronousBigDataMessagesByReceiverRole(requester, group, receiverRole));
	}
	List<AsynchronousBigDataTable.Record> getAsynchronousBigDataMessagesByGroup(AbstractAgent requester, Group group)
	{
		if (group==null)
			throw new NullPointerException();

		if (asynchronousBigDataTable ==null) {
			bugReport(new MadkitException("Cannot get asynchronous messages when no database was loaded !"));
			return Collections.emptyList();
		}
		try {
			return asynchronousBigDataTable.getAsynchronousMessagesByGroup(group);
		} catch (DatabaseException e) {
			bugReport(e);
			return Collections.emptyList();
		}
	}
	List<AsynchronousBigDataTable.Record> getAsynchronousBigDataMessagesBySenderRole(AbstractAgent requester, Group group, String senderRole)
	{
		if (group==null)
			throw new NullPointerException();

		if (asynchronousBigDataTable ==null) {
			bugReport(new MadkitException("Cannot get asynchronous messages when no database was loaded !"));
			return Collections.emptyList();
		}
		try {
			return asynchronousBigDataTable.getAsynchronousMessagesBySenderRole(group, senderRole);
		} catch (DatabaseException e) {
			bugReport(e);
			return Collections.emptyList();
		}
	}
	List<AsynchronousBigDataTable.Record> getAsynchronousBigDataMessagesByReceiverRole(AbstractAgent requester, Group group, String receiverRole)
	{
		if (group==null)
			throw new NullPointerException();

		if (asynchronousBigDataTable ==null) {
			bugReport(new MadkitException("Cannot get asynchronous messages when no database was loaded !"));
			return Collections.emptyList();
		}
		try {
			return asynchronousBigDataTable.getAsynchronousMessagesByReceiverRole(group, receiverRole);
		} catch (DatabaseException e) {
			bugReport(e);
			return Collections.emptyList();
		}
	}
	long getAsynchronousBigDataMessagesNumberByGroup(AbstractAgent requester, Group group)
	{
		if (group==null)
			throw new NullPointerException();

		if (asynchronousBigDataTable ==null) {
			bugReport(new MadkitException("Cannot get asynchronous messages when no database was loaded !"));
			return 0;
		}
		try {
			return asynchronousBigDataTable.getAsynchronousMessagesNumberByGroup(group);
		} catch (DatabaseException e) {
			bugReport(e);
			return 0;
		}
	}
	long getAsynchronousBigDataMessagesNumberBySenderRole(AbstractAgent requester, Group group, String senderRole)
	{
		if (group==null)
			throw new NullPointerException();

		if (asynchronousBigDataTable ==null) {
			bugReport(new MadkitException("Cannot get asynchronous messages when no database was loaded !"));
			return 0;
		}
		try {
			return asynchronousBigDataTable.getAsynchronousMessagesNumberBySenderRole(group, senderRole);
		} catch (DatabaseException e) {
			bugReport(e);
			return 0;
		}
	}
	long getAsynchronousBigDataMessagesNumberByReceiverRole(AbstractAgent requester, Group group, String receiverRole)
	{
		if (group==null)
			throw new NullPointerException();

		if (asynchronousBigDataTable ==null) {
			bugReport(new MadkitException("Cannot get asynchronous messages when no database was loaded !"));
			return 0;
		}
		try {
			return asynchronousBigDataTable.getAsynchronousMessagesNumberByReceiverRole(group, receiverRole);
		} catch (DatabaseException e) {
			bugReport(e);
			return 0;
		}
	}
	long cancelAsynchronousBigDataMessagesByGroup(AbstractAgent requester, Group group)  {
		if (group==null)
			throw new NullPointerException();

		if (asynchronousBigDataTable ==null) {
			bugReport(new MadkitException("Cannot cancel asynchronous messages when no database was loaded !"));
			return 0;
		}
		try {
			return asynchronousBigDataTable.cancelAsynchronousMessagesByGroup(requester, group);
		} catch (DatabaseException e) {
			bugReport(e);
			return 0;
		}
	}
	long cancelAsynchronousBigDataMessagesBySenderRole(AbstractAgent requester, Group group, String senderRole)  {
		if (group==null)
			throw new NullPointerException();

		if (asynchronousBigDataTable ==null) {
			bugReport(new MadkitException("Cannot cancel asynchronous messages when no database was loaded !"));
			return 0;
		}
		try {
			return asynchronousBigDataTable.cancelAsynchronousMessagesBySenderRole(requester, group, senderRole);
		} catch (DatabaseException e) {
			bugReport(e);
			return 0;
		}
	}
	long cancelAsynchronousBigDataMessagesByReceiverRole(AbstractAgent requester, Group group, String receiverRole)  {
		if (group==null)
			throw new NullPointerException();

		if (asynchronousBigDataTable ==null) {
			bugReport(new MadkitException("Cannot cancel asynchronous messages when no database was loaded !"));
			return 0;
		}
		try {
			return asynchronousBigDataTable.cancelAsynchronousMessagesByReceiverRole(requester, group, receiverRole);
		} catch (DatabaseException e) {
			bugReport(e);
			return 0;
		}
	}
	ReturnCode sendMessage(final AbstractAgent requester, AbstractGroup group, final String role, final Message message,
			final String senderRole) {
		try {
			ArrayList<AgentAddress> receivers = new ArrayList<>();
			Group[] groups = getRepresentedGroups(group, role);

			if (groups.length == 0)
				return ReturnCode.NOT_ROLE;
			for (Group g : groups) {
				AgentAddress receiver = getAnotherRolePlayer(requester, g, role);
				if (receiver != null)
					receivers.add(receiver);
			}
			if (receivers.size() == 0) {
				return NO_RECIPIENT_FOUND;
			}

			AgentAddress receiver = receivers.get((int) (Math.random() * receivers.size()));
			return buildAndSendMessage(getSenderAgentAddress(requester, receiver, senderRole), receiver, message);
		} catch (CGRNotAvailable e) {
			return e.getCode();
		}
	}

	ReturnCode sendMessage(AbstractAgent requester, AgentAddress receiver, final Message message,
			final String senderRole) {
		// check that the AA is valid : the targeted agent is still playing the
		// corresponding role or it was a candidate request
		AgentAddress target = resolveAddress(receiver);
		if (target == null) {
			if (receiver instanceof CandidateAgentAddress)
				target = receiver;
			else
				return INVALID_AGENT_ADDRESS;

		}

		if (target.getAgent() != null) {

			State s = target.getAgent().getState();

			if (s.compareTo(LIVING) > 0)
				return ReturnCode.INVALID_AGENT_ADDRESS;
		}

		try {
			// get the role for the sender and then send
			AgentAddress senderAA = getSenderAgentAddress(requester, target, senderRole);

			return buildAndSendMessage(senderAA, target, message);

		} catch (CGRNotAvailable e) {

			return e.getCode();
		}
	}

	final AgentAddress resolveAddress(AgentAddress receiver) {
		final InternalRole roleObject = receiver.getRoleObject();
		if (roleObject != null) {

			if (roleObject.players == null) {// has been traveling
				try {
					return getRole(roleObject.getGroup(), roleObject.getRoleName()).resolveAgentAddress(receiver);
				} catch (CGRNotAvailable e) {
					return null;
				}
			}
			else
				return roleObject.resolveAgentAddress(receiver);
			//return receiver;
		}
		return null;
	}

	boolean isConcernedBy(AbstractAgent requester, AgentAddress agentAddress) {
		if (agentAddress == null)
			return false;
		agentAddress = resolveAddress(agentAddress);
		if (agentAddress == null)
			return false;
		else
			return requester == agentAddress.getAgent();
	}

	boolean isLocalAgentAddressValid(AbstractAgent requester, AgentAddress agentAddress) {
		if (agentAddress == null)
			return false;
		agentAddress = resolveAddress(agentAddress);
		return agentAddress != null && agentAddress.getAgent() != null;
	}

	ReturnCode broadcastMessageWithRole(final AbstractAgent requester, AbstractGroup group, final String role,
			final Message messageToSend, String senderRole, boolean receiveAllRepliesInOneBlock) {
		return this.broadcastMessageWithRole(requester, group, role, messageToSend, senderRole, null, false,
				receiveAllRepliesInOneBlock);
	}

	ReturnCode broadcastMessageWithRole(final AbstractAgent requester, AbstractGroup group, final String role,
			final Message messageToSend, String senderRole, AtomicInteger numberOfReceivers,
			boolean sendIndividualNetwork, boolean receiveAllRepliesInOneBlock) {
		ArrayList<AgentAddress> agentAddressesSender = new ArrayList<>();
		AgentAddress oneReceiver = null;
		ReturnCode notAvailable = null;

		if (numberOfReceivers == null)
			numberOfReceivers = new AtomicInteger(0);
		AtomicInteger nonSentNetworkNumberOfReceivers=new AtomicInteger(0);
		Replies replies = null;
		if (receiveAllRepliesInOneBlock) {
			sendIndividualNetwork = true;
			requester.addConversation(replies = new Replies(messageToSend));
			messageToSend.setNeedReply(true);
		}
		messageToSend.getConversationID().setOrigin(kernelAddress);
		Group[] representedGroups;
		try {
			representedGroups = getRepresentedGroups(group, role);
			for (Group g : representedGroups) {
				try {
					List<AgentAddress> receivers = getOtherRolePlayers(requester, g, role);
					if (receivers != null && receivers.size() > 0) {

						final AgentAddress senderAgentAddress = getSenderAgentAddress(requester,
								receivers.iterator().next(), senderRole);
						// receivers.addAll(aas);
						oneReceiver = receivers.iterator().next();
						if (senderAgentAddress.getGroup().isDistributed())
							agentAddressesSender.add(senderAgentAddress);
						messageToSend.setSender(senderAgentAddress);
						broadcasting(receivers, messageToSend, numberOfReceivers, nonSentNetworkNumberOfReceivers, sendIndividualNetwork);
						if (numberOfReceivers.get()>0)
							notAvailable=null;
					}
				} catch (CGRNotAvailable e) {
					notAvailable = e.getCode();
				}
			}
		} catch (CGRNotAvailable e) {
			notAvailable = e.getCode();
		}



		ReturnCode rc = null;

		if (!sendIndividualNetwork && getMadkitConfig().networkProperties.network && agentAddressesSender.size() > 0 && nonSentNetworkNumberOfReceivers.get()>0) {
			rc = broadcastNetworkMessageWithRole(messageToSend, agentAddressesSender.iterator().next(), group, role,
					agentAddressesSender);
			if (rc == SUCCESS || rc == TRANSFER_IN_PROGRESS) {
				numberOfReceivers.addAndGet(nonSentNetworkNumberOfReceivers.get());
				if (nonSentNetworkNumberOfReceivers.get() > 0)
					notAvailable = null;
			}
		}
		if (numberOfReceivers.get() == 0) {
			if (notAvailable == null) {
				notAvailable = NO_RECIPIENT_FOUND;

			}
			notAvailable.setNumberOfConcernedAgents(0);

			return notAvailable;
		}
		if (rc==null)
			rc= SUCCESS;
		rc.setNumberOfConcernedAgents(numberOfReceivers.get());

		if (replies != null) {
			boolean remove;
			if (numberOfReceivers.get() > 0) {
				replies.setAllMessagesSent(numberOfReceivers.get());
				remove = replies.areAllRepliesSent();
			} else
				remove = true;
			if (remove) {
				if (requester.removeConversation(replies) && numberOfReceivers.get() > 0)
					requester.receiveMessage(replies);
			}
		}

        messageToSend.setReceiver(oneReceiver);
        informHooks(AgentActionEvent.BROADCAST_MESSAGE, messageToSend);
        return rc;

	}

	List<Message> broadcastMessageWithRoleAndWaitForReplies(final AbstractAgent requester, AbstractGroup group,
			final String role, Message messageToSend, final String senderRole, final Integer timeOutMilliSeconds)
			throws InterruptedException {
		AtomicInteger numberOfReceivers = new AtomicInteger(0);
		messageToSend.setNeedReply(true);
		ReturnCode rc = this.broadcastMessageWithRole(requester, group, role, messageToSend, senderRole,
				numberOfReceivers, true, false);
		if (rc.equals(ReturnCode.SUCCESS)) {
			return requester.waitAnswers(messageToSend, numberOfReceivers.get(), timeOutMilliSeconds);
		} else {
			if (requester.getKernel() != this && requester.isWarningOn()) {// is
				// loggable

				if (rc == NO_RECIPIENT_FOUND) {
					requester.handleException(Influence.BROADCAST_MESSAGE_AND_WAIT, new MadkitWarning(rc));
				} else if (rc == ROLE_NOT_HANDLED) {
					requester.handleException(Influence.BROADCAST_MESSAGE_AND_WAIT,
							new OrganizationWarning(rc, new Group("null", "null"), senderRole));
				} else {
					requester.handleException(Influence.BROADCAST_MESSAGE_AND_WAIT,
							new OrganizationWarning(rc, new Group("null", "null"), role));
				}
			}
			return null;
		}
	}

	private void broadcasting(final Collection<AgentAddress> receivers, Message m, AtomicInteger numberOfReceivers,AtomicInteger nonSentNetworkNumberOfReceivers,
			boolean includeNetworkAgents) {// TODO
		// optimize
		// without
		m.getConversationID().setOrigin(kernelAddress); // cloning
		AgentAddress sender = m.getSender();

		for (final AgentAddress agentAddress : receivers) {

			if (agentAddress != null) {// TODO this should not be possible
				if (includeNetworkAgents || agentAddress.getAgent() != null) {
					m = m.clone();
					m.setSender(sender);
					m.setReceiver(agentAddress);

					ReturnCode rc = sendMessage(m, agentAddress.getAgent());
					if ((rc == ReturnCode.SUCCESS || rc == ReturnCode.TRANSFER_IN_PROGRESS)
							&& numberOfReceivers != null)
						numberOfReceivers.incrementAndGet();
				}
				else if (nonSentNetworkNumberOfReceivers != null)
					nonSentNetworkNumberOfReceivers.incrementAndGet();
			}
		}

	}

	final ReturnCode broadcastNetworkMessageWithRole(Message m, AgentAddress role, AbstractGroup _destination_groups,
			String _destination_role, ArrayList<AgentAddress> _agentAddressesSender) {
		updateNetworkAgent();
		if (netAgent != null && distantKernelAddresses.size() > 0) {
			try {
				m.setSender(role);
				m.getConversationID().setOrigin(kernelAddress);
                LocalLanMessage localLanMessage = MadkitNetworkAccess.getBroadcastLocalLanMessage(m, _destination_groups,
						_destination_role, _agentAddressesSender);
                if (localLanMessage==null)
                	throw new NullPointerException();

                ((Message)Objects.requireNonNull(localLanMessage)).setSender(netEmitter);
                ((Message)localLanMessage).setReceiver(netAgent);
				netAgent.getAgent().receiveMessage(localLanMessage);

                return localLanMessage.getMessageLocker().waitUnlock(_agentAddressesSender.get(0).getAgent(),
						true);
			} catch (InterruptedException e) {
				if (logger != null)
					logger.severeLog("A problem occurs during a lan message sending", e);
				return ReturnCode.TRANSFER_FAILED;
			}

		} else
			return NO_RECIPIENT_FOUND;
	}

	private ReturnCode sendMessage(Message m, AbstractAgent target) {

		if (target == null) {

			return sendNetworkMessageWithRole(m);
		}
		target.receiveMessage(m);
		return SUCCESS;
		/*
		 * if (target.receiveMessage(m)!=null || m!=null) return SUCCESS; else return
		 * ReturnCode.NO_RECIPIENT_FOUND;
		 */
	}

	@SuppressWarnings("unused")
	final ReturnCode sendNetworkCGRSynchroMessageWithRole(AbstractAgent requester, CGRSynchro m) {
		updateNetworkAgent();
		if (netAgent != null) {
			m.getConversationID().setOrigin(getKernelAddress());
			((Message) m).setSender(netUpdater);
			((Message) m).setReceiver(netAgent);

			/*if (lockSocketUntilCGRSynchroIsSent && requester!=null) {
				m.initMessageLocker();
				netAgent.getAgent().receiveMessage(m);
				try {
					return m.getMessageLocker().waitUnlock(requester, true);
				} catch (InterruptedException e) {
					if (logger != null)
						logger.severeLog("A problem occurs during a lan CGR sending", e);
					return ReturnCode.TRANSFER_FAILED;
				}
			}
			else {*/
				netAgent.getAgent().receiveMessage(m);
				return SUCCESS;
			//}
		}
		return SEVERE;
	}

	final ReturnCode sendNetworkKernelMessageWithRole(KernelMessage m) {
		updateNetworkAgent();
		if (netAgent != null) {
			((Message) m).setSender(kernelRole);
			((Message) m).setReceiver(netAgent);
			netAgent.getAgent().receiveMessage(m);
			return SUCCESS;
		}
		return SEVERE;
	}

	private ReturnCode sendNetworkMessageWithRole(Message m) {
		updateNetworkAgent();
		if (netAgent != null) {
			try {

                DirectLocalLanMessage directLocalLanMessage = MadkitNetworkAccess.getDirectLocalLanMessageInstance(m);
                if (directLocalLanMessage==null)
                	throw new NullPointerException();

                ((Message)Objects.requireNonNull(directLocalLanMessage)).setSender(netEmitter);
                ((Message)directLocalLanMessage).setReceiver(netAgent);
				netAgent.getAgent().receiveMessage(directLocalLanMessage);
				return directLocalLanMessage.getMessageLocker().waitUnlock(m.getSender().getAgent(), true);
			} catch (InterruptedException e) {
				if (logger != null)
					logger.severeLog("A problem occurs during a lan message sending", e);
				return ReturnCode.TRANSFER_FAILED;
			}

		}
		return SEVERE;
	}

	private void updateNetworkAgent() {
		if (netAgent == null || !checkAgentAddress(netAgent)) {// Is it still playing the
			// role ?
			netAgent = getAgentWithRole(Groups.NETWORK, com.distrimind.madkit.agr.LocalCommunity.Roles.NET_AGENT);
		}
	}

	ReturnCode anomalyDetectedWithOneConnection(AbstractAgent requester, boolean candidateToBan,
			ConnectionIdentifier connection_identifier, String message) {
		updateNetworkAgent();
		if (netAgent != null) {
			Message m = new AnomalyDetectedMessage(candidateToBan, connection_identifier, message);
			m.setSender(netSecurity);
			m.setReceiver(netAgent);
			netAgent.getAgent().receiveMessage(m);
			return ReturnCode.SUCCESS;
		}
		return SEVERE;
	}

	ReturnCode anomalyDetectedWithOneDistantKernel(AbstractAgent requester, boolean candidateToBan,
			KernelAddress kernelAddress, String message) {
		updateNetworkAgent();
		if (netAgent != null) {
			Message m = new AnomalyDetectedMessage(candidateToBan, kernelAddress, message);
			m.setSender(netSecurity);
			m.setReceiver(netAgent);
			netAgent.getAgent().receiveMessage(m);
			return ReturnCode.SUCCESS;
		}
		return SEVERE;
	}

	// ////////////////////////////////////////////////////////////
	// //////////////////////// Launching and Killing
	// ////////////////////////////////////////////////////////////


	void launchAgentBucketWithRoles(final AbstractAgent requester, List<AbstractAgent> bucket, int cpuCoreNb,
			Role... cgrLocations) {
		cpuCoreNb=cpuCoreNb>0?cpuCoreNb:1;
		if (cgrLocations != null && cgrLocations.length != 0) {
			class AJ extends AgentsJob
			{
				AJ(List<AbstractAgent> list) {
					super(list);
				}

				@Override
				protected AJ clone() throws CloneNotSupportedException {
					return new AJ(getClonedList());
				}

				AJ() {
				}

				@Override
				AgentsJob createNewAgentJobWithList(List<AbstractAgent> l) {
					return new AJ(l);
				}

				@Override
				void proceedAgent(AbstractAgent a) {
					// no need to test : I created these instances :this is not true for the list
					// case //TODO
					a.state.set(INITIALIZING);
					a.setKernel(MadkitKernel.this);
					a.getAlive().set(true);
					if (a.logger!=null && a.logger!= AgentLogger.defaultAgentLogger)
						a.logger.close();
					a.logger = null;
				}
			}
			AgentsJob init = new AJ();
			doMulticore(init.getJobs(bucket, cpuCoreNb));
			synchronized (this) {
				for (final Role cgrLocation : cgrLocations) {
					createGroup(requester, cgrLocation.getGroup(), null, false);
					InternalGroup g;
					try {
						g = getGroup(cgrLocation.getGroup());
					} catch (CGRNotAvailable e) {
						// not possible
						throw new AssertionError(e);
					}
					boolean roleCreated = false;
					InternalRole r = g.get(cgrLocation.getRole());
					if (r == null) {
						r = g.createRole(cgrLocation.getRole());
						roleCreated = true;
					}
					r.addMembers(bucket, roleCreated, true);
					// test vs assignment ? -> No: cannot touch the organizational
					// structure !!
				}
				class AJ2 extends AgentsJob
				{
					@Override
					protected AJ2 clone() throws CloneNotSupportedException {
						return new AJ2(getClonedList());
					}
					AJ2(List<AbstractAgent> list) {
						super(list);
					}

					AJ2() {
					}

					@Override
					AgentsJob createNewAgentJobWithList(List<AbstractAgent> l) {
						return new AJ2(l);
					}

					@Override
					void proceedAgent(AbstractAgent a) {
						try {
							a.activate();
							a.state.set(State.ACTIVATED);
						} catch (Throwable e) {
							requester.cannotLaunchAgent(a != null ? a.getClass().getName()
									: "launchAgentBucketWithRoles : list contains null", e, null);
						}
					}
				}

				init = new AJ2();
				doMulticore(init.getJobs(bucket, cpuCoreNb));
			}
		} else {
			class AJ extends AgentsJob
			{

				@Override
				protected AJ clone() throws CloneNotSupportedException {
					return new AJ(getClonedList());
				}
				AJ(List<AbstractAgent> list) {
					super(list);
				}

				AJ() {
				}

				@Override
				AgentsJob createNewAgentJobWithList(List<AbstractAgent> l) {
					return new AJ(l);
				}

				@Override
				void proceedAgent(AbstractAgent a) {
					// no need to test : I created these instances :this is not true for the list
					// case //TODO
					a.state.set(ACTIVATING);
					a.setKernel(MadkitKernel.this);
					a.getAlive().set(true);
					if (a.logger!=null && a.logger!= AgentLogger.defaultAgentLogger)
						a.logger.close();
					a.logger = null;
					try {
						a.activate();
						synchronized (a.state) {
							a.state.set(State.ACTIVATED);
							a.state.notifyAll();
						}
					} catch (Throwable e) {
						requester.cannotLaunchAgent("launchAgentBucketWithRoles : " + a.getClass().getName(), e, null);
					}
				}
			}
			AgentsJob aj = new AJ();
			doMulticore(aj.getJobs(bucket, cpuCoreNb));
		}
	}


	List<AbstractAgent> createBucket(final String agentClass, int bucketSize, int cpuCoreNb)
			throws InstantiationException, IllegalAccessException, ClassNotFoundException, IllegalArgumentException, InvocationTargetException, NoSuchMethodException, SecurityException {
		if (agentClass == null)
			throw new NullPointerException("agentClass");
		@SuppressWarnings("unchecked")
		final Class<? extends AbstractAgent> constructor = (Class<? extends AbstractAgent>) MadkitClassLoader
				.getLoader().loadClass(agentClass);
		cpuCoreNb = cpuCoreNb > 0 ? cpuCoreNb : 1;
		final List<AbstractAgent> result = new ArrayList<>(bucketSize);
		final int nbOfAgentsPerTask = bucketSize / (cpuCoreNb);

		//final CompletionService<List<AbstractAgent>> ecs = new ExecutorCompletionService<>(serviceExecutor);
		ArrayList<Future<List<AbstractAgent>>> futures=new ArrayList<>(cpuCoreNb);
		for (int i = 0; i < cpuCoreNb; i++) {
			futures.add(serviceExecutor.submit(() -> {
				final List<AbstractAgent> list = new ArrayList<>(nbOfAgentsPerTask);
				for (int j = nbOfAgentsPerTask; j > 0; j--) {
					list.add(constructor.getDeclaredConstructor().newInstance());
				}
				return list;
			}));
		}
		// adding the missing ones when the division results as a real number
		for (int i = bucketSize - nbOfAgentsPerTask * cpuCoreNb; i > 0; i--) {
			result.add(constructor.getConstructor().newInstance());
		}
		for (int i = 0; i < cpuCoreNb; ++i) {
			try {
				result.addAll(futures.get(i).get());
			} catch (InterruptedException | ExecutionException e) {
				e.printStackTrace();
			}
        }
		return result;
	}

	private void doMulticore(ArrayList<AgentsJob> arrayList) {
		try {
			serviceExecutor.invokeAll(arrayList);
		} catch (InterruptedException e1) {
			e1.printStackTrace();
		}
	}

	ReturnCode launchAgent(final AbstractAgent requester, final AbstractAgent agent, final int timeOutSeconds,
			final boolean defaultGUI) {

		long timeOutNano=getTimeOutNanoFromSeconds(timeOutSeconds);


		try {
			if (requester == null)
				throw new NullPointerException("requester");
			if (agent == null)
				throw new NullPointerException("agent");
			if (logger != null && logger.isLoggable(Level.FINEST))
				logger.finest(requester + " launching " + agent + " by " + Thread.currentThread());
			ReturnCode returnCode;

			Future<ReturnCode> future = serviceExecutor.submit(() -> launchingAgent(agent, defaultGUI));

			returnCode = future.get(timeOutNano, TimeUnit.NANOSECONDS);
			if (returnCode == AGENT_CRASH || returnCode == ALREADY_LAUNCHED) {
				Exception e = new MadkitWarning(returnCode);
				requester.getLogger().severeLog(Influence.LAUNCH_AGENT.failedString(), e);
			}

			return returnCode;
		} catch (InterruptedException e) {// requester has been killed or
											// something
			// requester.handleInterruptedException();
			return this.killAgent(requester, agent, 0, KillingType.JUST_KILL_IT);
			// return TIMEOUT;
		} catch (ExecutionException e) {// BUG on launching agent
			bugReport("Launching task failed on " + agent, e);
			return SEVERE;
		} catch (TimeoutException e) {// launch task time out
			return TIMEOUT;
		}
	}

	protected ReturnCode launchingAgent(final AbstractAgent agent, boolean defaultGUI) {
		// All this has to be done by a system thread
		// because if the job starts, it has to be done till the end

		synchronized (agent.state) {
			if (!agent.state.compareAndSet(NOT_LAUNCHED, INITIALIZING) || shutDown) {
				return ALREADY_LAUNCHED;
			}
			agent.state.notifyAll();
		}

		// System.err.println("adding "+agent.getName()+" using
		// "+Thread.currentThread()+
		// agent.getState());
		agent.setKernel(this);
		informHooks(AgentActionEvent.AGENT_STARTED, agent);
		if (defaultGUI)
			agent.createGUIOnStartUp();
		Level defaultLevel = getMadkitConfig().agentLogLevel;
		if (agent.logger == AgentLogger.defaultAgentLogger) {// not changed in the
																// constructor
			if (defaultLevel == Level.OFF) {// default not changed and global is
											// OFF
				agent.logger = null;
			} else {
				agent.setLogLevel(defaultLevel);
				agent.getLogger().setWarningLogLevel(getMadkitConfig().warningLogLevel);
			}
		}
		// final AgentExecutor ae = agent.getAgentExecutor();

		// AbstractAgent
		if (!(agent instanceof Agent)) {
			ReturnCode r = AGENT_CRASH;
			final Future<ReturnCode> activationAttempt = serviceExecutor.submit(agent::activation);
			try {
				r = activationAttempt.get();
			} catch (ExecutionException | InterruptedException e) {
				bugReport(agent + " activation task failed using " + Thread.currentThread(), e);
			}
            if (r != SUCCESS) {
				synchronized (agent.state) {
					agent.state.notifyAll();
				}
				startEndBehavior(agent, false);
			} else {
				if (agent.isAlive()) {// ! self kill -> safe to make this here

					if (agent instanceof AgentFakeThread) {
						agent.messageBox.getLocker().lock();
						try {
							agent.state.set(LIVING);
							if (!agent.messageBox.isEmpty())
								((AgentFakeThread) agent).manageTaskMessage(true);
						} finally {
							agent.messageBox.getLocker().unlock();
						}
					} else
						agent.state.set(LIVING);

				}
			}
			return r;
		}
		// Agent
		try {
			final Agent a = (Agent) agent;
			final AgentExecutor ae = a.getAgentExecutor();
			synchronized (threadedAgents) {
				// do that even if not started for cleaning properly
				threadedAgents.add(a);
			}
			//ae.setThreadFactory(a.isDaemon() ? daemonAgentThreadFactory : normalAgentThreadFactory);
			if (!shutDown) {
				return ae.start().get();
			}
			return AGENT_CRASH;
		} catch (InterruptedException | ExecutionException e) {
			if (!shutDown) {
				// Kernel cannot be interrupted !!
				bugReport(e);
				return SEVERE;
			}
		} catch (CancellationException e) {
			// This is the case when the agent is killed during activation
			return AGENT_CRASH;
		}
		return TIMEOUT;
	}
	public static long getTimeOutNanoFromSeconds(int timeOutSeconds)
	{
		return timeOutSeconds==Integer.MAX_VALUE?Long.MAX_VALUE:TimeUnit.SECONDS.toNanos(timeOutSeconds);
	}

	ReturnCode killAgent(final AbstractAgent requester, final AbstractAgent target, final int timeOutSeconds,
			final KillingType killing_type) {

		long timeOutNano=getTimeOutNanoFromSeconds(timeOutSeconds);

		if (target.getState().compareTo(ACTIVATING) < 0) {
			return NOT_YET_LAUNCHED;
		} else if (target.isKillingInProgress()) {
			if (target instanceof Agent)
				((Agent) target).getAgentExecutor().cancelEnd(true);
			if (timeOutSeconds < Integer.MAX_VALUE) {

				final long expirationTime = System.currentTimeMillis() + (((long) timeOutSeconds) * 1000L);
				long delay = expirationTime - System.currentTimeMillis();
				try {
					wait(this, new LockerCondition(target.state) {

						@Override
						public boolean isLocked() {

							return target.isKillingInProgress();
						}
					}, delay);
				} catch (InterruptedException | TimeoutException ignored) {

				}


				if (target.isKillingInProgress())
					zombieDetected(target.getState(), target);
				else
					return ReturnCode.ALREADY_KILLED;
				return ReturnCode.KILLING_ALREADY_IN_PROGRESS;
			}
		} else if (target.getState().equals(State.TERMINATED) || !target.isAlive()) {
			return ReturnCode.ALREADY_KILLED;
		}

		final Future<ReturnCode> killAttempt = serviceExecutor.submit(() -> killingAgent(requester, target, killing_type));
		try {
			if (timeOutSeconds<=0)
				return killAttempt.get(1, TimeUnit.MILLISECONDS);
			else {

				return killAttempt.get(timeOutNano, TimeUnit.NANOSECONDS);
			}
		} catch (InterruptedException e) {// requester has been killed or
											// something
			// requester.handleInterruptedException();

			return TIMEOUT;
		} catch (TimeoutException e) {
			zombieDetected(State.ENDING, target);
			return TIMEOUT;
		} catch (ExecutionException e) {// BUG kill failed
			bugReport("Kill failed: " + target, e);
			return SEVERE;
			// } catch (TimeoutException e) {// kill task time out
			// return TIMEOUT;
		}
	}

	protected final ReturnCode killingAgent(final AbstractAgent requester, final AbstractAgent target,
			KillingType killing_type) {
		target.waitUntilReadyForKill();
		synchronized (target.state) {
			if (killing_type.equals(KillingType.WAIT_AGENT_PURGE_ITS_MESSAGES_BOX_BEFORE_KILLING_IT) ) {
				if (target instanceof Agent || target instanceof AgentFakeThread) {
					if (target.state.get().include(State.WAIT_FOR_KILL))
						return ReturnCode.KILLING_ALREADY_IN_PROGRESS;
					target.setLivingButWaitingForMessages();
				}
			}
			// this has to be done by a system thread : the job must be done
			if (!target.getAlive().compareAndSet(true, false)) {
				if (target.isKillingInProgress() || target.getState().equals(LIVING)) {
					zombieDetected(target.getState(), target);
					return ReturnCode.KILLING_ALREADY_IN_PROGRESS;
				} else
					return ALREADY_KILLED;
			}
			if (target.isLivingButWaitingForMessages()) {

				try {
					//if (target instanceof AgentFakeThread || target instanceof Agent) {
						wait(this, new LockerCondition(target.state) {

							@Override
							public boolean isLocked() {
								return !target.messageBox.isEmpty();
							}
						});
					//}
				} catch (InterruptedException e) {
					bugReport(e);
				}
			}
		}

		releaseHookEvents(target);

		ReturnCode rc;
		if (target instanceof Agent) {
			// extends Agent and not launched in bucket mode
			if (((Agent) target).myThread != null) {
				rc = killThreadedAgent((Agent) target);// TODO check
			}
			else
				return KILLING_ALREADY_IN_PROGRESS;
		} else {
			try {
				requester.wait(new LockerCondition(target.state) {

					@Override
					public boolean isLocked() {
						return target.state.get().compareTo(State.ACTIVATED) < 0 || target.state.get() == State.ZOMBIE;
					}
				});
			} catch (Exception ignored) {
			}

			// stopAbstractAgentProcess(ACTIVATED, target);
			rc = startEndBehavior(target, true);

		}
		return rc;

	}



	protected void zombieDetected(State s, AbstractAgent target) {

		boolean zombie = false;
		synchronized (target.state) {
			State previous = target.state.getAndSet(State.ZOMBIE);
			if (previous != State.TERMINATED) {
				zombie = true;
				target.state.set(State.ZOMBIE);
				//target.state.get().setPreviousState(previous);
				target.state.notifyAll();
			} else {
				target.state.set(State.TERMINATED);
			}
			target.state.notifyAll();
		}
		if (zombie) {
			if (isWarningOn()) {
				logger.log(Level.WARNING, "During the " + s + " state, the agent " + target + " becomes a zombie.");
			}
			informHooks(AgentActionEvent.AGENT_ZOMBIE, target);
		}
	}


	final ReturnCode startEndBehavior(final AbstractAgent target, boolean callEndingFunction) {
        //final PoolExecutor executor = asDaemon ? serviceExecutor : lifeExecutor;
		if (callEndingFunction) {
			final Future<Boolean> endAttempt = serviceExecutor.submit(target::ending);
			try {
				endAttempt.get();
			} catch (InterruptedException e) {
				System.err.println("----------------------\n\n---------------------------------------------");
			} catch (ExecutionException e) {
				bugReport("Killing task failed on " + target, e);
			}
		}
		if (!(target instanceof Agent)) {
			target.terminate();
		}
		return SUCCESS;
	}

	@Override
	protected void end() {




	}

	private ReturnCode killThreadedAgent(final Agent target) {
		synchronized (target.state) {

			if (target.state.get().equals(State.ENDING) || target.state.get().equals(State.ZOMBIE)
					|| target.state.get().equals(State.WAIT_FOR_KILL))
				target.getAgentExecutor().cancelEnd(true);
			if (!target.state.get().equals(State.ZOMBIE))
				target.state.set(State.ENDING);
			target.getAgentExecutor().cancelLive(true);
			target.getAgentExecutor().cancelActivate(true);
			try {
				LockerCondition locker = new LockerCondition() {

					@Override
					public boolean isLocked() {
						return !target.state.get().equals(State.TERMINATED);
					}
				};

				locker.setLocker(target.state);
				this.wait(this, locker);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
		}
		return ReturnCode.SUCCESS;


	}

	// /////////////////////////////////////////////////////////////////////////
	// //////////////////////// Organization access
	// /////////////////////////////////////////////////////////////////////////
	private Organization getCommunity(final String community) throws CGRNotAvailable {
		Organization org = organizations.get(community);
		if (org == null)
			throw new CGRNotAvailable(NOT_COMMUNITY);
		return org;
	}

	final InternalGroup getGroup(Group group) throws CGRNotAvailable {
		InternalGroup g = getCommunity(group.getCommunity()).get(group);
		if (g == null)
			throw new CGRNotAvailable(NOT_GROUP);
		return g;
	}

	final InternalRole getRole(Group group, final String role) throws CGRNotAvailable {
		if (role == null)
			throw new NullPointerException("role");
		InternalRole r = getGroup(group).get(role);
		if (r == null)
			throw new CGRNotAvailable(NOT_ROLE);
		return r;
	}


	final List<AgentAddress> getOtherRolePlayers(AbstractAgent abstractAgent, Group group, String role)
			throws CGRNotAvailable {
		// never null without throwing Ex
		final List<AgentAddress> result = getRole(group, role).getAgentAddressesCopy();

		InternalRole.removeAgentAddressOf(abstractAgent, result);
		if (!result.isEmpty()) {
			return result;
		}
		return null;
	}

	final AgentAddress getAnotherRolePlayer(AbstractAgent abstractAgent, Group group, String role)
			throws CGRNotAvailable {
		List<AgentAddress> others = getOtherRolePlayers(abstractAgent, group, role);
		if (others != null) {
			return others.get((int) (Math.random() * others.size()));
		}
		return null;
	}

	// /////////////////////////////////////////////////////////////////////////
	// //////////////////////// Messaging
	// /////////////////////////////////////////////////////////////////////////

	private ReturnCode buildAndSendMessage(final AgentAddress sender, final AgentAddress receiver, final Message m) {
		m.setSender(sender);
		m.setReceiver(receiver);
		m.getConversationID().setOrigin(kernelAddress);
		final ReturnCode r = sendMessage(m, receiver.getAgent());
		if (r == SUCCESS || r == ReturnCode.TRANSFER_IN_PROGRESS) {
			informHooks(AgentActionEvent.SEND_MESSAGE, m);
		}
		return r;
	}

	final AgentAddress getSenderAgentAddress(final AbstractAgent sender, final AgentAddress receiver, String senderRole)
			throws CGRNotAvailable {
		AgentAddress senderAA;
		final InternalRole targetedRole = receiver.getRoleObject();
		if (targetedRole == null)
			throw new CGRNotAvailable(ReturnCode.NO_RECIPIENT_FOUND);

		// no role given
		if (senderRole == null) {
			// looking for any role in this group, starting with the receiver role
			senderAA = targetedRole.getAgentAddressInGroupOrParentGroups(sender);

			// if still null : this SHOULD be a candidate's request to the manager or it is
			// an error
			if (senderAA == null) {
				if (targetedRole.getRoleName().equals(com.distrimind.madkit.agr.Organization.GROUP_MANAGER_ROLE)
						|| targetedRole.getRoleName().equals(com.distrimind.madkit.agr.LocalCommunity.Roles.TASK_MANAGER_ROLE)
						|| (targetedRole.getRoleName().equals(Roles.GUI) && targetedRole.getGroup().equals(Groups.GUI)))
					return new CandidateAgentAddress(sender, targetedRole, kernelAddress, isAutoCreateGroup(sender));
				throw new CGRNotAvailable(NOT_IN_GROUP);
			}
			return senderAA;
		}

		// message sent with a particular role : check that
		// look into the senderRole role if the agent is in
		senderAA = targetedRole.getMyGroup().getAgentAddressInGroupOrInParentGroupsWithRole(sender, senderRole);

		if (senderAA == null) {// if still null : this SHOULD be a
			// candidate's request to the manager or it
			// is an error
			if ((senderRole.equals(com.distrimind.madkit.agr.Organization.GROUP_CANDIDATE_ROLE)
					&& targetedRole.getRoleName().equals(com.distrimind.madkit.agr.Organization.GROUP_MANAGER_ROLE))
					|| (senderRole.equals(com.distrimind.madkit.agr.LocalCommunity.Roles.TASK_MANAGER_ROLE)
							&& targetedRole.getRoleName()
									.equals(com.distrimind.madkit.agr.LocalCommunity.Roles.TASK_CALLER_ROLE))
					|| (senderRole.equals(com.distrimind.madkit.agr.LocalCommunity.Roles.TASK_CALLER_ROLE)
							&& targetedRole.getRoleName()
									.equals(com.distrimind.madkit.agr.LocalCommunity.Roles.TASK_MANAGER_ROLE)))
				return new CandidateAgentAddress(sender, targetedRole, kernelAddress, isAutoCreateGroup(sender));
			if (targetedRole.getAgentAddressInGroupOrParentGroups(sender) == null)
				throw new CGRNotAvailable(NOT_IN_GROUP);
			throw new CGRNotAvailable(ROLE_NOT_HANDLED);
		}
		return senderAA;
	}

	// /////////////////////////////////////////////////////////////////////////
	// //////////////////////// Simulation
	// /////////////////////////////////////////////////////////////////////////

	boolean addOverlooker(final AbstractAgent requester, Overlooker<? extends AbstractAgent> o) {
		boolean add;
		synchronized (this) {
			add = operatingOverlookers.add(o);
		}
		if (add)
			o.addToKernel(this);
		return add;
	}



	boolean removeOverlooker(final AbstractAgent requester, Overlooker<? extends AbstractAgent> o) {
		try {
			boolean removed;
			synchronized (this) {
				removed = operatingOverlookers.remove(o);
			}
			if (removed)
				o.removeFromKernel();
			return removed;
		} catch (IllegalAccessException e) {
			this.bugReport(e);
			return false;
		}
	}

	// /////////////////////////////////////////////////////////////////////////
	// //////////////////////// Internal functioning
	// /////////////////////////////////////////////////////////////////////////

	void removeCommunity(String community) {
		organizations.remove(community);
	}

	@Override
	public KernelAddress getKernelAddress() {
		return kernelAddress;
	}

	@Override
	public String getServerInfo() {
		if (netAgent != null) {
			return netAgent.getAgent().getServerInfo();
		}
		return "";
	}

	Set<Overlooker<? extends AbstractAgent>> getOperatingOverlookers() {
		return operatingOverlookers;
	}


	void removeAgentFromOrganizations(AbstractAgent theAgent) {
		removeAllAutoRequestedGroups(theAgent);
		synchronized (organizations) {
			organizations.values().removeIf(org -> {
				for (final Group group : org.removeAgentFromAllGroups(theAgent, true)) {
					sendNetworkCGRSynchroMessageWithRole(null, new CGRSynchro(LEAVE_GROUP, new AgentAddress(theAgent,
							new InternalRole(group), kernelAddress, isAutoCreateGroup(theAgent)), true));
				}
				return org.isEmpty();
			});
		}
	}

	void removeDistantKernelAddressFromCGR(KernelAddress ka)
	{
		synchronized (organizations) {
			organizations.values().removeIf(org -> {
				org.removeDistantKernelAddressForAllGroups(ka);
				return org.isEmpty();
			});
		}

	}

	@Override
	public MadkitProperties getMadkitConfig() {
		return platform.getConfigOption();
	}

	MadkitKernel getMadkitKernel() {
		return this;
	}

	@Override
	public TreeSet<String> getExistingCommunities() {
		return new TreeSet<>(organizations.keySet());
	}

	@Override
	public Enumeration<Group> getExistingGroups(String community) {
		try {
			return getCommunity(community).keys();
		} catch (CGRNotAvailable e) {
			return null;
		}
	}

	@Override
	public TreeSet<String> getExistingRoles(Group group) {
		if (group.isUsedSubGroups())
			return null;
		try {
			return new TreeSet<>(getGroup(group).keySet());
		} catch (CGRNotAvailable e) {
			return null;
		}
	}

	boolean isCommunity(AbstractAgent requester, String community) {
		try {
			getCommunity(community);
			return true;
		} catch (CGRNotAvailable e) {
			return false;
		}
	}

	boolean isGroup(AbstractAgent requester, Group group) {
		try {
			getGroup(group);
			return true;
		} catch (CGRNotAvailable e) {
			return false;
		}
	}

	boolean isRole(final AbstractAgent requester, Group group, String role) {
		try {
			getRole(group, role);
			return true;
		} catch (CGRNotAvailable e) {
			return false;
		}
	}

	final void importDistantOrg(CGRSynchros synchros) {
		final Map<String, Map<Group, Map<String, Collection<AgentAddress>>>> distantOrg = synchros.getOrganizationSnapShot();

		synchronized (organizations) {
			for (final String communityName : distantOrg.keySet()) {
				Map<Group, Map<String, Collection<AgentAddress>>> value=distantOrg.get(communityName);
				boolean empty=true;
				for (Map<String, Collection<AgentAddress>> m : value.values())
				{
					for (Collection<AgentAddress> c : m.values()) {
						if (c.size() > 0 && c.iterator().next() != null) {
							empty=false;
							break;
						}
					}
					if (!empty)
						break;
				}
				if (empty)
					continue;

				Organization org = new Organization(communityName, this);
				Organization previous = organizations.putIfAbsent(communityName, org);
				if (previous != null) {
					org = previous;
				}
				org.importDistantOrg(value, this);
			}
		}
		final ArrayList<Group> removedGroups = synchros.getRemovedGroups();
		final KernelAddress distantKernelAddress = synchros.getDistantKernelAddress();
		if (logger != null && logger.isLoggable(Level.FINER))
			logger.finer("Importing removed distant groups..." + removedGroups);
		for (Group g : removedGroups) {
			Organization org = organizations.get(g.getCommunity());
			org.removeDistantGroup(distantKernelAddress, g, this);
		}

	}

	@Override
	final public Map<String, Map<Group, Map<String, Collection<AgentAddress>>>> getOrganizationSnapShot(boolean global) {
		Map<String, Map<Group, Map<String, Collection<AgentAddress>>>> export = new TreeMap<>();
		synchronized (organizations) {
			for (Map.Entry<String, Organization> org : organizations.entrySet()) {
				Map<Group, Map<String, Collection<AgentAddress>>> currentOrg = org.getValue().getOrgMap(global);
				if (!currentOrg.isEmpty())
					export.put(org.getKey(), currentOrg);
			}
		}
		return export;
	}

	@Override
	final public Map<String, Map<Group, Map<String, Collection<AgentAddress>>>> getOrganizationSnapShot(
			List<Group> concerned_groups, ListGroupsRoles distantAcceptedGroups) {

		if (concerned_groups == null)
			throw new NullPointerException("concerned_group");
		Map<String, Map<Group, Map<String, Collection<AgentAddress>>>> export = new TreeMap<>();
		synchronized (organizations) {
			for (Map.Entry<String, Organization> org : organizations.entrySet()) {
				Map<Group, Map<String, Collection<AgentAddress>>> m = org.getValue().getOrgMap(concerned_groups, distantAcceptedGroups);
				if (!m.isEmpty()) {
					String com = org.getKey();
					Map<Group, Map<String, Collection<AgentAddress>>> cur = export.get(com);
					if (cur == null) {
						export.put(com, m);
					} else
						cur.putAll(m);
				}
			}
			return export;
		}
	}

	// void logCurrentOrganization(Logger requester, Level lvl){
	// if(requester != null){
	// String message = "Current organization is\n";
	// if(organizations.isEmpty()){
	// message+="\n ------------ EMPTY !! ------------\n";
	// }
	// for(final Map.Entry<String, Organization> org :
	// organizations.entrySet()){
	// message+="\n\n--"+org.getKey()+"----------------------";
	// for(final Map.Entry<String, Group> group : org.getValue().entrySet()){
	// // final AgentAddress manager = group.getValue().getManager().get();
	// message+="\n|--"+group.getKey()+"--";// managed by
	// ["+manager.getAgent()+"] "+manager+" --\n";
	// for(final Map.Entry<String, Role> role: group.getValue().entrySet()){
	// message+="\n||--"+role.getKey()+"--";
	// message+="\n|||--players- "+role.getValue().getPlayers();
	// message+="\n|||--addresses- = "+role.getValue().getAgentAddresses();
	// }
	// }
	// message+="\n-----------------------------";
	// }
	// requester.log(lvl, message+"\n");
	// }
	// }

	final void injectMessage(final DirectLocalLanMessage m) {
		m.setReadyForInjection();
        final AgentAddress receiver = m.getOriginalMessage().getReceiver();
		final AgentAddress sender = m.getOriginalMessage().getSender();

		try {
			final InternalRole receiverRole = kernel.getRole(receiver.getGroup(), receiver.getRole());
			receiver.setRoleObject(receiverRole);
			if (sender != null && receiverRole.getMyGroup().isDistributed()) {
				final InternalRole senderRole = kernel.getRole(sender.getGroup(), sender.getRole());
				if (senderRole.getMyGroup().isDistributed()) {
				//if (injectRoleFromDistantPeer(sender.getGroup(), sender)) {
					final AbstractAgent target = receiverRole.getAbstractAgentWithAddress(receiver);
					if (target != null) {
						// updating sender address
						receiver.setAgent(target);
						((Message) m).setReceiver(receiver);
						((Message) m).setSender(sender);

						target.receiveMessage(m);
						informHooks(AgentActionEvent.SEND_MESSAGE, m.getOriginalMessage());
					} else {
						((Message) m).markMessageAsRead();
						if (logger != null && logger.isLoggable(Level.FINER))
							logger.finer(
									m + " received but the agent address is no longer valid !! Current distributed org is "
											+ getOrganizationSnapShot(false));
					}
				}
				else
					((Message) m).markMessageAsRead();
			} else
				((Message) m).markMessageAsRead();
		} catch (CGRNotAvailable e) {
			kernel.bugReport("Cannot inject " + m + ", receiver=" + receiver + ", sender=" + sender + "\n"
					+ getOrganizationSnapShot(false), e);
			((Message) m).markMessageAsRead();
		}
	}

	final void injectMessage(final BroadcastLocalLanMessage m) {
		m.setReadyForInjection();
        AgentAddress oneReceiver = null;
		AgentAddress oneSender = null;
		int receiversSize = 0;
		try {
			for (Group g : getRepresentedGroups(m.getAbstractGroup(), m.getRole())) {
				if (g.isDistributed()) {
					try {
						List<AgentAddress> receivers = getOtherRolePlayers(null, g, m.getRole());
						if (receivers != null && receivers.size() > 0) {
							oneReceiver = receivers.iterator().next();
							final AgentAddress senderAgentAddress = m
									.getAgentAddressSenderFromReceiver(oneReceiver);
							if (senderAgentAddress != null) {
								receiversSize += receivers.size();
								oneSender = senderAgentAddress;
								((Message) m).setSender(senderAgentAddress);
								broadcasting(receivers, m, null, null,false);
							}
						}
					} catch (CGRNotAvailable e) {
						kernel.bugReport("Cannot inject " + m + "\n" + getOrganizationSnapShot(false), e);
					}
				}
			}
		} catch (CGRNotAvailable e) {
			kernel.bugReport("Cannot inject " + m + "\n" + getOrganizationSnapShot(false), e);
		}

		if (receiversSize > 0) {
			m.getOriginalMessage().setReceiver(oneReceiver);
			m.getOriginalMessage().setSender(oneSender);
			informHooks(AgentActionEvent.BROADCAST_MESSAGE, m.getOriginalMessage());
		}
		((Message) m).markMessageAsRead();
	}

	@SuppressWarnings("BooleanMethodIsAlwaysInverted")
	final boolean injectGroupFromDistantPeer(Group group, final AgentAddress agentAddress)
	{
		if (group.isDistributed()) {
			Organization organization;
			try {
				organization = getCommunity(group.getCommunity());
			} catch (CGRNotAvailable e) {
				organization = new Organization(group.getCommunity(), this);
				organizations.put(group.getCommunity(), organization);
			}
			if (organization.putIfAbsent(group,
					new InternalGroup(group, agentAddress, organization, false)) == null) {
				informHooks(AgentActionEvent.CREATE_GROUP, agentAddress);
			}
			return true;
		}
		else
			return false;

	}
	final boolean injectRoleFromDistantPeer(Group group, final AgentAddress agentAddress)
	{
		if (!group.isDistributed())
			return false;
		InternalGroup g=null;
		try {
			g=getGroup(group);
		} catch (CGRNotAvailable e) {
			if (!injectGroupFromDistantPeer(group, agentAddress))
				return false;

		}
		try {
			if (g==null)
				g=getGroup(group);
			if (g.isDistributed())
			{
				if (g.addDistantMember(agentAddress)) {
					informHooks(AgentActionEvent.REQUEST_ROLE, agentAddress);
				}
				return true;
			}
			else
				return false;

		} catch (CGRNotAvailable e) {
			return false;
		}
	}

	final void injectOperation(CGRSynchro m) {
		final AgentAddress agentAddress = m.getContent();
		final Group group = agentAddress.getGroup();
		final String roleName = agentAddress.getRole();
		synchronized (organizations) {
			switch (m.getCode()) {
			case CREATE_GROUP:
				if (!injectGroupFromDistantPeer(group, agentAddress))
					logInjectOperationFailure(m, agentAddress, null);
				break;
			case REQUEST_ROLE:
				if (!injectRoleFromDistantPeer(group, agentAddress))
					logInjectOperationFailure(m, agentAddress, null);
				break;
			case LEAVE_ROLE:
				try {
					if (getRole(agentAddress.getGroup(), roleName).removeDistantMember(agentAddress, m.isManualOperation()))
						informHooks(AgentActionEvent.LEAVE_ROLE, agentAddress);
				} catch (CGRNotAvailable e) {
					logInjectOperationFailure(m, agentAddress, e);
				}
				break;
			case LEAVE_GROUP:
				try {
					InternalGroup g=getGroup(group);
					if (g.isDistributed()) {
						if (g.removeDistantMember(agentAddress, m.isManualOperation()))
							informHooks(AgentActionEvent.LEAVE_GROUP, agentAddress);
					}
					else
						logInjectOperationFailure(m, agentAddress, null);
				} catch (CGRNotAvailable ignored) {

				}
				break;
			// case CGRSynchro.LEAVE_ORG://TODO to implement
			// break;
			default:
				bugReport(new UnsupportedOperationException("case not treated in injectOperation"));
				break;
			}
		}
	}


	private void logInjectOperationFailure(CGRSynchro m, final AgentAddress agentAddress, CGRNotAvailable e) {
		getLogger().log(Level.FINE, "distant CGR " + m.getCode() + " update failed on " + agentAddress, e);
	}

	private void leaveAllGroupsOfAllAgents()
	{
		HashMap<AbstractAgent, AutoRequestedGroups> arg;
		synchronized (auto_requested_groups) {
			arg = new HashMap<>(this.auto_requested_groups);
		}
		for (Map.Entry<AbstractAgent, AutoRequestedGroups> e : arg.entrySet()) {
			AbstractAgent aa = e.getKey();
			if (!(aa instanceof MadkitKernel) && aa.isAlive() && aa.getState().compareTo(State.WAIT_FOR_KILL) < 0)
				killAgent(aa);
		}

		ArrayList<Organization> organizations;
		synchronized (this.organizations)
		{
			for (KernelAddress ka : new ArrayList<>(distantKernelAddresses))
				removeDistantKernelAddressFromCGR(ka);
			distantKernelAddresses.clear();
			organizations=new ArrayList<>(this.organizations.values());
		}


		for (Organization o : organizations)
		{
			ArrayList<InternalGroup> groups;
			synchronized (this.organizations)
			{
				groups=new ArrayList<>(o.values());
			}
			for (InternalGroup g : groups)
			{
				ArrayList<InternalRole> roles;
				synchronized (this.organizations)
				{
					roles=new ArrayList<>(g.values());
				}
				for (InternalRole r : roles)
				{
					for (AbstractAgent aa : r.getAgentsList())
					{
						if (!(aa instanceof MadkitKernel) && aa.isAlive() && aa.getState().compareTo(State.WAIT_FOR_KILL)<0)
							killAgent(aa);
					}
				}
			}
		}
	}

	@Override
	void terminate() {
		// AgentLogger.closeLoggersFrom(kernelAddress);
		super.terminate(false);
		AgentLogger.removeLoggers(this);
		AgentStatusPanel.remove(kernelAddress);
		AgentLogLevelMenu.remove(kernelAddress);
		if (getMadkitConfig().savePropertiesAfterKernelKill)
		{
			try {
				getMadkitConfig().saveConfiguration();
			} catch (IOException | PropertiesParseException e) {
				if (logger!=null)
					logger.severeLog("Unable to save config file", e);
			}

		}
		if (getMadkitConfig().killAllNonThreadedAgentsDuringMaDKitClosing)
			leaveAllGroupsOfAllAgents();
		if (getMadkitConfig().isUseMadkitSchedulerWithFortunaSecureRandom())
			Fortuna.setPersonalDefaultScheduledExecutorService(null);
		if (this.serviceExecutor!=null) {
			this.serviceExecutor.shutdownNow();
			boolean valid=true;
			try {
				if (!this.serviceExecutor.awaitTermination(10, TimeUnit.SECONDS)) {
					getLogger().warning("Life executor not terminated !");
					valid = false;
				}
			} catch (InterruptedException e) {
				e.printStackTrace();
				valid=false;
			}

			if (valid && logger!=null)
				logger.finer("***** Service executors terminated ********\n");
			this.serviceExecutor = null;
		}
		try {
			getMadkitConfig().resetCacheFileCenter();
			getMadkitConfig().setDatabaseFactory(null);
		} catch (Throwable e) {
			e.printStackTrace();
		}

		Group.madkitKernelKilled(kernelAddress);
		if (getMadkitConfig().madkitLogLevel != Level.OFF) {
			System.out.println("\n-----------------------------------------------------------------------------"
					+ "\n\t Kernel " + getNetworkID() + "\n\t is shutting down, Bye !"
					+ "\n-----------------------------------------------------------------------------\n");
		}
		kernel = this;
		loggedKernel = null;
		platform = null;
		asynchronousMessageTable =null;
		asynchronousBigDataTable =null;
		operatingOverlookers.clear();
		synchronized (state) {
			state.set(TERMINATED);
			state.notify();
		}
	}

	protected void exit() throws InterruptedException {
		if (shutDown)
			return;
		shutDown = true;
		sendNetworkKernelMessageWithRole(new KernelMessage(KernelAction.EXIT));
		broadcastMessageWithRole(MadkitKernel.this, Groups.GUI,
				Roles.GUI, new KernelMessage(KernelAction.EXIT), null,
				false);
		while (getAgentWithRole(Groups.GUI, Roles.GUI) != null) {
			pause(10);
		}
		// pause(10);//be sure that last executors have started
		if (logger != null)
			logger.finer("***** SHUTTING DOWN MADKIT ********\n");
		killAgents(true);
		killAgent(this);

	}

	private void launchNetwork() {
		if (getMadkitConfig().networkProperties.network) {
			updateNetworkAgent();
			if (netAgent == null) {
				final NetworkAgent na = new NetworkAgent();
				final ReturnCode r = launchAgent(na);
				if (r == SUCCESS) {
					if (logger != null)
						logger.fine("\n\t****** Network agent launched ******\n");
				} // TODO i18n
				else {
					if (logger != null)
						logger.severe("\n\t****** Problem launching network agent ******\n");
				}
			} else {
				if (sendNetworkKernelMessageWithRole(new KernelMessage(KernelAction.LAUNCH_NETWORK)) == SUCCESS) {
					if (logger != null)
						logger.fine("\n\t****** Network agent up ******\n");
				} else {
					if (logger != null)
						logger.fine("\n\t****** Problem relaunching network ******\n");
				}

			}
		} else if (logger != null)
			logger.severe("Network is disabled into the madkit properties. Impossible to launch it.");
	}


	private void killAgents(boolean untilEmpty) throws InterruptedException {
		if (getMadkitConfig().killAllNonThreadedAgentsDuringMaDKitClosing)
			leaveAllGroupsOfAllAgents();

		ArrayList<Agent> l;
		synchronized (threadedAgents) {
			threadedAgents.remove(this);
			l = new ArrayList<>(threadedAgents);
		}
		do {
			for (final Agent a : l) {
				killAgent(this, a, 10, KillingType.JUST_KILL_IT);
			}
			pause(10);
			synchronized (threadedAgents) {
				l = new ArrayList<>(threadedAgents);
			}
		} while (untilEmpty && !l.isEmpty());
	}

	/*
	 * boolean createGroupIfAbsent(AbstractAgent abstractAgent, String community,
	 * String group, Gatekeeper gatekeeper, boolean isDistributed) { return
	 * createGroup(abstractAgent, community, group, gatekeeper, isDistributed) ==
	 * SUCCESS; }
	 */

	void bugReport(Throwable e) {
		bugReport("", e);
	}

	void bugReport(String m, Throwable e) {
		getMadkitKernel().getLogger().severeLog("********************** KERNEL PROBLEM, please bug report " + m, e); // Kernel
	}

	@SuppressWarnings("unused")
    ReturnCode destroyCommunity(AbstractAgent abstractAgent, String community) {
		synchronized (organizations) {
			try {
				getCommunity(community).destroy();
				return SUCCESS;
			} catch (CGRNotAvailable e) {
				return e.getCode();
			}
		}
	}

	@SuppressWarnings("unused")
    ReturnCode destroyGroup(AbstractAgent abstractAgent, Group group) {
		synchronized (organizations) {
			try {
				if (group.isUsedSubGroups())
					return ReturnCode.MULTI_GROUP_NOT_ACCEPTED;
				getGroup(group).destroy();
				return SUCCESS;
			} catch (CGRNotAvailable e) {
				return e.getCode();
			}
		}
	}

	@SuppressWarnings("unused")
    ReturnCode destroyRole(AbstractAgent abstractAgent, Group group, String role) {
		synchronized (organizations) {
			try {
				if (group.isUsedSubGroups())
					return ReturnCode.MULTI_GROUP_NOT_ACCEPTED;
				getRole(group, role).destroy();
				return SUCCESS;
			} catch (CGRNotAvailable e) {
				return e.getCode();
			}
		}
	}

	void removeThreadedAgent(Agent myAgent) {
		synchronized (threadedAgents) {
			if (!threadedAgents.remove(myAgent) && logger!=null)
				logger.warning("Threaded agent not found during removing");

			if (logger != null && logger.isLoggable(Level.FINEST))
				logger.finest(threadedAgents.toString());
		}
	}

	AgentAddress getAgentAddressIn(AbstractAgent agent, Group _group, String role) {
		try {
			return getRole(_group, role).getAgentAddressOf(agent);
		} catch (CGRNotAvailable e) {
			if (agent.isWarningOn()) {
				agent.setAgentStackTrace(e);
				agent.handleException(Influence.GET_AGENT_ADDRESS_IN,
						new OrganizationWarning(e.getCode(), _group, role));
			}
			return null;
		}
	}

	final boolean isHooked() {
		return !hooks.isEmpty();
	}

	final TreeSet<Group> getGroupsOf(AbstractAgent abstractAgent, String community) {
		final TreeSet<Group> groups = new TreeSet<>();
		try {
			for (final InternalGroup g : getCommunity(community).values()) {
				if (g.isIn(abstractAgent)) {
					groups.add(g.getGroup());
				}
			}
			return groups;
		} catch (CGRNotAvailable e) {
			return null;
		}
	}

	final TreeSet<String> getRolesOf(AbstractAgent abstractAgent, Group group) {
		if (group.isUsedSubGroups())
			return null;
		final TreeSet<String> roles = new TreeSet<>();
		try {
			for (final InternalRole r : getGroup(group).values()) {
				if (r.contains(abstractAgent)) {
					roles.add(r.getRoleName());
				}
			}
		} catch (CGRNotAvailable e) {
			return null;
		}
		return roles;
	}


	private ScheduledPoolExecutor getDefaultScheduledExecutorService() {
		return serviceExecutor;
	}


	void wait(AbstractAgent requester, LockerCondition locker) throws InterruptedException {
		if (!serviceExecutor.wait(locker) ) {
			regularWait(requester, locker);
		}
	}
	void wait(AbstractAgent requester, LockerCondition locker, long delayMillis) throws InterruptedException, TimeoutException {
		if (!serviceExecutor.wait(locker, delayMillis, TimeUnit.MILLISECONDS)) {
			regularWait(requester, locker, delayMillis);
		}
	}
	void wait(AbstractAgent requester, LockerCondition locker, Lock personalLocker, Condition personalCondition, long delay, TimeUnit unit) throws InterruptedException, TimeoutException {
		if (!serviceExecutor.wait(locker, personalLocker, personalCondition, delay, unit) ) {
			regularWait(requester, locker, personalLocker, personalCondition,  delay, unit);
		}
	}
	void wait(AbstractAgent requester, LockerCondition locker, Lock personalLocker, Condition personalCondition) throws InterruptedException{
		if (!serviceExecutor.wait(locker, personalLocker, personalCondition) ) {
			regularWait(requester, locker, personalLocker, personalCondition);
		}
	}
	void regularWait(@SuppressWarnings("unused") AbstractAgent requester, LockerCondition locker, Lock personalLocker, Condition personalCondition, long delay, TimeUnit unit) throws InterruptedException, TimeoutException {

		long start=System.nanoTime();
		delay=unit.toNanos(delay);

		personalLocker.lock();
		try {
			while (locker.isLocked() && !locker.isCanceled()) {
				locker.beforeCycleLocking();
				if (!personalCondition.await(delay, TimeUnit.NANOSECONDS))
					delay=-1;
				locker.afterCycleLocking();
				long end=System.nanoTime();
				delay-=end-start;
				if (delay<0)
					throw new TimeoutException();
				start=end;
			}
		}
		finally {
			personalLocker.unlock();
		}

	}
	void regularWait(@SuppressWarnings("unused") AbstractAgent requester, LockerCondition locker, Lock personalLocker, Condition personalCondition) throws InterruptedException {
		personalLocker.lock();
		try {
			while (locker.isLocked() && !locker.isCanceled()) {
				locker.beforeCycleLocking();
				personalCondition.await();
				locker.afterCycleLocking();
			}
		}
		finally {
			personalLocker.unlock();
		}

	}
	void regularWait(@SuppressWarnings("unused") AbstractAgent requester, LockerCondition locker, long delayMillis) throws InterruptedException, TimeoutException {
		long start=System.currentTimeMillis();

		synchronized (locker.getLocker()) {
			while (locker.isLocked() && !locker.isCanceled()) {
				locker.beforeCycleLocking();
				locker.getLocker().wait(delayMillis);
				locker.afterCycleLocking();
				long end=System.currentTimeMillis();
				delayMillis-=end-start;
				if (delayMillis<0)
					throw new TimeoutException();
				start=end;
			}
		}

	}
	void regularWait(AbstractAgent requester, LockerCondition locker) throws InterruptedException {
		synchronized (locker.getLocker()) {
			while (locker.isLocked() && !locker.isCanceled()) {
				locker.beforeCycleLocking();
				locker.getLocker().wait();
				locker.afterCycleLocking();
			}
		}

	}


	void sleep(AbstractAgent requester, long millis) throws InterruptedException {
		if (serviceExecutor==null || !serviceExecutor.sleep(millis, TimeUnit.MILLISECONDS) ) {
			Thread.sleep(millis);
		}
	}


	TaskID scheduleTask(AbstractAgent agent, Task<?> _task, boolean ask_for_execution_confirmation) {
		if (_task == null)
			throw new NullPointerException("_task");
		if (agent == null)
			throw new NullPointerException("agent");
		return scheduleTask(agent, getDefaultScheduledExecutorService(), _task, ask_for_execution_confirmation);
	}


	private TaskID scheduleTask(final AbstractAgent agent, ScheduledPoolExecutor executorService,
			final Task<?> _task, final boolean ask_for_execution_confirmation) {
		if (_task == null)
			throw new NullPointerException("_task");
		if (executorService == null)
			return null;
		else {
			final TaskID taskID = new TaskID();
			Runnable runnable = new Runnable() {

				@Override
				public void run() {
					boolean killed=agent.state.get().compareTo(State.WAIT_FOR_KILL) >= 0 || !agent.isAlive();
					try {

						if (!_task.isExecuteEvenIfLauncherAgentIsKilled() && killed) {
							cancelTask(agent, taskID, false);
							return;
						}

						long date_begin = System.currentTimeMillis();
						_task.run();
						if (_task.isRepetitive())
							_task.renewTask();
						if (ask_for_execution_confirmation && !killed) {
							Message m = new TasksExecutionConfirmationMessage(taskID, _task, date_begin,
									System.currentTimeMillis());
							m.setIDFrom(taskID);

							agent.receiveMessage(m);
						}
					} catch (Exception e) {
						if (agent.logger != null && !killed)
							agent.logger.severeLog("Exception in task execution : ", e);
						else
							e.printStackTrace();

					}
				}

				@Override
				public String toString() {
					return _task.toString();
				}
			};

			ScheduledFuture<?> future;
			if (_task.isRepetitive()) {
				future = executorService.scheduleWithFixedDelay(runnable,
								Math.max(0, _task.getTimeOfExecution() - System.currentTimeMillis()),
								_task.getDurationBetweenEachRepetition(), TimeUnit.MILLISECONDS);
			} else {
				future = executorService.schedule(runnable,
								Math.max(0, _task.getTimeOfExecution() - System.currentTimeMillis()),
								TimeUnit.MILLISECONDS);
			}
			taskID.setFuture(future);
			return taskID;
		}
	}

	ConcurrentHashMap<String, Organization> getOrganizations() {
		return organizations;
	}

	boolean cancelTask(AbstractAgent agent, TaskID tasks_id, boolean mayInterruptIfRunning) {
		return tasks_id.cancelTask(mayInterruptIfRunning);
	}

	/*
	 * boolean setTaskManagerExecutorPriority(AbstractAgent requester, String
	 * _task_agent_name, int priority) {
	 * 
	 * ScheduledThreadPoolExecutor executor=getScheduledExecutorService(requester,
	 * _task_agent_name); if (executor==null) return false;
	 * executor.setThreadsPriority(priority); return true; }
	 */

	boolean isConcernedByAutoRequestRole(AbstractAgent agent, Group group, String role) {
		synchronized (auto_requested_groups) {
			AutoRequestedGroups arg = auto_requested_groups.get(agent);
			if (arg == null)
				return false;
			return arg.isConcernedBy(group, role);
		}
	}

	void removeAllAutoRequestedGroups(AbstractAgent requester) {
		AutoRequestedGroups args;
		synchronized (auto_requested_groups) {
			args = auto_requested_groups.remove(requester);
			if (args != null) {
				args.destroy();
			}
		}
		if (args != null)
			Group.removeGroupChangesNotifier(args);
	}

	void leaveAutoRequestedRole(AbstractAgent requester, String role) {
		AutoRequestedGroups notifier = null;
		synchronized (auto_requested_groups) {
			AutoRequestedGroups args = auto_requested_groups.get(requester);
			if (args != null) {
				args.destroy(role);
				if (args.isEmpty())
					notifier = auto_requested_groups.remove(requester);
			}
		}
		if (notifier != null)
			Group.removeGroupChangesNotifier(notifier);
	}

	void leaveAutoRequestedRole(AbstractAgent requester, AbstractGroup group, String role) {
		AutoRequestedGroups notifier = null;
		synchronized (auto_requested_groups) {
			AutoRequestedGroups args = auto_requested_groups.get(requester);
			if (args != null) {
				args.destroy(group, role);
				if (args.isEmpty()) {
					notifier = auto_requested_groups.remove(requester);
				}
			}
		}
		if (notifier != null)
			Group.removeGroupChangesNotifier(notifier);

	}

	void leaveAutoRequestedGroup(AbstractAgent requester, AbstractGroup group) {
		AutoRequestedGroups notifier = null;
		synchronized (auto_requested_groups) {
			AutoRequestedGroups args = auto_requested_groups.get(requester);
			if (args != null) {
				args.destroy(group);
				if (args.isEmpty()) {
					notifier = auto_requested_groups.remove(requester);
				}
			}
		}
		if (notifier != null)
			Group.removeGroupChangesNotifier(notifier);

	}

	void autoRequestRole(AbstractAgent requester, AbstractGroup _groups, String role, SecureExternalizable passKey) {
		AutoRequestedGroups args;
		boolean addNotifier = false;
		synchronized (auto_requested_groups) {
			args = auto_requested_groups.get(requester);
			if (args == null) {
				args = new AutoRequestedGroups(requester);
				auto_requested_groups.put(requester, args);
				addNotifier = true;
			}
			args.addAutoRequestedGroup(new AutoRequestedGroup(requester, _groups, role, passKey));
		}
		if (addNotifier)
			Group.addGroupChangesNotifier(args);
	}

	ReturnCode cancelBigDataTransfer(AbstractAgent requester, BigDataTransferID bigDataTransferID)
	{
		if (netAgent!=null) {
			Message m=new CancelBigDataTransferMessage(bigDataTransferID);
			m.getConversationID().setOrigin(getKernelAddress());
			m.setSender(netUpdater);
			m.setReceiver(netAgent);
			netAgent.getAgent().receiveMessage(m);
			return SUCCESS;
		}
		else
			return IGNORED;
	}

	ReturnCode cancelAsynchronousBigData(AbstractAgent requester, AsynchronousBigDataTransferID asynchronousBigDataTransferID)
	{
		if (asynchronousBigDataTable!=null) {
			try {
				return asynchronousBigDataTable.cancelTransfer(requester, asynchronousBigDataTransferID.getAsynchronousBigDataInternalIdentifier());
			} catch (DatabaseException e) {
				getLogger().severeLog("Unexpected exception", e);
				return IGNORED;
			}
		}
		else
			return IGNORED;
	}

	public AsynchronousBigDataTransferID getAsynchronousBigDataTransferIDInstance(AbstractAgent requester, ExternalAsynchronousBigDataIdentifier externalAsynchronousBigDataIdentifier)
	{
		if (externalAsynchronousBigDataIdentifier==null)
			throw new NullPointerException();
		if (asynchronousMessageTable==null)
			return null;
		try {
			AsynchronousBigDataTable.Record r = asynchronousBigDataTable.getRecordsWithAllFields("asynchronousBigDataIdentifier", externalAsynchronousBigDataIdentifier).stream().findAny().orElse(null);
			if (r==null)
				return null;
			else
				return new AsynchronousBigDataTransferID(r.getAsynchronousBigDataInternalIdentifier(), externalAsynchronousBigDataIdentifier, this);
		} catch (DatabaseException e) {
			getLogger().severeLog("Unexpected exception", e);
			return null;
		}

	}

	BigDataTransferID sendAsynchronousBigData(AbstractAgent requester, AgentAddress senderAA, AgentAddress receiverAA,
											  AsynchronousBigDataTable.Record record)
			throws IOException {
		if (senderAA == null)
			throw new NullPointerException("agentAddress");
		if (receiverAA==null)
			throw new NullPointerException();
		RandomInputStream inputStream=record.getAsynchronousBigDataToSendWrapper().getRandomInputStream(record.getExternalAsynchronousBigDataIdentifier());
		if (inputStream==null) {
			try {
				asynchronousBigDataTable.cancelTransfer(requester, record);
			} catch (DatabaseException e) {
				getLogger().severeLog("Unexpected exception", e);
			}
			return null;
		}
		RealTimeTransferStat stat = new RealTimeTransferStat(
				getMadkitConfig().networkProperties.bigDataStatDurationMean,
				getMadkitConfig().networkProperties.bigDataStatDurationMean / 10);
		BigDataPropositionMessage message = new BigDataPropositionMessage(inputStream, record.getCurrentStreamPosition(),
				inputStream.length()-record.getCurrentStreamPosition(), record.isTransferStarted()?null:record.getAttachedData(),
				receiverAA.isFrom(getKernelAddress()), requester.getMadkitConfig().networkProperties.maxBufferSize,
				stat, record.getMessageDigestType(), record.isExcludedFromEncryption(),
				record.getAsynchronousBigDataInternalIdentifier(), record.getExternalAsynchronousBigDataIdentifier(),
				record.getTimeOutInMs());

		ReturnCode rc = buildAndSendMessage(senderAA, receiverAA, message);
		// get the role for the sender and then send
		if (rc != ReturnCode.SUCCESS && rc != ReturnCode.TRANSFER_IN_PROGRESS)
			return null;
		else
			return message.getConversationID();
	}

	BigDataTransferID sendBigData(AbstractAgent requester, AgentAddress agentAddress, RandomInputStream stream,
			long pos, long length, SecureExternalizable attachedData, String senderRole, MessageDigestType messageDigestType, boolean excludeFromEncryption)
			throws IOException {
		if (agentAddress == null)
			throw new NullPointerException("agentAddress");
		AgentAddress target = resolveAddress(agentAddress);
		if (target == null)
			return null;
		else {
			RealTimeTransferStat stat = new RealTimeTransferStat(
					getMadkitConfig().networkProperties.bigDataStatDurationMean,
					getMadkitConfig().networkProperties.bigDataStatDurationMean / 10);
			BigDataPropositionMessage message = new BigDataPropositionMessage(stream, pos, length, attachedData,
					target.isFrom(getKernelAddress()), requester.getMadkitConfig().networkProperties.maxBufferSize,
					stat, messageDigestType, excludeFromEncryption);

			try {
				ReturnCode rc = buildAndSendMessage(getSenderAgentAddress(requester, target, senderRole), target,
						message);
				// get the role for the sender and then send
				if (rc != ReturnCode.SUCCESS && rc != ReturnCode.TRANSFER_IN_PROGRESS)
					return null;
				else
					return new BigDataTransferID(message.getConversationID(), stat);
			} catch (CGRNotAvailable e) {
				return null;
			}
		}
	}

	void setAsynchronousTransferAsStarted(AbstractAgent requester, AbstractDecentralizedIDGenerator asynchronousBigDataInternalIdentifier)
	{
		if (asynchronousBigDataTable==null)
		{
			logLifeException(new DatabaseLoadingException("The MKLE database was not loaded"));
		}
		else
		{
			asynchronousBigDataTable.setAsynchronousTransferAsStarted(asynchronousBigDataInternalIdentifier);
		}
	}

	void acceptDistantBigDataTransfer(AbstractAgent requester, BigDataPropositionMessage originalMessage) {
		broadcastMessage(LocalCommunity.Groups.NETWORK, LocalCommunity.Roles.DISTANT_KERNEL_AGENT_ROLE,
				new ObjectMessage<>(originalMessage), false);
	}

	void transferLostForBigDataTransfer(AbstractAgent requester, ConversationID conversationID, int idPacket,
										AgentAddress sender, AgentAddress receiver, long readDataLength, long durationInMs, AbstractDecentralizedIDGenerator asynchronousBigDataInternalIdentifier,
										ExternalAsynchronousBigDataIdentifier externalAsynchronousBigDataIdentifier,
										BigDataResultMessage.Type cancelingType) {
		assert cancelingType==BigDataResultMessage.Type.CONNECTION_LOST || cancelingType==BigDataResultMessage.Type.TRANSFER_CANCELED;
		BigDataResultMessage m = new BigDataResultMessage(cancelingType, readDataLength,
				idPacket, durationInMs, asynchronousBigDataInternalIdentifier, externalAsynchronousBigDataIdentifier);
		m.setSender(receiver);
		m.setReceiver(sender);
		sender.getAgent().receiveMessage(m);
	}

	class AutoRequestedGroups implements GroupChangesNotifier {
		private final AbstractAgent agent;
		private final ArrayList<AutoRequestedGroup> auto_requested_groups;

		AutoRequestedGroups(AbstractAgent _agent) {
			agent = _agent;
			auto_requested_groups = new ArrayList<>();
		}

		public AbstractAgent getAgent() {
			return agent;
		}

		@SuppressWarnings("unchecked")
		@Override
		public void potentialChangesInGroups() {
			ArrayList<AutoRequestedGroup> list;
			synchronized (MadkitKernel.this.auto_requested_groups) {

				list = (ArrayList<AutoRequestedGroup>) auto_requested_groups.clone();
			}
			for (AutoRequestedGroup arg : list)
				arg.potentialChangesInGroups();
		}

		public void destroy() {
			for (AutoRequestedGroup arg : auto_requested_groups)
				arg.destroy();
			auto_requested_groups.clear();
		}

		public void destroy(String role) {
			auto_requested_groups.removeIf(arg -> {
				if (arg.getRole().equals(role)) {
					arg.destroy();
					return true;
				}
				else
					return false;
			});
		}

		public boolean destroy(AbstractGroup group) {
			final Reference<Boolean> deleted=new Reference<>(false);
			auto_requested_groups.removeIf(arg -> {
				if (arg.getGroups().equals(group)) {
					arg.destroy();
					deleted.set(true);
					return true;
				}
				else
					return false;
			});
			return deleted.get();
		}

		public boolean destroy(AbstractGroup group, String role) {
			final Reference<Boolean> deleted=new Reference<>(false);
			auto_requested_groups.removeIf(arg -> {
				if (arg.getGroups().equals(group) && arg.getRole().equals(role)) {
					arg.destroy();
					deleted.set(true);
					return true;
				}
				else
					return false;
			});
			return deleted.get();
		}

		public void addAutoRequestedGroup(AutoRequestedGroup _group) {
			boolean deleted = destroy(_group.getGroups());
			auto_requested_groups.add(_group);
			if (deleted)
				_group.potentialChangesInGroups();
		}

		public boolean isConcernedBy(Group _group, String role) {
			for (AutoRequestedGroup arg : auto_requested_groups) {
				if (arg.getRole().equals(role) && arg.getGroups().includes(_group))
					return true;
			}
			return false;
		}

		public boolean isEmpty() {
			return auto_requested_groups.isEmpty();
		}

	}

	static class AutoRequestedGroup {
		private final AbstractAgent agent;
		private AbstractGroup group;
		private final String role;
		private final SecureExternalizable passKey;
		private final AtomicReference<Group[]> groups = new AtomicReference<>(null);
		private final MadkitKernel kernel;

		AutoRequestedGroup(AbstractAgent _agent, AbstractGroup _group, String _role, SecureExternalizable _passKey) {
			agent = _agent;
			group = _group.clone();
			role = _role;
			passKey = _passKey;
			kernel = agent.getKernel();
		}

		public void potentialChangesInGroups() {
			// detect changes
			boolean change = false;
			Group[] gps = group == null ? new Group[0] : group.getRepresentedGroups(kernel.getKernelAddress());
			Group[] current_groups = groups.get();
			if (current_groups != gps && !(current_groups != null && current_groups.length == 0 && gps.length == 0)) {
				change = true;
			}

			if (change) {
				// detect group changes
				ArrayList<Group> groups_to_request = new ArrayList<>();
				ArrayList<Group> groups_to_leave = new ArrayList<>();
				if (current_groups == null) {
                    Collections.addAll(groups_to_request, gps);
				} else {
					for (Group g : gps) {
						boolean found = false;
						for (Group g2 : current_groups) {
							if (g2.equals(g)) {
								found = true;
								break;
							}
						}
						if (!found)
							groups_to_request.add(g);
					}

					for (Group g : current_groups) {
						boolean found = false;
						for (Group g2 : gps) {
							if (g.equals(g2)) {
								found = true;
								break;
							}
						}
						if (!found)
							groups_to_leave.add(g);
					}
				}

				groups.set(gps);
				// apply changes
				for (Group g : groups_to_request) {
					kernel.requestRole(agent, g, role, passKey, false);
				}
				for (Group g : groups_to_leave) {
					kernel.leaveRole(agent, g, role, false);
				}

			}
		}

		public AbstractGroup getGroups() {
			return group;
		}

		public AbstractAgent getAgent() {
			return agent;
		}

		public String getRole() {
			return role;
		}


		@Override
		public boolean equals(Object other) {

			if (other == null)
				return false;
			if (other instanceof AutoRequestedGroup) {
				return equals((AutoRequestedGroup) other);
			}
			return false;
		}

		public boolean equals(AutoRequestedGroup other) {
			if (other == null)
				return false;
			return other.agent == agent && other.group.equals(group) && other.role.equals(role);
		}

		public void destroy() {
			group = null;
			potentialChangesInGroups();
		}
	}

	Object weakSetBoard(AbstractAgent requester, Group group, String name, Object data) {
		try {
			return getGroup(group).weakSetBoard(requester, name, data);
		} catch (CGRNotAvailable e) {
			if (requester.isWarningOn())
				requester.logger.warning("weakSetBoard exception : " + e.getCode());
			e.printStackTrace();
		}
		return null;
	}

	Object setBoard(AbstractAgent requester, Group group, String name, Object data) {
		try {
			return getGroup(group).setBoard(requester, name, data);
		} catch (CGRNotAvailable e) {
			if (requester.isWarningOn())
				requester.logger.warning("weakSetBoard exception : " + e.getCode());
			e.printStackTrace();
		}
		return null;
	}

	Object getBoard(AbstractAgent requester, Group group, String name) {
		try {
			return getGroup(group).getBoard(requester, name);
		} catch (CGRNotAvailable ignored) {
		}
		return null;
	}

	Object removeBoard(AbstractAgent requester, Group group, String name) {
		try {
			return getGroup(group).removeBoard(requester, name);
		} catch (CGRNotAvailable e) {
			if (requester.isWarningOn())
				requester.logger.warning("weakSetBoard exception : " + e.getCode());
			e.printStackTrace();
		}
		return null;
	}

	boolean checkMemoryLeakAfterNetworkStopped() {
		Object b = getBoard(LocalCommunity.Groups.NETWORK, LocalCommunity.Boards.NETWORK_BOARD);
		if (b == null)
			return true;
		return MadkitNetworkAccess.checkNetworkBoardEmpty(b);
	}

	int numberOfValidGeneratedID() {
		synchronized (generatorIdTransfer) {
			return generatorIdTransfer.getNumberOfMemorizedIds();
		}
	}

	IDGeneratorInt getIDTransferGenerator() {
		return generatorIdTransfer;
	}

	ReturnCode setCentralDatabaseBackupReceiverFactory(AbstractAgent ignored, CentralDatabaseBackupReceiverFactory<?> centralDatabaseBackupReceiverFactory) throws DatabaseException {
		boolean changed=getMadkitConfig().setCentralDatabaseBackupReceiverFactoryImpl(centralDatabaseBackupReceiverFactory);
		if (changed) {
			if (netAgent != null) {
				Message m = new EmptyMessage();
				m.setSender(netCentralDatabaseBackupReceiverChecker);
				m.setReceiver(netAgent);
				netAgent.getAgent().receiveMessage(m);

			}
			return SUCCESS;
		}
		else
			return IGNORED;


	}
}

final class CGRNotAvailable extends Exception {

	/**
	 * 
	 */
	private static final long serialVersionUID = -375379801933609564L;
	final private ReturnCode code;

	/**
	 * @return the code
	 */
	ReturnCode getCode() {
		return code;
	}

	@Override
	public String toString() {
		return super.toString() + " " + getCode();
	}


	CGRNotAvailable(ReturnCode code) {
		this.code = code;
	}


}

abstract class AgentsJob implements Callable<Void>, Cloneable {
	private List<AbstractAgent> list;

	AgentsJob(List<AbstractAgent> list) {
		this.list = list;
	}

	@Override
	protected abstract AgentsJob clone() throws CloneNotSupportedException;

	AgentsJob() {
	}

	@Override
	public Void call() {
		for (final AbstractAgent a : list) {
			proceedAgent(a);
		}
		return null;
	}
	protected List<AbstractAgent> getClonedList()
	{
		return new ArrayList<>(list);
	}


	final ArrayList<AgentsJob> getJobs(List<AbstractAgent> l, int cpuCoreNb) {

		final ArrayList<AgentsJob> workers = new ArrayList<>(cpuCoreNb);
		int bucketSize = l.size();
		final int nbOfAgentsPerTask = bucketSize / cpuCoreNb;
		if (nbOfAgentsPerTask == 0) {
			list = l;
			workers.add(this);
			return workers;
		}
		for (int i = 0; i < cpuCoreNb; i++) {
			int firstIndex = nbOfAgentsPerTask * i;
			workers.add(createNewAgentJobWithList(l.subList(firstIndex, firstIndex + nbOfAgentsPerTask)));
		}
		workers.add(createNewAgentJobWithList(l.subList(cpuCoreNb * nbOfAgentsPerTask, l.size())));
		return workers;
	}

	abstract AgentsJob createNewAgentJobWithList(List<AbstractAgent> l) ;


	abstract void proceedAgent(AbstractAgent a);




}
