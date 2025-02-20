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

import com.distrimind.madkit.agr.LocalCommunity;
import com.distrimind.madkit.exceptions.*;
import com.distrimind.madkit.kernel.*;
import com.distrimind.madkit.kernel.network.AbstractData.DataTransferType;
import com.distrimind.madkit.kernel.network.TransferAgent.IDTransfer;
import com.distrimind.madkit.kernel.network.TransferAgent.TryDirectConnection;
import com.distrimind.madkit.kernel.network.connection.ConnectionProtocol.ConnectionClosedReason;
import com.distrimind.madkit.message.ObjectMessage;
import com.distrimind.util.CircularArrayList;
import com.distrimind.util.Cleanable;
import com.distrimind.util.Reference;
import com.distrimind.util.Timer;
import com.distrimind.util.concurrent.LockerCondition;

import java.io.IOException;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.nio.channels.spi.SelectorProvider;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.util.*;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Represent an server socket selector for {@link SocketChannel}, and
 * {@link DatagramChannel}
 *
 * @author Jason Mahdjoub
 * @version 1.0
 * @since MadkitLanEdition 1.0
 */
@SuppressWarnings({"unused", "UnusedReturnValue"})
final class NIOAgent extends Agent implements Cleanable {

	private static final class Finalizer extends Cleanable.Cleaner
	{
		private Logger logger;
		private final ArrayList<PendingConnection> pending_connections = new ArrayList<>();
		private final ArrayList<PersonalSocket> personal_sockets_list = new ArrayList<>(100);
		private final HashMap<AgentNetworkID, PersonalSocket> personal_sockets = new HashMap<>();
		private final HashMap<DatagramChannel, PersonalDatagramChannel> personal_datagram_channels = new HashMap<>();
		private final HashMap<AgentAddress, PersonalDatagramChannel> personal_datagram_channels_per_agent_address = new HashMap<>();
		private final HashMap<InetAddress, PersonalDatagramChannel> personal_datagram_channels_per_ni_address = new HashMap<>();
		// The channel on which we'll accept connections
		private final ArrayList<Server> serverChannels = new ArrayList<>();
		// The selector we'll be monitoring
		private final Selector selector;
		private Finalizer(NIOAgent cleaner, Logger logger) throws ConnectionException {
			super(cleaner);
			try {
				this.logger=logger;
				selector = SelectorProvider.provider().openSelector();


			} catch (IOException e) {
				throw new ConnectionException(e);
			}
		}

		@Override
		protected void performCleanup() {
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

			//noinspection unchecked
			for (PersonalSocket ps : ((ArrayList<PersonalSocket>) this.personal_sockets_list.clone())) {
				if (!ps.isClosed()) {
					ps.closeConnection(ConnectionClosedReason.CONNECTION_LOST);
					try {
						for (NoBackData ad : ps.noBackDataToSend)
							ad.data.unlockMessage();
					}
					catch (Exception e)
					{
						if (logger != null)
							logger.log(Level.SEVERE, "Unexpected exception", e);
					}
				}
			}

			this.personal_sockets.clear();
			this.personal_sockets_list.clear();


			if (logger != null && logger.isLoggable(Level.FINER))
				logger.finer("Closing all datagram channels !");
			for (Object o : this.personal_datagram_channels.values().toArray()) {
				PersonalDatagramChannel dc = (PersonalDatagramChannel) o;
				dc.closeConnection(false);
			}
			this.personal_datagram_channels.clear();
			this.personal_datagram_channels_per_agent_address.clear();
			this.personal_datagram_channels_per_ni_address.clear();

			if (logger != null && logger.isLoggable(Level.FINER))
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
			if (logger != null && logger.isLoggable(Level.FINER))
				logger.finer("Closing selector !");
			if (selector.isOpen()) {
				try {
					this.selector.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}

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









	private final HashMap<InetAddress, Integer> numberOfConnectionsPerIP = new HashMap<>();


	private boolean stopping = false;
	private AgentAddress myAgentAddress = null;
	private long delayInNanoToWaitToRespectGlobalBandwidthLimit =0;
    private RealTimeTransferStat realTimeGlobalDownloadStat =null, realTimeGlobalUploadStat=null;
    private double realTimeDownloadStatDuration=0.0, realTimeUploadStatDuration=0.0;
	private volatile LockerCondition actualLocker=null;
	private final Finalizer finalizer;
	NIOAgent() throws ConnectionException {
		finalizer=new Finalizer(this, logger);


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



	private long getDownloadToWaitInMsToBecomeUnderBandwidthLimit()
    {
        long max=getMaximumGlobalDownloadSpeedInBytesPerSecond();

        if (max!=Integer.MAX_VALUE)
        {
			double l= (double)(max*2L);

            if (realTimeGlobalDownloadStat.isOneCycleDone()) {
                double v = ((double) realTimeGlobalDownloadStat.getNumberOfIdentifiedBytesDuringTheLastCycle()) / realTimeDownloadStatDuration ;
                if (v >= l) {
                    return (long) ((v - l) / l * 1000.0);
                }
            }
        }

        return 0;
    }

    private long getUploadToWaitInMsToBecomeUnderBandwidthLimit()
    {
        long max=getMaximumGlobalUploadSpeedInBytesPerSecond();


        if (max!=Integer.MAX_VALUE)
        {
			double l= (double)(max*2L);

            if (realTimeGlobalUploadStat.isOneCycleDone()) {

                double v = ((double) realTimeGlobalUploadStat.getNumberOfIdentifiedBytesDuringTheLastCycle()) / realTimeUploadStatDuration ;
                if (v>=l)
                {
                    return (long)((v-l)/l*1000.0);
                }
            }

        }


        return 0;
    }

	@Override
	public void setLogLevel(Level newLevel) {
		super.setLogLevel(newLevel);
		finalizer.logger=logger;
	}

	@Override
	protected void end() {
		clean();

		if (logger != null && logger.isLoggable(Level.INFO))
			logger.info("NIOAgent KILLED !");

	}

	@Override
	public Message receiveMessage(Message _m) {
		_m = super.receiveMessage(_m);
		this.finalizer.selector.wakeup();
		LockerCondition lc=actualLocker;
		if (lc!=null)
			lc.notifyLocker();
		return _m;
	}
	private void checkPingPongMessagesAndPendingConnections() throws IOException {
		ArrayList<PersonalSocket> connections_to_close = new ArrayList<>();
		for (PersonalSocket ps : finalizer.personal_sockets_list) {
			if (ps.isWaitingForPongMessage()) {
				if (System.nanoTime()
						- ps.getTimeSendingPingMessageNano() > getMadkitConfig().networkProperties.connectionTimeOutInMs *1000000L) {
					if (logger != null && logger.isLoggable(Level.FINER))
						logger.finer("Pong not received, closing connection : " + ps);

					connections_to_close.add(ps);
				}
			} else {
				StatsBandwidth sb = ps.agentSocket.getStatistics();
				RealTimeTransferStat upload = sb.getBytesUploadedInRealTime(
						NetworkProperties.DEFAULT_TRANSFER_STAT_IN_REAL_TIME_PER_30_SECONDS_SEGMENTS);
				boolean ping = false;
				if (ps.hasDataToSend() && upload.getNumberOfIdentifiedBytesDuringTheLastCycle() == 0 && System.nanoTime()
						- ps.getLastDataWroteNano() > getMadkitConfig().networkProperties.connectionTimeOutInMs *1000000L) {

					ping = true;
				} else {
					RealTimeTransferStat download = sb.getBytesDownloadedInRealTime(
							NetworkProperties.DEFAULT_TRANSFER_STAT_IN_REAL_TIME_PER_30_SECONDS_SEGMENTS);

					if (download.getNumberOfIdentifiedBytesDuringTheLastCycle() == 0 && upload.getNumberOfIdentifiedBytesDuringTheLastCycle() == 0
							&& download.isOneCycleDone() && upload.isOneCycleDone()) {
						ping = true;
					} else if (sb
							.getBytesDownloadedInRealTime(
									NetworkProperties.DEFAULT_TRANSFER_STAT_IN_REAL_TIME_PER_5_MINUTES_SEGMENTS)
							.getNumberOfIdentifiedBytesDuringTheLastCycle() == 0
							&& System.nanoTime() - ps
							.getLastDataWroteNano() > getMadkitConfig().networkProperties.connectionTimeOutInMs *1000000L) {
						ping = true;
					}

				}
				if (ping) {
					if (logger != null && logger.isLoggable(Level.FINEST))
						logger.finest("Sending ping message : " + ps.finalizer.socketChannel.getRemoteAddress());

					ps.sendPingMessage();
				}
			}
		}
		if (finalizer.pending_connections.size() > 0) {
			ArrayList<PendingConnection> pendingConnectionsFinished = new ArrayList<>();
			ArrayList<PendingConnection> pendingConnectionsCanceled = new ArrayList<>();

			for (PendingConnection pc : finalizer.pending_connections) {
				try {
					if (pc.getSocketChannel().isConnectionPending() && pc.getSocketChannel().finishConnect())
						pendingConnectionsFinished.add(pc);
					else if (pc.getNanoTime() + getMadkitConfig().networkProperties.connectionTimeOutInMs*1000000L < System
							.nanoTime()) {
						pendingConnectionsCanceled.add(pc);
					}
				} catch (ConnectException e) {
					if (logger != null && logger.isLoggable(Level.INFO))
						logger.info(e + ", local_interface=" + pc.local_interface + ", ip="
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
	}
	void handleReceivedMessages() throws IOException, TransferException {
		// checking messages
		Message m = nextMessage();
		while (m != null) {
			if (m instanceof DataToSendMessage) {
				DataToSendMessage dtsm = (DataToSendMessage) m;
				PersonalSocket ps = finalizer.personal_sockets.get(dtsm.socket);
				if (ps != null)
					ps.addDataToSend(dtsm.data);
				else
				{
					try {
						((DataToSendMessage) m).data.cancel();
					} catch (MadkitException e) {
						if (logger!=null)
							logger.severeLog("Cannot close packet data", e);
					}
					sendReply(m, new ConnectionClosed(dtsm.socket, ConnectionClosedReason.CONNECTION_LOST, null,
							null, null));

					if (logger != null)
						logger.warning("Receiving data to send, but personal socket not found ! ");
				}

			} else if (m instanceof ConnectionClosed) {
				if (logger != null && logger.isLoggable(Level.FINER))
					logger.finer("Receiving : " + m);
				ConnectionClosed cc = (ConnectionClosed) m;
				PersonalSocket ps = finalizer.personal_sockets.get(cc.socket);
				if (ps != null)
					ps.closeConnection(cc.reason);
			} else if (m.getClass() == AbstractAgentSocket.AgentSocketKilled.class) {
				if (finalizer.personal_sockets_list.size() == 0 && NIOAgent.this.stopping)
					NIOAgent.this.killAgent(NIOAgent.this);

			} else if (m instanceof PongMessageReceived) {
				PongMessageReceived pmr = (PongMessageReceived) m;
				PersonalSocket ps = finalizer.personal_sockets.get(pmr.socket);
				ps.pongMessageReceived();
			}

			else if (m instanceof BindInetSocketAddressMessage) {
				if (logger != null && logger.isLoggable(Level.FINER))
					logger.finer("Receiving : " + m);
				BindInetSocketAddressMessage message = (BindInetSocketAddressMessage) m;
				InetSocketAddress addr = message.getInetSocketAddress();
				if (message.getType().equals(BindInetSocketAddressMessage.Type.BIND)) {
					if (this.getMadkitConfig().networkProperties.needsServerSocket(addr, addr.getPort())) {
						boolean alreadyBind = false;
						for (Server s : finalizer.serverChannels) {

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
								serverChannel.register(finalizer.selector, SelectionKey.OP_ACCEPT);
								Server s = new Server(serverChannel, addr);
								finalizer.serverChannels.add(s);
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
					boolean multicastToRemove = finalizer.personal_datagram_channels_per_ni_address.size() > 0;
					for (Iterator<Server> it = finalizer.serverChannels.iterator(); it.hasNext();) {
						Server s = it.next();
						if (s.address.equals(addr)) {
							s.serverChannels.close();

							List<PendingConnection> pcs=new ArrayList<>();
							for (PendingConnection pc : this.finalizer.pending_connections)
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

							for (PersonalSocket ps : this.finalizer.personal_sockets_list)
							{
								if (ps.isConcernedBy(s))
									sendMessage(ps.agentAddress,
											new AskForConnectionMessage(ConnectionClosedReason.CONNECTION_PROPERLY_CLOSED,
													ps.agentSocket.distantIP,
													(InetSocketAddress) ps.finalizer.socketChannel.getRemoteAddress(), null, false, false));
							}

							it.remove();

							if (finalizer.personal_datagram_channels_per_ni_address.size() == 0)
								break;
							if (logger != null && logger.isLoggable(Level.FINER))
								logger.finer("Server channel closed : " + s);

						} else if (s.address.getAddress().equals(addr.getAddress()))
							multicastToRemove = true;
					}
					if (multicastToRemove)
						finalizer.personal_datagram_channels_per_ni_address.get(addr.getAddress()).closeConnection(false);
				}
			} else if (m instanceof UpnpIGDAgent.NetworkInterfaceInformationMessage) {
				UpnpIGDAgent.NetworkInterfaceInformationMessage nm=(UpnpIGDAgent.NetworkInterfaceInformationMessage)m;
				for (NetworkInterface ni : nm.getNewDisconnectedInterfaces())
				{
					for (PersonalSocket ps : this.finalizer.personal_sockets_list)
					{
						sendMessage(ps.agentAddress,
								new AskForConnectionMessage(ConnectionClosedReason.CONNECTION_LOST,
										ps.agentSocket.distantIP,
										(InetSocketAddress) ps.finalizer.socketChannel.getRemoteAddress(), null, false, false));
					}
					List<PendingConnection> pcs=new ArrayList<>();
					for (PendingConnection pc : this.finalizer.pending_connections)
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
					for (Iterator<Server> it = finalizer.serverChannels.iterator(); it.hasNext();) {
						Server s = it.next();
						if (s.isConcernedBy(ni)) {
							PersonalDatagramChannel mc=finalizer.personal_datagram_channels_per_ni_address.get(s.address.getAddress());

							s.serverChannels.close();
							it.remove();

							if (mc!=null)
								mc.closeConnection(false);
							if (logger != null && logger.isLoggable(Level.FINER))
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
						PersonalSocket found = getPersonalSocket(con.getChosenIP(), con.interface_address);
						if (found != null) {
							if (con.concernsIndirectConnection())
								found.closeIndirectConnection(con.connection_closed_reason, con.getIDTransfer(),
										con.getIndirectAgentAddress());
							else
								found.closeConnection(con.connection_closed_reason);
						} else {
							if (logger != null && logger.isLoggable(Level.FINEST))
								logger.finest("Connection to close not found (address=" + con.getChosenIP()
										+ ", interface_address=" + con.interface_address + ", closeReason="
										+ con.connection_closed_reason + ")");
						}
					} else {
						if (con.type.equals(ConnectionStatusMessage.Type.CONNECT)) {
							if (!stopping)
								initiateConnection(con.getIP(), con.getChosenIP(),
										con.interface_address.getAddress(), con);
						} else if (con.type.equals(ConnectionStatusMessage.Type.DISCONNECT)) {
							parsePersonalSockets(con.getIP(), con.interface_address, found->{
								if (con.connection_closed_reason == null || con.connection_closed_reason
										.equals(ConnectionClosedReason.CONNECTION_PROPERLY_CLOSED)) {
									sendMessage(found.agentAddress,
											new AskForConnectionMessage(
													ConnectionClosedReason.CONNECTION_PROPERLY_CLOSED, con.getIP(),
													con.getChosenIP(), con.interface_address, false, false));
								} else {
									sendMessage(found.agentAddress, con);
								}
							});
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
					PersonalDatagramChannel pdc = finalizer.personal_datagram_channels_per_agent_address.get(m.getSender());
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
				finalizer.personal_datagram_channels_per_agent_address.get(m.getSender()).closeConnection(true);
			} else if (m instanceof NetworkAgent.StopNetworkMessage) {
				if (logger != null && logger.isLoggable(Level.FINER))
					logger.finer("Receiving stop network order");

				boolean kill = true;
				stopping = true;

				NetworkAgent.StopNetworkMessage message = (NetworkAgent.StopNetworkMessage) m;
				for (Server s : this.finalizer.serverChannels)
					s.serverChannels.close();
				List<PendingConnection> pcs=new ArrayList<>(this.finalizer.pending_connections);
				for (PendingConnection pc : pcs)
					pendingConnectionFailed(pc.socketChannel, ConnectionClosedReason.CONNECTION_PROPERLY_CLOSED);
				for (PersonalSocket ps : finalizer.personal_sockets_list) {
					kill = false;
					sendMessage(ps.agentAddress,
							new AskForConnectionMessage(message.getNetworkCloseReason().getConnectionClosedReason(),
									ps.agentSocket.distantIP,
									(InetSocketAddress) ps.finalizer.socketChannel.getRemoteAddress(), null, false, false));
				}

				while (finalizer.personal_datagram_channels.size() > 0)
					finalizer.personal_datagram_channels.values().iterator().next().closeConnection(false);
				LockerCondition lc=actualLocker;
				if (lc!=null)
					lc.notifyLocker();
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
			else if (m.getClass()==PauseBigDataTransferMessage.class)
			{
				PauseBigDataTransferMessage p=(PauseBigDataTransferMessage)m;
				finalizer.personal_sockets.forEach((k,v) -> {
					if (p.getKernelAddress()==null || v.agentSocket.distantInterfacedKernelAddress.equals(p.getKernelAddress()))
						v.setBigDataTransferPaused(p.isTransferSuspended());
				});
			}
			else if (logger!=null)
				logger.warning("Unexpected message "+m);
			m = nextMessage();
		}
	}

	@SuppressWarnings({"SuspiciousMethodCalls"})
    @Override
	protected void liveCycle() {
		try {
			// Wait for an event one of the registered channels
			long delay;
			if (finalizer.pending_connections.size() > 0)
				delay = getMadkitConfig().networkProperties.selectorTimeOutInMsWhenWaitingPendingConnections;
			else {
				delay = getMadkitConfig().networkProperties.selectorTimeOutInMs;
				if (delayInNanoToWaitToRespectGlobalBandwidthLimit > 0) {

					delayInNanoToWaitToRespectGlobalBandwidthLimit -= System.nanoTime();

					if (delayInNanoToWaitToRespectGlobalBandwidthLimit > 0) {
						delayInNanoToWaitToRespectGlobalBandwidthLimit /=1000000L;
						try {
							this.sleep(delayInNanoToWaitToRespectGlobalBandwidthLimit);
						}
						catch (InterruptedException ignored)
						{

						}
						delay -= delayInNanoToWaitToRespectGlobalBandwidthLimit;
						if (delay < 0)
							delay = 0;
						delayInNanoToWaitToRespectGlobalBandwidthLimit = 0;
					}
				}
			}
            if (delay>0 && isMessageBoxEmpty()) {
				this.finalizer.selector.select(delay);
			}


			checkPingPongMessagesAndPendingConnections();

			handleReceivedMessages();

            // Iterate over the set of keys for which events are available
			Set<SelectionKey> selectedKeys=this.finalizer.selector.selectedKeys();
			Iterator<SelectionKey> selectedKeysIterator = selectedKeys.iterator();
			boolean hasSpeedLimitation=hasNetworkSpeedLimitationDuringDownloadOrDuringUpload();
            long limitDownload=Long.MIN_VALUE;
            long limitUpload=Long.MIN_VALUE;
			while (selectedKeysIterator.hasNext() && isMessageBoxEmpty()) {
				SelectionKey key = selectedKeysIterator.next();


				if (!key.channel().isOpen() || !key.isValid()) {
					if (hasSpeedLimitation)
                    	selectedKeysIterator.remove();
				}
				else if (key.isAcceptable()) {
					if (hasSpeedLimitation)
                    	selectedKeysIterator.remove();
					this.accept(key);
				} else {
					if (key.isReadable()) {
						if (hasSpeedLimitation) {
							long ld = getDownloadToWaitInMsToBecomeUnderBandwidthLimit()*1000000L;
							if (ld > 0) {
								long curTime = System.nanoTime();
								limitDownload = ld + curTime;
								if (limitUpload > curTime) {
									this.delayInNanoToWaitToRespectGlobalBandwidthLimit = Math.min(limitDownload, limitUpload);
									return;
								}
								continue;
							}
							selectedKeysIterator.remove();
						}
						SelectableChannel sc = key.channel();
						if (sc instanceof SocketChannel)
							((PersonalSocket) key.attachment()).read(key);
						else if (sc instanceof DatagramChannel) {
							finalizer.personal_datagram_channels.get(sc).read(key);
						}
					} else if (key.isValid() && key.isWritable()) {
						if (hasSpeedLimitation) {
							long lu = getUploadToWaitInMsToBecomeUnderBandwidthLimit()*1000000L;

							if (lu > 0) {
								long curTime = System.nanoTime();
								limitUpload = lu + curTime;
								if (limitDownload > curTime) {
									this.delayInNanoToWaitToRespectGlobalBandwidthLimit = Math.min(limitDownload, limitUpload);
									return;
								}
								continue;
							}

							selectedKeysIterator.remove();
						}
						SelectableChannel sc = key.channel();
						if (sc instanceof SocketChannel) {
							PersonalSocket ps = (PersonalSocket) key.attachment();
							if (ps != null)
								ps.write(key);
							else if (logger != null)
								logger.warning("Personal socket not found " + sc);
						} else {
							finalizer.personal_datagram_channels.get(sc).write(key);
						}

					}
					else if (hasSpeedLimitation)
                        selectedKeysIterator.remove();

				}
			}
			if (!hasSpeedLimitation)
				selectedKeys.clear();
            long curTime=System.nanoTime()/1000000L;
			if (limitDownload>curTime)
			    this.delayInNanoToWaitToRespectGlobalBandwidthLimit =limitDownload;
			else if (limitUpload>curTime)
                this.delayInNanoToWaitToRespectGlobalBandwidthLimit =limitUpload;
			else
                this.delayInNanoToWaitToRespectGlobalBandwidthLimit =0;

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
			if (!stopping && getMadkitConfig().networkProperties.autoConnectWithLocalSitePeers
					&& !finalizer.personal_datagram_channels_per_ni_address.containsKey(addr.getAddress())) {
				for (InetAddress ia : finalizer.personal_datagram_channels_per_ni_address.keySet()) {
					if (InetAddressFilter.isSameLocalNetwork(addr.getAddress(), ia))
						return;
				}

				finalizer.personal_datagram_channels_per_ni_address.put(addr.getAddress(), null);
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

		PersonalDatagramChannel pdc = new PersonalDatagramChannel(dc,
				DatagramLocalNetworkPresenceMessage.getMaxDatagramMessageLength(), mlcm.getSender(),
				mlcm.getNetworkInterfaceAddress(), mlcm.getGroupIPAddress(), mlcm.getPort());
		finalizer.personal_datagram_channels.put(dc, pdc);
		finalizer.personal_datagram_channels_per_agent_address.put(mlcm.getSender(), pdc);
		finalizer.personal_datagram_channels_per_ni_address.put(mlcm.getNetworkInterfaceAddress(), pdc);
		if (logger != null && logger.isLoggable(Level.FINER))
			logger.finer("Multicast listener added : " + mlcm);

	}
	private void parsePersonalSockets(AbstractIP _distant_inet_address,
											 InetSocketAddress _local_interface_address, Consumer<PersonalSocket> consumer) {
		for (PersonalSocket ps : finalizer.personal_sockets_list) {
			try {
				InetSocketAddress isa_local = (InetSocketAddress) ps.finalizer.socketChannel.getLocalAddress();
				if (_local_interface_address==null || isa_local.equals(_local_interface_address)) {
					InetSocketAddress isa_remote = (InetSocketAddress) ps.finalizer.socketChannel.getRemoteAddress();
					if (isa_remote.getPort()==_distant_inet_address.getPort()) {
						for (InetAddress ia : _distant_inet_address.getInetAddresses()) {
							if (isa_remote.getAddress().equals(ia)) {
								consumer.accept(ps);
							}
						}
					}

				}
			} catch (Exception e) {
				if (logger != null)
					logger.log(Level.SEVERE, "Unexpected exception", e);

			}
		}
	}
	private PersonalSocket getPersonalSocket(InetSocketAddress _distant_inet_address,
			InetSocketAddress _local_interface_address) {
		PersonalSocket found = null;
		for (PersonalSocket ps : finalizer.personal_sockets_list) {
			try {
				InetSocketAddress isa_local = (InetSocketAddress) ps.finalizer.socketChannel.getLocalAddress();
				if (_local_interface_address==null || isa_local.equals(_local_interface_address)) {
					InetSocketAddress isa_remote = (InetSocketAddress) ps.finalizer.socketChannel.getRemoteAddress();
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
			if (ok) {
				finishConnection(ip, socket);
			}

			return socket;
		} catch (IOException e) {
			if (logger != null)
				logger.severeLog("Connection failed ", e);
			if (socket != null)
				pendingConnectionFailed(socket, null);
			throw e;
		}
	}


	private void finishConnection(AbstractIP ip, SocketChannel sc) throws IOException {
		if (stopping) {
			return;
		}
		try {
			InetSocketAddress isaRemote = (InetSocketAddress) sc.getRemoteAddress();
			InetSocketAddress isaLocal = (InetSocketAddress) sc.getLocalAddress();

			if (logger != null && logger.isLoggable(Level.FINER))
				logger.finer("Finishing connection (localInetAddress=" + isaLocal + ", removeInetAddress=" + isaRemote
						+ "]");

			try {
				sc.finishConnect();

				SelectionKey clientKey = sc.register(this.finalizer.selector, SelectionKey.OP_READ);
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

						PersonalSocket ps = new PersonalSocket(socketChannel, agent, clientKey);

						finalizer.personal_sockets.put(agent.getNetworkID(), ps);
						finalizer.personal_sockets_list.add(ps);

						broadcastMessageWithRole(LocalCommunity.Groups.LOCAL_NETWORKS,
								LocalCommunity.Roles.LOCAL_NETWORK_ROLE,
								new ConnectionStatusMessage(ConnectionStatusMessage.Type.CONNECT, ip, isaRemote,
										isaLocal),
								LocalCommunity.Roles.NIO_ROLE);

						if (local_asking)
							pendingConnectionSucceeded(socketChannel);

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

		if (stopping) {
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

				SelectionKey clientKey = socketChannel.register(this.finalizer.selector,
						SelectionKey.OP_READ);

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

	private static final class FirstDataFinalizer extends AbstractData.Finalizer
	{
		private boolean unlocked = false;

		private FirstDataFinalizer() {
			super();
		}

		@Override
		void unlockMessage(boolean cancel) {
			unlocked = true;
		}

		@Override
		boolean isUnlocked() {
			return unlocked;
		}
	}
	private static class FirstData extends AbstractData<FirstDataFinalizer> {

		ByteBuffer data;

		private final IDTransfer id;

		FirstData(NIOAgent agent, DatagramLocalNetworkPresenceMessage message) throws IOException, OverflowException {
			super(true, new FirstDataFinalizer());
			data = new DatagramData(message).getByteBuffer();
			id = IDTransfer.generateIDTransfer(MadkitKernelAccess.getIDTransferGenerator(agent));
		}

		@Override
		boolean isDataBuildInProgress()
		{
			return false;
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

		@Override
		boolean isCanceledNow()
		{
			if (isCanceled())
				return data==null || data.remaining()==0 || data.position()==0;
			else
				return false;
		}

		@Override
		boolean isFinished() {
			return data==null;
		}

		@Override
		boolean isCurrentByteBufferFinished() {
			return data==null;
		}
		@Override
		public boolean isCurrentByteBufferFinishedOrNotStarted() {
			return isCurrentByteBufferFinished();
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

		@Override
		Object getLocker()
		{
			return null;
		}


	}

	private class NoBackData
	{
		private final PersonalSocket personalSocket;
		private final AbstractData<?> data;
		private volatile ByteBuffer buffer;
		private TransferException pendingException =null;
		private IDTransfer idTransfer=null;


		private NoBackData(PersonalSocket personalSocket, AbstractData<?> data) {
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

		boolean isReady() throws TransferException
		{
			throwPendingException();


			try
			{

				return buffer!=null || !data.isCurrentByteBufferFinished();
			}
			catch(PacketException e)
			{
				throw new TransferException(e);
			}
		}

		boolean isCanceled()
		{
			return data.isUnlocked();
		}



		boolean isDataLoadingCanceled() throws TransferException, PacketException
		{
			takeNextData();
			return buffer==null && data.isFinished();
		}

		Object getLocker()
		{
			return data.getLocker();
		}

		void takeNextData() throws TransferException
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
		private void throwPendingException() throws TransferException
		{
			if (pendingException !=null)
			{
				try
				{
					throw pendingException;
				}
				finally
				{
					pendingException =null;
				}
			}
		}
		ByteBuffer getBuffer() throws TransferException
		{
			throwPendingException();
			takeNextData();
			if (buffer==null) {
				throw new InternalError();
			}
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
	private static final class PersonalSocketFinalizer extends Cleaner
	{
		public final SocketChannel socketChannel;

		private PersonalSocketFinalizer(PersonalSocket cleaner, SocketChannel socketChannel) {
			super(cleaner);
			this.socketChannel = socketChannel;
		}

		@Override
		protected void performCleanup() {
			try {
				if (this.socketChannel.isConnected())
					this.socketChannel.close();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
	private class PersonalSocket implements Cleanable{
		private final PersonalSocketFinalizer finalizer;
		private final AgentAddress agentAddress;
		private final AgentSocket agentSocket;

		protected CircularArrayList<AbstractData<?>> shortDataToSend ;
		protected CircularArrayList<DistantKernelAgent.BigPacketData> bigDataToSend ;
		protected CircularArrayList<AbstractAgentSocket.BlockDataToTransfer> dataToTransfer ;
		private final Deque<NoBackData> noBackDataToSend;
		// private LinkedList<FileData> bigDataWaiting=new LinkedList<>();
		private DataTransferType dataTransferType = DataTransferType.SHORT_DATA;
		protected int bigDataToSendIndex = 0;
		private boolean waitingForPongMessage = false;
		private long time_sending_ping_messageNano;
		private long last_data_wrote_nano;
		// private int read_locked=0;
		private boolean is_closed = false;
		private ConnectionClosedReason cs=null;
		private DatagramData firstReceivedData = new DatagramData();
		private boolean firstPacketSent = false;
		private final ByteBuffer readSizeBlock = ByteBuffer.allocate(Block.getBlockSizeLength());
		private ByteBuffer readBuffer = null;
		private final int maxBlockSize;
		private volatile boolean canPrepareNextData=true;
		private boolean bigDataTransferPaused=false;
		private final SelectionKey clientKey;
		private final int numberOfNoBackData;
		public boolean isClosed() {
			return is_closed;
		}

		private void initDataToSendLists()
		{
			shortDataToSend=new CircularArrayList<>(5);
			bigDataToSend=new CircularArrayList<>(5);
			dataToTransfer=new CircularArrayList<>(5);
		}

		void setBigDataTransferPaused(boolean bigDataTransferPaused) {
			this.bigDataTransferPaused = bigDataTransferPaused;

			if (!is_closed && hasDataToSend()) {
				try {
					setNextTransferType();
					prepareNextDataToNextIfNecessary();
				} catch (TransferException e) {
					if (logger != null)
						logger.severeLog("unexpected exception", e);
					closeConnection(ConnectionClosedReason.CONNECTION_ANOMALY);
					if (actualLocker != null) {
						actualLocker.notifyLocker();
					}

					return;
				}
				if ((clientKey.interestOps() & SelectionKey.OP_WRITE) != SelectionKey.OP_WRITE)
					clientKey.interestOps(SelectionKey.OP_WRITE | SelectionKey.OP_READ);
				if (!bigDataTransferPaused) {
					if (actualLocker != null) {
						actualLocker.notifyLocker();
					}
				}
			}

		}

		public PersonalSocket(SocketChannel _socketChannel, AgentSocket _agent, SelectionKey clientKey)
				throws OverflowException, IOException, NoSuchAlgorithmException, NoSuchProviderException, TransferException {
			if (clientKey==null)
				throw new NullPointerException();
			initDataToSendLists();
			numberOfNoBackData=3;
			noBackDataToSend=new CircularArrayList<>(numberOfNoBackData);
			finalizer=new PersonalSocketFinalizer(this, _socketChannel);
			this.clientKey=clientKey;
			clientKey.attach(this);
			agentSocket = _agent;
			agentAddress = agentSocket.getAgentAddressIn(LocalCommunity.Groups.NETWORK,
					LocalCommunity.Roles.SOCKET_AGENT_ROLE);
			last_data_wrote_nano = time_sending_ping_messageNano = System.nanoTime();
			maxBlockSize=_agent.getMaxBlockSize();

			addDataToSend(new FirstData(NIOAgent.this,
					new DatagramLocalNetworkPresenceMessage(System.currentTimeMillis(),
							getMadkitConfig().projectVersion, getMadkitConfig().madkitVersion, getMadkitConfig().minimumProjectVersion, getMadkitConfig().minimumMadkitVersion, null,
							getKernelAddress())));
		}

		void prepareNextDataToNextIfNecessary() throws TransferException
		{


			if (canPrepareNextData && noBackDataToSend.size()<numberOfNoBackData)
			{
				AbstractData<?> ad=getNextData();
				if (ad!=null)
				{
					NoBackData nbd=new NoBackData(this, ad);
					noBackDataToSend.addLast(nbd);
					nbd.takeNextData();
					waitDataReady();
				}
			}
		}

		NoBackData getNextNoBackData() throws TransferException
		{
		    if (is_closed)
		        return null;
			NoBackData res;

			do
			{
				if (noBackDataToSend.size()==0) {
					try {
						setNextTransferType();
						prepareNextDataToNextIfNecessary();
					} catch (TransferException e) {
						if (logger != null)
							logger.severeLog("unexpected exception", e);
						closeConnection(ConnectionClosedReason.CONNECTION_ANOMALY);
						return null;
					}
					if (noBackDataToSend.size()==0)
						return null;
				}
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
					throw new TransferException(e);
				}
			} while(res==null);


			if (!res.isReady())
				if (!waitDataReady())
					return null;
			return res;
		}
		private boolean freeNoBackData() throws PacketException, TransferException
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
		long getLastDataWroteNano() {
			return last_data_wrote_nano;
		}

		@Override
		public String toString() {
			SocketAddress local = null, remote = null;
			try {
				local = finalizer.socketChannel.getLocalAddress();
				remote = finalizer.socketChannel.getRemoteAddress();
			} catch (Exception ignored) {

			}
			return "Socket[localAddress" + local + ", remoteAddress=" + remote + ", agentSocket=" + agentSocket + "]";
		}



		public boolean isReadLocked() {
			return /* read_locked>1 || */ agentSocket.isTransferReadPaused();
		}

		public void sendPingMessage() {
			if (agentAddress!=null)
				NIOAgent.this.sendMessage(agentAddress, new SendPingMessage());
			time_sending_ping_messageNano = System.nanoTime();
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

		public long getTimeSendingPingMessageNano() {
			return time_sending_ping_messageNano;
		}

		public boolean hasDataToSend() {
			return shortDataToSend.size()>0 || (!bigDataTransferPaused && bigDataToSend.size()>0) || dataToTransfer.size()>0;
		}

		public boolean addDataToSend(AbstractData<?> _data) throws TransferException {



			if (logger != null && logger.isLoggable(Level.FINEST))
				logger.finest(this + " - data to send : " + _data);
			boolean opWrite=true;
			switch (_data.getDataTransferType()) {
				case SHORT_DATA:

					if (_data.isPriority() && shortDataToSend.size() > 0) {

						int i = 1;


						while (i<shortDataToSend.size()) {
							if (!shortDataToSend.get(i).isPriority()) {
								break;
							}
							else
								++i;
						}
						shortDataToSend.add(i, _data);


					} else
						shortDataToSend.add(_data);
					break;
				case BIG_DATA:
					bigDataToSend.add((DistantKernelAgent.BigPacketData) _data);
					if (bigDataTransferPaused)
						opWrite=false;
					break;
				case DATA_TO_TRANSFER:
					dataToTransfer.addLast((AbstractAgentSocket.BlockDataToTransfer) _data);
					break;
			}
			checkValidTransferType();
			prepareNextDataToNextIfNecessary();

			if (!is_closed && (opWrite || (opWrite=hasDataToSend())) && (clientKey.interestOps() & SelectionKey.OP_WRITE) != SelectionKey.OP_WRITE)
				clientKey.interestOps(SelectionKey.OP_WRITE | SelectionKey.OP_READ);

			return opWrite;

		}

		private boolean hasPriorityDataToSend() {
			return shortDataToSend.size() > 0 && shortDataToSend.get(0).isPriority();
		}

		private boolean waitDataReady() throws TransferException {
			try {
				if (actualLocker!=null)
					return false;
				final Reference<Boolean> hasData = new Reference<>(this.noBackDataToSend.size()>0);
				final NoBackData first=this.noBackDataToSend.peekFirst();
				final Reference<Boolean> validData = new Reference<>(first!=null && first.isReady());


				if (!hasData.get() || validData.get())
					return hasData.get();
				if (first==null)
					throw new NullPointerException();
				final Reference<TransferException> exception=new Reference<>();

				NIOAgent.this.wait(actualLocker=new LockerCondition(first.getLocker()) {
					/*private boolean secondLoop=false;


					@Override
					public void beforeCycleLocking() {
						if (secondLoop) {
							try {
								//checkPingPongMessagesAndPendingConnections();
								boolean oneMessage=!isMessageBoxEmpty();
								handleReceivedMessages();
								if (oneMessage && !isLocked())
									cancelLock();
							} catch (IOException e) {
								e.printStackTrace();
								cancelLock();
							} catch (TransferException e) {
								exception.set(e);
								cancelLock();
							}
						}
					}

					@Override
					public void afterCycleLocking() {
						//secondLoop=true;

					}*/

					@Override
					public boolean isLocked() {
						NoBackData first=noBackDataToSend.peekFirst();
						try
						{
							hasData.set(first!=null);
							validData.set(first!=null && first.isReady());
						}
						catch(TransferException e)
						{
							exception.set(e);
							return false;
						}

						return exception.get()==null && first!=null && !validData.get() && NIOAgent.this.getState().compareTo(AbstractAgent.State.ENDING)<0 && agentSocket.getState().compareTo(AbstractAgent.State.ENDING)<0;
					}


				}, getMadkitConfig().networkProperties.connectionTimeOutInMs);

				if (exception.get()!=null)
					throw exception.get();
				return validData.get();
				//}
			} catch (InterruptedException | TimeoutException e) {
				return false;
			} finally {
				actualLocker=null;
			}
		}

		private boolean isTransferTypeChangePossible() throws TransferException {
			AbstractData<?> data = null;
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
				return data.isCurrentByteBufferFinishedOrNotStarted() ;
			} catch (PacketException e) {
				throw new TransferException("dataTransferType="+dataTransferType,e);
			}

		}


		private boolean purgeObsoleteDataAndTellsIfHasDataToSend()
		{
			boolean r=purgeObsoleteDataAndTellsIfHasDataToSend(shortDataToSend);
			r|=purgeObsoleteDataAndTellsIfHasDataToSend(dataToTransfer);
			{
				while (bigDataToSend.size()>0)
				{
					AbstractData<?> ad=bigDataToSend.get(bigDataToSendIndex);
					if (ad.isCanceledNow()) {
						try {
							ad.closeStream();
						} catch (IOException e) {
							if (logger!=null)
								logger.severeLog("", e);
						}
						bigDataToSend.remove(bigDataToSendIndex);
						if (bigDataToSend.size()>0)
							bigDataToSendIndex=bigDataToSendIndex%bigDataToSend.size();
						else
							bigDataToSendIndex=0;
					}
					else
						return true;
				}
			}
			return r;
		}
		private boolean purgeObsoleteDataAndTellsIfHasDataToSend(CircularArrayList<? extends AbstractData<?>> l)
		{
			while (l.size()>0)
			{
				AbstractData<?> ad=l.getFirst();
				if (ad.isCanceledNow()) {
					try {
						ad.closeStream();
					} catch (IOException e) {
						if (logger!=null)
							logger.severeLog("", e);
					}
					l.removeFirst();
				}
				else
					return true;
			}
			return false;
		}

		private void checkValidTransferType() throws TransferException {
				if (!isTransferTypeChangePossible())
					return;
				boolean valid_data=purgeObsoleteDataAndTellsIfHasDataToSend();
				if (!valid_data || is_closed || hasPriorityDataToSend()) {
					dataTransferType = DataTransferType.SHORT_DATA;

				} else {


					switch (dataTransferType) {
						case SHORT_DATA:
							if (shortDataToSend.size() == 0) {
								if (bigDataToSend.size()>0)
									dataTransferType = DataTransferType.BIG_DATA;
								else if (dataToTransfer.size()>0)
									dataTransferType=DataTransferType.DATA_TO_TRANSFER;
							}

							break;
						case BIG_DATA:
							if (bigDataTransferPaused || bigDataToSend.size() == 0) {
								if (dataToTransfer.size()>0)
									dataTransferType = DataTransferType.DATA_TO_TRANSFER;
								else
									dataTransferType = DataTransferType.SHORT_DATA;
							}

							break;
						case DATA_TO_TRANSFER:
							if (dataToTransfer.size() == 0) {
								if (shortDataToSend.size()>0)
									dataTransferType = DataTransferType.SHORT_DATA;
								else if (bigDataToSend.size()>0)
									dataTransferType=DataTransferType.BIG_DATA;
								else
									dataTransferType = DataTransferType.SHORT_DATA;

							}

							break;
						}
				}



		}
		private boolean setNextTransferType() throws TransferException {

			if (!purgeObsoleteDataAndTellsIfHasDataToSend() || is_closed) {
				dataTransferType = DataTransferType.SHORT_DATA;
				return !is_closed;
			}
			if (isTransferTypeChangePossible()) {
				if (hasPriorityDataToSend())
				{
					dataTransferType = DataTransferType.SHORT_DATA;
					return true;
				}
				switch (dataTransferType) {
					case SHORT_DATA:
						if (!bigDataTransferPaused && bigDataToSend.size() > 0)
							dataTransferType = DataTransferType.BIG_DATA;
						else if (dataToTransfer.size() > 0)
							dataTransferType = DataTransferType.DATA_TO_TRANSFER;

						break;
					case BIG_DATA:
						if (dataToTransfer.size() > 0)
							dataTransferType = DataTransferType.DATA_TO_TRANSFER;
						else if (shortDataToSend.size() > 0 || bigDataTransferPaused)
							dataTransferType = DataTransferType.SHORT_DATA;


						break;
					case DATA_TO_TRANSFER:
						if (shortDataToSend.size() > 0)
							dataTransferType = DataTransferType.SHORT_DATA;
						else if (!bigDataTransferPaused && bigDataToSend.size() > 0)
							dataTransferType = DataTransferType.BIG_DATA;
						else if (bigDataTransferPaused)
							dataTransferType = DataTransferType.SHORT_DATA;

						break;
				}
				return true;
			}
			else {
				return false;
			}

		}

		private AbstractData<?> getNextData() throws TransferException {
			if (is_closed)
				return null;
			switch (dataTransferType) {
				case SHORT_DATA:
					if (shortDataToSend.size() > 0)
					{
						AbstractData<?> ad=shortDataToSend.getFirst();
						if (ad.isDataBuildInProgress())
							return null;
						return ad;
					}
					else
						return null;
				case BIG_DATA:
					if (bigDataToSend.size() > 0)
					{
						AbstractData<?> ad=bigDataToSend.get(bigDataToSendIndex);
						if (ad.isDataBuildInProgress())
							return null;
						return ad;
					}
					else
						throw new TransferException("Unexpected exception !");
				case DATA_TO_TRANSFER:
					if (dataToTransfer.size() > 0)
					{
						AbstractData<?> ad=dataToTransfer.getFirst();
						if (ad.isDataBuildInProgress())
							return null;
						return ad;
					}
					else
						throw new TransferException("Unexpected exception !");
			}
			return null;

		}

		@SuppressWarnings("ThrowFromFinallyBlock")
        private boolean free(AbstractData<?> d) throws TransferException {
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
								throw new TransferException("Unexpected exception !", e);
							} finally {
								if (shortDataToSend.size()>0 && shortDataToSend.getFirst()==d) {
									AbstractData<?> removed = shortDataToSend.removeFirst();
									if (removed != d)
										throw new InternalError();
								}
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
								throw new TransferException("Unexpected exception !", e);
							}
							if (bigDataToSend.size()>0 && bigDataToSend.get(bigDataToSendIndex)==d) {
								AbstractData<?> removed = bigDataToSend.remove(bigDataToSendIndex);
								if (removed != d)
									throw new InternalError();

								bigDataToSendIndex = bigDataToSend.size() == 0 ? 0 : bigDataToSendIndex % bigDataToSend.size();
							}
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
								throw new TransferException("Unexpected exception !", e);
							}
							if (dataToTransfer.size()>0 && dataToTransfer.getFirst()==d) {
								AbstractData<?> removed = dataToTransfer.removeFirst();
								if (removed != d)
									throw new InternalError();
							}

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
				throw new TransferException(e);
			}
		}

		private Timer timer_send = null;
		private final Timer timer_send_static = new Timer(true);
		private int data_sent = 0;

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
					data_read = finalizer.socketChannel.read(readSizeBlock);

					if (!readSizeBlock.hasRemaining())
					{
						int size=Block.getBlockSize(readSizeBlock.array(), 0);
						if (size<=Block.getBlockSizeLength() || size>maxBlockSize)
						{
							if (logger != null)
								logger.severe("Invalid block size "+size+" (max="+maxBlockSize+"). Impossible to receive new bytes (connection closed).");
							this.agentSocket.proceedEventualBan(true);
							closeConnection(ConnectionClosedReason.CONNECTION_ANOMALY);
							key.cancel();
							return;
						}
						readBuffer = ByteBuffer.allocate(size);
						readBuffer.put(readSizeBlock.array());
						readSizeBlock.clear();
						int s=finalizer.socketChannel.read(readBuffer);
						if (s<0)
							data_read=s;
						else
							data_read+=s;
					}
				}
				else
					data_read = finalizer.socketChannel.read(readBuffer);

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
				receivedData(key, readBuffer);
				readBuffer=null;
			}


		}

		private void receivedData(SelectionKey key, ByteBuffer data) {
			data.clear();
			if (firstReceivedData != null) {
				firstReceivedData.put(data.array(), 0, data.limit());
				if (!firstReceivedData.isValid(false)) {
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
								closeConnection(ConnectionClosedReason.CONNECTION_ANOMALY);
								key.cancel();
								return;
								//receivedData(key, bb, bb.position());
							}
						} else {
							if (logger != null && logger.isLoggable(Level.INFO))
								logger.info("Incompatible peers : "
										+"\n\tDefault project code name: "+ MadkitProperties.defaultProjectCodeName
										+"\n\tLocal MaDKit build number: "
										+DatagramLocalNetworkPresenceMessage.getVersionLong(getMadkitConfig().madkitVersion)
										+"\n\tLocal project build number: "+(getMadkitConfig().projectVersion==null?null:DatagramLocalNetworkPresenceMessage.getVersionLong(getMadkitConfig().projectVersion))
										+"\n\tMinimum MaDKit build number: "+DatagramLocalNetworkPresenceMessage.getVersionLong(getMadkitConfig().minimumMadkitVersion)
										+"\n\tMinimum project build number: "+(getMadkitConfig().minimumProjectVersion==null?null:DatagramLocalNetworkPresenceMessage.getVersionLong(getMadkitConfig().minimumProjectVersion))
										+"\n\tLocal kernel address: "+getKernelAddress());

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
					logger.finest("Receiving new initial bytes (" + data.limit() + " bytes) from "
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

				if (data != null) {
					boolean dataFinished;
					int remaining = -1;
					if ((dataFinished = data.isFinished()) || !data.isReady()) {
						if (dataFinished) {
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


						if (buf!=null) {
							if (firstPacketSent) {
								if (timer_send == null) {
									timer_send = timer_send_static;
									timer_send.reset();
								}
								else {
									agentSocket.getStatistics().newDataSent(data.getIDTransfer(), data_sent,
											timer_send.getDeltaMilli());
								}
							}

							data_sent = finalizer.socketChannel.write(buf);
						}
						if (firstPacketSent)
						{

							agentSocket.getStatistics().newDataSent(data.getIDTransfer(), data_sent);
						}

						remaining = buf==null?0:buf.remaining();
						if (freeNoBackData() && remaining > 0)
							throw new IllegalAccessError();

						if (data_sent > 0) {
							if (logger != null && logger.isLoggable(Level.FINEST))
								logger.finest("New data sent (" + data_sent + " bytes, bufferRemaining="
										+ remaining + ", totalBufferLength=" + (buf==null?"Canceled":buf.capacity()) + ")");
							last_data_wrote_nano = System.nanoTime();
						}
					}
					if (remaining > 0)
						return;
					data=getNextNoBackData();
					if (data!=null)
					    return;

				}
				if (!is_closed)
					key.interestOps( SelectionKey.OP_READ);

			} catch (IOException e) {
				if (logger != null && logger.isLoggable(Level.FINER))
					logger.finer("Unexpected exception during write process : " + e);

				closeConnection(ConnectionClosedReason.CONNECTION_LOST);
				key.cancel();
			}

		}


		public void closeConnection(ConnectionClosedReason cs)
		{
			closeConnection(cs, false);
		}
		public void closeConnection(ConnectionClosedReason cs, boolean delaying) {
			if (logger != null && logger.isLoggable(Level.FINER))
				logger.finer("Closing connection : " + this);
			this.cs=cs;
			is_closed = true;
			try {
				for (NoBackData ad : noBackDataToSend)
					ad.data.unlockMessage();
				for (AbstractData<?> ad : shortDataToSend)
					ad.unlockMessage();
				for (AbstractData<?> ad : bigDataToSend)
					ad.unlockMessage();
				for (AbstractData<?> ad : dataToTransfer)
					ad.unlockMessage();
			}
			catch (Exception e)
			{
				if (logger != null)
					logger.log(Level.SEVERE, "Unexpected exception", e);
			}
			try
			{
				if (this.finalizer.socketChannel.isOpen())
					this.finalizer.socketChannel.socket().getOutputStream().flush();
			}
			catch(Exception e)
			{
				e.printStackTrace();
			}
			delaying&=getMadkitConfig().networkProperties.delayInMsBeforeClosingConnectionNormally>0;
			if (delaying)
                NIOAgent.this.scheduleTask(new Task<Void>(getMadkitConfig().networkProperties.delayInMsBeforeClosingConnectionNormally + System.currentTimeMillis(), true) {
					@Override
					public Void call() {
						if (isAlive())
							receiveMessage(new ObjectMessage<>(PersonalSocket.this));
						else
							finishCloseConnection();
						return null;
					}
				});
			else
				finishCloseConnection();

		}

		public void finishCloseConnection()
		{

			if (agentAddress!=null)
				NIOAgent.this.finalizer.personal_sockets.remove(this.agentAddress.getAgentNetworkID());
			NIOAgent.this.finalizer.personal_sockets_list.remove(this);



			InetSocketAddress isa=null;
			InetSocketAddress isaLocal=null;
			try {

				isa = (InetSocketAddress) this.finalizer.socketChannel.getRemoteAddress();
				isaLocal = (InetSocketAddress) this.finalizer.socketChannel.getLocalAddress();
				removeConnectedIP(isa.getAddress());


				if (clientKey != null) {
					clientKey.cancel();
					clientKey.channel().close();
					if (finalizer.socketChannel.isConnected())
						finalizer.socketChannel.close();//TODO remove ?
				} else
					finalizer.socketChannel.close();
			} catch (Exception e) {
				if (logger != null && logger.isLoggable(Level.FINE))
					logger.log(Level.FINE, "Unexpected exception", e);
			}
			try {

				if (isa!=null && isaLocal!=null) {
					boolean found = false;

					servers:for (Server s : NIOAgent.this.finalizer.serverChannels) {
						if (!s.address.equals(isaLocal))
							continue;

						for (PersonalSocket sc : NIOAgent.this.finalizer.personal_sockets_list) {
							try {
								if (sc.finalizer.socketChannel.getLocalAddress().equals(s.address)) {
									InetSocketAddress isa2 = (InetSocketAddress) sc.finalizer.socketChannel.getRemoteAddress();

									if (isa.equals(isa2)) {
										found = true;
										break servers;
									}
								}
							} catch (Exception e) {
								if (logger != null && logger.isLoggable(Level.FINE))
									logger.log(Level.FINE, "Unexpected exception", e);
							}
						}

					}
					if (!found ) {
						for (AgentAddress aa : getAgentsWithRole(LocalCommunity.Groups.LOCAL_NETWORKS,
								LocalCommunity.Roles.LOCAL_NETWORK_ROLE)) {

							sendMessageWithRole(aa,
								new ConnectionStatusMessage(ConnectionStatusMessage.Type.DISCONNECT,
										new DoubleIP(isa), isa, isaLocal, cs),
								LocalCommunity.Roles.NIO_ROLE);
						}
					}

				}


			} catch (Exception e) {
				if (logger != null)
					logger.log(Level.SEVERE, "Unexpected exception", e);
			}

			if (agentAddress!=null && ReturnCode.isSuccessOrIsTransferInProgress(NIOAgent.this.sendMessageWithRole(agentAddress, new ConnectionClosed(this.agentAddress.getAgentNetworkID(),
					cs, new CircularArrayList<>(shortDataToSend), bigDataToSend, new CircularArrayList<>(dataToTransfer)), LocalCommunity.Roles.NIO_ROLE)))
			{
				try {
					for (NoBackData ad : noBackDataToSend)
						ad.data.unlockMessage();
					for (AbstractData<?> ad : shortDataToSend)
						ad.unlockMessage();
					for (AbstractData<?> ad : bigDataToSend)
						ad.unlockMessage();
					for (AbstractData<?> ad : dataToTransfer)
						ad.unlockMessage();
				}
				catch (Exception e)
				{
					if (logger != null)
						logger.log(Level.SEVERE, "Unexpected exception", e);
				}
			}
			else
			{
				try {
					for (NoBackData ad : noBackDataToSend)
						ad.data.unlockMessage();
					for (AbstractData<?> ad : shortDataToSend)
						ad.unlockMessage();
					for (AbstractData<?> ad : bigDataToSend)
						ad.cancel();
					for (AbstractData<?> ad : dataToTransfer)
						ad.unlockMessage();
				}
				catch (Exception e)
				{
					if (logger != null)
						logger.log(Level.SEVERE, "Unexpected exception", e);
				}
			}
			bigDataToSend=new CircularArrayList<>(5);

			try {
				if (stopping && isAlive() && NIOAgent.this.finalizer.personal_sockets.isEmpty())
					killAgent(NIOAgent.this);
			}
			finally {
				LockerCondition lc=actualLocker;
				if (lc!=null)
					lc.notifyLocker();
			}
		}
		public void closeIndirectConnection(ConnectionClosedReason cs, IDTransfer transferID,
				AgentAddress indirectAgentAddress) {

			this.dataToTransfer.removeIf(ad -> ad.getIDTransfer().equals(transferID));
			if (agentAddress!=null) {
				NIOAgent.this.sendMessageWithRole(
						indirectAgentAddress, new ConnectionClosed(this.agentAddress.getAgentNetworkID(), cs,
								new ArrayList<>(0), new ArrayList<>(0), dataToTransfer),
						LocalCommunity.Roles.NIO_ROLE);
			}
		}

		public boolean isConcernedBy(Server s) throws IOException {

			return this.finalizer.socketChannel.getLocalAddress().equals(s.address);
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
		private final SelectionKey selectionKey;

		PersonalDatagramChannel(DatagramChannel datagramSocket, int maxDataSize, AgentAddress multicastAgent,
				InetAddress localAddress, InetAddress groupIP, int port) throws ClosedChannelException {
			this.datagramChannel = datagramSocket;
			selectionKey=this.datagramChannel.register(NIOAgent.this.finalizer.selector, SelectionKey.OP_READ);
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



		void addDataToSend(DatagramData data) {
			messagesToSend.add(data);

			if ((selectionKey.interestOps() & SelectionKey.OP_WRITE) != SelectionKey.OP_WRITE)
				selectionKey.interestOps(SelectionKey.OP_WRITE | SelectionKey.OP_READ);
		}

		void read(SelectionKey key) {
			try {

				if (currentDatagramData == null)
					currentDatagramData = new DatagramData();

				datagramChannel.receive(currentDatagramData.getByteBuffer());

				if (!currentDatagramData.isValid(true))
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


		void closeConnection(boolean sentFromMulticastAgent) {
			if (logger != null && logger.isLoggable(Level.FINER))
				logger.finer("Closing datagram channel : " + this);

			try {
				finalizer.personal_datagram_channels.remove(datagramChannel);
				finalizer.personal_datagram_channels_per_agent_address.remove(multicastAgent);
				finalizer.personal_datagram_channels_per_ni_address.remove(localAddress);
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
			InetAddress local_interface, AskForConnectionMessage callerMessage) {
		if (logger != null && logger.isLoggable(Level.FINER))
			logger.finer("Pending connection (inetSocketAddress=" + inetSocketAddress + ", local_interface="
					+ local_interface + ")");
		finalizer.pending_connections
				.add(new PendingConnection(ip, socketChannel, inetSocketAddress, local_interface, callerMessage));
	}

	private void pendingConnectionSucceeded(SocketChannel socketChannel) {

		for (Iterator<PendingConnection> it = finalizer.pending_connections.iterator(); it.hasNext();) {
			PendingConnection pc = it.next();
			if (pc.isConcernedBy(socketChannel)) {
				if (pc.getCallerMessage() != null && pc.getCallerMessage().getJoinedPiece() != null
						&& pc.getCallerMessage().getOriginalSender() != null
						&& pc.getCallerMessage().getJoinedPiece().getClass() == TryDirectConnection.class) {
					sendMessageWithRole(pc.getCallerMessage().getOriginalSender(),
							new ObjectMessage<>(new TransferAgent.DirectConnectionSucceeded(
									((TryDirectConnection) pc.getCallerMessage().getJoinedPiece()).getIDTransfer())),
							LocalCommunity.Roles.NIO_ROLE);
					pc.getCallerMessage().setJoinedPiece(null, null);
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

		for (Iterator<PendingConnection> it = finalizer.pending_connections.iterator(); it.hasNext();) {
			PendingConnection pc = it.next();
			if (pc.isConcernedBy(socketChannel)) {
				if (pc.getCallerMessage() != null && pc.getCallerMessage().getJoinedPiece() != null
						&& pc.getCallerMessage().getOriginalSender() != null
						&& pc.getCallerMessage().getJoinedPiece() == TryDirectConnection.class) {
					sendMessageWithRole(pc.getCallerMessage().getOriginalSender(),
							new ObjectMessage<>(new TransferAgent.DirectConnectionFailed(
									((TryDirectConnection) pc.getCallerMessage().getJoinedPiece()).getIDTransfer())),
							LocalCommunity.Roles.NIO_ROLE);
				}
				else {
					if (pc.getCallerMessage()==null)
						throw new NullPointerException();

					if (pc.getCallerMessage().getOriginalSender()!=null)
					{
						if (reason==null)
							reason=ConnectionClosedReason.IP_NOT_REACHED;
						sendMessageWithRole(pc.getCallerMessage().getOriginalSender(),
								new ConnectionStatusMessage(ConnectionStatusMessage.Type.DISCONNECT, pc.getIP(), pc.getCallerMessage().getChosenIP(), pc.getCallerMessage().interface_address, reason, pc.getCallerMessage().getNumberOfAnomalies(), pc.getCallerMessage().getTimeUTCOfAnomaliesCycle()),
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
		private final AskForConnectionMessage callerMessage;
		private final SocketChannel socketChannel;
		private final long nanoTime;
		private final AbstractIP ip;

		PendingConnection(AbstractIP ip, SocketChannel socketChannel, InetSocketAddress inetSocketAddress,
				InetAddress local_interface, AskForConnectionMessage callerMessage) {
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
			this.callerMessage = callerMessage;
			this.ip = ip;
			nanoTime = System.nanoTime();
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

		AskForConnectionMessage getCallerMessage() {
			return callerMessage;
		}

		long getNanoTime() {
			return nanoTime;
		}

		SocketChannel getSocketChannel() {
			return socketChannel;
		}

	}

}
