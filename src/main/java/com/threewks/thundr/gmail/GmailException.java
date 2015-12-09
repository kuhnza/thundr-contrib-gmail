package com.threewks.thundr.gmail;

import com.threewks.thundr.exception.BaseException;

public class GmailException extends BaseException {

    public GmailException(Throwable cause) {
        super(cause);
    }

    public GmailException(Throwable cause, String format, Object... formatArgs) {
        super(cause, format, formatArgs);
    }

    public GmailException(String format, Object... formatArgs) {
        super(format, formatArgs);
    }
}
