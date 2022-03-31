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

import static org.testng.AssertJUnit.assertFalse;
import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.assertTrue;
import static org.testng.AssertJUnit.assertNull;
import org.testng.annotations.Test;
import org.testng.AssertJUnit;
import com.distrimind.madkit.kernel.AbstractAgent;
import com.distrimind.madkit.kernel.AgentAddress;
import com.distrimind.madkit.kernel.Group;
import com.distrimind.madkit.kernel.TestNGMadkit;
import com.distrimind.madkit.kernel.AbstractAgent.ReturnCode;

/**
 * @author Fabien Michel
 * @author Jason Mahdjoub
 * @since MaDKit 5.0.0.15
 * @since MadkitLanEdition 1.0
 * @version 1.0
 * 
 */

public class GetAgentAddressInTest extends TestNGMadkit {

	@Test
	public void success() {
		launchTest(new AbstractAgent() {
			@Override
			protected void activate() {
				createDefaultCGR(this);
				assertNotNull(getAgentAddressIn(GROUP, ROLE));
			}
		});
	}

	@Test
	public void nullAfterLeaveRole() {
		launchTest(new AbstractAgent() {
			@Override
			protected void activate() {
				createDefaultCGR(this);
				AgentAddress aa = getAgentAddressIn(GROUP, ROLE);
				assertNotNull(aa);
				assertTrue(checkAgentAddress(aa));
				AssertJUnit.assertEquals(ReturnCode.SUCCESS, leaveRole(GROUP, ROLE));
				assertFalse(checkAgentAddress(aa));
				aa = getAgentAddressIn(GROUP, ROLE);
				assertNull(aa);
			}
		});
	}

	@Test
	public void nullAfterLeaveGroup() {
		launchTest(new AbstractAgent() {
			@Override
			protected void activate() {
				createDefaultCGR(this);
				AgentAddress aa = getAgentAddressIn(GROUP, ROLE);
				assertNotNull(aa);
				assertTrue(checkAgentAddress(aa));
				leaveGroup(GROUP);
				assertFalse(checkAgentAddress(aa));
				aa = getAgentAddressIn(GROUP, ROLE);
				assertNull(aa);
			}
		});
	}

	@Test
	public void nullCommunity() {
		launchTest(new AbstractAgent() {
			@Override
			protected void activate() {
				createDefaultCGR(this);
                assertNotNull(getAgentAddressIn(new Group(null, G), ROLE));
                noExceptionFailure();
            }
		}, ReturnCode.AGENT_CRASH);
	}

	@Test
	public void nullGroup() {
		launchTest(new AbstractAgent() {
			@Override
			protected void activate() {
				createDefaultCGR(this);
				//noinspection ConstantConditions
				assertNotNull(getAgentAddressIn(null, ROLE));
                noExceptionFailure();
            }
		}, ReturnCode.AGENT_CRASH);
	}

	@Test
	public void nullRole() {
		launchTest(new AbstractAgent() {
			@Override
			protected void activate() {
				createDefaultCGR(this);
                assertNotNull(getAgentAddressIn(GROUP, null));
                noExceptionFailure();
            }
		}, ReturnCode.AGENT_CRASH);
	}

	@Test
	public void roleNotExist() {
		launchTest(new AbstractAgent() {
			@Override
			protected void activate() {
				createDefaultCGR(this);
				assertNull(getAgentAddressIn(GROUP, aa()));
			}
		}, ReturnCode.SUCCESS);
	}

	@Test
	public void roleNotHandled() {
		launchTest(new AbstractAgent() {
			@Override
			protected void activate() {
				createDefaultCGR(this);
				launchAgent(new AbstractAgent() {
					@Override
					protected void activate() {
						requestRole(GROUP, "a");
						createGroup(new Group(C, "a"));
						requestRole(new Group(C, "a"), "a");
					}
				});
				assertNull(getAgentAddressIn(GROUP, "a"));
				assertNull(getAgentAddressIn(new Group(C, "a"), "a"));
			}
		}, ReturnCode.SUCCESS);
	}

	@Test
	public void groupNotExist() {
		launchTest(new AbstractAgent() {
			@Override
			protected void activate() {
				createDefaultCGR(this);
				assertNull(getAgentAddressIn(new Group(C, aa()), aa()));
			}
		}, ReturnCode.SUCCESS);
	}

	@Test
	public void communityNotExist() {
		launchTest(new AbstractAgent() {
			@Override
			protected void activate() {
				createDefaultCGR(this);
				assertNull(getAgentAddressIn(new Group(aa(), aa()), aa()));
			}
		}, ReturnCode.SUCCESS);
	}

}
