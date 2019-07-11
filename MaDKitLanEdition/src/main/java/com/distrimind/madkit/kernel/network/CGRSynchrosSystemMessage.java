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
package com.distrimind.madkit.kernel.network;

import com.distrimind.madkit.kernel.AgentAddress;
import com.distrimind.madkit.kernel.Group;
import com.distrimind.madkit.kernel.KernelAddress;
import com.distrimind.util.io.*;

import java.io.IOException;
import java.util.*;

/**
 * 
 * @author Jason Mahdjoub
 * @version 1.2
 * @since MadkitLanEdition 1.0
 */
final class CGRSynchrosSystemMessage implements WithoutInnerSizeControl {

	private Map<String, Map<Group, Map<String, Collection<AgentAddress>>>> organization_snap_shot;
	private ArrayList<Group> removedGroups;
	@SuppressWarnings("unused")
	CGRSynchrosSystemMessage()
	{
		
	}
	CGRSynchrosSystemMessage(Map<String, Map<Group, Map<String, Collection<AgentAddress>>>> organization_snap_shop,
							 KernelAddress from, ArrayList<Group> removedGroups) {
		if (organization_snap_shop == null)
			throw new NullPointerException();
		if (removedGroups == null)
			throw new NullPointerException();
		this.organization_snap_shot = cleanUp(organization_snap_shop, from);
		this.removedGroups = removedGroups;
	}
	
	@Override
	public void readExternal(SecuredObjectInputStream in) throws IOException, ClassNotFoundException {

		
		
		
		int globalSize=NetworkProperties.GLOBAL_MAX_SHORT_DATA_SIZE;
		int totalSize=4;
		organization_snap_shot= new HashMap<>();
		int size=in.readInt();
		if (size<0)
			throw new MessageExternalizationException(Integrity.FAIL_AND_CANDIDATE_TO_BAN);
		for (int i=0;i<size;i++)
		{
			String comunity=in.readString(false, Group.MAX_COMMUNITY_LENGTH);
			int size2=in.readInt();
			if (size2<0)
				throw new MessageExternalizationException(Integrity.FAIL_AND_CANDIDATE_TO_BAN);
			totalSize+=4+ SerializationTools.getInternalSize(comunity, Group.MAX_COMMUNITY_LENGTH);
			if (totalSize>globalSize)
				throw new MessageExternalizationException(Integrity.FAIL_AND_CANDIDATE_TO_BAN);
			Map<Group, Map<String, Collection<AgentAddress>>> groups= new HashMap<>();
			for (int j=0;j<size2;j++)
			{
				Group group=in.readObject(false, Group.class);
				totalSize+=group.getInternalSerializedSize()+4;
				if (totalSize>globalSize)
					throw new MessageExternalizationException(Integrity.FAIL_AND_CANDIDATE_TO_BAN);
				int size3=in.readInt();
				if (size3<0)
					throw new MessageExternalizationException(Integrity.FAIL_AND_CANDIDATE_TO_BAN);
				Map<String, Collection<AgentAddress>> roles= new HashMap<>();
				for (int k=0;k<size3;k++)
				{
					String role=in.readString(false, Group.MAX_ROLE_NAME_LENGTH);
					totalSize+=4+SerializationTools.getInternalSize(comunity, Group.MAX_ROLE_NAME_LENGTH);
					int size4=in.readInt();
					if (totalSize>globalSize)
						throw new MessageExternalizationException(Integrity.FAIL_AND_CANDIDATE_TO_BAN);
					if (size4<0)
						throw new MessageExternalizationException(Integrity.FAIL_AND_CANDIDATE_TO_BAN);
					HashSet<AgentAddress> agentAddresses=new HashSet<>();
					for (int l=0;l<size4;l++)
					{
						AgentAddress aa=in.readObject( false, AgentAddress.class);
						totalSize+=aa.getInternalSerializedSize();
						if (totalSize>globalSize)
							throw new MessageExternalizationException(Integrity.FAIL_AND_CANDIDATE_TO_BAN);
						
						agentAddresses.add(aa);
					}
					roles.put(role, agentAddresses);
				}
				groups.put(group, roles);
			}
			organization_snap_shot.put(comunity, groups);
		}
		size=in.readInt();
		totalSize+=size;
		if (size<0 || totalSize+size*4>globalSize)
			throw new MessageExternalizationException(Integrity.FAIL_AND_CANDIDATE_TO_BAN);
		removedGroups=new ArrayList<>(size);
		for (int i=0;i<size;i++)
		{
			Group g=in.readObject( false, Group.class);
			totalSize+=g.getInternalSerializedSize();
			if (totalSize>globalSize)
				throw new MessageExternalizationException(Integrity.FAIL_AND_CANDIDATE_TO_BAN);
			removedGroups.add(g);
		}
		for (Map.Entry<String, Map<Group, Map<String, Collection<AgentAddress>>>> e0 : organization_snap_shot.entrySet()) {
			for (Map.Entry<Group, Map<String, Collection<AgentAddress>>> e : e0.getValue().entrySet()) {
				for (Map.Entry<String, Collection<AgentAddress>> e2 : e.getValue().entrySet()) {
					for (AgentAddress aa : e2.getValue()) {
						for (Group g : removedGroups)
							if (aa.getGroup().equals(g))
								throw new MessageExternalizationException(Integrity.FAIL);
					}
				}
			}
		}
		
	}

	@Override
	public void writeExternal(SecuredObjectOutputStream oos) throws IOException {
		
		oos.writeInt(organization_snap_shot.size());
		for (Map.Entry<String, Map<Group, Map<String, Collection<AgentAddress>>>> e : organization_snap_shot.entrySet())
		{
			oos.writeString(e.getKey(), false, Group.MAX_COMMUNITY_LENGTH);
			oos.writeInt(e.getValue().size());
			for (Map.Entry<Group, Map<String, Collection<AgentAddress>>> e2 : e.getValue().entrySet())
			{
				oos.writeObject(e2.getKey(), false);
				oos.writeInt(e2.getValue().size());
				for (Map.Entry<String, Collection<AgentAddress>> e3 : e2.getValue().entrySet())
				{
					oos.writeString(e3.getKey(), false, Group.MAX_ROLE_NAME_LENGTH);
					oos.writeInt(e3.getValue().size());
					for (AgentAddress aa : e3.getValue())
						oos.writeObject(aa, false);
				}
			}
		}
		oos.writeInt(removedGroups.size());
		for (Group g : removedGroups)
			oos.writeObject(g, false);
		
	}

	private static Map<String, Map<Group, Map<String, Collection<AgentAddress>>>> cleanUp(
			Map<String, Map<Group, Map<String, Collection<AgentAddress>>>> organization_snap_shop, KernelAddress from) {
		Map<String, Map<Group, Map<String, Collection<AgentAddress>>>> res = new TreeMap<>();

		for (Map.Entry<String, Map<Group, Map<String, Collection<AgentAddress>>>> e0 : organization_snap_shop.entrySet()) {
			Map<Group, Map<String, Collection<AgentAddress>>> c = new TreeMap<>();
			for (Map.Entry<Group, Map<String, Collection<AgentAddress>>> e : e0.getValue().entrySet()) {
				Map<String, Collection<AgentAddress>> o = new TreeMap<>();
				for (Map.Entry<String, Collection<AgentAddress>> e2 : e.getValue().entrySet()) {
					Set<AgentAddress> h = new HashSet<>();
					for (AgentAddress aa : e2.getValue()) {
						if (aa.getKernelAddress().equals(from)) {
							h.add(aa);
						}
					}
					if (!h.isEmpty())
						o.put(e2.getKey(), h);
				}
				if (!o.isEmpty())
					c.put(e.getKey(), o);
			}
			if (!c.isEmpty())
				res.put(e0.getKey(), c);
		}
		return res;
	}

	private ArrayList<Group> cleanUp(Map<String, Map<Group, Map<String, Collection<AgentAddress>>>> organization_snap_shop,
			ArrayList<Group> removedGroups) {
		ArrayList<Group> res = new ArrayList<>();
		for (Group g : removedGroups) {
			boolean add = true;
			for (Map.Entry<String, Map<Group, Map<String, Collection<AgentAddress>>>> e0 : organization_snap_shop.entrySet()) {
				for (Map.Entry<Group, Map<String, Collection<AgentAddress>>> e : e0.getValue().entrySet()) {
					for (Map.Entry<String, Collection<AgentAddress>> e2 : e.getValue().entrySet()) {
						for (AgentAddress aa : e2.getValue()) {
							if (g.equals(aa.getGroup())) {
								add = false;
								break;
							}
						}
						if (!add)
							break;
					}
					if (!add)
						break;
				}
				if (!add)
					break;
			}
			if (add)
				res.add(g);
		}
		return res;
	}

	/*public Map<String, Map<Group, Map<String, Set<AgentAddress>>>> getOrganisationSnapShot() {
		return organization_snap_shot;
	}*/

	/*public ArrayList<Group> getRemovedGroups() {
		return removedGroups;
	}*/

	CGRSynchros getCGRSynchros(KernelAddress distantKernelAddress) {
		Map<String, Map<Group, Map<String, Collection<AgentAddress>>>> org = cleanUp(organization_snap_shot,
				distantKernelAddress);
		return new CGRSynchros(org, distantKernelAddress, cleanUp(org, removedGroups));
	}

	
	@Override
	public boolean excludedFromEncryption() {
		return false;
	}

	
}
