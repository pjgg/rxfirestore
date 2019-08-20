package com.github.pjgg.rxfirestore.exceptions;

public class NotFoundExceptions extends RxFirestoreExceptions {

	public static final int NOT_FOUND_CODE = 2;

	public NotFoundExceptions(String msg) {
		super(NOT_FOUND_CODE, msg);
	}
}
