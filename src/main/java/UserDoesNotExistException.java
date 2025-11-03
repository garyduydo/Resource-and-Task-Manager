public class UserDoesNotExistException extends Exception {
    public UserDoesNotExistException(String errMsg)
    {
        super(errMsg);
    }
}
