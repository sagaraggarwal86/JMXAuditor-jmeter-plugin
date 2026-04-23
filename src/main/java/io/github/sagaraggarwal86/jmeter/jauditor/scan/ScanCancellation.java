package io.github.sagaraggarwal86.jmeter.jauditor.scan;

public final class ScanCancellation {
    private volatile Reason reason = Reason.NONE;

    public void userCancel() {
        reason = Reason.USER_CANCEL;
    }

    public void timeout() {
        reason = Reason.TIMEOUT;
    }

    public Reason reason() {
        return reason;
    }

    public enum Reason {NONE, USER_CANCEL, TIMEOUT}
}
