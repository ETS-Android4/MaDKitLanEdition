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

import com.distrimind.madkit.exceptions.NIOException;
import com.distrimind.madkit.exceptions.PacketException;
import com.distrimind.madkit.kernel.TestNGMadkit;
import com.distrimind.madkit.kernel.MadkitProperties;
import com.distrimind.madkit.kernel.network.connection.ConnectionProtocol;
import com.distrimind.madkit.kernel.network.connection.unsecured.UnsecuredConnectionProtocolProperties;
import com.distrimind.util.crypto.AbstractSecureRandom;
import com.distrimind.util.crypto.MessageDigestType;
import com.distrimind.util.crypto.SecureRandomType;
import com.distrimind.util.io.*;
import org.testng.AssertJUnit;
import org.testng.annotations.*;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.util.ArrayList;
import java.util.Random;

/**
 * 
 * @author Jason Mahdjoub
 * @version 1.0
 * @since MadkitLanEdition 1.0
 */
@SuppressWarnings("ResultOfMethodCallIgnored")
public class ReadWritePacketTests extends TestNGMadkit {
	public static final int testsNumber = 300;
	final AbstractSecureRandom rand;
	private final byte[] originalData;
	private byte[] data;
	private File fileInput;
	private File fileOutput;
	private final int idPacket;
	private final ConnectionProtocol<?> connectionProtocol;
	private final DataSocketSynchronizer synchronizer;
	private final SAI socketAgentInterface;

	static class SAI implements DataSocketSynchronizer.SocketAgentInterface {
		private boolean fail = false;
		private boolean received = false;
		private Block originalBlock = null;

		@Override
		public void receivedBlock(Block _block) {
			AssertJUnit.assertArrayEquals(originalBlock.getBytes(), _block.getBytes());
			received = true;
		}

		@Override
		public boolean processInvalidBlock(Exception _e, Block _block, boolean _candidate_to_ban) {
			_e.printStackTrace();
			fail = true;
			AssertJUnit.fail();
			return true;
		}

		@Override
		public boolean isBannedOrDefinitelyRejected() {
			return fail;
		}

		void setOriginalBlock(Block originalBlock) {
			this.originalBlock = originalBlock;
			this.received = false;
		}

		boolean isReceived() {
			return received;
		}

	}

	@DataProvider
	public static Object[][] data() {
		ArrayList<Object[]> res = new ArrayList<>(testsNumber);
		for (int i = 0; i < testsNumber; i++) {
			Object[] o = new Object[1];
			o[0] = getData();

			res.add(o);
		}
		return res.toArray(new Object[res.size()][res.get(0).length]);
	}

	@Factory(dataProvider = "data")
	public ReadWritePacketTests(byte[] data) throws NIOException, UnknownHostException, NoSuchAlgorithmException, NoSuchProviderException {
		this.rand=SecureRandomType.DEFAULT.getSingleton(null);
		idPacket=Math.abs(rand.nextInt());
		this.originalData = data;
		MadkitProperties madkitProperties=new MadkitProperties();
		madkitProperties.networkProperties=new NetworkProperties();
		connectionProtocol = new UnsecuredConnectionProtocolProperties().getConnectionProtocolInstance(
				new InetSocketAddress(InetAddress.getByName("254.168.45.1"), 10),
				new InetSocketAddress(InetAddress.getByName("192.168.0.1"), 10), null, madkitProperties, false,
				false, madkitProperties.networkProperties.encryptionRestrictionForConnectionProtocols);
		synchronizer = new DataSocketSynchronizer();
		socketAgentInterface = new SAI();

	}

	@BeforeMethod
	public void createFile() throws IOException {
		data = originalData.clone();
		fileInput = new File(System.getProperty("java.io.tmpdir"), "TEST_RANDOM_FILE_INPUT_STREAM");
		fileOutput = new File(System.getProperty("java.io.tmpdir"), "TEST_RANDOM_FILE_OUTPUT_STREAM");
		try (FileOutputStream fos = new FileOutputStream(fileInput)) {
			fos.write(data);
		}
	}

	@AfterMethod
	public void deleteFile() {
		fileInput.delete();
	}

	/*
	 * @Test public void testByteArrayInputStream() throws IOException {
	 * testInputStream(true, fileInput); }
	 * 
	 * @Test public void testFileInputStream() throws IOException { if
	 * (originalData.length<10000) testInputStream(false, fileInput); }
	 */

	@Test
	public void testByteArrayOutputInputStream() throws IOException {
		testOutputInputStream(true);
	}

	@Test
	public void testFileOutputStream() throws IOException {
		if (originalData.length < 10000)
			testOutputInputStream(false);
	}


	public void testInputStream(boolean byteArray, byte[] dataIn, File fileInput) throws IOException {
		try (RandomInputStream input = byteArray ? new RandomByteArrayInputStream(dataIn)
				: new RandomFileInputStream(fileInput)) {
			AssertJUnit.assertEquals(data.length, input.length());
			if (byteArray)
				AssertJUnit.assertEquals(data.length, input.available());
			for (int i = 0; i < data.length; i++) {
				AssertJUnit.assertEquals(i, input.currentPosition());
				AssertJUnit.assertEquals(data[i], (byte) input.read());
				AssertJUnit.assertEquals(i + 1, input.currentPosition());

			}
			AssertJUnit.assertEquals(data.length, input.length());
			AssertJUnit.assertEquals(0, input.available());
			AssertJUnit.assertEquals(-1, input.read());
			AssertJUnit.assertEquals(data.length, input.currentPosition());
		}

		try (RandomInputStream input = byteArray ? new RandomByteArrayInputStream(dataIn)
				: new RandomFileInputStream(fileInput)) {
			byte[] data2 = new byte[(int) input.length()];
			input.read(data2);
			AssertJUnit.assertEquals(data.length, data2.length);
			for (int i = 0; i < data.length; i++)
				AssertJUnit.assertEquals(data[i], data2[i]);
			AssertJUnit.assertEquals(data.length, input.length());
			AssertJUnit.assertEquals(0, input.available());
			AssertJUnit.assertEquals(-1, input.read());
			AssertJUnit.assertEquals(data.length, input.currentPosition());
		}

		if (byteArray) {
			try (RandomByteArrayInputStream input = new RandomByteArrayInputStream(dataIn)) {
				byte[] data2 = input.getBytes();
				input.read(data2);
				AssertJUnit.assertEquals(data.length, data2.length);
				for (int i = 0; i < data.length; i++)
					AssertJUnit.assertEquals(data[i], data2[i]);
				AssertJUnit.assertEquals(data.length, input.length());
				AssertJUnit.assertEquals(0, input.available());
				AssertJUnit.assertEquals(-1, input.read());
				AssertJUnit.assertEquals(data.length, input.currentPosition());
			}
		}
		try (RandomInputStream input = byteArray ? new RandomByteArrayInputStream(dataIn)
				: new RandomFileInputStream(fileInput)) {
			for (int i = 0; i < 10; i++) {
				AssertJUnit.assertEquals(i, input.currentPosition());
				AssertJUnit.assertEquals(data[i], (byte) input.read());
				AssertJUnit.assertEquals(i + 1, input.currentPosition());
			}
			input.mark(100);
			for (int i = 10; i < 20; i++) {
				AssertJUnit.assertEquals(i, input.currentPosition());
				AssertJUnit.assertEquals(data[i], (byte) input.read());
				AssertJUnit.assertEquals(i + 1, input.currentPosition());
			}
			input.reset();
			for (int i = 10; i < data.length; i++) {
				AssertJUnit.assertEquals(i, input.currentPosition());
				AssertJUnit.assertEquals(data[i], (byte) input.read());
				AssertJUnit.assertEquals(i + 1, input.currentPosition());
			}
		}
		try (RandomInputStream input = byteArray ? new RandomByteArrayInputStream(dataIn)
				: new RandomFileInputStream(fileInput)) {
			for (int i = 0; i < 10; i++) {
				AssertJUnit.assertEquals(i, input.currentPosition());
				AssertJUnit.assertEquals(data[i], (byte) input.read());
				AssertJUnit.assertEquals(i + 1, input.currentPosition());
			}
			input.mark(10);
			for (int i = 10; i < 21; i++) {
				AssertJUnit.assertEquals(i, input.currentPosition());
				AssertJUnit.assertEquals(data[i], (byte) input.read());
				AssertJUnit.assertEquals(i + 1, input.currentPosition());
			}
			try {
				input.reset();
				noExceptionFailure();
			} catch (IOException ignored) {

			}
		}
	}

	public void testOutputInputStream(boolean byteArray) throws IOException {
		if (fileOutput.exists())
			fileOutput.delete();
		byte[] dataout = null;
		try (RandomOutputStream output = byteArray ? new RandomByteArrayOutputStream()
				: new RandomFileOutputStream(fileOutput)) {
			AssertJUnit.assertEquals(0, output.length());
			output.setLength(data.length);
			AssertJUnit.assertEquals(0, output.currentPosition());
			AssertJUnit.assertEquals(data.length, output.length());

			for (int i = 0; i < data.length; i++) {
				AssertJUnit.assertEquals(i, output.currentPosition());
				output.write(data[i]);
				AssertJUnit.assertEquals(i + 1, output.currentPosition());

			}
			AssertJUnit.assertEquals(data.length, output.length());
			AssertJUnit.assertEquals(data.length, output.currentPosition());
			if (output instanceof RandomByteArrayOutputStream)
				dataout = ((RandomByteArrayOutputStream) output).getBytes();
		}
		testInputStream(byteArray, dataout, fileOutput);
		fileOutput.delete();
		dataout = null;
		try (RandomOutputStream output = byteArray ? new RandomByteArrayOutputStream()
				: new RandomFileOutputStream(fileOutput)) {
			AssertJUnit.assertEquals(0, output.length());
			output.setLength(data.length);
			AssertJUnit.assertEquals(data.length, output.length());
			output.write(data);
			AssertJUnit.assertEquals(data.length, output.length());
			AssertJUnit.assertEquals(data.length, output.currentPosition());
			if (output instanceof RandomByteArrayOutputStream)
				dataout = ((RandomByteArrayOutputStream) output).getBytes();
		}
		testInputStream(byteArray, dataout, fileOutput);
		fileOutput.delete();

	}

	@Test
	public void testReadWritePacket() throws PacketException, NoSuchAlgorithmException, NIOException, NoSuchProviderException, IOException {
		NetworkProperties np=new NetworkProperties();
		np.addConnectionProtocol(new UnsecuredConnectionProtocolProperties());
		ConnectionProtocol<?> conProto=np.getConnectionProtocolInstance(new InetSocketAddress(InetAddress.getLoopbackAddress(), 1000), new InetSocketAddress(InetAddress.getLoopbackAddress(), 1001), null, new MadkitProperties(), true, false);

		testReadWritePacket(conProto, null);
		testReadWritePacket(conProto, MessageDigestType.BC_BLAKE2B_512);
	}

	public void testReadWritePacket(ConnectionProtocol<?> conProto, MessageDigestType messageDigestType)
			throws PacketException, NoSuchAlgorithmException, NIOException, NoSuchProviderException, IOException {
		for (int i = 0; i < 100; i++) {
			byte val = (byte) rand.nextInt(1 << WritePacket.getMaximumLocalRandomValuesBitsNumber());
			AssertJUnit.assertEquals(val,
					WritePacket.decodeLocalNumberRandomVal(WritePacket.encodeLocalNumberRandomVal(val, rand)));
		}
		testReadWritePacket(conProto, PacketPartHead.TYPE_PACKET, (short) 50, (short) 0, 0, data.length, false,
				messageDigestType);
		testReadWritePacket(conProto, PacketPartHead.TYPE_PACKET, (short) 200, (short) 0, 0, data.length, false,
				messageDigestType);
		testReadWritePacket(conProto, PacketPartHead.TYPE_PACKET, (short) 50, (short) 10, 0, data.length, false,
				messageDigestType);
		testReadWritePacket(conProto, PacketPartHead.TYPE_PACKET, (short) 200, (short) 10, 0, data.length, false,
				messageDigestType);
		testReadWritePacket(conProto, PacketPartHead.TYPE_PACKET, (short) 50, (short) 0, 10, data.length - 50, false,
				messageDigestType);
		testReadWritePacket(conProto, PacketPartHead.TYPE_PACKET, (short) 200, (short) 0, 10, data.length - 50, false,
				messageDigestType);
		testReadWritePacket(conProto, PacketPartHead.TYPE_PACKET, (short) 50, (short) 10, 10, data.length - 50, false,
				messageDigestType);
		testReadWritePacket(conProto, PacketPartHead.TYPE_PACKET, (short) 200, (short) 10, 10, data.length - 50, false,
				messageDigestType);
		testReadWritePacket(conProto, PacketPartHead.TYPE_PACKET, (short) 50, (short) 0, 0, data.length, true, messageDigestType);
		testReadWritePacket(conProto, PacketPartHead.TYPE_PACKET, (short) 200, (short) 0, 0, data.length, true,
				messageDigestType);
		testReadWritePacket(conProto, PacketPartHead.TYPE_PACKET, (short) 50, (short) 10, 0, data.length, true,
				messageDigestType);
		testReadWritePacket(conProto, PacketPartHead.TYPE_PACKET, (short) 200, (short) 10, 0, data.length, true,
				messageDigestType);
		testReadWritePacket(conProto, PacketPartHead.TYPE_PACKET, (short) 50, (short) 0, 10, data.length - 50, true,
				messageDigestType);
		testReadWritePacket(conProto, PacketPartHead.TYPE_PACKET, (short) 200, (short) 0, 10, data.length - 50, true,
				messageDigestType);
		testReadWritePacket(conProto, PacketPartHead.TYPE_PACKET, (short) 50, (short) 10, 10, data.length - 50, true,
				messageDigestType);
		testReadWritePacket(conProto, PacketPartHead.TYPE_PACKET, (short) 200, (short) 10, 10, data.length - 50, true,
				messageDigestType);

		testReadWritePacket(conProto, PacketPartHead.TYPE_PACKET, Short.MAX_VALUE *2, (short) 0, 0, data.length, false,
				messageDigestType);
		testReadWritePacket(conProto, PacketPartHead.TYPE_PACKET, Short.MAX_VALUE *2, (short) 0, 0, data.length, false,
				messageDigestType);
		testReadWritePacket(conProto, PacketPartHead.TYPE_PACKET, Short.MAX_VALUE *2, (short) 10, 0, data.length, false,
				messageDigestType);
		testReadWritePacket(conProto, PacketPartHead.TYPE_PACKET, Short.MAX_VALUE *2, (short) 10, 0, data.length, false,
				messageDigestType);
		testReadWritePacket(conProto, PacketPartHead.TYPE_PACKET, Short.MAX_VALUE *2, (short) 0, 10, data.length - 50, false,
				messageDigestType);
		testReadWritePacket(conProto, PacketPartHead.TYPE_PACKET, Short.MAX_VALUE *2, (short) 0, 10, data.length - 50, false,
				messageDigestType);
		testReadWritePacket(conProto, PacketPartHead.TYPE_PACKET, Short.MAX_VALUE *2, (short) 10, 10, data.length - 50,
				false, messageDigestType);
		testReadWritePacket(conProto, PacketPartHead.TYPE_PACKET, Short.MAX_VALUE *2, (short) 10, 10, data.length - 50,
				false, messageDigestType);
		testReadWritePacket(conProto, PacketPartHead.TYPE_PACKET, Short.MAX_VALUE *2, (short) 0, 0, data.length, true,
				messageDigestType);
		testReadWritePacket(conProto, PacketPartHead.TYPE_PACKET, Short.MAX_VALUE *2, (short) 0, 0, data.length, true,
				messageDigestType);
		testReadWritePacket(conProto, PacketPartHead.TYPE_PACKET, Short.MAX_VALUE *2, (short) 10, 0, data.length, true,
				messageDigestType);
		testReadWritePacket(conProto, PacketPartHead.TYPE_PACKET, Short.MAX_VALUE *2, (short) 10, 0, data.length, true,
				messageDigestType);
		testReadWritePacket(conProto, PacketPartHead.TYPE_PACKET, Short.MAX_VALUE *2, (short) 0, 10, data.length - 50, true,
				messageDigestType);
		testReadWritePacket(conProto, PacketPartHead.TYPE_PACKET, Short.MAX_VALUE *2, (short) 0, 10, data.length - 50, true,
				messageDigestType);
		testReadWritePacket(conProto, PacketPartHead.TYPE_PACKET, Short.MAX_VALUE *2, (short) 10, 10, data.length - 50, true,
				messageDigestType);
		testReadWritePacket(conProto, PacketPartHead.TYPE_PACKET, Short.MAX_VALUE *2, (short) 10, 10, data.length - 50, true,
				messageDigestType);

	}

	@Test(enabled = false)
	private void testReadWritePacket(ConnectionProtocol<?> conProto, int _type, int _max_buffer_size, short random_values_size, long _start_position,
			long length, boolean _transfert_as_big_data, MessageDigestType messageDigestType)
			throws PacketException, NoSuchAlgorithmException, NIOException, NoSuchProviderException, IOException {
		WritePacket output = new WritePacket(_type, idPacket, _max_buffer_size, random_values_size, rand,
				new RandomByteArrayInputStream(data), _start_position, length, _transfert_as_big_data,
				messageDigestType);
		AssertJUnit.assertEquals(_transfert_as_big_data, output.concernsBigData());
		AssertJUnit.assertEquals(length, output.getDataLength());
		int messageDigestSize = messageDigestType == null ? 0
				: messageDigestType.getMessageDigestInstance().getDigestLengthInBytes();
		AssertJUnit.assertEquals(length + messageDigestSize, output.getDataLengthWithHashIncluded());
		AssertJUnit.assertEquals(idPacket, output.getID());
		AssertJUnit.assertEquals(0, output.getReadDataLengthIncludingHash());
		AssertJUnit.assertFalse(output.isFinished());
		ReadPacket read = null;
		RandomByteArrayOutputStream outputStream = new RandomByteArrayOutputStream();
		
		
		do {
			PacketPart pp = output.getNextPart(conProto, false);
			assert pp != null;
			AssertJUnit.assertTrue(pp.getHead().isPacketPart());
			AssertJUnit.assertFalse(pp.isReadyToBeRead());
			AssertJUnit.assertTrue(pp.isReadyToBeSent());
			AssertJUnit.assertEquals(pp.getHead().getID(), idPacket);
			AssertJUnit.assertTrue(pp.getHead().isPacketPart());
			if (read == null) {
				AssertJUnit.assertTrue(pp.getHead().isFirstPacketPart());
				AssertJUnit.assertEquals(length, pp.getHead().getTotalLength());
				AssertJUnit.assertEquals(_start_position, pp.getHead().getStartPosition());

			}

			SubBlock bpp = testDataSynchronizer(pp);

			if (!output.isFinished())
				AssertJUnit.assertTrue(bpp.getSize() >= _max_buffer_size);
			pp = new PacketPart(bpp, _max_buffer_size, random_values_size);
			AssertJUnit.assertTrue(pp.isReadyToBeRead());
			AssertJUnit.assertFalse(pp.isReadyToBeSent());
			AssertJUnit.assertTrue(pp.getHead().isPacketPart());
			if (read == null) {
				AssertJUnit.assertTrue(pp.getHead().isFirstPacketPart());

				AssertJUnit.assertEquals(length + messageDigestSize, pp.getHead().getTotalLength());
				AssertJUnit.assertEquals(_start_position, pp.getHead().getStartPosition());

				read = new ReadPacket(pp, outputStream, messageDigestType);
				// Assert.assertEquals(read.getCurrentPosition(), 0);
				AssertJUnit.assertEquals(idPacket, read.getID());
			} else {
				AssertJUnit.assertFalse(pp.getHead().isFirstPacketPart());
				read.readNewPart(pp);
			}
			if (output.isFinished())
				AssertJUnit.assertTrue(read.isFinished());
			else
				AssertJUnit.assertFalse(read.isFinished());
			AssertJUnit.assertFalse(read.isTemporaryInvalid());
			AssertJUnit.assertFalse(read.isInvalid());
			AssertJUnit.assertTrue(read.isValid());

		} while (!output.isFinished());
		AssertJUnit.assertEquals(length + messageDigestSize, output.getReadDataLengthIncludingHash());
		AssertJUnit.assertEquals(length, output.getReadDataLength());
		AssertJUnit.assertTrue(read.isFinished());
		byte[] res = outputStream.getBytes();
		AssertJUnit.assertEquals(_start_position + length, res.length);
		for (int i = 0; i < length; i++)
			AssertJUnit.assertEquals(data[(int) _start_position + i], res[(int) _start_position + i]);
	}

	@Test(enabled = false)
	private SubBlock testDataSynchronizer(PacketPart pp) throws NIOException, PacketException {

		SubBlocksStructure sbs = new SubBlocksStructure(pp, connectionProtocol, false);
		byte[] ppb = pp.getSubBlock().getEncapsulatedBytes();
		byte[] b = new byte[ppb.length + Block.getHeadSize()];
		System.arraycopy(ppb, 0, b, Block.getHeadSize(), ppb.length);
		final Block block = new Block(b, b.length, sbs, -1);
		socketAgentInterface.setOriginalBlock(block);

		synchronizer.receiveData(b, socketAgentInterface);
		AssertJUnit.assertTrue(socketAgentInterface.isReceived());
		return new SubBlock(ppb, 0, ppb.length);
	}

	public static byte[] getData() {
		Random rand = new Random(System.currentTimeMillis());
		byte[] res = new byte[100 + rand.nextInt(1000000)];
		for (int i = 0; i < res.length; i++)
			res[i] = (byte) rand.nextInt();
		return res;
	}
}
