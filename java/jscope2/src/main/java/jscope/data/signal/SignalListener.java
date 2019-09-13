/*
 * To change this template, choose Tools | Templates and open the template in the editor.
 */
package jscope.data.signal;

/**
 * @author manduchi
 */
public interface SignalListener{
	static final int	UPDATE_PENDING	= 0;	// change busy-state
	static final int	UPDATE_REPAINT	= 1;	// repaint signal and PENDING
	static final int	UPDATE_LIMITS	= 2;	// change limits and REPAINT

	void signalUpdated(int mode);
}
