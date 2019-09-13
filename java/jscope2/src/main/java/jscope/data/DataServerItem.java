package jscope.data;

import java.util.Map;

/* $Id$ */
public class DataServerItem{
	public String				server;
	public String				browse_class;
	public String				class_name;
	public String				name;
	public Map<String, String>	properties;
	public String				user;

	public DataServerItem(){
		this(null, null, null, null, null, null);
	}

	public DataServerItem(final String user){
		this(null, null, user, null, null, null);
	}

	public DataServerItem(final String name, final String server, final String user, final String class_name, final String browse_class, final Map<String, String> properties){
		this.name = name;
		this.server = server;
		this.user = user;
		this.class_name = class_name;
		this.browse_class = browse_class;
		this.properties = properties;
	}

	public boolean equals(final DataServerItem dsi) {
		try{
			return this.name.equals(dsi.name);
			// &&
			// argument.equals(dsi.argument) &&
			// class_name.equals(dsi.class_name) &&
			// browse_class.equals(dsi.browse_class);
		}catch(final Exception exc){
			return false;
		}
	}

	public boolean equals(final String name_in) {
		return this.name.equals(name_in);
	}

	@Override
	public String toString() {
		return this.name;
	}
}
