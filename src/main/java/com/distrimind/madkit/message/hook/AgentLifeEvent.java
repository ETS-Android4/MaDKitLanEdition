/*
 * Copyright or © or Copr. Fabien Michel (1997)
 * 
 * fmichel@lirmm.fr
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
package com.distrimind.madkit.message.hook;

import com.distrimind.madkit.kernel.AbstractAgent;

/**
 * A message which is sent to agents that have requested a hook on
 * {@link HookMessage.AgentActionEvent#AGENT_STARTED} or
 * {@link HookMessage.AgentActionEvent#AGENT_TERMINATED}
 * 
 * @author Fabien Michel
 * @since MadKit 5.0.0.19
 * @version 0.9
 * 
 */
public class AgentLifeEvent extends HookMessage {

	
	final private AbstractAgent source;

	public AgentLifeEvent(AgentActionEvent agentAction, AbstractAgent agent) {
		super(agentAction);
		source = agent;
	}

	/**
	 * The agent which is related to this event
	 * 
	 * @return the source agent of the event
	 */
	public AbstractAgent getSource() {
		return source;
	}


	@Override
	public String toString() {
		return super.toString() + " from " + source;
	}

}
