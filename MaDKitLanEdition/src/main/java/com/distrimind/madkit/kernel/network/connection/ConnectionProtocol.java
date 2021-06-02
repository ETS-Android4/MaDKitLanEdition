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
package com.distrimind.madkit.kernel.network.connection;

import com.distrimind.madkit.exceptions.BlockParserException;
import com.distrimind.madkit.exceptions.ConnectionException;
import com.distrimind.madkit.exceptions.NIOException;
import com.distrimind.madkit.exceptions.PacketException;
import com.distrimind.madkit.i18n.ErrorMessages;
import com.distrimind.madkit.kernel.MadkitProperties;
import com.distrimind.madkit.kernel.network.*;
import com.distrimind.madkit.message.hook.HookMessage.AgentActionEvent;
import com.distrimind.ood.database.DatabaseWrapper;
import com.distrimind.util.crypto.AbstractSecureRandom;
import com.distrimind.util.io.Integrity;
import com.distrimind.util.io.MessageExternalizationException;
import com.distrimind.util.io.SecuredObjectInputStream;
import com.distrimind.util.io.SecuredObjectOutputStream;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * Represents a connection protocol that encapsulate lan packets.
 * 
 * Connection protocols can be imbricated.
 * 
 * They can be used for encrypting issue, security issue, or data check issue.
 * 
 * 
 * @author Jason Mahdjoub
 * @version 1.4
 * @since MadkitLanEdition 1.0
 * @param <CP> the connection protocol type
 */
public abstract class ConnectionProtocol<CP extends ConnectionProtocol<CP>> implements Iterable<ConnectionProtocol<?>> {

	
	public enum ConnectionClosedReason {
		/**
		 * The connection has been closed giving a reason
		 */
		CONNECTION_PROPERLY_CLOSED(AgentActionEvent.CONNEXION_PROPERLY_CLOSED),

		/**
		 * The connection has been lost
		 */
		CONNECTION_LOST(AgentActionEvent.CONNEXION_LOST),

		/**
		 * The given ip was not reacheable
		 */
		IP_NOT_REACHED(AgentActionEvent.CONNEXION_LOST),

		/**
		 * The connection encountered an anomaly, which can be security problem. If too
		 * much anomalies has been reached, the distant peer ip will be banned.
		 */
		CONNECTION_ANOMALY(AgentActionEvent.CONNEXION_CLOSED_BECAUSE_OF_NETWORK_ANOMALY);

		private final AgentActionEvent agentAgentEvent;

		ConnectionClosedReason(AgentActionEvent _agentAgentEvent) {
			agentAgentEvent = _agentAgentEvent;
		}

		public AgentActionEvent getAgentActionEvent() {
			return agentAgentEvent;
		}
	}

	public enum ConnectionState {
		NOT_CONNECTED, CONNECTION_ESTABLISHED, CONNECTION_ABORDED, CONNECTION_CLOSED
	}

	protected final InetSocketAddress distant_inet_address;
	protected final InetSocketAddress local_interface_address;
	private ConnectionState connection_state = ConnectionState.NOT_CONNECTED;
	private ConnectionClosedReason connection_close_reason = null;
	private Boolean this_ask_connection = null;
	protected final ConnectionProtocolProperties<?> connection_protocol_properties;
	protected final NetworkProperties network_properties;
	protected final DatabaseWrapper sql_connection;
	protected final ConnectionProtocol<?> subProtocol;
	private boolean connectionFinishedMessageReceived = false;
	private volatile PointToPointTransferedBlockChecker pointToPointTransferedBlockChecker=null;
	protected final boolean isServer;
    protected final boolean mustSupportBidirectionnalConnectionInitiative;
	//private CounterSelector counterSelector=null;

	
	protected ConnectionProtocol(InetSocketAddress _distant_inet_address, InetSocketAddress _local_interface_address,
			ConnectionProtocol<?> _subProtocol, DatabaseWrapper sql_connection, MadkitProperties _properties, ConnectionProtocolProperties<?> cpp,
			int subProtocolLevel, boolean isServer, boolean mustSupportBidirectionnalConnectionInitiative)
			throws ConnectionException {
		if (_distant_inet_address == null)
			throw new NullPointerException("_distant_inet_address");
		if (_properties == null)
			throw new NullPointerException("_properties");
		this.isServer=isServer;
        this.mustSupportBidirectionnalConnectionInitiative=mustSupportBidirectionnalConnectionInitiative;
		distant_inet_address = _distant_inet_address;
		local_interface_address = _local_interface_address;
		subProtocol = _subProtocol;
		network_properties = _properties.networkProperties;
		connection_protocol_properties =  cpp;
		if (mustSupportBidirectionnalConnectionInitiative
				&& !connection_protocol_properties.supportBidirectionnalConnectionInitiative())
			throw new ConnectionException(
					"This connection protocol do not support bi-directionnal connection initiation !");
		this.sql_connection = sql_connection;

	}

	public ConnectionProtocol<?> getSubProtocol() {
		return subProtocol;
	}

	public final boolean needsMadkitLanEditionDatabase()
	{
		return connection_protocol_properties.needsMadkitLanEditionDatabase();
	}

	public DatabaseWrapper getDatabaseWrapper() {
		return sql_connection;
	}

	public ConnectionProtocolProperties<?> getProperties() {
		return connection_protocol_properties;
	}

	/**
	 * 
	 * @return true if the current local server is asking the connection, or false
	 *         if the distant server is asking it.
	 */
	public boolean isCurrentServerAskingConnection() {
		return this_ask_connection;
	}

	/**
	 * This function aims to manage received message from distant client.
	 * 
	 * When the connection have just been established, the first message is
	 * {@link AskConnection}. This first message inform if it is the current local
	 * peer that ask for a connection, or the distant peer. When the connection has
	 * finished its protocol, this function returns {@link ConnectionFinished}
	 * message.
	 * 
	 * This function calls #@link {@link #getNextStep(ConnectionMessage)} function.
	 * 
	 * @param _m
	 *            the distant message to manage
	 * @return a new message to send.
	 * @throws ConnectionException
	 *             if a problem occurs during the message reading
	 * @see #getNextStep(ConnectionMessage)
	 */
	public final ConnectionMessage setAndGetNextMessage(ConnectionMessage _m) throws ConnectionException {
		if (_m instanceof DoubleConnectionMessage)
		{
			DoubleConnectionMessage m=(DoubleConnectionMessage)_m;
			ConnectionMessage rep1=setAndGetNextMessage(m.getMessage1());
			ConnectionMessage rep2=setAndGetNextMessage(m.getMessage2());
			if (rep1==null) {
				return null;
			}
			else if (rep2==null)
				return rep1;
			else
				return new DoubleConnectionMessage(rep1, rep2);

		}
		if (this_ask_connection==null && _m instanceof AskConnection) {
			this_ask_connection = ((AskConnection) _m).isYouAreAsking();
		}
		if (_m instanceof ConnectionFinished) {
			if (((ConnectionFinished) _m).getState().equals(ConnectionState.CONNECTION_ESTABLISHED))
				connectionFinishedMessageReceived = true;
			else {
				ConnectionMessage res = getNextStep(_m);
				if (res instanceof ConnectionFinished) {
					ConnectionFinished cf = (ConnectionFinished) res;
					if (cf.getConnectionClosedReason() == null)
						setConnectionClosed(ConnectionClosedReason.CONNECTION_ANOMALY);
					else
						setConnectionClosed(cf.getConnectionClosedReason());
				} else {
					setConnectionClosed(ConnectionClosedReason.CONNECTION_ANOMALY);
				}
				return res;
			}

		}
		ConnectionMessage res = getNextStep(_m);
		if (res instanceof ConnectionFinished) {
			ConnectionFinished cf = (ConnectionFinished) res;
			connection_state = cf.getState();
			if (cf.getConnectionClosedReason() != null)
				setConnectionClosed(cf.getConnectionClosedReason());
		}
		return res;
	}

	/**
	 * Call the function {@link #closeConnection(ConnectionClosedReason)} and set
	 * the connection closed.
	 * 
	 * @param _reason
	 *            the reason for closing connection
	 * @throws ConnectionException
	 *             if the connection was already closed.
	 * @see ConnectionClosedReason
	 * @see #closeConnection(ConnectionClosedReason)
	 */
	public final void setConnectionClosed(ConnectionClosedReason _reason) throws ConnectionException {
		if (_reason == null)
			throw new NullPointerException("_reason");
		if (connection_state.equals(ConnectionState.CONNECTION_CLOSED))
			throw new ConnectionException("Connection already closed !");
		connection_close_reason = _reason;
		switch (connection_close_reason) {
		case CONNECTION_ANOMALY:
		case CONNECTION_LOST:
			connection_state = ConnectionState.CONNECTION_ABORDED;
			break;
		case CONNECTION_PROPERLY_CLOSED:
			connection_state = ConnectionState.CONNECTION_CLOSED;
			break;
		}

		closeConnection(_reason);
	}

	/**
	 * 
	 * @return the reason used to close the connection or null if the connection was
	 *         not closed.
	 */
	public ConnectionClosedReason getConnectionCloseReason() {
		return connection_close_reason;
	}

	/**
	 * 
	 * @return the state of the connection.
	 * @see ConnectionState
	 */
	public ConnectionState getConnectionState() {
		return connection_state;
	}

	/**
	 * 
	 * @return true if this connection protocol encrypt the data. False else.
	 */
	public final boolean isCrypted()
	{
		return connection_protocol_properties.isEncrypted();
	}

	/**
	 * This function aims to manage received message from distant client.
	 * 
	 * @param _m
	 *            the distant message to manage
	 * @return a new message to send.
	 * @throws ConnectionException
	 *             if a problem occurs during the message reading
	 */
	protected abstract ConnectionMessage getNextStep(ConnectionMessage _m) throws ConnectionException;

	/**
	 * Close the current connection
	 * 
	 * @param _reason
	 *            the reason for closing connection
	 * @throws ConnectionException
	 *             if a problem occurs.
	 * @see ConnectionClosedReason
	 */
	protected abstract void closeConnection(ConnectionClosedReason _reason) throws ConnectionException;

	/**
	 * 
	 * @return a parser that enables from primitive data (bytes) messages to manage.
	 *         If the current connection protocol encrypt the data, that the parser
	 *         will decipher the primitive data.
	 */
	public abstract SubBlockParser getParser();
	
	

	public final PacketPart getPacketPart(Block _block, NetworkProperties properties) throws NIOException {
		if (_block == null)
			throw new NullPointerException("_block");
		if (_block.getTransferID() != -1)
			throw new NIOException("Unexpected exception !");
		
		/*CounterSelector.State state=_block.getCounterState();
		for (Iterator<ConnectionProtocol<?>> it = this.iterator(); it.hasNext(); ) {
			ConnectionProtocol<?> cp=it.next();
			try
			{
				cp.getPacketCounter().selectMyCounters(state);
			}
			catch(PacketException e)
			{
				throw new NIOException("Invalid block with "+cp.getClass(), false, false);
			}
		}*/
		
		SubBlockInfo sbi;
		try {
			sbi = new SubBlockInfo(new SubBlock(_block), true, false);
		} catch (BlockParserException e) {
			throw new NIOException("Unexpected exception", e);
		}
		if (lastSBS==null)
			lastSBS = new SubBlocksStructure(_block, this);
		else
			lastSBS.init(_block, this);
		int i = 0;
		for (Iterator<ConnectionProtocol<?>> it = this.iterator(); it.hasNext(); i++) {
			ConnectionProtocol<?> cp = it.next();
			PacketCounter pc=cp.getPacketCounter();
			if (pc!=null)
			    pc.incrementMyCounters();
			boolean valid;
			boolean candidate_to_ban =false;
			SubBlockParser sbp = cp.getParser();
			
			try {
				sbi = sbp.getSubBlock(sbi.getSubBlock());
				valid = sbi.isValid();
				candidate_to_ban = sbi.isCandidateToBan();

			} catch (BlockParserException e) {
				valid = false;
			}
			if (valid) {
				try {
					sbi = new SubBlockInfo(lastSBS.getSubBlockForChild(sbi.getSubBlock(), i), true, false);
				} catch (BlockParserException e) {
					valid = false;
				}
			}
			if (!valid) {
				throw new NIOException("Invalid block with "+cp.getClass(), false, candidate_to_ban);
			}
		}
		try
		{
			return new PacketPart(sbi.getSubBlock(), properties.maxBufferSize,
				properties.maxRandomPacketValues);
		}
		catch(PacketException e)
		{
			throw new NIOException(e);
		}
	}

	public final void setPointToPointTransferedBlockChecker(PointToPointTransferedBlockChecker v)
	{
		pointToPointTransferedBlockChecker=v;
	}
	public final PointToPointTransferedBlockChecker getPointToPointTransferedBlockChecker()
	{
		return pointToPointTransferedBlockChecker;
	}
	public boolean isConnectionEstablishedForAllSubProtocols() {
		for (ConnectionProtocol<?> cp : this) {
			if (!cp.isConnectionEstablished())
				return false;
		}
		return true;
	}
	public final Block getBlock(WritePacket _packet, int _transfert_type, AbstractSecureRandom random, boolean excludedFromEncryption)
			throws NIOException {
		try {
			PacketPart packet_part = _packet.getNextPart(this);
			if (packet_part == null)
				return null;
			
			//byte counter=getCounterSelector().getNewCounterID();
			
			//SubBlocksStructure sbs = new SubBlocksStructure(packet_part, this);
			/*Block block = new Block(packet_part, sbs, _transfert_type);
			SubBlock subBlock = new SubBlock(block.getBytes(), sbs.initial_packet_offset, sbs.initial_packet_size);*/
			SubBlock subBlock= packet_part.getSubBlock();
			int i = this.sizeOfSubConnectionProtocols();
			for (Iterator<ConnectionProtocol<?>> it = this.reverseIterator(); it.hasNext(); i--) {
				ConnectionProtocol<?> cp = it.next();
				PacketCounter pc=cp.getPacketCounter();
				if (pc!=null)
				    pc.incrementOtherCounters();

				subBlock = lastSBS.getSubBlockForParent(cp.getParser().getParentBlock(subBlock, excludedFromEncryption), i, random);
			}
			PointToPointTransferedBlockChecker ptp=pointToPointTransferedBlockChecker;
			if (ptp!=null)
			{
				subBlock=ptp.prepareBlockToSend(subBlock);
			}
			return new Block(subBlock.getBytes(), subBlock.getSize()+subBlock.getOffset(), lastSBS, _transfert_type);
		} catch (PacketException | BlockParserException e) {
			throw new NIOException(e);
		}
		
	}
	private SubBlocksStructure lastSBS;

	public SubBlock initSubBlock(int packetSize) throws NIOException
	{
		try {
			if (lastSBS==null)
				lastSBS = new SubBlocksStructure(packetSize, this);
			else
				lastSBS.init(packetSize, this);
			Block block = new Block(lastSBS);
			return new SubBlock(block.getBytes(), lastSBS.initial_packet_offset, lastSBS.initial_packet_size);
		} catch (PacketException e) {
			throw new NIOException(e);
		}
	}

	/**
	 * 
	 * @return true if the connection state is not equals to
	 *         {@link ConnectionState#NOT_CONNECTED}.
	 */
	public final boolean hasFinished() {
		return !connection_state.equals(ConnectionState.NOT_CONNECTED);
	}

	/**
	 * 
	 * @return true if the connection state is not equals to
	 *         {@link ConnectionState#CONNECTION_ESTABLISHED}.
	 */
	public final boolean isConnectionEstablished() {
		return connection_state.equals(ConnectionState.CONNECTION_ESTABLISHED) && connectionFinishedMessageReceived;

	}

	public int getMaximumBodyOutputSizeForEncryption(int size) throws BlockParserException
	{
		return connection_protocol_properties.getMaximumBodyOutputSizeForEncryption(size);
	}


	public int getMaximumSizeHead() throws BlockParserException {
		return connection_protocol_properties.getMaximumHeadSize();
	}

	/**
	 * 
	 * @return true if the connection was established but closed for now.
	 */
	@SuppressWarnings("BooleanMethodIsAlwaysInverted")
	public final boolean isConnectionFinishedButClosed() {
		return connection_state.equals(ConnectionState.CONNECTION_ABORDED)
				|| connection_state.equals(ConnectionState.CONNECTION_CLOSED);
	}

	/**
	 * 
	 * @return the distant peer IP
	 */
	public final InetSocketAddress getDistantInetSocketAddress() {
		return distant_inet_address;
	}

	/**
	 * 
	 * @return the local interface address
	 */
	public final InetSocketAddress getLocalInterfaceAddress() {
		return local_interface_address;
	}

	/**
	 * @return an iterator that will parse the current connection protocol and its
	 *         sub protocols
	 */
	public Iterator<ConnectionProtocol<?>> iterator() {
		return new It();
	}

	/**
	 * @param from_it iterator
	 * 
	 * @return an iterator that will parse the current connection protocol and its
	 *         sub protocols
	 */
	public Iterator<ConnectionProtocol<?>> iterator(Iterator<ConnectionProtocol<?>> from_it) {
		return new It(from_it);
	}

	/**
	 * 
	 * @return an iterator that will parse the current connection protocol and its
	 *         sub protocols in a reverse way
	 */
	public Iterator<ConnectionProtocol<?>> reverseIterator() {
		return new ReverseIt();
	}

	/**
	 * @param from_it iterator
	 * @return an iterator that will parse the current connection protocol and its
	 *         sub protocols in a reverse way
	 */
	public Iterator<ConnectionProtocol<?>> reverseIterator(Iterator<ConnectionProtocol<?>> from_it) {
		return new ReverseIt(from_it);
	}

	public TransferedBlockChecker getTransferredBlockChecker() throws ConnectionException {
		return getTransferredBlockChecker(
				(subProtocol == null || this.isCrypted()) ? null : subProtocol.getTransferredBlockChecker());
	}

	protected abstract TransferedBlockChecker getTransferredBlockChecker(TransferedBlockChecker subBlockChercker)
			throws ConnectionException;

	public boolean isTransferBlockCheckerChanged() {
		if (isTransferBlockCheckerChangedImpl())
			return true;
		else if (subProtocol != null)
			return subProtocol.isTransferBlockCheckerChanged();
		else
			return false;
	}

	protected abstract boolean isTransferBlockCheckerChangedImpl();

	public int sizeOfSubConnectionProtocols() {
		if (subProtocol != null)
			return subProtocol.sizeOfSubConnectionProtocols() + 1;
		else
			return 0;
	}

	class It implements Iterator<ConnectionProtocol<?>> {
		protected ConnectionProtocol<?> current = null;

		It() {

		}

		It(Iterator<ConnectionProtocol<?>> from_it) {
			if (from_it instanceof ConnectionProtocol.It) {
				ConnectionProtocol<?>.It fi = (ConnectionProtocol<?>.It) from_it;
				current = fi.current;
			} else if (from_it instanceof ConnectionProtocol.ReverseIt) {
				ConnectionProtocol<?>.ReverseIt rfromit = (ConnectionProtocol<?>.ReverseIt) from_it;

				int pos = rfromit.current_pos - 1;
				if (pos >= rfromit.list.size()) {
					if (rfromit.list.size() > 0)
						current = rfromit.list.get(rfromit.list.size()-1);
					else
						current = null;
				} else if (pos < 0)
					current = null;
				else
					current = rfromit.list.get(pos);
			} else
				throw new IllegalArgumentException(ErrorMessages.INCOMPATIBLE_ITERATOR.toString());
		}

		@Override
		public boolean hasNext() {
			if (current == null)
				return true;
			else
				return current.subProtocol != null;
		}

		@Override
		public ConnectionProtocol<?> next() throws NoSuchElementException {
			if (current == null) {
				current = ConnectionProtocol.this;
			} else
				current = current.subProtocol;
			if (current == null)
				throw new NoSuchElementException(ErrorMessages.ITERATOR_NO_MORE_ELEMENTS.toString());

			return current;
		}

		protected ConnectionProtocol<CP> getConnectionProtocolInstance() {
			return ConnectionProtocol.this;
		}

		@Override
		public void remove() {
			throw new IllegalAccessError();

		}

	}

	class ReverseIt implements Iterator<ConnectionProtocol<?>> {
		protected ArrayList<ConnectionProtocol<?>> list = new ArrayList<>(5);
		protected int current_pos;

		ReverseIt() {
			It it = new It();
			while (it.hasNext())
				list.add(it.next());
			current_pos = list.size();
		}

		ReverseIt(Iterator<ConnectionProtocol<?>> from_it) {

			if (from_it instanceof ConnectionProtocol.It) {
				ConnectionProtocol<?>.It fi = (ConnectionProtocol<?>.It) from_it;
				ConnectionProtocol<?>.ReverseIt it = (ConnectionProtocol<?>.ReverseIt) fi.getConnectionProtocolInstance().reverseIterator();
				list = it.list;
				current_pos = list.size();

				if (fi.current == null)
					current_pos = 0;
				else {
					boolean found = false;
					for (int i = 0; i < list.size(); i++) {
						if (list.get(i) == fi.current) {
							current_pos = i;
							found = true;
							break;
						}
					}
					if (!found)
						throw new IllegalArgumentException(
								ErrorMessages.CONNECTION_PROTOCOL_AND_ITERATOR_NOT_SAME_INSTANCE.toString());
				}
			} else if (from_it instanceof ConnectionProtocol.ReverseIt) {
				ConnectionProtocol<?>.ReverseIt rfromit = (ConnectionProtocol<?>.ReverseIt) from_it;
				current_pos = rfromit.current_pos;
			} else
				throw new IllegalArgumentException(ErrorMessages.INCOMPATIBLE_ITERATOR.toString());
		}

		@Override
		public boolean hasNext() {
			return current_pos > 0;
		}

		@Override
		public ConnectionProtocol<?> next() throws NoSuchElementException {
			try {
				if (hasNext()) {
					return list.get(--current_pos);
				} else
					throw new NoSuchElementException(ErrorMessages.ITERATOR_NO_MORE_ELEMENTS.toString());
			} catch (Exception e) {
				throw new NoSuchElementException(ErrorMessages.ITERATOR_NO_MORE_ELEMENTS.toString());
			}
		}

		/*protected ConnectionProtocol<CP> getConnectionProtocolInstance() {
			return ConnectionProtocol.this;
		}*/

		@Override
		public void remove() {
			throw new IllegalAccessError();

		}

	}

	public static class NullBlockChecker extends TransferedBlockChecker {

		private short headSize;
		@SuppressWarnings("unused")
		NullBlockChecker()
		{
			
		}
		public NullBlockChecker(TransferedBlockChecker _subChecker, boolean isCrypted, short headSize) {
			super(_subChecker, !isCrypted);
			if (headSize < 0)
				throw new IllegalArgumentException("headSize");
			this.headSize = headSize;
		}

		public NullBlockChecker(int recursiveNumber, boolean isCrypted, short headSize) {
			this(recursiveNumber <= 0 ? null : new NullBlockChecker(--recursiveNumber, isCrypted, headSize), isCrypted,
					headSize);
		}

		@Override
		public SubBlockInfo checkSubBlock(SubBlock _block) throws BlockParserException {
			return new SubBlockInfo(
					new SubBlock(_block.getBytes(), _block.getOffset() + headSize, _block.getSize() - headSize), true,
					false);
		}

		@Override
		public int getInternalSerializedSize() {
			return 2;
		}
		
		@Override
		public void readExternal(SecuredObjectInputStream in) throws IOException, ClassNotFoundException {
			super.readExternal(in);
			headSize=in.readShort();
			if (headSize < 0)
				throw new MessageExternalizationException(Integrity.FAIL);
		}

		@Override
		public void writeExternal(SecuredObjectOutputStream oos) throws IOException {
			super.writeExternal(oos);
			oos.writeShort(headSize);
		}
		
		
	}
	
	
	public abstract PacketCounter getPacketCounter();

	/*public CounterSelector getCounterSelector() {
		return counterSelector;
	}

	public void setCounterSelector(CounterSelector counterSelector) {
		this.counterSelector = counterSelector;
		if (this.subProtocol!=null)
			subProtocol.setCounterSelector(counterSelector);
	}*/
	
	
}
