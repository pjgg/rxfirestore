package com.github.pjgg.rxfirestore.exceptions;

public abstract class RxFirestoreExceptions extends RuntimeException {

	private final int errorCode;
	private final String msg;

	public RxFirestoreExceptions(final int errorCode, final String msg) {
		this.errorCode = errorCode;
		this.msg = msg;
	}

	public int getErrorCode() {
		return errorCode;
	}
}
