package com.cboy.audio;

import io.github.jaredmdobson.concentus.OpusApplication;
import io.github.jaredmdobson.concentus.OpusDecoder;
import io.github.jaredmdobson.concentus.OpusEncoder;
import io.github.jaredmdobson.concentus.OpusException;

import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

public class OpusProcessor {
    private static final int DEFAULT_FRAME_SIZE = 960; // Opus典型帧大小


    public byte[] opusFrameToPcm(byte[] opus) throws OpusException {
        OpusDecoder decoder = new OpusDecoder(AudioFormat.SAMPLE_RATE, AudioFormat.CHANNEL);
        short[] pcmBuffer = new short[DEFAULT_FRAME_SIZE];
        int samplesDecoded = decoder.decode(opus, 0, opus.length, pcmBuffer, 0, DEFAULT_FRAME_SIZE, false);
        ByteBuffer byteBuffer = ByteBuffer.allocate(DEFAULT_FRAME_SIZE * 2);
        for (int i = 0; i < samplesDecoded; i++) {
            // little endian
            byteBuffer.put(i * 2, (byte)(pcmBuffer[i] & 0xFF));
            byteBuffer.put(i * 2 + 1, (byte)((pcmBuffer[i] >> 8) & 0xFF));
        }
        return byteBuffer.array();
    }

    /**
     *
     * @param pcmData little endian
     * @param samplesRate hz
     * @param bitsDepth 2 bytes
     * @param channel 1 mono
     * @param frameDurationMs millisecond
     * @return opus frame list
     */
    public List<byte[]> pcmToOpus(byte[] pcmData, int samplesRate, int bitsDepth, int channel, int frameDurationMs) throws OpusException{

        OpusEncoder encoder = new OpusEncoder(samplesRate, channel, OpusApplication.OPUS_APPLICATION_VOIP);

        int sampleBytes = bitsDepth / 8; // samples的大小 byte

        int frameSize = samplesRate * frameDurationMs / 1000 * channel; // 一帧的sample数 (frame需要乘以channel)

        List<byte[]> opusFrames = new LinkedList<>();
        short[] pcmBuffer = new short[frameSize];
        for (int i = 0; i < pcmData.length; i += frameSize * sampleBytes) {
            int pcmEnd = Math.min(i+frameSize * sampleBytes, pcmData.length);
            for (int j = i, k = 0; j < pcmEnd; k++) {
                short value = 0;
                byte low = pcmData[j++];
                byte high = pcmData[j++];
                value |= low;
                value |= (short) (high << 8);
                pcmBuffer[k] = value;
            }
            byte[] opusBuffer = new byte[1275]; // max frame of opus
            int opusLength = encoder.encode(pcmBuffer, 0, pcmBuffer.length, opusBuffer, 0, opusBuffer.length);
            byte[] opusFrame = new byte[opusLength];
            System.arraycopy(opusBuffer, 0, opusFrame, 0, opusLength);
            opusFrames.add(opusFrame);
        }
        return opusFrames;
    }
    static final String audioFilePath = "/Users/yuhangbin/Desktop/output.pcm";

    public static void main(String[] args) throws Exception{
        OpusProcessor opusProcessor = new OpusProcessor();
        byte[] pcm = Files.readAllBytes(Path.of(audioFilePath));
        List<byte[]> result = opusProcessor.pcmToOpus(pcm, 16000, 16, 1, 20);
        System.out.println(result.size());
    }
}
