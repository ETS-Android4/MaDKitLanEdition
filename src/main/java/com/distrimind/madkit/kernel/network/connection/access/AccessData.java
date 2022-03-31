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
package com.distrimind.madkit.kernel.network.connection.access;

import com.distrimind.madkit.kernel.network.InetAddressFilters;

import java.net.InetAddress;

/**
 * Represents data enabling to gives default right access
 * 
 * @author Jason Mahdjoub
 * @see LoginData
 * @since MadkitLanEdition 1.0
 * @version 1.0
 */
public abstract class AccessData {
	private final InetAddressFilters filters = new InetAddressFilters();

	public InetAddressFilters getFilters() {
		return filters;
	}

	/**
	 * Tells if the filter accept the connection with the given parameters
	 * 
	 * @param _distant_inet_address
	 *            the distant inet address
	 * @param _distant_port the distant port
	 * @param _local_port
	 *            the local port
	 * @return true if the filter accept the connection with the given parameters
	 */
	public boolean isConcernedBy(InetAddress _distant_inet_address, int _distant_port, int _local_port) {
		return filters.isConcernedBy(_distant_inet_address, _distant_port, _local_port);
	}

	/**
	 * 
	 * @return the default group enabled for this access, without identification.
	 * 
	 * @throws AccessException
	 *             if a problem occurs
	 */
	public abstract ListGroupsRoles getDefaultGroupsAccess() throws AccessException;

	/**
	 * 
	 * @param o
	 *            another object to compare
	 * @return true if this AccessData equals those given as parameter
	 */
	public abstract boolean equals(Object o);
}
