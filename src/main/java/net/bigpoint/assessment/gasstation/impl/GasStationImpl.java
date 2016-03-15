package net.bigpoint.assessment.gasstation.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.bigpoint.assessment.gasstation.GasPump;
import net.bigpoint.assessment.gasstation.GasStation;
import net.bigpoint.assessment.gasstation.GasType;
import net.bigpoint.assessment.gasstation.exceptions.GasTooExpensiveException;
import net.bigpoint.assessment.gasstation.exceptions.NotEnoughGasException;

public class GasStationImpl implements GasStation {

    private Map<GasType, LinkedBlockingQueue<LockedPumpHelper>> gasTypePumpsMap = new ConcurrentHashMap<GasType, LinkedBlockingQueue<LockedPumpHelper>>();
    
    private Map<GasType, Double> gasPrices = new ConcurrentHashMap<GasType, Double>();
    
    static Logger log = Logger.getLogger(GasStationImpl.class.getName());
    
    private AtomicInteger numberOfcancellationsNoGas = new AtomicInteger(0);
    private AtomicInteger numberOfCancellationsTooExpensive = new AtomicInteger(0);
    private AtomicInteger salesRevenue = new AtomicInteger(0);
    private AtomicInteger salesCounter = new AtomicInteger(0);
    

    public GasStationImpl() {
        for (GasType type : GasType.values()) {
        	gasTypePumpsMap.put(type, new LinkedBlockingQueue<LockedPumpHelper>());
        }
    }

    @Override
    public void addGasPump(GasPump pump) {
        LinkedBlockingQueue<LockedPumpHelper> stationPumps;
        LockedPumpHelper newPump = new LockedPumpHelper(pump);
		stationPumps = gasTypePumpsMap.get(pump.getGasType());
		stationPumps.add(newPump);
		
    }

    @Override
    public Collection<GasPump> getGasPumps() {
        List<GasPump> allPumps = new ArrayList<GasPump>();
        for (GasType gasType : gasTypePumpsMap.keySet()) {
            for (LockedPumpHelper gasTypePumpLock : gasTypePumpsMap.get(gasType)) {
            	allPumps.add(gasTypePumpLock.getLockedPump());
            }
        }
        return allPumps;
    }
    
    
    private LockedPumpHelper routeToSuitablePump(GasType type, double amountInLiters) throws NotEnoughGasException{
        LinkedBlockingQueue<LockedPumpHelper> suitableGasTypePumps = gasTypePumpsMap.get(type);
        if (suitableGasTypePumps == null || suitableGasTypePumps.isEmpty()) {
   		 	log.log(Level.INFO, "Gas type "+ type.toString() +" not available !");
            throw new IllegalArgumentException();
        }
        LockedPumpHelper suitablePump = null;
        for (LockedPumpHelper tempPump : suitableGasTypePumps) {
            if (amountInLiters > tempPump.getRemainingAmount()) {
                continue; 
            }
            
            suitablePump = tempPump;
            
            if (suitablePump.getNumberOfCars() > tempPump.getNumberOfCars()) {
            	 suitablePump = tempPump;
            }
        }
        if(suitablePump == null) {
   		 log.log(Level.INFO, "Gas type "+ type.toString() +" is not enough for the sale !");
   		 throw new NotEnoughGasException();
   	 }
        return suitablePump;
    }
    
    @Override
    public double buyGas(GasType type, double amountInLiters, double maxPricePerLiter) throws NotEnoughGasException, GasTooExpensiveException {
       LockedPumpHelper pumpLockHelper = null;
       boolean gasSold = false;
       double price = getPrice(type);
       if (price > maxPricePerLiter) {
       	numberOfCancellationsTooExpensive.incrementAndGet();
           throw new GasTooExpensiveException();
       }

       try {
    	   pumpLockHelper = routeToSuitablePump(type, amountInLiters);
       } catch (NotEnoughGasException notEnoughGasException) {
       	numberOfcancellationsNoGas.incrementAndGet();
            throw notEnoughGasException;
       }
       
       pumpLockHelper.arriveOnPump();
        	   try {
        		   if (amountInLiters <= pumpLockHelper.getRemainingAmount()){
        			   pumpLockHelper.pumpGas(amountInLiters);
        			   gasSold = true;
        		   }
        		   
          		} finally {
          			pumpLockHelper.exitPump();
          		}
       if (gasSold) {
           double amountDue = amountInLiters * price;
           incrementRevenue(amountDue);
           salesCounter.incrementAndGet();
           return amountDue;
       } else {
           return buyGas(type, amountInLiters, maxPricePerLiter);
       }
   }

    private void incrementRevenue(double value) {
    	salesRevenue.addAndGet((int)(value * 1000));
    }

    @Override
    public double getRevenue() {
        return salesRevenue.get() / 1000d;
    }

    @Override
    public int getNumberOfSales() {
        return salesCounter.get();
    }

    @Override
    public int getNumberOfCancellationsNoGas() {
        return numberOfcancellationsNoGas.get();
    }

    @Override
    public int getNumberOfCancellationsTooExpensive() {
        return numberOfCancellationsTooExpensive.get();
    }
    
    @Override
    public double getPrice(GasType type) {
        Double price = gasPrices.get(type);
        if (price == null) {
        	log.log(Level.INFO, "Missing gas price for gas type "+ type.toString());
        	throw new IllegalArgumentException();
        }
        return price;
    }
    
    @Override
    public void setPrice(GasType type, double price) {
    	gasPrices.put(type, price);
    }

}