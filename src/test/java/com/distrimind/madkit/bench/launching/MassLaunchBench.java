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
package com.distrimind.madkit.bench.launching;

import org.testng.annotations.Test;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

import com.distrimind.madkit.kernel.*;
import com.distrimind.madkit.performance.MiniAgent;
import com.distrimind.madkit.testing.util.agent.NormalAA;
import com.distrimind.madkit.testing.util.agent.PongAgent;

/**
 * @author Fabien Michel
 * @author Jason Mahdjoub
 * @since MaDKit 5.0.0.10
 * @since MadkitLanEdition 1.0
 * @version 1.0
 * 
 */
public class MassLaunchBench extends TestNGMadkit {

	@Test
	public void massAALaunchWithBucket() {// TODO more cases
		launchTest(new AbstractAgent() {
			@Override
			protected void activate() {
				setLogLevel(Level.OFF);
				System.err.println("begin");
				for (int i = 0; i < 10; i++) {
					startTimer();
					launchAgentBucket(AbstractAgent.class.getName(), 10_000_000, 11);
					stopTimer("bucket launch time = ");
				}
			}
		});
	}

	@Test
	public void massAALaunchWithBucketRoles() {
		launchTest(new AbstractAgent() {
			@Override
			protected void activate() {
				System.err.println("begin");
				startTimer();
				launchAgentBucket(AbstractAgent.class.getName(), 1000000, new Role(GROUP, ROLE));
				stopTimer("bucket launch time = ");
				System.err.println("done\n\n");
			}
		}, AbstractAgent.ReturnCode.SUCCESS, false, new MadkitEventListener() {
			@Override
			public void onMaDKitPropertiesLoaded(MadkitProperties properties) {
				properties.killAllNonThreadedAgentsDuringMaDKitClosing=false;
			}
		});
	}

	@Test
	public void massAALaunchWithBucketRolesAndRequestIgnored() {
		launchTest(new AbstractAgent() {
			@Override
			protected void activate() {
				System.err.println("begin");
				startTimer();
				List<AbstractAgent> l = launchAgentBucket(MiniAgent.class.getName(), 1000000, new Role(GROUP, ROLE));
				stopTimer("bucket launch time = ");
				System.err.println("done\n\n");
				System.err.println(l.get(0).requestRole(GROUP, ROLE));
			}
		}, AbstractAgent.ReturnCode.SUCCESS, false, properties -> properties.killAllNonThreadedAgentsDuringMaDKitClosing=false);
	}

	@Test
	public void massAALaunch() {// TODO more cases
		addMadkitArgs("--agentLogLevel", "OFF");
		launchTest(new AbstractAgent() {
			@Override
			protected void activate() {
				setLogLevel(Level.OFF);
				System.err.println("begin");
				long total = 0;
				int j;
				for (j = 0; j < 4; j++) {
					startTimer();
					for (int i = 0; i < 100000; i++) {
						launchAgent(new AbstractAgent());
					}
					total += stopTimer("launch time = ");
				}
				System.err.println("done\n\naverage time is " + (total / (j * 1000000)) + " ms");
			}
		});
	}

	@SuppressWarnings("unchecked")
	@Test
	public void massInstantiation() throws ClassNotFoundException, InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException, NoSuchMethodException, SecurityException {
		final Class<? extends AbstractAgent> constructor = (Class<? extends AbstractAgent>) MadkitClassLoader
				.getLoader().loadClass(AbstractAgent.class.getName());
		int nbOfAgentsPerTask = 1_000_000;
		long sum = 0;
		long min = Long.MAX_VALUE;
		long max = Long.MIN_VALUE;
		int nbIteration = 100;
		for (int i = 0; i < nbIteration; i++) {
			startTimer();
			final List<AbstractAgent> list = new ArrayList<>(nbOfAgentsPerTask);
			for (int j = nbOfAgentsPerTask; j > 0; j--) {
				list.add(constructor.getDeclaredConstructor().newInstance());
			}
			if (i > 1) {
				long delta = stopTimer("done ");
				sum += delta;
				min = Math.min(min, delta);
				max = Math.max(max, delta);
			}
		}
		long average = sum / (nbIteration - 2);// the first two are ignored
		System.out.println("Min    : " + min + " ms");
		System.out.println("Average: " + average + " ms");
		System.out.println("Max    : " + max + " ms");
	}
	// as of beltegeuse -Xmx huge
	// Min : 82 ms
	// Average: 101 ms
	// Max : 1329 ms

	@Test
	public void massLaunch() {
		addMadkitArgs("--agentLogLevel", "OFF");
		launchTest(new AbstractAgent() {
			@Override
			protected void activate() throws InterruptedException {
				if (logger != null) {
					logger.info("\n******************* STARTING MASS LAUNCH *******************\n");
				}
				for (int j = 0; j < 3; j++) {
					startTimer();
					System.err.println("begin");
					for (int i = 0; i < 1_000_000; i++) {
						if (i%50000==0) {
							System.out.println("---------------------------");
							System.out.println("Max number of threads " + getScheduledPoolExecutor(this).getMaximumNumberOfThreads());
							System.out.println("Number of threads " + getScheduledPoolExecutor(this).getCurrentNumberOfExecutors());
							System.out.println("Number of working threads " + getScheduledPoolExecutor(this).getCurrentNumberOfWorkingThreads());
							System.out.println("Number of suspended threads " + getScheduledPoolExecutor(this).getCurrentNumberOfSuspendedThreads());
							System.out.println("Number of tasks " + getScheduledPoolExecutor(this).getCurrentNumberOfWaitingTasks());

						}
						launchAgent(new AbstractAgent(), 0);
					}
					sleep(5000);
					System.out.println("---------------------------");
					System.out.println("Max number of threads " + getScheduledPoolExecutor(this).getMaximumNumberOfThreads());
					System.out.println("Number of threads " + getScheduledPoolExecutor(this).getCurrentNumberOfExecutors());
					System.out.println("Number of working threads " + getScheduledPoolExecutor(this).getCurrentNumberOfWorkingThreads());
					System.out.println("Number of suspended threads " + getScheduledPoolExecutor(this).getCurrentNumberOfSuspendedThreads());
					System.out.println("Number of tasks " + getScheduledPoolExecutor(this).getCurrentNumberOfWaitingTasks());
					stopTimer("done ");
				}
			}
		});
	}

	@Test
	public void massLaunchWithList() {
		addMadkitArgs("--agentLogLevel", "OFF");
		launchTest(new AbstractAgent() {
			@Override
			protected void activate() {
				if (logger != null) {
					logger.info("\n******************* STARTING MASS LAUNCH *******************\n");
				}
				List<AbstractAgent> agents = new ArrayList<>(1_000_000);
				for (int i = 0; i < 1_000_000; i++) {
					agents.add(new AbstractAgent());
				}
				startTimer();
				System.err.println("begin");
				launchAgentBucket(agents);
				stopTimer("done ");
				startTimer();
				System.err.println("begin");
				for (AbstractAgent abstractAgent : agents) {
					killAgent(abstractAgent);
				}
				stopTimer("done ");
			}
		});
	}

	@Test
	public void massAgentLaunch() {
		addMadkitArgs("--agentLogLevel", "OFF");
		launchTest(new AbstractAgent() {
			@Override
			protected void activate() {
				if (logger != null) {
					logger.info("\n******************* STARTING MASS LAUNCH *******************\n");
				}
				List<AbstractAgent> agents = new ArrayList<>(1000);
				for (int i = 0; i < 1000; i++) {
					agents.add(new PongAgent());
				}
				startTimer();
				System.err.println("begin");
				for (AbstractAgent abstractAgent : agents) {
					launchAgent(abstractAgent);
				}
				stopTimer("done ");
				TestNGMadkit.pause(this, 100000);
			}
		});
	}

	@Test
	public void massNormalLifeLaunch() {// TODO more cases
		launchTest(new AbstractAgent() {
			@Override
			protected void activate() {

				if (logger != null) {
					logger.info("\n******************* STARTING MASS LAUNCH *******************\n");
				}
				/* AbstractAgent a = */ launchAgent(AbstractAgent.class.getName());
				startTimer();
				System.err.println("begin");
				launchAgentBucket(NormalAA.class.getName(), 30100);
				stopTimer("done ");
			}
		});
	}
}