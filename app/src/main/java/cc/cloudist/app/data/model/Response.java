package cc.cloudist.app.data.model;

public class Response<T> {
    public Integer code;
    public String error;
    public T results;
}
