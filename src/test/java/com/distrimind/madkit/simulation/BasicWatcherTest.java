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
package com.distrimind.madkit.simulation;

import static org.testng.AssertJUnit.assertEquals;
import org.testng.annotations.Test;
import static com.distrimind.madkit.kernel.AbstractAgent.ReturnCode.SUCCESS;
import com.distrimind.madkit.kernel.AbstractAgent;
import com.distrimind.madkit.kernel.TestNGMadkit;
import com.distrimind.madkit.kernel.Probe;
import com.distrimind.madkit.kernel.Watcher;

/**
 * @author Fabien Michel
 * @since MaDKit 5.0.0.2
 * @version 0.9
 * 
 */

public class BasicWatcherTest extends TestNGMadkit {

	@Test
	public void addingNullProbe() {
		launchTest(new AbstractAgent() {
			@Override
			protected void activate() {
				createDefaultCGR(this);
				Watcher s = new Watcher();
				assertEquals(SUCCESS, launchAgent(s));
				try {
					Probe<AbstractAgent> a = new Probe<>(null, null);
					s.addProbe(a);
					noExceptionFailure();
				} catch (NullPointerException e) {
					e.printStackTrace();
				}
				try {
					Probe<AbstractAgent> a = new Probe<>(GROUP, null);
					s.addProbe(a);
					noExceptionFailure();
				} catch (NullPointerException e) {
					e.printStackTrace();
				}
				try {
					Probe<AbstractAgent> a = new Probe<>(null, ROLE);
					s.addProbe(a);
					noExceptionFailure();
				} catch (NullPointerException e) {
					e.printStackTrace();
				}
			}
		});
	}

	@Test
	public void addingAndRemovingProbes() {
		launchTest(new AbstractAgent() {
			@Override
			protected void activate() {
				createDefaultCGR(this);
				Watcher s = new Watcher();
				assertEquals(SUCCESS, launchAgent(s));

				// ///////////////////////// REQUEST ROLE ////////////////////////
				Probe<AbstractAgent> a = new Probe<>(GROUP, ROLE);
				s.addProbe(a);
				assertEquals(1, a.size());

				destroyGroup(GROUP);
				// assertEquals(SUCCESS, code);
				assertEquals(0, a.size());
				assertEquals(ReturnCode.SUCCESS, createGroup(GROUP, null));
				assertEquals(SUCCESS, requestRole(GROUP, ROLE, null));

				assertEquals(1, a.size());
				assertEquals(SUCCESS, leaveGroup(GROUP));
				assertEquals(0, a.size());
				// Adding and removing while group does not exist
				s.removeProbe(a);
				assertEquals(0, a.size());
				s.addProbe(a);
				assertEquals(0, a.size());
				assertEquals(SUCCESS, createGroup(GROUP, null));
				assertEquals(SUCCESS, requestRole(GROUP, ROLE, null));
				AbstractAgent other = new AbstractAgent() {
					protected void activate() {
						assertEquals(SUCCESS, requestRole(GROUP, ROLE, null));
					}
				};
				assertEquals(SUCCESS, launchAgent(other));
				assertEquals(2, a.size());
				s.removeProbe(a);
				assertEquals(0, a.size());

				s.addProbe(a);
				assertEquals(2, a.size());
				assertEquals(SUCCESS, leaveGroup(GROUP));
				assertEquals(1, a.size());
				assertEquals(SUCCESS, other.leaveGroup(GROUP));
				assertEquals(0, a.size());
				assertEquals(SUCCESS, createGroup(GROUP, null));
				assertEquals(SUCCESS, requestRole(GROUP, ROLE, null));
				assertEquals(SUCCESS, other.requestRole(GROUP, ROLE, null));
				assertEquals(2, a.size());
				killAgent(s);
				assertEquals(0, a.size());
			}
		});
	}

}
