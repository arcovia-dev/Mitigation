package dev.abunai.confidentiality.mitigation.tests;

public class OutDataChar extends AbstractChar{

    public OutDataChar(String type, String value) {
        super("OutData", type, value);

    }
    
    public InDataChar toIn() {
        return new InDataChar(type(),value());
    }
}
