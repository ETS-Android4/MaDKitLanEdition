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
package com.distrimind.madkit.performance;

import static org.testng.AssertJUnit.assertNotNull;
import org.testng.annotations.Test;
import java.util.List;
import java.util.logging.Level;

import com.distrimind.madkit.kernel.*;
import com.distrimind.madkit.testing.util.agent.NormalAA;

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
	public void massAALaunchWithBucketAndNoRoles() {
		final int tries = 1;// should be at least 100 to bench, this has no use for only testing
		launchTest(new AbstractAgent() {
			@Override
			protected void activate() {
				setLogLevel(Level.INFO);
				long total = 0;
				// createGroup("test", "group", false, null);
				for (int i = 0; i < tries; i++) {
					startTimer();
					assertNotNull(launchAgentBucket(AbstractAgent.class.getName(), 1000000));
					total += stopTimer("bucket launch time = ");
				}
				if (logger != null)
					logger.info("average launch time = " + (total / (tries * 1000000)) + " ms");
			}
		}, AbstractAgent.ReturnCode.SUCCESS, false, properties -> properties.killAllNonThreadedAgentsDuringMaDKitClosing=false);
	}

	@Test
	public void massAALaunchWithBucketRoles() {
		final int tries = 1;// should be at least 100 to bench, this has no use for only testing
		launchTest(new AbstractAgent() {
			@Override
			protected void activate() {
				setLogLevel(Level.INFO);
				long total = 0;
				// createGroup("test", "group", false, null);
				for (int i = 0; i < tries; i++) {
					startTimer();
					assertNotNull(launchAgentBucket(AbstractAgent.class.getName(), 1000000, new Role(GROUP, ROLE)));
					total += stopTimer("bucket launch time = ");
				}
				if (logger != null)
					logger.info("average launch time = " + (total / (tries * 1000000)) + " ms");
			}
		}, AbstractAgent.ReturnCode.SUCCESS, false, properties -> properties.killAllNonThreadedAgentsDuringMaDKitClosing=false);
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
				// createGroup("test", "group", false, null);
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
				System.err.println("done\n\naverage time is " + (total / (j * 1000000L)) + " ms");
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
				/* AbstractAgent a = */launchAgent("madkit.kernel.AbstractAgent");
				// a.createGroup("test", "group", false, null);
				startTimer();
				System.err.println("begin");
				launchAgentBucket(NormalAA.class.getName(), 30100);
				stopTimer("done ");
			}
		});
	}
}