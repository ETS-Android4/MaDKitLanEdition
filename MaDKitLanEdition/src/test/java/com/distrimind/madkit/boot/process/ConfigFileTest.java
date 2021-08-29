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
package com.distrimind.madkit.boot.process;

import static org.testng.AssertJUnit.assertEquals;
import org.testng.annotations.Test;
import java.net.URL;
import java.util.logging.Level;

import com.distrimind.madkit.kernel.AbstractAgent;
import com.distrimind.madkit.kernel.JunitMadkit;

/**
 * @author Fabien Michel
 * @author Jason Mahdjoub
 * @since MaDKit 5.0.1
 * @since MadkitLanEdition 1.0
 * @version 1.0
 * 
 */

public class ConfigFileTest extends JunitMadkit {

	// TODO rehabilitate this test

	/*
	 * @Test public void configFile() { addMadkitArgs("--configFiles",
	 * "{test/com/distrimind/madkit/boot/process/madkit.xml}","--madkitLogLevel",
	 * Level.ALL.toString()); launchTest(new AbstractAgent() {
	 * 
	 * @Override protected void activate() { assertEquals(new
	 * File("mkledbtest"),getMadkitConfig().databaseFile); } }); }
	 */

	@Test
	public void multiConfigFileXML() {
		addMadkitArgs("--configFiles",
				"{src/test/resources/com/distrimind/madkit/boot/process/madkit.xml;src/test/resources/com/distrimind/madkit/boot/process/madkit2.xml}",
				"--madkitLogLevel", Level.ALL.toString());
		launchTest(new AbstractAgent() {
			@Override
			protected void activate() {
				try {
					// assertEquals(new File("mkledbtest"),getMadkitConfig().databaseFile);
					assertEquals(new URL("http://madkitlanedition.test.com"), getMadkitConfig().madkitWeb);
				} catch (Exception e) {
					assertEquals(-1, 0);
				}
			}
		});
	}
	
	@Test
	public void multiConfigFileYAML() {
		addMadkitArgs("--configFiles",
				"{src/test/resources/com/distrimind/madkit/boot/process/madkit.yaml;src/test/resources/com/distrimind/madkit/boot/process/madkit2.yaml}",
				"--madkitLogLevel", Level.ALL.toString());
		launchTest(new AbstractAgent() {
			@Override
			protected void activate() {
				try {
					// assertEquals(new File("mkledbtest"),getMadkitConfig().databaseFile);
					assertEquals(new URL("http://madkitlanedition.test.com"), getMadkitConfig().madkitWeb);
				} catch (Exception e) {
					assertEquals(-1, 0);
				}
			}
		});
	}

	@Test
	public void multiConfigOptionsXML() {
		addMadkitArgs("--configFiles",
				"{src/test/resources/com/distrimind/madkit/boot/process/madkit.xml;src/test/resources/com/distrimind/madkit/boot/process/madkit2.xml}",
				"--networkProperties.network", "true", "--madkitLogLevel", Level.ALL.toString());
		launchTest(new AbstractAgent() {
			@Override
			protected void activate() {
				try {
					// assertEquals(new File("mkledbtest"),getMadkitConfig().databaseFile);
					assertEquals(new URL("http://madkitlanedition.test.com"), getMadkitConfig().madkitWeb);
				} catch (Exception e) {
					assertEquals(-1, 0);
				}
			}
		});
	}
	
	@Test
	public void multiConfigOptionsYAML() {
		addMadkitArgs("--configFiles",
				"{src/test/resources/com/distrimind/madkit/boot/process/madkit.yaml;src/test/resources/com/distrimind/madkit/boot/process/madkit2.yaml}",
				"--networkProperties.network", "true", "--madkitLogLevel", Level.ALL.toString());
		launchTest(new AbstractAgent() {
			@Override
			protected void activate() {
				try {
					// assertEquals(new File("mkledbtest"),getMadkitConfig().databaseFile);
					assertEquals(new URL("http://madkitlanedition.test.com"), getMadkitConfig().madkitWeb);
				} catch (Exception e) {
					assertEquals(-1, 0);
				}
			}
		});
	}
}
