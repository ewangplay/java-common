/*******************************************************************************
Copyright ArxanFintech Technology Ltd. 2018 All Rights Reserved.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

                 http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*******************************************************************************/

package com.arxanfintech.common.crypto.core.cryptohash;

public abstract class DigestEngine implements Digest {

	/**
	 * Reset the hash algorithm state.
	 */
	protected abstract void engineReset();

	/**
	 * Process one block of data.
	 *
	 * @param data   the data block
	 */
	protected abstract void processBlock(byte[] data);

	/**
	 * Perform the final padding and store the result in the
	 * provided buffer. This method shall call {@link #flush}
	 * and then {@link #update} with the appropriate padding
	 * data in order to get the full input data.
	 *
	 * @param buf   the output buffer
	 * @param off   the output offset
	 */
	protected abstract void doPadding(byte[] buf, int off);

	/**
	 * This function is called at object creation time; the
	 * implementation should use it to perform initialization tasks.
	 * After this method is called, the implementation should be ready
	 * to process data or meaningfully honour calls such as
	 * {@link #getDigestLength}
	 */
	protected abstract void doInit();

	private int digestLen, blockLen, inputLen;
	private byte[] inputBuf, outputBuf;
	private long blockCount;

	/**
	 * Instantiate the engine.
	 */
	public DigestEngine()
	{
		doInit();
		digestLen = getDigestLength();
		blockLen = getInternalBlockLength();
		inputBuf = new byte[blockLen];
		outputBuf = new byte[digestLen];
		inputLen = 0;
		blockCount = 0;
	}

	private void adjustDigestLen()
	{
		if (digestLen == 0) {
			digestLen = getDigestLength();
			outputBuf = new byte[digestLen];
		}
	}

	/** @see com.arxanfintech.common.crypto.core.cryptohash.crypto.cryptohash.Digest */
	public byte[] digest()
	{
		adjustDigestLen();
		byte[] result = new byte[digestLen];
		digest(result, 0, digestLen);
		return result;
	}

	/** @see com.arxanfintech.common.crypto.core.cryptohash.crypto.cryptohash.Digest */
	public byte[] digest(byte[] input)
	{
		update(input, 0, input.length);
		return digest();
	}

	/** @see com.arxanfintech.common.crypto.core.cryptohash.crypto.cryptohash.Digest */
	public int digest(byte[] buf, int offset, int len)
	{
		adjustDigestLen();
		if (len >= digestLen) {
			doPadding(buf, offset);
			reset();
			return digestLen;
		} else {
			doPadding(outputBuf, 0);
			System.arraycopy(outputBuf, 0, buf, offset, len);
			reset();
			return len;
		}
	}

	/** @see com.arxanfintech.common.crypto.core.cryptohash.crypto.cryptohash.Digest */
	public void reset()
	{
		engineReset();
		inputLen = 0;
		blockCount = 0;
	}

	/** @see com.arxanfintech.common.crypto.core.cryptohash.crypto.cryptohash.Digest */
	public void update(byte input)
	{
		inputBuf[inputLen ++] = (byte)input;
		if (inputLen == blockLen) {
			processBlock(inputBuf);
			blockCount ++;
			inputLen = 0;
		}
	}

	/** @see com.arxanfintech.common.crypto.core.cryptohash.crypto.cryptohash.Digest */
	public void update(byte[] input)
	{
		update(input, 0, input.length);
	}

	/** @see com.arxanfintech.common.crypto.core.cryptohash.crypto.cryptohash.Digest */
	public void update(byte[] input, int offset, int len)
	{
		while (len > 0) {
			int copyLen = blockLen - inputLen;
			if (copyLen > len)
				copyLen = len;
			System.arraycopy(input, offset, inputBuf, inputLen,
				copyLen);
			offset += copyLen;
			inputLen += copyLen;
			len -= copyLen;
			if (inputLen == blockLen) {
				processBlock(inputBuf);
				blockCount ++;
				inputLen = 0;
			}
		}
	}

	/**
	 * Get the internal block length. This is the length (in
	 * bytes) of the array which will be passed as parameter to
	 * {@link #processBlock}. The default implementation of this
	 * method calls {@link #getBlockLength} and returns the same
	 * value. Overriding this method is useful when the advertised
	 * block length (which is used, for instance, by HMAC) is
	 * suboptimal with regards to internal buffering needs.
	 *
	 * @return  the internal block length (in bytes)
	 */
	protected int getInternalBlockLength()
	{
		return getBlockLength();
	}

	/**
	 * Flush internal buffers, so that less than a block of data
	 * may at most be upheld.
	 *
	 * @return  the number of bytes still unprocessed after the flush
	 */
	protected final int flush()
	{
		return inputLen;
	}

	/**
	 * Get a reference to an internal buffer with the same size
	 * than a block. The contents of that buffer are defined only
	 * immediately after a call to {@link #flush()}: if
	 * {@link #flush()} return the value {@code n}, then the
	 * first {@code n} bytes of the array returned by this method
	 * are the {@code n} bytes of input data which are still
	 * unprocessed. The values of the remaining bytes are
	 * undefined and may be altered at will.
	 *
	 * @return  a block-sized internal buffer
	 */
	protected final byte[] getBlockBuffer()
	{
		return inputBuf;
	}

	/**
	 * Get the "block count": this is the number of times the
	 * {@link #processBlock} method has been invoked for the
	 * current hash operation. That counter is incremented
	 * <em>after</em> the call to {@link #processBlock}.
	 *
	 * @return  the block count
	 */
	protected long getBlockCount()
	{
		return blockCount;
	}

	/**
	 * This function copies the internal buffering state to some
	 * other instance of a class extending {@code DigestEngine}.
	 * It returns a reference to the copy. This method is intended
	 * to be called by the implementation of the {@link #copy}
	 * method.
	 *
	 * @param dest   the copy
	 * @return  the value {@code dest}
	 */
	protected Digest copyState(DigestEngine dest)
	{
		dest.inputLen = inputLen;
		dest.blockCount = blockCount;
		System.arraycopy(inputBuf, 0, dest.inputBuf, 0,
			inputBuf.length);
		adjustDigestLen();
		dest.adjustDigestLen();
		System.arraycopy(outputBuf, 0, dest.outputBuf, 0,
			outputBuf.length);
		return dest;
	}
}