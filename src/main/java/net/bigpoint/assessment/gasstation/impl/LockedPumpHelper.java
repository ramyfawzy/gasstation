package net.bigpoint.assessment.gasstation.impl;

import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.bigpoint.assessment.gasstation.GasPump;
import net.bigpoint.assessment.gasstation.GasType;

public class LockedPumpHelper {
	
	private static Logger log = Logger.getLogger(LockedPumpHelper.class.getName());
	
	private final ReentrantLock pumpLock = new ReentrantLock(true);

    private GasPump lockedPump;

    public LockedPumpHelper(GasPump lockedPump) {
        this.lockedPump = lockedPump;
    }

    public void arriveOnPump() {
    	log.log(Level.INFO, "Car is entering pump ...");
    	pumpLock.lock();
    }

    public void pumpGas(double requiredAmount) {
       
        if (!pumpLock.isHeldByCurrentThread()) {
            throw new IllegalStateException("GasPumpQueue not locked by the current thread");
        }
        log.log(Level.INFO, "Car is being served ..."); 
        lockedPump.pumpGas(requiredAmount);
    }

    public void exitPump() {
    	if (pumpLock.isHeldByCurrentThread()) {
    	log.log(Level.INFO, "Car is leaving pump ..."); 	
    	pumpLock.unlock();
    	}
    }

    public double getRemainingAmount() {
        return lockedPump.getRemainingAmount();
    }

    public GasType getGasType() {
        return lockedPump.getGasType();
    }

    public GasPump getLockedPump() {
        return lockedPump;
    }
    
    public Integer getNumberOfCars() {
    	return pumpLock.getQueueLength();
    }
}