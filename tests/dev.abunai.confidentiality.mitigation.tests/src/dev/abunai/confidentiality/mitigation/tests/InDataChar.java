package dev.abunai.confidentiality.mitigation.tests;

public class InDataChar extends AbstractChar{

    public InDataChar(String type, String value) {
        super("InData", type, value);
    }
    
    public OutDataChar toOut() {
        return new OutDataChar(type(),value());
    }
}
