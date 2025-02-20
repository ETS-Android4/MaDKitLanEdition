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

import com.distrimind.madkit.kernel.*;
import com.distrimind.madkit.kernel.network.*;
import com.distrimind.madkit.kernel.network.connection.access.AbstractAccessProtocolProperties;
import com.distrimind.madkit.kernel.network.connection.access.AccessProtocolWithP2PAgreementProperties;
import com.distrimind.madkit.kernel.network.connection.access.ListGroupsRoles;
import com.distrimind.madkit.kernel.network.connection.secured.P2PSecuredConnectionProtocolPropertiesWithKeyAgreement;
import com.distrimind.madkit.message.BooleanMessage;
import com.distrimind.ood.database.InFileEmbeddedH2DatabaseFactory;
import com.distrimind.ood.database.exceptions.DatabaseException;
import com.distrimind.util.FileTools;
import com.distrimind.util.Reference;
import com.distrimind.util.crypto.SymmetricAuthenticatedSignatureType;
import com.distrimind.util.crypto.SymmetricEncryptionType;
import com.distrimind.util.io.RandomByteArrayInputStream;
import com.distrimind.util.io.RandomInputStream;
import com.distrimind.util.io.SecuredObjectInputStream;
import com.distrimind.util.io.SecuredObjectOutputStream;
import org.testng.Assert;
import org.testng.AssertJUnit;

import java.io.File;
import java.io.IOException;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Collections;
import java.util.Random;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;


/**
 * @author Jason Mahdjoub
 * @since MadkitLanEdition 1.5
 * @version 1.0
 * 
 */
public class BigDataTransferSpeed extends TestNGMadkit {
	final NetworkEventListener eventListener1;
	final NetworkEventListener eventListener2;
    final long downloadLimitInBytesPerSecond, uploadLimitInBytesPerSecond;
    final boolean cancelTransfer, cancelTransferFromReceiver, asynchronousMessage, restartMessage, restartWithLeaveRequestRole, globalDisconnection, transferPaused, pauseAll;
    static final DoubleIP ipToConnect;
    static
    {
        DoubleIP ip=null;
        try {
            ip=new DoubleIP(5000, (Inet4Address) InetAddress.getByName("127.0.0.1"), (Inet6Address) InetAddress.getByName("::1"));
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
        ipToConnect=ip;
    }
    final File databaseFile1=new File("tmpDatabaseFile1");
    final File databaseFile2=new File("tmpDatabaseFile2");

	public BigDataTransferSpeed(final long downloadLimitInBytesPerSecond, final long uploadLimitInBytesPerSecond,
                                boolean cancelTransfer, boolean cancelTransferFromReceiver, boolean asynchronousMessage,
                                boolean restartMessage, boolean restartWithLeaveRequestRole, boolean globalDisconnection,
                                boolean transferPaused, boolean pauseAll) throws UnknownHostException {
        if(databaseFile1.exists())
            FileTools.deleteDirectory(databaseFile1);
        if(databaseFile2.exists())
            FileTools.deleteDirectory(databaseFile2);
        this.cancelTransfer = cancelTransfer;
        this.cancelTransferFromReceiver=cancelTransferFromReceiver;
        this.asynchronousMessage=asynchronousMessage;
        this.restartMessage=restartMessage;
        this.restartWithLeaveRequestRole=restartWithLeaveRequestRole;
        this.downloadLimitInBytesPerSecond=downloadLimitInBytesPerSecond;
        this.uploadLimitInBytesPerSecond=uploadLimitInBytesPerSecond;
        this.globalDisconnection=globalDisconnection;
        this.transferPaused=transferPaused;
        this.pauseAll=pauseAll;
        P2PSecuredConnectionProtocolPropertiesWithKeyAgreement p2pprotocol=new P2PSecuredConnectionProtocolPropertiesWithKeyAgreement();
        p2pprotocol.isServer = true;
        p2pprotocol.symmetricEncryptionType=SymmetricEncryptionType.AES_GCM;
        p2pprotocol.symmetricSignatureType= SymmetricAuthenticatedSignatureType.HMAC_SHA2_384;

        ListGroupsRoles defaultGroupAccess=new ListGroupsRoles();
        defaultGroupAccess.addGroupsRoles(TestNGMadkit.GROUP);


        AbstractAccessProtocolProperties app = new AccessProtocolWithP2PAgreementProperties();
        this.eventListener1 = new NetworkEventListener(true, false, false, null,
                new ConnectionsProtocolsMKEventListener(p2pprotocol), new AccessProtocolPropertiesMKEventListener(app),
                new AccessDataMKEventListener(AccessDataMKEventListener.getDefaultAccessData(defaultGroupAccess)), 5000,
                Collections.emptyList(),
                InetAddress.getByName("127.0.0.1"), InetAddress.getByName("::1")) {

            @Override
            public void onMaDKitPropertiesLoaded(MadkitProperties _properties) {
                super.onMaDKitPropertiesLoaded(_properties);
                _properties.networkProperties.networkLogLevel = Level.INFO;
                _properties.networkProperties.maximumGlobalDownloadSpeedInBytesPerSecond=downloadLimitInBytesPerSecond;
                _properties.networkProperties.maximumGlobalUploadSpeedInBytesPerSecond=uploadLimitInBytesPerSecond;
                try {
                    _properties.setDatabaseFactory(new InFileEmbeddedH2DatabaseFactory(databaseFile1));
                } catch (DatabaseException e) {
                    e.printStackTrace();
                }
            }
        };
        this.eventListener1.maxBufferSize=Short.MAX_VALUE*2;

		P2PSecuredConnectionProtocolPropertiesWithKeyAgreement u = new P2PSecuredConnectionProtocolPropertiesWithKeyAgreement();
		u.isServer = false;
        u.symmetricEncryptionType=p2pprotocol.symmetricEncryptionType;
		u.symmetricSignatureType= p2pprotocol.symmetricSignatureType;
        u.messageDigestType=p2pprotocol.messageDigestType;

		app = new AccessProtocolWithP2PAgreementProperties();
		
		this.eventListener2 = new NetworkEventListener(true, false, false, null,
				new ConnectionsProtocolsMKEventListener(u), new AccessProtocolPropertiesMKEventListener(app),
				new AccessDataMKEventListener(AccessDataMKEventListener.getDefaultAccessData(defaultGroupAccess)), 5000,
                Collections.singletonList(ipToConnect)
				) {

			@Override
			public void onMaDKitPropertiesLoaded(MadkitProperties _properties) {
			    super.onMaDKitPropertiesLoaded(_properties);
                _properties.networkProperties.networkLogLevel = Level.INFO;
                _properties.networkProperties.maximumGlobalDownloadSpeedInBytesPerSecond=downloadLimitInBytesPerSecond;
				_properties.networkProperties.maximumGlobalUploadSpeedInBytesPerSecond=uploadLimitInBytesPerSecond;
                try {
                    _properties.setDatabaseFactory(new InFileEmbeddedH2DatabaseFactory(databaseFile2));
                } catch (DatabaseException e) {
                    e.printStackTrace();
                }
			}
		};
        this.eventListener2.maxBufferSize=this.eventListener1.maxBufferSize;
	}
    public BigDataTransferSpeed(final long downloadLimitInBytesPerSecond, final long uploadLimitInBytesPerSecond) throws UnknownHostException {
        this(downloadLimitInBytesPerSecond, uploadLimitInBytesPerSecond, false, false, false, false, false, false, false,false);
    }
    private static final long timeOut = 20000;

    static final class AsynchronousIdentifier implements ExternalAsynchronousBigDataIdentifier {
        private int id;
        @SuppressWarnings("unused")
        AsynchronousIdentifier()
        {

        }

        public AsynchronousIdentifier(int id) {
            this.id = id;
        }

        @Override
        public int getInternalSerializedSize() {
            return 4;
        }

        @Override
        public void writeExternal(SecuredObjectOutputStream out) throws IOException {
            out.writeInt(id);
        }

        @Override
        public void readExternal(SecuredObjectInputStream in) throws IOException, ClassNotFoundException {
            id=in.readInt();
        }

        @Override
        public String toString() {
            return "AsynchronousIdentifier{" +
                    "id=" + id +
                    '}';
        }
    }
    static class AsynchronousToSendWrapper implements AsynchronousBigDataToSendWrapper
    {

        @Override
        public RandomInputStream getRandomInputStream(ExternalAsynchronousBigDataIdentifier externalAsynchronousBigDataIdentifier) {
            return new RandomByteArrayInputStream(bigDataToSendArray.get());
        }

        @Override
        public int getInternalSerializedSize() {
            return 0;
        }

        @Override
        public void writeExternal(SecuredObjectOutputStream out) throws IOException {

        }

        @Override
        public void readExternal(SecuredObjectInputStream in) throws IOException, ClassNotFoundException {

        }
    }
    static Reference<byte[]> bigDataToSendArray=new Reference<>(null);

	public void bigDataTransfer() {
        try {
            final AtomicReference<Boolean> transfered1 = new AtomicReference<>(null);
            final AtomicReference<Boolean> transfered2 = new AtomicReference<>(null);
            final int size = 400000000;
            bigDataToSendArray.set(new byte[size]);
            Random r = new Random(System.currentTimeMillis());
            r.nextBytes(bigDataToSendArray.get());
            // addMadkitArgs("--kernelLogLevel",Level.INFO.toString(),"--networkLogLevel",Level.FINEST.toString());
            launchTest(new AbstractAgent() {
                @Override
                protected void end() {
                    while (nextMessage() != null) ;
                }

                @Override
                protected void activate() throws InterruptedException {
                    AbstractAgent bigDataSenderAgent = new NormalAgent() {
                        @Override
                        protected void activate() {


                        }

                        private boolean sendBigMessage(boolean encrypt, long delay) throws InterruptedException {
                            try {
                                IBigDataTransferID transferID;
                                if (asynchronousMessage)
                                    AssertJUnit.assertNotNull(transferID = this.sendBigDataWithRoleOrDifferSendingUntilRecipientWasFound(GROUP, ROLE,
                                            encrypt ? new AsynchronousIdentifier(1) : new AsynchronousIdentifier(0), new AsynchronousToSendWrapper(),null,
                                            null, ROLE2, !encrypt, 1000000));
                                else {
                                    AgentAddress aa = getAgentWithRole(GROUP, ROLE);
                                    Assert.assertNotNull(aa);
                                    AssertJUnit.assertNotNull(transferID = this.sendBigData(aa, new RandomByteArrayInputStream(bigDataToSendArray.get()), 0, bigDataToSendArray.get().length, null, null, !encrypt));
                                }
                                if (cancelTransfer && !cancelTransferFromReceiver) {
                                    sleep(100);
                                    AssertJUnit.assertEquals(AbstractAgent.ReturnCode.SUCCESS, this.cancelBigDataTransfer(transferID));
                                }
                                else if (restartMessage && restartWithLeaveRequestRole && !cancelTransferFromReceiver && asynchronousMessage)
                                {
                                    sleep(100);
                                    System.out.println("Leave role from sender");
                                    this.leaveRole(GROUP, ROLE2);
                                    sleep(1000);

                                    System.out.println("Reconnect from sender");
                                    this.requestRole(GROUP, ROLE2);
                                }
                                else if (transferPaused && !cancelTransferFromReceiver)
                                {
                                    sleep(100);

                                    if (pauseAll) {
                                        System.out.println("Pause transfers from sender (all distant peers concerned)");
                                        setAllBigDataTransfersPaused(true);
                                    }
                                    else {
                                        System.out.println("Pause transfers from sender (one distant peer concerned)");
                                        setBigDataTransfersOfAGivenKernelPaused(getAvailableDistantKernels().stream().findAny().orElse(null), true);
                                    }

                                    sleep(2000);
                                    Assert.assertEquals(transferID.getBytePerSecondsStat().getNumberOfIdentifiedBytesDuringTheLastCycle(), 0);
                                    System.out.println("Restart transfers from sender");
                                    if (pauseAll)
                                        setAllBigDataTransfersPaused(false);
                                    else
                                        setBigDataTransfersOfAGivenKernelPaused(getAvailableDistantKernels().stream().findAny().orElse(null), false);

                                }
                                Message m = this.waitNextMessage(delay);
                                sleep(300);
                                AssertJUnit.assertTrue(m==null?"null":m.toString(),m instanceof BigDataResultMessage);
                                BigDataResultMessage bdrm = (BigDataResultMessage) m;

                                boolean tr1 = false;
                                if (bdrm.getType() == BigDataResultMessage.Type.BIG_DATA_TRANSFERRED) {
                                    tr1 = true;
                                    AssertJUnit.assertFalse(cancelTransfer);

                                    if (!transferPaused && !restartMessage && this.getMaximumGlobalUploadSpeedInBytesPerSecond() != Long.MAX_VALUE) {
                                        double speed = ((double) bdrm.getTransferredDataLength()) / ((double) bdrm.getTransferDuration()) * 1000.0;
                                        AssertJUnit.assertTrue(speed < getMaximumGlobalUploadSpeedInBytesPerSecond() * 2);
                                        AssertJUnit.assertTrue(speed > getMaximumGlobalUploadSpeedInBytesPerSecond() / 2.0);
                                    }
                                    if (!restartMessage && transferID.getBytePerSecondsStat()!=null)
                                        Assert.assertTrue(transferID.getBytePerSecondsStat().getNumberOfIdentifiedBytesFromCreationOfTheseStatistics()>=bigDataToSendArray.get().length, ""+transferID.getBytePerSecondsStat().getNumberOfIdentifiedBytesFromCreationOfTheseStatistics());
                                } else if (bdrm.getType() == BigDataResultMessage.Type.TRANSFER_CANCELED) {
                                    tr1 = true;
                                    AssertJUnit.assertTrue(cancelTransfer);
                                    AgentAddress aa = getAgentWithRole(GROUP, ROLE);
                                    Assert.assertNotNull(aa);
                                    sendMessage(aa, new BooleanMessage(true));
                                    m = waitNextMessage();
                                    Assert.assertNotNull(m);
                                    Assert.assertTrue(m instanceof BooleanMessage);
                                    Assert.assertEquals(((BooleanMessage) m).getContent(), Boolean.TRUE);

                                } else
                                    AssertJUnit.fail("" + bdrm.getType());

                                return tr1;
                            } catch (IOException e) {
                                e.printStackTrace();
                                return false;
                            }
                        }

                        @Override
                        protected void liveCycle() throws InterruptedException {
                            requestRole(GROUP, ROLE2);
                            sleep(3000);

                            long delay;
                            if (downloadLimitInBytesPerSecond != Integer.MAX_VALUE || uploadLimitInBytesPerSecond != Integer.MAX_VALUE)
                                delay = Math.max(60000, size / Math.min(downloadLimitInBytesPerSecond, uploadLimitInBytesPerSecond) * 1000 + 20000);
                            else
                                delay = 60000;
                            AgentAddress aa = getAgentWithRole(GROUP, ROLE);
                            if (aa == null)
                                throw new NullPointerException();


                            transfered1.set(sendBigMessage(false, delay));
                            AssertJUnit.assertTrue("", transfered1.get());
                            sleep(1000);
                            transfered2.set(sendBigMessage(true, delay));
                            AssertJUnit.assertTrue("", transfered2.get());
                            this.killAgent(this);
                        }

                        @Override
                        protected void end() {
                            while (nextMessage() != null) ;
                        }
                    };
                    launchThreadedMKNetworkInstance(Level.INFO, AbstractAgent.class, bigDataSenderAgent, eventListener1);
                    sleep(400);
                    BigDataTransferReceiverAgent bigDataTransferAgent = new BigDataTransferReceiverAgent(2, uploadLimitInBytesPerSecond, cancelTransfer, cancelTransferFromReceiver, asynchronousMessage, restartMessage, restartWithLeaveRequestRole, globalDisconnection, transferPaused, pauseAll);
                    launchThreadedMKNetworkInstance(Level.INFO, AbstractAgent.class, bigDataTransferAgent, eventListener2);

                    while (transfered1.get() == null || transfered2.get() == null) {
                        sleep(1000);
                    }
                    bigDataToSendArray.set(null);
                    AssertJUnit.assertTrue(transfered1.get());
                    AssertJUnit.assertTrue(transfered2.get());

                    //noinspection UnusedAssignment
                    bigDataTransferAgent = null;
                    //noinspection UnusedAssignment
                    bigDataSenderAgent = null;
                    for (Madkit mk : getHelperInstances(this, 2))
                        stopNetworkWithMadkit(mk);

                    for (Madkit mk : getHelperInstances(this, 2)) {
                        checkConnectedKernelsNb(this, mk, 0, timeOut);
                        checkConnectedIntancesNb(this, mk, 0, timeOut);
                    }
                    sleep(400);

                    cleanHelperMDKs(this);
                    AssertJUnit.assertEquals(getHelperInstances(this, 0).size(), 0);


                }
            });
            AssertJUnit.assertTrue(transfered1.get());
            AssertJUnit.assertTrue(transfered2.get());
        }
        finally {

            cleanHelperMDKs();
            if(databaseFile1.exists())
                FileTools.deleteDirectory(databaseFile1);
            if(databaseFile2.exists())
                FileTools.deleteDirectory(databaseFile2);
        }

	}
	
	
	

}
