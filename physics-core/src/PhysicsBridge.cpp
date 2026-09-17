#include "PhysicsBridge.h"
#include "../include/state.h"
#include "../include/gravity.h"
#include "../include/Integrator.h"
#include <jni.h>
#include <memory>
#include <cmath>

static simulateuz::RocketState g_rocket_state;

static void reset_simulation_state() {
    g_rocket_state = simulateuz::RocketState();
    // Launch pad on Earth's equator: altitude 0 (distance = EARTH_RADIUS along Y axis)
    g_rocket_state.position = simulateuz::Vector3D(0.0, simulateuz::EARTH_RADIUS, 0.0);
    // Initial velocity at launchpad (can account for Earth's rotational speed ~465 m/s eastward)
    g_rocket_state.velocity = simulateuz::Vector3D(0.0, 0.0, 0.0);
    g_rocket_state.dry_mass = 25000.0;       // 25 metric tons
    g_rocket_state.fuel_mass = 395000.0;     // 395 metric tons
    g_rocket_state.thrust_force = 7607000.0; // 7.6 MN (Falcon 9 full thrust)
    g_rocket_state.isp = 311.0;              // 311s Specific Impulse
    g_rocket_state.is_engine_on = true;
    g_rocket_state.thrust_direction = simulateuz::Vector3D(0.0, 1.0, 0.0); // Pointing straight up initially
    g_rocket_state.time = 0.0;
}

extern "C" {

void init_simulation() {
    reset_simulation_state();
}

void step_simulation(double dt) {
    // Implement an automatic gravity turn profile as rocket gains altitude:
    // Below 1 km: vertical climb
    // Between 1 km and 70 km: tilt gradually toward orbital insertion (downrange east)
    double alt = get_altitude();
    if (alt > 1000.0 && alt < 70000.0) {
        double progress = (alt - 1000.0) / 69000.0; // 0 to 1
        // Pitch angle from 90 deg (vertical) down to ~15 deg
        double angle_rad = (90.0 - progress * 75.0) * (3.14159265358979323846 / 180.0);
        g_rocket_state.thrust_direction = simulateuz::Vector3D(std::cos(angle_rad), std::sin(angle_rad), 0.0);
    } else if (alt >= 70000.0) {
        // Orbit injection burn direction
        g_rocket_state.thrust_direction = simulateuz::Vector3D(1.0, 0.1, 0.0).normalized();
    }

    g_rocket_state = simulateuz::RK4Integrator::step(g_rocket_state, dt);
}

void set_thrust(double magnitude) {
    g_rocket_state.thrust_force = magnitude;
    if (magnitude <= 0.0) {
        g_rocket_state.is_engine_on = false;
    } else if (g_rocket_state.fuel_mass > 0.0) {
        g_rocket_state.is_engine_on = true;
    }
}

void set_engine_state(int state) {
    g_rocket_state.is_engine_on = (state != 0 && g_rocket_state.fuel_mass > 0.0);
}

void set_thrust_direction(double x, double y, double z) {
    g_rocket_state.thrust_direction = simulateuz::Vector3D(x, y, z).normalized();
}

double get_altitude() {
    double r = g_rocket_state.position.magnitude();
    return r - simulateuz::EARTH_RADIUS;
}

double get_velocity() {
    return g_rocket_state.velocity.magnitude();
}

double get_fuel_mass() {
    return g_rocket_state.fuel_mass;
}

double get_dry_mass() {
    return g_rocket_state.dry_mass;
}

double get_thrust() {
    return g_rocket_state.is_engine_on ? g_rocket_state.thrust_force : 0.0;
}

double get_pos_x() { return g_rocket_state.position.x; }
double get_pos_y() { return g_rocket_state.position.y; }
double get_pos_z() { return g_rocket_state.position.z; }

double get_vel_x() { return g_rocket_state.velocity.x; }
double get_vel_y() { return g_rocket_state.velocity.y; }
double get_vel_z() { return g_rocket_state.velocity.z; }

double get_time() { return g_rocket_state.time; }

// =========================================================================
// JNI bindings for com.simulateuz.PhysicsEngine
// =========================================================================

JNIEXPORT void JNICALL Java_com_simulateuz_PhysicsEngine_initSimulation(JNIEnv *, jclass) {
    init_simulation();
}

JNIEXPORT void JNICALL Java_com_simulateuz_PhysicsEngine_stepSimulation(JNIEnv *, jclass, jdouble dt) {
    step_simulation(dt);
}

JNIEXPORT void JNICALL Java_com_simulateuz_PhysicsEngine_setThrust(JNIEnv *, jclass, jdouble magnitude) {
    set_thrust(magnitude);
}

JNIEXPORT void JNICALL Java_com_simulateuz_PhysicsEngine_setEngineState(JNIEnv *, jclass, jboolean on) {
    set_engine_state(on ? 1 : 0);
}

JNIEXPORT void JNICALL Java_com_simulateuz_PhysicsEngine_setThrustDirection(JNIEnv *, jclass, jdouble x, jdouble y, jdouble z) {
    set_thrust_direction(x, y, z);
}

JNIEXPORT jdouble JNICALL Java_com_simulateuz_PhysicsEngine_getAltitude(JNIEnv *, jclass) {
    return get_altitude();
}

JNIEXPORT jdouble JNICALL Java_com_simulateuz_PhysicsEngine_getVelocity(JNIEnv *, jclass) {
    return get_velocity();
}

JNIEXPORT jdouble JNICALL Java_com_simulateuz_PhysicsEngine_getFuelMass(JNIEnv *, jclass) {
    return get_fuel_mass();
}

JNIEXPORT jdouble JNICALL Java_com_simulateuz_PhysicsEngine_getDryMass(JNIEnv *, jclass) {
    return get_dry_mass();
}

JNIEXPORT jdouble JNICALL Java_com_simulateuz_PhysicsEngine_getThrust(JNIEnv *, jclass) {
    return get_thrust();
}

JNIEXPORT jdouble JNICALL Java_com_simulateuz_PhysicsEngine_getTime(JNIEnv *, jclass) {
    return get_time();
}

JNIEXPORT jdouble JNICALL Java_com_simulateuz_PhysicsEngine_getPosX(JNIEnv *, jclass) {
    return get_pos_x();
}

JNIEXPORT jdouble JNICALL Java_com_simulateuz_PhysicsEngine_getPosY(JNIEnv *, jclass) {
    return get_pos_y();
}

JNIEXPORT jdouble JNICALL Java_com_simulateuz_PhysicsEngine_getPosZ(JNIEnv *, jclass) {
    return get_pos_z();
}

JNIEXPORT jdouble JNICALL Java_com_simulateuz_PhysicsEngine_getVelX(JNIEnv *, jclass) {
    return get_vel_x();
}

JNIEXPORT jdouble JNICALL Java_com_simulateuz_PhysicsEngine_getVelY(JNIEnv *, jclass) {
    return get_vel_y();
}

JNIEXPORT jdouble JNICALL Java_com_simulateuz_PhysicsEngine_getVelZ(JNIEnv *, jclass) {
    return get_vel_z();
}

} // extern "C"
