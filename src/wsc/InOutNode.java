package wsc;

import java.util.Set;

public interface InOutNode {
    public Set<String> getInputs();
    public Set<String> getOutputs();
    public Set<String> getPrecondtion();
    public Set<String> getPostcondtion();
}
