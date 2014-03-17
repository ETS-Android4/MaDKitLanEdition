/*
 * Copyright 1997-2012 Fabien Michel, Olivier Gutknecht, Jacques Ferber
 * 
 * This file is part of MaDKit.
 * 
 * MaDKit is free software: you can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free
 * Software Foundation, either version 3 of the License, or (at your option) any
 * later version.
 * 
 * MaDKit is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with MaDKit. If not, see <http://www.gnu.org/licenses/>.
 */
package madkit.testing.util.agent;

import static madkit.kernel.JunitMadkit.COMMUNITY;
import static madkit.kernel.JunitMadkit.GROUP;
import static madkit.kernel.JunitMadkit.ROLE;
import madkit.kernel.Message;

/**
 * @author Fabien Michel
 * @since MaDKit 5.0.0.6
 * @version 0.9
 * 
 */
@SuppressWarnings("serial")
public class ForEverReplierAgent extends NormalAgent {
	
	
	private Class <? extends Message>	type;

	public ForEverReplierAgent() {
		createGUIOnStartUp();
	}

	public ForEverReplierAgent(Class<? extends Message> messageType) {
		type = messageType;
	}

	@Override
	protected void activate() {
		super.activate();
		sendMessage(COMMUNITY, GROUP, ROLE, getNewMessage());
	}

	/**
	 * @return
	 * @throws InstantiationException
	 * @throws IllegalAccessException
	 */
	private Message getNewMessage() {
		try {
			return type == null ? new Message() : type.newInstance();
		} catch (InstantiationException | IllegalAccessException e) {
			e.printStackTrace();
		}
		return null;
	}

	@Override
	protected void live() {
		while (true) {
			sendReply(waitNextMessage(), getNewMessage());
		}
	}
}
