package com.github.tvbox.osc.util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.Random;
import java.util.zip.Inflater;

public final class EncryptionUtils {
    public Random random;

    {
        if (random == null) {
            random = new Random();
        }
    }

    public final ByteArrayOutputStream outputStream = new ByteArrayOutputStream(8);
    public byte[] inputArray;
    public byte[] outputArray;
    public byte[] tempArray;
    public int index;
    public int length;
    public int padding;
    public int count;
    public int offset;
    public byte[] key;
    public boolean initialized = true;
    public int position;

    public String byteHexStr(byte[] bytes) {
        char[] hexArray = "0123456789ABCDEF".toCharArray();
        StringBuilder sb = new StringBuilder();
        for (byte aByte : bytes) {
            sb.append(hexArray[(aByte & 240) >> 4]);
            sb.append(hexArray[aByte & 15]);
        }
        return sb.toString();
    }

    public long convertToLong(byte[] byteArray, int offset, int length) {
        int end = length > 8 ? offset + 8 : length + offset;
        long result = 0;
        while (offset < end) {
            result = (result << 8) | ((long) (byteArray[offset] & 255));
            offset++;
        }
        return (4294967295L & result) | (result >>> 32);
    }

    public byte[] encrypt(byte[] input, int offset) {
        long value1 = convertToLong(input, offset, 4);
        long value2 = convertToLong(input, offset + 4, 4);

        if (key != null) {
            long key1 = convertToLong(key, 0, 4);
            long key2 = convertToLong(key, 4, 4);
            long key3 = convertToLong(key, 8, 4);
            long key4 = convertToLong(key, 12, 4);
            long temp = 3816266640L;
            for (int i = 16; i > 0; i--) {
                value2 = value2 - ((value1 << 4) + key3 ^ value1 + temp ^ (value1 >>> 5) + key4) & 4294967295L;
                value1 = value1 - ((value2 << 4) + key1 ^ value2 + temp ^ (value2 >>> 5) + key2) & 4294967295L;
                temp = temp - 2654435769L & 4294967295L;
            }
            outputStream.reset();
            writeInt((int) value1);
            writeInt((int) value2);
            return outputStream.toByteArray();
        }
        return null;
    }

    public void processInput(byte[] input, int offset, int length) {
        padding = 0;
        while (true) {
            if (padding >= 8) {
                outputArray = encrypt(outputArray, 0);
                position += 8;
                index += 8;
                padding = 0;
                return;
            }
            if (position + padding >= length) {
                return;
            }
            byte[] tempArray = outputArray;
            tempArray[padding] ^= input[index + offset + padding];
            padding = padding + 1;
        }
    }

    public byte[] decompress(byte[] input, byte[] key) {
        Inflater inflater = new Inflater();
        inflater.setInput(input);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        while (true) {
            try {
                if (inflater.finished()) {
                    break;
                }
                int inflate = inflater.inflate(buffer);
                if (inflate == 0) {
                    break;
                }
                outputStream.write(buffer, 0, inflate);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        inflater.end();
        try {
            outputStream.close();
        } catch (IOException e1) {
            e1.printStackTrace();
        }
        byte[] byteArray = outputStream.toByteArray();
        int length = byteArray.length;
        offset = 0;
        index = 0;
        this.key = key;
        byte[] paddingArray = new byte[8];
        if (length % 8 == 0 && length >= 16) {
            outputArray = encrypt(byteArray, 0);
            padding = (byte) (outputArray[0] & 7);
            int i = (length - padding) - 10;
            if (i >= 0) {
                for (int j = 0; j < 8; j++) {
                    paddingArray[j] = 0;
                }
                tempArray = new byte[i];
                offset = 0;
                index = 8;
                position = 8;
                padding++;
                count = 1;
                while (count <= 2) {
                    if (padding < 8) {
                        padding = padding + 1;
                        count = count + 1;
                    }
                    if (padding == 8) {
                        processInput(byteArray, 0, length);
                        paddingArray = byteArray;
                    }
                }
                int k = 0;
                while (i != 0) {
                    if (padding < 8) {
                        if (tempArray != null) {
                            if (outputArray != null) {
                                tempArray[k] = (byte) (paddingArray[offset + padding] ^ outputArray[padding]);
                                k++;
                                i--;
                                padding = padding + 1;
                            }
                        }
                    }
                    if (padding == 8) {
                        offset = index - 8;
                        processInput(byteArray, 0, length);
                        paddingArray = byteArray;
                    }
                }
                count = 1;
                while (true) {
                    if (count < 8) {
                        if (padding < 8) {
                            if (((byte) (paddingArray[offset + padding] ^ outputArray[padding])) != 0) {
                                break;
                            } else {
                                padding = padding + 1;
                            }
                        }
                        if (padding == 8) {
                            offset = index;
                            processInput(byteArray, 0, length);
                            paddingArray = byteArray;
                        }
                        count++;
                    } else {
                        if (tempArray != null) {
                            return tempArray;
                        }
                    }
                }
            }
        }
        return null;
    }

    public byte[] encryptData(byte[] input, byte[] key) {
        int length = input.length;
        if (key == null) {
            return input;
        } else {
            inputArray = new byte[8];
            outputArray = new byte[8];
            count = 0;
            offset = 0;
            index = 0;
            this.key = key;
            initialized = true;
            int padding = (length + 10) % 8;
            this.padding = padding;
            if (padding != 0) {
                this.padding = 8 - padding;
            }
            tempArray = new byte[this.padding + length + 10];
            padding = generateRandom();
            int paddingLength = this.padding;
            inputArray[0] = (byte) (padding & 248 | paddingLength);
            if (1 <= paddingLength) {
                padding = 1;
                while (true) {
                    inputArray[padding] = (byte) (generateRandom() & 255);
                    if (padding == paddingLength) {
                        break;
                    }
                    padding++;
                }
            }
            this.padding++;
            for (padding = 0; padding <= 7; padding++) {
                outputArray[padding] = 0;
            }
            count = 1;
            while (count <= 2) {
                if (this.padding < 8) {
                    inputArray[this.padding] = (byte) (generateRandom() & 255);
                    this.padding = this.padding + 1;
                    count++;
                }
                if (this.padding == 8) {
                    processPadding();
                }
            }
            padding = 0;
            while (length > 0) {
                if (this.padding < 8) {
                    inputArray[this.padding] = input[padding];
                    this.padding = this.padding + 1;
                    length = length - 1;
                    padding = padding + 1;
                }
                if (this.padding == 8) {
                    processPadding();
                }
            }
            count = 1;
            while (true) {
                if (count > 7) {
                    if (tempArray != null) {
                        return tempArray;
                    }
                }
                if (this.padding < 8) {
                    inputArray[this.padding] = 0;
                    this.padding = this.padding + 1;
                    count = count + 1;
                }
                if (this.padding == 8) {
                    processPadding();
                }
            }
        }
    }

    public void processPadding() {
        this.padding = 0;
        while (true) {
            if (this.padding >= 8) {
                long value1 = convertToLong(inputArray, 0, 4);
                long value2 = convertToLong(inputArray, 4, 4);
                if (key != null) {
                    long key1 = convertToLong(key, 0, 4);
                    long key2 = convertToLong(key, 4, 4);
                    long key3 = convertToLong(key, 8, 4);
                    long key4 = convertToLong(key, 12, 4);
                    long temp = 0L;
                    for (this.padding = 16; this.padding > 0; this.padding--) {
                        temp = temp + 2654435769L & 4294967295L;
                        value1 = value1 + ((value2 << 4) + key1 ^ value2 + temp ^ (value2 >>> 5) + key2) & 4294967295L;
                        value2 = value2 + ((value1 << 4) + key3 ^ value1 + temp ^ (value1 >>> 5) + key4) & 4294967295L;
                    }
                    outputStream.reset();
                    writeInt((int) value1);
                    writeInt((int) value2);

                    if (tempArray != null) {
                        System.arraycopy(outputStream.toByteArray(), 0, tempArray, offset, 8);
                        this.padding = 0;
                        while (true) {
                            if (this.padding >= 8) {
                                if (inputArray != null) {
                                    System.arraycopy(inputArray, 0, outputArray, 0, 8);
                                    index = offset;
                                    offset = offset + 8;
                                    this.padding = 0;
                                    initialized = false;
                                    return;
                                }
                            }

                            tempArray[offset + this.padding] = (byte) (tempArray[offset + this.padding] ^ outputArray[this.padding]);
                            this.padding = this.padding + 1;
                        }
                    }
                }
            }

            if (initialized) {
                inputArray[this.padding] = (byte) (inputArray[this.padding] ^ outputArray[this.padding]);
            } else {
                assert tempArray != null;
                inputArray[this.padding] = (byte) (tempArray[index + this.padding] ^ inputArray[this.padding]);
            }
            this.padding = this.padding + 1;
        }
    }

    public int generateRandom() {
        return random.nextInt();
    }

    public void writeInt(int value) {
        outputStream.write(value >>> 24);
        outputStream.write(value >>> 16);
        outputStream.write(value >>> 8);
        outputStream.write(value);
    }
    
    public static final byte[] f14728z = {73, 76, 79, 86, 69, 66, 73, 68, 73, 89, 73, 78, 71, 82, 72, 73};
    public static EncryptionUtils enc = new EncryptionUtils();

    public static String getToken() {
        String valueOf = String.valueOf(System.currentTimeMillis());
        try {
            return enc.byteHexStr(enc.encryptData(valueOf.getBytes("UTF-8"), f14728z));
        } catch (UnsupportedEncodingException e) {
            // TODO 自动生成的 catch 块
            e.printStackTrace();
            return "";
        }
    }

    public static String dec(byte[] data) {
        try {
            return new String(enc.decompress(data, f14728z), "UTF-8");
        } catch (UnsupportedEncodingException e) {
            // TODO 自动生成的 catch 块
            e.printStackTrace();
            return "";
        }
    }
}
