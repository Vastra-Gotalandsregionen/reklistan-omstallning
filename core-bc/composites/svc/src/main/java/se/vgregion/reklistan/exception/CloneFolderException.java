package se.vgregion.reklistan.exception;


/**
 * @author Erik Andersson - Monator Technologies AB
 */
public class CloneFolderException extends Exception {

    public CloneFolderException() {
        super();
    }

    public CloneFolderException(String message) {
        super(message);
    }

    public CloneFolderException(String message, Throwable cause) {
        super(message, cause);
    }

    public CloneFolderException(Throwable cause) {
        super(cause);
    }

    protected CloneFolderException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
