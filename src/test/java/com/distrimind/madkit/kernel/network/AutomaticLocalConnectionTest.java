package com.distrimind.madkit.kernel.network;

import com.distrimind.madkit.kernel.AbstractAgent;
import com.distrimind.madkit.kernel.TestNGMadkit;
import com.distrimind.madkit.kernel.Madkit;
import org.testng.AssertJUnit;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Factory;
import org.testng.annotations.Test;

import java.util.logging.Level;

public class AutomaticLocalConnectionTest extends TestNGMadkit {

	final NetworkEventListener eventListener1;
	final NetworkEventListener eventListener2;

	@DataProvider(name="data")
	public static Object[][] data() {
		return NetworkEventListener.getNetworkEventListenersForPeerToPeerConnectionsWithRandomProperties(true, false,
				true, true, true, null,null, 2, 1, 2);
	}

	@Factory(dataProvider = "data")
	public AutomaticLocalConnectionTest(NetworkEventListener eventListener1, NetworkEventListener eventListener2) {
		this.eventListener1 = eventListener1;
		this.eventListener2 = eventListener2;
		this.eventListener1.durationBeforeCancelingTransferConnection = 90000;
		this.eventListener2.durationBeforeCancelingTransferConnection = 120000;
	}

	private static final long timeOut = 120000;

	@Test
	public void testAutomaticLocalConnection() {
		cleanHelperMDKs();
		launchTest(new AbstractAgent() {
			@Override
			protected void activate() throws InterruptedException {
				launchThreadedMKNetworkInstance(Level.INFO, AbstractAgent.class, null, eventListener2);
				pause(2000);
				launchThreadedMKNetworkInstance(Level.INFO, AbstractAgent.class, null, eventListener1);
				pause(1400);
				for (Madkit m : getHelperInstances(this, 2)) {
					checkConnectedKernelsNb(this, m, 1, timeOut);
				}
				for (Madkit m : getHelperInstances(this, 2)) {
					checkConnectedIntancesNb(this, m, 2, timeOut);
				}
				sleep(400);
				cleanHelperMDKs(this);
				AssertJUnit.assertEquals(getHelperInstances(this, 0).size(), 0);

			}
		});
	}

	/*
	 * @Test public void testSameKernelAddress() { cleanHelperMDKs(); launchTest(new
	 * AbstractAgent() {
	 * 
	 * @Override protected void activate() throws InterruptedException {
	 * launchThreadedMKNetworkInstance(Level.INFO, AbstractAgent.class, null,
	 * eventListener2); pause(2000);
	 * 
	 * KernelAddress
	 * ka1=AutomaticLocalConnectionTest.this.getKernelAddress(getHelperInstances(1).
	 * get(0)); try(ByteArrayOutputStream baos=new ByteArrayOutputStream()) {
	 * try(ObjectOutputStream oos=new ObjectOutputStream(baos)) {
	 * oos.writeObject(ka1); } try(ByteArrayInputStream bais=new
	 * ByteArrayInputStream(baos.toByteArray())) { try(ObjectInputStream ois=new
	 * ObjectInputStream(bais)) { ka1=(KernelAddress)ois.readObject(); } } }
	 * catch(Exception e) { e.printStackTrace(); }
	 * 
	 * launchThreadedMKNetworkInstance(Level.INFO, AbstractAgent.class, null,
	 * eventListener1, ka1); pause(300); for (Madkit m : getHelperInstances(2)) {
	 * checkConnectedKernelsNb(this, m, 0, timeOut); } for (Madkit m :
	 * getHelperInstances(2)) { checkConnectedIntancesNb(this, m, 0, timeOut); }
	 * sleep(400); cleanHelperMDKs(this);
	 * Assert.assertEquals(getHelperInstances(0).size(), 0);
	 * 
	 * } }); }
	 */

}
