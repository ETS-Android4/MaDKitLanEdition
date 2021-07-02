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
package com.distrimind.madkit.kernel;

import com.distrimind.madkit.agr.LocalCommunity;
import com.distrimind.madkit.i18n.ErrorMessages;
import com.distrimind.madkit.i18n.I18nUtilities;
import com.distrimind.madkit.kernel.AbstractAgent.ReturnCode;
import com.distrimind.madkit.kernel.network.connection.access.ListGroupsRoles;
import com.distrimind.madkit.message.ObjectMessage;
import com.distrimind.util.io.SecureExternalizable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.distrimind.madkit.kernel.AbstractAgent.ReturnCode.*;

/**
 * @author Oliver Gutknecht
 * @author Fabien Michel since v.3
 * @author Jason Mahdjoub
 * @version 5.1
 * @since MaDKitLanEdition 1.0
 * 
 */
final class InternalGroup extends ConcurrentHashMap<String, InternalRole> {

	private static final long serialVersionUID = 498214902172237862L;
	// private AbstractAgent manager;
	// private final AtomicReference<AgentAddress> manager;
	private final Gatekeeper gatekeeper;
	private final Logger logger;
	private final Group group;

	private final Organization communityObject;
	private final boolean isSecured;

	private final boolean distributed;
	private final boolean manually_created;
	private final Map<String, Object> board = new HashMap<>();

	InternalGroup(Group _group, AbstractAgent creator, Organization organization, boolean manually_created) {
		if (_group.isUsedSubGroups())
			throw new IllegalAccessError();
		gatekeeper = _group.getGateKeeper();
		distributed = _group.isDistributed();
		isSecured = gatekeeper != null;
		group = _group;
		communityObject = organization;
		logger = communityObject.getLogger();
		this.manually_created = manually_created;
		put(com.distrimind.madkit.agr.Organization.GROUP_MANAGER_ROLE, new ManagerRole(this, creator, isSecured));
	}

	/**
	 * @return the communityObject
	 */
	final Organization getCommunityObject() {
		return communityObject;
	}

	boolean containsDistantAgents()
	{
		return values().stream().anyMatch(InternalRole::containsDistantAgents);
	}

	void clearLocalAgents()
	{
		entrySet().removeIf(e -> !e.getValue().clearLocalAgents());
	}


	InternalGroup(Group _group, AgentAddress manager, Organization communityObject, boolean manually_created) {
		// manager = creator;
		distributed = true;
		this.communityObject = communityObject;
		logger = communityObject.getLogger();
		if (manager instanceof GroupManagerAddress) {
			isSecured = ((GroupManagerAddress) manager).isGroupSecured();
		} else {
			isSecured = false;
		}
		gatekeeper = null;
		group = _group;
		this.manually_created = manually_created;
		put(com.distrimind.madkit.agr.Organization.GROUP_MANAGER_ROLE, new ManagerRole(this, manager));
	}

	boolean isSecured() {
		return isSecured;
	}

	/*
	 * String getName() { return group.getPath(); }
	 */

	Group getGroup() {
		return group;
	}


	ReturnCode requestRole(final AbstractAgent requester, final String roleName, final SecureExternalizable memberCard,
			boolean manually_requested) {
		if (roleName == null)
			throw new NullPointerException(ErrorMessages.R_NULL.toString());
		if (isSecured) {
			final MadkitKernel myKernel = getCommunityObject().getMyKernel();
			if (gatekeeper == null) {
				final AgentAddress manager = myKernel.getAgentWithRole(group,
						com.distrimind.madkit.agr.Organization.GROUP_MANAGER_ROLE);
				final AgentAddress distantAgentWithRole = myKernel.getDistantAgentWithRole(requester,
						LocalCommunity.Groups.KERNELS, com.distrimind.madkit.agr.Organization.GROUP_MANAGER_ROLE,
						manager.getKernelAddress());

				MicroAgent<Boolean> ma;
				myKernel.launchAgent(ma = new MicroAgent<Boolean>() {
					@SuppressWarnings("unchecked")
					protected void activate() {
						super.activate();
						try {
							Message m = sendMessageAndWaitForReply(distantAgentWithRole,
									new RequestRoleSecure(requester.getClass(),
											myKernel.getSenderAgentAddress(requester, manager, null), roleName,
											memberCard),
									10000);
							if (m != null) {
								setResult(((ObjectMessage<Boolean>) m).getContent());
							} else {
								setResult(null);
							}
						} catch (CGRNotAvailable | InterruptedException e) {
							e.printStackTrace();
						}
					}
				});
				if (!ma.getResult())
					return ACCESS_DENIED;
			} else {
				try {
					if (!gatekeeper.allowAgentToTakeRole(group, roleName, requester.getClass(),
							requester.getNetworkID(), memberCard)) {
						return ACCESS_DENIED;
					}
				} catch (Exception e) {
					e.printStackTrace();
					if (logger != null)
						logger.log(Level.SEVERE, "Error into Gatekeeper " + gatekeeper, e);
					return ReturnCode.AGENT_CRASH;
				}
			}
		}
		// TODO there is another RC : manager role is already handled
		synchronized (this) {
			final InternalRole r = getOrCreateRole(roleName);
			if (r.addMember(requester, manually_requested)) {
				// now trigger overlooker updates if needed. Note that the role always still
				// exits here because requester is in
				r.addToOverlookers(requester);
				return SUCCESS;
			}
			return ROLE_ALREADY_HANDLED;
		}
	}

	/**
	 * @return the gatekeeper
	 */
	Gatekeeper getGatekeeper() {
		return gatekeeper;
	}

	InternalRole createRole(final String roleName) {
		final InternalRole r = new InternalRole(this, roleName);
		put(roleName, r);
		r.initializeOverlookers();
		return r;
	}

	void removeRole(String roleName) {
		synchronized (this) {
			remove(roleName);
			if (logger != null)
				logger.finer("Removing" + I18nUtilities.getCGRString(group, roleName));
			checkEmptyness();
		}
	}

	private void checkEmptyness() {
		if (isEmpty() || (!manually_created && size() == 1
				&& keys().nextElement().equals(com.distrimind.madkit.agr.Organization.GROUP_MANAGER_ROLE))) {
			communityObject.removeGroup(group);
		}
	}


	List<InternalRole> leaveGroup(final AbstractAgent requester, boolean manually_requested) {
		List<InternalRole> affectedRoles = null;
		synchronized (this) {
			for (final InternalRole r : values()) {
				/*
				 * boolean remove=true; if (manually_requested)
				 * remove=!this.getCommunityObject().getMyKernel().isConcernedByAutoRequestRole(
				 * requester, r.getGroup(), r.getRoleName());
				 */
				if (r.removeMember(requester, manually_requested) == SUCCESS) {
					if (affectedRoles == null)
						affectedRoles = new ArrayList<>();
					affectedRoles.add(r);
				}
			}

			

		}
		return affectedRoles;
	}

	void removeDistantKernelAddressForAllRoles(KernelAddress ka)
	{
		synchronized (this) {
			for (final InternalRole r : values()) {
				r.removeDistantMembers(ka);

			}
		}
	}

	void removeDistantKernelAddressForAllRolesThatDoesNotAcceptDistantRoles(KernelAddress ka, ListGroupsRoles listGroupsRoles)
	{
		synchronized (this) {
			for (final InternalRole r : values()) {
				if (!listGroupsRoles.isDistantRoleAcceptable(getGroup(), r.getRoleName()))
					r.removeDistantMembers(ka);

			}
		}
	}

	boolean isIn(AbstractAgent agent) {
		for (final InternalRole r : values()) {
			if (r.contains(agent))
				return true;
		}
		return false;
	}

	// /**
	// * @param roleName
	// * @return
	// * @throws getAgentWithRoleWarning
	// */
	// List<AgentAddress> getRolePlayers(final String roleName) throws
	// CGRException {
	// try {
	// return get(roleName).getAgentAddresses();
	// } catch (NullPointerException e) {
	// throw new CGRException(NOT_ROLE, printCGR(communityName,groupName,
	// roleName),e);
	// }
	// }

	AgentAddress getAgentAddressOf(AbstractAgent abstractAgent) {
		for (final InternalRole r : values()) {
			final AgentAddress aa = r.getAgentAddressOf(abstractAgent);
			if (aa != null)
				return aa;
		}
		return null;
	}

	AgentAddress getAgentAddressInGroupOrInParentGroups(AbstractAgent abstractAgent) {
		for (final InternalRole r : values()) {
			final AgentAddress aa = r.getAgentAddressOf(abstractAgent);
			if (aa != null)
				return aa;
		}
		if (!group.equals(AbstractGroup.getRootGroup(group.getCommunity()))) {
			Group parent = group.getParent();
			if (parent == null)
				return null;
			InternalGroup ig = communityObject.get(parent);
			if (ig != null)
				return ig.getAgentAddressInGroupOrInParentGroups(abstractAgent);
		}
		return null;
	}


	AgentAddress getAgentAddressInGroupOrInParentGroupsWithRole(AbstractAgent abstractAgent, String role) {

		final InternalRole r = get(role);
		if (r != null) {
			final AgentAddress aa = r.getAgentAddressOf(abstractAgent);
			if (aa != null)
				return aa;
		}

		if (!group.equals(AbstractGroup.getRootGroup(group.getCommunity())) && group.getParent() != null) {
			InternalGroup ig = communityObject.get(group.getParent());
			if (ig != null)
				return ig.getAgentAddressInGroupOrInParentGroups(abstractAgent);
		}
		return null;
	}

	// boolean empty(){
	// return super.isEmpty() && manager.get() == null;
	// }

	/**
	 * @return the distributed
	 */
	boolean isDistributed() {
		return distributed;
	}

	boolean hasRoleWith(AbstractAgent a) {
		for (InternalRole ir : this.values()) {
			if (ir.contains(a)) {
				return true;
			}
		}
		return false;
	}

	Map<String, Collection<AgentAddress>> getLocalGroupMap(ListGroupsRoles distantGroupsRoles) {
		final Map<String, Collection<AgentAddress>> export = new TreeMap<>();
		for (final Map.Entry<String, InternalRole> org : entrySet()) {
			if (distantGroupsRoles.isDistantRoleAcceptable(getGroup(), org.getValue().getRoleName()))
				export.put(org.getKey(), org.getValue().buildAndGetLocalAddresses());
		}
		return export;
	}
	Map<String, Collection<AgentAddress>> getLocalGroupMap() {
		final Map<String, Collection<AgentAddress>> export = new TreeMap<>();
		for (final Map.Entry<String, InternalRole> org : entrySet()) {
			export.put(org.getKey(), org.getValue().buildAndGetLocalAddresses());
		}
		return export;
	}

	Map<String, Collection<AgentAddress>> getGroupMap() {
		final Map<String, Collection<AgentAddress>> export = new TreeMap<>();
		for (final Map.Entry<String, InternalRole> org : entrySet()) {
			export.put(org.getKey(), org.getValue().buildAndGetAddresses());
		}
		return export;
	}


	void importDistantOrg(final Map<String, Collection<AgentAddress>> map, MadkitKernel madkitKernel) {
		for (final String roleName : map.keySet()) {
			final Collection<AgentAddress> list = map.get(roleName);
			if (list == null)
				continue;
			synchronized (this) {
				getOrCreateRole(roleName).importDistantOrg(list, madkitKernel);
			}
		}
	}

	boolean addDistantMember(AgentAddress content) {
		final String roleName = content.getRole();
		final InternalRole r;
		synchronized (this) {
			r = getOrCreateRole(roleName);
		}
		return r.addDistantMemberIfNecessary(content);
	}

	InternalRole getOrCreateRole(final String roleName) {
		InternalRole r = get(roleName);
		if (r == null)
			return createRole(roleName);
		return r;
	}


	boolean removeDistantMember(final AgentAddress aa, boolean manually_requested) {
		// boolean in = false;
		boolean changed=false;

		for (final InternalRole r : values()) {
			aa.setRoleObject(r);// required for equals to work
			changed|=r.removeDistantMember(aa, manually_requested);
		}
		aa.setRoleObject(null);
		// if (manager.get().equals(aa)){
		// manager.set(null);
		// in = true;
		// }
		// if(in){
		// if(isEmpty()){
		// communityObject.removeGroup(groupName);
		// }
		// }
		return changed;
	}

	void removeAgentsFromDistantKernel(final KernelAddress kernelAddress, MadkitKernel madkitKernel) {
		if (logger != null)
			logger.finest("Removing all agents from distant kernel " + kernelAddress + " in" + this);
		for (final InternalRole r : values()) {
			r.removeAgentsFromDistantKernel(kernelAddress, madkitKernel);
		}
	}

	/**
	 * @param oldManager
	 *            the last manager
	 * 
	 */
	void chooseNewManager(AbstractAgent oldManager) {
		synchronized (this) {
			if (!isEmpty()) {
				for (final InternalRole r : values()) {
                    for (InternalRole.ParametrizedAgent a : r.getPlayers()) {
                        if (a.agent != oldManager) {
                            put(com.distrimind.madkit.agr.Organization.GROUP_MANAGER_ROLE,
                                    new ManagerRole(this, a.agent, false));
                            return;
                        }
                    }
				}
			}
		}
	}

	@Override
	public String toString() {
		return I18nUtilities.getCGRString(group) + values();
	}

	final void destroy() {
		for (InternalRole r : values()) {
			r.destroy();
		}
		communityObject.removeGroup(group);
	}

	final Object weakSetBoard(AbstractAgent requester, String name, Object data) throws CGRNotAvailable {
		if (!isIn(requester))
			throw new CGRNotAvailable(NOT_IN_GROUP);
		synchronized (board) {
			Object res = board.get(name);
			if (res == null) {
				board.put(name, data);
				return data;
			} else
				return res;

		}
	}

	final Object setBoard(AbstractAgent requester, String name, Object data) throws CGRNotAvailable {
		if (!isIn(requester))
			throw new CGRNotAvailable(NOT_IN_GROUP);
		synchronized (board) {
			return board.put(name, data);
		}
	}

	final Object getBoard(AbstractAgent requester, String name) throws CGRNotAvailable {
		if (!isIn(requester))
			throw new CGRNotAvailable(NOT_IN_GROUP);
		synchronized (board) {
			return board.get(name);
		}
	}

	final Object removeBoard(AbstractAgent requester, String name) throws CGRNotAvailable {
		if (!isIn(requester))
			throw new CGRNotAvailable(NOT_IN_GROUP);
		synchronized (board) {
			return board.remove(name);
		}
	}

}