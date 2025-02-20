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
package com.distrimind.madkit.simulation;

import static org.testng.AssertJUnit.assertNotNull;
import org.testng.annotations.Test;
import org.testng.annotations.BeforeMethod;
import org.testng.Assert;
import org.testng.AssertJUnit;
import java.lang.reflect.Field;

import com.distrimind.madkit.kernel.AbstractAgent;
import com.distrimind.madkit.kernel.Group;
import com.distrimind.madkit.kernel.Probe;

/**
 * Has to be outside of madkit.kernel for really testing visibility
 * 
 * @author Fabien Michel1
 * @since MaDKit 5.0.0.13
 * @version 0.9
 * 
 */
@SuppressWarnings("all")
public class ProbeTest {

	Probe<TestAgent> a;
	TestAgent agt;

	@BeforeMethod
	public void setUp() throws Exception {
		a = new Probe<>(new Group("t", "t"), "t");
		agt = new TestAgent() {
			boolean bool2 = false;
		};
	}

	@Test
	public void testActivator() {
		try {
			a = new Probe<>(null, null);
			AssertJUnit.assertFalse(true);
		} catch (NullPointerException e) {

		}

	}

	@Test
	public void testFindFieldOnInheritedPublic()
			throws NoSuchFieldException, IllegalArgumentException, IllegalAccessException {
		Field m = a.findFieldOn(agt.getClass(), "bool");
		assertNotNull(m);
		System.err.println(m.get(agt));
	}

	@Test
	public void testFindFieldOnPublic() throws NoSuchFieldException, IllegalArgumentException, IllegalAccessException {
		Field m = a.findFieldOn(agt.getClass(), "bool2");
		assertNotNull(m);
		System.err.println(m.get(agt));
	}

	@Test
	public void testFindFieldOnNotExist() {
		try {
			Field m = a.findFieldOn(agt.getClass(), "notExist");
			Assert.fail("ex not thrown");
		} catch (NoSuchFieldException e) {
			e.printStackTrace();
		}
	}

	@Test
	public void testFindFieldOnProtected()
			throws NoSuchFieldException, IllegalArgumentException, IllegalAccessException {
		Field m = a.findFieldOn(agt.getClass(), "value");
		assertNotNull(m);
		System.err.println(m.get(agt));
	}

	@Test
	public void testFindFieldOnPrivate() throws NoSuchFieldException, IllegalArgumentException, IllegalAccessException {
		Field m = a.findFieldOn(agt.getClass(), "something");
		assertNotNull(m);
		System.err.println(m.get(agt));
	}

}

@SuppressWarnings("all")
class TestAgent extends AbstractAgent {
	public boolean bool = false;
	protected int value = 2;
	private double something = 3.0;
}
