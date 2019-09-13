package jscope.waveform;

interface WaveformEditorListener{
	void waveformUpdated(float[] waveX, float[] waveY, int newIdx);
}