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

import static com.distrimind.madkit.kernel.TestNGMadkit.GROUP;
import static com.distrimind.madkit.kernel.TestNGMadkit.ROLE;

import java.io.IOException;
import java.util.logging.Level;

/**
 * @author Fabien Michel
 * @since MaDKit 5.0.0.7
 * @version 0.9
 * 
 */
public class FaultyAA extends DoItDuringLifeCycleAbstractAgent {


	public FaultyAA(boolean inActivate, boolean inEnd) {
		super(inActivate, inEnd);
	}

	public FaultyAA(boolean inActivate) {
		super(inActivate);
	}

	public FaultyAA() {
		super();
	}

	@Override
	public void activate() {
		requestRole(GROUP, ROLE);
		super.activate();
	}

	@SuppressWarnings("null")
	@Override
	public void doIt() {
		if (logger != null)
			logger.info("crashing myself");
		Object o = null;
		o.toString();
	}

	public static void main(String[] args) throws IOException {
		String[] myArgs = { "agentLogLevel", Level.ALL.toString() };
		executeThisAgent(myArgs);
	}

}