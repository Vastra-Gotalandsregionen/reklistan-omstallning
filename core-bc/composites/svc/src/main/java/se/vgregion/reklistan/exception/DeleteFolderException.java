package se.vgregion.reklistan.exception;


/**
 * @author Erik Andersson - Monator Technologies AB
 */
public class DeleteFolderException extends Exception {

    public DeleteFolderException() {
        super();
    }

    public DeleteFolderException(String message) {
        super(message);
    }

    public DeleteFolderException(String message, Throwable cause) {
        super(message, cause);
    }

    public DeleteFolderException(Throwable cause) {
        super(cause);
    }

    protected DeleteFolderException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
