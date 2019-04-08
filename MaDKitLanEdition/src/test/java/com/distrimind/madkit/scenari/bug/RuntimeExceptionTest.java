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
package com.distrimind.madkit.scenari.bug;

import static com.distrimind.madkit.kernel.AbstractAgent.ReturnCode.ALREADY_KILLED;
import static com.distrimind.madkit.kernel.AbstractAgent.ReturnCode.SUCCESS;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.distrimind.madkit.kernel.AbstractAgent;
import com.distrimind.madkit.kernel.JunitMadkit;
import com.distrimind.madkit.kernel.AbstractAgent.ReturnCode;
import com.distrimind.madkit.testing.util.agent.FaultyAA;

/**
 * @author Fabien Michel
 * @author Jason Mahdjoub
 * @since MaDKit 5.0.0.10
 * @since MadkitLanEdition 1.0
 * @version 1.0
 * 
 */

public class RuntimeExceptionTest extends JunitMadkit {

	@Test
	public void nullPointerInActivate() {
		final AbstractAgent a = new FaultyAA(true);
		launchTest(a, ReturnCode.AGENT_CRASH, true, new Runnable() {

			@Override
			public void run() {
				System.err.println(getKernel(a.getKernelAddress()).getOrganizationSnapShot(false));
				assertAgentIsTerminated(a);
				assertFalse(getKernel(a.getKernelAddress()).isCommunity(C));
			}
		});
	}

	@Test
	public void nullPointerInEnd() {
		launchTest(new AbstractAgent() {
			protected void activate() {
				AbstractAgent a = new FaultyAA(false, true);
				assertEquals(SUCCESS, launchAgent(a));
				assertTrue(a.isAlive());
				assertTrue(getKernel(getKernelAddress()).isCommunity(C));
				assertEquals(SUCCESS, killAgent(a));
				assertEquals(ALREADY_KILLED, killAgent(a));
				assertAgentIsTerminated(a);
				assertFalse(getKernel(getKernelAddress()).isCommunity(C));
			}
		});
	}

}
