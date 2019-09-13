package jscope.data;

import java.util.EventListener;

public interface ProviderEventListener extends EventListener{
	public void handleProviderEvent(DataProvider dp, String info, int curr, int max);
}
