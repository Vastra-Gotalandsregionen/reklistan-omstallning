package se.vgregion.reklistan.exception;


/**
 * @author Erik Andersson - Monator Technologies AB
 */
public class UnpublishFolderException extends Exception {

    public UnpublishFolderException() {
        super();
    }

    public UnpublishFolderException(String message) {
        super(message);
    }

    public UnpublishFolderException(String message, Throwable cause) {
        super(message, cause);
    }

    public UnpublishFolderException(Throwable cause) {
        super(cause);
    }

    protected UnpublishFolderException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
