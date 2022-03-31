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

/**
 * 
 * @author Jason Mahdjoub
 * @version 1.1
 * @since MadkitLanEdition 1.0
 */
public final class SubBlock {
	private byte[] block;
	private int offset;
	private int size;
	
	public SubBlock(Block _block) {
		setBlock(_block);
	}

	public void setBlock(Block _block)
	{
		block = _block.getBytes();
		offset = Block.getHeadSize();
		size = block.length - offset;
	}

	public SubBlock(Block _block, int _offset, int _size) {
		block = _block.getBytes();
		offset = _offset;
		size = _size;
	}

	public void setBlock(byte[] block, int offset, int size)
	{
		this.block=block;
		this.offset=offset;
		this.size=size;
	}
	
	



	public SubBlock(byte[] _block) {
		this(_block, 0, _block.length);
	}

	public SubBlock(byte[] _block, int _offset, int _size) {
		block = _block;
		offset = _offset;
		size = _size;
	}

	public void setOffsetAndSize(int offset, int size)
	{
		this.offset=offset;
		this.size=size;

	}

	public byte[] getBytes() {
		return block;
	}

	public byte[] getEncapsulatedBytes() {
		if (offset == 0 && size == block.length)
			return block;
		else {
			byte[] res = new byte[size];
			System.arraycopy(block, offset, res, 0, size);
			return res;
		}

	}

	public int getOffset() {
		return offset;
	}

	public int getSize() {
		return size;
	}


	@Override
	public String toString() {
		return "SubBlock{" +
				"block.length=" + block.length +
				", offset=" + offset +
				", size=" + size +
				'}';
	}
}
