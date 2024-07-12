package rarsreborn;

import rarsreborn.core.simulator.Simulator32;

public class SimulatorThread implements Runnable{
    Simulator32 simulator;

    SimulatorThread(Simulator32 sim) {
        simulator = sim;
    }

    @Override
    public void run() {
        simulator.run();
    }
}
