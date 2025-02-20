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
package com.distrimind.madkit.logging;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertTrue;

import com.distrimind.madkit.kernel.TestNGMadkit;
import org.testng.AssertJUnit;
import org.testng.annotations.Test;
import org.testng.annotations.BeforeMethod;
import static com.distrimind.madkit.kernel.AbstractAgent.ReturnCode.SUCCESS;
import java.io.File;
import java.util.Objects;
import java.util.logging.Level;

import com.distrimind.madkit.boot.process.CreateLogFilesTest;
import com.distrimind.madkit.kernel.AbstractAgent;

/**
 * @author Fabien Michel
 * @author Jason Mahdjoub
 * @since MadkitLanEdition 1.0
 * @version 1.0
 * 
 */
public class DuplicateNamesTest extends TestNGMadkit {

	protected File f;

	@BeforeMethod
	public void init() {
		addMadkitArgs("--agentLogLevel", Level.OFF.toString());
	}

	@Test
	public void setNameTest() {
		launchTest(new AbstractAgent() {
			@Override
			protected void activate() {
				setName(C);
				setLogLevel(Level.ALL);
				assertEquals(SUCCESS, launchAgent(new SetNameAgent(), true));
			}
		});
	}

	@Test
	public void setNameTestOnLogFile() {
		addMadkitArgs("--createLogFiles");
		addMadkitArgs("--guiLogLevel", Level.ALL.toString());
		launchTest(new AbstractAgent() {

			@Override
			protected void activate() {
				setName(C);
				setLogLevel(Level.ALL);
				assertEquals(SUCCESS, launchAgent(new SetNameAgent(), true));
				f = getMadkitConfig().logDirectory;
			}
		});
		System.err.println(f);
		assertTrue(f.exists());
		assertTrue(f.isDirectory());
		AssertJUnit.assertEquals(2, Objects.requireNonNull(f.listFiles(CreateLogFilesTest.filter)).length);
	}
}

class SetNameAgent extends AbstractAgent {

	public SetNameAgent() {
		setName(TestNGMadkit.C);
	}

	@Override
	protected void activate() {
		setLogLevel(Level.ALL);
		if (logger != null)
			logger.info("test");
		setLogLevel(Level.OFF);
		if (logger != null)
			logger.info("test no display");
		setLogLevel(Level.ALL);
		if (logger != null)
			logger.info("test 2");
	}
}