package com.kofua.jadx.stringdecrypt;

public class StringDecryptState {
	private boolean active;

	public StringDecryptState(boolean active) {
		this.active = active;
	}

	public boolean isActive() {
		return active;
	}

	public void setActive(boolean active) {
		this.active = active;
	}
}
