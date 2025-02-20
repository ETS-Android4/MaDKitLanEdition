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
package com.distrimind.madkit.api.abstractAgent;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertTrue;

import com.distrimind.madkit.kernel.TestNGMadkit;
import org.testng.annotations.Test;
import static com.distrimind.madkit.kernel.AbstractAgent.ReturnCode.AGENT_CRASH;
import static com.distrimind.madkit.kernel.AbstractAgent.ReturnCode.ALREADY_KILLED;
import static com.distrimind.madkit.kernel.AbstractAgent.ReturnCode.ALREADY_LAUNCHED;
import static com.distrimind.madkit.kernel.AbstractAgent.ReturnCode.NOT_YET_LAUNCHED;
import static com.distrimind.madkit.kernel.AbstractAgent.ReturnCode.SUCCESS;
import static com.distrimind.madkit.kernel.AbstractAgent.ReturnCode.TIMEOUT;
import java.util.ArrayList;
import java.util.logging.Level;

import com.distrimind.madkit.kernel.AbstractAgent;
import com.distrimind.madkit.testing.util.agent.DoItDuringLifeCycleAbstractAgent;
import com.distrimind.madkit.testing.util.agent.FaultyAA;
import com.distrimind.madkit.testing.util.agent.NormalAA;
import com.distrimind.madkit.testing.util.agent.SelfAbstractKill;
import com.distrimind.madkit.testing.util.agent.TimeOutAA;
import com.distrimind.madkit.testing.util.agent.UnstopableAbstractAgent;

/**
 * @author Fabien Michel
 * @author Jason Mahdjoub
 * @since MaDKit 5.0.0.7
 * @since MadkitLanEdition 1.0
 * @version 1.0
 * 
 */

public class KillAbstractAgentTest extends TestNGMadkit {

	@Test
	public void returnSuccess() {
		addMadkitArgs("--kernelLogLevel", "FINEST");
		launchTest(new AbstractAgent() {
			@Override
			protected void activate() {
				// setLogLevel(Level.ALL);
				NormalAA naa = new NormalAA();
				assertEquals(SUCCESS, launchAgent(naa));
				assertEquals(SUCCESS, killAgent(naa));
			}
		});
	}

	@Test
	public void returnNOT_YET_LAUNCHEDAfterImmediateLaunch() {
		launchTest(new AbstractAgent() {
			@Override
			protected void activate() {
				// setLogLevel(Level.ALL);
				TimeOutAA to = new TimeOutAA(true, true);
				assertEquals(TIMEOUT, launchAgent(to, 0));
				ReturnCode r = killAgent(to);
				assertTrue(r == NOT_YET_LAUNCHED || r == SUCCESS);
			}
		}, true);
	}

	@Test
	public void returnAlreadyKilledAfterCrash() {
		addMadkitArgs("--agentLogLevel", "FINEST");
		launchTest(new AbstractAgent() {
			@Override
			protected void activate() {
				// setLogLevel(Level.ALL);
				FaultyAA f = new FaultyAA(true);
				if (logger != null)
					logger.info("activating");
				assertEquals(AGENT_CRASH, launchAgent(f));
				TestNGMadkit.pause(this, 100);
				assertEquals(ALREADY_KILLED, killAgent(f));
			}
		});
	}

	@Test
	public void returnAlreadyKilled() {
		addMadkitArgs("--agentLogLevel", "FINEST");
		launchTest(new AbstractAgent() {
			@Override
			protected void activate() {
				// setLogLevel(Level.ALL);
				AbstractAgent f = new AbstractAgent();
				if (logger != null)
					logger.info("activating");
				assertEquals(SUCCESS, launchAgent(f));
				assertEquals(SUCCESS, killAgent(f));
				assertEquals(ALREADY_KILLED, killAgent(f));
				assertAgentIsTerminated(f);
			}
		});
	}

	@Test
	public void massKill() {
		// addMadkitArgs(LevelOption.kernelLogLevel.toString(), "ALL");
		launchTest(new AbstractAgent() {
			final ArrayList<AbstractAgent> list = new ArrayList<>(100);

			@Override
			protected void activate() {
				// setLogLevel(Level.ALL);
				startTimer();
				for (int i = 0; i < 100; i++) {
					AbstractAgent t = new AbstractAgent();
					list.add(t);
					assertEquals(SUCCESS, launchAgent(t));
				}
				stopTimer("launch time ");
				startTimer();
				for (AbstractAgent a : list) {
					// killAgent(a,0);
					assertEquals(SUCCESS, killAgent(a, 10));
				}
				stopTimer("kill time ");
			}
		});
	}

	@Test
	public void returnTimeOut() {
		launchTest(new AbstractAgent() {
			@Override
			protected void activate() {
				setLogLevel(Level.ALL);
				TimeOutAA to = new TimeOutAA(true, true);
				assertEquals(TIMEOUT, launchAgent(to, 1));
				while (to.getState() != State.LIVING) {
					TestNGMadkit.pause(this, 100);
				}
				assertEquals(TIMEOUT, killAgent(to, 1));
				assertEquals(ALREADY_KILLED, killAgent(to));
				assertEquals(ALREADY_LAUNCHED, launchAgent(to));
				TestNGMadkit.pause(this, 1000);
			}
		}, true);
	}

	@Test
	public void returnAleradyKilled() {
		addMadkitArgs("--kernelLogLevel", "ALL");
		launchTest(new AbstractAgent() {
			@Override
			protected void activate() {
				NormalAA target = new NormalAA();
				setLogLevel(Level.ALL);
				assertEquals(SUCCESS, launchAgent(target));
				assertEquals(SUCCESS, killAgent(target));
				assertEquals(ALREADY_KILLED, killAgent(target));
			}
		});
	}

	@Test
	public void noTimeoutKill() {
		addMadkitArgs("--agentLogLevel", "ALL");
		launchTest(new AbstractAgent() {
			@Override
			protected void activate() {
				setLogLevel(Level.ALL);
				NormalAA target = new NormalAA() {
					@Override
					public void end() {
						TestNGMadkit.pause(this, 1000);
					}
				};
				assertEquals(SUCCESS, launchAgent(target));
				assertEquals(ReturnCode.TIMEOUT, killAgent(target, 0));
			}
		});
	}

	@Test
	public void returnAgentCrash() {
		addMadkitArgs("--kernelLogLevel", "ALL");
		launchTest(new AbstractAgent() {
			@Override
			protected void activate() {
				setLogLevel(Level.ALL);
				FaultyAA f = new FaultyAA(true);
				assertEquals(AGENT_CRASH, launchAgent(f));
				assertEquals(ALREADY_LAUNCHED, launchAgent(f));
			}
		});
	}

	@Test
	public void selfKill() {
		launchTest(new AbstractAgent() {
			@Override
			protected void activate() {
				setLogLevel(Level.OFF);

				ReturnCode r = launchAgent(new SelfKillAA(true), 1);
				assertTrue(r == SUCCESS || r == AGENT_CRASH);
				AbstractAgent a = new SelfKillAA(false, true);
				assertEquals(SUCCESS, launchAgent(a, 2));
				assertEquals(SUCCESS, killAgent(a, 1));
			}
		});
	}

	@Test
	public void selfKillInActivate() {
		launchTest(new AbstractAgent() {
			@Override
			protected void activate() {
				setLogLevel(Level.OFF);
				assertEquals(SUCCESS, launchAgent(new SelfKillAA(true, true)));
				assertEquals(SUCCESS, launchAgent(new SelfKillAA(true, false)));
			}
		});
	}

	@Test
	public void selfKillInActivateWTO() {
		launchTest(new AbstractAgent() {
			@Override
			protected void activate() {
				setLogLevel(Level.OFF);
				assertEquals(SUCCESS, createGroup(GROUP));
				assertEquals(SUCCESS, launchAgent(new SelfAbstractKill(true, false, 0)));
				assertEquals(SUCCESS, launchAgent(new SelfAbstractKill(true, false, 1)));
				assertEquals(SUCCESS, launchAgent(new SelfAbstractKill(true, true, 1)));
			}
		});
	}

	@Test
	public void selfKilling() {
		launchTest(new AbstractAgent() {
			@Override
			protected void activate() {
				// the time out should not change anything because target == this
				assertEquals(SUCCESS, createGroup(GROUP));
				assertEquals(SUCCESS, launchAgent(new SelfAbstractKill(true, false, 0)));
				assertEquals(SUCCESS, launchAgent(new SelfAbstractKill(true, false, 1)));
				assertEquals(SUCCESS, launchAgent(new SelfAbstractKill(true, false, Integer.MAX_VALUE)));
				assertEquals(SUCCESS, launchAgent(new SelfAbstractKill(true, true, 0)));
				assertEquals(SUCCESS, launchAgent(new SelfAbstractKill(true, true, 1)));
				assertEquals(SUCCESS, launchAgent(new SelfAbstractKill(true, true, Integer.MAX_VALUE)));
				assertEquals(SUCCESS, launchAgent(new SelfAbstractKill(false, true, 0)));
				assertEquals(SUCCESS, launchAgent(new SelfAbstractKill(false, true, 1)));
				assertEquals(SUCCESS, launchAgent(new SelfAbstractKill(false, true, Integer.MAX_VALUE)));
				assertEquals(SUCCESS, launchAgent(new SelfAbstractKill(false, false, 0)));
				assertEquals(SUCCESS, launchAgent(new SelfAbstractKill(false, false, 1)));
				assertEquals(SUCCESS, launchAgent(new SelfAbstractKill(false, false, Integer.MAX_VALUE)));
			}
		});
	}

	@Test
	public void killFaulty() {
		launchTest(new AbstractAgent() {
			@Override
			protected void activate() {
				AbstractAgent a;
				assertEquals(AGENT_CRASH, launchAgent(a = new FaultyAA(true, false)));
				ReturnCode r = killAgent(a);
				assertTrue(r == SUCCESS || r == ALREADY_KILLED);

				assertEquals(TIMEOUT, launchAgent(a = new FaultyAA(true, false), 0));
				TestNGMadkit.pause(this, 100);
				r = killAgent(a);
				assertTrue(r == SUCCESS || r == ALREADY_KILLED);

				assertEquals(TIMEOUT, launchAgent(a = new FaultyAA(true, false), 0));
				TestNGMadkit.pause(this, 200);
				assertEquals(ALREADY_KILLED, killAgent(a));

				// in end
				assertEquals(SUCCESS, launchAgent(a = new FaultyAA(false, true)));
				assertEquals(SUCCESS, killAgent(a));

				assertEquals(SUCCESS, launchAgent(a = new FaultyAA(false, true)));
				assertEquals(SUCCESS, killAgent(a));

				assertEquals(SUCCESS, launchAgent(a = new FaultyAA(false, true)));
				assertEquals(SUCCESS, killAgent(a));

				assertEquals(SUCCESS, launchAgent(a = new FaultyAA(false, true)));
				assertEquals(TIMEOUT, killAgent(a, 0));
				TestNGMadkit.pause(this, 100);// avoid interleaving
				assertEquals(ALREADY_KILLED, killAgent(a));

			}
		});
	}

	@Test
	public void selfKillInActivateAndEnd() {
		launchTest(new AbstractAgent() {
			@Override
			protected void activate() {
				SelfAbstractKill a = new SelfAbstractKill(true, true, 0);
				assertEquals(SUCCESS, launchAgent(a));
				assertAgentIsTerminated(a);
			}
		});
	}

	@Test
	public void selfKillinActivateWithTimeout() {
		launchTest(new AbstractAgent() {
			@Override
			protected void activate() {
				SelfAbstractKill a = new SelfAbstractKill(true, false, 1);
				assertEquals(SUCCESS, launchAgent(a));
				assertEquals(ALREADY_KILLED, killAgent(a));
				assertAgentIsTerminated(a);
			}
		});
	}

	@Test
	public void selfKillinEndAndWaitKill() {
		launchTest(new AbstractAgent() {
			@Override
			protected void activate() {
				assertEquals(SUCCESS, launchAgent(new SelfAbstractKill(false, true, Integer.MAX_VALUE)));
			}
		});
	}

	@Test
	public void cascadeKills() {
		launchTest(new AbstractAgent() {
			@Override
			protected void activate() {
				Killer a = new Killer();
				Killer b = new Killer();
				a.setTarget(b);
				b.setTarget(a);
				launchAgent(a);
				launchAgent(b);
				assertEquals(SUCCESS, killAgent(a));
				assertEquals(ALREADY_KILLED, killAgent(a));
			}
		});
	}

	@Test
	public void returnSuccessAfterLaunchTimeOut() {
		launchTest(new AbstractAgent() {
			@Override
			protected void activate() {
				TimeOutAA to = new TimeOutAA(true, true);
				assertEquals(TIMEOUT, launchAgent(to, 1));
				assertEquals(SUCCESS, killAgent(to));
				assertAgentIsTerminated(to);
			}
		});
	}

	@Test
	public void killUnstopable() {
		launchTest(new AbstractAgent() {
			@Override
			protected void activate() {
				AbstractAgent unstopableAgent = new UnstopableAbstractAgent();
				unstopableAgent.setLogLevel(Level.FINER);
				startTimer();
				assertEquals(TIMEOUT, launchAgent(unstopableAgent, 1));
				stopTimer("launch time out ");
				assertEquals(TIMEOUT, killAgent(unstopableAgent, 1));
				assertAgentIsZombie(unstopableAgent);
			}
		});
	}

	@Test
	public void killUnstopableUsingSelfRef() {
		launchTest(new AbstractAgent() {
			@Override
			protected void activate() {
				AbstractAgent unstopableAgent = new UnstopableAbstractAgent();
				ReturnCode r = launchAgent(unstopableAgent, 2);
				assertTrue(TIMEOUT == r || SUCCESS == r);
				r = unstopableAgent.killAgent(unstopableAgent, 1);
				assertTrue(TIMEOUT == r || SUCCESS == r);
				if (r==SUCCESS)
					assertAgentIsTerminated(unstopableAgent);
				else
					assertAgentIsZombie(unstopableAgent);
				
					
			}
		});
		// printAllStacks();
	}

}

class SelfKillAA extends DoItDuringLifeCycleAbstractAgent {

	public SelfKillAA() {
		super();
	}

	public SelfKillAA(boolean inActivate, boolean inEnd) {
		super(inActivate, inEnd);
	}

	public SelfKillAA(boolean inActivate) {
		super(inActivate);
	}

	@Override
	public void doIt() {
		super.doIt();
		killAgent(this);
	}
}

class Killer extends AbstractAgent {
	AbstractAgent target;

	/**
	 * @param target
	 *            the target to set
	 */
	final void setTarget(AbstractAgent target) {
		this.target = target;
	}

	@Override
	protected void end() {
		killAgent(target);
		killAgent(target);
	}

}
