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

import static com.distrimind.madkit.i18n.I18nUtilities.getCGRString;
import static com.distrimind.madkit.kernel.AbstractAgent.ReturnCode.ROLE_NOT_HANDLED;
import static com.distrimind.madkit.kernel.AbstractAgent.ReturnCode.SUCCESS;
import static com.distrimind.madkit.kernel.CGRSynchro.Code.LEAVE_ROLE;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.distrimind.madkit.agr.Organization;
import com.distrimind.madkit.kernel.AbstractAgent.ReturnCode;
import com.distrimind.madkit.message.hook.HookMessage.AgentActionEvent;
import com.distrimind.madkit.util.ExternalizableAndSizable;
import com.distrimind.madkit.util.SerializationTools;

/**
 * /** Reifying the notion of Role in AGR
 * 
 * @author Fabien Michel
 * @author Jason Mahdjoub
 * @since MaDKitLanEdition 1.0
 * @version 5.2
 * 
 */
@SuppressWarnings({"SynchronizeOnNonFinalField", "ExternalizableWithoutPublicNoArgConstructor", "SameParameterValue"})
class InternalRole implements ExternalizableAndSizable {// TODO test with arraylist

	private static final long serialVersionUID = 4447153943733812916L;

	protected transient Collection<AbstractAgent> players;//=new HashSet<>();// TODO test copyonarraylist and linkedhashset
	private transient List<AbstractAgent> tmpReferenceableAgents;
	protected volatile transient List<AgentAddress> agentAddresses;
    protected transient Collection<AgentAddress> distantAgentAddresses;
	protected volatile transient boolean modified = true;
	private transient AtomicReference<Set<Overlooker<? extends AbstractAgent>>> overlookers = new AtomicReference<>(
			null);
	protected transient InternalGroup myGroup;
	transient private Logger logger;
	private transient KernelAddress kernelAddress;
	private transient AtomicLong number_of_manually_requested_role;
	private transient HashMap<KernelAddress, AtomicInteger> number_of_manually_distant_requested_role;

	private Group group;
	private String roleName;
	InternalRole()
	{
		
	}
	/**
	 * @return the kernelAddress
	 */
	KernelAddress getKernelAddress() {
		return kernelAddress;
	}
	
	@Override
	public void writeExternal(ObjectOutput oos) throws IOException {
		SerializationTools.writeExternalizableAndSizable(oos, group, false);
		SerializationTools.writeString(oos, roleName, Group.MAX_ROLE_NAME_LENGTH, true);
	}
	@Override
	public void readExternal(ObjectInput ois) throws IOException, ClassNotFoundException {
		Object o=SerializationTools.readExternalizableAndSizable(ois, false);
		if (o instanceof Group)
		{
			group=(Group)o;
			roleName=SerializationTools.readString(ois, Group.MAX_ROLE_NAME_LENGTH, true);
		}
		else
			throw new IOException();
	}
	
	@Override
	public int getInternalSerializedSize() {
		
		return group.getInternalSerializedSize()+2+(roleName==null?0:roleName.length()*2);
	}
	InternalRole(final InternalGroup groupObject, final String roleName) {
		players = new ArrayList<>();
		distantAgentAddresses=new ArrayList<>();
		tmpReferenceableAgents = null;
		group = groupObject.getGroup();
		this.roleName = roleName;
		final MadkitKernel k = groupObject.getCommunityObject().getMyKernel();
		logger = groupObject.getCommunityObject().getLogger();
		myGroup = groupObject;
		kernelAddress = k.getKernelAddress();
		number_of_manually_requested_role = new AtomicLong(0);
		number_of_manually_distant_requested_role = new HashMap<>();
		if (logger != null) {
			// logger.setLevel(Level.ALL);
			logger.finer(toString() + " created");
		}
		overlookers.set(new LinkedHashSet<Overlooker<? extends AbstractAgent>>());

	}

	// @Override
	// public boolean equals(Object obj) { //override should not be required
	// if(this == obj)
	// return true;
	// Role other = (Role) obj;
	// return communityName.equals(other.communityName) &&
	// groupName.equals(other.groupName) &&
	// roleName.equals(other.roleName);
	// }

	synchronized void initializeOverlookers() {
		for (final Overlooker<? extends AbstractAgent> o : myGroup.getCommunityObject().getMyKernel()
				.getOperatingOverlookers()) {
			if (o.isConcernedBy(group, roleName)) {
				addOverlooker(o);
				o.internalRoleInitialized(this);
			}
		}
	}
	
	private synchronized void removeOverlookers()
	{
		for (final Overlooker<? extends AbstractAgent> o : myGroup.getCommunityObject().getMyKernel()
				.getOperatingOverlookers()) {
			if (o.isConcernedBy(group, roleName)) {
				o.internalRoleRemoved(this);
			}
			overlookers.set(new LinkedHashSet<Overlooker<? extends AbstractAgent>>());
		}
	}

	private AtomicInteger getReference(KernelAddress distant) {
		if (distant == null)
			return null;
		AtomicInteger v = number_of_manually_distant_requested_role.get(distant);
		if (v == null) {
			v = new AtomicInteger(0);
			number_of_manually_distant_requested_role.put(distant, v);
		}
		return v;
	}

	private void incrementReferences(KernelAddress distant) {
		long number = number_of_manually_requested_role.incrementAndGet();
		if (distant != null) {
			synchronized (players) {
				getReference(distant).incrementAndGet();
			}
		}
		if (number == 1) {

			group.incrementMadKitReferences(kernelAddress);
		}
	}

	private void updateReferences(KernelAddress distant, int n) {
		long number = number_of_manually_requested_role.addAndGet(n);
		if (distant != null) {
			synchronized (players) {
				int v = getReference(distant).addAndGet(n);
				if (v < 0)
					logger.log(Level.WARNING,
							"Incoherent reference v (v<0) : " + number + " for kernel address " + distant,
							new IllegalAccessError());
				if (v <= 0)
					number_of_manually_distant_requested_role.remove(distant);
			}
		}

		if (n > 0) {
			if (number == n) {
				group.incrementMadKitReferences(kernelAddress);
			}
		} else if (n < 0) {
			if (number <= 0) {
				group.decrementMadKitReferences(kernelAddress);
			}
			if (number < 0)
				logger.log(Level.WARNING, "Incoherent reference number (number<0) : " + number,
						new IllegalAccessError());
		}
	}

	private void decrementReferences(KernelAddress distant) {
		long number = 1;
		if (!this.roleName.equals(Organization.GROUP_MANAGER_ROLE))
			number = number_of_manually_requested_role.decrementAndGet();

		if (distant != null) {
			synchronized (players) {
				int v = getReference(distant).decrementAndGet();
				if (v < 0)
					logger.log(Level.WARNING,
							"Incoherent reference v (v<0) : " + number + " for kernel address " + distant,
							new IllegalAccessError());
				if (v <= 0)
					number_of_manually_distant_requested_role.remove(distant);
			}
		}
		if (number == 0) {
			group.decrementMadKitReferences(kernelAddress);
		}
		if (number < 0) {
			throw new IllegalAccessError("" + this);
			// logger.log(Level.SEVERE, "Incoherent reference number (number<0) : "+number,
			// new IllegalAccessError());
		}
	}

	InternalRole(Group _group) {
		group = _group;
		roleName = null;
		players = null;
		distantAgentAddresses=null;
		overlookers.set(null);
		myGroup = null;
		logger = null;
		kernelAddress = null;
		number_of_manually_requested_role = null;
		number_of_manually_distant_requested_role = null;
	}

	/**
	 * @return the players
	 */
	Collection<AbstractAgent> getPlayers() {
		return players;
	}

	/**
	 * @return the myGroup
	 */
	InternalGroup getMyGroup() {
		return myGroup;
	}

	/**
	 * @return the groupName
	 */
	final Group getGroup() {
		return group;
	}

	final void addOverlooker(final Overlooker<? extends AbstractAgent> o) {
		synchronized (overlookers) {
			Set<Overlooker<? extends AbstractAgent>> co = new LinkedHashSet<>(overlookers.get());
			co.add(o);
			overlookers.set(co);
		}
	}

	final void removeOverlooker(final Overlooker<? extends AbstractAgent> o) {
		synchronized (overlookers) {
			Set<Overlooker<? extends AbstractAgent>> co = new LinkedHashSet<>(overlookers.get());
			co.remove(o);
			overlookers.set(co);
		}

	}

	/**
	 * @return the roleName
	 */
	final String getRoleName() {
		return roleName;
	}

	@Override
	public String toString() {
		return getCGRString(group, roleName);
	}


	boolean addMember(final AbstractAgent requester, boolean manually_requested) {
		synchronized (players) {
			for (AbstractAgent aa : players)// TODO looks like I should use linkedhashset
				if (aa==requester)
					return false;
			
			players.add(requester);
			if (logger != null) {
				logger.finest(requester.getName() + " is now playing " + getCGRString(group, roleName));
			}
			// System.err.println(requester.getName() + " is now playing " +
			// getCGRString(communityName, groupName, roleName));
			// System.err.println(this+" current players---\n"+players+"\n\n");
			agentAddresses=null;
			/*if (agentAddresses != null) {
				agentAddresses.add(new AgentAddress(requester, this, kernelAddress, manually_requested));
			}*/
			modified = true;
		}
		// needs to be synchronized so that adding occurs prior to getAgentList
		// So addToOverlookers(requester); has to be called in group
		if (manually_requested)
			incrementReferences(null);


		return true;
	}

	final void addMembers(final List<AbstractAgent> bucket, final boolean roleJustCreated, boolean manually_requested) {
		// System.err.println("add members "+bucket.size());
		synchronized (players) {
			players.addAll(bucket);// is optimized
			/*if (agentAddresses != null) {
				final Set<AgentAddress> addresses = new HashSet<>(bucket.size() + agentAddresses.size(), 0.9f);// TODO
																												// try
																												// load
																												// factor
				for (final AbstractAgent a : bucket) {
					addresses.add(new AgentAddress(a, this, kernelAddress, manually_requested));
				}
				addresses.addAll(agentAddresses);// TODO test vs assignment : this because knowing the size
				agentAddresses = addresses;
			}*/
			agentAddresses=null;
			modified = true;
		}
		if (manually_requested)
			updateReferences(null, bucket.size());
		if (roleJustCreated) {
			initializeOverlookers();
		} else {
			addToOverlookers(bucket);
		}
	}

	/*final void addDistantMember(final AgentAddress content) {
		boolean ok;
		synchronized (players) {
			content.setRoleObject(this);// required for equals to work
			ok = distantAgentAddresses.add(content);
			agentAddresses=null;
		}
		if (ok && content.isManuallyRequested())
			incrementReferences(content.getKernelAddress());
	}*/

	final boolean addDistantMemberIfNecessary(final AgentAddress content) {
		boolean ok;
		content.setRoleObject(this);// required for equals to work
		synchronized (players) {

			if (!distantAgentAddresses.contains(content)) {
				ok = distantAgentAddresses.add(content);
				if (!ok)
					content.setRoleObject(null);
				agentAddresses = null;
			}
			else
				ok=false;
		}
		if (ok && content.isManuallyRequested())
			incrementReferences(content.getKernelAddress());
		return ok;
	}

	ReturnCode removeMember(final AbstractAgent requester, boolean manually_requested) {
		synchronized (players) {
			boolean removed=false;
			for (Iterator<AbstractAgent> it=players.iterator();it.hasNext();)
			{
				if (it.next()==requester)
				{
					it.remove();
					removed=true;
					break;
				}
			}
			if (!removed) {
				if (myGroup.isIn(requester)) {
					return ROLE_NOT_HANDLED;
				}
				return ReturnCode.NOT_IN_GROUP;
			}
			agentAddresses=null;
			/*if (agentAddresses != null) {
				Objects.requireNonNull(removeAgentAddressOf(requester, agentAddresses)).setRoleObject(null);
			}*/
			if (logger != null) {
				logger.finest(requester.getName() + " has leaved role " + getCGRString(group, roleName) + "\n");
			}
			modified = true;
		}
		removeFromOverlookers(requester);// TODO put that in the synchronized ?
		if (manually_requested)
			decrementReferences(null);
		checkEmptyness();
		return SUCCESS;
	}

	final void removeMembers(final List<AbstractAgent> bucket, boolean manually_requested) {
		int number;
		List<AbstractAgent> removed=null;
		if (group.isDistributed())
			removed=new ArrayList<>(bucket.size());
		synchronized (players) {
			number = 0;
			for (AbstractAgent aa : bucket)
			{
				for (Iterator<AbstractAgent> it=players.iterator();it.hasNext();)
				{
				
					if (it.next()==aa)
					{
						it.remove();
						++number;
						if (removed!=null && aa!=null)
							removed.add(aa);
						break;
					}
				}
			}
			
			/*if (distantAgentAddresses != null) {

				for (Iterator<AgentAddress> i = distantAgentAddresses.iterator(); i.hasNext();) {
					AgentAddress aa = i.next();
					AbstractAgent agent = aa.getAgent();
					if (agent != null && bucket.remove(agent)) {
						i.remove();
						aa.setRoleObject(null);// cost is high because of string creation...
					}
				}
            }*/
            agentAddresses=null;
			modified = true;
		}
		if (manually_requested)
			updateReferences(null, number);
		removeFromOverlookers(bucket);
		if (removed!=null)
			for (AbstractAgent aa : removed)
				aa.getMadkitKernel().sendNetworkCGRSynchroMessageWithRole(new CGRSynchro(LEAVE_ROLE,
						new AgentAddress(aa, this, kernelAddress, manually_requested), manually_requested));

	}


	@SuppressWarnings("unused")
	boolean removeDistantMember(final AgentAddress content, boolean manually_requested) {

        AgentAddress aa;
        synchronized (players) {
            aa = removeDistantAgentAddress(content);
        }
        if (aa != null && aa.isManuallyRequested())
            decrementReferences(aa.getKernelAddress());
        checkEmptyness();
		return aa!=null;

	}

	final List<AgentAddress> buildAndGetAddresses() {
        List<AgentAddress> set=agentAddresses;
        if (set == null) {
            synchronized (players) {
                if (agentAddresses==null) {
                    set=new ArrayList<>(players.size()+distantAgentAddresses.size());
                    for (final AbstractAgent a : players) {
                        set.add(new AgentAddress(a, this, kernelAddress,
                                !getMyGroup().getCommunityObject().getMyKernel().isAutoCreateGroup(a)));
                    }
                    set.addAll(distantAgentAddresses);
                    agentAddresses = set=Collections.unmodifiableList(set);
                }
                else
                    set=agentAddresses;
            }
        }
        return set;


	}

	private AgentAddress getAndRemoveDistantAgentAddress(AgentAddress aa) {
		for (Iterator<AgentAddress> it = distantAgentAddresses.iterator(); it.hasNext();) {
			AgentAddress a = it.next();
			if (a.equals(aa)) {
				it.remove();
				agentAddresses=null;
				return a;
			}
		}
		return null;
	}

	private AgentAddress removeDistantAgentAddress(AgentAddress aa) {
		AgentAddress a = getAndRemoveDistantAgentAddress(aa);
		if (a != null) {
			if (logger != null) {
				logger.finest(aa + " has leaved role " + getCGRString(group, roleName) + "\n");
			}
			aa.setRoleObject(null);
		}
		return a;
	}


	void removeAgentsFromDistantKernel(final KernelAddress kernelAddress2, MadkitKernel madkitKernel) {
		if (distantAgentAddresses != null) {
			if (logger != null)
				logger.finest("Removing all agents from distant kernel " + kernelAddress2 + " in" + this);
			int number = 0;
			synchronized (players) {
				for (Iterator<AgentAddress> iterator = distantAgentAddresses.iterator(); iterator.hasNext();) {// TODO
					AgentAddress aa = iterator.next();
					if (aa.getKernelAddress().equals(kernelAddress2)) {
						iterator.remove();
						aa.setRoleObject(null);
						++number;
						agentAddresses=null;
						madkitKernel.informHooks(AgentActionEvent.LEAVE_ROLE, aa);
					}
				}

				checkEmptyness();
			}
			updateReferences(kernelAddress2, -number);
		}
	}

	void checkEmptyness() {
		synchronized (players) {
			if ((players == null || players.isEmpty()) && (distantAgentAddresses == null || distantAgentAddresses.isEmpty())) {
				cleanAndRemove();
			}
		}
	}

	/**
	 * 
	 */
	private void cleanAndRemove() {
		/*
		 * for (final Overlooker<? extends AbstractAgent> o : overlookers) {
		 * o.setOverlookedRole(null); }
		 */
		if (number_of_manually_requested_role.getAndSet(0) > 0) {
			group.decrementMadKitReferences(this.kernelAddress);
		}
		myGroup.removeRole(roleName);
		removeOverlookers();
		// overlookers = null;
		tmpReferenceableAgents = null;
		// players = null;
		agentAddresses = null;
		distantAgentAddresses.clear();
		players.clear();
	}

	// /**
	// * @param requester the agent by which I am now empty
	// *
	// */
	// private void deleteMySelfFromOrg(AbstractAgent requester) {
	// for (final Overlooker<? extends AbstractAgent> o : overlookers) {
	// o.setOverlookedRole(null);
	// }
	// myGroup.removeRole(roleName);
	// }

	final void destroy() {
		if (distantAgentAddresses != null) {
			for (AgentAddress aa : distantAgentAddresses) {
				aa.setRoleObject(null);// TODO optimize
			}
		}
		cleanAndRemove();
	}

	final List<AgentAddress> getAgentAddressesCopy() {
		return new ArrayList<>(buildAndGetAddresses());
	}



	static AgentAddress removeAgentAddressOf(final AbstractAgent requester,
			final Collection<AgentAddress> agentAddresses2) {
		// if(requester == null)
		// throw new AssertionError("Wrong use ^^");
		for (final Iterator<AgentAddress> iterator = agentAddresses2.iterator(); iterator.hasNext();) {
			try {
				final AgentAddress aa = iterator.next();
				if (aa.getAgent() == requester) {// TODO test speed with hashcode test
					iterator.remove();
					return aa;
				}
			} catch (NullPointerException e) {
				e.printStackTrace();
			}
		}
		return null;
	}

	// boolean empty() {
	// return ( (players == null || players.isEmpty()) && (agentAddresses == null ||
	// agentAddresses.isEmpty()) );//simply not possible if not following remove A
	// }

	// /**
	// * @return all the agent addresses: This list is never null because an empty
	// role does not exist
	// */
	// Set<AgentAddress> getAgentAddresses() {
	// buildAgentAddressesList();
	// return agentAddresses;
	// }


	final AgentAddress getAgentAddressInGroup(final AbstractAgent abstractAgent) {
		final AgentAddress aa = getAgentAddressOf(abstractAgent);
		if (aa != null)
			return aa;
		return myGroup.getAgentAddressOf(abstractAgent);
	}


	final AgentAddress getAgentAddressInGroupOrParentGroups(final AbstractAgent abstractAgent) {
		final AgentAddress aa = getAgentAddressOf(abstractAgent);
		if (aa != null)
			return aa;
		return myGroup.getAgentAddressInGroupOrInParentGroups(abstractAgent);
	}

	final List<AbstractAgent> getAgentsList() {
		List<AbstractAgent> res=tmpReferenceableAgents;
		if (modified) {
			synchronized (players) {
				if (modified) {
					modified = false;// TODO do a bench : new seems a little bit better
					// long startTime = System.nanoTime();
					res=tmpReferenceableAgents = Collections.unmodifiableList(new ArrayList<>(players));
					// tmpReferenceableAgents =
					// (ArrayList<AbstractAgent>)referenceableAgents.clone();
					// long estimatedTime = System.nanoTime() - startTime;
					// System.err.println(estimatedTime);
				}
				else
					res=tmpReferenceableAgents;
			}
		}
		return res;
	}

	final void addToOverlookers(AbstractAgent a) {

		for (final Overlooker<? extends AbstractAgent> o : overlookers.get()) {
			o.addAgent(a);
		}
	}

	private void addToOverlookers(List<AbstractAgent> l) {
		for (final Overlooker<? extends AbstractAgent> o : overlookers.get()) {
			o.addAgents(l);
		}
	}

	final void removeFromOverlookers(AbstractAgent a) {
		for (final Overlooker<? extends AbstractAgent> o : overlookers.get()) {
			o.removeAgent(a);
		}
	}

	private void removeFromOverlookers(List<AbstractAgent> l) {
		for (final Overlooker<? extends AbstractAgent> o : overlookers.get()) {
			o.removeAgents(l);
		}
	}


	void importDistantOrg(Collection<AgentAddress> list, MadkitKernel madkitKernel) {
		synchronized (players) {
			//buildAndGetAddresses();
			for (final AgentAddress aa : list) {
				aa.setRoleObject(this);
				if (!distantAgentAddresses.contains(aa)) {
					distantAgentAddresses.add(aa);
					//agentAddresses.add(aa);
					if (aa.isManuallyRequested())
						incrementReferences(aa.getKernelAddress());
					agentAddresses = null;
					madkitKernel.informHooks(AgentActionEvent.REQUEST_ROLE, aa);
				}
				// if (agentAddresses.add(aa)) {
				// }
				// else{
				// Logger l = myGroup.getCommunityObject().getMyKernel().logger;
				// if (l != null) {
				// l.log(Level.FINER, "Already have this address ");
				// }
				// }
			}
		}
	}

	AgentAddress getAgentAddressOf(final AbstractAgent a) {

		// final KernelAddress ka = a.getKernelAddress();
		synchronized (players) {
			for (final AgentAddress aa : buildAndGetAddresses()) {// TODO when offline second part is useless
				if (aa.getAgentID() == a.getAgentID() && aa.getAgent() != null)// && ka.equals(aa.getKernelAddress()))
					return aa;
			}
		}
		return null;
	}


	AbstractAgent getAbstractAgentWithAddress(AgentAddress aa) {
		if (players != null) {
			final int hash = aa.hashCode();
			synchronized (players) {
				for (final AbstractAgent agent : players) {
					if (agent.hashCode() == hash)
						return agent;
				}
			}
		}
		return null;
	}

	final boolean contains(AbstractAgent agent) {
		synchronized (players) {
			return players.contains(agent);
		}
	}


	final AgentAddress resolveAgentAddress(AgentAddress anAA) {
		for (final AgentAddress aa : buildAndGetAddresses()) {
			if (aa.equals(anAA)) {
				return aa;
			}
		}
		return null;
	}

}