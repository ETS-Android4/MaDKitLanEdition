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

import java.io.IOException;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.channels.spi.SelectorProvider;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;

import com.distrimind.madkit.agr.LocalCommunity;
import com.distrimind.madkit.exceptions.ConnectionException;
import com.distrimind.madkit.exceptions.MadkitException;
import com.distrimind.madkit.exceptions.OverflowException;
import com.distrimind.madkit.exceptions.PacketException;
import com.distrimind.madkit.exceptions.SelfKillException;
import com.distrimind.madkit.exceptions.TransfertException;
import com.distrimind.madkit.kernel.AbstractAgent;
import com.distrimind.madkit.kernel.Agent;
import com.distrimind.madkit.kernel.AgentAddress;
import com.distrimind.madkit.kernel.AgentNetworkID;
import com.distrimind.madkit.kernel.Group;
import com.distrimind.madkit.kernel.Message;
import com.distrimind.madkit.kernel.NetworkAgent;
import com.distrimind.madkit.kernel.Task;
import com.distrimind.madkit.kernel.network.AbstractData.DataTransferType;
import com.distrimind.madkit.kernel.network.TransferAgent.IDTransfer;
import com.distrimind.madkit.kernel.network.TransferAgent.TryDirectConnection;
import com.distrimind.madkit.kernel.network.connection.ConnectionProtocol.ConnectionClosedReason;
import com.distrimind.madkit.message.ObjectMessage;
import com.distrimind.madkit.util.concurrent.LockerCondition;
import com.distrimind.util.Timer;

/**
 * Represent an server socket selector for {@link SocketChannel}, and
 * {@link DatagramChannel}
 * 
 * @author Jason Mahdjoub
 * @version 1.0
 * @since MadkitLanEdition 1.0
 */
@SuppressWarnings({"unused", "UnusedReturnValue"})
final class NIOAgent extends Agent {



	/*
	 * //the The host:port combination to listen on private final InetAddress
	 * hostAddress; private final int port;
	 */

	protected static class Server {
		protected final ServerSocketChannel serverChannels;
		protected final InetSocketAddress address;

		protected Server(ServerSocketChannel _serverChannels, InetSocketAddress _address) {
			serverChannels = _serverChannels;
			address = _address;
		}

		@Override
		public String toString() {
			return "ServerSocketChannel[server=" + serverChannels + ", address=" + address + "]";
		}

		public boolean isConcernedBy(NetworkInterface ni) {
			for (Enumeration<InetAddress> e = ni.getInetAddresses(); e.hasMoreElements();)
				if (e.nextElement().equals(this.address.getAddress()))
					return true;
			return false;
		}
	}

	// The channel on which we'll accept connections
	private final ArrayList<Server> serverChannels = new ArrayList<>();

	// The selector we'll be monitoring
	protected final Selector selector;

	// The buffer into which we'll read data when it's available
	

	protected HashMap<AgentNetworkID, PersonalSocket> personal_sockets = new HashMap<>();
	protected ArrayList<PersonalSocket> personal_sockets_list = new ArrayList<>(100);

	protected HashMap<DatagramChannel, PersonalDatagramChannel> personal_datagram_channels = new HashMap<>();
	protected HashMap<AgentAddress, PersonalDatagramChannel> personal_datagram_channels_per_agent_address = new HashMap<>();
	protected HashMap<InetAddress, PersonalDatagramChannel> personal_datagram_channels_per_ni_address = new HashMap<>();
	protected HashMap<InetAddress, Integer> numberOfConnectionsPerIP = new HashMap<>();
	protected final ArrayList<PendingConnection> pending_connections = new ArrayList<>();

	protected Group group = null;
	private boolean stoping = false;
	protected AgentAddress myAgentAddress = null;
	protected long localOnlineTime = -1;
	protected long delayToWaitToRespectGlobalBandwidthLimit=0;
    private RealTimeTransfertStat realTimeGlobalDownloadStat =null, realTimeGlobalUploadStat=null;
    private double realTimeDownloadStatDuration=0.0, realTimeUploadStatDuration=0.0;

	NIOAgent() throws ConnectionException {
		/*
		 * hostAddress=_hostAddress; port=_port;
		 */

		try {
			selector = SelectorProvider.provider().openSelector();

			/*
			 * // Create a new non-blocking server socket channel serverChannel =
			 * ServerSocketChannel.open(); serverChannel.configureBlocking(false);
			 * 
			 * // Bind the server socket to the specified address and port InetSocketAddress
			 * isa = new InetSocketAddress(this.hostAddress, this.port);
			 * 
			 * serverChannel.socket().bind(isa); // Register the server socket channel,
			 * indicating an interest in // accepting new connections
			 * serverChannel.register(selector, SelectionKey.OP_ACCEPT);
			 */

		} catch (IOException e) {
			throw new ConnectionException(e);
		}

	}

	private boolean addIfPossibleNewConnectedIP(InetAddress inetAddress) {
		if (inetAddress == null)
			throw new NullPointerException("inetAddress");
		Integer i = numberOfConnectionsPerIP.get(inetAddress);
		int nb = i == null ? 0 : i;

		if ((inetAddress instanceof Inet4Address)
				&& nb + 1 > getMadkitConfig().networkProperties.numberOfMaximumConnectionsFromOneIPV4) {
			return false;
		} else if ((inetAddress instanceof Inet6Address)
				&& nb + 1 > getMadkitConfig().networkProperties.numberOfMaximumConnectionsFromOneIPV6) {
			return false;
		}
		numberOfConnectionsPerIP.put(inetAddress, nb + 1);
		return true;
	}

	private void removeConnectedIP(InetAddress inetAddress) {
		if (inetAddress == null)
			throw new NullPointerException("inetAddress");

		Integer i = numberOfConnectionsPerIP.remove(inetAddress);
		int nb = i == null ? 0 : i;
		--nb;
		if (nb > 0)
			numberOfConnectionsPerIP.put(inetAddress, nb);
	}

	@Override
	protected void activate() {
		setLogLevel(getMadkitConfig().networkProperties.networkLogLevel);
		if (logger != null && logger.isLoggable(Level.FINE))
			logger.fine("Launching NIOAgent ...");
		localOnlineTime = System.currentTimeMillis();
        this.realTimeGlobalDownloadStat =getMadkitConfig().networkProperties.getGlobalStatsBandwidth().getBytesDownloadedInRealTime(NetworkProperties.DEFAULT_TRANSFER_STAT_IN_REAL_TIME_PER_ONE_SECOND_SEGMENTS);
        this.realTimeGlobalUploadStat=getMadkitConfig().networkProperties.getGlobalStatsBandwidth().getBytesUploadedInRealTime(NetworkProperties.DEFAULT_TRANSFER_STAT_IN_REAL_TIME_PER_ONE_SECOND_SEGMENTS);
        this.realTimeDownloadStatDuration=((double)this.realTimeGlobalDownloadStat.getDurationMilli())/1000.0;
        this.realTimeUploadStatDuration=((double)this.realTimeGlobalUploadStat.getDurationMilli())/1000.0;
		this.requestRole(LocalCommunity.Groups.NETWORK, LocalCommunity.Roles.NIO_ROLE);
		this.requestRole(LocalCommunity.Groups.LOCAL_NETWORKS, LocalCommunity.Roles.NIO_ROLE);
		myAgentAddress = this.getAgentAddressIn(LocalCommunity.Groups.NETWORK, LocalCommunity.Roles.NIO_ROLE);

		if (logger != null && logger.isLoggable(Level.FINE))
			logger.fine("NIOAgent LAUNCHED !");
	}

	@SuppressWarnings("unchecked")
	private void closeAllNow() {
		if (logger != null && logger.isLoggable(Level.FINER))
			logger.finer("Closing all sockets !");

		for (PendingConnection pc : pending_connections) {
			try {
				if (pc.getSocketChannel().isOpen())
					pc.getSocketChannel().close();
			} catch (Exception ignored) {

			}
		}

		pending_connections.clear();

		for (PersonalSocket ps : ((ArrayList<PersonalSocket>) this.personal_sockets_list.clone()))
			if (!ps.isClosed())
				ps.closeConnection(ConnectionClosedReason.CONNECTION_LOST);

		this.personal_sockets.clear();
		this.personal_sockets_list.clear();


		if (logger != null)
			logger.finer("Closing all datagram channels !");
		for (Object o : this.personal_datagram_channels.values().toArray()) {
			PersonalDatagramChannel dc = (PersonalDatagramChannel) o;
			dc.closeConnection(false);
		}
		this.personal_datagram_channels.clear();
		this.personal_datagram_channels_per_agent_address.clear();
		this.personal_datagram_channels_per_ni_address.clear();

		if (logger != null)
			logger.finer("Closing all servers !");

		for (Server s : this.serverChannels) {
			try {
				if (s.serverChannels.isOpen())
					s.serverChannels.close();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		this.serverChannels.clear();
		if (logger != null)
			logger.finer("Closing selector !");
		if (this.selector.isOpen()) {
			try {
				this.selector.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

	}

	private long getDownloadToWaitInMsToBecomeUnderBandwidthLimit()
    {
        long res=0;
        long max=getMaximumGlobalDownloadSpeedInBytesPerSecond();
        long limit=max*2L;
        if (max!=Integer.MAX_VALUE)
        {
            @SuppressWarnings("UnnecessaryLocalVariable") double l=limit;

            if (realTimeGlobalDownloadStat.isOneCycleDone()) {
                double v = ((double) realTimeGlobalDownloadStat.getNumberOfIndentifiedBytes()) / realTimeDownloadStatDuration ;
                if (v >= l) {
                    res = (long) ((v - l) / l * 1000.0);
                }
            }
        }

        return res;
    }

    private long getUploadToWaitInMsToBecomeUnderBandwidthLimit()
    {
        long res=0;
        long max=getMaximumGlobalUploadSpeedInBytesPerSecond();
        long limit=max*2L;

        if (max!=Integer.MAX_VALUE)
        {
            @SuppressWarnings("UnnecessaryLocalVariable") double l=limit;

            if (realTimeGlobalUploadStat.isOneCycleDone()) {

                double v = ((double) realTimeGlobalUploadStat.getNumberOfIndentifiedBytes()) / realTimeUploadStatDuration ;
                if (v>=l)
                {
                    res=(long)((v-l)/l*1000.0);
                }

            }

        }


        return res;
    }



	@SuppressWarnings("deprecation")
	@Override
	public void finalize() {
		closeAllNow();
	}

	@Override
	protected void end() {
		closeAllNow();

		if (logger != null && logger.isLoggable(Level.INFO))
			logger.info("NIOAgent KILLED !");

	}

	@Override
	public Message receiveMessage(Message _m) {
		_m = super.receiveMessage(_m);
		this.selector.wakeup();
		return _m;
	}

	@SuppressWarnings({"SuspiciousMethodCalls"})
    @Override
	protected void liveCycle() {
		try {
			// Wait for an event one of the registered channels
			long delay;
			if (pending_connections.size() > 0)
				delay = getMadkitConfig().networkProperties.selectorTimeOutWhenWaitingPendingConnections;
			else {
                delay = getMadkitConfig().networkProperties.selectorTimeOut;

                if (delayToWaitToRespectGlobalBandwidthLimit > 0) {

                    delayToWaitToRespectGlobalBandwidthLimit -= System.currentTimeMillis();

                    if (delayToWaitToRespectGlobalBandwidthLimit > 0) {
                        this.sleep(delayToWaitToRespectGlobalBandwidthLimit);

                        delay -= delayToWaitToRespectGlobalBandwidthLimit;
                        if (delay < 0)
                            delay = 0;
                        delayToWaitToRespectGlobalBandwidthLimit = 0;
                    }
                }
            }
            if (delay>0)
                this.selector.select(delay);

			ArrayList<PersonalSocket> connections_to_close = new ArrayList<>();
			for (PersonalSocket ps : personal_sockets_list) {
				if (ps.isWaitingForPongMessage()) {
					if (System.currentTimeMillis()
							- ps.getTimeSendingPingMessage() > getMadkitConfig().networkProperties.connectionTimeOut) {
						if (logger != null && logger.isLoggable(Level.FINER))
							logger.finer("Pong not received, closing connection : " + ps);

						connections_to_close.add(ps);
					}
				} else {
					StatsBandwidth sb = ps.agentSocket.getStatistics();
					RealTimeTransfertStat upload = sb.getBytesUploadedInRealTime(
							NetworkProperties.DEFAULT_TRANSFER_STAT_IN_REAL_TIME_PER_30_SECONDS_SEGMENTS);
					boolean ping = false;
					if (ps.hasDataToSend() && upload.getNumberOfIndentifiedBytes() == 0 && System.currentTimeMillis()
							- ps.getLastDataWritedUTC() > getMadkitConfig().networkProperties.connectionTimeOut) {

						ping = true;
					} else {
						RealTimeTransfertStat download = sb.getBytesDownloadedInRealTime(
								NetworkProperties.DEFAULT_TRANSFER_STAT_IN_REAL_TIME_PER_30_SECONDS_SEGMENTS);

						if (download.getNumberOfIndentifiedBytes() == 0 && upload.getNumberOfIndentifiedBytes() == 0
								&& download.isOneCycleDone() && upload.isOneCycleDone()) {
							ping = true;
						} else if (sb
								.getBytesDownloadedInRealTime(
										NetworkProperties.DEFAULT_TRANSFER_STAT_IN_REAL_TIME_PER_5_MINUTES_SEGMENTS)
								.getNumberOfIndentifiedBytes() == 0
								&& System.currentTimeMillis() - ps
										.getLastDataWritedUTC() > getMadkitConfig().networkProperties.connectionTimeOut) {
							ping = true;
						}

					}
					if (ping) {
						if (logger != null && logger.isLoggable(Level.FINEST))
							logger.finest("Sending ping message : " + ps.socketChannel.getRemoteAddress());

						ps.sendPingMessage();
					}
				}
			}
			if (pending_connections.size() > 0) {
				ArrayList<PendingConnection> pendingConnectionsFinished = new ArrayList<>();
				ArrayList<PendingConnection> pendingConnectionsCanceled = new ArrayList<>();

				for (PendingConnection pc : pending_connections) {
					try {
						if (pc.getSocketChannel().isConnectionPending() && pc.getSocketChannel().finishConnect())
							pendingConnectionsFinished.add(pc);
						else if (pc.getTimeUTC() + getMadkitConfig().networkProperties.connectionTimeOut < System
								.currentTimeMillis()) {
							pendingConnectionsCanceled.add(pc);
						}
					} catch (ConnectException e) {
						if (logger != null)
							logger.info(e.toString() + ", local_insterface=" + pc.local_interface + ", ip="
									+ pc.getInetSocketAddress());
						pendingConnectionsCanceled.add(pc);
					}
				}
				for (PendingConnection pc : pendingConnectionsFinished)
					finishConnection(pc.getIP(), pc.getSocketChannel());
				for (PendingConnection pc : pendingConnectionsCanceled)
					pendingConnectionFailed(pc.getSocketChannel(), null);
			}
			for (PersonalSocket ps : connections_to_close) {
				if (logger != null && logger.isLoggable(Level.FINER))
					logger.finer("Closing pending connection");
				ps.closeConnection(ConnectionClosedReason.CONNECTION_LOST);
			}

			// checking messages

			Message m = null;
			if (!this.isMessageBoxEmpty())
				m = nextMessage();
			while (m != null) {
				if (m instanceof DataToSendMessage) {
					DataToSendMessage dtsm = (DataToSendMessage) m;
					PersonalSocket ps = personal_sockets.get(dtsm.socket);
					if (ps != null)
						ps.addDataToSend(dtsm.data);
					else
					{
						sendReply(m, new ConnectionClosed(dtsm.socket, ConnectionClosedReason.CONNECTION_LOST, null,
								null, null));

						if (logger != null)
							logger.warning("Receiving data to send, but personnal socket not found ! ");
					}

				} else if (m instanceof ConnectionClosed) {
					if (logger != null && logger.isLoggable(Level.FINER))
						logger.finer("Receiving : " + m);
					ConnectionClosed cc = (ConnectionClosed) m;
					PersonalSocket ps = personal_sockets.get(cc.socket);
					if (ps != null)
						ps.closeConnection(cc.reason);
				} else if (m.getClass() == AbstractAgentSocket.AgentSocketKilled.class) {
					if (personal_sockets_list.size() == 0 && NIOAgent.this.stoping)
						NIOAgent.this.killAgent(NIOAgent.this);
				} else if (m instanceof PongMessageReceived) {
					PongMessageReceived pmr = (PongMessageReceived) m;
					PersonalSocket ps = personal_sockets.get(pmr.socket);
					ps.pongMessageReceived();
				}
				/*
				 * else if (m instanceof LockReadMessage) { PersonalSocket
				 * ps=personal_sockets.get(((LockReadMessage) m).socket); ps.lockRead(); } else
				 * if (m instanceof UnlockReadMessage) { PersonalSocket
				 * ps=personal_sockets.get(((UnlockReadMessage) m).socket);
				 * 
				 * ps.unlockRead(); }
				 */
				else if (m instanceof BindInetSocketAddressMessage) {
					if (logger != null && logger.isLoggable(Level.FINER))
						logger.finer("Receiving : " + m);
					BindInetSocketAddressMessage message = (BindInetSocketAddressMessage) m;
					InetSocketAddress addr = message.getInetSocketAddress();
					if (message.getType().equals(BindInetSocketAddressMessage.Type.BIND)) {
						if (this.getMadkitConfig().networkProperties.needsServerSocket(addr)) {
							boolean alreadyBind = false;
							for (Server s : serverChannels) {

								if (s.address.equals(addr)) {
									alreadyBind = true;
									break;
								}
							}
							if (!alreadyBind) {
								try {
									ServerSocketChannel serverChannel = ServerSocketChannel.open();

									serverChannel.configureBlocking(false);
									serverChannel.socket().bind(addr);
									// Register the server socket channel, indicating an interest in
									// accepting new connections
									serverChannel.register(selector, SelectionKey.OP_ACCEPT);
									Server s = new Server(serverChannel, addr);
									serverChannels.add(s);
									if (logger != null && logger.isLoggable(Level.FINER))
										logger.finer("Server channel opened : " + s);

								} catch (Exception e) {
									if (logger != null)
										logger.log(Level.SEVERE,
												"Impossible to bind the connection to the next address : " + addr, e);
								}
							}
						}
						bindDatagramData(addr);
					} else if (message.getType().equals(BindInetSocketAddressMessage.Type.DISCONNECT)) {
						boolean multicastToRemove = personal_datagram_channels_per_ni_address.size() > 0;
						for (Iterator<Server> it = serverChannels.iterator(); it.hasNext();) {
							Server s = it.next();
							if (s.address.equals(addr)) {
								s.serverChannels.close();

								List<PendingConnection> pcs=new ArrayList<>();
								for (PendingConnection pc : this.pending_connections)
								{
									if (pc.isConcernedBy(s))
									{
										pcs.add(pc);
									}
								}
								for (PendingConnection pc : pcs)
								{
									pendingConnectionFailed(pc.socketChannel, ConnectionClosedReason.CONNECTION_LOST);
								}

								for (PersonalSocket ps : this.personal_sockets_list)
								{
									if (ps.isConcernedBy(s))
										sendMessage(ps.agentAddress,
											new AskForConnectionMessage(ConnectionClosedReason.CONNECTION_PROPERLY_CLOSED,
													ps.agentSocket.distantIP,
													(InetSocketAddress) ps.socketChannel.getRemoteAddress(), null, false, false));
								}

								it.remove();

								if (personal_datagram_channels_per_ni_address.size() == 0)
									break;
								if (logger != null)
									logger.finer("Server channel closed : " + s);

							} else if (s.address.getAddress().equals(addr.getAddress()))
								multicastToRemove = true;
						}
						if (multicastToRemove)
							personal_datagram_channels_per_ni_address.get(addr.getAddress()).closeConnection(false);
					}
				} else if (m instanceof UpnpIGDAgent.NetworkInterfaceInformationMessage) {
					UpnpIGDAgent.NetworkInterfaceInformationMessage nm=(UpnpIGDAgent.NetworkInterfaceInformationMessage)m;
					for (NetworkInterface ni : nm.getNewDisconnectedInterfaces())
					{
						for (PersonalSocket ps : this.personal_sockets_list)
						{
							sendMessage(ps.agentAddress,
									new AskForConnectionMessage(ConnectionClosedReason.CONNECTION_LOST,
											ps.agentSocket.distantIP,
											(InetSocketAddress) ps.socketChannel.getRemoteAddress(), null, false, false));
						}
						List<PendingConnection> pcs=new ArrayList<>();
						for (PendingConnection pc : this.pending_connections)
						{
							if (pc.isConcernedBy(ni))
							{
								pcs.add(pc);
							}
						}
						for (PendingConnection pc : pcs)
						{
							pendingConnectionFailed(pc.socketChannel, ConnectionClosedReason.CONNECTION_LOST);
						}
						for (Iterator<Server> it = serverChannels.iterator(); it.hasNext();) {
							Server s = it.next();
							if (s.isConcernedBy(ni)) {
								PersonalDatagramChannel mc=personal_datagram_channels_per_ni_address.get(s.address.getAddress());

								s.serverChannels.close();
								it.remove();

								if (mc!=null)
									mc.closeConnection(false);
								if (logger != null)
									logger.finer("Server channel closed : " + s);

							}

						}

					}

				} else if (m instanceof AskForConnectionMessage) {
					try {
						if (logger != null && logger.isLoggable(Level.FINER))
							logger.finer("Receiving : " + m);

						AskForConnectionMessage con = (AskForConnectionMessage) m;
						if (con.now && con.type.equals(ConnectionStatusMessage.Type.DISCONNECT)) {
							PersonalSocket found = getPersonalSocket(con.getChoosenIP(), con.interface_address);
							if (found != null) {
								if (con.concernsIndirectConnection())
									found.closeIndirectConnection(con.connection_closed_reason, con.getIDTransfer(),
											con.getIndirectAgentAddress());
								else
									found.closeConnection(con.connection_closed_reason);
							} else {
								if (logger != null && logger.isLoggable(Level.FINEST))
									logger.finest("Connection to close not found (address=" + con.getChoosenIP()
											+ ", interface_address=" + con.interface_address + ", closeReason="
											+ con.connection_closed_reason + ")");
							}
						} else {
							if (con.type.equals(ConnectionStatusMessage.Type.CONNECT)) {
								if (!stoping)
									initiateConnection(con.getIP(), con.getChoosenIP(),
											con.interface_address.getAddress(), con);
							} else if (con.type.equals(ConnectionStatusMessage.Type.DISCONNECT)) {
								PersonalSocket found = getPersonalSocket(con.getChoosenIP(), con.interface_address);
								if (con.connection_closed_reason == null || con.connection_closed_reason
										.equals(ConnectionClosedReason.CONNECTION_PROPERLY_CLOSED)) {
									sendMessage(found.agentAddress,
											new AskForConnectionMessage(
													ConnectionClosedReason.CONNECTION_PROPERLY_CLOSED, con.getIP(),
													con.getChoosenIP(), con.interface_address, false, false));
								} else {
									sendMessage(found.agentAddress, con);
								}
							}
						}
					} catch (Exception e) {
						if (logger != null)
							logger.log(Level.SEVERE, "Unexpected exception", e);
					}
				} else if (m instanceof MulticastListenerConnectionMessage) {
					try {
						addMulticastListener((MulticastListenerConnectionMessage) m);
					} catch (Exception e) {
						if (logger != null)
							logger.log(Level.SEVERE, "Unexpected exception", e);
					}
				} else if (m instanceof DatagramLocalNetworkPresenceMessage) {
					if (logger != null && logger.isLoggable(Level.FINEST))
						logger.finest("Receiving datagram data to send : " + m);

					try {
						PersonalDatagramChannel pdc = personal_datagram_channels_per_agent_address.get(m.getSender());
						if (pdc == null) {
							if (logger != null)
								logger.warning("No corresponding datagram channel : " + m);
						} else
							pdc.addDataToSend(new DatagramData((DatagramLocalNetworkPresenceMessage) m));
					} catch (Exception e) {
						if (logger != null)
							logger.log(Level.SEVERE, "Unexpected exception", e);
					}
				} else if (m instanceof MulticastListenerDeconnectionMessage) {
					if (logger != null && logger.isLoggable(Level.FINER))
						logger.finer("Receiving : " + m);
					personal_datagram_channels_per_agent_address.get(m.getSender()).closeConnection(true);
				} else if (m instanceof NetworkAgent.StopNetworkMessage) {
					if (logger != null && logger.isLoggable(Level.FINER))
						logger.finer("Receiving stop network order");

					boolean kill = true;
					stoping = true;
					NetworkAgent.StopNetworkMessage message = (NetworkAgent.StopNetworkMessage) m;
					for (Server s : this.serverChannels)
						s.serverChannels.close();
					List<PendingConnection> pcs=new ArrayList<>(this.pending_connections);
					for (PendingConnection pc : pcs)
						pendingConnectionFailed(pc.socketChannel, ConnectionClosedReason.CONNECTION_PROPERLY_CLOSED);
					for (PersonalSocket ps : personal_sockets_list) {
						kill = false;
						sendMessage(ps.agentAddress,
								new AskForConnectionMessage(message.getNetworkCloseReason().getConnectionClosedReason(),
										ps.agentSocket.distantIP,
										(InetSocketAddress) ps.socketChannel.getRemoteAddress(), null, false, false));
					}

					while (personal_datagram_channels.size() > 0)
						personal_datagram_channels.values().iterator().next().closeConnection(false);

					if (kill)
						this.killAgent(this);
				}
				else if (m instanceof ObjectMessage)
				{
					Object c=((ObjectMessage<?>) m).getContent();
					if (c instanceof PersonalSocket)
						((PersonalSocket) c).finishCloseConnection();
					else if (logger!=null)
						logger.warning("Unexpected message "+m);
						
				}
				else if (logger!=null)
					logger.warning("Unexpected message "+m);
				m = nextMessage();
			}

            // Iterate over the set of keys for which events are available
			Iterator<SelectionKey> selectedKeys = this.selector.selectedKeys().iterator();

            long limitDownload=Long.MIN_VALUE;
            long limitUpload=Long.MIN_VALUE;
			while (selectedKeys.hasNext()) {
				SelectionKey key = selectedKeys.next();

				/*
				 * if (((Map<?, ?>) key.attachment()).get(channelType).equals( serverChannel)) {
				 * 
				 * }
				 */
				if (!key.channel().isOpen() || !key.isValid()) {
                    selectedKeys.remove();
					continue;
				}
				// Check what event is available and deal with it

				/*
				 * if (key.isConnectable()) { System.out.println(key); ///finishConnection(key);
				 * } else
				 */if (key.isAcceptable()) {
                    selectedKeys.remove();
					this.accept(key);
				} else {
					if (key.isReadable()) {

                        long ld=getDownloadToWaitInMsToBecomeUnderBandwidthLimit();
                        if (ld>0) {
                            long curTime=System.currentTimeMillis();
                            limitDownload = ld+curTime;
                            if (limitUpload > curTime) {
                                this.delayToWaitToRespectGlobalBandwidthLimit = Math.min(limitDownload, limitUpload);
                                return;
                            }
                            continue;
                        }
                        selectedKeys.remove();
						SelectableChannel sc = key.channel();
						if (sc instanceof SocketChannel)
							((PersonalSocket) key.attachment()).read(key);
						else if (sc instanceof DatagramChannel) {
							personal_datagram_channels.get(sc).read(key);
						}
					} else if (key.isValid() && key.isWritable()) {
                        long lu=getUploadToWaitInMsToBecomeUnderBandwidthLimit();
                        if (lu>0) {
                            long curTime = System.currentTimeMillis();
                            limitUpload = lu+ curTime;
                            if (limitDownload > curTime) {
                                this.delayToWaitToRespectGlobalBandwidthLimit = Math.min(limitDownload, limitUpload);
                                return;
                            }
                            continue;
                        }
                        selectedKeys.remove();
						SelectableChannel sc = key.channel();
						if (sc instanceof SocketChannel) {
							PersonalSocket ps = (PersonalSocket) key.attachment();
							if (ps != null)
								ps.write(key);
							else if (logger != null)
								logger.warning("Personal socket not found " + sc);
						} else {
							personal_datagram_channels.get(sc).write(key);
						}

					}
					else
                        selectedKeys.remove();

				}
			}
            long curTime=System.currentTimeMillis();
			if (limitDownload>curTime)
			    this.delayToWaitToRespectGlobalBandwidthLimit=limitDownload;
			else if (limitUpload>curTime)
                this.delayToWaitToRespectGlobalBandwidthLimit=limitUpload;
			else
                this.delayToWaitToRespectGlobalBandwidthLimit=0;


		} catch (SelfKillException e) {
			throw e;
		} catch (Exception e) {
			if (logger != null)
				logger.severeLog("Unexpected exception", e);
			e.printStackTrace();
		}

	}

	private void bindDatagramData(InetSocketAddress addr) {
		try {
			if (!stoping && getMadkitConfig().networkProperties.autoConnectWithLocalSitePeers
					&& !personal_datagram_channels_per_ni_address.containsKey(addr.getAddress())) {
				for (InetAddress ia : personal_datagram_channels_per_ni_address.keySet()) {
					if (InetAddressFilter.isSameLocalNetwork(addr.getAddress(), ia))
						return;
				}

				personal_datagram_channels_per_ni_address.put(addr.getAddress(), null);
				NetworkInterface ni = NetworkInterface.getByInetAddress(addr.getAddress());

				if (ni != null && ni.supportsMulticast()) {
					MulticastListenerAgent mtlAgent = new MulticastListenerAgent(
							NetworkInterface.getByInetAddress(addr.getAddress()), addr.getAddress());
					launchAgent(mtlAgent);
				}
			}
		} catch (Exception e) {
			if (logger != null)
				logger.log(Level.SEVERE, "Impossible to open a multicast listener with the address : " + addr, e);
		}

	}

	private void addMulticastListener(MulticastListenerConnectionMessage mlcm) throws IOException {
		DatagramChannel dc = mlcm.instantiateDatagramChannel();
		/*
		 * if (dc.isConnected()) {
		 */
		PersonalDatagramChannel pdc = new PersonalDatagramChannel(dc,
				DatagramLocalNetworkPresenceMessage.getMaxDatagramMessageLength(), mlcm.getSender(),
				mlcm.getNetworkInterfaceAddress(), mlcm.getGroupIPAddress(), mlcm.getPort());
		personal_datagram_channels.put(dc, pdc);
		personal_datagram_channels_per_agent_address.put(mlcm.getSender(), pdc);
		personal_datagram_channels_per_ni_address.put(mlcm.getNetworkInterfaceAddress(), pdc);
		if (logger != null && logger.isLoggable(Level.FINER))
			logger.finer("Multicast listener added : " + mlcm);
		/*
		 * } else if (logger!=null) logger.
		 * warning("Impossible to initiate datagram channel and to listen local peers apparition : "
		 * +mlcm);
		 */
	}

	private PersonalSocket getPersonalSocket(InetSocketAddress _distant_inet_address,
			InetSocketAddress _local_interface_address) {
		PersonalSocket found = null;
		for (PersonalSocket ps : personal_sockets_list) {
			try {
				InetSocketAddress isa_local = (InetSocketAddress) ps.socketChannel.getLocalAddress();
				if (isa_local.equals(_local_interface_address)) {
					InetSocketAddress isa_remote = (InetSocketAddress) ps.socketChannel.getRemoteAddress();
					if (isa_remote.equals(_distant_inet_address)) {
						found = ps;
						break;
					}

				}
			} catch (Exception e) {
				if (logger != null)
					logger.log(Level.SEVERE, "Unexpected exception", e);

			}
		}
		return found;
	}

	private SocketChannel initiateConnection(AbstractIP ip, InetSocketAddress address, InetAddress local_interface,
			AskForConnectionMessage originalMessage) throws IOException {
		if (logger != null && logger.isLoggable(Level.FINER))
			logger.finer("Connection initiation (address=" + address + ", localInterface=" + local_interface + ")");
		SocketChannel socket = null;
		try {
			socket = SocketChannel.open();

			// socket.bind(new InetSocketAddress(local_interface, address.getPort()));
			socket.configureBlocking(false);

			boolean ok = socket.connect(address);
			addPendingConnection(ip, socket, address, local_interface, originalMessage);
			if (ok)
				finishConnection(ip, socket);

			return socket;
		} catch (IOException e) {
			if (logger != null)
				logger.severeLog("Connection failed ", e);
			if (socket != null)
				pendingConnectionFailed(socket, null);
			throw e;
		}
	}

	/*
	 * private void finishConnection(SelectionKey key) { if (stoping) {
	 * key.cancel();
	 * 
	 * return; }
	 * 
	 * 
	 * 
	 * try { finishConnection(null, (SocketChannel)key.channel()); }
	 * catch(IOException e) { key.cancel(); if (logger!=null)
	 * logger.log(Level.WARNING, "Connection failed (local initiative).", e); } }
	 */

	private void finishConnection(AbstractIP ip, SocketChannel sc) throws IOException {
		if (stoping) {
			return;
		}
		try {
			InetSocketAddress isaRemote = (InetSocketAddress) sc.getRemoteAddress();
			InetSocketAddress isaLocal = (InetSocketAddress) sc.getLocalAddress();
			/*
			 * if (ip==null) ip=new DoubleIP(isaRemote);
			 */
			if (logger != null)
				logger.finer("Finishing connection (localInetAddress=" + isaLocal + ", removeInetAddress=" + isaRemote
						+ "]");

			try {
				sc.finishConnect();

				SelectionKey clientKey = sc.register(this.selector, SelectionKey.OP_CONNECT | SelectionKey.OP_READ);
				addSocket(ip, sc, true, clientKey);
			} catch (IOException e) {
				if (logger != null)
					logger.severeLog("Connection failed ", e);
				pendingConnectionFailed(sc, null);
				throw e;
			}
		} catch (IOException e) {
			if (logger != null)
				logger.log(Level.WARNING, "Connection failed (local initiative).", e);
			throw e;
		}
	}

	private void addSocket(AbstractIP ip, SocketChannel socketChannel, boolean local_asking, SelectionKey clientKey) {
		try {
			InetSocketAddress isaRemote = (InetSocketAddress) socketChannel.getRemoteAddress();
			InetSocketAddress isaLocal = (InetSocketAddress) socketChannel.getLocalAddress();
			if (ip == null)
				ip = new DoubleIP(isaRemote);
			try {
				if (getMadkitConfig().networkProperties.isConnectionPossible(isaRemote, isaLocal, false, !local_asking,
						false) && addIfPossibleNewConnectedIP(isaRemote.getAddress())) {
					DistantKernelAgent dka = new DistantKernelAgent();
					this.launchAgent(dka);
					AgentAddress dkaaa = dka.getAgentAddressIn(LocalCommunity.Groups.NETWORK,
							LocalCommunity.Roles.DISTANT_KERNEL_AGENT_ROLE);
					AgentSocket agent = new AgentSocket(ip, dkaaa, socketChannel, myAgentAddress, isaRemote, isaLocal,
							local_asking);
					this.launchAgent(agent);
					if (agent.getState().equals(State.LIVING) && dka.getState().equals(State.LIVING)) {

						PersonalSocket ps = new PersonalSocket(socketChannel, agent);
						clientKey.attach(ps);
						personal_sockets.put(agent.getNetworkID(), ps);
						personal_sockets_list.add(ps);
						/*int max_block_size = agent.getMaxBlockSize();
						if (readBuffer == null || readBuffer.capacity() < max_block_size)
							readBuffer = ByteBuffer.allocate(max_block_size);*/

						broadcastMessageWithRole(LocalCommunity.Groups.LOCAL_NETWORKS,
								LocalCommunity.Roles.LOCAL_NETWORK_ROLE,
								new ConnectionStatusMessage(ConnectionStatusMessage.Type.CONNECT, ip, isaRemote,
										isaLocal),
								LocalCommunity.Roles.NIO_ROLE);

						if (local_asking)
							pendingConnectionSuceeded(socketChannel);

						if (logger != null && logger.isLoggable(Level.FINER))
							logger.finer("Connection established  : " + ps);
					} else {
						removeConnectedIP(isaRemote.getAddress());
						if (!dka.getState().equals(State.LIVING)) {
							if (logger != null) {
								logger.severe("Distant kernel agent not launched !");
							}
						} else
							killAgent(dka);

						if (!agent.getState().equals(State.LIVING)) {
							if (logger != null) {
								logger.severe("Agent socket not launched !");
							}
						} else
							killAgent(agent);

						if (local_asking)
							pendingConnectionFailed(socketChannel, ConnectionClosedReason.CONNECTION_ANOMALY);

						socketChannel.close();

					}
				} else {
					if (local_asking)
						pendingConnectionFailed(socketChannel, ConnectionClosedReason.CONNECTION_PROPERLY_CLOSED);
					socketChannel.close();
				}
			} catch (Exception e) {
				if (local_asking)
					pendingConnectionFailed(socketChannel, ConnectionClosedReason.CONNECTION_ANOMALY);
				socketChannel.close();
				throw e;
			}
		} catch (Exception e) {
			if (logger != null)
				logger.log(Level.SEVERE, "Unexpected exception", e);
		}
	}

	private void accept(SelectionKey _key) {
		if (logger != null && logger.isLoggable(Level.FINER))
			logger.finer("Accepting connection (_key=" + _key + ")");

		if (stoping) {
			_key.cancel();
			return;
		}
		try {
			// For an accept to be pending the channel must be a server socket channel.
			ServerSocketChannel serverSocketChannel = (ServerSocketChannel) _key.channel();

			// Accept the connection and make it non-blocking
			SocketChannel socketChannel = serverSocketChannel.accept();

			try {
				socketChannel.configureBlocking(false);

				// Register the new SocketChannel with our Selector, indicating
				// we'd like to be notified when there's data waiting to be read

				SelectionKey clientKey = socketChannel.register(this.selector,
						SelectionKey.OP_CONNECT | SelectionKey.OP_READ);

				addSocket(null, socketChannel, false, clientKey);
			} catch (IOException e) {
				try {
					socketChannel.close();
				} catch (IOException ignored) {
				}
				throw e;

			}

		} catch (IOException e) {
			_key.cancel();
			if (logger != null)
				logger.log(Level.WARNING, "Connection failed (distant initiative).", e);
		}
	}

	/*
	 * private static class FileData { public final AbstractData data; private int
	 * count_before_sending;
	 * 
	 * public FileData(AbstractData _data, int count) { data=_data;
	 * count_before_sending=count; }
	 * 
	 * public boolean decrement() { return (--count_before_sending)==0; } public
	 * void increment() { if (count_before_sending!=0) ++count_before_sending; } }
	 */

	private static class FirstData extends AbstractData {

		ByteBuffer data;
		private boolean unlocked = false;
		private final IDTransfer id;

		FirstData(NIOAgent agent, DatagramLocalNetworkPresenceMessage message) throws IOException, OverflowException {
			super(true);
			data = new DatagramData(message).getByteBuffer();
			id = IDTransfer.generateIDTransfer(MadkitKernelAccess.getIDTransferGenerator(agent));
		}
		
		@Override
		boolean isDataBuildInProgress()
		{
			return false;
		}

		@Override
		void unlockMessage() {
			unlocked = true;
		}

		@Override
		boolean isUnlocked() {
			return unlocked;
		}

		@Override
		ByteBuffer getByteBuffer() {
			try
			{
				return data;
			}
			finally
			{
				data=null;
			}
		}

		/*@Override
		boolean isReady() {
			return true;
		}*/

		@Override
		boolean isFinished() {
			return data==null;//data.remaining() == 0;
		}

		@Override
		boolean isCurrentByteBufferFinished() {
			return data==null;//data.remaining() == 0;
		}

		@Override
		DataTransferType getDataTransferType() {
			return DataTransferType.SHORT_DATA;
		}

		@Override
		IDTransfer getIDTransfer() {
			return id;
		}

		@Override
		void reset() {
			data.clear();
		}

		/*@Override
		boolean isCurrentByteBufferStarted() {
			return data.position() > 0;
		}*/
		
		@Override 
		Object getLocker()
		{
			return null;
		}


	}

	private class NoBackData 
	{
		private PersonalSocket personalSocket;
		private AbstractData data;
		private volatile ByteBuffer buffer;
		private TransfertException pendingExcetion=null;
		private IDTransfer idTransfer=null;
		
		
		private NoBackData(PersonalSocket personalSocket, AbstractData data) {
			if (data==null)
				throw new NullPointerException();
			this.personalSocket=personalSocket;
			this.data=data;
			this.personalSocket.canPrepareNextData=false;
		}
		
		boolean isLastMessage() throws PacketException
		{
			return data.isLastMessage() && data.isFinished();
		}
		
		boolean isReady() throws TransfertException
		{
			throwPendingException();

			
			try
			{
			
				return buffer!=null || !data.isCurrentByteBufferFinished();
			}
			catch(PacketException e)
			{
				throw new TransfertException(e);
			}
		}
		
		boolean isCanceled()
		{
			return data.isUnlocked();
		}
		
		boolean isDataLoadingCanceled() throws TransfertException, PacketException
		{
			takeNextData();
			return buffer==null && data.isFinished();
		}
		
		Object getLocker()
		{
			return data.getLocker();
		}
		
		void takeNextData() throws TransfertException
		{
			try
			{
				if (buffer==null)
				{
					buffer=this.data.getByteBuffer();
					if (buffer!=null)
					{
						idTransfer=this.data.getIDTransfer();
						if (idTransfer==null)
							throw new NullPointerException();
						personalSocket.free(this.data);
						personalSocket.canPrepareNextData=true;
						personalSocket.prepareNextDataToNextIfNecessary();
					}
				}
			}
			catch(PacketException e)
			{
				if (logger != null && logger.isLoggable(Level.FINER))
					logger.finer("Unexpected exception during write process : " + e);

				personalSocket.closeConnection(ConnectionClosedReason.CONNECTION_LOST);
			}
			
		}
		private void throwPendingException() throws TransfertException
		{
			if (pendingExcetion!=null)
			{
				try
				{
					throw pendingExcetion;
				}
				finally
				{
					pendingExcetion=null;
				}
			}
		}
		ByteBuffer getBuffer() throws TransfertException
		{
			throwPendingException();
			takeNextData();
			if (buffer==null)
				throw new InternalError();
			return buffer;
		}
		
		boolean isFinished() 
		{
			return buffer!=null && buffer.remaining()==0;
		}
		
		IDTransfer getIDTransfer()
		{
			return idTransfer;
		}


		
	}
	
	private class PersonalSocket {
		public final SocketChannel socketChannel;
		public final AgentAddress agentAddress;
		public final AgentSocket agentSocket;

		protected LinkedList<AbstractData> shortDataToSend = new LinkedList<>();
		protected ArrayList<AbstractData> bigDataToSend = new ArrayList<>();
		protected LinkedList<AbstractData> dataToTransfer = new LinkedList<>();
		private LinkedList<NoBackData> noBackDataToSend=new LinkedList<>();
		// private LinkedList<FileData> bigDataWaiting=new LinkedList<>();
		private DataTransferType dataTransferType = DataTransferType.SHORT_DATA;
		protected int bigDataToSendIndex = 0;
		private boolean waitingForPongMessage = false;
		private long time_sending_ping_message;
		private long last_data_writed_utc;
		// private int read_locked=0;
		private boolean is_closed = false;
		private ConnectionClosedReason cs=null;
		private DatagramData firstReceivedData = new DatagramData();
		private boolean firstPacketSent = false;
		private final ByteBuffer readSizeBlock = ByteBuffer.allocate(Block.getBlockSizeLength());
		private ByteBuffer readBuffer = null; 
		private final int maxBlockSize;
		private volatile boolean canPrepareNextData=true;
		public boolean isClosed() {
			return is_closed;
		}

		public PersonalSocket(SocketChannel _socketChannel, AgentSocket _agent)
				throws OverflowException, IOException, NoSuchAlgorithmException, NoSuchProviderException, TransfertException {
			socketChannel = _socketChannel;
			agentSocket = _agent;
			agentAddress = agentSocket.getAgentAddressIn(LocalCommunity.Groups.NETWORK,
					LocalCommunity.Roles.SOCKET_AGENT_ROLE);
			last_data_writed_utc = time_sending_ping_message = System.currentTimeMillis();
			maxBlockSize=_agent.getMaxBlockSize();
			/*socketChannel.setOption(StandardSocketOptions.SO_SNDBUF, maxBlockSize);
			socketChannel.setOption(StandardSocketOptions.SO_RCVBUF, maxBlockSize);*/

			addDataToSend(new FirstData(NIOAgent.this,
					new DatagramLocalNetworkPresenceMessage(System.currentTimeMillis(),
							getMadkitConfig().projectVersion, getMadkitConfig().madkitVersion, getMadkitConfig().minimumProjectVersion, getMadkitConfig().minimumMadkitVersion, null,
							getKernelAddress())));
		}
		
		void prepareNextDataToNextIfNecessary() throws TransfertException
		{

			
			if (canPrepareNextData && noBackDataToSend.size()<3)
			{
				AbstractData ad=getNextData();
				if (ad!=null)
				{
					NoBackData nbd=new NoBackData(this, ad);
					noBackDataToSend.addLast(nbd);
					nbd.takeNextData();
					waitDataReady();
				}
			}
		}

		NoBackData getNextNoBackData() throws TransfertException
		{
		    if (is_closed)
		        return null;
			NoBackData res;
			
			do
			{
				if (noBackDataToSend.size()==0)
					return null;
				res=noBackDataToSend.getFirst();
				try
				{
					if (res.isDataLoadingCanceled())
					{
						noBackDataToSend.removeFirst();
						res=null;
						prepareNextDataToNextIfNecessary();
					}
				}
				catch(MadkitException e)
				{
					throw new TransfertException(e);
				}
			} while(res==null);
			
				
			if (!res.isReady())
				if (!waitDataReady())
					return null;
			return res;
		}
		private boolean freeNoBackData() throws PacketException, TransfertException
		{
			if (noBackDataToSend.getFirst().isFinished())
			{
				firstPacketSent = true;
				NoBackData d=noBackDataToSend.removeFirst();
				if (d.isLastMessage()) {
					if (logger != null && logger.isLoggable(Level.FINER))
						logger.finer("Sending last message (agentSocket=" + agentAddress + ")");
					this.closeConnection(ConnectionClosedReason.CONNECTION_PROPERLY_CLOSED, true);
					return true;
				}

				prepareNextDataToNextIfNecessary();
				return true;
			}
			else
				return false;
		}
		long getLastDataWritedUTC() {
			return last_data_writed_utc;
		}

		@Override
		public String toString() {
			SocketAddress local = null, remote = null;
			try {
				local = socketChannel.getLocalAddress();
				remote = socketChannel.getRemoteAddress();
			} catch (Exception ignored) {

			}
			return "Socket[localAddress" + local + ", remoteAddress=" + remote + ", agentSocket=" + agentSocket + "]";
		}

		@SuppressWarnings("deprecation")
        @Override
		public void finalize() {
			try {
				if (this.socketChannel.isConnected())
					this.socketChannel.close();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		/*
		 * public void lockRead() { ++read_locked; } public void unlockRead() {
		 * --read_locked; }
		 */
		public boolean isReadLocked() {
			return /* read_locked>1 || */ agentSocket.isTransferReadPaused();
		}

		public void sendPingMessage() {
			NIOAgent.this.sendMessage(agentAddress, new SendPingMessage());
			time_sending_ping_message = System.currentTimeMillis();
			waitingForPongMessage = true;
		}

		public boolean isWaitingForPongMessage() {
			return waitingForPongMessage;
		}

		public void pongMessageReceived() {
			if (logger != null && logger.isLoggable(Level.FINEST))
				logger.finest("Received pong message : " + this);

			waitingForPongMessage = false;
		}

		public long getTimeSendingPingMessage() {
			return time_sending_ping_message;
		}

		public boolean hasDataToSend() {
			return shortDataToSend.size()>0 || bigDataToSend.size()>0 || dataToTransfer.size()>0;
		}

		public boolean addDataToSend(AbstractData _data) throws TransfertException {
			if (shortDataToSend != null && bigDataToSend != null) {

				/*
				 * if (sk==null || !sk.isValid()) return false;
				 */

				if (logger != null && logger.isLoggable(Level.FINEST))
					logger.finest(this + " - data to send : " + _data);

				switch (_data.getDataTransferType()) {
				case SHORT_DATA:
					// _data.setPriority(prioritary);
					if (_data.isPriority() && shortDataToSend.size() > 0) {

						ListIterator<AbstractData> itDataToSend = shortDataToSend.listIterator();
						itDataToSend.next();

						while (itDataToSend.hasNext()) {
							if (!itDataToSend.next().isPriority()) {
								itDataToSend.previous();
								break;
							}
						}
						itDataToSend.add(_data);

						/*
						 * Iterator<FileData> it=bigDataWaiting.iterator(); while(it.hasNext()) {
						 * it.next().increment(); }
						 */
					} else
						shortDataToSend.add(_data);
					break;
				case BIG_DATA:
					bigDataToSend.add(_data);
					/*
					 * if (dataToSend.size()==0) bigDataToSend.add(_data); else
					 * bigDataWaiting.add(new FileData(_data, dataToSend.size()));
					 */

					break;
				case DATA_TO_TRANSFER:
					dataToTransfer.addLast(_data);
					break;
				}
				
				checkValidTransferType();
				prepareNextDataToNextIfNecessary();
				
				SelectionKey sk = socketChannel.keyFor(NIOAgent.this.selector);
				if (!is_closed && (sk.interestOps() & SelectionKey.OP_WRITE) != SelectionKey.OP_WRITE)
					sk.interestOps(SelectionKey.OP_CONNECT | SelectionKey.OP_WRITE | SelectionKey.OP_READ);
				
				return true;
			} else
				return false;
		}

		private boolean hasPrioritaryDataToSend() {
			return shortDataToSend.size() > 0 && shortDataToSend.get(0).isPriority();
		}

		/*private boolean waitDataReady() {
			try {
				synchronized (myAgentAddress) {
					final AtomicBoolean hasData = new AtomicBoolean(hasDataToSend());
					final AtomicBoolean validData = new AtomicBoolean(
							((shortDataToSend.size() > 0 && shortDataToSend.getFirst().isReady())
									|| (bigDataToSend.size() > bigDataToSendIndex
											&& bigDataToSend.get(bigDataToSendIndex).isReady())
									|| (dataToTransfer.size() > 0 && dataToTransfer.getFirst().isReady())));

					if (!hasData.get() || validData.get())
						return hasData.get();

					NIOAgent.this.wait(new LockerCondition(myAgentAddress) {

						@Override
						public boolean isLocked() {
							return hasData.get() && !validData.get();
						}

						@Override
						public void afterCycleLocking() {
							hasData.set(hasDataToSend());
							validData.set(((shortDataToSend.size() > 0 && shortDataToSend.getFirst().isReady())
									|| (bigDataToSend.size() > bigDataToSendIndex
											&& bigDataToSend.get(bigDataToSendIndex).isReady())
									|| (dataToTransfer.size() > 0 && dataToTransfer.getFirst().isReady())));
						}
					});
					return validData.get();
				}
			} catch (InterruptedException e) {
				return false;
			}
		}*/
		private boolean waitDataReady() throws TransfertException {
			try {
				//synchronized (this.agentSocket) {
					final AtomicBoolean hasData = new AtomicBoolean(this.noBackDataToSend.size()>0);
					NoBackData first=null;
					final AtomicBoolean validData = new AtomicBoolean(this.noBackDataToSend.size()>0 && (first=this.noBackDataToSend.getFirst()).isReady());
					
					final AtomicReference<TransfertException> exception=new AtomicReference<>();
					if (!hasData.get() || validData.get())
						return hasData.get();
				if (first==null)
					throw new NullPointerException();

                NIOAgent.this.wait(new LockerCondition(first.getLocker()) {
						
						@Override
						public boolean isLocked() {

							NoBackData first=noBackDataToSend.peekFirst();
							try
							{
								hasData.set(first!=null);
								validData.set(first!=null && first.isReady());
							}
							catch(TransfertException e)
							{
								exception.set(e);
								return false;
							}
							return exception.get()==null && first!=null && (!validData.get() || first.isCanceled() || agentSocket.getState().compareTo(AbstractAgent.State.ENDING)>=0);
						}

						
					});
					if (exception.get()!=null)
						throw exception.get();
					return validData.get();
				//}
			} catch (InterruptedException e) {
				return false;
			}
		}
		private boolean isTransferTypeChangementPossible() throws TransfertException {
			AbstractData data = null;
			switch (dataTransferType) {
			case SHORT_DATA:
				if (shortDataToSend.size() > 0) {
					data = shortDataToSend.getFirst();
				} else
					return true;
				break;
			case BIG_DATA:
				if (bigDataToSend.size() > bigDataToSendIndex) {
					data = bigDataToSend.get(bigDataToSendIndex);
				} else
					return true;
				break;
			case DATA_TO_TRANSFER:
				if (dataToTransfer.size() > 0) {
					data = dataToTransfer.getFirst();
				} else
					return true;
				break;
			}
			try {
				return data == null || data.isCurrentByteBufferFinished();
			} catch (PacketException e) {
				throw new TransfertException(e);
			}

		}

		private boolean checkValidTransferType() throws TransfertException {
				boolean changement = isTransferTypeChangementPossible();
				if (!changement)
					return true;
				boolean valid_data=hasDataToSend();
				//boolean valid_data = waitDataReady();
				// boolean valid_data=(shortDataToSend.size()>0 &&
				// shortDataToSend.getFirst().isReady()) ||
				// (bigDataToSend.size()>bigDataToSendIndex &&
				// bigDataToSend.get(bigDataToSendIndex).isReady()) || (dataToTransfer.size()>0
				// && dataToTransfer.getFirst().isReady());
				if (!valid_data || is_closed || hasPrioritaryDataToSend()) {
					dataTransferType = DataTransferType.SHORT_DATA;
					return !is_closed;
				} else {
					valid_data = false;
					while (!valid_data) {
						switch (dataTransferType) {
						case SHORT_DATA:
							if (shortDataToSend.size() == 0)
								dataTransferType = DataTransferType.BIG_DATA;
							else
								valid_data = true;
							break;
						case BIG_DATA:
							if (bigDataToSend.size() == 0)
								dataTransferType = DataTransferType.DATA_TO_TRANSFER;
							else
								valid_data = true;
							break;
						case DATA_TO_TRANSFER:
							if (dataToTransfer.size() == 0)
								dataTransferType = DataTransferType.SHORT_DATA;
							else
								valid_data = true;
							break;
						}
					}
					return true;
				}
			
		}

		private boolean setNextTransferType() throws TransfertException {
			switch (dataTransferType) {
			case SHORT_DATA:
				dataTransferType = DataTransferType.BIG_DATA;
				break;
			case BIG_DATA:
				dataTransferType = DataTransferType.DATA_TO_TRANSFER;
				break;
			case DATA_TO_TRANSFER:
				dataTransferType = DataTransferType.SHORT_DATA;
				break;
			}
			return checkValidTransferType();
		}

		private AbstractData getNextData() throws TransfertException {
			if (is_closed)
				return null;
			switch (dataTransferType) {
			case SHORT_DATA:
				if (shortDataToSend.size() > 0)
				{
					AbstractData ad=shortDataToSend.getFirst();
					if (ad.isDataBuildInProgress())
						return null;
					return ad;
				}
				else
					return null;
			case BIG_DATA:
				if (bigDataToSend.size() > 0)
				{
					AbstractData ad=bigDataToSend.get(bigDataToSendIndex);
					if (ad.isDataBuildInProgress())
						return null;
					return ad;
				}
				else
					throw new TransfertException("Unexpected exception !");
			case DATA_TO_TRANSFER:
				if (dataToTransfer.size() > 0)
				{
					AbstractData ad=dataToTransfer.getFirst();
					if (ad.isDataBuildInProgress())
						return null;
					return ad;
				}
				else
					throw new TransfertException("Unexpected exception !");
			}
			return null;

		}

		@SuppressWarnings("ThrowFromFinallyBlock")
        private boolean free(AbstractData d) throws TransfertException {
			try
			{
				
				boolean finished = d.isFinished();
				switch (dataTransferType) {
				case SHORT_DATA:
					if (d.isCurrentByteBufferFinished() || finished) {
						if (finished) {
							try {
								d.unlockMessage();
							} catch (Exception e) {
								throw new TransfertException("Unexpected exception !", e);
							} finally {
								AbstractData removed=shortDataToSend.removeFirst();
								if (removed!=d)
									throw new InternalError();
							}
						}
						setNextTransferType();
						return true;
					}
					return false;
	
				case BIG_DATA:
					if (d.isCurrentByteBufferFinished() || finished) {
						if (finished) {
							try {
								d.unlockMessage();
							} catch (Exception e) {
								throw new TransfertException("Unexpected exception !", e);
							}
							AbstractData removed=bigDataToSend.remove(bigDataToSendIndex);
							if (removed!=d)
								throw new InternalError();

							bigDataToSendIndex = bigDataToSend.size() == 0 ? 0 : bigDataToSendIndex % bigDataToSend.size();
						} else {
							bigDataToSendIndex = bigDataToSend.size() == 0 ? 0
									: (bigDataToSendIndex + 1) % bigDataToSend.size();
						}
	
						setNextTransferType();
						return true;
					}
					return false;
	
				case DATA_TO_TRANSFER:
					if (d.isCurrentByteBufferFinished() || finished) {
						if (finished) {
							try {
								d.unlockMessage();
							} catch (Exception e) {
								throw new TransfertException("Unexpected exception !", e);
							}
							AbstractData removed=dataToTransfer.removeFirst();
							if (removed!=d)
								throw new InternalError();

						}
						setNextTransferType();
						return true;
					}
					return false;
				}
				return false;
			}
			catch(PacketException e)
			{
				throw new TransfertException(e);
			}
		}

		private Timer timer_send = null;
		private int data_sended = 0;

		public void read(SelectionKey key) {

			if (is_closed)
				return;
			if (isReadLocked())
				return;
			// Clear out our read buffer so it's ready for new data
			
			int data_read;
			try {
				if (this.readBuffer==null)
				{
					data_read = socketChannel.read(readSizeBlock);
					
					if (!readSizeBlock.hasRemaining())
					{
						int size=Block.getBlockSize(readSizeBlock.array(), 0);
						if (size<=0 || size>maxBlockSize)
						{
							if (logger != null)
								logger.severe("Invalid block size "+size+" (max="+maxBlockSize+"). Impossible to receive new bytes (connection closed).");
							this.agentSocket.proceedEventualBan(true);
							closeConnection(ConnectionClosedReason.CONNECTION_ANOMALY);
							key.cancel();
							return;
						}
						readBuffer=ByteBuffer.allocate(size);
						readBuffer.put(readSizeBlock.array());
						readSizeBlock.clear();
						int s=socketChannel.read(readBuffer);
						if (s<0)
							data_read=s;
						else
							data_read+=s;
					}
				}
				else
					data_read = socketChannel.read(readBuffer);

				if (data_read < 0) {
					// Remote entity shut the socket down cleanly. Do the
					// same from our end and cancel the channel.
					if (logger != null && logger.isLoggable(Level.FINER))
						logger.finer("Impossible to receive new bytes (connection closed).");
					closeConnection(ConnectionClosedReason.CONNECTION_PROPERLY_CLOSED);
					key.cancel();
					return;
				}

			} catch (IOException e) {
				/*
				 * if (logger!=null) logger.severeLog("Unexpected exception : ", e);
				 */

				// The remote forcibly closed the connection, cancel
				// the selection key and close the channel.
				if (logger != null && logger.isLoggable(Level.FINER))
					logger.finer("Connection exception (connection closed) : " + e);

				closeConnection(ConnectionClosedReason.CONNECTION_LOST);
				key.cancel();
				return;
			}
			
			// boolean hasRemaining=readBuffer.hasRemaining();
			if (readBuffer!=null && !readBuffer.hasRemaining()) {
				receivedData(key, readBuffer, data_read);
				readBuffer=null;
			}
			/*
			 * if (((key.interestOps() & SelectionKey.OP_WRITE) != SelectionKey.OP_WRITE) &&
			 * (data_read==0 || hasRemaining) && hasDataToSend()) {
			 * key.interestOps(SelectionKey.OP_WRITE | SelectionKey.OP_READ); }
			 */

		}

		private void receivedData(SelectionKey key, ByteBuffer data, int data_read) {
			data.clear();
			if (firstReceivedData != null) {
				firstReceivedData.put(data.array(), 0, data_read);
				if (!firstReceivedData.isValid()) {
					if (logger != null && logger.isLoggable(Level.FINER))
						logger.finer("first received data invalid : " + firstReceivedData);
					this.agentSocket.proceedEventualBan(true);
					closeConnection(ConnectionClosedReason.CONNECTION_ANOMALY);
					key.cancel();
					return;
				} else if (firstReceivedData.isComplete()) {
					try {
						DatagramLocalNetworkPresenceMessage dm=firstReceivedData.getDatagramLocalNetworkPresenceMessage();
						if (dm.isCompatibleWith(getMadkitConfig().projectVersion, getMadkitConfig().madkitVersion,
								getMadkitConfig().minimumProjectVersion, getMadkitConfig().minimumMadkitVersion,
								getKernelAddress())) {
							ByteBuffer bb = firstReceivedData.getUnusedReceivedData();
							firstReceivedData = null;
							if (bb != null) {
								receivedData(key, bb, bb.capacity());
							}
						} else {
							firstReceivedData = null;
							closeConnection(ConnectionClosedReason.CONNECTION_PROPERLY_CLOSED);
							key.cancel();
							return;
						}
					} catch (Exception e) {
						if (logger!=null)
							logger.severeLog("Invalid first packet", e);
						this.agentSocket.proceedEventualBan(true);
						closeConnection(ConnectionClosedReason.CONNECTION_ANOMALY);
						key.cancel();
						return;
					}
				}

				if (logger != null && logger.isLoggable(Level.FINEST))
					logger.finest("Receiving new initial bytes (" + data_read + " bytes) from "
							+ this.agentSocket.getDistantInetSocketAddress());
			} else {
				NIOAgent.this.sendMessage(agentAddress, new DataReceivedMessage(data.array()));
				if (logger != null && logger.isLoggable(Level.FINEST))
					logger.finest("Receiving new bytes (" + data.array().length + " bytes) from "
							+ this.agentSocket.getDistantInetSocketAddress());
			}
		}
		private boolean dataNotAlreadyTwoTimes=false;
		public void write(SelectionKey key) throws MadkitException {
			try {
				if (is_closed)
					return;

				NoBackData data = getNextNoBackData();

				//while (data != null) {
					boolean datafinished;
					int remaining = -1;
					if ((datafinished = data.isFinished()) || !data.isReady()) {
						if (datafinished) {
							freeNoBackData();
						} else {
							if (dataNotAlreadyTwoTimes)
							{
								if (!waitDataReady()) {
									if (logger != null && logger.isLoggable(Level.FINEST))
										logger.finest("Invalid transfer type detected !");
									return;
								}
							}
							else
							{
								dataNotAlreadyTwoTimes=true;
								return;
							}
						}

					} else {
						dataNotAlreadyTwoTimes=false;
						ByteBuffer buf = data.getBuffer();

						if (buf == null) {
							throw new TransfertException("Unexpected exception !");
						} else {
							if (firstPacketSent) {
								if (timer_send == null)
									timer_send = new Timer(true);
								else {
									agentSocket.getStatistics().newDataSent(data.getIDTransfer(), data_sended,
											timer_send.getDeltaMili());
								}
							}

							data_sended = socketChannel.write(buf);

							if (firstPacketSent)
							{
								
								agentSocket.getStatistics().newDataSent(data.getIDTransfer(), data_sended);
							}

							remaining = buf.remaining();
							if (freeNoBackData() && remaining > 0)
								throw new IllegalAccessError();

							if (data_sended > 0) {
								if (logger != null && logger.isLoggable(Level.FINEST))
									logger.finest("New data sent (" + data_sended + " bytes, bufferRemaining="
											+ remaining + ", totalBufferLength=" + buf.capacity() + ")");
								last_data_writed_utc = System.currentTimeMillis();
							}

						}
					}
					if (remaining > 0)
						return;
					data=getNextNoBackData();
					if (data!=null)
					    return;
					/*if (data == null) {
						timer_send = null;

					}*/
					/*
					 * if (data_sended==0 && remaining>0) { //key.interestOps(SelectionKey.OP_READ);
					 * return; }
					 */

				//}
				if (!is_closed)
					key.interestOps(SelectionKey.OP_CONNECT | SelectionKey.OP_READ);

				/*
				 * if (key.isValid()) key.interestOps(SelectionKey.OP_READ);
				 */
			} catch (IOException e) {
				if (logger != null && logger.isLoggable(Level.FINER))
					logger.finer("Unexpected exception during write process : " + e);

				closeConnection(ConnectionClosedReason.CONNECTION_LOST);
				key.cancel();
			}

		}

		/*
		 * public void prepareCloseConnection(ConnectionClosedReason cs) {
		 * NIOAgent.this.sendMessageWithRole(agentAddress, new
		 * ConnectionClosed(socketChannel, cs, dataToSend, bigDataToSend),
		 * LocalCommunity.Roles.NIO_ROLE); }
		 */
		public void closeConnection(ConnectionClosedReason cs)
		{
			closeConnection(cs, false);
		}
		public void closeConnection(ConnectionClosedReason cs, boolean delaying) {
			if (logger != null)
				logger.finer("Closing connection : " + this);
			this.cs=cs;
			is_closed = true;
			try
			{
				if (this.socketChannel.isOpen())
					this.socketChannel.socket().getOutputStream().flush();
			}
			catch(Exception e)
			{
				e.printStackTrace();
			}
			delaying&=getMadkitConfig().networkProperties.delayInMsBeforeClosingConnectionNormally>0;
			if (delaying)
                NIOAgent.this.scheduleTask(new Task<>(new Callable<Void>() {

                    @Override
                    public Void call() {
                        if (isAlive())
                            receiveMessage(new ObjectMessage<>(PersonalSocket.this));
                        else
                            finishCloseConnection();
                        return null;
                    }
                }, getMadkitConfig().networkProperties.delayInMsBeforeClosingConnectionNormally + System.currentTimeMillis()));
			else
				finishCloseConnection();

		}

		public void finishCloseConnection()
		{
			
			
			personal_sockets.remove(this.agentAddress.getAgentNetworkID());
			personal_sockets_list.remove(this);

			

			InetSocketAddress isa=null;
			try {
				isa = (InetSocketAddress) this.socketChannel.getRemoteAddress();
				removeConnectedIP(isa.getAddress());

				SelectionKey key = socketChannel.keyFor(selector);
				if (key != null) {
					key.cancel();
					key.channel().close();
					if (socketChannel.isConnected())
                        socketChannel.close();//TODO remove ?
				} else
					socketChannel.close();
			} catch (Exception e) {
				if (logger != null)
					logger.log(Level.FINE, "Unexpected exception", e);
			}
			try {

				for (Server s : serverChannels) {
					boolean found = false;
					for (PersonalSocket sc : personal_sockets_list) {
						try {
							if (sc.socketChannel.getLocalAddress().equals(s.address)) {
								InetSocketAddress isa2 = (InetSocketAddress) sc.socketChannel.getRemoteAddress();
								if (isa==null)
									throw new NullPointerException();

                                if (isa.equals(isa2)) {
									found = true;
									break;
								}
							}
						} catch (Exception e) {
							if (logger != null)
								logger.log(Level.FINE, "Unexpected exception", e);
						}
					}
					if (!found) {
						if (!stoping) {
							for (AgentAddress aa : getAgentsWithRole(LocalCommunity.Groups.LOCAL_NETWORKS,
									LocalCommunity.Roles.LOCAL_NETWORK_ROLE)) {
								if (isa==null)
									throw new NullPointerException();
                                sendMessageWithRole(aa,
                                        new ConnectionStatusMessage(ConnectionStatusMessage.Type.DISCONNECT,
                                                new DoubleIP(isa), isa, s.address, cs),
                                        LocalCommunity.Roles.NIO_ROLE);
                            }
						}
					}
				}
			} catch (Exception e) {
				if (logger != null)
					logger.log(Level.SEVERE, "Unexpected exception", e);
			}
			NIOAgent.this.sendMessageWithRole(agentAddress, new ConnectionClosed(this.agentAddress.getAgentNetworkID(),
					cs, shortDataToSend, bigDataToSend, dataToTransfer), LocalCommunity.Roles.NIO_ROLE);
			shortDataToSend = new LinkedList<>();
			bigDataToSend = new ArrayList<>();
			dataToTransfer = new LinkedList<>();
			if (stoping && isAlive() && personal_sockets.isEmpty())
			    killAgent(NIOAgent.this);
		}
		public void closeIndirectConnection(ConnectionClosedReason cs, IDTransfer transferID,
				AgentAddress indirectAgentAddress) {
			//ArrayList<AbstractData> transferedDataCanceled = new ArrayList<>();
			for (Iterator<AbstractData> it = this.dataToTransfer.iterator(); it.hasNext();) {
				AbstractData ad = it.next();
				if (ad.getIDTransfer().equals(transferID)) {
					//transferedDataCanceled.add(ad);
					it.remove();
				}
			}
			NIOAgent.this.sendMessageWithRole(
					indirectAgentAddress, new ConnectionClosed(this.agentAddress.getAgentNetworkID(), cs,
							new ArrayList<AbstractData>(0), new ArrayList<AbstractData>(0), dataToTransfer),
					LocalCommunity.Roles.NIO_ROLE);
		}

		public boolean isConcernedBy(Server s) throws IOException {

			return this.socketChannel.getLocalAddress().equals(s.address);
		}
	}

	private class PersonalDatagramChannel {
		private final DatagramChannel datagramChannel;
		private final LinkedList<DatagramData> messagesToSend = new LinkedList<>();
		private DatagramData currentDatagramData = null;
		private final AgentAddress multicastAgent;
		private final InetAddress localAddress;
		private final InetAddress groupIP;
		private final int port;

		PersonalDatagramChannel(DatagramChannel datagramSocket, int maxDataSize, AgentAddress multicastAgent,
				InetAddress localAddress, InetAddress groupIP, int port) throws ClosedChannelException {
			this.datagramChannel = datagramSocket;
			this.datagramChannel.register(NIOAgent.this.selector, SelectionKey.OP_READ);
			this.multicastAgent = multicastAgent;
			this.localAddress = localAddress;
			this.groupIP = groupIP;
			this.port = port;
		}

		@Override
		public String toString() {
			return "PersonalDatagramChannel[channel=" + datagramChannel + ", multiCastAgentAddress=" + multicastAgent
					+ ", localAddress=" + localAddress + "]";
		}

		/*
		 * InetAddress getLocalAddress() { return localAddress; }
		 * 
		 * AgentAddress getMulticastAgent() { return multicastAgent; }
		 */

		void addDataToSend(DatagramData data) {
			messagesToSend.add(data);

			SelectionKey sk = datagramChannel.keyFor(NIOAgent.this.selector);
			if ((sk.interestOps() & SelectionKey.OP_WRITE) != SelectionKey.OP_WRITE)
				sk.interestOps(SelectionKey.OP_WRITE | SelectionKey.OP_READ);
		}

		void read(SelectionKey key) {
			try {

				if (currentDatagramData == null)
					currentDatagramData = new DatagramData();

				datagramChannel.receive(currentDatagramData.getByteBuffer());

				if (!currentDatagramData.isValid())
					currentDatagramData = null;
				else if (currentDatagramData.isComplete()) {

					try {
						DatagramLocalNetworkPresenceMessage dlnpm = currentDatagramData
								.getDatagramLocalNetworkPresenceMessage();
						NIOAgent.this.sendMessageWithRole(multicastAgent, dlnpm, LocalCommunity.Roles.NIO_ROLE);
						currentDatagramData = currentDatagramData.getNextDatagramData();
					} catch (IOException e) {
						currentDatagramData = null;
					}
				}
			} catch (Exception e) {
				if (logger != null)
					logger.severeLog("Unexpected exception : ", e);
				key.cancel();
				closeConnection(false);
			}
		}

		void write(SelectionKey key) {
			try {

				while (messagesToSend.size() > 0) {
					ByteBuffer dd = messagesToSend.getFirst().getByteBuffer();
					// datagramChannel.send(dd, this.groupIPAddress);
					datagramChannel.send(dd, new InetSocketAddress(groupIP, port));
					// datagramChannel.write(dd);

					if (!dd.hasRemaining())
						messagesToSend.removeFirst();
					else
						return;
				}
				if ((key.interestOps() & SelectionKey.OP_WRITE) == SelectionKey.OP_WRITE)
					key.interestOps(SelectionKey.OP_READ);
			} catch (Exception e) {
				if (logger != null)
					logger.severeLog("Unexpected exception : ", e);

				key.cancel();
				closeConnection(false);
			}
		}

		/*
		 * DatagramChannel getDatagramChannel() { return datagramChannel; }
		 */

		void closeConnection(boolean sentFromMulticastAgent) {
			if (logger != null)
				logger.finer("Closing datagram channel : " + this);

			try {
				personal_datagram_channels.remove(datagramChannel);
				personal_datagram_channels_per_agent_address.remove(multicastAgent);
				personal_datagram_channels_per_ni_address.remove(localAddress);
				datagramChannel.close();
				if (!sentFromMulticastAgent)
					sendMessageWithRole(multicastAgent, new MulticastListenerDeconnectionMessage(),
							LocalCommunity.Roles.NIO_ROLE);
			} catch (Exception e) {
				if (logger != null)
					logger.severeLog("", e);
			}
		}
	}

	private void addPendingConnection(AbstractIP ip, SocketChannel socketChannel, InetSocketAddress inetSocketAddress,
			InetAddress local_interface, AskForConnectionMessage askerMessage) {
		if (logger != null && logger.isLoggable(Level.FINER))
			logger.finer("Pending connection (inetSocketAddress=" + inetSocketAddress + ", local_interface="
					+ local_interface + ")");
		pending_connections
				.add(new PendingConnection(ip, socketChannel, inetSocketAddress, local_interface, askerMessage));
	}

	private void pendingConnectionSuceeded(SocketChannel socketChannel) {

		for (Iterator<PendingConnection> it = pending_connections.iterator(); it.hasNext();) {
			PendingConnection pc = it.next();
			if (pc.isConcernedBy(socketChannel)) {
				if (pc.getAskerMessage() != null && pc.getAskerMessage().getJoinedPiece() != null
						&& pc.getAskerMessage().getOriginalSender() != null
						&& pc.getAskerMessage().getJoinedPiece().getClass() == TryDirectConnection.class) {
					sendMessageWithRole(pc.getAskerMessage().getOriginalSender(),
							new ObjectMessage<>(new TransferAgent.DirectConnectionSuceeded(
									((TryDirectConnection) pc.getAskerMessage().getJoinedPiece()).getIDTransfer())),
							LocalCommunity.Roles.NIO_ROLE);
					pc.getAskerMessage().setJoinedPiece(null, null);
				}

				it.remove();
				return;
			}
		}
		if (logger != null)
			logger.warning("Pending connection not found ! (socketChannel=" + socketChannel + ")");
	}

	private void pendingConnectionFailed(SocketChannel socketChannel, ConnectionClosedReason reason) {
		if (logger != null && logger.isLoggable(Level.FINER))
			logger.finer("Connection FAILED (socketChannel=" + socketChannel + "]");

		for (Iterator<PendingConnection> it = pending_connections.iterator(); it.hasNext();) {
			PendingConnection pc = it.next();
			if (pc.isConcernedBy(socketChannel)) {
				if (pc.getAskerMessage() != null && pc.getAskerMessage().getJoinedPiece() != null
						&& pc.getAskerMessage().getOriginalSender() != null
						&& pc.getAskerMessage().getJoinedPiece() == TryDirectConnection.class) {
					sendMessageWithRole(pc.getAskerMessage().getOriginalSender(),
							new ObjectMessage<>(new TransferAgent.DirectConnectionFailed(
									((TryDirectConnection) pc.getAskerMessage().getJoinedPiece()).getIDTransfer())),
							LocalCommunity.Roles.NIO_ROLE);
				}
				else {
					if (pc.getAskerMessage()==null)
						throw new NullPointerException();

					if (pc.getAskerMessage().getOriginalSender()!=null)
					{
						if (reason==null)
							reason=ConnectionClosedReason.IP_NOT_REACHED;
						sendMessageWithRole(pc.getAskerMessage().getOriginalSender(),
								new ConnectionStatusMessage(ConnectionStatusMessage.Type.DISCONNECT, pc.getIP(), pc.getAskerMessage().getChoosenIP(), pc.getAskerMessage().interface_address, reason, pc.getAskerMessage().getNumberOfAnomalies(), pc.getAskerMessage().getTimeUTCOfAnomaliesCycle()),
								LocalCommunity.Roles.NIO_ROLE);

					}
				}
				it.remove();
				return;
			}
		}
	}

	private static class PendingConnection {
		protected final InetSocketAddress inetSocketAddress;
		protected final InetAddress local_interface;
		private final AskForConnectionMessage askerMessage;
		private final SocketChannel socketChannel;
		private final long timeUTC;
		private final AbstractIP ip;

		PendingConnection(AbstractIP ip, SocketChannel socketChannel, InetSocketAddress inetSocketAddress,
				InetAddress local_interface, AskForConnectionMessage askerMessage) {
			if (inetSocketAddress == null)
				throw new NullPointerException("inetSocketAddress");
			if (local_interface == null)
				throw new NullPointerException("local_interface");
			if (socketChannel == null)
				throw new NullPointerException("socketChannel");
			if (ip == null)
				throw new NullPointerException("ip");

			this.socketChannel = socketChannel;
			this.inetSocketAddress = inetSocketAddress;
			this.local_interface = local_interface;
			this.askerMessage = askerMessage;
			this.ip = ip;
			timeUTC = System.currentTimeMillis();
		}

		InetSocketAddress getInetSocketAddress() {
			return inetSocketAddress;
		}

		AbstractIP getIP() {
			return ip;
		}

		@Override
		public boolean equals(Object o) {
			if (o == null)
				return false;
			if (o.getClass() == PendingConnection.class) {
				PendingConnection pc = (PendingConnection) o;
				return inetSocketAddress.equals(pc.inetSocketAddress) && local_interface.equals(pc.local_interface);
			}
			return false;
		}

		boolean isConcernedBy(SocketChannel socketChannel) {
			if (socketChannel == null)
				return false;
			else
				return this.socketChannel.equals(socketChannel);
		}

		boolean isConcernedBy(NetworkInterface ni) {
			for (Enumeration<InetAddress> e=ni.getInetAddresses();e.hasMoreElements();)
				if (e.nextElement().equals(this.local_interface))
					return true;
			return false;
		}

		boolean isConcernedBy(Server s) throws IOException {
			return this.socketChannel.getLocalAddress().equals(s.address);
		}

		@Override
		public int hashCode() {
			return inetSocketAddress.hashCode() + local_interface.hashCode();
		}

		AskForConnectionMessage getAskerMessage() {
			return askerMessage;
		}

		long getTimeUTC() {
			return timeUTC;
		}

		SocketChannel getSocketChannel() {
			return socketChannel;
		}

	}

}
