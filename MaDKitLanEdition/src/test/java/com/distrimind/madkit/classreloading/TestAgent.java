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
package com.distrimind.madkit.classreloading;

import java.lang.reflect.InvocationTargetException;
import java.util.logging.Level;

import com.distrimind.madkit.classreloading.anotherPackage.Fake;
import com.distrimind.madkit.kernel.Agent;
import com.distrimind.madkit.kernel.Madkit;
import com.distrimind.madkit.kernel.MadkitClassLoader;

/**
 * @author Fabien Michel
 * @author Jason Mahdjoub
 * @since MadKit 5.0.0.18
 * @since MadkitLanEdition 1.0
 * @version 1.0
 * 
 */
public class TestAgent extends Agent {
	/**
	 * 
	 */
	public TestAgent() {
		setLogLevel(Level.ALL);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see madkit.kernel.AbstractAgent#activate()
	 */
	@Override
	protected void activate() throws InterruptedException {
		setLogLevel(Level.ALL);
		super.activate();
		if (logger != null) {
			logger.info("\n\na\n\n");
			FakeObject o = new FakeObject();
			logger.info("\nfake is " + o);
			logger.info("\nfake2 is " + (new Fake()));
			pause(8000);
			try {
				System.err.println(System.getProperty("java.class.path"));
				MadkitClassLoader.reloadClass("madkit.classreloading.anotherPackage.Fake");
				logger.info("after reload : " + MadkitClassLoader.getLoader()
						.loadClass("madkit.classreloading.anotherPackage.Fake").getDeclaredConstructor().newInstance());
			} catch (ClassNotFoundException | SecurityException | NoSuchMethodException | InvocationTargetException | IllegalArgumentException | IllegalAccessException | InstantiationException e) {
				e.printStackTrace();
			}
            logger.info("\nfake3 is " + (new Fake()));
			pause(8000);
			try {
				MadkitClassLoader.reloadClass("madkit.classreloading.anotherPackage.Fake");
			} catch (ClassNotFoundException e) {
				e.printStackTrace();
			}
			logger.info("\nfake4 is " + (new Fake()));
		}
	}


	@Override
	protected void liveCycle() throws InterruptedException {
		if (logger != null)
			logger.info("a");
		pause(1000);

		this.killAgent(this);
	}

	public static void main(String[] argss) {
		String[] args = { "--agentLogLevel", "INFO", "--MadkitLogLevel", "OFF", "--orgLogLevel", "OFF",
				"--launchAgents", "{" + TestAgent.class.getName() + "}" + ",true" };
		Madkit.main(args);
	}

}

class FakeObject {
	@Override
	public String toString() {
		return "a";
	}
}