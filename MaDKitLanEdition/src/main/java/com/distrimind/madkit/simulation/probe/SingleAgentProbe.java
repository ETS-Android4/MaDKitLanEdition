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
package com.distrimind.madkit.simulation.probe;

import java.lang.reflect.Field;

import com.distrimind.madkit.kernel.AbstractAgent;
import com.distrimind.madkit.kernel.AbstractGroup;
import com.distrimind.madkit.kernel.Probe;
import com.distrimind.madkit.simulation.SimulationException;

/**
 * This probe inspects fields of type T on only one agent of type A and its
 * subclasses. This is designed for probing one single agent, i.e. methods are
 * designed and optimized in this respect.
 * 
 * @param <A>
 *            the most common class type expected in this group (e.g.
 *            AbstractAgent)
 * @param <T>
 *            the type of the property, i.e. Integer (this works if the field is
 *            an int, i.e. a primitive type)
 * @author Fabien Michel
 * @author Jason Mahdjoub
 * @since MaDKitLanEdition 1.0
 * @version 2.0
 * 
 */
public class SingleAgentProbe<A extends AbstractAgent, T> extends Probe<A>// TODO make a thread safe version
{
	final private String fieldName;
	private Field field;
	private A probedAgent;

	/**
	 * Builds a new SingleAgentProbe considering a CGR location and the name of the
	 * class's field.
	 * 
	 * This constructor has the same effect than
	 * <code>#SingleAgentProbe(groups, role, true)</code>
	 * 
	 * 
	 * @param groups the groups
	 * @param role the role name
	 * @param fieldName
	 *            the name of a field which is encapsulated in the type &lt;A&gt;
	 */
	public SingleAgentProbe(AbstractGroup groups, String role, String fieldName) {
		super(groups, role);
		this.fieldName = fieldName;
	}

	/**
	 * Builds a new SingleAgentProbe considering a CGR location and the name of the
	 * class's field.
	 * 
	 * 
	 * @param groups the groups
	 * @param role the role
	 * @param fieldName
	 *            the name of a field which is encapsulated in the type &lt;A&gt;
	 * @param unique
	 *            Tells if the function {@link #getCurrentAgentsList()}
	 *            must returns unique references.
	 */
	public SingleAgentProbe(AbstractGroup groups, String role, String fieldName, boolean unique) {
		super(groups, role, unique);
		this.fieldName = fieldName;
	}

	@Override
	protected void adding(A agent) {
		if (field == null) {// TODO replace or not
			try {
				field = findFieldOn(agent.getClass(), fieldName);
				probedAgent = agent;
			} catch (NoSuchFieldException e) {
				throw new SimulationException(this + " on " + agent, e);
			}
		}
	}

	@Override
	protected void removing(A agent) {
		super.removing(agent);
		field = null;
	}

	/**
	 * Get the current probed agent.
	 * 
	 * @return the agent which is currently probed
	 */
	public A getProbedAgent() {
		return probedAgent;
	}

	/**
	 * Returns the current value of the agent's field
	 * 
	 * @return the value of the agent's field
	 */
	@SuppressWarnings("unchecked")
	public T getPropertyValue() {
		try {
			return (T) field.get(probedAgent);
		} catch (IllegalAccessException e) {
			throw new SimulationException(this + " on " + probedAgent, e);
		}
	}

	@Override
	public String toString() {
		return super.toString() + (probedAgent == null ? "" : " : " + probedAgent);
	}

	/**
	 * Should be used to work with primitive types or fields which are initially
	 * <code>null</code>
	 * 
	 * @param value the property value
	 */
	public void setPropertyValue(final T value) {
		try {
			field.set(probedAgent, value);
		} catch (IllegalArgumentException | IllegalAccessException e) {
			throw new SimulationException(this + " on " + probedAgent, e);
		}
	}

}