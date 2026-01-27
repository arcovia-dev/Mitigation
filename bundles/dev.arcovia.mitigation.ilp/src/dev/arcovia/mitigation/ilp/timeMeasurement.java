package dev.arcovia.mitigation.ilp;

public class timeMeasurement {
    private long startTime;
    private long constraints;
    private long dfdAnalysis;
    private long solving;
    private long endTime;
    
    public void start() {
        this.startTime = System.currentTimeMillis();
        
    }
    
    public void constraints() {
        this.constraints =System.currentTimeMillis();
    }
    
    public void analysis () {
        this.dfdAnalysis = System.currentTimeMillis();
    }
    
    public void solving () {
        this.solving = System.currentTimeMillis();
    }
    
    public void stop() {
        this.endTime = System.currentTimeMillis();
    }
    
    public long getExecutionTime() {
        return endTime - startTime;
    }
    
    public long getSolvingTime() {
        return solving - dfdAnalysis;
    }
    public long getIsolatedExecutio() {
        return endTime - dfdAnalysis + constraints - startTime;
    }
    
    
}
