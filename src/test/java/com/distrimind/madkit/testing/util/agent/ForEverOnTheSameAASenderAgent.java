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
package com.distrimind.madkit.testing.util.agent;

import com.distrimind.madkit.kernel.Agent;
import com.distrimind.madkit.kernel.AgentAddress;
import com.distrimind.madkit.kernel.Message;
import com.distrimind.madkit.message.StringMessage;

import static com.distrimind.madkit.kernel.TestNGMadkit.GROUP;
import static com.distrimind.madkit.kernel.TestNGMadkit.ROLE;

/**
 * @author Fabien Michel
 * @author Jason Mahdjoub
 * @since MadkitLanEdition 1.0
 * @version 1.0
 * 
 */
public class ForEverOnTheSameAASenderAgent extends Agent {
	private boolean first = true;
	private AgentAddress aa;
	private int cycle = 10;
	private long waitDuration = 0;

	public ForEverOnTheSameAASenderAgent() {

	}

	public ForEverOnTheSameAASenderAgent(int cycle, long waitDurationAfterRequestingRole) {
		this.cycle = cycle;
		this.waitDuration = waitDurationAfterRequestingRole;
	}

	@Override
	protected void activate() throws InterruptedException {

		requestRole(GROUP, ROLE);
		if (waitDuration > 0)
			sleep(waitDuration);
		sendMessage(GROUP, ROLE, new StringMessage("empty message"));
	}

	@Override
	protected void liveCycle() throws InterruptedException {
		if (first) {
			Message m = waitNextMessage(3000);
			aa = m.getSender();
			if (aa != null) {
				sendMessage(aa, new StringMessage("test message"));
				first = false;
			} else
				this.killAgent(this);
		}

		if (waitNextMessage(3000) == null)
			this.killAgent(this);
		sendMessage(aa, new StringMessage("test message"));
		if (cycle-- == 0)
			this.killAgent(this);
	}
}
