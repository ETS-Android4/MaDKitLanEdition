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
import static org.testng.AssertJUnit.assertFalse;
import static org.testng.AssertJUnit.assertTrue;

import com.distrimind.madkit.kernel.TestNGMadkit;
import org.testng.AssertJUnit;
import org.testng.annotations.AfterClass;
import org.testng.annotations.Test;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import com.distrimind.madkit.kernel.AbstractAgent;
import com.distrimind.madkit.kernel.Madkit;

/**
 * @author Fabien Michel
 * @author Jason Mahdjoub
 * @since MaDKit 5.0.0.10
 * @since MadkitLanEdition 1.0
 * @version 1.0
 * 
 */

@SuppressWarnings("ResultOfMethodCallIgnored")
public class CreateLogFilesTest extends TestNGMadkit {

	File f;
	public static final FilenameFilter filter = (dir, s) -> !s.contains(".lck");

	static void delete(File f) throws IOException {
		if (f.isDirectory()) {
			for (File c : Objects.requireNonNull(f.listFiles()))
				delete(c);
		}
		if (!f.delete())
			throw new FileNotFoundException("Failed to delete file: " + f);
	}


	@Test
	public void logDirectoryUniqueness() throws IOException {
		new TestNGMadkit();
		String dir = System.getProperty("java.io.tmpdir") + File.separatorChar + testName;
		try {
			delete(new File(dir));
		} catch (IOException ignored) {
		}

		String[] args = { "--madkitLogLevel", "OFF", "--desktop", "false", "--forceDesktop", "true", "--launchAgents",
				"{com.distrimind.madkit.kernel.AbstractAgent}", // to not have the desktop mode by
				// default
				// Option.logDirectory.toString(), getBinTestDir(),
				// LevelOption.agentLogLevel.toString(), "ALL",
				"--createLogFiles", "--kernelLogLevel", "INFO", "--logDirectory", dir };
		List<Madkit> lm=new ArrayList<>();
		for (int i = 0; i < 50; i++) {
			lm.add(new Madkit(args));
		}
		pause(null, 200);
		assertEquals(50, Objects.requireNonNull(new File(dir).listFiles()).length);
		for (Madkit m : lm)
			closeMadkit(m);
	}

	@Test
	public void defaultLogDirectory() {
		addMadkitArgs("--createLogFiles", "--kernelLogLevel", "INFO");
		launchTest(new AbstractAgent() {

			@Override
			protected void activate() {
				System.err.println(getMadkitConfig().logDirectory);
				f = getMadkitConfig().logDirectory;
			}
		});
		pause(null, 100);
		System.err.println(f);
		assertTrue(f.exists());
		assertTrue(f.isDirectory());

		assertEquals(3, Objects.requireNonNull(f.listFiles(filter)).length);
	}

	@Test
	public void oneAgentLog() {
		launchTest(new AbstractAgent() {

			@Override
			protected void activate() {
				f = new File(getMadkitConfig().logDirectory, getLogger().getName());
				assertFalse(f.exists());
				getLogger().createLogFile();
				System.err.println(f);
				assertTrue(f.exists());
				f.delete();
			}
		});
	}

	@Test
	public void oneAgentLogInConstructor() {
		launchTest(new LogTester() {

			@Override
			protected void activate() throws InterruptedException {
				super.activate();
				f = new File(this.getMadkitConfig().logDirectory, "[" + this.getName() + "]");
				System.err.println(f);
				assertTrue(f.exists());
				f.delete();
			}
		});
		pause(null, 100);
	}

	@Test
	public void noLogDirectory() {
		addMadkitArgs("--createLogFiles", "false", "--kernelLogLevel", "INFO");
		launchTest(new AbstractAgent() {

			@Override
			protected void activate() {
				System.err.println(getMadkitConfig().logDirectory);
				assertFalse(getMadkitConfig().logDirectory.exists());
			}
		});
		pause(null, 100);
	}

	@Test
	public void absoluteLogDirectory() {
		addMadkitArgs("--createLogFiles", "--logDirectory", System.getProperty("java.io.tmpdir"), "--kernelLogLevel",
				"ALL", "--madkitLogLevel", "OFF");
		launchTest(new AbstractAgent() {

			@Override
			protected void activate() {
				System.err.println(getMadkitConfig().logDirectory);
				f = getMadkitConfig().logDirectory;
			}
		});
		assertTrue(f.exists());
		assertTrue(f.isDirectory());
		pause(null, 500);
		assertEquals(3, Objects.requireNonNull(f.listFiles(filter)).length);
	}

	@Test
	public void noFilesOnLogOFF() {
		addMadkitArgs("--createLogFiles", "--kernelLogLevel", "OFF", "--agentLogLevel", "OFF", "--madkitLogLevel",
				"ALL");
		launchTest(new AbstractAgent() {

			@Override
			protected void activate() {
				System.err.println(getMadkitConfig().logDirectory);
				f = getMadkitConfig().logDirectory;
			}
		});
		assertFalse(f.exists());
	}

	@Test
	public void noKernelFile() {
		addMadkitArgs("--createLogFiles", "TRUE", "--kernelLogLevel", "OFF");
		launchTest(new AbstractAgent() {

			@Override
			protected void activate() {
				assertTrue(getMadkitConfig().createLogFiles);
				System.err.println("log directory : " + getMadkitConfig().logDirectory);
				f = getMadkitConfig().logDirectory;
				assertTrue(f.exists());
				assertTrue(f.isDirectory());
			}
		});
		pause(null, 500);
		AssertJUnit.assertEquals(2, Objects.requireNonNull(f.listFiles(filter)).length);
	}

	@AfterClass
	public static void clean() {
		System.err.println(new File("logs").getAbsolutePath());
		new File("logs").delete();
	}

}

class LogTester extends AbstractAgent {

	public LogTester() {

	}

	@Override
	protected void activate() throws InterruptedException {
		super.activate();
		getLogger().createLogFile();
	}
}