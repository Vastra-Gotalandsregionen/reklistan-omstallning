package se.vgregion.reklistan.exception;


/**
 * @author Erik Andersson - Monator Technologies AB
 */
public class PublishFolderException extends Exception {

    public PublishFolderException() {
        super();
    }

    public PublishFolderException(String message) {
        super(message);
    }

    public PublishFolderException(String message, Throwable cause) {
        super(message, cause);
    }

    public PublishFolderException(Throwable cause) {
        super(cause);
    }

    protected PublishFolderException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
