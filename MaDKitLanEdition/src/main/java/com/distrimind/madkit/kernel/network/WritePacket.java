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
import com.distrimind.madkit.exceptions.UnknownPacketTypeException;
import com.distrimind.madkit.kernel.network.connection.ConnectionProtocol;
import com.distrimind.util.Bits;
import com.distrimind.util.crypto.AbstractMessageDigest;
import com.distrimind.util.crypto.AbstractSecureRandom;
import com.distrimind.util.crypto.MessageDigestType;
import com.distrimind.util.io.RandomFileInputStream;
import com.distrimind.util.io.RandomInputStream;

import java.io.EOFException;
import java.io.IOException;
import java.util.Random;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 
 * @author Jason Mahdjoub
 * @version 1.0
 * @since MadkitLanEdition 1.0
 * @see ReadPacket
 */
public final class WritePacket {
	private final AbstractMessageDigest messageDigest;
	private final byte[] digestResult;
	private int digestResultPos = -1;
	private final RandomInputStream input_stream;
	private final int id_packet;
	private final long start_position;
	private final AtomicLong current_pos = new AtomicLong(0);
	private final int max_buffer_size;
	private final long data_length;
	private final long data_length_with_message_digest;
	private final boolean redownloaded;
	private boolean finished = false;
	private final boolean transfer_as_big_data;
	private final short random_values_size;
	private final AbstractSecureRandom random;

	public WritePacket(int _type, int _id_packet, int _max_buffer_size, short random_values_size, AbstractSecureRandom rand,
			RandomInputStream _input_stream) throws PacketException, IOException {
		this(_type, _id_packet, _max_buffer_size, random_values_size, rand, _input_stream, 0, _input_stream.length());
	}

	public WritePacket(int _type, int _id_packet, int _max_buffer_size, short random_values_size, AbstractSecureRandom rand,
			RandomInputStream _input_stream, MessageDigestType messageDigestType) throws PacketException, IOException {
		this(_type, _id_packet, _max_buffer_size, random_values_size, rand, _input_stream, 0, _input_stream.length(),
				_input_stream instanceof RandomFileInputStream, messageDigestType);
	}

	public WritePacket(int _type, int _id_packet, int _max_buffer_size, short random_values_size, AbstractSecureRandom rand,
			RandomInputStream _input_stream, long _data_length) throws PacketException {
		this(_type, _id_packet, _max_buffer_size, random_values_size, rand, _input_stream, 0, _data_length);
	}

	public WritePacket(int _type, int _id_packet, int _max_buffer_size, short random_values_size, AbstractSecureRandom rand,
			RandomInputStream _input_stream, long _start_position, long length) throws PacketException {
		this(_type, _id_packet, _max_buffer_size, random_values_size, rand, _input_stream, _start_position, length,
				_input_stream instanceof RandomFileInputStream, null);
	}

	public WritePacket(int _type, int _id_packet, int _max_buffer_size, short random_values_size, AbstractSecureRandom rand,
			RandomInputStream _input_stream, long _start_position, long length, boolean _transfer_as_big_data,
			MessageDigestType messageDigestType) throws PacketException {
		if ((_type & PacketPartHead.TYPE_PACKET) != PacketPartHead.TYPE_PACKET)
			throw new UnknownPacketTypeException("The given type is not a packet type (" + _type + ")");
		try {
			if (messageDigestType == null) {
				messageDigest = null;
				digestResult = null;
			} else {
				messageDigest = messageDigestType.getMessageDigestInstance();
				messageDigest.reset();
				digestResult = new byte[messageDigest.getDigestLengthInBytes()];
			}
		} catch (Exception e) {
			throw new PacketException(e);
		}

		redownloaded = (_type & PacketPartHead.TYPE_PACKET_REDOWNLOADED) == PacketPartHead.TYPE_PACKET_REDOWNLOADED;

		input_stream = _input_stream;
		id_packet = _id_packet;
		current_pos.set(start_position=_start_position);
		max_buffer_size = _max_buffer_size;
		this.random_values_size =random_values_size;

		if (this.random_values_size == 0)
			random = null;
		else {
			if (rand == null)
				throw new NullPointerException("rand");
			random = rand;
		}
		try {
			data_length = length;
			data_length_with_message_digest = messageDigest == null ? data_length
					: data_length + messageDigest.getDigestLengthInBytes();
			if (_input_stream.length() - _start_position < data_length)
				throw new PacketException("The given data length (" + data_length + ") cannot be greater than "
						+ (_input_stream.length() - _start_position));
			_input_stream.seek(_start_position);
		} catch (IOException e) {
			throw new PacketException(e);
		}

		transfer_as_big_data = _transfer_as_big_data;
	}

	private static short getMiniRandomValueSize() {
		return 3;
	}

	private static byte getMaximumLocalRandomValues() {
		return 32;
	}

	static byte getMaximumLocalRandomValuesBitsNumber() {
		return 5;
	}

	static byte encodeLocalNumberRandomVal(byte val, Random rand) {
		return (byte) ((rand
				.nextInt(1 << (8 - getMaximumLocalRandomValuesBitsNumber())) << getMaximumLocalRandomValuesBitsNumber())
				| (int) val);
	}

	static byte decodeLocalNumberRandomVal(byte val) {
		return (byte) ((val & 255) & ((1 << getMaximumLocalRandomValuesBitsNumber()) - 1));
	}

	static short getMaximumGlobalRandomValues(int _max_buffer_size) {
		return (short) Math.min(_max_buffer_size / 2, Short.MAX_VALUE);
	}

	public long getDataLengthWithHashIncluded() {
		return data_length_with_message_digest;
	}

	public long getDataLength() {
		return data_length;
	}

	RandomInputStream getInputStream() {
		return input_stream;
	}

	public boolean concernsBigData() {
		return transfer_as_big_data;
	}

	private PacketPartHead setHeadPart(boolean last_packet, AbstractByteTabOutputStream tab) throws EOFException {
		byte type = PacketPartHead.TYPE_PACKET;
		if (current_pos.get() == start_position) {
			if (redownloaded)
				type |= PacketPartHead.TYPE_PACKET_REDOWNLOADED;
			else
				type |= PacketPartHead.TYPE_PACKET_HEAD;
		}
		if (last_packet)
			type |= PacketPartHead.TYPE_PACKET_LAST;
		tab.writeHead(type, this);
		return new PacketPartHead(type, id_packet, tab.getRealDataSizeWithoutPacketHeadSize(), data_length,
				start_position);
	}

	public int getID() {
		return id_packet;
	}

	public boolean isFinished() {
		return finished;
	}

	public long getReadDataLengthIncludingHash() {
		return current_pos.get() - start_position;
	}

	public long getReadDataLength() {
		return Math.min(current_pos.get() - start_position, data_length);
	}

	public PacketPart getNextPart(ConnectionProtocol<?> conProto) throws PacketException, NIOException {
		try {
			if (finished)
				return null;
			boolean first_packet = (current_pos.get() == start_position);
			int headSize = PacketPartHead.getHeadSize(first_packet);
			long dataSizeRemaining=data_length - (current_pos.get() - start_position);
			AbstractByteTabOutputStream res = getByteTabOutputStream(conProto,
					data_length + start_position <= current_pos.get() ? null : messageDigest, max_buffer_size, headSize,
					data_length_with_message_digest - (current_pos.get() - start_position), (conProto.isCrypted() && dataSizeRemaining<max_buffer_size)?random_values_size:0, random);
			boolean last_packet = (current_pos.get()
					+ res.getRealDataSizeWithoutPacketHeadSize()) == (data_length_with_message_digest + start_position);
			PacketPartHead pph = setHeadPart(last_packet, res);

			int currentPacketDataSize = (int) Math.max(
					Math.min(res.getRealDataSizeWithoutPacketHeadSize(),dataSizeRemaining ),
					0);

			// byte[] res=new byte[PacketPartHead.getHeadSize(first_packet)+size];
			if (currentPacketDataSize > 0) {
				// int offset=PacketPartHead.getHeadSize(first_packet);
				int read_data = res.writeData(input_stream, currentPacketDataSize);
				if (read_data != currentPacketDataSize)
					throw new IllegalAccessError("Illegal wrote data quantity : wrote=" + read_data + ", expected="
							+ currentPacketDataSize);
				// int read_data=input_stream.readFully(res, offset, size);

				current_pos.addAndGet(read_data);

			}
			if (messageDigest != null) {
				if (current_pos.get() == data_length + start_position && digestResultPos < 0) {
					int dl = messageDigest.digest(digestResult, 0, digestResult.length);
					if (dl != digestResult.length)
						throw new IllegalAccessError("Invalid signature size !");
					digestResultPos = 0;
					res.disableMessageDigest();
				}

				if (current_pos.get() >= data_length + start_position) {
					if (digestResultPos < 0)
						throw new IllegalAccessError();
					int currentDigestSize = res.getRealDataSizeWithoutPacketHeadSize() - currentPacketDataSize;
					if (currentDigestSize > 0) {
						int read_data = res.writeData(digestResult, digestResultPos, currentDigestSize);
						if (read_data != currentDigestSize)
							throw new IllegalAccessError("Illegal wrote hash data quantity : wrote=" + read_data
									+ ", expected=" + currentDigestSize);
						digestResultPos += read_data;
						current_pos.addAndGet(read_data);
					}

				}
			}
			if (current_pos.get() == data_length_with_message_digest + start_position) {
				finished = true;
			}
			res.finalizeTab();
			int totalDataLength = res.getWrittenData() - headSize;
			if (totalDataLength != res.getRealDataSizeWithoutPacketHeadSize())
				throw new IllegalAccessError("The length returned by the input stream (" + totalDataLength
						+ ") does not corresponds to the effective contained data ("
						+ res.getRealDataSizeWithoutPacketHeadSize() + ").");

			if (current_pos.get() > data_length_with_message_digest + start_position) {
				finished = true;
				throw new IllegalAccessError(
						"The length returned by the input stream does not corresponds to the effective contained data.");
			}

			return new PacketPart(res.getSubBlock(), pph);
		} catch (IOException | IllegalAccessError  e) {
			throw new PacketException(e);
		}
	}

	protected static abstract class AbstractByteTabOutputStream {
		protected AbstractMessageDigest messageDigest;

		protected AbstractByteTabOutputStream(AbstractMessageDigest messageDigest) {
			this.messageDigest = messageDigest;
		}

		//abstract boolean writeData(byte d);

		abstract int writeData(byte[] d, int offset, int size);

		abstract int writeData(RandomInputStream is, int size) throws IOException;

		abstract void finalizeTab();

		abstract SubBlock getSubBlock();


		abstract int getRealDataSizeWithoutPacketHeadSize();

		

		private static final byte[] tmpByteTab=new byte[24];

		void writeHead(byte type, WritePacket wp)  {
			synchronized(tmpByteTab)
			{
				tmpByteTab[0]=type;
				Bits.putInt(tmpByteTab, 1, wp.id_packet);
				Bits.putUnsignedInt24Bits(tmpByteTab, 5, getRealDataSizeWithoutPacketHeadSize());
				if (wp.current_pos.get() == wp.start_position) {
					Bits.putLong(tmpByteTab, 8, wp.data_length_with_message_digest);
					Bits.putLong(tmpByteTab, 16, wp.start_position);
					writeData(tmpByteTab, 0, 24);
				}
				else
					writeData(tmpByteTab, 0, 8);
			}
		}
		
		void disableMessageDigest() {
			messageDigest = null;
		}

		abstract int getWrittenData();

	}

	static AbstractByteTabOutputStream getByteTabOutputStream(ConnectionProtocol<?> conProto, AbstractMessageDigest messageDigest,
			int max_buffer_size, int packet_head_size, long _data_remaining, short random_values_size, AbstractSecureRandom rand) throws NIOException {
		if (random_values_size<0)
			throw new NullPointerException();
		if (_data_remaining<=0)
			throw new IllegalArgumentException();
		if (random_values_size == 0)
			return new ByteTabOutputStream(conProto, messageDigest, max_buffer_size, packet_head_size, _data_remaining);
		else
			return new ByteTabOutputStreamWithRandomValues(conProto, messageDigest, max_buffer_size, packet_head_size,
					_data_remaining, _data_remaining>max_buffer_size?0:random_values_size, rand);
	}
	
	

	protected static class ByteTabOutputStream extends AbstractByteTabOutputStream {
		private final byte[] tab;
		private final SubBlock subBlock;
		private int cursor;
		private final int realDataSize_WithoutHead;

		ByteTabOutputStream(ConnectionProtocol<?> connectionProtocol, AbstractMessageDigest messageDigest, int max_buffer_size, int packet_head_size,
				long _data_remaining) throws NIOException {
			super(messageDigest);
			/*
			 * realDataSize_WithoutHead=(short)Math.min(_data_remaining, max_buffer_size);
			 * int size=(int)(realDataSize_WithoutHead+packet_head_size); tab=new
			 * byte[size];
			 */
			realDataSize_WithoutHead = (int)Math.min(_data_remaining, max_buffer_size);
			int packetSize=packet_head_size + realDataSize_WithoutHead+1;
			subBlock=connectionProtocol.initSubBlock(packetSize);
			//tab = new byte[size];
			tab=subBlock.getBytes();
			cursor = subBlock.getOffset();
			tab[cursor++]=0;
		}
		static int getMaxOutputSize(int max_buffer_size, int packet_head_size)
		{
			return max_buffer_size+packet_head_size+1;
		}


		@Override
		int getWrittenData() {
			return cursor-subBlock.getOffset()-1;
		}

		@Override
		int writeData(byte[] _d, int _offset, int size) {
			System.arraycopy(_d, _offset, tab, cursor, size);
			if (messageDigest != null)
				messageDigest.update(_d, _offset, size);
			cursor += size;
			return size;
		}

		@Override
		int writeData(RandomInputStream _is, int _size) throws IOException {
			_is.readFully(tab, cursor, _size);
			if (messageDigest != null)
				messageDigest.update(tab, cursor, _size);
			cursor += _size;
			return _size;
		}

		@Override
		void finalizeTab() {
		}


		@Override
		SubBlock getSubBlock()
		{
			return subBlock;
		}
		

		@Override
		int getRealDataSizeWithoutPacketHeadSize() {
			return realDataSize_WithoutHead;
		}


	}

	protected static class ByteTabOutputStreamWithRandomValues extends AbstractByteTabOutputStream {
		private final SubBlock subBlock;
		private final AbstractSecureRandom random;
		private final byte[] tab;
		private short random_values_size_remaining;
		private int cursor;
		private int nextRandValuePos;
		private final int realDataSize_WithoutHead;
		private int randomValuesWritten = 0;
		private final int shiftedTabLength;

		ByteTabOutputStreamWithRandomValues(ConnectionProtocol<?> conProto, AbstractMessageDigest messageDigest, int max_buffer_size,
				int packet_head_size, long _data_remaining, short max_random_values_size, AbstractSecureRandom rand) throws NIOException {
			super(messageDigest);

			this.random = rand;
			short min = getMiniRandomValueSize();
			short random_values_size;
			if (max_random_values_size >= min)
				random_values_size = (short)(min + rand.nextInt(
						Math.min(getMaximumGlobalRandomValues(max_buffer_size), max_random_values_size) - min +1));
			else
				random_values_size = min;
			random_values_size_remaining = random_values_size;
			/*
			 * int size=(int)(Math.min(_data_remaining, max_buffer_size)+packet_head_size);
			 * tab=new byte[(int)Math.max(Math.min(size, max_buffer_size),
			 * Math.min(size+this.random_values_size, max_buffer_size))];
			 * realDataSize_WithoutHead=(short)(tab.length-this.random_values_size-
			 * packet_head_size); data_size=(short)(tab.length-this.random_values_size);
			 */
			int size = (int) (Math.min(_data_remaining, max_buffer_size));
			int packetSize=size + packet_head_size + random_values_size+1;
			subBlock=conProto.initSubBlock(packetSize);
			//tab = new byte[size + packet_head_size + this.random_values_size];
			tab=subBlock.getBytes();
			realDataSize_WithoutHead = size;
			cursor = subBlock.getOffset();
			nextRandValuePos = cursor+1;
			shiftedTabLength=subBlock.getOffset()+subBlock.getSize();
			tab[cursor++]=1;
		}
		
		static int getMaxOutputSize(int max_buffer_size, short max_random_values_size, int packet_head_size)
		{
			return max_buffer_size+Math.min(getMaximumGlobalRandomValues(max_buffer_size), max_random_values_size)+packet_head_size+1;
		}

		@Override
		int getWrittenData() {
			return cursor - randomValuesWritten - subBlock.getOffset()-1;
		}

		@Override
		int writeData(byte[] d, int offset, int size) {
			int total = 0;
			while (size > 0) {
				int length = Math.min(nextRandValuePos - cursor, size);
				if (length > 0) {
					System.arraycopy(d, offset, tab, cursor, length);
					if (messageDigest != null)
						messageDigest.update(tab, cursor, length);
					offset += length;
					cursor += length;
					size -= length;
					total += length;
					writeRandomValues();
				} else if (cursor >= shiftedTabLength)
					return total;
				else
					writeRandomValues();
			}
			return total;
		}

		@Override
		int writeData(RandomInputStream is, int size) throws IOException {
			int total = 0;
			while (size > 0) {
				int length = Math.min(nextRandValuePos - cursor, size);

				if (length > 0) {
					is.readFully(tab, cursor, length);
					if (messageDigest != null)
						messageDigest.update(tab, cursor, length);

					cursor += length;
					total += length;
					size -= length;
					writeRandomValues();
				} else if (cursor >= shiftedTabLength)
					return total;
				else
					writeRandomValues();
			}
			return total;
		}

		private void writeRandomValues() {
			if (cursor == nextRandValuePos) {
				random_values_size_remaining = (short) Math.min(shiftedTabLength - cursor, random_values_size_remaining);
				if (random_values_size_remaining < getMiniRandomValueSize()) {
					random_values_size_remaining = 0;
					nextRandValuePos = shiftedTabLength;
					return;
				}

				short nbRandMax = (short) Math.min(random_values_size_remaining - getMiniRandomValueSize() + 1,
						getMaximumLocalRandomValues() - 1);
				byte nbRand = (byte) (random.nextInt(nbRandMax) + 1);
				byte[] tabRand = new byte[nbRand];
				random.nextBytes(tabRand);
				byte nextRand = -1;
				if (random_values_size_remaining - getMiniRandomValueSize() * 2 + 1 - nbRand >= 0)
					nextRand = (byte) (random.nextInt(64) + 64);
				tab[cursor++] = encodeLocalNumberRandomVal(nbRand, random);
				for (byte aTabRand : tabRand) tab[cursor++] = aTabRand;
				tab[cursor++] = nextRand;
				randomValuesWritten += 2 + tabRand.length;
				if (nextRand == -1)
					nextRandValuePos = shiftedTabLength;
				else
					nextRandValuePos = cursor + nextRand;
				random_values_size_remaining -= (nbRand + 2);
			}
		}

		@Override
		void finalizeTab() {
			byte[] b = new byte[shiftedTabLength - cursor];
			random.nextBytes(b);
			System.arraycopy(b, 0, tab, cursor, b.length);
		}

		@Override
		SubBlock getSubBlock()
		{
			return subBlock;
		}

		@Override
		int getRealDataSizeWithoutPacketHeadSize() {
			return this.realDataSize_WithoutHead;
		}

	}

	
}
