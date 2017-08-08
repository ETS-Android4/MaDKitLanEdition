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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.FilenameFilter;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.junit.Test;

import com.distrimind.madkit.kernel.AbstractAgent;
import com.distrimind.madkit.kernel.AgentLogger;
import com.distrimind.madkit.kernel.KernelException;
import com.distrimind.madkit.kernel.AbstractAgent.ReturnCode;

/**
 * @author Fabien Michel
 * @author Jason Mahdjoub
 * @since MaDKit 5.0.0.6
 * @since MadkitLanEdition 1.0
 * @version 1.0
 * 
 * 
 */
public class AgentLoggerTest extends JunitMadkit {

	@Test
	public void noLogger() {
		AbstractAgent a = new AbstractAgent();
		a.logger.info("testing");
		assertSame(AgentLogger.defaultAgentLogger, a.logger);
	}

	@Test
	public void logLevelOFF() {
		AgentLog a = new AgentLog(Level.OFF);
		assertNull(a.logger);
		a.setLogLevel(Level.ALL);
		assertNotNull(a.logger);
		a.logger.info("testing");
	}

	@Test
	public void logLevelALL() {
		AgentLog a = new AgentLog(Level.ALL);
		assertNotNull(a.logger);
		try {
			a.createGroup(new Group("test", "test"));
			fail();
		} catch (KernelException e) {
			e.printStackTrace();
		}
		a.setLogLevel(Level.OFF);
		assertNull(a.logger);
		try {
			a.createGroup(new Group("test", "test"));
			fail();
		} catch (KernelException e) {
			e.printStackTrace();
		}
	}

	@Test
	public void setNameAndThenLog() {
		AgentLog a = new AgentLog(Level.OFF);
		a.setName("TEST");
		assertNull(a.logger);
		a.setLogLevel(Level.ALL);
		assertNotNull(a.logger);
		System.err.println(a.getName());
		System.err.println(a.getName());
		assertEquals("[" + a.getName() + "]", a.logger.getName());
	}

	@Test
	public void logAndThenSetName() {
		AgentLog a = new AgentLog(Level.ALL);
		assertNotNull(a.logger);
		String defaultName = "[" + a.getClass().getSimpleName() + "-" + a.getAgentID() + "]";
		assertEquals(defaultName, a.logger.getName());
		a.setName("TEST");
		assertNotNull(a.logger);
		assertEquals(defaultName, a.logger.getName());
	}

	@Test
	public void logOnAndOffAndOnEquality() {
		AgentLog a = new AgentLog(Level.ALL);
		assertNotNull(a.logger);
		Logger l = a.logger;
		a.setLogLevel(Level.OFF);
		assertNull(a.logger);
		a.setLogLevel(Level.ALL);
		assertNotNull(a.logger);
		assertEquals(l, a.logger);
	}

	@Test
	public void severeLogTest() {
		launchTest(new AbstractAgent() {
			@Override
			protected void activate() {
				getLogger().severeLog("test", null);
				getLogger().severeLog("test", new Exception());
				JunitMadkit.pause(this, 1000);
			}
		}, ReturnCode.SUCCESS, true);
	}

	@Test
	public void onlyOneFileTest() {
		addMadkitArgs("--createLogFiles", "false");
		launchTest(new AbstractAgent() {
			@Override
			protected void activate() {
				System.err.println(getMadkitConfig().logDirectory);
				getLogger().createLogFile();
				if (logger != null)
					logger.fine(getName());
				File f = getMadkitConfig().logDirectory;
				assertEquals(1, f.listFiles(new FilenameFilter() {
					@Override
					public boolean accept(File arg0, String s) {
						return s.contains(getName()) && !s.contains(".lck");
					}
				}).length);
			}
		}, ReturnCode.SUCCESS, true);

	}

}

class AgentLog extends AbstractAgent {
	AgentLog(Level lvl) {
		setLogLevel(lvl);
	}
}