package net.bigpoint.assessment.gasstation.test;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import net.bigpoint.assessment.gasstation.GasPump;
import net.bigpoint.assessment.gasstation.GasStation;
import net.bigpoint.assessment.gasstation.GasType;
import net.bigpoint.assessment.gasstation.exceptions.GasTooExpensiveException;
import net.bigpoint.assessment.gasstation.exceptions.NotEnoughGasException;
import net.bigpoint.assessment.gasstation.impl.GasStationImpl;

public class GasStationTest {

    private GasStation gasStation;

    @Before
    public void prepareTestStation() {
    	
        gasStation = new GasStationImpl();

        gasStation.setPrice(GasType.REGULAR, 1.4);
        gasStation.setPrice(GasType.SUPER, 1.9);
        gasStation.setPrice(GasType.DIESEL, 1.1);
        
    }
    
    @After
    public void destroyTestStation() {
        gasStation = null;
    }
    
    @Test
    public void testGetGasPumps() {
    	gasStation.addGasPump((new GasPump(GasType.REGULAR, 350)));
    	gasStation.addGasPump((new GasPump(GasType.SUPER, 150)));
    	gasStation.addGasPump((new GasPump(GasType.DIESEL, 700)));
        Collection<GasPump> pumps = gasStation.getGasPumps();
        Assert.assertEquals(pumps.size(), 3);
    }
    
    
    @Test
    public void testAddGasPump() {
    	Assert.assertEquals(gasStation.getGasPumps().isEmpty(), true);
        gasStation.addGasPump((new GasPump(GasType.REGULAR, 420)));
        Assert.assertEquals(gasStation.getGasPumps().size(), 1);
    }
    
    @Test(expected = GasTooExpensiveException.class)
    public void testFailTooExpensive() throws  GasTooExpensiveException, NotEnoughGasException{
    	 	gasStation.addGasPump((new GasPump(GasType.SUPER, 420)));
            gasStation.buyGas(GasType.SUPER, 20, 1.8);
            Assert.assertEquals(1 , gasStation.getNumberOfCancellationsTooExpensive(), 0d);
    }
    
    @Test
    public void testGetRevenue() throws GasTooExpensiveException, NotEnoughGasException {
    	gasStation.addGasPump((new GasPump(GasType.REGULAR, 350)));
        Assert.assertEquals(14, gasStation.buyGas(GasType.REGULAR, 10, 1.4), 0d );
        Assert.assertEquals(14, gasStation.getRevenue(), 0d);
    }
    
    @Test
    public void testGetNumberOfSales() throws GasTooExpensiveException, NotEnoughGasException {
    	gasStation.addGasPump((new GasPump(GasType.REGULAR, 350)));
        Assert.assertEquals(14, gasStation.buyGas(GasType.REGULAR, 10, 1.4), 0d );
        Assert.assertEquals(1, gasStation.getNumberOfSales(), 0d);
    }

    @Test(expected = NotEnoughGasException.class)
    public void testFailNotEnoughGas() throws NotEnoughGasException, GasTooExpensiveException {
    	 gasStation.addGasPump((new GasPump(GasType.REGULAR, 50)));
         gasStation.buyGas(GasType.REGULAR, 51, 1.4);
         Assert.assertEquals(1 , gasStation.getNumberOfCancellationsNoGas(), 0d);
    }

    @Test
    public void testGetAndSetPrice() {
        gasStation.setPrice(GasType.DIESEL, 1.897);
        Assert.assertEquals(1.897, gasStation.getPrice(GasType.DIESEL), 0d);
    }
    
    @Test
    public void testBuyGasMultipleThreads() throws InterruptedException, ExecutionException {
    	
    	gasStation.addGasPump((new GasPump(GasType.SUPER, 70)));
    	gasStation.addGasPump((new GasPump(GasType.SUPER, 60)));
    	
    	simulateBuyGasThreads(100);
    }
    
    
    private void simulateBuyGasThreads(final int threadCount) throws InterruptedException, ExecutionException {
        Callable<Double> task = new Callable<Double>() {
            @Override
            public Double call() throws NotEnoughGasException, GasTooExpensiveException {
            	// will be called by 100 threads, making revenue of 190$
                return gasStation.buyGas(GasType.SUPER, 1, 1.9);
            }
        };
        List<Callable<Double>> tasks = Collections.nCopies(threadCount, task);
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        executorService.invokeAll(tasks);
        
        // revenue must be 190$ by this step
        Assert.assertEquals(190.0, gasStation.getRevenue(), 0d);
    }
}