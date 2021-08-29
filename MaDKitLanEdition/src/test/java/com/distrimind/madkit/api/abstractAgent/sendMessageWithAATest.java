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
import static org.testng.AssertJUnit.assertNotNull;
import org.testng.annotations.Test;
import static com.distrimind.madkit.kernel.AbstractAgent.ReturnCode.INVALID_AGENT_ADDRESS;
import static com.distrimind.madkit.kernel.AbstractAgent.ReturnCode.NOT_IN_GROUP;
import static com.distrimind.madkit.kernel.AbstractAgent.ReturnCode.ROLE_NOT_HANDLED;
import static com.distrimind.madkit.kernel.AbstractAgent.ReturnCode.SUCCESS;
import com.distrimind.madkit.agr.Organization;
import com.distrimind.madkit.kernel.AbstractAgent;
import com.distrimind.madkit.kernel.AgentAddress;
import com.distrimind.madkit.kernel.JunitMadkit;
import com.distrimind.madkit.kernel.Message;
import com.distrimind.madkit.kernel.AbstractAgent.ReturnCode;

/**
 * @author Fabien Michel
 * @author Jason Mahdjoub
 * @since MaDKit 5.0.0.7
 * @since MadkitLanEdition 1.0
 * @version 1.0
 * 
 */

public class sendMessageWithAATest extends JunitMadkit {

	final AbstractAgent target = new AbstractAgent() {
		@Override
		protected void activate() {
			assertEquals(SUCCESS, createGroup(GROUP));
			assertEquals(SUCCESS, requestRole(GROUP, ROLE));
		}
	};

	@Test
	public void returnSuccess() {
		launchTest(new AbstractAgent() {
			@Override
			protected void activate() {
				assertEquals(SUCCESS, launchAgent(target));
				assertEquals(SUCCESS, requestRole(GROUP, ROLE));

				AgentAddress aa = getAgentWithRole(GROUP, ROLE);
				assertNotNull(aa);

				// Without role
				assertEquals(SUCCESS, sendMessage(aa, new Message()));
				Message m = target.nextMessage();
				assertNotNull(m);
				assertEquals(ROLE, m.getReceiver().getRole());

				// With role
				assertEquals(SUCCESS, sendMessageWithRole(aa, new Message(), ROLE));
				m = target.nextMessage();
				assertNotNull(m);
				assertEquals(ROLE, m.getReceiver().getRole());
			}
		});
	}

	@Test
	public void returnSuccessOnCandidateRole() {
		launchTest(new AbstractAgent() {
			@Override
			protected void activate() {
				assertEquals(SUCCESS, launchAgent(target));

				// Without role
				AgentAddress aa = getAgentWithRole(GROUP, Organization.GROUP_MANAGER_ROLE);
				assertNotNull(aa);
				assertEquals(SUCCESS, sendMessage(aa, new Message()));
				Message m = target.nextMessage();
				assertNotNull(m);
				assertEquals(Organization.GROUP_MANAGER_ROLE, m.getReceiver().getRole());
				assertEquals(Organization.GROUP_CANDIDATE_ROLE, m.getSender().getRole());

				// With role
				aa = getAgentWithRole(GROUP, Organization.GROUP_MANAGER_ROLE);
				assertNotNull(aa);
				assertEquals(SUCCESS, sendMessageWithRole(aa, new Message(), Organization.GROUP_CANDIDATE_ROLE));
				m = target.nextMessage();
				assertNotNull(m);
				assertEquals(Organization.GROUP_MANAGER_ROLE, m.getReceiver().getRole());
				assertEquals(Organization.GROUP_CANDIDATE_ROLE, m.getSender().getRole());
			}
		});
	}

	@Test
	public void returnInvalidAA() {
		launchTest(new AbstractAgent() {
			@Override
			protected void activate() {
				assertEquals(SUCCESS, launchAgent(target));
				assertEquals(SUCCESS, requestRole(GROUP, ROLE));
				AgentAddress aa = getAgentWithRole(GROUP, ROLE);
				assertEquals(SUCCESS, target.leaveRole(GROUP, ROLE));
				assertEquals(INVALID_AGENT_ADDRESS, sendMessage(aa, new Message()));

				// With role
				assertEquals(INVALID_AGENT_ADDRESS, sendMessageWithRole(aa, new Message(), ROLE));
			}
		});
	}

	@Test
	public void returnNotInGroup() {
		launchTest(new AbstractAgent() {
			@Override
			protected void activate() {
				assertEquals(SUCCESS, launchAgent(target));
				AgentAddress aa = getAgentWithRole(GROUP, ROLE);
				assertEquals(NOT_IN_GROUP, sendMessageWithRole(aa, new Message(), ROLE));
				assertEquals(SUCCESS, target.leaveRole(GROUP, ROLE));
				assertEquals(INVALID_AGENT_ADDRESS, sendMessage(aa, new Message()));
				assertEquals(ReturnCode.NOT_ROLE, sendMessage(GROUP, ROLE, new Message()));

				// With role
			}
		});
	}

	@Test
	public void returnRoleNotHandled() {
		launchTest(new AbstractAgent() {
			@Override
			protected void activate() {
				assertEquals(SUCCESS, launchAgent(target));
				assertEquals(SUCCESS, requestRole(GROUP, ROLE));

				AgentAddress aa = getAgentWithRole(GROUP, ROLE);
				assertEquals(ROLE_NOT_HANDLED, sendMessageWithRole(aa, new Message(), aa()));

			}
		});
	}

	@Test
	public void nullArgs() {
		launchTest(new AbstractAgent() {
			@Override
			protected void activate() {
                sendMessage(null, null);
                noExceptionFailure();
            }
		}, ReturnCode.AGENT_CRASH);
	}

	@Test
	public void nullAA() {
		launchTest(new AbstractAgent() {
			@Override
			protected void activate() {
                sendMessage(null, new Message());
                noExceptionFailure();
            }
		}, ReturnCode.AGENT_CRASH);
	}

	@Test
	public void nullMessage() {
		launchTest(new AbstractAgent() {
			@Override
			protected void activate() {
				assertEquals(SUCCESS, launchAgent(target));
				assertEquals(SUCCESS, requestRole(GROUP, ROLE));
				AgentAddress aa = getAgentWithRole(GROUP, ROLE);
                sendMessage(aa, null);
                noExceptionFailure();
            }
		}, ReturnCode.AGENT_CRASH);
	}

}
