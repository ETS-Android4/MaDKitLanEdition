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
package com.distrimind.madkit.kernel.network.connection.unsecured;

import com.distrimind.madkit.exceptions.BlockParserException;
import com.distrimind.madkit.exceptions.ConnectionException;
import com.distrimind.madkit.kernel.MadkitProperties;
import com.distrimind.madkit.kernel.network.PacketCounter;
import com.distrimind.madkit.kernel.network.SubBlock;
import com.distrimind.madkit.kernel.network.SubBlockInfo;
import com.distrimind.madkit.kernel.network.SubBlockParser;
import com.distrimind.madkit.kernel.network.connection.*;
import com.distrimind.ood.database.DatabaseWrapper;
import com.distrimind.util.io.SecuredObjectInputStream;
import com.distrimind.util.io.SecuredObjectOutputStream;

import java.io.IOException;
import java.net.InetSocketAddress;

/**
 * Represents a connection protocol that do not have any security management and
 * any cryptographic algorithm.
 * 
 * @author Jason Mahdjoub
 * @version 1.2
 * @since MadkitLanEdition 1.0
 */
public class UnsecuredConnectionProtocol extends ConnectionProtocol<UnsecuredConnectionProtocol> {
	private final Parser parser;
	private boolean connected = false;
	private final NullPacketCounter packetCounter=new NullPacketCounter();

	@SuppressWarnings("unused")
	private UnsecuredConnectionProtocol(InetSocketAddress _distant_inet_address,
                                        InetSocketAddress _local_interface_address, ConnectionProtocol<?> _subProtocol,
                                        DatabaseWrapper _sql_connection, MadkitProperties mkProperties, ConnectionProtocolProperties<?> cpp, int subProtocolLevel, boolean isServer,
                                        boolean mustSupportBidirectionalConnectionInitiative
										) throws ConnectionException {
		super(_distant_inet_address, _local_interface_address, _subProtocol, _sql_connection, mkProperties,cpp,
				subProtocolLevel, isServer, mustSupportBidirectionalConnectionInitiative);
		parser = new Parser();
	}




	@Override
	protected ConnectionMessage getNextStep(ConnectionMessage _m) {
		if (!connected) {
			if (_m instanceof AskConnection) {
				if (((AskConnection) _m).isYouAreAsking())
					return new AskConnection(false);
				else {
					connected = true;
					return new ConnectionFinished(getDistantInetSocketAddress(), (byte[])null);
				}
			} else if (_m instanceof ConnectionFinished) {
				ConnectionFinished cf = (ConnectionFinished) _m;
				if (!cf.getState().equals(ConnectionProtocol.ConnectionState.CONNECTION_ESTABLISHED)) {
					if (cf.getState().equals(ConnectionProtocol.ConnectionState.CONNECTION_CLOSED)) {
						return new ConnectionFinished(this.getDistantInetSocketAddress(),
								ConnectionClosedReason.CONNECTION_PROPERLY_CLOSED);
					} else {
						return new ConnectionFinished(this.getDistantInetSocketAddress(),
								ConnectionClosedReason.CONNECTION_LOST);
					}
				} else {
					connected = true;
					return new ConnectionFinished(getDistantInetSocketAddress(), (byte[])null);
				}
			} else {
				return new UnexpectedMessage(getDistantInetSocketAddress());
			}

		} else {
			if (_m instanceof ConnectionFinished) {
				ConnectionFinished cf = (ConnectionFinished) _m;
				if (!cf.getState().equals(ConnectionProtocol.ConnectionState.CONNECTION_ESTABLISHED)) {
					if (cf.getState().equals(ConnectionProtocol.ConnectionState.CONNECTION_CLOSED)) {
						return new ConnectionFinished(this.getDistantInetSocketAddress(),
								ConnectionClosedReason.CONNECTION_PROPERLY_CLOSED);
					} else {
						return new ConnectionFinished(this.getDistantInetSocketAddress(),
								ConnectionClosedReason.CONNECTION_LOST);
					}
				}
				return null;
			} else {
				return new UnexpectedMessage(getDistantInetSocketAddress());
			}
		}
	}

	@Override
	protected void closeConnection(ConnectionClosedReason _reason) {
		connected = false;
	}

	@Override
	public SubBlockParser getParser() {
		return parser;
	}

	@Override
	public TransferedBlockChecker getTransferredBlockChecker(TransferedBlockChecker subBlockChecker) {
		return new BlockChecker(subBlockChecker);
	}

	static class Parser extends SubBlockParser {
		public Parser() throws ConnectionException {
			super(null, null, null, null, null);
		}
		@Override
		public SubBlockInfo getSubBlock(SubBlockInfo subBlockInfo) throws BlockParserException {
			subBlockInfo.set(true, false);
			return subBlockInfo;
		}

		@Override
		public SubBlock getParentBlock(SubBlock _block, boolean excludeFromEncryption) throws BlockParserException {
			return getParentBlockWithNoTreatments(_block);
		}

		@Override
		public int getHeadSize() {
			return 0;
		}

		@Override
		public int getBodyOutputSizeForEncryption(int _size) {
			return _size;
		}

		@Override
		public int getBodyOutputSizeForDecryption(int _size) {
			return _size;
		}


		@Override
		public SubBlockInfo checkIncomingPointToPointTransferredBlock(SubBlock _block) throws BlockParserException {
			return new SubBlockInfo(_block, true, false);
		}

		@Override
		public SubBlock signIfPossibleOutgoingPointToPointTransferredBlock(SubBlock _block) {
			return _block;
		}

		@Override
		public boolean canAvoidSignatureCounter() {
			return true;
		}


	}

	static class BlockChecker extends TransferedBlockChecker {

		protected BlockChecker(TransferedBlockChecker _subChecker) {
			super(_subChecker, true);
		}

		@SuppressWarnings("unused")
		BlockChecker()
		{
			
		}

		@Override
		public SubBlockInfo checkSubBlock(SubBlock _block) throws BlockParserException {
			return new SubBlockInfo(_block, true, false);
		}

		@Override
		public int getInternalSerializedSize() {
			return 0;
		}

		@Override
		public void readExternal(SecuredObjectInputStream in) throws IOException, ClassNotFoundException {
			super.readExternal(in);
		}

		@Override
		public void writeExternal(SecuredObjectOutputStream oos) throws IOException {
			super.writeExternal(oos);
		}


	}

	@Override
	public boolean isTransferBlockCheckerChangedImpl() {
		return false;
	}

	@Override
	public PacketCounter getPacketCounter() {
		return packetCounter;
	}

}
