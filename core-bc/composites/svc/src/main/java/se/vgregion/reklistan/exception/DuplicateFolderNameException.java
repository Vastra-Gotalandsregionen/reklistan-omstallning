package se.vgregion.reklistan.exception;


/**
 * @author Erik Andersson - Monator Technologies AB
 */
public class DuplicateFolderNameException extends Exception {
    public DuplicateFolderNameException() {
        super();
    }

    public DuplicateFolderNameException(String message) {
        super(message);
    }

    public DuplicateFolderNameException(String message, Throwable cause) {
        super(message, cause);
    }

    public DuplicateFolderNameException(Throwable cause) {
        super(cause);
    }

    protected DuplicateFolderNameException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
