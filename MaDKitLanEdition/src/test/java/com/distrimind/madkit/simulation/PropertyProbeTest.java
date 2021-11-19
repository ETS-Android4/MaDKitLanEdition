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

import com.distrimind.madkit.kernel.AbstractAgent;
import com.distrimind.madkit.kernel.AbstractAgent.ReturnCode;
import com.distrimind.madkit.kernel.TestNGMadkit;
import com.distrimind.madkit.kernel.Watcher;
import com.distrimind.madkit.simulation.probe.PropertyProbe;
import com.distrimind.madkit.testing.util.agent.NormalAA;
import com.distrimind.madkit.testing.util.agent.SimulatedAgent;
import com.distrimind.madkit.testing.util.agent.SimulatedAgentBis;
import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import static com.distrimind.madkit.kernel.AbstractAgent.ReturnCode.SUCCESS;
import static org.testng.AssertJUnit.assertEquals;

/**
 * @author Fabien Michel
 * @author Jason Mahdjoub
 * @since MaDKit 5.0.0.2
 * @since MadkitLanEdition 1.0
 * @version 1.0
 * 
 */

public class PropertyProbeTest extends TestNGMadkit {

	@Test
	public void primitiveTypeProbing() {
		launchTest(new Watcher() {

			protected void activate() {
				SimulatedAgent agent;
				assertEquals(SUCCESS, launchAgent(agent = new SimulatedAgent()));
				PropertyProbe<AbstractAgent, Integer> fp = new PropertyProbe<>(GROUP, ROLE, "privatePrimitiveField");
				addProbe(fp);
				assertEquals(1, fp.getPropertyValue(agent).intValue());
				PropertyProbe<AbstractAgent, Double> fp2 = new PropertyProbe<>(GROUP, ROLE, "publicPrimitiveField");
				addProbe(fp2);
				assertEquals(2, fp2.getPropertyValue(agent).intValue());
				agent.setPrivatePrimitiveField(10);
				assertEquals(10, fp.getPropertyValue(agent).intValue());
				assertEquals(SUCCESS, launchAgent(agent = new SimulatedAgent() {

					@Override
					public void setPrivatePrimitiveField(int privatePrimitiveField) {
						super.setPrivatePrimitiveField(100);
					}
				}));
				agent.setPrivatePrimitiveField(10);
				assertEquals(2, fp.size());
				assertEquals(100, fp.getPropertyValue(agent).intValue());
			}
		});
	}

	@Test
	public void multiTypeProbing() {
		launchTest(new Watcher() {

			protected void activate() {
				SimulatedAgent agent;
				SimulatedAgentBis agentBis;
				assertEquals(SUCCESS, launchAgent(agent = new SimulatedAgent()));
				assertEquals(SUCCESS, launchAgent(agentBis = new SimulatedAgentBis()));
				PropertyProbe<AbstractAgent, Integer> fp = new PropertyProbe<>(GROUP, ROLE, "privatePrimitiveField");
				addProbe(fp);
				assertEquals(1, fp.getPropertyValue(agent).intValue());
				assertEquals(1, fp.getPropertyValue(agentBis).intValue());
				PropertyProbe<AbstractAgent, Double> fp2 = new PropertyProbe<>(GROUP, ROLE, "publicPrimitiveField");
				addProbe(fp2);
				double i = fp2.getPropertyValue(agent);
				System.err.println(i);
				assertEquals(2, fp2.getPropertyValue(agent).intValue());
				agent.setPrivatePrimitiveField(10);
				assertEquals(10, fp.getPropertyValue(agent).intValue());
				assertEquals(SUCCESS, launchAgent(agent = new SimulatedAgent() {

					@Override
					public void setPrivatePrimitiveField(int privatePrimitiveField) {
						super.setPrivatePrimitiveField(100);
					}
				}));
				agent.setPrivatePrimitiveField(10);
				assertEquals(3, fp.size());
				assertEquals(100, fp.getPropertyValue(agent).intValue());
			}
		});
	}

	@Test
	public void wrongTypeProbing() {
		launchTest(new Watcher() {

			protected void activate() {
				SimulatedAgent agent;
				assertEquals(SUCCESS, launchAgent(agent = new SimulatedAgent()));
				PropertyProbe<AbstractAgent, String> fp = new PropertyProbe<>(GROUP, ROLE, "privatePrimitiveField");
				addProbe(fp);
                System.err.println(fp.getPropertyValue(agent));
                noExceptionFailure();
            }
		}, ReturnCode.AGENT_CRASH);
	}

	@SuppressWarnings("unused")
	@Test
	public void wrongSourceProbing() {
		launchTest(new Watcher() {

			protected void activate() {
				assertEquals(SUCCESS, launchAgent(new SimulatedAgent()));
				PropertyProbe<AbstractAgent, Integer> fp = new PropertyProbe<>(GROUP, ROLE, "privatePrimitiveField");
				addProbe(fp);
                NormalAA normalAA = new NormalAA() {

                    final String privatePrimitiveField = "test";
                };
                System.err.println(fp.getPropertyValue(normalAA));
                int i = fp.getPropertyValue(normalAA);
                noExceptionFailure();
            }
		}, ReturnCode.AGENT_CRASH);
	}

	@Test
	public void wrongTypeSetting() {
		launchTest(new Watcher() {

			protected void activate() {
				SimulatedAgent agent;
				assertEquals(SUCCESS, launchAgent(agent = new SimulatedAgent()));
				PropertyProbe<AbstractAgent, Object> fp = new PropertyProbe<>(GROUP, ROLE, "privatePrimitiveField");
				addProbe(fp);
                fp.setPropertyValue(agent, "a");
                noExceptionFailure();
            }
		}, ReturnCode.AGENT_CRASH);
	}

	@Test
	public void noSuchFieldProbing() {
		launchTest(new AbstractAgent() {

			protected void activate() {
				launchDefaultAgent(this);
				PropertyProbe<AbstractAgent, String> fp = new PropertyProbe<>(GROUP, ROLE, "privatePrimitiveField");
				Watcher s = new Watcher();
				assertEquals(SUCCESS, launchAgent(s));
				s.addProbe(fp);
                System.err.println(fp.getPropertyValue(fp.getCurrentAgentsList().get(0)));
                noExceptionFailure();
            }
		}, ReturnCode.AGENT_CRASH);
	}

	@Test
	public void testGetMinAndGetMax() {
		launchTest(new AbstractAgent() {

			protected void activate() {
				for (int i = 0; i < 10; i++) {
					// launchDefaultAgent(this);
					SimulatedAgent agent;
					assertEquals(SUCCESS, launchAgent(agent = new SimulatedAgent()));
					agent.publicPrimitiveField = i;
				}
				PropertyProbe<AbstractAgent, Double> fp = new PropertyProbe<>(GROUP, ROLE, "publicPrimitiveField");
				Watcher s = new Watcher();
				assertEquals(SUCCESS, launchAgent(s));
				s.addProbe(fp);
				assertEquals(9d, fp.getMaxValue());
				assertEquals(0d, fp.getMinValue());
			}
		}, ReturnCode.SUCCESS);
	}

	@Test
	public void getAverageTest() {
		launchTest(new AbstractAgent() {

			protected void activate() {
				for (int i = 0; i < 12; i++) {
					// launchDefaultAgent(this);
					SimulatedAgent agent;
					assertEquals(SUCCESS, launchAgent(agent = new SimulatedAgent()));
					agent.publicPrimitiveField = i;
					agent.setPrivatePrimitiveField(i * 2);
				}
				PropertyProbe<AbstractAgent, String> fp = new PropertyProbe<>(GROUP, ROLE, "publicPrimitiveField");
				PropertyProbe<AbstractAgent, String> fpInt = new PropertyProbe<>(GROUP, ROLE, "privatePrimitiveField");
				Watcher s = new Watcher();
				assertEquals(SUCCESS, launchAgent(s));
				s.addProbe(fp);
				s.addProbe(fpInt);
				assertEquals(5.5d, fp.getAverageValue(), 0.0);
				assertEquals(11d, fpInt.getAverageValue(), 0.0);
			}
		}, ReturnCode.SUCCESS);
	}

	@Test
	public void noAgentgetAverageTest() {
		launchTest(new AbstractAgent() {

			protected void activate() {
				PropertyProbe<AbstractAgent, String> fp = new PropertyProbe<>(GROUP, ROLE, "publicPrimitiveField");
				Watcher s = new Watcher();
				assertEquals(SUCCESS, launchAgent(s));
				s.addProbe(fp);
				AssertJUnit.assertTrue(Double.isNaN(fp.getAverageValue()));
			}
		}, ReturnCode.SUCCESS);
	}

	@Test
	public void getMinAndGetMaxnotComparable() {
		launchTest(new AbstractAgent() {

			protected void activate() {
				// launchDefaultAgent(this);
				assertEquals(SUCCESS, launchAgent(new SimulatedAgent()));
				PropertyProbe<AbstractAgent, String> fp = new PropertyProbe<>(GROUP, ROLE, "objectField");
				Watcher s = new Watcher();
				assertEquals(SUCCESS, launchAgent(s));
				s.addProbe(fp);
				try {
					System.err.println(fp.getMaxValue());
					noExceptionFailure();
				} catch (SimulationException e) {
					e.printStackTrace();
				}
				try {
					System.err.println(fp.getAverageValue());
					noExceptionFailure();
				} catch (SimulationException e) {
					e.printStackTrace();
				}
				try {
					System.err.println(fp.getMinValue());
					noExceptionFailure();
				} catch (SimulationException e) {
					e.printStackTrace();
				}
			}
		}, ReturnCode.SUCCESS);
	}

}