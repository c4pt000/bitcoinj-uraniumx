/*
 * Copyright 2013 Google Inc.
 * Copyright 2014 Andreas Schildbach
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.bitcoinj.params;

import java.math.BigInteger;
import java.net.URI;
import java.util.Date;

import org.bitcoinj.core.Block;
import org.bitcoinj.core.ECKey;
import org.bitcoinj.core.NetworkParameters;
import org.bitcoinj.core.StoredBlock;
import org.bitcoinj.core.Utils;
import org.bitcoinj.core.VerificationException;
import org.bitcoinj.net.discovery.HttpDiscovery;
import org.bitcoinj.store.BlockStore;
import org.bitcoinj.store.BlockStoreException;

import static com.google.common.base.Preconditions.checkState;

/**
 * Parameters for the testnet, a separate public instance of Bitcoin that has relaxed rules suitable for development
 * and testing of applications and new Bitcoin versions.
 */
public class TestNet3Params extends AbstractBitcoinNetParams {
      public static final int TESTNET_MAJORITY_WINDOW = 1000;
    public static final int TESTNET_MAJORITY_REJECT_BLOCK_OUTDATED = 750;
    public static final int TESTNET_MAJORITY_ENFORCE_BLOCK_UPGRADE = 501;
    public TestNet3Params() {
    super();
        id = ID_TESTNET;
         packetMagic = 0x0b201807;

        maxTarget = Utils.decodeCompactBits(0x1f0fffffL);
        port = 18235;
        addressHeader = 112;
        p2shHeader = 227;
        dumpedPrivateKeyHeader = 199;
        segwitAddressHrp = "trdc";
        genesisBlock.setTime(1557009671L);
        genesisBlock.setDifficultyTarget(0x1f0fffffL);
        genesisBlock.setNonce(2743);
        spendableCoinbaseDepth = 10;
        subsidyDecreaseBlockCount = 100000;
        String genesisHash = genesisBlock.getHashAsString();
        checkState(genesisHash.equals("6589ebb12bf31c219738eddb2f892f0ea4ae1ba7e24f55718fbcbdf30eba1b03"));

        majorityEnforceBlockUpgrade = TESTNET_MAJORITY_ENFORCE_BLOCK_UPGRADE;
        majorityRejectBlockOutdated = TESTNET_MAJORITY_REJECT_BLOCK_OUTDATED;
        majorityWindow = TESTNET_MAJORITY_WINDOW;

        dnsSeeds = new String[] {
            "testseed.jrn.me.uk"
        };
        // Note this is the same as the BIP32 testnet, as BIP44 makes HD wallets
        // chain agnostic. Dogecoin mainnet has its own headers for legacy reasons.
        
      
        
        
        bip32HeaderP2PKHpub = 0x043587CF;
        bip32HeaderP2PKHpriv = 0x04358394;
    }

      
    

    private static TestNet3Params instance;
    public static synchronized TestNet3Params get() {
        if (instance == null) {
            instance = new TestNet3Params();
        }
        return instance;
    }

    @Override
    public String getPaymentProtocolId() {
        return PAYMENT_PROTOCOL_ID_TESTNET;
    }

    // February 16th 2012
    private static final Date testnetDiffDate = new Date(1329264000000L);

    @Override
    public void checkDifficultyTransitions(final StoredBlock storedPrev, final Block nextBlock,
        final BlockStore blockStore) throws VerificationException, BlockStoreException {
        if (!isDifficultyTransitionPoint(storedPrev.getHeight()) && nextBlock.getTime().after(testnetDiffDate)) {
            Block prev = storedPrev.getHeader();

            // After 15th February 2012 the rules on the testnet change to avoid people running up the difficulty
            // and then leaving, making it too hard to mine a block. On non-difficulty transition points, easy
            // blocks are allowed if there has been a span of 20 minutes without one.
            final long timeDelta = nextBlock.getTimeSeconds() - prev.getTimeSeconds();
            // There is an integer underflow bug in bitcoin-qt that means mindiff blocks are accepted when time
            // goes backwards.
            if (timeDelta >= 0 && timeDelta <= NetworkParameters.TARGET_SPACING * 2) {
                // Walk backwards until we find a block that doesn't have the easiest proof of work, then check
                // that difficulty is equal to that one.
                StoredBlock cursor = storedPrev;
                while (!cursor.getHeader().equals(getGenesisBlock()) &&
                           cursor.getHeight() % getInterval() != 0 &&
                           cursor.getHeader().getDifficultyTargetAsInteger().equals(getMaxTarget()))
                        cursor = cursor.getPrev(blockStore);
                BigInteger cursorTarget = cursor.getHeader().getDifficultyTargetAsInteger();
                BigInteger newTarget = nextBlock.getDifficultyTargetAsInteger();
                if (!cursorTarget.equals(newTarget))
                        throw new VerificationException("Testnet block transition that is not allowed: " +
                        Long.toHexString(cursor.getHeader().getDifficultyTarget()) + " vs " +
                        Long.toHexString(nextBlock.getDifficultyTarget()));
            }
        } else {
            super.checkDifficultyTransitions(storedPrev, nextBlock, blockStore);
        }
    }
}
