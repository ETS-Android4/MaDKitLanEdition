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

package com.distrimind.madkit.kernel;

import com.distrimind.madkit.exceptions.SelfKillException;
import com.distrimind.madkit.message.MessageWithSilentInference;
import com.distrimind.util.concurrent.LockerCondition;

import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * This type of agent is between an AbstractAgent type and an Agent type. It is
 * designed for large scale systems. Large scale systems can't use a huge number
 * of Agent type, since each of them has a its own thread and since the
 * operating system can manage only a few limited number of threads (several
 * thousands). The solution could be to schedule AbstractAgent types, non
 * threaded, through a {@link Scheduler} agent. But here, the system works in
 * full CPU mode even if it has nothing to do. The solution produced here
 * consists of automatically scheduling AgentFakeThread agents types through
 * several threads with a limited number (the default maximum number of threads
 * is <code>Runtime.getRuntime().availableProcessors()</code>.
 * 
 * To use the AgentFakeThread class, the user must inherit it, and overwrite the
 * method {@link #liveByStep(Message)}. This method is called for every received
 * message. If no message is received, the system does not consume CPU time. No
 * {@link #wait()} method must be called into this function. The AgentFakeThread
 * agents must be launched through the function
 * {@link AbstractAgent#launchAgent(AbstractAgent)} or equivalent. The threads
 * managing these agents are created progressively when AgentFakeThread are
 * launched. So, if no agent is launched, the system does not consume any
 * memory. If one agent is launched, only one thread is created, etc, until the
 * maximum number of threads is reached. The threads are destroyed also
 * progressively.
 * 
 * To change the priority of these threads, call the function
 * {@link #setThreadPriorityOfMaDKItServiceExecutor(int)} (int)}.
 * 
 * 
 * @author Jason Mahdjoub
 * @version 1.0
 * @since MadKitLanEdition 1.0
 */
public abstract class AgentFakeThread extends AbstractAgent {

	// private AgentAddress task_agent_addesss=null;
	/*
	 * private static class ExecutorProperties {
	 * 
	 * final int maximumPoolSize; final int defaultThreadPriotity; final long
	 * timeOutSeconds; ExecutorProperties(int maximumPoolSize, int priority, long
	 * timeOutSeconds) { this.maximumPoolSize=maximumPoolSize;
	 * this.defaultThreadPriotity=priority; this.timeOutSeconds=timeOutSeconds;
	 * 
	 * } }
	 */
	/*
	 * private final String agent_task_name; private ExecutorProperties
	 * executorProperties;
	 */
	final AtomicBoolean messageReadAlreadyInProgress = new AtomicBoolean(false);

	/**
	 * Construct an AgentFakeThread with the default task manager agent. All agents
	 * constructed like this will be managed with the same task manager agent.
	 */
	public AgentFakeThread() {
	}


	/**
	 * {@inheritDoc}
	 */
	@Override
	public void sleep(long millis) throws InterruptedException {
		if (getState().compareTo(State.LIVING) >= 0)
			getMadkitKernel().sleep(this, millis);
		else
			super.sleep(millis);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void wait(LockerCondition lockerCondition) throws InterruptedException {
		getMadkitKernel().wait(this, lockerCondition);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void wait(LockerCondition lockerCondition, long timeOutMillis) throws InterruptedException {
		getMadkitKernel().wait(this, lockerCondition);
	}

	/**
	 * This method is called for every received message. When the agent has no task
	 * to do, the function must end. No {@link #wait()} method must be called into
	 * this function.
	 * 
	 * @param _message
	 *            the received message
	 * @throws InterruptedException if the current thread is interrupted
	 * @since MadKitLanExtension 1.0
	 */
	protected abstract void liveByStep(final Message _message) throws InterruptedException;

	/**
	 * This method offers a convenient way for regular object to send messages to
	 * Agents, especially threaded agents. For instance when a GUI wants to discuss
	 * with its linked agent: This allows to enqueue work to do in their life cycle
	 * 
	 * @param m
	 *            the received message
	 */
	@Override
	public Message receiveMessage(Message m) {
		State s = state.get();
		if (!s.include(State.WAIT_FOR_KILL) && s != State.ENDING && s != State.TERMINATED && s != State.ZOMBIE) {
			getMessageBox().getLocker().lock();
			try {
				m = super.receiveMessageUnsafe(m);

				if (m == null) {
					return null;
				}

				if (state.get().include(State.LIVING)) {
					manageTaskMessage(false);
				}
				return m;
			} finally {
				messageBox.getLocker().unlock();
			}

		}
		return null;
	}

	boolean canContinueToManageMessages() {
		State s = getState();
		return s == State.LIVING || isLivingButWaitingForMessages();
	}

	void manageTaskMessage(boolean force) {

		boolean schedule = false;
		if (messageBox.size() == 1 || (force && messageBox.size() > 0)) {
			if (messageReadAlreadyInProgress.compareAndSet(false, true)) {
				schedule = true;
			}
		}
		if (schedule) {
			getMadkitKernel().getMaDKitServiceExecutor().submit((Callable<Void>) () -> {
				if (canContinueToManageMessages()) {
					messageBox.getLocker().lock();
					Message m;
					try
					{
						m = nextMessage();
					} finally {
						messageBox.getLocker().unlock();
					}
					if (m != null) {
						try {
							AgentFakeThread.this.setMyThread(Thread.currentThread());
							liveByStep(m);

						} catch (final SelfKillException e) {
							if (e.killing_type
									.equals(KillingType.WAIT_AGENT_PURGE_ITS_MESSAGES_BOX_BEFORE_KILLING_IT)) {

								getMadkitKernel().getMaDKitServiceExecutor().execute(() -> getKernel().killAgent(AgentFakeThread.this, AgentFakeThread.this,
										e.timeOutSeconds, e.killing_type));
							} else
								getKernel().killAgent(AgentFakeThread.this, AgentFakeThread.this, e.timeOutSeconds,e.killing_type);

						} catch (InterruptedException ignored) {

						} catch (Throwable e) {
							logLifeException(e);
						} finally {
							messageBox.getLocker().lock();
							try {
								messageReadAlreadyInProgress.set(false);
								if (canContinueToManageMessages()) {
									manageTaskMessage(true);
								}
							} finally {
								messageBox.getLocker().unlock();
							}

						}
					} else {
						messageBox.getLocker().lock();
						try {
							messageReadAlreadyInProgress.set(false);
						} finally {
							messageBox.getLocker().unlock();
						}
					}
					if (state.get().equals(State.LIVING_BUT_WAIT_FOR_KILL)) {
						synchronized (state) {
							state.notify();
						}
					}

				} else
					System.err.println(AgentFakeThread.this
							+ "------------cannot manage task message !!!!!!!!!!!!!! : " + getState());
				return null;
			});
		}
	}

	/**
	 * Send a message to itself in order to execute runnable only when receiving the message, being synchronized
	 * with the life cycle of the agent, and avoiding to use synchronized data
	 * @param action the action to run
	 */
	public void synchronizeActionWithLiveByCycleFunction(Runnable action)
	{
		receiveMessage(new RunnableMessage(action));
	}


}
