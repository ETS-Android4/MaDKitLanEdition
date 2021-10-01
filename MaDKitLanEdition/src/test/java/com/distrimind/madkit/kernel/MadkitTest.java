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

import static org.testng.AssertJUnit.assertTrue;
import static org.testng.AssertJUnit.assertNull;
import org.testng.annotations.Test;
import com.distrimind.madkit.action.KernelAction;
import java.util.Arrays;

/**
 * @author Fabien Michel
 * @author Jason Mahdjoub
 * @since MaDKit 5.0.0.10
 * @since MadkitLanEdition 1.0
 * @version 1.0
 * 
 */
public class MadkitTest {


	// @Before
	// public void before() {
	// mk = new Madkit(null);
	// }

	/**
	 * 
	 */
	@Test(enabled = false)
	private void testMKlogLevelBoot(String MKLogLevel) {
		System.err.println("\n\n\n\n\n--------------------MK log level = " + MKLogLevel + "-------------------");
		String[] args = { "--madkitLogLevel", MKLogLevel };
		Madkit m = new Madkit(args);
		System.err.println("\n\n--------------------MK log level = " + MKLogLevel + "-------------------\n\n\n\n\n");
		closeMadkit(m);
	}

	@Test
	public void nullArgs() {
		Madkit m = new Madkit();
		closeMadkit(m);
	}

	@Test
	public void noArg() {
		Madkit m = new Madkit();
		System.err.println(Arrays.toString(m.args));
		closeMadkit(m);
	}

	@Test
	public void buildConfigFromArgsTest() {
		Madkit m = new Madkit();
		String[] args = new String[1];
		args[0] = "";
		assertTrue(m.buildConfigFromArgs(args).isEmpty());
		assertTrue(m.buildConfigFromArgs(null).isEmpty());
		args = new String[2];
		args[0] = "";
		args[1] = "";
		assertTrue(m.buildConfigFromArgs(args).isEmpty());
		closeMadkit(m);
	}

	@Test
	public void mkLogLevelALL() {
		testMKlogLevelBoot("ALL");
	}

	@Test
	public void mkLogLevelFINEST() {
		testMKlogLevelBoot("FINEST");
	}

	@Test
	public void mkLogLevelOFF() {
		testMKlogLevelBoot("OFF");
	}

	@Test
	public void testOptionAutoLogDir() {
		// TODO
	}

	@Test
	public void defaultAgentLogging() throws InterruptedException {
		Madkit m = new Madkit();
		Thread.sleep(100);
		assertNull(m.getKernel().logger);
		String[] args = { "--desktop", "false", "--forceDesktop", "true" };
		closeMadkit(m);
		m = new Madkit(args);
		Thread.sleep(100);
		assertNull(m.getKernel().logger);
		String[] argss = { "--launchAgents", "{com.distrimind.madkit.kernel.Agent}" };
		closeMadkit(m);
		m = new Madkit(argss);
		Thread.sleep(100);
		assertNull(m.getKernel().logger);
		closeMadkit(m);
	}

	@Test
	public void doActionLaunchAgentWithDesktop() throws InterruptedException {
		Madkit m = new Madkit();
		Agent a = new Agent();
		m.doAction(KernelAction.LAUNCH_AGENT, a, Boolean.TRUE);
		Thread.sleep(100);
		assertTrue(a.isAlive());
		Thread.sleep(1000);
		closeMadkit(m);
	}

	@Test
	public void doActionLaunchAgentNoDesktop() throws InterruptedException {
		Madkit m = new Madkit("--desktop", "false", "--forceDesktop", "true");
		Agent a = new Agent();
		m.doAction(KernelAction.LAUNCH_AGENT, a, Boolean.TRUE);
		Thread.sleep(100);
		assertTrue(a.isAlive());
		Thread.sleep(1000);
		m.doAction(KernelAction.LAUNCH_AGENT, a = new Agent(), Boolean.TRUE);
		Thread.sleep(2000);
		assertTrue(a.isAlive());
		closeMadkit(m);

	}

	public static void closeMadkit(Madkit m) {
		if (m == null)
			return;
		if (m.getKernel().isAlive())
			m.doAction(KernelAction.STOP_NETWORK);

		if (m.getKernel().isAlive())
			m.doAction(KernelAction.EXIT);
		try {
			Thread.sleep(500);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	@Test
	public void buildSessionTest() {
		Madkit m=new Madkit("--desktop", "false", "--forceDesktop", "true", "--kernelLogLevel", "ALL");
		closeMadkit(m);
	}

}
