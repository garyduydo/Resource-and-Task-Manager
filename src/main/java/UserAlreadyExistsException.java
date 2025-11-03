public class UserAlreadyExistsException extends Exception {
    public UserAlreadyExistsException(String errMsg)
    {
        super(errMsg);
    }
}
