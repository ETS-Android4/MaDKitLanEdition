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
package com.distrimind.madkit.kernel.network.connection.secured;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;

import gnu.vm.jgnu.security.InvalidAlgorithmParameterException;
import gnu.vm.jgnu.security.InvalidKeyException;
import gnu.vm.jgnu.security.NoSuchAlgorithmException;
import gnu.vm.jgnu.security.NoSuchProviderException;
import gnu.vm.jgnu.security.SignatureException;
import gnu.vm.jgnu.security.spec.InvalidKeySpecException;
import gnu.vm.jgnu.security.spec.InvalidParameterSpecException;
import gnu.vm.jgnux.crypto.ShortBufferException;

import com.distrimind.madkit.exceptions.BlockParserException;
import com.distrimind.madkit.exceptions.ConnectionException;
import com.distrimind.madkit.kernel.MadkitProperties;
import com.distrimind.madkit.kernel.network.Block;
import com.distrimind.madkit.kernel.network.NetworkProperties;
import com.distrimind.madkit.kernel.network.SubBlock;
import com.distrimind.madkit.kernel.network.SubBlockInfo;
import com.distrimind.madkit.kernel.network.SubBlockParser;
import com.distrimind.madkit.kernel.network.connection.ConnectionFinished;
import com.distrimind.madkit.kernel.network.connection.ConnectionMessage;
import com.distrimind.madkit.kernel.network.connection.ConnectionProtocol;
import com.distrimind.madkit.kernel.network.connection.TransferedBlockChecker;
import com.distrimind.madkit.kernel.network.connection.UnexpectedMessage;
import com.distrimind.ood.database.DatabaseWrapper;
import com.distrimind.util.Bits;
import com.distrimind.util.crypto.ASymmetricKeyPair;
import com.distrimind.util.crypto.ASymmetricKeyWrapperType;
import com.distrimind.util.crypto.AbstractSecureRandom;
import com.distrimind.util.crypto.SymmetricAuthentifiedSignatureCheckerAlgorithm;
import com.distrimind.util.crypto.SymmetricAuthentifiedSignatureType;
import com.distrimind.util.crypto.SymmetricAuthentifiedSignerAlgorithm;
import com.distrimind.util.crypto.SymmetricEncryptionAlgorithm;
import com.distrimind.util.crypto.SymmetricEncryptionType;
import com.distrimind.util.crypto.SymmetricSecretKey;
import com.distrimind.util.sizeof.ObjectSizer;

/**
 * Represents a connection protocol used between a client and a server. This
 * class must be used by the server. There is no certificate, so the public key
 * must be known in advance with this protocol.
 * 
 * @author Jason Mahdjoub
 * @version 1.1
 * @since MadkitLanEdition 1.0
 * @see ClientSecuredConnectionProtocolWithKnownPublicKey
 */
public class ServerSecuredConnectionProtocolWithKnwonPublicKey
		extends ConnectionProtocol<ServerSecuredConnectionProtocolWithKnwonPublicKey> {
	Step current_step = Step.NOT_CONNECTED;

	ASymmetricKeyPair myKeyPairForEncryption;

	protected SymmetricEncryptionType symmetricEncryptionType=null;
	protected SymmetricEncryptionAlgorithm symmetricAlgorithm = null;
	protected SymmetricAuthentifiedSignerAlgorithm signer = null;
	protected SymmetricAuthentifiedSignatureCheckerAlgorithm signatureChecker=null;
	protected SymmetricSecretKey mySecretKeyForEncryption=null,mySecretKeyForSignature=null;
	protected SymmetricAuthentifiedSignatureType signatureType;
	protected ASymmetricKeyWrapperType keyWrapper=null;
	
	protected int signature_size;
	protected short secretKeySizeBits;
	private final SubBlockParser parser;

	protected final ServerSecuredProcotolPropertiesWithKnownPublicKey hproperties;
	private final AbstractSecureRandom approvedRandom;
	final int maximumSignatureSize;
	boolean firstMessageReceived = false;
	private boolean needToRefreshTransferBlockChecker = true;
	//private boolean currentBlockCheckerIsNull = true;

	private ServerSecuredConnectionProtocolWithKnwonPublicKey(InetSocketAddress _distant_inet_address,
			InetSocketAddress _local_interface_address, ConnectionProtocol<?> _subProtocol,
			DatabaseWrapper sql_connection, MadkitProperties mkProperties, NetworkProperties _properties, int subProtocolLevel, boolean isServer,
			boolean mustSupportBidirectionnalConnectionInitiative) throws ConnectionException {
		super(_distant_inet_address, _local_interface_address, _subProtocol, sql_connection, _properties,
				subProtocolLevel, isServer, mustSupportBidirectionnalConnectionInitiative);
		hproperties = (ServerSecuredProcotolPropertiesWithKnownPublicKey) super.connection_protocol_properties;

		hproperties.checkProperties();

		myKeyPairForEncryption = null;

		signature_size = -1;
		this.symmetricAlgorithm = null;
		this.keyWrapper=null;
		signatureType = null;
		symmetricEncryptionType=null;
		secretKeySizeBits=-1;
		try {
			approvedRandom=mkProperties.getApprovedSecureRandom();

			maximumSignatureSize = hproperties.getMaximumSignatureSizeBits();
		} catch (NoSuchAlgorithmException | NoSuchProviderException e) {
			throw new ConnectionException(e);
		}
		if (hproperties.enableEncryption)
			parser = new ParserWithEncryption();
		else
			parser = new ParserWithNoEncryption();

	}

	void initMyKeyPair(int identifier) throws BlockParserException {
		if (myKeyPairForEncryption != null)
			return;
		try {
			myKeyPairForEncryption = hproperties.getKeyPairForEncryption(identifier);
			if (myKeyPairForEncryption == null)
				throw new BlockParserException(
						"Unkonw encryption profile. Impossible to find key pair identified by " + identifier);
			
			signatureType = hproperties.getSignatureType(identifier);
			if (signatureType == null)
				throw new BlockParserException(
						"Unkonw encryption profile. Impossible to find signature identified by " + identifier);
			
			keyWrapper=hproperties.getKeyWrapper(identifier);

			if (keyWrapper == null)
				throw new BlockParserException(
						"Unkonw encryption profile. Impossible to find keyWrapper identified by " + identifier);
			
			symmetricEncryptionType = hproperties.getSymmetricEncryptionType(identifier);
			if (symmetricEncryptionType == null)
				throw new BlockParserException(
						"Unkonw encryption profile. Impossible to find symmetric encryption type identified by "
								+ identifier);
			secretKeySizeBits=hproperties.getSymmetricEncryptionKeySizeBits(identifier);
			
			signature_size = signatureType.getSignatureSizeInBits()/8;

		} catch (Exception e) {
			if (e instanceof BlockParserException)
				throw (BlockParserException) e;
			else
				throw new BlockParserException(e);
		}
	}
	
	boolean isProfileInitialized()
	{
		return firstMessageReceived;
	}

	private void setSecretKeys(AskClientServerConnection askMessage) throws ConnectionException {
		try {
			if (askMessage.getSecretKeyForEncryption()==null && hproperties.enableEncryption)
				throw new ConnectionException("Secret key empty !");
			
			if (hproperties.enableEncryption)
			{
				mySecretKeyForEncryption=keyWrapper.unwrapKey(myKeyPairForEncryption.getASymmetricPrivateKey(), askMessage.getSecretKeyForEncryption());
				symmetricAlgorithm=new SymmetricEncryptionAlgorithm(approvedRandom, mySecretKeyForEncryption);
			}
			else
				mySecretKeyForEncryption=null;
			
			mySecretKeyForSignature=keyWrapper.unwrapKey(myKeyPairForEncryption.getASymmetricPrivateKey(), askMessage.getSecretKeyForSignature());
			
			signer = new SymmetricAuthentifiedSignerAlgorithm(mySecretKeyForSignature);
			signatureChecker = new SymmetricAuthentifiedSignatureCheckerAlgorithm(mySecretKeyForSignature);
			// this.secret_key=symmetricAlgorithm.getSecretKey();
		} catch (Exception e) {
			resetKeys();
			throw new ConnectionException(e);
		}
	}

	private void resetKeys() {
		mySecretKeyForEncryption = null;
		mySecretKeyForSignature=null;
		signer = null;
		signatureChecker=null;
		symmetricAlgorithm = null;
	}

	private enum Step {
		NOT_CONNECTED, WAITING_FOR_CONNECTION_CONFIRMATION, CONNECTED,
	}

	@Override
	protected ConnectionMessage getNextStep(ConnectionMessage _m) {
		switch (current_step) {
		case NOT_CONNECTED: {
			
			if (_m instanceof AskClientServerConnection) {
				AskClientServerConnection ask = (AskClientServerConnection) _m;

				try {
					setSecretKeys(ask);
				} catch (ConnectionException e) {
					return new ConnectionFinished(this.getDistantInetSocketAddress(),
							ConnectionClosedReason.CONNECTION_ANOMALY);
				}
				current_step = Step.WAITING_FOR_CONNECTION_CONFIRMATION;
				return new ConnectionFinished(getDistantInetSocketAddress());
			} else if (_m instanceof ConnectionFinished) {
				if (((ConnectionFinished) _m).getState()
						.equals(ConnectionProtocol.ConnectionState.CONNECTION_ESTABLISHED)) {
					return new UnexpectedMessage(this.getDistantInetSocketAddress());
				} else
					return new ConnectionFinished(this.getDistantInetSocketAddress(),
							ConnectionClosedReason.CONNECTION_ANOMALY);
			} else {
				return new UnexpectedMessage(this.getDistantInetSocketAddress());
			}
		}
		case WAITING_FOR_CONNECTION_CONFIRMATION: {
			if (_m instanceof ConnectionFinished && ((ConnectionFinished) _m).getState()
					.equals(ConnectionProtocol.ConnectionState.CONNECTION_ESTABLISHED)) {
				current_step = Step.CONNECTED;
				return null;
			} else if (_m instanceof ConnectionFinished) {
				return new UnexpectedMessage(this.getDistantInetSocketAddress());
			} else {
				return new UnexpectedMessage(this.getDistantInetSocketAddress());
			}

		}
		case CONNECTED: {
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
				return new UnexpectedMessage(this.getDistantInetSocketAddress());
			}
		}
		}
		return null;
	}

	@Override
	public boolean isCrypted() {
		return hproperties.enableEncryption;
	}

	@Override
	protected void closeConnection(ConnectionClosedReason _reason) {
		if (_reason.equals(ConnectionClosedReason.CONNECTION_ANOMALY)) {
			resetKeys();
		}
		current_step = Step.NOT_CONNECTED;
	}

	private class ParserWithEncryption extends SubBlockParser {

		ParserWithEncryption() {

		}

		@Override
		public int getBodyOutputSizeForEncryption(int size) throws BlockParserException {
			
			try {
				if (current_step==Step.NOT_CONNECTED)
					return size;
				else
					return symmetricAlgorithm.getOutputSizeForEncryption(size)+1;

			} catch (Exception e) {
				throw new BlockParserException(e);
			}
		}

		@Override
		public int getBodyOutputSizeForDecryption(int size) throws BlockParserException {
			try {
				switch (current_step) {
				case NOT_CONNECTED:
					return size;
				case WAITING_FOR_CONNECTION_CONFIRMATION:
				case CONNECTED:
					return symmetricAlgorithm.getOutputSizeForDecryption(size-1);
				}
			} catch (Exception e) {
				throw new BlockParserException(e);
			}
			throw new IllegalAccessError();
		}

		@Override
		public SubBlockInfo getSubBlock(SubBlock _block) throws BlockParserException {
			switch (current_step) {
			case NOT_CONNECTED: {
				
				
				if (!isProfileInitialized())
				{
					int identifier = Bits.getInt(_block.getBytes(), _block.getOffset());
					initMyKeyPair(identifier);
				}
				SubBlock res = new SubBlock(_block.getBytes(), _block.getOffset() + getSizeHead(),
						getBodyOutputSizeForDecryption(_block.getSize() - getSizeHead()));
				setFirstMessageReceived();
				
				return new SubBlockInfo(res, true, false);
			}
			case WAITING_FOR_CONNECTION_CONFIRMATION:
			case CONNECTED: {
				int off=_block.getOffset() + getSizeHead();
				boolean excludedFromEncryption=_block.getBytes()[off]==1;
				if (excludedFromEncryption)
				{
					int s=Block.getBlockSize(_block.getBytes(), off+1);
					if (s>Block.BLOCK_SIZE_LIMIT)
						throw new BlockParserException();
					
					try  {
						
						SubBlock res = new SubBlock(new byte[_block.getBytes().length], _block.getOffset() + getSizeHead(),
								s);
	
						boolean check = signatureChecker.verify(_block.getBytes(), res.getOffset(),
								_block.getSize() - getSizeHead(), _block.getBytes(), _block.getOffset(), signature_size);
						System.arraycopy(_block.getBytes(), off+4, res.getBytes(), res.getOffset(), s);
						return new SubBlockInfo(res, check, !check);
					} catch (Exception e) {
						SubBlock res = new SubBlock(_block.getBytes(), _block.getOffset() + getSizeHead(),
								getBodyOutputSizeForDecryption(_block.getSize() - getSizeHead()));
						return new SubBlockInfo(res, false, true);
					}
				}
				else
				{
					try (ByteArrayInputStream bais = new ByteArrayInputStream(_block.getBytes(),
							_block.getOffset() + getSizeHead()+1, _block.getSize() - getSizeHead()-1)) {
						byte[] tmp = symmetricAlgorithm.decode(bais);
	
						if (tmp.length > getBodyOutputSizeForDecryption(_block.getSize() - getSizeHead()))
							throw new BlockParserException("Invalid block size for decoding.");
						SubBlock res = new SubBlock(new byte[_block.getBytes().length], _block.getOffset() + getSizeHead(),
								tmp.length);
	
						boolean check = signatureChecker.verify(_block.getBytes(), res.getOffset(),
								_block.getSize() - getSizeHead(), _block.getBytes(), _block.getOffset(), signature_size);
						System.arraycopy(tmp, 0, res.getBytes(), res.getOffset(), tmp.length);
						return new SubBlockInfo(res, check, !check);
					} catch (Exception e) {
						SubBlock res = new SubBlock(_block.getBytes(), _block.getOffset() + getSizeHead(),
								getBodyOutputSizeForDecryption(_block.getSize() - getSizeHead()));
						return new SubBlockInfo(res, false, true);
					}
				}
			}
			}
			throw new BlockParserException("Unexpected exception");
		}

		@Override
		public SubBlock getParentBlock(SubBlock _block, boolean excludeFromEncryption) throws BlockParserException {
			try {
				int outputSize = getBodyOutputSizeForEncryption(_block.getSize());
				switch (current_step) {
				case NOT_CONNECTED:
				{
					SubBlock res = new SubBlock(_block.getBytes().clone(), _block.getOffset() - getSizeHead(),
							outputSize + getSizeHead());
					return res;
				}
				case WAITING_FOR_CONNECTION_CONFIRMATION:
				case CONNECTED: {
					SubBlock res = new SubBlock(new byte[_block.getBytes().length], _block.getOffset() - getSizeHead(),
							outputSize + getSizeHead());
					if (excludeFromEncryption)
					{
						res.getBytes()[_block.getOffset()]=1;
						Block.putShortInt(res.getBytes(), _block.getOffset()+1, _block.getSize());
						System.arraycopy(_block.getBytes(), _block.getOffset(), res.getBytes(), _block.getOffset()+4, _block.getSize());
						signer.sign(res.getBytes(), _block.getOffset(), outputSize, res.getBytes(), res.getOffset(), signature_size);
					}
					else
					{
						byte []tmp=null;
						try(ByteArrayOutputStream baos=new ByteArrayOutputStream(outputSize))
						{
							baos.write(0);
							symmetricAlgorithm.encode(_block.getBytes(), _block.getOffset(), _block.getSize(), baos);
							baos.flush();
							tmp=baos.toByteArray();
						}
	
						if (outputSize != tmp.length)
							throw new BlockParserException("Invalid block size for encoding.");
						System.arraycopy(tmp, 0, res.getBytes(), _block.getOffset(), tmp.length);
						signer.sign(tmp, 0, tmp.length, res.getBytes(), res.getOffset(),
								signature_size);
					}
					return res;
				}
				}
			} catch (Exception e) {
				throw new BlockParserException(e);
			}
			throw new IllegalAccessError();
		}

		@Override
		public int getSizeHead() {
			//return signature_size;
			if (isProfileInitialized())
				return signature_size;
			else
				return ObjectSizer.sizeOf(hproperties.getLastEncryptionProfileIdentifier());
		}

		@Override
		public int getMaximumSizeHead() {
			return maximumSignatureSize;
		}

		@Override
		public SubBlockInfo checkEntrantPointToPointTransferedBlock(SubBlock _block) throws BlockParserException {
			SubBlock res = new SubBlock(_block.getBytes(), _block.getOffset() + getSizeHead(),
					_block.getSize() - getSizeHead());
			switch (current_step) {
			
			case NOT_CONNECTED: {
				
				return new SubBlockInfo(res, true, false);
			}
			case WAITING_FOR_CONNECTION_CONFIRMATION:
			case CONNECTED: {
				try {

					boolean check = signatureChecker.verify(_block.getBytes(), res.getOffset(),
							res.getSize(), _block.getBytes(), _block.getOffset(), signature_size);
					return new SubBlockInfo(res, check, !check);
				} catch (Exception e) {
					return new SubBlockInfo(res, false, true);
				}
			}
			}
			throw new BlockParserException("Unexpected exception");
		}

		@Override
		public SubBlock signIfPossibleSortantPointToPointTransferedBlock(SubBlock _block) throws BlockParserException {
			try {
				SubBlock res = new SubBlock(_block.getBytes().clone(), _block.getOffset() - getSizeHead(),
						_block.getSize() + getSizeHead());

				switch (current_step) {
				case NOT_CONNECTED:
				{
					return res;
				}
				case WAITING_FOR_CONNECTION_CONFIRMATION:
				case CONNECTED: {
					signer.sign(_block.getBytes(), _block.getOffset(), _block.getSize(), res.getBytes(), res.getOffset(),
							getSizeHead());
					return res;
				}
				}
			} catch (Exception e) {
				throw new BlockParserException(e);
			}
			throw new IllegalAccessError();
		}

	}

	private class ParserWithNoEncryption extends SubBlockParser {
		ParserWithNoEncryption() {

		}

		@Override
		public int getBodyOutputSizeForEncryption(int size) {
			return size;
		}

		@Override
		public SubBlockInfo getSubBlock(SubBlock _block) throws BlockParserException {

			try
			{
			switch (current_step) {
			case NOT_CONNECTED: {
				if (!isProfileInitialized())
				{
					int identifier = Bits.getInt(_block.getBytes(), _block.getOffset());
					initMyKeyPair(identifier);
				}
				SubBlock res = new SubBlock(_block.getBytes(), _block.getOffset() + getSizeHead(),
						getBodyOutputSizeForDecryption(_block.getSize() - getSizeHead()));
				setFirstMessageReceived();				
				return new SubBlockInfo(res, true, false);
			}
			case WAITING_FOR_CONNECTION_CONFIRMATION:
			case CONNECTED: {
				SubBlock res = new SubBlock(_block.getBytes(), _block.getOffset() + getSizeHead(),
						getBodyOutputSizeForDecryption(_block.getSize() - getSizeHead()));
				try {
					boolean check = signatureChecker.verify(res.getBytes(), res.getOffset(),
							_block.getSize() - getSizeHead(), _block.getBytes(), _block.getOffset(), signature_size);
					return new SubBlockInfo(res, check, !check);
				} catch (SignatureException | InvalidKeyException | NoSuchAlgorithmException
						| InvalidKeySpecException | ShortBufferException | IllegalStateException | NoSuchProviderException | InvalidAlgorithmParameterException | InvalidParameterSpecException | IOException e) {
					return new SubBlockInfo(res, false, true);
				}
			}
			}
			}
			catch(Exception e)
			{
				throw new BlockParserException(e);
			}
			throw new BlockParserException("Unexpected exception");
		}

		@Override
		public SubBlock getParentBlock(SubBlock _block, boolean excludeFromEncryption) throws BlockParserException {
			try {
				SubBlock res = new SubBlock(_block.getBytes().clone(), _block.getOffset() - getSizeHead(),
						getBodyOutputSizeForEncryption(_block.getSize()) + getSizeHead());
				if ((current_step!=Step.NOT_CONNECTED))
					signer.sign(_block.getBytes(), _block.getOffset(),
						getBodyOutputSizeForEncryption(_block.getSize()), res.getBytes(), res.getOffset(),
						signature_size);
				return res;
			} catch (Exception e) {
				throw new BlockParserException(e);
			}

		}

		@Override
		public int getSizeHead() {
			//return signature_size;
			if (isProfileInitialized())
				return signature_size;
			else
				return ObjectSizer.sizeOf(hproperties.getLastEncryptionProfileIdentifier());
		}

		@Override
		public int getBodyOutputSizeForDecryption(int _size) {
			return _size;
		}

		@Override
		public int getMaximumSizeHead() {
			return maximumSignatureSize;
		}
		
		@Override
		public SubBlockInfo checkEntrantPointToPointTransferedBlock(SubBlock _block) throws BlockParserException {
			SubBlock res = new SubBlock(_block.getBytes(), _block.getOffset() + getSizeHead(),
					_block.getSize() - getSizeHead());
			switch (current_step) {
			
			case NOT_CONNECTED: {
				
				return new SubBlockInfo(res, true, false);
			}
			case WAITING_FOR_CONNECTION_CONFIRMATION:
			case CONNECTED: {
				try {

					boolean check = signatureChecker.verify(_block.getBytes(), res.getOffset(),
							res.getSize(), _block.getBytes(), _block.getOffset(), signature_size);
					return new SubBlockInfo(res, check, !check);
				} catch (Exception e) {
					return new SubBlockInfo(res, false, true);
				}
			}
			}
			throw new BlockParserException("Unexpected exception");
		}

		@Override
		public SubBlock signIfPossibleSortantPointToPointTransferedBlock(SubBlock _block) throws BlockParserException {
			try {
				SubBlock res = new SubBlock(_block.getBytes().clone(), _block.getOffset() - getSizeHead(),
						_block.getSize() + getSizeHead());
				signer.sign(_block.getBytes(), _block.getOffset(), _block.getSize(), res.getBytes(), res.getOffset(),
						getSizeHead());
				return res;
			} catch (Exception e) {
				throw new BlockParserException(e);
			}
		}		

	}

	@Override
	public SubBlockParser getParser() {
		return parser;
	}

	void setFirstMessageReceived() {
		this.firstMessageReceived = true;
		this.needToRefreshTransferBlockChecker = true;
	}

	@Override
	protected TransferedBlockChecker getTransferedBlockChecker(TransferedBlockChecker subBlockChercker)
			throws ConnectionException {
		try {
			needToRefreshTransferBlockChecker=false;
			return new ConnectionProtocol.NullBlockChecker(subBlockChercker, this.isCrypted(),
					(short) parser.getSizeHead());
		} catch (Exception e) {
			needToRefreshTransferBlockChecker = true;
			throw new ConnectionException(e);
		}
	}

	@Override
	public boolean isTransferBlockCheckerChangedImpl() {
		return needToRefreshTransferBlockChecker;

	}

	/*private static class BlockChecker extends TransferedBlockChecker {
		private final ASymmetricSignatureType signatureType;
		private final int signatureSize;
		private transient AbstractSignature signature;
		private transient ASymmetricPublicKey publicKey;

		protected BlockChecker(TransferedBlockChecker _subChecker, ASymmetricSignatureType signatureType,
				ASymmetricPublicKey publicKey, int signatureSize, boolean isCrypted) throws NoSuchAlgorithmException {
			super(_subChecker, !isCrypted);
			this.signatureType = signatureType;
			this.publicKey = publicKey;
			this.signatureSize = signatureSize;
			initSignature();
		}

		@Override
		public Integrity checkDataIntegrity() {
			if (signatureType == null)
				return Integrity.FAIL;
			if (signature == null)
				return Integrity.FAIL;
			if (publicKey == null)
				return Integrity.FAIL;
			return Integrity.OK;
		}

		private void initSignature() throws NoSuchAlgorithmException {
			this.signature = signatureType.getSignatureInstance();
		}

		private void writeObject(ObjectOutputStream os) throws IOException {
			os.defaultWriteObject();
			byte encodedPK[] = publicKey.encode();
			os.writeInt(encodedPK.length);
			os.write(encodedPK);
		}

		private void readObject(ObjectInputStream is) throws IOException {
			try {
				is.defaultReadObject();
				int len = is.readInt();
				byte encodedPK[] = new byte[len];
				is.read(encodedPK);
				publicKey = ASymmetricPublicKey.decode(encodedPK);
				initSignature();
			} catch (IOException e) {
				throw e;
			} catch (Exception e) {
				throw new IOException(e);
			}
		}

		@Override
		public SubBlockInfo checkSubBlock(SubBlock _block) throws BlockParserException {
			try {
				SubBlock res = new SubBlock(_block.getBytes(), _block.getOffset() + signatureSize,
						_block.getSize() - signatureSize);
				signature.initVerify(publicKey);
				signature.update(res.getBytes(), res.getOffset(), res.getSize());
				boolean check = signature.verify(_block.getBytes(), _block.getOffset(), signatureSize);
				return new SubBlockInfo(res, check, !check);
			} catch (Exception e) {
				throw new BlockParserException(e);
			}
		}

	}*/

	@Override
	public boolean needsMadkitLanEditionDatabase() {
		return false;
	}

}
