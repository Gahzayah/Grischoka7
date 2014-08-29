/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package grischa.dev.moka7.exceptions;

/**
 *
 * @author hinrichm
 */
public class ISOException extends S7Exception {
        /**
     * Constructs an {@code ISOException} with {@code null}
     * as its error detail message.
     */
    public ISOException() {
        super();
    }

    /**
     * Constructs an {@code ISOException} with the specified detail message.
     *
     * @param message
     *        The detail message (which is saved for later retrieval
     *        by the {@link #getMessage()} method)
     */
    public ISOException(String message) {
        super(message);
    }

    /**
     * Constructs an {@code ISOException} with the specified detail message
     * and cause.
     *
     * <p> Note that the detail message associated with {@code cause} is
     * <i>not</i> automatically incorporated into this exception's detail
     * message.
     *
     * @param message
     *        The detail message (which is saved for later retrieval
     *        by the {@link #getMessage()} method)
     *
     * @param cause
     *        The cause (which is saved for later retrieval by the
     *        {@link #getCause()} method).  (A null value is permitted,
     *        and indicates that the cause is nonexistent or unknown.)
     *
     * @since 1.6
     */
    public ISOException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Constructs an {@code ISOException} with the specified cause and a
     * detail message of {@code (cause==null ? null : cause.toString())}
     * (which typically contains the class and detail message of {@code cause}).
     * This constructor is useful for S7Exception that are little more
     * than wrappers for other throwables.
     *
     * @param cause
     *        The cause (which is saved for later retrieval by the
     *        {@link #getCause()} method).  (A null value is permitted,
     *        and indicates that the cause is nonexistent or unknown.)
     *
     * @since 1.6
     */
    public ISOException(Throwable cause) {
        super(cause);
    }
}
