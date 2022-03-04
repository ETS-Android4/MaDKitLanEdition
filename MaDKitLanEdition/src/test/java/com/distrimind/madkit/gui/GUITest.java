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
package com.distrimind.madkit.gui;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertFalse;
import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.assertTrue;

import com.distrimind.madkit.gui.swing.action.AgentAction;
import org.testng.annotations.Test;
import javax.swing.JFrame;

import com.distrimind.madkit.kernel.AbstractAgent;
import com.distrimind.madkit.kernel.Agent;
import com.distrimind.madkit.kernel.TestNGMadkit;
import com.distrimind.madkit.kernel.Message;
import com.distrimind.madkit.testing.util.agent.AlwaysInCGRNormalAA;

/**
 * @author Fabien Michel
 * @since MaDKit 5.0.0.9
 * @version 0.9
 * 
 */

public class GUITest extends TestNGMadkit {

	@Test
	public void hasGUITest() {
		launchTest(new AbstractAgent() {
			@Override
			protected void activate() {
				AbstractAgent a = new AbstractAgent();
				assertEquals(ReturnCode.SUCCESS, launchAgent(a, true));
				assertTrue(a.hasGUI());
			}
		});
	}

	public void kill() {
		launchTest(new AbstractAgent() {
			@Override
			protected void activate() {
				AbstractAgent a = new AbstractAgent();
				launchAgent(a, 0, true);
				for (int i = 0; i < 100; i++) {
					killAgent(a);
				}
			}
		});
	}

	@Test
	public void setupFrameTest() {
		System.setProperty("java.awt.headless", "false");
		launchTest(new AbstractAgent() {
			@Override
			protected void activate() {
				assertEquals(ReturnCode.SUCCESS, launchAgent(new AbstractAgent() {
					private boolean ok = false;
					private JFrame f;

					@Override
					public void setupFrame(Object ...parameters) {
						ok = true;
						f = (JFrame)parameters[0];
					}

					@Override
					protected void activate() {
						assertTrue(ok);
						f.dispose();
					}
				}, true));
			}
		});
	}

	@Test
	public void noAAMessageTest() {
		launchTest(new AbstractAgent() {
			@Override
			protected void activate() {
				assertEquals(ReturnCode.SUCCESS, launchAgent(new AbstractAgent() {
					@Override
					public void setupFrame(Object...parameters) {
						assertNotNull(parameters[0]);
					}

					@Override
					protected void activate() {
						Message m = nextMessage();
						System.err.println(m);
					}
				}, true));
			}
		});
	}

	@Test
	public void launchAgentByGUITest() {
		launchTest(new AbstractAgent() {
			@Override
			protected void activate() {
				AbstractAgent a = launchAgent(AlwaysInCGRNormalAA.class.getName());
				assertEquals(1, getAgentsWithRole(GROUP, ROLE).size());
				com.distrimind.madkit.gui.swing.action.AgentAction.LAUNCH_AGENT.getActionFor(a, a.getClass().getName(), Boolean.TRUE)
						.actionPerformed(null);
				assertEquals(2, getAgentsWithRole(GROUP, ROLE).size());
			}
		});
	}

	@Test
	public void killAgentByGUITest() {
		launchTest(new AbstractAgent() {
			@Override
			protected void activate() {
				AbstractAgent a = launchAgent(AlwaysInCGRNormalAA.class.getName());
				AgentAction.KILL_AGENT.getActionFor(a, a).actionPerformed(null);
				assertFalse(a.isAlive());
			}
		});
	}

	@Test
	public void noAgentMessageTest() {
		launchTest(new AbstractAgent() {
			@Override
			protected void activate() {
				assertEquals(ReturnCode.SUCCESS, launchAgent(new Agent() {
					@Override
					public void setupFrame(Object...parameters) {
						assertNotNull(parameters[0]);
						try {
							Thread.sleep(10000);
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
					}

					@Override
					protected void activate() {
						Message m = nextMessage();
						System.err.println(m);
					}
				}, true));
			}
		});
	}

}