// package jTraverser;
class IllegalDataException extends Exception{
    private static final long serialVersionUID = -4526579649695188493L;
    Data                      invalid_data;

    public IllegalDataException(final String message, final Data invalid_data){
        super(message);
        this.invalid_data = invalid_data;
    }
}