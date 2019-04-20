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

import com.distrimind.madkit.exceptions.OverflowException;
import com.distrimind.madkit.kernel.AbstractAgent;
import com.distrimind.madkit.kernel.JunitMadkit;
import com.distrimind.madkit.kernel.network.TransferAgent.IDTransfer;
import com.distrimind.madkit.util.SecuredObjectInputStream;
import com.distrimind.madkit.util.SecuredObjectOutputStream;
import org.junit.Assert;
import org.junit.Test;

import java.io.*;

/**
 * @author Jason Mahdjoub
 * 
 * @since MadkitLanEdition 1.0
 * @version 1.0
 * 
 */
public class IDTransferTest extends JunitMadkit {
	@Test
	public void testIDTransfer() {
		launchTest(new AbstractAgent() {
			@SuppressWarnings("UnusedAssignment")
            @Override
			protected void activate() throws InterruptedException {
				try {
					int idNumber = 100;
					IDTransfer[] ids = new IDTransfer[idNumber];

					for (int i = 0; i < idNumber; i++) {
						ids[i] = IDTransfer.generateIDTransfer(MadkitKernelAccess.getIDTransferGenerator(this));
					}
					Assert.assertEquals(100, MadkitKernelAccess.numberOfValidGeneratedID(this));
					for (int i = 0; i < idNumber; i++) {
						for (int j = 0; j < idNumber; j++) {
							if (i == j) {
                                Assert.assertEquals(ids[i], ids[j]);
							} else
								Assert.assertNotEquals(ids[i], ids[j]);
						}
						try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
							try (DataOutputStream dos=new DataOutputStream(baos);SecuredObjectOutputStream soos=new SecuredObjectOutputStream(dos)) {
								Assert.assertTrue(ids[i].isGenerated());
								soos.writeObject(ids[i], false);
							}
							try (ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray())) {
								try (DataInputStream dis=new DataInputStream(bais);SecuredObjectInputStream ois = new SecuredObjectInputStream(dis)) {
									IDTransfer id = ois.readObject(false, IDTransfer.class);
									Assert.assertFalse(id.isGenerated());
									Assert.assertEquals(id, ids[i]);
								}
							}
						}
					}
					ids=null;
                    System.gc();
					System.gc();
					Thread.sleep(500);
					Assert.assertEquals(0, MadkitKernelAccess.numberOfValidGeneratedID(this));
					IDTransfer id = IDTransfer.generateIDTransfer(MadkitKernelAccess.getIDTransferGenerator(this));
					//noinspection ResultOfMethodCallIgnored
					id.getID();
					// Assert.assertEquals(0, id.getID());
					Assert.assertEquals(1, MadkitKernelAccess.numberOfValidGeneratedID(this));

				} catch (OverflowException | IOException | ClassNotFoundException e) {
					e.printStackTrace();
					Assert.fail();
				}
			}
		});
	}
}
