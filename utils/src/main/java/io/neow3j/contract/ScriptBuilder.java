package io.neow3j.contract;

import io.neow3j.constants.OpCode;
import io.neow3j.utils.ArrayUtils;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import static io.neow3j.utils.ArrayUtils.trimLeadingZeroes;
import static java.nio.charset.StandardCharsets.UTF_8;

public class ScriptBuilder {

    private DataOutputStream stream;
    private ByteBuffer buffer;
    private ByteArrayOutputStream byteStream;

    public ScriptBuilder() {
        byteStream = new ByteArrayOutputStream();
        stream = new DataOutputStream(byteStream);
        buffer = ByteBuffer.wrap(new byte[8]).order(ByteOrder.LITTLE_ENDIAN);
    }

    public ScriptBuilder opCode(OpCode opCode) {
        writeByte(opCode.getValue());
        return this;
    }

    public ScriptBuilder appCall(byte[] scriptHash) {
        return call(scriptHash, OpCode.APPCALL);
    }

    public ScriptBuilder tailCall(byte[] scriptHash) {
        return call(scriptHash, OpCode.TAILCALL);
    }

    private ScriptBuilder call(byte[] scriptHash, OpCode opCode) {
        if (scriptHash.length != 20) {
            // a NEO script hash is always 160 bit/20 byte long
            throw new IllegalArgumentException("Script hash must be 20 bytes long.");
        }
        writeByte(opCode.getValue());
        // Needs to be written in reverse because of NEO's little-endianness of integers.
        writeReversed(scriptHash);
        return this;
    }

    public ScriptBuilder sysCall(String operation) {
        if (operation.length() == 0)
            throw new IllegalArgumentException("Provided operation string is empty.");

        writeByte(OpCode.SYSCALL.getValue());
        byte[] operationBytes = operation.getBytes(UTF_8);
        if (operationBytes.length > 252)
            throw new IllegalArgumentException("Provided operation is too long.");

        byte[] callArgument = ArrayUtils.concatenate((byte)operationBytes.length, operationBytes);
        writeByte(OpCode.SYSCALL.getValue());
        write(callArgument);
        return this;
    }

    public ScriptBuilder pushInteger(int v) {
        return pushInteger(BigInteger.valueOf(v));
    }

    public ScriptBuilder pushInteger(BigInteger number) {
        if (number.intValue() == -1) {
            writeByte(OpCode.PUSHM1.getValue());
        } else if (number.intValue() == 0) {
            writeByte(OpCode.PUSH0.getValue());
        } else if (number.intValue() >= 1 && number.intValue() <= 16) {
            // OpCodes PUSH1 to PUSH16
            int base = (OpCode.PUSH1.getValue() - 1);
            writeByte(base + number.intValue());
        } else {
            // If the number is larger than 16, it needs to be pushed as a data array.
            pushData(trimLeadingZeroes(number.toByteArray()));
        }
        return this;
    }

    public ScriptBuilder pushBoolean(boolean bool) throws IOException {
        if (bool) {
            writeByte(OpCode.PUSHT.getValue());
        } else {
            writeByte(OpCode.PUSHF.getValue());
        }
        return this;
    }

    /**
     * Adds the data to the script, prefixed with the correct code for its length.
     * @param data The data to add to the script.
     */
    public ScriptBuilder pushData(String data) {
        if (data != null) {
            pushData(data.getBytes(UTF_8));
        } else {
            pushData("".getBytes());
        }
        return this;
    }

    /**
     * Adds the data to the script, prefixed with the correct code for its length.
     * @param data The data to add to the script.
     */
    public ScriptBuilder pushData(byte[] data) {
        pushDataLength(data.length);
        write(data);
        return this;
    }

    public ScriptBuilder pushDataLength(int length) {
        if (length <= OpCode.PUSHBYTES75.getValue()) {
            // For up to 75 bytes of data we can use the OpCodes PUSHBYTES01 to PUSHBYTES75 directly.
            writeByte(length);
        } else if (length <= 255) {
            // If the data is 76 to 255 (0xff) bytes long then write PUSHDATA1 + uint8
            writeByte(OpCode.PUSHDATA1.getValue());
            writeByte(length);
        } else if (length <= 65535) {
            // If the data is 256 to 65535 (0xffff) bytes long then write PUSHDATA2 + uint16
            writeByte(OpCode.PUSHDATA2.getValue());
            writeShort(length);
        } else{
            // If the data is bigger than 65536 then write PUSHDATA4 + uint32
            writeByte(OpCode.PUSHDATA4.getValue());
            writeInt(length);
        }
        return this;
    }

    private void writeByte(int v) {
        try { stream.writeByte(v); }
        catch (IOException e) {throw new IllegalStateException("Got IOException without doing IO.");}
    }

    private void writeShort(int v) {
        buffer.putInt(0, v);
        try { stream.write(buffer.array(), 0, 2); }
        catch (IOException e) {throw new IllegalStateException("Got IOException without doing IO.");}
    }

    private void writeInt(int v) {
        buffer.putInt(0, v);
        try { stream.write(buffer.array(), 0, 4); }
        catch (IOException e) {throw new IllegalStateException("Got IOException without doing IO.");}
    }

    private void write(byte[] data) {
        try { stream.write(data); }
        catch (IOException e) {throw new IllegalStateException("Got IOException without doing IO.");}
    }

    private void writeReversed(byte[] data) {
        try { stream.write(ArrayUtils.reverseArray(data)); }
        catch (IOException e) {throw new IllegalStateException("Got IOException without doing IO.");}
    }

    public byte[] toArray() {
        try { stream.flush(); }
        catch (IOException e) {throw new IllegalStateException("Got IOException without doing IO.");}
        return byteStream.toByteArray();
    }
}
