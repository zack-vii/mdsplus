/*
 * To change this template, choose Tools | Templates and open the template in the editor.
 */
package jscope.data.wavedata;

/**
 * Defines the methods called by WaveData to report regions of increased resolution or new data available
 */
public interface WaveDataListener{
	public void dataRegionUpdated(Object currData, boolean update_limits);

	public void legendUpdated(String name);

	public void sourceUpdated(XYData xydata);

	public void updatePending();
}
