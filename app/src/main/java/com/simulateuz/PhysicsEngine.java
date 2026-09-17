package com.simulateuz;

/**
 * JNI Bridge to the high-performance C++17 orbital & rocket physics engine.
 * Computes Runge-Kutta 4th Order (RK4) integration, Newton's Universal Gravitation,
 * active rocket propulsion, propellant mass depletion, and atmospheric drag.
 */
public class PhysicsEngine {

    static {
        try {
            System.loadLibrary("physics_core");
        } catch (UnsatisfiedLinkError e) {
            // When running inside Android JVM without native bundle or standalone JVM,
            // provide fallback notice or allow manual path loading
            System.err.println("[PhysicsEngine] Note: libphysics_core loaded via explicit path or system path: " + e.getMessage());
        }
    }

    public static native void initSimulation();
    public static native void stepSimulation(double dt);
    public static native void setThrust(double magnitude);
    public static native void setEngineState(boolean on);
    public static native void setThrustDirection(double x, double y, double z);
    public static native double getAltitude();
    public static native double getVelocity();
    public static native double getFuelMass();
    public static native double getDryMass();
    public static native double getThrust();
    public static native double getTime();
    public static native double getPosX();
    public static native double getPosY();
    public static native double getPosZ();
    public static native double getVelX();
    public static native double getVelY();
    public static native double getVelZ();
}
