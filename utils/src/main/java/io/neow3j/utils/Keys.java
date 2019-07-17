package io.neow3j.utils;

import io.neow3j.constants.NeoConstants;
import io.neow3j.constants.OpCode;
import io.neow3j.contract.ScriptBuilder;
import io.neow3j.crypto.Base58;
import io.neow3j.crypto.Hash;
import io.neow3j.crypto.exceptions.AddressFormatException;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static io.neow3j.constants.NeoConstants.MAX_PUBLIC_KEYS_PER_MULTISIG_ACCOUNT;
import static io.neow3j.utils.ArrayUtils.concatenate;

/**
 * Key utilities.
 */
public class Keys {

    private Keys() {
    }

    public static String getAddress(BigInteger publicKey) {
        return getAddress(Numeric.toHexStringNoPrefix(publicKey));
    }

    public static String getAddress(String publicKeyWithNoPrefix) {
        return getAddress(Numeric.hexStringToByteArray(publicKeyWithNoPrefix));
    }

    public static String getAddress(byte[] publicKey) {
        byte[] scriptHash = getScriptHashFromPublicKey(publicKey);
        return toAddress(scriptHash);
    }

    public static String getMultiSigAddress(int amountSignatures, List<BigInteger> publicKeys) {
        return getMultiSigAddress(amountSignatures, publicKeys.toArray(new BigInteger[0]));
    }

    public static String getMultiSigAddress(int amountSignatures, BigInteger... publicKeys) {
        byte[][] pubKeysArray = new byte[publicKeys.length][];
        for (int i = 0; i < publicKeys.length; i++) {
            pubKeysArray[i] = publicKeys[i].toByteArray();
        }
        byte[] scriptHash = getScriptHashFromPublicKey(amountSignatures, pubKeysArray);
        return toAddress(scriptHash);
    }

    public static String getMultiSigAddress(int amountSignatures, byte[]... publicKeys) {
        byte[] scriptHash = getScriptHashFromPublicKey(amountSignatures, publicKeys);
        return toAddress(scriptHash);
    }

    public static byte[] getScriptHashFromPublicKey(byte[] publicKey) {
        return getScriptHashFromPublicKey(1, publicKey);
    }

    public static byte[] getScriptHashFromPublicKey(int amountSignatures, byte[]... publicKeys) {
        byte[] verificationScript;
        if (publicKeys.length == 1) {
            verificationScript = getVerificationScriptFromPublicKey(publicKeys[0]);
        } else {
            verificationScript = getVerificationScriptFromPublicKeys(amountSignatures, publicKeys);
        }
        return Hash.calculateScriptHash(verificationScript);
    }

    /**
     * Checks if the given public key is in encoded format and encodes it if not.
     */
    public static byte[] checkAndEncodePublicKey(byte[] publicKey) {
        if (!isPublicKeyEncoded(publicKey)) {
            return getPublicKeyEncoded(publicKey);
        } else {
            return publicKey;
        }
    }

    public static byte[] publicKeyIntegerToByteArray(BigInteger publicKey) {
        return Numeric.toBytesPadded(publicKey, NeoConstants.PUBLIC_KEY_SIZE);
    }

    public static byte[] privateKeyIntegerToByteArray(BigInteger privateKey) {
        return Numeric.toBytesPadded(privateKey, NeoConstants.PRIVATE_KEY_SIZE);
    }

    /**
     * Creates the verification script for the given key.
     * Checks if the key is in encoded format, and encodes it if not.
     * @return the verification script.
     */
    public static byte[] getVerificationScriptFromPublicKey(BigInteger publicKey) {
        byte[] publicKeyBytes = checkAndEncodePublicKey(publicKeyIntegerToByteArray(publicKey));
        return getVerificationScriptFromPublicKeyEncoded(publicKeyBytes);
    }

    /**
     * Creates the verification script for the given key.
     * Checks if the key is in encoded format, and encodes it if not.
     * @return the verification script.
     */
    public static byte[] getVerificationScriptFromPublicKey(byte[] publicKey) {
        publicKey = checkAndEncodePublicKey(publicKey);
        return getVerificationScriptFromPublicKeyEncoded(publicKey);
    }

    private static byte[] getVerificationScriptFromPublicKeyEncoded(byte[] encodedPublicKey) {
        return new ScriptBuilder()
                .pushData(encodedPublicKey)
                .opCode(OpCode.CHECKSIG)
                .toArray();
    }

    /**
     * Creates the multi-sig verification script for the given keys and the signing threshold.
     * Checks if the keys are in encoded format, and encodes them if not.
     * @return the multi-sig verification script.
     */
    public  static byte[] getVerificationScriptFromPublicKeys(int signingThreshold,
                                                              List<BigInteger> publicKeys) {

       return getVerificationScriptFromPublicKeys(
               signingThreshold,
               publicKeys.stream()
                       .map(Keys::publicKeyIntegerToByteArray)
                       .toArray(byte[][]::new)
       );
    }

    /**
     * Creates the multi-sig verification script for the given keys and the signing threshold.
     * Checks if the keys are in encoded format, and encodes them if not.
     * @return the multi-sig verification script.
     */
    public  static byte[] getVerificationScriptFromPublicKeys(int signingThreshold,
                                                              byte[]... publicKeys) {

        List<byte[]> encodedPublicKeys = new ArrayList<>(publicKeys.length);
        for (byte[] key : publicKeys) {
            encodedPublicKeys.add(checkAndEncodePublicKey(key));
        }

        return getVerificationScriptFromPublicKeysEncoded(signingThreshold, encodedPublicKeys);
    }

    private static byte[] getVerificationScriptFromPublicKeysEncoded(int signingThreshold,
                                                                    List<byte[]> encodedPublicKeys) {

        if (signingThreshold < 2 || signingThreshold > encodedPublicKeys.size()) {
            throw new IllegalArgumentException("Signing threshold must be at least 2 and not " +
                    "higher than the number of public keys.");
        }
        if (encodedPublicKeys.size() > MAX_PUBLIC_KEYS_PER_MULTISIG_ACCOUNT) {
            throw new IllegalArgumentException("At max " + MAX_PUBLIC_KEYS_PER_MULTISIG_ACCOUNT +
                    " public keys can take part in a multi-sig account");
        }
        ScriptBuilder builder = new ScriptBuilder()
                .pushInteger(signingThreshold);
        encodedPublicKeys.forEach(key -> builder
                .pushData(key));
        return builder
                .pushInteger(encodedPublicKeys.size())
                .opCode(OpCode.CHECKMULTISIG)
                .toArray();
    }


    public static byte[] getPublicKeyEncoded(byte[] publicKeyNotEncoded) {
        // converting to unsigned (Java byte's type is signed)
        int[] publicKeyArray = new int[publicKeyNotEncoded.length];
        for (int i = 0; i < publicKeyNotEncoded.length; i++) {
            publicKeyArray[i] = publicKeyNotEncoded[i] & 0xFF;
        }
        // based on: https://tools.ietf.org/html/rfc5480#section-2.2
        if (publicKeyArray[64] % 2 == 1) {
            return concatenate(new byte[]{0x03}, Arrays.copyOfRange(publicKeyNotEncoded, 1, NeoConstants.PUBLIC_KEY_SIZE));
        } else {
            return concatenate(new byte[]{0x02}, Arrays.copyOfRange(publicKeyNotEncoded, 1, NeoConstants.PUBLIC_KEY_SIZE));
        }
    }

    public static boolean isPublicKeyEncoded(byte[] publicKey) {
        if (publicKey.length > 1 && publicKey[0] != 0x04) {
            return true;
        }
        return false;
    }

    public static String toAddress(byte[] scriptHash) {
        byte[] data = new byte[1];
        data[0] = NeoConstants.COIN_VERSION;
        byte[] dataAndScriptHash = concatenate(data, scriptHash);
        byte[] checksum = Hash.sha256(Hash.sha256(dataAndScriptHash));
        byte[] first4BytesCheckSum = new byte[4];
        System.arraycopy(checksum, 0, first4BytesCheckSum, 0, 4);
        byte[] dataToEncode = concatenate(dataAndScriptHash, first4BytesCheckSum);
        return Base58.encode(dataToEncode);
    }

    // TODO 14.07.19 claude: Write test
    public static boolean isValidAddress(String address) {
        byte[] data;
        try {
             data = Base58.decode(address);
        } catch (AddressFormatException e) {
            return false;
        }
        if (data.length != 25) return false;
        if (data[0] != NeoConstants.COIN_VERSION) return false;
        byte[] checksum = Hash.sha256(Hash.sha256(data, 0, 21));
        for (int i = 0; i < 4; i++) {
            if (data[data.length - 4 + i] != checksum[i]) return false;
        }
        return true;
    }

    public static byte[] toScriptHash(String address) {
        if (!isValidAddress(address)) throw new IllegalArgumentException("Not a valid NEO address.");
        byte[] data = Base58.decode(address);
        byte[] buffer = new byte[20];
        System.arraycopy(data, 1, buffer, 0, 20);
        return buffer;
    }

    public static String scriptHashToAddress(String input) {
        byte[] inputBytes = Numeric.hexStringToByteArray(input);
        return toAddress(inputBytes);
    }
}
