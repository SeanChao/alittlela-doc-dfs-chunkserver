package org.alittlela.util;

import org.alittlela.Result;

public class ResultUtil {
	public static final int OK = 0; // non-exist append id
	public static final int ERROR = -1;
	public static final int NO_SUCH_APPEND = -2; // non-exist append id

	public static Result newResult(int status, String msg) {
		return org.alittlela.Result.newBuilder().setStatus(status).setMessage(msg).build();
	}

	public static Result newResult(int status) {
		return newResult(status, null);
	}

	public static Result success() {
		return newResult(OK);
	}

	public static Result error(String msg) {
		return newResult(ERROR, msg);
	}

	public static Result error() {
		return error("unspecified error");
	}

	public static boolean isOk(Result result) {
		return result.getStatus() == OK;
	}
}
